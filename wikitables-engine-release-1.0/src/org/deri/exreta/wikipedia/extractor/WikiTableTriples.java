package org.deri.exreta.wikipedia.extractor;

import gnu.trove.map.hash.THashMap;
import gnu.trove.set.hash.THashSet;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
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
import org.deri.exreta.dal.dbpedia.query.DAOInterface;
import org.deri.exreta.dal.dbpedia.query.DBpediaDAO;
import org.deri.exreta.dal.dbpedia.query.QueryBuilder.EntityType;
import org.deri.exreta.dal.main.LocationConstants;
import org.deri.exreta.wikipedia.parser.dto.CellType;
import org.deri.exreta.wikipedia.parser.test.ParseTriplesOutput;
import org.deri.exreta.wikitable.rank.RankRelation;
import org.deri.exreta.wikitable.rank.RankRelation.RankOpt;
import org.deri.exreta.wikitable.rank.Similarity;
import org.dom4j.Document;
import org.dom4j.io.SAXReader;

import tartar.table_flattening.TableExtractor;
import websphinx.Element;
import websphinx.Page;
import websphinx.Tag;
import cl.em.utils.files.DirectoryUtils;
import cl.em.utils.math.MathUtils;
import cl.em.utils.performance.MemoryUtils;
import cl.em.utils.performance.TimeWatch;
import cl.em.utils.string.StringUtils;
import cl.yahoo.webtables.features.FeaturesExtractorYData;

/**
 * Extraction of triples from each table in a given set of Wikipedia articles.
 * 
 * @author Emir Muñoz <emir@emunoz.org>
 * @version 2.3.1
 * 
 */
public class WikiTableTriples
{
	private static final Logger						_log				= Logger.getLogger(WikiTableTriples.class);
	private static final tartar.common.Logger		logger				= new tartar.common.Logger();
	private static THashSet<String>					articleExceptions	= new THashSet<String>();
	private static THashMap<String, PredicateStat>	predicates			= new THashMap<String, PredicateStat>();
	private static String							outputFilename;
	private static String							split;
	private static String							WORKSPACE;
	private DAOInterface							sparql				= null;
	private THashSet<String>						articlesPathSet;
	private FeaturesExtractorYData					thisTableExtractor;
	private WikiTableAccess							access;
	private String									sourceFullPath;
	private BufferedWriter							outLog;
	private BufferedWriter							outTriple;
	private RankRelation							rank;
	private TimeWatch								watch;

	/**
	 * Constructor of WikiTableTriplesDemo
	 * 
	 * @param _workspace Configuration 'server' or 'local'.
	 */
	public WikiTableTriples(Properties properties, String machine)
	{
		if (properties != null)
		{
			sparql = new DBpediaDAO(properties);
			// if (_workspace.equalsIgnoreCase("local"))
			// WORKSPACE = LocationConstants.LOCAL_FOLDER;
			// else if (_workspace.equalsIgnoreCase("server"))
			// WORKSPACE = LocationConstants.SERVER_FOLDER;
			// else if (_workspace.equalsIgnoreCase("test"))
			// WORKSPACE = LocationConstants.SERVER_TEST;
			// else
			// WORKSPACE = _workspace;
			WORKSPACE = properties.getProperty("SERVER_FOLDER");
		} else
			_log.fatal("Error initialization of WikiTableTriples workspace null");
		access = new WikiTableAccess(sparql);

		// loading predicates and statistics
		BufferedReader br = null;
		String line;
		try
		{
			br = new BufferedReader(new FileReader("./conf/exceptions"));
			while ((line = br.readLine()) != null && line.length() > 0)
			{
				articleExceptions.add(line);
			}
			_log.info("Readed " + articleExceptions.size() + " exceptions");
		} catch (FileNotFoundException ex1)
		{
			_log.error("./conf/exceptions (No such file or directory) " + split + " on machine " + machine);
		} catch (IOException ex2)
		{
			_log.error("Error reading file ./conf/exceptions " + split + " on machine " + machine + ", Message: "
					+ ex2.getMessage());
		}

		// loading predicates and statistics
		try
		{
			br = new BufferedReader(new FileReader(WORKSPACE + LocationConstants.PREDICATES_STAT));
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
			_log.error(WORKSPACE + LocationConstants.PREDICATES_STAT + " (No such file or directory)" + split
					+ " on machine " + machine + ", Message: " + ex1.getMessage());
		} catch (IOException ex2)
		{
			_log.error("Error reading file " + WORKSPACE + LocationConstants.PREDICATES_STAT + split + " on machine "
					+ machine + ", Message: " + ex2.getMessage());
		}
	}

