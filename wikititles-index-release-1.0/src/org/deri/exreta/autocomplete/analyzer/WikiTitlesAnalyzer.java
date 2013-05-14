package org.deri.exreta.autocomplete.analyzer;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;

import org.apache.log4j.Logger;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.core.LowerCaseFilter;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.util.Version;

/**
 * Wikipedia titles Analyzer.
 * 
 * @author Emir Muñoz <emir@emunoz.org>
 * @version 0.0.1
 * @since 2013-03-18
 * 
 */
public class WikiTitlesAnalyzer extends Analyzer
{
	private static final Logger	_log	= Logger.getLogger(WikiTitlesAnalyzer.class);
	/** lucene version */
	private Version				matchVersion;

	/**
	 * Constructor.
	 * 
	 * @param matchVersion Lucene version.
	 */
	public WikiTitlesAnalyzer(Version matchVersion)
	{
		this.matchVersion = matchVersion;
	}

	/**
	 * Create tokenizer and filters instances.
	 */
	@Override
	protected TokenStreamComponents createComponents(String fieldName, Reader reader)
	{
		Tokenizer source = new WikiTitleTokenizer(matchVersion, reader);
		TokenStream filter = new LowerCaseFilter(matchVersion, source);
		return new TokenStreamComponents(source, filter);
		// //return new TokenStreamComponents(new WhitespaceTokenizer(matchVersion, reader));
		// return new TokenStreamComponents(new WikiTitleTokenizer(matchVersion, reader));
	}

	public static void main(String[] args) throws IOException
	{
		// text to tokenize
		// final String text = "This is a demo of the TokenStream API";
		final String text = "This_is_a_demo_of_the_TokenStream_API";

		Version matchVersion = Version.LUCENE_42;
		WikiTitlesAnalyzer analyzer = new WikiTitlesAnalyzer(matchVersion);
		TokenStream stream = analyzer.tokenStream("title", new StringReader(text + " "));

		// get the CharTermAttribute from the TokenStream
		CharTermAttribute termAtt = stream.addAttribute(CharTermAttribute.class);

		try
		{
			stream.reset();
			// print all tokens until stream is exhausted
			while (stream.incrementToken())
			{
				System.out.println(termAtt.toString());
			}
			stream.end();
		} finally
		{
			stream.close();
		}
		_log.info("###END### test");
	}

}
