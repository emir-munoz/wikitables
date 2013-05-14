package org.deri.exreta.wikipedia.parser.html;

import info.bliki.api.creator.DumpDocumentCreator;
import info.bliki.api.creator.TopicData;
import info.bliki.api.creator.WikiDB;
import info.bliki.wiki.dump.IArticleFilter;
import info.bliki.wiki.dump.Siteinfo;
import info.bliki.wiki.dump.WikiArticle;
import info.bliki.wiki.dump.WikiXMLParser;
import info.bliki.wiki.filter.Encoder;
import info.bliki.wiki.impl.DumpWikiModel;

import java.io.IOException;

import org.apache.log4j.Logger;
import org.apache.log4j.xml.DOMConfigurator;
import org.deri.exreta.dal.main.LocationConstants;
import org.xml.sax.SAXException;

/**
 * Create static HTML files from a given Mediawiki dump
 * 
 * @author Emir Muñoz <emir@emunoz.org>
 *         Modified from:
 *         http://code.google.com/p/gwtwiki/source/browse/trunk/info.bliki.wiki/bliki-pdf/src/test/java/info
 *         /bliki/api/creator/Dump2HTMLCreatorExample.java
 * 
 */
public class Dump2HTMLCreator
{
	private static final Logger	_log	= Logger.getLogger(Dump2HTMLCreator.class);

	public Dump2HTMLCreator()
	{
		super();
	}

	static class DemoArticleFilter implements IArticleFilter
	{
		WikiDB					wikiDB;
		int						counter;
		private final String	htmlDirectory;
		private final String	imageDirectory;

		public DemoArticleFilter(WikiDB db, String htmlDirectory, String imageDirectory)
		{
			this.counter = 0;
			this.wikiDB = db;
			if (htmlDirectory.charAt(htmlDirectory.length() - 1) != '/')
			{
				htmlDirectory = htmlDirectory + "/";
			}
			this.htmlDirectory = htmlDirectory;
			this.imageDirectory = imageDirectory;
		}

		public void process(WikiArticle page, Siteinfo siteinfo) throws SAXException
		{
			if (page.isMain() || page.isCategory() || page.isProject())
			{
				String title = page.getTitle();
				String titleURL = Encoder.encodeTitleLocalUrl(title);
				String generatedHTMLFilename = htmlDirectory + titleURL + ".html";
				DumpWikiModel wikiModel = new DumpWikiModel(wikiDB, siteinfo, "${image}", "${title}", imageDirectory);
				DumpDocumentCreator creator = new DumpDocumentCreator(wikiModel, page);
				creator.setHeader(HTMLConstants.HTML_HEADER1 + HTMLConstants.CSS_SCREEN_STYLE
						+ HTMLConstants.HTML_HEADER2);
				creator.setFooter(HTMLConstants.HTML_FOOTER);
				wikiModel.setUp();
				try
				{
					creator.renderToFile(generatedHTMLFilename);
					System.out.print('.');
					if (++counter >= 80)
					{
						System.out.println(' ');
						counter = 0;
					}
				} catch (IOException e)
				{
					System.out.println("Error at process function: " + page.getTitle());
					e.printStackTrace();
				} catch (Exception e1)
				{
					System.out.println("Error at process function: " + page.getTitle());
					e1.printStackTrace();
				}
			}
		}
	}

	static class DemoTemplateArticleFilter implements IArticleFilter
	{
		WikiDB	wikiDB;
		int		counter;

		public DemoTemplateArticleFilter(WikiDB wikiDB)
		{
			this.wikiDB = wikiDB;
			this.counter = 0;
		}