	/**
	 * Function to define the set of articles to process.
	 * Case #1: a given article to be downloaded and processed.
	 * Case #2: a path with pre-downloaded articles.
	 * 
	 * @param article
	 * @throws Exception
	 */
	public void init(String article) throws Exception
	{
		thisTableExtractor = new FeaturesExtractorYData();
		sourceFullPath = LocationConstants.WIKI_HTML + split + "dump/";

		// Case #1: work with some split
		if (article.isEmpty())
		{
			articlesPathSet = DirectoryUtils.getFilesList(sourceFullPath, ".html");
			_log.info("Reading " + articlesPathSet.size() + " HTML files readed from " + sourceFullPath);
		} else
			// Case #2: work directly with an Wikipedia article
			_log.info("Reading article: \"" + article + "\"");

		// Output log file
		FileWriter fstreamLog = new FileWriter(outputFilename + ".log");
		outLog = new BufferedWriter(fstreamLog);

		// Output file for triples
		FileWriter fstreamTriple = new FileWriter(outputFilename + ".nq");
		outTriple = new BufferedWriter(fstreamTriple);

		// Input HTML file
		File wikiArti = null;
		InputStream wikiArtiUrl = null;

		// Starting parsing process
		_log.debug("Processing articles now:\n");
		int artiCounter = 0;
		// int upperbound = 0;

		// Instantiate a watch object
		watch = TimeWatch.start();

		// Case #1
		if (article.isEmpty())
		{
			// for each html found in the folder
			for (String articlePath : articlesPathSet)
			{
				// FIXME: JUST TESTING == DELETE
				// ++upperbound;
				// if (upperbound == 5000)
				// break;
				// ====================== DELETE

				if (++artiCounter <= 100)
					System.out.print(".");
				else
				{
					System.out.println(".");
					artiCounter = 0;
				}

				if (articleExceptions.contains(articlePath))
					continue;

				// Reading and parsing the HTML file
				// with NekoHTML we solve some ill-formed problems
				wikiArti = new File(articlePath);
				wikiArtiUrl = new FileInputStream(wikiArti);

				// Parsing HTML as XML file
				SAXParser xmlReader = new org.cyberneko.html.parsers.SAXParser();
				SAXReader reader = new SAXReader(xmlReader);
				Document wikiDoc = reader.read(wikiArtiUrl);

				// Extract HTML tables and RDF relations
				try
				{
					doRelationExtraction(wikiDoc, articlePath);
				} catch (Exception ex)
				{
					throw new Exception(ex);
				}
			}
		} else
		{
			// Read the article from a given URL
			String root = "http://en.wikipedia.org/wiki/";
			LoadURLHandler loadHandler = new LoadURLHandler();
			InputStream page = loadHandler.downloadURLRetry(root + article, 10);

			// Parsing HTML as XML file
			SAXParser xmlReader = new org.cyberneko.html.parsers.SAXParser();
			SAXReader reader = new SAXReader(xmlReader);
			Document wikiDoc = reader.read(page);

			// Extract HTML tables and RDF relations
			// System.out.println(LocationConstants.WIKI_HTML + split + article + ".html");
			try
			{
				doRelationExtraction(wikiDoc, LocationConstants.WIKI_HTML + split + "dump/" + article + ".html");
			} catch (Exception ex)
			{
				throw new Exception(ex);
			}
		}

		_log.info("Finished! See files " + outputFilename + ".log and " + outputFilename + ".nq " + "Time: "
				+ watch.elapsedTime());

		// Closing I/O
		outLog.flush();
		outLog.close();
		fstreamLog.close();
		outTriple.flush();
		outTriple.close();
		fstreamTriple.close();
	}

