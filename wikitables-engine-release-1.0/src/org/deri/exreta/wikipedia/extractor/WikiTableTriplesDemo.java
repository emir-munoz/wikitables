package org.deri.exreta.wikipedia.extractor;

import gnu.trove.map.hash.THashMap;
import gnu.trove.set.hash.THashSet;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.apache.log4j.xml.DOMConfigurator;
import org.cyberneko.html.parsers.SAXParser;
import org.deri.exreta.dal.collections.CustomEntry;
import org.deri.exreta.dal.connection.thread.LoadURLHandler;
import org.deri.exreta.dal.dbpedia.dto.CellResources;
import org.deri.exreta.dal.dbpedia.dto.PredicateStat;
import org.deri.exreta.dal.dbpedia.dto.Resource;
import org.deri.exreta.dal.dbpedia.dto.Score;
import org.deri.exreta.dal.dbpedia.dto.TableCell;
import org.deri.exreta.dal.dbpedia.dto.TableHeader;
import org.deri.exreta.dal.dbpedia.dto.TableRelation;
import org.deri.exreta.dal.dbpedia.dto.Triple;
import org.deri.exreta.dal.dbpedia.dto.TripleString;
import org.deri.exreta.dal.dbpedia.query.DBpediaDAO;
import org.deri.exreta.dal.dbpedia.query.QueryBuilder.EntityType;
import org.deri.exreta.dal.exceptions.DownloadException;
import org.deri.exreta.dal.main.LocationConstants;
import org.deri.exreta.dal.weka.model.FVTripleDTO;
import org.deri.exreta.dal.weka.model.PredictionTriple;
import org.deri.exreta.wikipedia.parser.dto.CellType;
import org.deri.exreta.wikipedia.parser.test.ParseTriplesOutput;
import org.deri.exreta.wikitable.rank.RankRelation;
import org.deri.exreta.wikitable.rank.Similarity;
import org.deri.exreta.wikitable.rank.RankRelation.RankOpt;
import org.dom4j.Document;
import org.dom4j.io.SAXReader;

import tartar.table_flattening.TableExtractor;
import websphinx.Element;
import websphinx.Page;
import websphinx.Tag;
import cl.em.utils.math.MathUtils;
import cl.em.utils.performance.MemoryUtils;
import cl.em.utils.string.StringUtils;
import cl.yahoo.webtables.features.FeaturesExtractorYData;

/**
 * Extraction of triples from each table in a given set of Wikipedia articles.
 * 
 * @author Emir Muñoz <emir@emunoz.org>
 * @version 0.2.1
 * 
 */
public class WikiTableTriplesDemo
{
	private static final Logger					_log		= Logger.getLogger(WikiTableTriplesDemo.class);
	private static final tartar.common.Logger	logger		= new tartar.common.Logger();
	private THashMap<String, PredicateStat>		predicates	= new THashMap<String, PredicateStat>();
	private LoadURLHandler						loadHandler	= new LoadURLHandler();
	private SAXParser							xmlReader	= new org.cyberneko.html.parsers.SAXParser();
	private DBpediaDAO							sparql;
	private ArrayList<PredictionTriple>			listCandidateTriples;
	private StringBuilder						featureVector;
	private FVTripleDTO							candidateTriples;
	private InputStream							page;
	private SAXReader							reader;
	private WikiTableAccess						access;
	private FeaturesExtractorYData				thisTableExtractor;
	private RankRelation						rank;
	private String								WORKSPACE;

	/**
	 * Constructor of WikiTableTriplesDemo
	 * 
	 * @param properties Configuration with paths.
	 */
	public WikiTableTriplesDemo(Properties properties)
	{
		// initialize class
		if (properties != null)
		{
			_log.info("Initializating extractor WikiTableTriplesDemo");
			WORKSPACE = properties.getProperty("SERVER_FOLDER");
			sparql = new DBpediaDAO(properties);
		} else
		{
			_log.error("Workspace parameter was null. Using 'server' configuration by default.");
			System.exit(0);
		}
		access = new WikiTableAccess(sparql);

		// loading predicates and statistics
		BufferedReader br = null;
		String line;
		try
		{
			br = new BufferedReader(new FileReader(WORKSPACE + properties.getProperty("PREDICATES_STAT")));
			PredicateStat predi = null;
			while ((line = br.readLine()) != null && line.length() > 0)
			{
				predi = new PredicateStat(line);
				predicates.put(predi.getPredicate(), predi);
			}
			br.close();
			_log.info("Readed " + predicates.size() + " predicates");
		} catch (FileNotFoundException ex1)
		{
			_log.error(WORKSPACE + properties.getProperty("PREDICATES_STAT") + " (No such file or directory)"
					+ ", Message: " + ex1.getMessage());
		} catch (IOException ex2)
		{
			_log.error("Error reading file " + WORKSPACE + properties.getProperty("PREDICATES_STAT") + ", Message: "
					+ ex2.getMessage());
		}

		// initialize parser for HTML page
		if (reader == null)
			reader = new SAXReader(xmlReader);
	}

