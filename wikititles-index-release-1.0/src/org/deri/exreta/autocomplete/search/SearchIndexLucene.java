package org.deri.exreta.autocomplete.search;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import org.apache.log4j.Logger;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.SortField;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;
import org.deri.exreta.autocomplete.analyzer.WikiTitlesAnalyzer;

import cl.em.utils.performance.TimeWatch;

/**
 * Class to perform search over a index of Wikipedia Titles.
 * 
 * @author Emir Muñoz <emir@emunoz.org>
 * @since 2013-03-12
 * 
 */
public class SearchIndexLucene
{
	private static final Logger		_log		= Logger.getLogger(SearchIndexLucene.class);
	private static int				hitsPerPage	= 20;
	private static TimeWatch		watch		= TimeWatch.start();
	private static IndexSearcher	searcher;
	private static QueryParser		parser;
	private static IndexReader		reader;

	/**
	 * Constructor.
	 * 
	 * @param indexPath Path where is located the index.
	 */
	public SearchIndexLucene(String indexPath)
	{
		try
		{
			loadSearcher(indexPath);
		} catch (IOException e)
		{
			_log.error("Error loading title's lucene index.");
		}
	}

	/**
	 * Constructor.
	 * 
	 * @param indexPath Path where is located the index.
	 * @param hits Number of hits to be retrieved.
	 */
	public SearchIndexLucene(String indexPath, int hits)
	{
		try
		{
			hitsPerPage = hits;
			loadSearcher(indexPath);
		} catch (IOException e)
		{
			_log.error("Error loading title's lucene index.");
		}
	}

	/**
	 * Load the existing index to perform searches.
	 * 
	 * @param indexPath Path where is located the index.
	 * @throws IOException
	 */
	private void loadSearcher(String indexPath) throws IOException
	{
		// Directory directory = new RAMDirectory(); // RAM index storage
		Directory directory = FSDirectory.open(new File(indexPath)); // disk index storage
		reader = DirectoryReader.open(directory);
		searcher = new IndexSearcher(reader);
		_log.info("Index of titles with " + reader.numDocs() + " titles, loaded in " + watch.elapsedTime());

		Analyzer analyzer = new WikiTitlesAnalyzer(Version.LUCENE_42);
		parser = new QueryParser(Version.LUCENE_42, "title", analyzer);
		parser.setDefaultOperator(QueryParser.Operator.AND);

		directory.close();
		// reader can only be closed when there is no need to access the documents any more.
		// reader.close();
	}

	/**
	 * Close index reader.
	 */
	public void closeIndex()
	{
		try
		{
			reader.close();
			_log.info("Index reader closed.");
		} catch (IOException e)
		{
			_log.error("Error closing lucene index reader.");
		}
	}

	/**
	 * Function to retrieve Wikipedia titles given a user query.
	 * 
	 * @param query User query.
	 * @return A list of string titles.
	 * @throws IOException
	 */
	private ArrayList<String> doTitleSearch(Query query) throws IOException
	{
		ArrayList<String> docs = new ArrayList<String>();

		// SortField sortField = new SortField("title", Type.STRING_VAL, true);
		// Sort sortBySender = new Sort(sortField);
		// //WildcardQuery query2 = new WildcardQuery(new Term("title","manchester*"));
		// TopFieldDocs topDocs =
		// searcher.search(query,null,20,sortBySender);
		// //Sorting by index order
		// topDocs = searcher.search(query,null,20,Sort.INDEXORDER);
		//
		// System.out.println("Total hits " + topDocs.totalHits);
		// // Get an array of references to matched documents
		// ScoreDoc[] scoreDosArray = topDocs.scoreDocs;
		// for (ScoreDoc scoredoc : scoreDosArray)
		// {
		// // Retrieve the matched document and show relevant details
		// Document doc = searcher.doc(scoredoc.doc);
		// System.out.println(doc.getField("title").stringValue());
		// }

		Sort custSort = new Sort(new SortField("len", SortField.Type.INT));
		ScoreDoc[] hits = searcher.search(query, null, hitsPerPage, custSort).scoreDocs;

		// TopScoreDocCollector collector = TopScoreDocCollector.create(hitsPerPage, false);
		// searcher.search(query, collector);
		// // searcher.search(query, null, 100, new Sort(new SortField("title", SortField.Type.STRING_VAL, true)));
		// ScoreDoc[] hits = collector.topDocs().scoreDocs;

		for (int i = 0; i < hits.length; ++i)
		{
			int docId = hits[i].doc;
			Document d = searcher.doc(docId);
			String title = d.get("title");
			title = title.trim(); // delete whitespaces
			docs.add(title);
		}
		return docs;
	}

	/**
	 * Function to retrieve Wikipedia titles given a string of terms.
	 * 
	 * @param terms Terms inserted by user in the interface.
	 * @return A list of string titles.
	 * @throws IOException
	 */
	public ArrayList<String> searchFor(String terms) throws IOException
	{
		// parse terms and insert wildcards
		String escapequery = QueryParser.escape(terms);
		if (escapequery.endsWith(" "))
			escapequery = escapequery.substring(0, escapequery.length() - 1);
		escapequery = escapequery.replace(" ", "* ");
		escapequery = escapequery.replaceAll("$", "*");
		Query query = null;

		try
		{
			query = parser.parse(escapequery);
		} catch (ParseException e)
		{
			_log.error("Error parsing query " + escapequery);
		}

		return doTitleSearch(query);
	}

	public static void main(String[] args) throws ParseException, IOException
	{
		SearchIndexLucene searchlucene = new SearchIndexLucene("./index/index-title");
		String querystr = "manchester united";

		ArrayList<String> result = searchlucene.searchFor(querystr);
		for (String docTitle : result)
		{
			System.out.println(docTitle);
		}

		searchlucene.closeIndex();
		System.out.println("Elapsed time: " + watch.elapsedTime());
	}
}
