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
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;
import org.apache.log4j.xml.DOMConfigurator;
import org.cyberneko.html.parsers.SAXParser;
import org.dom4j.Document;
import org.dom4j.io.SAXReader;

import tartar.table_flattening.TableExtractor;
import websphinx.Element;
import websphinx.Page;
import websphinx.Tag;
import cl.em.utils.files.DirectoryUtils;
import cl.em.utils.performance.TimeWatch;
import cl.yahoo.webtables.features.FeaturesExtractorYData;

/**
 * Class in charge of extract the general information of tables in a Wikipedia article.
 * 
 * @author Emir Muñoz <emir@emunoz.org>
 * @version 2.2
 */
public class WikiTableVector
{
	private static String		outputFileVector	= "./vector.out";
	private static final Logger	_log				= Logger.getLogger(WikiTableVector.class);

	public static void main(String[] args) throws Exception
	{
		DOMConfigurator.configure("./conf/log4j.xml");
		tartar.common.Logger logger = new tartar.common.Logger();

		FeaturesExtractorYData extractor = null;
		File file = null;
		InputStream url = null;

		if (args.length < 1)
		{
			System.err.println("Please enter the name of split folder.");
			System.exit(0);
		}

		String thisSplit = args[0]; // there are 27 splits
		_log.info("Starting execution of WikiTablesVector for " + thisSplit);
		// String mainDirectory = LocationConstants.WIKI_HTML + split + "/";
		if (!thisSplit.endsWith("/"))
			thisSplit = thisSplit + "/";
		String mainDirectory = thisSplit;
		// We list all the *.html files from the folder split_x/dump/
		THashSet<String> pathFileSet = DirectoryUtils.getFilesList(mainDirectory + "dump/", ".html");
		System.out.println(pathFileSet.size() + " HTML files readed from " + mainDirectory + "dump/");

		// Declare output file
		String[] pathOutput = thisSplit.split("/");
		outputFileVector = outputFileVector.replace(".out", "_" + pathOutput[pathOutput.length - 1] + ".out");
		FileWriter fstream = new FileWriter(outputFileVector);
		BufferedWriter out = new BufferedWriter(fstream);
		FileWriter fstream2 = new FileWriter(outputFileVector + ".not-well-formed");
		BufferedWriter out2 = new BufferedWriter(fstream2);

		// Instantiate a Date object
		TimeWatch watch = TimeWatch.start();

		System.out.println("Processing files");
		int count = 0;
		// int tableCounter = 1;

		StringBuilder reportLine = new StringBuilder();
		// For all the *.html files readed
		for (String pathFile : pathFileSet)
		{
			reportLine = new StringBuilder();
			if (++count <= 100)
				System.out.print(".");
			else
			{
				System.out.println(".");
				count = 0;
			}
			// Reading the HTML file
			file = new File(pathFile);
			url = new FileInputStream(file);

			SAXParser xmlReader = new org.cyberneko.html.parsers.SAXParser();
			SAXReader reader = new SAXReader(xmlReader);
			Document wikiDoc = reader.read(url);

			String xmlPage = "";
			// if (pathFile.contains("Earth"))
			// {
			// Processing the file to extract the web-tables
			xmlPage = wikiDoc.asXML().replace("<TD/>", "<TD>&nbsp;</TD>\n")
					.replaceAll("<TD>[\\s|\n]*</TD>", "<TD>&nbsp;</TD>\n")
					.replaceAll("<TD ALIGN=\"RIGHT\">[\\s|\n]*</TD>", "<TD ALIGN=\"RIGHT\">&nbsp;</TD>\n")
					.replaceAll("<TH>[\\s|\n]*!", "<TH>&nbsp;</TH><TH>");
			// System.out.println(xmlPage);
			// }

			Page page = new Page(new URL("http://www.example.com/"), xmlPage); // doc.asXML()
			// System.out.println(page.getLength() + "\t" + page.toHTML());
			ArrayList<Element> list = new ArrayList<Element>();
			list = new TableExtractor(logger).ExtractLeafTables(page);
			String thisArticleName = pathFile.replace(thisSplit, "");
			thisArticleName = thisArticleName.substring(5, thisArticleName.length() - 5);

			int usefulTables = 0;
			int wellFormedTables = 0;
			int notWellFormedTables = 0;
			int tocTables = 0;
			int infoboxTables = 0;
			int smallDimTables = 0;
			int otherTables = 0;
			Iterator<Element> iter = list.iterator();
			while (iter.hasNext())
			{
				Element tableEle = iter.next();
				// System.out.println(tableEle.toHTML());
				extractor = new FeaturesExtractorYData();
				int status = extractor.setTable(tableEle);
				if (status == 1)
				{
					wellFormedTables++;
					// ToC tables
					Tag tag = tableEle.getStartTag();
					if (tag.getHTMLAttribute("class") != null
							&& (tag.getHTMLAttribute("class").equalsIgnoreCase("toc")
									|| tag.getHTMLAttribute("class").contains("toc") || tag.getHTMLAttribute("class")
									.contains("TOC")))
					{
						tocTables++;
						continue;
					}
					// Infobox tables
					if (tag.getHTMLAttribute("class") != null
							&& (tag.getHTMLAttribute("class").equalsIgnoreCase("infobox")
									|| tag.getHTMLAttribute("class").contains("infobox")
									|| tag.getHTMLAttribute("class").contains("Infobox") || tag.getHTMLAttribute(
									"class").contains("InfoBox")))
					{
						infoboxTables++;
						continue;
					}
					if (tag.getHTMLAttribute("class") != null
							&& (tag.getHTMLAttribute("class").equals("navbox")
									|| tag.getHTMLAttribute("class").contains("metadata")
									|| tag.getHTMLAttribute("class").contains("maptable") || tag.getHTMLAttribute(
									"class").contains("vcard")))
					{
						otherTables++;
						continue;
					}
					// Tables whose dimension are smaller than 2x2
					if (extractor.getColumns() < 2 || extractor.getRows() < 2)
					{
						smallDimTables++;
						continue;
					}
					// Tables whose dimension is 2x2 or higher
					if (extractor.getColumns() >= 2 && extractor.getRows() >= 2)
					{
						usefulTables++;
						continue;
					}
					otherTables++;
				} else
				{
					notWellFormedTables++;
					out2.write("### " + thisArticleName + " ###\n" + tableEle.toString() + "\n\n");
				}
			}

			reportLine.append(thisArticleName + "\t" + list.size() + "\t" + wellFormedTables + "\t"
					+ notWellFormedTables + "\t" + tocTables + "\t" + infoboxTables + "\t" + smallDimTables + "\t"
					+ otherTables + "\t" + usefulTables + "\n");
			out.write(reportLine.toString());
		}
		out.close();
		out2.close();

		long passedTimeInSeconds = watch.time(TimeUnit.SECONDS);
		System.out.println("");
		System.out.println(pathFileSet.size() + " HTML files readed from " + mainDirectory + "dump/ in "
				+ passedTimeInSeconds + " seconds");
		System.out.println(watch.elapsedTime());
		System.out.println("Finished! See the file " + outputFileVector);
		_log.info("Finished execution of WikiTablesVector of " + thisSplit);
	}
}