	/**
	 * Return the list of candidate triples
	 * 
	 * @return
	 */
	public FVTripleDTO getCandidateTriples()
	{
		return candidateTriples;
	}

	/**
	 * Function to initialize the processing of an articles.
	 * 
	 * @param article A given article name to be downloaded and processed.
	 * @throws Exception
	 */
	public void init(String article) throws Exception
	{
		// instantiate time
		candidateTriples = new FVTripleDTO();
		listCandidateTriples = new ArrayList<PredictionTriple>();
		featureVector = new StringBuilder();
		thisTableExtractor = new FeaturesExtractorYData();
		reader.resetHandlers();

		// escape article's title
		URI uri = new URI("http", "en.wikipedia.org", "/wiki/" + article, null);
		String articleURL = uri.toASCIIString();
		_log.info("Trying to download wikipedia article " + articleURL);

		// read the article from a given URL
		page = loadHandler.downloadURLRetry(articleURL, 10);

		if (page == null)
			throw new DownloadException("Wikipedia article could not be downloaded.");
		else
			_log.info(String.format("Wikipedia article: %s downloaded successfuly!", articleURL));

		// parsing HTML as XML file
		Document wikiDoc = reader.read(page);

		// extract relations given an HTML page with tables
		try
		{
			_log.info("Starting triple extraction");
			doRelationExtraction(wikiDoc, article);
		} catch (Exception ex)
		{
			_log.info(ex.getMessage());
			throw new Exception(ex);
		}

		// save triples and its features
		candidateTriples.setCandidateTriples(listCandidateTriples);
		featureVector.trimToSize();
		candidateTriples.setFeatureVector(featureVector.toString());
	}

