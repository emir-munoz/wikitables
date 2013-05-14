package org.deri.exreta.wikipedia.parser.test;

import gnu.trove.set.hash.THashSet;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;

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
import cl.yahoo.webtables.features.FeaturesExtractorYData;

public class WikiTableExtractor
{
	static String				outputFileTables	= "./tables.html";
	private static final Logger	_log				= Logger.getLogger(WikiTableExtractor.class);

	public static void main(String[] args) throws Exception
	{
		DOMConfigurator.configure("./conf/log4j.xml");
		tartar.common.Logger logger = new tartar.common.Logger();

		if (args.length < 1)
		{
			System.out.println("Please enter the name of split folder.");
			// System.out.println("Argument 1 - Enter the path to the seeds file.");
			// System.out.println("Argument 2 - Enter the threshold for content tables.");
			System.exit(0);
		}

		// File archivo = new File(args[0]); // path of the seeds file
		// FileReader fr = new FileReader(archivo);
		// BufferedReader br = new BufferedReader(fr);
		// String lineSeed = "";
		//
		// if ((lineSeed = br.readLine()) != null)
		// {
		// System.out.println("Starting the analysis of the site: " + lineSeed);
		// lineSeed = lineSeed.replace("https://", "").replace("http://", "");
		// if (lineSeed.startsWith("www"))
		// lineSeed = lineSeed.substring(4);
		// lineSeed = lineSeed.substring(0, lineSeed.indexOf("/"));
		// } else
		// {
		// System.out.println("The seeds file is empty.");
		// System.exit(0);
		// }

		String split = args[0]; // there are 27 splits
		// String mainDirectory = LocationConstants.WIKI_HTML + split + "/";
		if (!split.endsWith("/"))
			split = split + "/";
		String mainDirectory = split;

		THashSet<String> pathFileSet = DirectoryUtils.getFilesList(mainDirectory + "dump/", ".html");
		FeaturesExtractorYData extractor = null;

		System.out.println(pathFileSet.size() + " HTML files readed from " + mainDirectory + "dump/");

		File file = null;
		InputStream url = null;

		String[] pathOutput = split.split("/");
		outputFileTables = outputFileTables.replace(".html", "_" + pathOutput[pathOutput.length - 1] + ".html");

		FileWriter fstream = new FileWriter(outputFileTables);
		BufferedWriter out = new BufferedWriter(fstream);

		out.write("<html><head><meta http-equiv='Content-Type' content='text/html; charset=UTF-8'></head><body>");

		// Instantiate a Date object
		Date date = new Date();
		// display time and date using toString()
		System.out.println(date.toString());
		System.out.println("Processing files");
		int count = 0;
		int tableCounter = 1;

		for (String pathFile : pathFileSet)
		{
			if (++count <= 100)
				System.out.print(".");
			else
			{
				System.out.println(".");
				count = 0;
			}
			// System.out.println("Processing file: " + pathFile);

			// Reading the HTML file
			file = new File(pathFile);
			url = new FileInputStream(file);

			SAXParser xmlReader = new org.cyberneko.html.parsers.SAXParser();
			SAXReader reader = new SAXReader(xmlReader);
			Document wikiDoc = reader.read(url);

			// Cerebral_arteriovenous_malformation OK
			// Roman_censor OK
			// Chechnya OK
			// Endomorphism OK
			// Economy_of_Chad Not well formed
			// Control_character OK
			// List_of_animated_television_series OK

			// David_Lynch

			String xmlPage = "";
			// if (pathFile.contains("Earth"))
			// {
			// Processing the file to extract the web-tables
			xmlPage = wikiDoc.asXML().replace("<TD/>", "<TD>&nbsp;</TD>\n")
					.replaceAll("<TD>[\\s|\n]*</TD>", "<TD>&nbsp;</TD>\n")
					.replaceAll("<TD ALIGN=\"RIGHT\">\\s*</TD>", "<TD ALIGN=\"RIGHT\">&nbsp;</TD>\n")
					.replaceAll("<TH>[\\s|\n]*!", "<TH>&nbsp;</TH><TH>");
			// System.out.println(xmlPage);
			// }

			Page page = new Page(new URL("http://www.example.com/"), xmlPage); // doc.asXML()
			// System.out.println(page.getLength() + "\t" + page.toHTML());

			ArrayList<Element> list = new ArrayList<Element>();
			list = new TableExtractor(logger).ExtractLeafTables(page);

			String aux = pathFile.replace(split, "");
			aux = aux.substring(5);

			// if (list.size() > 0)
			// System.out.println("The page " + file + " have tables: " +
			// list.size());
			// else
			// System.out.println("The page '" + file + "' hasn't tables");

			// int usefulTables = 0;
			Iterator<Element> iter = list.iterator();
			while (iter.hasNext())
			{
				Element tableEle = iter.next();

				// // System.out.println(tableEle.toHTML());
				extractor = new FeaturesExtractorYData();
				extractor.setTable(tableEle);

				// consider only tables with dimension 2x2 or higher
				if (extractor.getColumns() >= 2 && extractor.getRows() >= 2)
				{
					// Discard the toc tables in the page
					Tag tag = tableEle.getStartTag();
					if (tag.getHTMLAttribute("class") != null && tag.getHTMLAttribute("class").equalsIgnoreCase("toc"))
						continue;

					// usefulTables++;
					// save tables in an output file
					out.write(tableCounter++ + ".<br /> File: " + file + "<br />Predictor: " + "<br />"
							+ tableEle.toHTML().replace("TABLE", "TABLE border='1'") + "<br /><hr />");
				}
			}
		}
		System.out.println("\nFinished! See the file " + outputFileTables);
		_log.debug("Finished execution of WikiTablesExtractor. See file " + outputFileTables);

		out.write("\n</body>\n</html>");
		out.close();
	}
}
