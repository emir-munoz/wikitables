package org.deri.exreta.autocomplete.index;

import java.io.File;
import java.io.IOException;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;
import org.deri.exreta.autocomplete.analyzer.WikiTitlesAnalyzer;
import org.deri.exreta.autocomplete.io.ReadWikiTitles;

import cl.em.utils.performance.TimeWatch;

/**
 * Build the index of Wikipedia Titles from dump.
 * 
 * @author Emir Muñoz <emir@emunoz.org>
 * @version 0.1.0
 * @since 2013-03-13
 * 
 */
public class BuildTitleIndex
{
	public static void main(String[] args) throws IOException
	{
		TimeWatch watch = TimeWatch.start();

		// 0. Specify the analyzer for tokenizing text. The same analyzer should be used for indexing and searching
		Analyzer analyzer = new WikiTitlesAnalyzer(Version.LUCENE_42);

		// 1. create index
		// Directory directory = new RAMDirectory(); // RAM index storage
		Directory directory = FSDirectory.open(new File("index/index-title"));  // disk index storage

		IndexWriterConfig indexWriterConfig = new IndexWriterConfig(Version.LUCENE_42, analyzer);
		IndexWriter writer = new IndexWriter(directory, indexWriterConfig);
		// System.out.println(directory.getClass() + " RamBuff:" + w.getRAMBufferSizeMB());
		System.out.println(directory.getClass() + " Index: " + writer.getConfig());

		// add titles from gz file
		ReadWikiTitles gzfile = new ReadWikiTitles();
		gzfile.readGZFile(writer, "data/enwiki-20130204-all-titles-in-ns0.gz", "UTF-8");

		// close streams
		writer.close();
		directory.close();

		System.out.println("Finished title index construction. Elapsed time: " + watch.elapsedTime());
	}
}