		public void process(WikiArticle page, Siteinfo siteinfo) throws SAXException
		{
			if (page.isTemplate())
			{
				// System.out.println(page.getTitle());
				String pageTitle = page.getTitle().length() >= 256 ? page.getTitle().substring(0, 255) : page
						.getTitle();
				// pageTitle = pageTitle.replace("&", "")
				TopicData topicData = new TopicData(pageTitle, page.getText());
				try
				{
					wikiDB.insertTopic(topicData);
					System.out.print('.');
					if (++counter >= 100)
					{
						System.out.println(' ');
						counter = 0;
					}
				} catch (Exception e)
				{
					System.out.println("Error at process function: " + page.getTitle());
					String mess = e.getMessage();
					if (mess == null)
					{
						throw new SAXException(e.getClass().getName());
					}
					throw new SAXException(mess);
				}
			}
		}
	}

	public static WikiDB prepareDB(String mainDirectory)
	{
		// the following subdirectory should not exist if you would like to
		// create a new database
		if (mainDirectory.charAt(mainDirectory.length() - 1) != '/')
		{
			mainDirectory = mainDirectory + "/";
		}
		String databaseSubdirectory = "WikiDumpDB";

		WikiDB db = null;

		try
		{
			db = new WikiDB(mainDirectory, databaseSubdirectory);
			return db;
		} catch (IOException e)
		{
			System.out.println("Error at prepareDB function: " + mainDirectory);
			e.printStackTrace();
		} catch (Exception e1)
		{
			System.out.println("Error at prepareDB function: " + mainDirectory);
			e1.printStackTrace();
		} finally
		{
			// if (db != null) {
			// try {
			// db.tearDown();
			// } catch (Exception e) {
			// e.printStackTrace();
			// }
			// }
		}
		return null;
	}

	/**
	 * Main
	 */
	public static void main(String[] args)
	{
		DOMConfigurator.configure("./conf/log4j.xml");

		boolean skipFirstPass = false;
		// String arg1 = "";

		// if (args.length < 1)
		// {
		// System.err.println("Usage: Dump2HTMLCreatorExample <XML-FILE> [<SKIP-FIRST_PASS>=true|yes]");
		// System.exit(-1);
		// }
		// if (args.length > 1)
		// {
		// arg1 = args[1].toLowerCase();
		// if (arg1.equals("true") || arg1.equals("yes"))
		// {
		// skipFirstPass = true;
		// System.out.println("Option <skip first pass> is set to true");
		// }
		// }

		String bz2Filename = LocationConstants.WIKI_BZ2 + "enwiki-latest-pages-articles10.xml-p000925001p001325000.bz2";
		// String bz2Filename = args[0];
		// String bz2Filename = args[0];
		WikiDB db = null;
		_log.info("Starting the HTML extraction from bz2 Wikipedia dump " + bz2Filename);
		try
		{
			// there are 27 splits
			String split_number = "split_10/";
			// String split_number = args[2];
			if (!split_number.endsWith("/"))
				split_number = split_number + "/";

			String mainDirectory = LocationConstants.WIKI_HTML + split_number;
			String htmlDirectory = mainDirectory + "dump/";

			// the following directory must exist for image references
			String imageDirectory = mainDirectory + "WikiDumpImages/";
			System.out.println("Prepare wiki database");
			db = prepareDB(mainDirectory);
			IArticleFilter handler;
			WikiXMLParser wxp;
			if (!skipFirstPass)
			{
				System.out.println("First pass - write templates to database:");
				handler = new DemoTemplateArticleFilter(db);
				wxp = new WikiXMLParser(bz2Filename, handler);
				wxp.parse();
				System.out.println(' ');
			}
			System.out.println("Second pass - write HTML files to directory:");
			handler = new DemoArticleFilter(db, htmlDirectory, imageDirectory);
			wxp = new WikiXMLParser(bz2Filename, handler);
			wxp.parse();
			System.out.println(' ');
			System.out.println("Done!");
			_log.info("Finished the HTML pages creation for bz2 file " + bz2Filename);
		} catch (Exception e)
		{
			System.out.println("Error at main function.");
			e.printStackTrace();
		} finally
		{
			if (db != null)
			{
				try
				{
					db.tearDown();
				} catch (Exception e)
				{
					System.out.println("Error in finally of main");
					e.printStackTrace();
				}
			}
		}
	}
}