	/**
	 * Function to read an HTML file, extract their tables, and discover relations.
	 * 
	 * @param wikiDoc HTML document to process.
	 * @param articleName Article name.
	 * @throws Exception
	 */
	private void doRelationExtraction(Document wikiDoc, String articleName) throws Exception
	{
		String xmlPage = "";

		// Processing the file to extract the web-tables
		xmlPage = wikiDoc.asXML().replace("<TD/>", "<TD>&nbsp;</TD>\n")
				.replaceAll("<TD>[\\s|\n]*</TD>", "<TD>&nbsp;</TD>\n")
				.replaceAll("<TD ALIGN=\"RIGHT\">\\s*</TD>", "<TD ALIGN=\"RIGHT\">&nbsp;</TD>\n")
				.replaceAll("<TH>[\\s|\n]*!", "<TH>&nbsp;</TH><TH>");

		/* *******************************************************
		 * Read HTML and extract all tables from the file
		 * ******************************************************
		 */
		// Extract the a list of <TABLE> tags from the page
		ArrayList<Element> tableList = new ArrayList<Element>();
		Page page = new Page(new URL("http://en.wikipedia.org/wiki/"), xmlPage);
		tableList = new TableExtractor(logger).ExtractLeafTables(page);

		// Iterate over all HTML tables
		Iterator<Element> iter = tableList.iterator();
		int tableCounter = 0;
		// _log.info("This page contains " + tableList.size() + " tables");

		Element tableEle = null;
		while (iter.hasNext())
		{
			tableCounter++;
			tableEle = iter.next();

			// Parse and transform the table into a matrix
			int status = thisTableExtractor.setTable(tableEle);

			// If the table is well-formed
			if (status == 1)
			{
				// Tables whose dimension is 2x2 or higher ==> it's a useful table
				if (thisTableExtractor.getColumns() >= 2 && thisTableExtractor.getRows() >= 2)
				{
					// Discard toc, infobox, metadata and navbox tables in the page
					Tag tag = tableEle.getStartTag();
					if (tag.getHTMLAttribute("class") != null
							&& (tag.getHTMLAttribute("class").equalsIgnoreCase("toc")
									|| tag.getHTMLAttribute("class").equalsIgnoreCase("toccolours")
									|| tag.getHTMLAttribute("class").equals("navbox")
									|| tag.getHTMLAttribute("class").contains("infobox")
									|| tag.getHTMLAttribute("class").contains("metadata")
									|| tag.getHTMLAttribute("class").contains("plainlinks")
									|| tag.getHTMLAttribute("class").contains("nowraplinks")
									|| tag.getHTMLAttribute("class").contains("maptable")
									|| tag.getHTMLAttribute("class").contains("vcard")
									|| tag.getHTMLAttribute("class").contains("navbox")
									|| tag.getHTMLAttribute("class").contains("navbox-inner")
									|| tag.getHTMLAttribute("class").contains("navbox-columns-table")
									|| tag.getHTMLAttribute("class").contains("collapsible") || tag.getHTMLAttribute(
									"class").contains("nowraplinks")))
					{
						// It's other type of table
						continue;
					}
					if (tag.getHTMLAttribute("id") != null && tag.getHTMLAttribute("id").contains("toc"))
						continue;

					// ***** number of columns
					int columnSize = thisTableExtractor.getColumns();
					// ***** number of rows
					int rowSize = thisTableExtractor.getRows(); // including header if there is one

					/* *******************************************************
					 * Here we can work with the table as a matrix
					 * *******************************************************
					 */
					// Determine column headers
					// List<THashSet<Resource>> tHeaders = access.getHeaders(thisTableExtractor);
					List<TableHeader> tHeaders = access.getHeaders(thisTableExtractor);

					// if (tHeaders != null)
					// for (THashSet<Resource> headerProperty : tHeaders)
					// {
					// // System.out.println(headerProperty.toString());
					// outLog.write("\n" + headerProperty.toString());
					// }

					// if (access.captionPresent)
					// outLog.write("\nThis table contains a caption");
					// else
					// outLog.write("\nThis table doesn't contains a caption");

					// Evaluate the boundaries of the real body for this table
					// if the table has column headers and/or caption, modify start row
					int startRow = 0;
					int endRow = rowSize;
					if (access.captionPresent)
						startRow++; // starts at row 1
					if (tHeaders != null)
						startRow++; // starts at row 2

					// ***************************************************************************
					// start processing the table: extracting resources and identiying relations
					// ***************************************************************************
					THashSet<TableCell> allTableCell = new THashSet<TableCell>();
					List<TableRelation> tableRowRelations = new ArrayList<TableRelation>();
					List<ArrayList<TableCell>> matrixTableCell = new ArrayList<ArrayList<TableCell>>();

					// Traverse the matrix collecting the found resources
					int[] realCountArray = new int[endRow];
					List<TableCell> sameRowCells = null;
					List<TableRelation> rowRelations = null;
					List<CellResources> rowResources = null;
					for (int rowCount = startRow; rowCount < endRow; rowCount++)
					{
						/* *******************************************************
						 * 1- List for all the resources of the table
						 * *******************************************************
						 */
						sameRowCells = new ArrayList<TableCell>();
						rowResources = access.getRowResources(thisTableExtractor, rowCount, CellType.TBODY);
						realCountArray[rowCount] = access.getRealColNumber();

						for (CellResources colCellResources : rowResources)
						{
							THashSet<Resource> cellResources = colCellResources.getResoSet();
							// Recover all the resource in each cell
							TableCell cell = null;
							for (Resource reso : cellResources)
							{
								cell = new TableCell(reso, rowCount, colCellResources.getColumn());
								sameRowCells.add(cell);

								// Add all the found resources for this cell to the big collection of resources
								if (!cell.getResource().isEmpty())
									allTableCell.add(cell);
							}
						}
						matrixTableCell.add((ArrayList<TableCell>) sameRowCells);

						/* *******************************************************
						 * 2- Identifying relations between elements in the same row
						 * *******************************************************
						 */
						rowRelations = sparql.getDBpediaRelationsCell(sameRowCells);
						tableRowRelations.addAll(rowRelations);
					} // endFor each row

					// get number of resources for each column
					int[] resourcePerColumn = new int[columnSize];
					for (int rowIndex = 0; rowIndex < matrixTableCell.size(); rowIndex++)
					{
						for (TableCell cell : matrixTableCell.get(rowIndex))
						{
							if (cell.getCol() != -1 && !cell.getResource().isEmpty())
								resourcePerColumn[cell.getCol()] = resourcePerColumn[cell.getCol()] + 1;
						}
					}

					// get unique resources for each column
					int[] uniqResourcePerColumn = new int[columnSize];
					List<THashSet<String>> resources = new ArrayList<THashSet<String>>();
					for (int i = 0; i < columnSize; i++)
					{
						resources.add(new THashSet<String>());
					}
					for (int rowIndex = 0; rowIndex < matrixTableCell.size(); rowIndex++)
					{
						for (TableCell cell : matrixTableCell.get(rowIndex))
						{
							if (cell.getCol() != -1 && !cell.getResource().isEmpty())
							{
								resources.get(cell.getCol()).add(cell.getResource().getURI());
							}
						}
					}
					for (int i = 0; i < uniqResourcePerColumn.length; i++)
					{
						uniqResourcePerColumn[i] = resources.get(i).size();
					}

					/*
					 * =====================================================================================
					 * At this point we have the extracted relation per row, based on the present resources.
					 * =====================================================================================
					 */
					Triple triple = null;
					Resource mainEntity = null;

					/*
					 * *******************************************************************************************
					 * Identifying relations between the main resource (page) and the resources within the table
					 * *******************************************************************************************
					 */
					List<TableCell> tableResources = new ArrayList<TableCell>();
					mainEntity = sparql.getResourceURI(articleName, EntityType.OBJECT);

					TableCell mainEntityCell = new TableCell(mainEntity, -1, -1);
					tableResources.add(mainEntityCell); // add article entity
					tableResources.addAll(allTableCell); // add all found resources

					// ===========================================================
					// IMPORTANT: Add the relations extracted from article title
					List<TableRelation> tableRelations = sparql.getDBpediaMainRelations(tableResources);
					tableRowRelations.addAll(tableRelations);
					// ===========================================================

					// create an instance for ranking
					rank = new RankRelation(tableRowRelations);

					/* *******************************
					 * Rank the extracted relations
					 * *******************************
					 */
					// Define properties for the ranking function
					double threshold = 0.0;
					Properties props_row = new Properties();
					props_row.put("n_t", rowSize - startRow); // bodySize = rowSize - startRow
					props_row.put("threshold", threshold);

					// In case there exists headers
					if (tHeaders != null)
						props_row.put("headers", tHeaders);

					// Call the ranking function
					rank.makeRanking(RankOpt.RANK_freq, props_row);

					// Show ranked and filtered relations
					int numbFoundRelations = rank.getFilteredRelations().size();
					// System.out.println("Table-filtered@" + threshold + "\t" + articleName + "\t"
					// + rank.getFilteredRelations());

					// For each of the rows in the matrix
					SortedMap<Integer, String> outcome = null;
					TableRelation relation_i = null;
					String[] headers = new String[2];
					PredicateStat pred = null;
					PredictionTriple candidateTriple = null;
					THashMap<String, Integer> potRelSumMap = new THashMap<String, Integer>(); // 2
					THashMap<String, Integer> uniqPotRelMap = new THashMap<String, Integer>(); // 1
					THashMap<String, Integer> uniqRelMap = new THashMap<String, Integer>(); // 3
					THashMap<String, Integer> potUniqRelMap = new THashMap<String, Integer>(); // 4
					Integer _uniquePRM = null; // 1
					Integer _uniqueRM = null; // 3

					/*
					 * Potential relations: resources_sub * resources_obj
					 */
					for (CustomEntry entry : rank.getFilteredRelations())
					{
						int potRelCounter = 0;
						String[] split = ((String) entry.getEntry().getKey()).split("#");

						int col1 = 0, col2 = 0;
						try
						{
							col1 = Integer.parseInt(split[1]);
							col2 = Integer.parseInt(split[2]);
						} catch (Exception ex)
						{
							continue;
						}

						int[] invResources;
						for (int rowIndex = 0; rowIndex < matrixTableCell.size(); rowIndex++)
						{
							invResources = new int[2];
							for (TableCell cell : matrixTableCell.get(rowIndex))
							{
								if (cell.getCol() != -1 && !cell.getResource().isEmpty())
								{
									if (cell.getCol() == col1)
									{
										invResources[0]++;
									} else if (cell.getCol() == col2)
									{
										invResources[1]++;
									}
								}
							}
							if (col1 == -1)
								invResources[0] = 1;
							if (col2 == -1)
								invResources[1] = 1;

							potRelCounter += invResources[0] * invResources[1];
						}
						potRelSumMap.put(split[1] + "#" + split[2], potRelCounter);
						// }
						// System.out.println("Relation=" + (String) entry.getEntry().getKey()
						// + " PotentialRelations=" + potRelCounter);
					}

					/*
					 * Potential unique relations
					 */
					for (CustomEntry entry : rank.getFilteredRelations())
					{
						String[] split = ((String) entry.getEntry().getKey()).split("#");
						String[] invResources;
						THashSet<String> pairsSet = new THashSet<String>();

						int col1 = 0, col2 = 0;
						try
						{
							col1 = Integer.parseInt(split[1]);
							col2 = Integer.parseInt(split[2]);
						} catch (Exception ex)
						{
							continue;
						}

						for (int rowIndex = 0; rowIndex < matrixTableCell.size(); rowIndex++)
						{
							invResources = new String[2];
							for (TableCell cell1 : matrixTableCell.get(rowIndex))
							{
								if (!cell1.getResource().isEmpty())
								{
									if (col1 == -1)
										invResources[0] = articleName;
									else if (cell1.getCol() == col1)
										invResources[0] = cell1.getResource().getURI();
									for (TableCell cell2 : matrixTableCell.get(rowIndex))
									{

										if (cell2.getCol() != -1 && !cell2.getResource().isEmpty())
										{
											if (col2 == -1)
												invResources[1] = articleName;
											else if (cell2.getCol() == col2)
												invResources[1] = cell2.getResource().getURI();
										}
										if (invResources[0] != null && invResources[1] != null)
										{
											pairsSet.add(invResources[0] + "#" + invResources[1]);
											// System.out.println(invResources[0] + "#" + invResources[1]);
										}
									}
								}
							}
						}
						potUniqRelMap.put(split[1] + "#" + split[2], pairsSet.size());
						// }
						// System.out.println("Relation=" + (String) entry.getEntry().getKey() + " PotentialRelations="
						// + pairsSet.size());
					}

					/*
					 * ==================================================================
					 * feature extraction
					 * ==================================================================
					 */
					// for all the resources found in the rows
					for (int rowCount = 0; rowCount < matrixTableCell.size(); rowCount++)
					{
						List<TableCell> rowCell = matrixTableCell.get(rowCount);
						headers = new String[2];

						// Check each selected relations.
						for (CustomEntry entry : rank.getFilteredRelations())
						{
							outcome = new TreeMap<Integer, String>();
							relation_i = new TableRelation();
							relation_i = relation_i.convertRelation((String) entry.getEntry().getKey(), (Score) entry
									.getEntry().getValue());

							if (relation_i == null)
							{
								_log.warn("The relation couln't be created for " + (String) entry.getEntry().getKey());
								continue;
							}

							/*
							 * Unique potential relations held (not counting duplicates in the same row - columns)
							 */
							if (uniqPotRelMap.contains((String) entry.getEntry().getKey()))
								_uniquePRM = uniqPotRelMap.get((String) entry.getEntry().getKey());
							else
							{
								_uniquePRM = rank.uniquePotentialRelationsHeld((String) entry.getEntry().getKey());
								uniqPotRelMap.put((String) entry.getEntry().getKey(), _uniquePRM);
							}
							// System.out.println(_uniquePR + "\t" + (String) entry.getEntry().getKey());

							/*
							 * Unique relations held (count each found relation with same resources once)
							 */
							if (uniqRelMap.contains((String) entry.getEntry().getKey()))
								_uniqueRM = uniqRelMap.get((String) entry.getEntry().getKey());
							else
							{
								_uniqueRM = rank.uniqueRelationsHeld((String) entry.getEntry().getKey());
								uniqRelMap.put((String) entry.getEntry().getKey(), _uniqueRM);
							}

							/*
							 * Start feature extraction
							 */
							// ===== feature #1
							outcome.put(1, String.valueOf(tableList.size()));
							// ===== feature #2
							outcome.put(2, String.valueOf(tableCounter));
							// ===== feature #3
							outcome.put(3, String.valueOf(rowSize));
							// ===== feature #4
							outcome.put(4, String.valueOf(realCountArray[rowCount + startRow]));
							// ===== feature #5
							outcome.put(5, String.valueOf(MathUtils.divide(rowSize,
									realCountArray[rowCount + startRow], LocationConstants.DECIMAL_PLACE)));
							// ===== feature #6
							outcome.put(6, String.valueOf(numbFoundRelations));
							// ===== feature #7
							if (relation_i.getCol1() != -1)
								outcome.put(7, String.valueOf(relation_i.getCol1() + 1));
							else
								outcome.put(7, String.valueOf(relation_i.getCol1()));
							// ===== feature #8
							if (relation_i.getCol2() != -1)
								outcome.put(8, String.valueOf(relation_i.getCol2() + 1));
							else
								outcome.put(8, String.valueOf(relation_i.getCol2()));
							// ===== feature #9
							if (relation_i.getCol1() != -1)
								outcome.put(9, String.valueOf(resourcePerColumn[relation_i.getCol1()]));
							else
								outcome.put(9, String.valueOf(-1.0)); // ===== feature
							// ===== feature #10
							if (relation_i.getCol2() != -1)
								outcome.put(10, String.valueOf(resourcePerColumn[relation_i.getCol2()]));
							else
								outcome.put(10, String.valueOf(-1.0));
							// ===== feature #11
							if (relation_i.getCol1() != -1 && relation_i.getCol2() != -1)
								outcome.put(11, String.valueOf(MathUtils.divide(
										resourcePerColumn[relation_i.getCol1()],
										resourcePerColumn[relation_i.getCol2()], LocationConstants.DECIMAL_PLACE)));
							else
								outcome.put(11, String.valueOf(0.0));
							// ===== feature #12
							if (relation_i.getCol1() == -1)
								outcome.put(12, String.valueOf(1));
							else
								outcome.put(12, String.valueOf(uniqResourcePerColumn[relation_i.getCol1()]));
							// ===== feature #13
							if (relation_i.getCol2() == -1)
								outcome.put(13, String.valueOf(1));
							else
								outcome.put(13, String.valueOf(uniqResourcePerColumn[relation_i.getCol2()]));
							// ===== feature #14
							if (relation_i.getCol1() != -1 && relation_i.getCol2() != -1)
								outcome.put(14, String.valueOf(MathUtils.divide(
										uniqResourcePerColumn[relation_i.getCol1()],
										uniqResourcePerColumn[relation_i.getCol2()], LocationConstants.DECIMAL_PLACE)));
							else
								outcome.put(14, String.valueOf(0.0));
							// outcome.put(14, String.valueOf(relation_i.getScore().getMaxPotentialRel()));
							// ===== feature #15
							outcome.put(15,
									String.valueOf(potRelSumMap.get(relation_i.getCol1() + "#" + relation_i.getCol2())));
							// outcome.put(15, String.valueOf(relation_i.getScore().getScore()));
							// ===== feature #16 --- unique potential relations across rows
							outcome.put(16, String.valueOf(potUniqRelMap.get(relation_i.getCol1() + "#"
									+ relation_i.getCol2())));
							// // outcome.put(16, String.valueOf(_uniquePRM.intValue()));
							// ===== feature #17 -- predicate statistics
							if (pred != null)
							{
								double commonness = (Double) (1 / (1 + Math.abs(Math.log10(MathUtils.divide(
										pred.getTimes(), LocationConstants.PRED_MAX_INSTA,
										LocationConstants.DECIMAL_PLACE)))));
								outcome.put(17,
										String.valueOf(MathUtils.round(commonness, LocationConstants.DECIMAL_PLACE)));
								// outcome.put(21, String.valueOf(MathUtils.divide(pred.getTimes(),
								// LocationConstants.PRED_MAX_INSTA, LocationConstants.DECIMAL_PLACE)));
							} else
								outcome.put(17, String.valueOf(0.0));
							// ===== feature #18
							// ratio different subjects for this predicate
							if (pred != null)
							{
								double ratio = (Double) (1 / (1 + Math.abs(Math.log10(MathUtils.divide(
										pred.getUniqSubj(), LocationConstants.PRED_MAX_SUBJ,
										LocationConstants.DECIMAL_PLACE)))));
								// outcome.put(22, String.valueOf(MathUtils.divide(pred.getUniqSubj(),
								// LocationConstants.PRED_MAX_SUBJ, LocationConstants.DECIMAL_PLACE)));
								outcome.put(18, String.valueOf(MathUtils.round(ratio, LocationConstants.DECIMAL_PLACE)));
							} else
								outcome.put(18, String.valueOf(0.0));
							// ===== feature #19
							if (pred != null)
							{
								double ratio = (Double) (1 / (1 + Math.abs(Math.log10(MathUtils.divide(
										pred.getUniqObj(), LocationConstants.PRED_MAX_OBJ,
										LocationConstants.DECIMAL_PLACE)))));
								// outcome.put(23, String.valueOf(MathUtils.divide(pred.getUniqObj(),
								// LocationConstants.PRED_MAX_OBJ, LocationConstants.DECIMAL_PLACE)));
								outcome.put(19, String.valueOf(MathUtils.round(ratio, LocationConstants.DECIMAL_PLACE)));
							} else
								outcome.put(19, String.valueOf(0.0));
							// ===== feature #20
							if (pred != null)
							{
								double subj = (Double) (1 / (1 + Math.abs(Math.log10(MathUtils.divide(
										pred.getUniqSubj(), LocationConstants.PRED_MAX_SUBJ,
										LocationConstants.DECIMAL_PLACE)))));
								double obje = (Double) (1 / (1 + Math.abs(Math.log10(MathUtils.divide(
										pred.getUniqObj(), LocationConstants.PRED_MAX_OBJ,
										LocationConstants.DECIMAL_PLACE)))));
								double ratio = (Double) MathUtils.divide(subj, obje, LocationConstants.DECIMAL_PLACE);
								outcome.put(20, String.valueOf(ratio));
							} else
								outcome.put(20, String.valueOf(0.0));

							// Features for column 1
							// System.out.println(relation_i.getCellRelation().getRelationURI());
							int[] featCol1 = access.getInnerCellContent(thisTableExtractor, rowCount + startRow,
									relation_i.getCol1(), CellType.TBODY);
							// Features for column 2
							int[] featCol2 = access.getInnerCellContent(thisTableExtractor, rowCount + startRow,
									relation_i.getCol2(), CellType.TBODY);

							// ===== feature #21 -- how many resources col1
							outcome.put(21, String.valueOf(featCol1[0]));
							// ===== feature #22 -- how many resources col2
							outcome.put(22, String.valueOf(featCol2[0]));
							// ===== feature #23
							if (featCol2[0] > 0)
								outcome.put(23, String.valueOf(MathUtils.divide(featCol1[0], featCol2[0],
										LocationConstants.DECIMAL_PLACE)));
							else
								outcome.put(23, String.valueOf(0.0));
							// ===== feature #24 -- length col1
							outcome.put(24, String.valueOf(featCol1[1]));
							// ===== feature #25 -- length col2
							outcome.put(25, String.valueOf(featCol2[1]));
							// ===== feature #26 -- formatting subject
							if (featCol1[2] > 0 || featCol1[3] > 0 || featCol1[4] > 0)
								outcome.put(26, String.valueOf(1));
							else
								outcome.put(26, String.valueOf(0));
							// ===== feature #27 -- formatting object
							if (featCol2[2] > 0 || featCol2[3] > 0 || featCol2[4] > 0)
								outcome.put(27, String.valueOf(1));
							else
								outcome.put(27, String.valueOf(0));

							// ******************************
							// similarity
							float similaritySub = 0.0f, similarityObj = 0.0f;
							// ===== feature #28
							if (relation_i.getCol1() == -1)
							{
								// "##PAGE_TITLE##"
								headers[0] = null;
								outcome.put(28, String.valueOf(-1.0));
							} else if (tHeaders != null && tHeaders.size() > 0)
							{
								try
								{
									for (TableHeader header : tHeaders)
									{
										if (header.gethColumn() == relation_i.getCol1())
											headers[0] = header.gethText();
									}
									String[] urlSplit = relation_i.getCellRelation().getRelationURI().split("/");
									similaritySub = Similarity.distanceStr(headers[0].toLowerCase(),
											urlSplit[urlSplit.length - 1]);
									// System.out.println("SIMILARITY=" + similarity + "\t" + reso.getURI()
									// + "\t" + triple.getRelation().getURI());
								} catch (Exception ex)
								{
									headers[0] = null;
								}
								outcome.put(28, String.valueOf(similaritySub));
							} else
							{
								// "##NO_HEADER##"
								outcome.put(28, String.valueOf(0.0));
							}
							// ===== feature #29
							if (relation_i.getCol2() == -1)
							{
								// "##PAGE_TITLE##"
								headers[1] = null;
								outcome.put(29, String.valueOf(-1.0));
							} else if (tHeaders != null && tHeaders.size() > 0)
							{
								try
								{
									for (TableHeader header : tHeaders)
									{
										if (header.gethColumn() == relation_i.getCol2())
											headers[1] = header.gethText();
									}
									String[] urlSplit = triple.getRelation().getURI().split("/");
									similarityObj = Similarity.distanceStr(headers[1].toLowerCase(),
											urlSplit[urlSplit.length - 1]);
									// similarity = Similarity.distanceStr(reso.getURI(), triple.getRelation()
									// .getURI());
									// System.out.println("SIMILARITY=" + similarity + "\t" + reso.getURI()
									// + "\t" + triple.getRelation().getURI());
								} catch (Exception ex)
								{
									headers[1] = null;
								}
								outcome.put(29, String.valueOf(similarityObj));
							} else
							{
								// "##NO_HEADER##"
								outcome.put(29, String.valueOf(0.0));
							}
							// ===== feature #30 -- maximum similarity
							if (similaritySub > similarityObj)
								outcome.put(30, String.valueOf(similaritySub));
							else
								outcome.put(30, String.valueOf(similarityObj));

							// ********************
							int rowsHold = 0; // number of rows in which the relation holds
							if (relation_i.getCellRelation() != null
									&& relation_i.getCellRelation().getRelation() != null
									&& !relation_i.getCellRelation().getRelation().isEmpty())
							{
								rowsHold = rank.freqRelationRows((String) entry.getEntry().getKey());
							}
							// ===== feature #31
							outcome.put(31, String.valueOf(rowsHold));
							// ===== feature #32
							outcome.put(32, String.valueOf(MathUtils.divide(rowsHold, rowSize,
									LocationConstants.DECIMAL_PLACE)));
							// ===== feature #33
							outcome.put(33, String.valueOf(relation_i.getScore().getPotentialRel()));
							// ===== feature #34
							outcome.put(34, String.valueOf(MathUtils.divide(relation_i.getScore().getPotentialRel(),
									potRelSumMap.get(relation_i.getCol1() + "#" + relation_i.getCol2()),
									LocationConstants.DECIMAL_PLACE)));
							// ===== feature #35 --- unique relations held
							outcome.put(35, String.valueOf(_uniqueRM.intValue()));
							// ===== feature #35 --- unique relations held
							outcome.put(36, String.valueOf(MathUtils.divide(_uniqueRM.intValue(),
									potUniqRelMap.get(relation_i.getCol1() + "#" + relation_i.getCol2()),
									LocationConstants.DECIMAL_PLACE)));

							Resource res1 = null, res2 = null;
							// ===============================
							// Generate triples for all the resources found in each cell
							// ===============================
							List<TableCell> subjectResources = access.filterByColumn(rowCell, relation_i.getCol1());
							List<TableCell> objectResources = access.filterByColumn(rowCell, relation_i.getCol2());

							// add main entity to the resources in subject and object
							if (relation_i.getCol1() == -1)
								subjectResources.add(mainEntityCell);
							if (relation_i.getCol2() == -1)
								objectResources.add(mainEntityCell);

							for (TableCell subjectReso : subjectResources)
							{
								res1 = null;
								if (subjectReso.getCol() == relation_i.getCol1())
									res1 = subjectReso.getResource();
								for (TableCell objectReso : objectResources)
								{
									res2 = null;
									if (objectReso.getCol() == relation_i.getCol2())
										res2 = objectReso.getResource();

									// ============= triple creation ==================
									// Apply filters
									if (res1 == null
											|| res2 == null
											|| res1.isEmpty()
											|| res2.isEmpty()
											|| res1.equals(res2)
											|| res1.getURI().equals(res2.getURI())
											|| res1.getURI().contains("List_of_")
											|| res2.getURI().contains("List_of_")
											|| relation_i.getCellRelation().getRelationURI().isEmpty()
											|| relation_i.getCellRelation().getRelationURI()
													.contains("wikiPageDisambiguates"))
									{
										// _log.debug(String.format(
										// "Skipping creation. This triple didn't pass the filters. RES1: %s, RES2: %s",
										// res1.getURI(), res2.getURI()));
										_log.debug("Skipping creation of triples. It didn't pass the filters");
										continue; // skip the creation of this triple
									}

									// System.out.println(res1 + "\t" + res2);

									// normalize URIs
									String uri1Norm = "", uri2Norm = "";
									uri1Norm = StringUtils.unicodeUnescape(res1.getURI());
									uri2Norm = StringUtils.unicodeUnescape(res2.getURI());
									// if (res1.getURI().contains("\\u0"))
									// uri1Norm = URLUTF8Encoder.unescape(uri1Norm);
									// if (res2.getURI().contains("\\u0"))
									// uri2Norm = URLUTF8Encoder.unescape(uri2Norm);
									res1.setURI(uri1Norm);
									res2.setURI(uri2Norm);

									// Create the triple for current relation
									triple = new Triple(res1, res2, relation_i.getCellRelation().getRelation());
									// ================================================

									// split[3] contains the source for the tiple. b for body and h for main entity
									String[] split = ((String) entry.getEntry().getKey()).split("#");
									// ===== feature #37
									if (split[3].equals("h"))
										outcome.put(37, String.valueOf(1));
									else
										outcome.put(37, String.valueOf(2));

									// check if the knowledge-base already have this triple
									boolean exists = sparql.existsTriple(triple);
									// ===== feature #38
									if (!exists)
										outcome.put(38, "1");
									else
										outcome.put(38, "2");

									if (!triple.toString().isEmpty())
									{
										TripleString trstr = new TripleString(triple.getResource1().getURI(), triple
												.getRelation().getURI(), triple.getResource2().getURI());

										candidateTriple = new PredictionTriple(trstr);
										candidateTriple.setExists(exists);
										listCandidateTriples.add(candidateTriple);
										featureVector.append("+1 " + ParseTriplesOutput.featureVectorToString(outcome)
												+ "\n");

										// System.out.println(candidateTriple.toString());
									}
								}
							} // endFor (TableCell subjectReso : subjectResources)
						} // endFor (CustomEntry entry : rank.getFilteredRelations())
					} // endFor (int rowCount = 0; rowCount < matrixTableCell.size(); rowCount++)
				} // else it's not-useful
			} // else it's a ill-formed table
		} // endwhile (iter.hasNext())
		MemoryUtils.freeMemory();
	}

