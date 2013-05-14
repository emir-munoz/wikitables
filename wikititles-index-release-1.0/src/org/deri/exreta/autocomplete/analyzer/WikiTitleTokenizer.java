package org.deri.exreta.autocomplete.analyzer;

import java.io.IOException;
import java.io.Reader;

import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.util.Version;

/**
 * Tokenizer for Wikipedia Titles.
 * 
 * @author Emir Muñoz <emir@emunoz.org>
 * 
 */
public class WikiTitleTokenizer extends Tokenizer
{
	// private static final int TWO_LETTERS = 2;
	private CharTermAttribute	termAtt	= addAttribute(CharTermAttribute.class);
	private final int[]			symbols	= new int[] { 34, 35, 40, 41, 44, 45, 47, 58, 60, 61, 62, 92, 94, 95, 96, 0, 9,
			10, 11, 12, 13, 32			};

	/**
	 * Constructor.
	 * 
	 * @param matchVersion Lucene version.
	 * @param input Input reader.
	 */
	protected WikiTitleTokenizer(Version matchVersion, Reader input)
	{
		super(input);
	}

	/**
	 * Function to decide when start the next token.
	 */
	@Override
	public boolean incrementToken() throws IOException
	{
		StringBuilder builder = new StringBuilder();
		boolean success = readAhead(builder);
		if (success)
		{
			termAtt.setEmpty();
			termAtt.append(builder);
		}
		return success;
	}

	/**
	 * Read character by character, identifying ends of tokens.
	 * 
	 * @param builder String to build a token.
	 * @return True if a token is found; False otherwise.
	 * @throws IOException
	 */
	private boolean readAhead(StringBuilder builder) throws IOException
	{
		int data = input.read();
		for (int sy : symbols)
		{
			if (sy == data)
				return true;
		}
		if (data != -1)
		{
			builder.append((char) data);
			return readAhead(builder);
		}
		return false;
	}
}
