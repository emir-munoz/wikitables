package org.deri.exreta.autocomplete.io;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.zip.GZIPInputStream;

import org.apache.log4j.Logger;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.IntField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexWriter;

/**
 * Class to read the Wikipedia titles dump and create the index.
 * 
 * @author Emir Muñoz <emir@emunoz.org>
 * @since 2013-03-12
 * 
 */
public class ReadWikiTitles
{
	private static final Logger	_log	= Logger.getLogger(ReadWikiTitles.class);
	private Document			doc;

	/**
	 * Read the GZ file line per line.
	 * 
	 * @param writer Lucene index writer.
	 * @param filename Name of the file with Wikipedia titles.
	 * @param encoding Encoding to be used.
	 */
	public void readGZFile(IndexWriter writer, String filename, String encoding)
	{
		if (encoding == null || encoding.equals(""))
			encoding = "UTF-8";
		try
		{
			InputStream fileStream = new FileInputStream(filename);
			InputStream gzipStream = new GZIPInputStream(fileStream);
			Reader decoder = new InputStreamReader(gzipStream, encoding);
			BufferedReader br = new BufferedReader(decoder);
			String line;
			int count = 0;

			// read the file line per line
			while ((line = br.readLine()) != null)
			{
				// here any filter for the titles
				doc = new Document();
				doc.add(new TextField("title", line + " ", Field.Store.YES));
				doc.add(new IntField("len", line.length(), Field.Store.YES));
				writer.addDocument(doc);
				System.out.println(++count);
			}
		} catch (FileNotFoundException e)
		{
			_log.error(String.format("File %s not found.", filename));
		} catch (IOException e)
		{
			_log.error("Error trying to read GZ file with Wikipedia Titles.");
		}

	}
}