	/**
	 * Main
	 * 
	 * @param args
	 * @throws Exception
	 */
	public static void main(String[] args)
	{
		DOMConfigurator.configure("./conf/log4j.xml");

		Properties props = new Properties();
		props.put("SERVER_FOLDER", "/home/emir/Work/engineering/wikipedia-tables/trunk/wikitables-engine-trunk");
		props.put("REDIRECT_INDEX", "/db38/redirects.ni");
		props.put("REDIRECT_SPARSE", "/db38/redirects.sp");
		props.put("LABELS_INDEX", "/db38/labels.ni");
		props.put("LABELS_SPARSE", "/db38/labels.sp");
		props.put("LABELS_EXCEPTIONS", "/db38/exceptions");
		props.put("DBPEDIA38_INDEX", "/db38/dbpedia38.en.ni");
		props.put("DBPEDIA38_SPARSE", "/db38/dbpedia38.en.sp");
		props.put("PREDICATES_STAT", "/db38/pred-stats.dat");
		props.put("TITLE_INDEX", "/index/index-title");
		props.put("NUM_TITLES", 25);

		WikiTableTriplesDemo triples = new WikiTableTriplesDemo(props);

		// put an URL to analyze directly from Wikipedia
		String article = "Comparison_of_IDEs";
		try
		{
			triples.init(article);
			System.out.println(triples.getCandidateTriples().getCandidateTriples().size());
		} catch (Exception e)
		{
			_log.error(String.format("Error in WikiTableTriplesDemo processing article: \"%s\" --- %s", article,
					e.getMessage()));
			e.printStackTrace();
		}
	}
}
