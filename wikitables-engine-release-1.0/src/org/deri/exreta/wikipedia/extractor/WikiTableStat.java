package org.deri.exreta.wikipedia.extractor;

import gnu.trove.set.hash.THashSet;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.apache.log4j.xml.DOMConfigurator;
import org.cyberneko.html.parsers.SAXParser;
import org.deri.exreta.dal.connection.thread.LoadURLHandler;
import org.deri.exreta.dal.main.LocationConstants;
import org.deri.exreta.wikipedia.parser.dto.WikiTableDTO;
import org.dom4j.Document;
import org.dom4j.io.SAXReader;
import org.jsoup.Jsoup;

import tartar.table_flattening.TableExtractor;
import websphinx.Element;
import websphinx.Page;
import websphinx.Tag;
import cl.em.utils.files.DirectoryUtils;
import cl.em.utils.performance.MemoryUtils;
import cl.em.utils.performance.TimeWatch;
import cl.yahoo.webtables.features.FeaturesExtractorYData;

/**
 * Class in charge of extract features for each table in a given set of Wikipedia articles.
 * 
 * @author Emir Muñoz <emir@emunoz.org>
 * @version 2.2
 * 
 */
public class WikiTableStat
{
	private static final Logger					_log		= Logger.getLogger(WikiTableStat.class);
	private static final tartar.common.Logger	logger		= new tartar.common.Logger();
	private static THashSet<String>				articles	= new THashSet<String>();
	private static THashSet<String>				pathFileSet;
	private FeaturesExtractorYData				extractor;
	private WikiTableAccess						access;
	private static String						outputFilename;
	private static String						fullPath;
	private static String						split;
	private BufferedWriter						outLog;

	public void init() throws Exception
	{
		// put an URL to analyze directly from Wikipedia
		String article = "";
		// Case #1: work with some split
		if (article.isEmpty())
		{
			fullPath = LocationConstants.WIKI_HTML + split + "dump/";
			pathFileSet = DirectoryUtils.getFilesList(fullPath, ".html");
			_log.info("Reading " + pathFileSet.size() + " HTML files from " + fullPath);
		} else
			// Case #2: work directly with an Wikipedia article
			_log.info("Reading article: \"" + article + "\" from Wikipedia");

		// Output log file
		FileWriter fstream = new FileWriter(outputFilename);
		outLog = new BufferedWriter(fstream);

		// Input HTML file
		File file = null;
		InputStream url = null;

		// Instantiate a Date object
		TimeWatch watch = TimeWatch.start();

		int count = 0;

		_log.debug("Processing files now:\n");
		if (article.isEmpty())
		{
			// for each html found in the folder
			for (String pathFile : pathFileSet)
			{
				if (++count <= 100)
					System.out.print(".");
				else
				{
					System.out.println(".");
					count = 0;
				}

				if (articles.contains(pathFile))
					continue;

				// Reading and parsing the HTML file
				// with NekoHTML we solve some ill-formed problems
				file = new File(pathFile);
				url = new FileInputStream(file);
				// Parsing HTML as XML file
				SAXParser xmlReader = new org.cyberneko.html.parsers.SAXParser();
				SAXReader reader = new SAXReader(xmlReader);
				Document wikiDoc = reader.read(url);
				// Read HTML tables
				readHTMLFiles(wikiDoc, pathFile);
			}
		} else
		{
			// Read the article from a given URL
			String root = "http://en.wikipedia.org/wiki/";
			LoadURLHandler load = new LoadURLHandler();
			InputStream page = load.downloadURL(root + article, 10);
			// Parsing HTML as XML file
			SAXParser xmlReader = new org.cyberneko.html.parsers.SAXParser();
			SAXReader reader = new SAXReader(xmlReader);
			Document wikiDoc = reader.read(page);
			// Read HTML tables
			readHTMLFiles(wikiDoc, LocationConstants.WIKI_HTML + split + article + ".html");
		}

		System.err.println("\nFinished! See files " + outputFilename + " and " + outputFilename + ".triple " + "Time: "
				+ watch.elapsedTime());

		outLog.flush();
		outLog.close();
		fstream.close();
	}