	/**
	 * Function to read an HTML file, extract their tables, and discover triples.
	 * 
	 * @param wikiDoc HTML document.
	 * @param pathFile Path to the file.
	 * @throws Exception
	 */
	public void doRelationExtraction(Document wikiDoc, String pathFile) throws Exception
	{
		// System.out.println(pathFile + "\t" + sourceFullPath);
		String xmlPage = "";
		// if (pathFile.endsWith("/Blizzard_Entertainment.html"))
		// {
		// Processing the file to extract the web-tables
		xmlPage = wikiDoc.asXML().replace("<TD/>", "<TD>&nbsp;</TD>\n")
				.replaceAll("<TD>[\\s|\n]*</TD>", "<TD>&nbsp;</TD>\n")
				.replaceAll("<TD ALIGN=\"RIGHT\">\\s*</TD>", "<TD ALIGN=\"RIGHT\">&nbsp;</TD>\n")
				.replaceAll("<TH>[\\s|\n]*!", "<TH>&nbsp;</TH><TH>");

		// // System.out.println("\n#### PAGE ####\n" + pathFile + "\n" + xmlPage + "\n#### END PAGE ####\n");
		// } else
		// return;

		/* *******************************************************
		 * Read HTML and extract all tables from the file
		 * ******************************************************
		 */
		// Extract the a list of <TABLE> tags from the page
		String pageTitle = pathFile.substring(sourceFullPath.length(), pathFile.length() - 5);
		ArrayList<Element> tableList = new ArrayList<Element>();
		Page page = new Page(new URL("http://www.example.com/"), xmlPage); // doc.asXML()
		tableList = new TableExtractor(logger).ExtractLeafTables(page);

		outLog.write("title=" + pageTitle + "\n");

		// Clean paths
		if (pathFile.endsWith(".html"))
			pathFile = pathFile.substring(0, pathFile.length() - 5);
		if (pathFile.startsWith("/wiki/")) // for the web cases
			pathFile = pathFile.substring(6);
		if (pathFile.startsWith(LocationConstants.WIKI_HTML)) // for direct files
		{
			String[] split = pathFile.split("/");
			pathFile = split[split.length - 1];
		}

		// Iterate over all HTML tables
		Iterator<Element> iter = tableList.iterator();
		int tableCounter = 0; // tables counter
		// _log.info("This page contains " + tableList.size() + " tables");

		Element tableEle = null;
		while (iter.hasNext())
		{
			tableCounter++; // changelog: moved to count only the well-formed
			tableEle = iter.next();

			// Parse an transform the table into a matrix
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

					// // ***** has caption?
					// if (!extractor.getCaption().trim().isEmpty())
					// {
					// wtableDto.setCaption(cl.em.utils.string.StringUtils.javaEscape(extractor.getCaption().trim()));
					// }

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
					outLog.write("column-headers=" + tHeaders + "\n");
					// System.out.println(tHeaders);

					// if (tHeaders != null)
					// for (THashSet<Resource> headerProperty : tHeaders)
					// {
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

					// outLog.write("\n** Table body **");
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
						// outLog.write("\n### ROW-" + rowCount + " ###\n");
						sameRowCells = new ArrayList<TableCell>();
						rowResources = access.getRowResources(thisTableExtractor, rowCount, CellType.TBODY);
						// System.out.println(rowResources);
						realCountArray[rowCount] = access.getRealColNumber();

						for (CellResources colCellResources : rowResources)
						{
							THashSet<Resource> cellResources = colCellResources.getResoSet();
							// outLog.write(cellResources.toString());
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
						 * Identifying relations between elements in the same row
						 * *******************************************************
						 */
						rowRelations = sparql.getDBpediaRelationsCell(sameRowCells);
						// outLog.write("\n" + rowRelations.toString());
						tableRowRelations.addAll(rowRelations);
					} // endFor each row

					// get number of resources for each column
					int[] resourcePerColumn = new int[columnSize];
					for (int rowIndex = 0; rowIndex < matrixTableCell.size(); rowIndex++)
					{
						for (TableCell cell : matrixTableCell.get(rowIndex))
						{
							// System.out.println(cell.getResource());
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

					// System.out.println(StringUtils.toString(resourcePerColumn));

					/*
					 * =====================================================================================
					 * At this point we have the extracted relation per row, based on the present resources.
					 * =====================================================================================
					 */
					// Listing the novel and old triples
					// List<Triple> oldTriples = new ArrayList<Triple>(); // used in outLog
					Triple triple = null;
					Resource mainEntity = null;

					/* *******************************************************************************************
					 * Identifying relations between the main resource (page) and the resources within the table
					 * *******************************************************************************************
					 */
					List<TableCell> tableResources = new ArrayList<TableCell>();
					mainEntity = sparql.getResourceURI(pageTitle, EntityType.OBJECT);

					TableCell mainEntityCell = new TableCell(mainEntity, -1, -1);
					tableResources.add(mainEntityCell); // add article entity
					tableResources.addAll(allTableCell); // add all found resources

					// ===========================================================
					// IMPORTANT: Add the relations extracted from article title
					List<TableRelation> tableRelations = sparql.getDBpediaMainRelations(tableResources);
					tableRowRelations.addAll(tableRelations);
					// ===========================================================

					// outLog.write("\n** Relations in rows **\n");
					// create an instance for ranking
					rank = new RankRelation(tableRowRelations);

					/* *******************************
					 * Ranking function for relations
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
					// System.out.println("Table-filtered@" + threshold + "\t" + pageTitle + "\t#" + numbFoundRelations
					// + " relations\t" + rank.getFilteredRelations());
					outLog.write("relations=" + rank.getFilteredRelations() + "\n");

					// For each of the rows in the matrix
					SortedMap<Integer, String> outcome = null;
					TableRelation relation_i = null;
					String[] headers = null;
					PredicateStat pred = null;
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
										invResources[0] = pageTitle;
									else if (cell1.getCol() == col1)
										invResources[0] = cell1.getResource().getURI();
									for (TableCell cell2 : matrixTableCell.get(rowIndex))
									{

										if (cell2.getCol() != -1 && !cell2.getResource().isEmpty())
										{
											if (col2 == -1)
												invResources[1] = pageTitle;
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
						List<TableCell> rowCell = matrixTableCell.get(rowCount); // once
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
							// System.out.println((String) entry.getEntry().getKey() + "\t"
							// + rank.uniqueRelations((String) entry.getEntry().getKey()));

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
							outcome.put(4, String.valueOf(columnSize));
							// ===== feature #5*
							outcome.put(5, String.valueOf(realCountArray[rowCount + startRow]));
							// ===== feature #6*
							outcome.put(6, String.valueOf(MathUtils.divide(rowSize, columnSize,
									LocationConstants.DECIMAL_PLACE)));
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
							// ===== feature #9*
							if (relation_i.getCol1() != -1)
								outcome.put(9, String.valueOf(resourcePerColumn[relation_i.getCol1()]));
							else
								outcome.put(9, String.valueOf(-1.0)); // ===== feature
							// ===== feature #10*
							if (relation_i.getCol2() != -1)
								outcome.put(10, String.valueOf(resourcePerColumn[relation_i.getCol2()]));
							else
								outcome.put(10, String.valueOf(-1.0));
							// ===== feature #11*
							if (relation_i.getCol1() != -1 && relation_i.getCol2() != -1)
								outcome.put(11, String.valueOf(MathUtils.divide(
										resourcePerColumn[relation_i.getCol1()],
										resourcePerColumn[relation_i.getCol2()], LocationConstants.DECIMAL_PLACE)));
							else
								outcome.put(11, String.valueOf(0.0));
							// ===== feature #12*
							outcome.put(12,
									String.valueOf(potRelSumMap.get(relation_i.getCol1() + "#" + relation_i.getCol2())));
							// ===== feature #13*
							outcome.put(13, String.valueOf(relation_i.getScore().getPotentialRel()));
							// ===== feature #14*
							outcome.put(14, String.valueOf(relation_i.getScore().getMaxPotentialRel()));
							// ===== feature #15*
							outcome.put(15, String.valueOf(relation_i.getScore().getScore()));

							int rowsHold = 0; // number of rows in which the relation holds
							if (relation_i.getCellRelation() != null
									&& relation_i.getCellRelation().getRelation() != null
									&& !relation_i.getCellRelation().getRelation().isEmpty())
							{
								rowsHold = rank.freqRelationRows((String) entry.getEntry().getKey());
								// System.out.println("REL=" + (String) entry.getEntry().getKey() + "\t" + " HOLDS="
								// + rowsHold);
							}

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

									// check if the knowledge-base already have this triple
									boolean exists = sparql.existsTriple(triple);
									float similaritySub = 0.0f, similarityObj = 0.0f; // , max_simi = 0.0f;

									// ===== feature -#7- #16*
									if (relation_i.getCol1() == -1)
									{
										// "##PAGE_TITLE##"
										headers[0] = null;
										outcome.put(16, String.valueOf(-1.0));
									} else if (tHeaders != null && tHeaders.size() > 0)
									{
										try
										{
											for (TableHeader header : tHeaders)
											{
												if (header.gethColumn() == relation_i.getCol1())
													headers[0] = header.gethText();
											}
											String[] urlSplit = triple.getRelation().getURI().split("/");
											similaritySub = Similarity.distanceStr(headers[0].toLowerCase(),
													urlSplit[urlSplit.length - 1]);
											// System.out.println("SIMILARITY=" + similarity + "\t" + reso.getURI()
											// + "\t" + triple.getRelation().getURI());
										} catch (Exception ex)
										{
											headers[0] = null;
										}
										outcome.put(16, String.valueOf(similaritySub));
									} else
									{
										// "##NO_HEADER##"
										outcome.put(16, String.valueOf(0.0));
									}

									// ===== feature -#8- #17*
									if (relation_i.getCol2() == -1)
									{
										// "##PAGE_TITLE##"
										headers[1] = null;
										outcome.put(17, String.valueOf(-1.0));
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
										outcome.put(17, String.valueOf(similarityObj));
									} else
									{
										// "##NO_HEADER##"
										outcome.put(17, String.valueOf(0.0));
									}

									// ===== feature #18* -- maximum similarity
									if (similaritySub > similarityObj)
										outcome.put(18, String.valueOf(similaritySub));
									else
										outcome.put(18, String.valueOf(similarityObj));

									// split[3] contains the source for the tiple. b for body and h for main entity
									String[] split = ((String) entry.getEntry().getKey()).split("#");

									// ===== feature -#9- #19*
									if (!exists)
										outcome.put(19, "1");
									else
										outcome.put(19, "2");

									// ===== feature -#10- #20*
									if (split[3].equals("h"))
										outcome.put(20, String.valueOf(1));
									else
										outcome.put(20, String.valueOf(2));

									// ========= feature -#11- #21*
									outcome.put(21, String.valueOf(rowsHold));

									pred = predicates.get("<" + split[0] + ">");

									// ===== feature #22* -- frequency of this relation in the KB
									if (pred != null)
										outcome.put(22, String.valueOf(pred.getTimes()));
									else
										outcome.put(22, String.valueOf(0));

									// ===== feature -#12- #23*
									// get predicate statistics
									if (pred != null)
									{
										double commonness = (Double) (1 / (1 + Math.abs(Math.log10(MathUtils.divide(
												pred.getTimes(), LocationConstants.PRED_MAX_INSTA,
												LocationConstants.DECIMAL_PLACE)))));
										outcome.put(23, String.valueOf(MathUtils.round(commonness,
												LocationConstants.DECIMAL_PLACE)));
										// outcome.put(21, String.valueOf(MathUtils.divide(pred.getTimes(),
										// LocationConstants.PRED_MAX_INSTA, LocationConstants.DECIMAL_PLACE)));
									} else
										outcome.put(23, String.valueOf(0.0));

									// ===== feature -#13- #24*
									// ratio different subjects for this predicate
									if (pred != null)
									{
										double ratio = (Double) (1 / (1 + Math.abs(Math.log10(MathUtils.divide(
												pred.getUniqSubj(), LocationConstants.PRED_MAX_SUBJ,
												LocationConstants.DECIMAL_PLACE)))));
										// outcome.put(22, String.valueOf(MathUtils.divide(pred.getUniqSubj(),
										// LocationConstants.PRED_MAX_SUBJ, LocationConstants.DECIMAL_PLACE)));
										outcome.put(24,
												String.valueOf(MathUtils.round(ratio, LocationConstants.DECIMAL_PLACE)));
									} else
										outcome.put(24, String.valueOf(0.0));

									// ===== feature -#14- #25*
									if (pred != null)
									{
										double ratio = (Double) (1 / (1 + Math.abs(Math.log10(MathUtils.divide(
												pred.getUniqObj(), LocationConstants.PRED_MAX_OBJ,
												LocationConstants.DECIMAL_PLACE)))));
										// outcome.put(23, String.valueOf(MathUtils.divide(pred.getUniqObj(),
										// LocationConstants.PRED_MAX_OBJ, LocationConstants.DECIMAL_PLACE)));
										outcome.put(25,
												String.valueOf(MathUtils.round(ratio, LocationConstants.DECIMAL_PLACE)));
									} else
										outcome.put(25, String.valueOf(0.0));

									// ===== feature -#15- #26*
									if (pred != null)
										outcome.put(26, String.valueOf(MathUtils.divide(pred.getUniqSubj(),
												pred.getUniqObj(), LocationConstants.DECIMAL_PLACE)));
									else
										outcome.put(26, String.valueOf(0.0));

									// Features for column 1
									// System.out.println(relation_i.getCellRelation().getRelationURI());
									int[] featCol1 = access.getInnerCellContent(thisTableExtractor,
											rowCount + startRow, relation_i.getCol1(), CellType.TBODY);
									// ===== feature -#16- #27* -- how many resources col1
									outcome.put(27, String.valueOf(featCol1[0]));
									// ===== feature -#17- #28* -- length col1
									outcome.put(28, String.valueOf(featCol1[1]));
									// ===== feature -#18- #29* -- bullets col1
									outcome.put(29, String.valueOf(featCol1[2]));
									// ===== feature #30* -- has format tags
									outcome.put(30, String.valueOf(featCol1[3]));
									// ===== feature #31* -- has break lines
									outcome.put(31, String.valueOf(featCol1[4]));

									// Features for column 2
									int[] featCol2 = access.getInnerCellContent(thisTableExtractor,
											rowCount + startRow, relation_i.getCol2(), CellType.TBODY);
									// ===== feature -#19- #32* -- how many resources col2
									outcome.put(32, String.valueOf(featCol2[0]));
									// ===== feature -#20- #33* -- length col2
									outcome.put(33, String.valueOf(featCol2[1]));
									// ===== feature -#31- #34* -- bullets col2
									outcome.put(34, String.valueOf(featCol2[2]));
									// ===== feature #35* -- has format tags
									outcome.put(35, String.valueOf(featCol2[3]));
									// ===== feature #36* -- has break lines
									outcome.put(36, String.valueOf(featCol2[4]));

									// ===== feature -#22- #37* -- frequency of the relation in the table
									outcome.put(37, String.valueOf((MathUtils.divide(rowsHold, rowSize,
											LocationConstants.DECIMAL_PLACE))));
									// ===== feature #38* -- how many relations we found for this table
									outcome.put(38, String.valueOf(numbFoundRelations));
									// ===== feature #39* -- ratio rows held divided by number of relations
									outcome.put(39, String.valueOf(MathUtils.divide(rowsHold, numbFoundRelations,
											LocationConstants.DECIMAL_PLACE)));
									// ===== feature #40* -- multiplicity ratio for cells
									if (featCol2[0] > 0)
										outcome.put(40, String.valueOf(MathUtils.divide(featCol1[0], featCol2[0],
												LocationConstants.DECIMAL_PLACE)));
									else
										outcome.put(40, String.valueOf(0.0));
									// ===== feature #41* --- unique potential relations across rows
									outcome.put(41, String.valueOf(_uniquePRM.intValue()));
									// ===== feature #42* --- unique relations held
									outcome.put(42, String.valueOf(_uniqueRM.intValue()));
									// ===== feature #43* --- potential unique relations
									outcome.put(
											43,
											String.valueOf(potUniqRelMap.get(relation_i.getCol1() + "#"
													+ relation_i.getCol2())));
									// ===== feature #44* --- unique number of resource in subject column
									if (relation_i.getCol1() == -1)
										outcome.put(44, String.valueOf(-1));
									else
										outcome.put(44, String.valueOf(uniqResourcePerColumn[relation_i.getCol1()]));
									// ===== feature #45* --- unique number of resource in object column
									if (relation_i.getCol2() == -1)
										outcome.put(45, String.valueOf(-1));
									else
										outcome.put(45, String.valueOf(uniqResourcePerColumn[relation_i.getCol2()]));

									// END OF FEATURES - PRINT FEATURES
									if (headers[0] == null)
										headers[0] = new String("##TITLE##");
									if (headers[1] == null)
										headers[1] = new String("##TITLE##");

									if (!triple.toString().isEmpty())
										outTriple.write("+1 " + ParseTriplesOutput.featureVectorToString(outcome)
												+ " # " + pageTitle + " " + triple.toString() + " " + headers[0]
												+ "#$#$#" + headers[1] + "\n");

									// Print new triples and store old
									// if (exists)
									// {
									// if (!triple.toString().isEmpty())
									// oldTriples.add(triple);
									// } else
									// outLog.write("\n" + triple.toString());

									// if (!exists)
									// {
									// // System.out.println(triple.toString());
									// outLog.write("\n" + triple.toString());
									// if (!triple.toString().isEmpty())
									// outTriple.write(pageTitle + "\t" + tablesList.size() + "\t" + tableCounter + "\t"
									// + rowSize + "\t" + columnSize + "\t" + relation_i.getCol1() + "\t"
									// + relation_i.getCol2() + "\t" + header1 + "\t" + header2 + "\t"
									// + rowCell.size() + "\t" + "n\t" + split[3] + "\t" + triple.toString()
									// + "\t" + rowsHold + "\t" + relation_i.getScore() + "\n");
									// } else
									// {
									// oldTriples.add(triple);
									// if (!triple.toString().isEmpty())
									// outTriple.write(pageTitle + "\t" + tablesList.size() + "\t" + tableCounter + "\t"
									// + rowSize + "\t" + columnSize + "\t" + relation_i.getCol1() + "\t"
									// + relation_i.getCol2() + "\t" + header1 + "\t" + header2 + "\t"
									// + rowCell.size() + "\t" + "o\t" + split[3] + "\t" + triple.toString()
									// + "\t" + rowsHold + "\t" + relation_i.getScore() + "\n");
									// }
								}
							}
						}
					}

					// Show triples that DBpedia already know
					// outLog.write("\n** Old triples **");
					// for (Triple _triple : oldTriples)
					// {
					// outLog.write("\n" + _triple.toString());
					// }

				} // else it's not-useful
			} // else it's a ill-formed table
			MemoryUtils.freeMemory();
			// outLog.flush();
			outTriple.flush();
		} // endwhile (iter.hasNext())
	}

	/**
	 * Main
	 * 
	 * @param args
	 * @throws Exception
	 */
	public static void main(String[] args) throws Exception
	{
		DOMConfigurator.configure("./conf/log4j.xml");

		// Read parameter split name which contains the set of HTML files
		if (args.length < 2)
		{
			System.err.println("Enter the split name and machine name. e.g., \"split_1 deri-srvgal20\"");
			System.exit(0);
		}
		// String spName = "split_27";
		// String machine = "local";
		String spName = args[0];
		String machine = args[1];

		split = spName + "/";
		outputFilename = "./triples-" + spName + ".out";
		// _log.fatal("Starting execution of WikiTablesTriples for " + split + " on machine " + machine);

		// test, local, server
		Properties props = new Properties();
		props.put("SERVER_FOLDER", args[2]);
		// add other properties
		WikiTableTriples triples = new WikiTableTriples(props, machine);

		// put an URL to analyze directly from Wikipedia
		// Leader_of_the_Opposition_(Prince_Edward_Island)
		// Clem_Jones_Tunnel
		// Saskatchewan_general_election,_1929
		// Chris_Vermeulen
		// KJEE
		// Bob_Anderson_(racing_driver)
		// String article = "KJEE";
		String article = "";
		try
		{
			triples.init(article);
		} catch (Exception e)
		{
			_log.error("Error in WikiTablesTriples for " + split + " on machine " + machine + "\nException: "
					+ e.getMessage());
			e.printStackTrace();
		}
		// _log.info("Finished execution of WikiTablesStat for " + split);
		// _log.fatal("Finished successfuly execution of WikiTablesTriples for " + split + " on machine " + machine);
	}
}