	/**
	 * Function to read an HTML file, extract their tables, and discover triples.
	 * 
	 * @param wikiDoc HTML document.
	 * @param pathFile Path to the file.
	 * @throws Exception
	 */
	public void readHTMLFiles(Document wikiDoc, String pathFile) throws Exception
	{
		extractor = null;
		access = null;
		// Cerebral_arteriovenous_malformation OK
		// Roman_censor OK
		// Chechnya OK
		// Endomorphism OK
		// Economy_of_Chad Not well formed
		// Control_character OK
		// List_of_animated_television_series OK
		// David_Lynch OK
		// Ally_McBeal
		// Alps
		String xmlPage = "";
		// Dublin.html
		// if (pathFile.endsWith("/Angolan_Armed_Forces.html") || pathFile.endsWith("/Dublin.html")
		// || pathFile.endsWith("/Electronegativity.html") || pathFile.endsWith("/Colorado.html") ||
		// pathFile.endsWith("/Antimony.html") || pathFile.endsWith("/Military_of_Burundi.html") ||
		// pathFile.endsWith("/Alkaloid.html") || pathFile.endsWith("/Andalusia.html") ||
		// pathFile.endsWith("/A.S._Roma.html") || pathFile.endsWith("/American_Football_League.html") ||
		// pathFile.endsWith("/Doctor_Who.html") || pathFile.endsWith("/Actinide.html") ||
		// pathFile.endsWith("/Foreign_relations_of_Belarus.html") ||
		// pathFile.endsWith("/Anatomical_Therapeutic_Chemical_Classification_System.html") ||
		// pathFile.endsWith("/Dayton'2C_Ohio.html") || pathFile.endsWith("/Brabham.html") ||
		// pathFile.endsWith("/Baltimore_Ravens.html") || pathFile.endsWith("/Commandant_of_the_Marine_Corps.html") ||
		// pathFile.endsWith("/AFC_Ajax.html") || pathFile.endsWith("/Electromagnetism.html") ||
		// pathFile.endsWith("/Arizona_Cardinals.html") ||
		// pathFile.endsWith("/Chief_Minister_of_the_Northern_Territory.html") ||
		// pathFile.endsWith("/Electromagnetic_field.html") || pathFile.endsWith("/Discus_throw.html"))
		// if (pathFile.endsWith("/Dayton'2C_Ohio.html"))
		// {
		// Processing the file to extract the web-tables
		xmlPage = wikiDoc.asXML().replace("<TD/>", "<TD>&nbsp;</TD>\n")
				.replaceAll("<TD>[\\s|\n]*</TD>", "<TD>&nbsp;</TD>\n")
				.replaceAll("<TD ALIGN=\"RIGHT\">\\s*</TD>", "<TD ALIGN=\"RIGHT\">&nbsp;</TD>\n")
				.replaceAll("<TH>[\\s|\n]*!", "<TH>&nbsp;</TH><TH>");

		// System.out.println("\n#### PAGE ####\n" + pathFile + "\n" + xmlPage + "\n#### END PAGE ####\n");
		// } else
		// return;

		/* *******************************************************
		 * Read HTML and extract all tables from the file
		 * ******************************************************
		 */
		// Extract the a list of <TABLE> tags from the page
		String pageTitle = pathFile.substring(fullPath.length(), pathFile.length() - 5);
		Page page = new Page(new URL("http://www.example.com/"), xmlPage); // doc.asXML()
		// System.out.println(page.getLength() + "\t" + page.toHTML());
		ArrayList<Element> list = new ArrayList<Element>();
		list = new TableExtractor(logger).ExtractLeafTables(page);

		THashSet<String> resourceSet = null; // to save found resources
		WikiTableDTO wtableDto = null; // to save the features extracted

		// Iterate over the whole list of found tables
		Iterator<Element> iter = list.iterator();
		while (iter.hasNext())
		{
			Element tableEle = iter.next();

			extractor = new FeaturesExtractorYData();
			access = new WikiTableAccess(null);

			int nExternalLinks = 0;
			int nFirstColLinks = 0;
			int nSecondColLinks = 0;
			int nSamePageLinks = 0;

			// Parse an transform the table into a matrix
			int status = extractor.setTable(tableEle);

			// If the table is well-formed
			if (status == 1)
			{
				wtableDto = new WikiTableDTO();
				resourceSet = new THashSet<String>(); // dbpedia resources set

				// Tables whose dimension is 2x2 or higher ==> it's a useful table
				if (extractor.getColumns() >= 2 && extractor.getRows() >= 2)
				{
					// Discard toc, infobox, metadata and navbox tables in the page
					Tag tag = tableEle.getStartTag();
					if (tag.getHTMLAttribute("class") != null
							&& (tag.getHTMLAttribute("class").equalsIgnoreCase("toc")
									|| tag.getHTMLAttribute("class").equals("navbox")
									|| tag.getHTMLAttribute("class").contains("infobox")
									|| tag.getHTMLAttribute("class").contains("metadata")
									|| tag.getHTMLAttribute("class").contains("maptable") || tag.getHTMLAttribute(
									"class").contains("vcard")))
					{
						// It's other type of table
						continue;
					}
					// ***** wikipage title
					wtableDto.setTitle(pageTitle);

					// ***** has caption?
					if (!extractor.getCaption().trim().isEmpty())
					{
						wtableDto.setCaption(cl.em.utils.string.StringUtils.javaEscape(extractor.getCaption().trim()));
					}

					// ***** number of columns
					int columnSize = extractor.getColumns();
					wtableDto.setnColumns(columnSize);
					// ***** number of rows
					int rowSize = extractor.getRows();
					wtableDto.setnRows(rowSize); // counting the header if there is one

					/*
					 * ******************************************************
					 * Here we can work with the table as a matrix
					 * ******************************************************
					 */
					// List<THashSet<Resource>> tHeaders = this.getColumnHeaders(extractor);
					// if (tHeaders != null && tHeaders.size() > 0)
					int headersRow = access.headersRow(extractor);
					wtableDto.setHeadersRow(headersRow);

					// List<String> columnValues;
					// Traverse the matrix collecting the found resources
					Element eleChild = null;
					org.jsoup.nodes.Document docu = null;
					for (int col = 0; col < columnSize; col++)
					{
						// Initialize the set of resources
						// columnValues = new ArrayList<String>();
						for (int row = 0; row < rowSize; row++)
						{
							eleChild = extractor.getHTMLElement(col, row);

							/* Extract all links with jsoup */
							// Extract all the <a href...> elements
							try
							{
								docu = (org.jsoup.nodes.Document) Jsoup.parse(eleChild.toHTML());
								for (org.jsoup.nodes.Element link : docu.select("a"))
								{
									if (link != null)
									{
										// ***** number of first column links
										if (col == 0)
											nFirstColLinks++;
										// ***** number of second column links
										else if (col == 1)
											nSecondColLinks++;

										// ***** one external link
										if (link.hasClass("externallink"))
										{
											nExternalLinks++;
											continue; // skip
										}

										String linkHref = access.filterLink(link.attr("href")); // "http://example.com/"

										// ***** has links to the same page
										if (!StringUtils.isEmpty(linkHref)
												&& linkHref.length() < LocationConstants.MAX_LENGTH_TXT)
											resourceSet.add(linkHref);
										else
											nSamePageLinks++;
									}
								}
							} catch (Exception e)
							{
								_log.warn("Error getting HTML content");
							}
						} // endFor rows
					} // endFor columns

					// set the properties for the analyzed table
					wtableDto.setExternarLinks(nExternalLinks);
					wtableDto.setFirstColumnLinks(nFirstColLinks);
					wtableDto.setSecondColumnLinks(nSecondColLinks);
					wtableDto.setSamePageLinks(nSamePageLinks);

					if (resourceSet.size() != 0)
					{
						// ***** set of anchor text
						wtableDto.setAnchors(resourceSet);
						// System.out.println(resourceSet.toString());
					}
				} // else it's a small dimension table
			} // else it's a ill-formed table

			if (wtableDto != null && !wtableDto.isEmpty())
				outLog.write(wtableDto.toString());

			MemoryUtils.freeMemory();
			outLog.flush();
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
		// String spName = "split_1";
		// String machine = "local";
		String spName = args[0];
		String machine = args[1];

		split = spName + "/";
		outputFilename = "./stat-" + spName + ".out";
		_log.fatal("Starting execution of WikiTablesStat for " + spName + " on machine " + machine);

		// String mainDirectory = split;
		// _log.info("Starting execution of WikiTablesStat for " + split);
		// fullPath = mainDirectory + "dump/";
		// String[] pathOutput = split.split("/");

		// outputFilename = outputFilename.replace(".out", "_" + pathOutput[pathOutput.length - 1] + ".out");
		// pathFileSet = DirectoryUtils.getFilesList(fullPath, ".html");

		// System.out.println(pathFileSet.size() + " HTML files readed from " + fullPath);

		WikiTableStat triples = new WikiTableStat();
		try
		{
			triples.init();
		} catch (Exception e)
		{
			_log.info("Error in WikiTablesStat for " + split + " on machine " + machine + "\nException: "
					+ e.getMessage());
			e.printStackTrace();
		}
		_log.info("Finished execution of WikiTablesStat for " + split + " on machine " + machine);
		_log.fatal("Finished execution of WikiTablesStat for " + split + " on machine " + machine);
	}
}
