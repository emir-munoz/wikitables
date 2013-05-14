package org.deri.exreta.dal.dbpedia.index;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.log4j.Logger;
import org.deri.exreta.dal.dbpedia.query.QueryBuilder.EntityType;
import org.deri.exreta.dal.main.LocationConstants;
import org.semanticweb.nxindex.NodesIndex;
import org.semanticweb.nxindex.block.NodesBlockReaderIO;
import org.semanticweb.nxindex.block.util.CallbackIndexerIO;
import org.semanticweb.nxindex.sparse.SparseIndex;
import org.semanticweb.yars.nx.Literal;
import org.semanticweb.yars.nx.Node;
import org.semanticweb.yars.nx.parser.NxParser;
import org.semanticweb.yars.nx.parser.ParseException;

import cl.em.utils.string.URLUTF8Encoder;

public class LabelsIndex
{
	private static final Logger	_log		= Logger.getLogger(LabelsIndex.class.getSimpleName());
	private static final int	TICKS		= 1000000;
	/** Nodes Block Reader */
	private NodesBlockReaderIO	nbr			= null;
	/** Nodes Block Index */
	private NodesIndex			qnbi		= null;
	/** Exceptional cases for resources */
	private Map<String, String>	exceptions	= null;

	/**
	 * Constructor for index.
	 * 
	 * @param index On-disk index file (suffix *.ni)
	 * @param sparse On-disk sparse file (suffix *.sp)
	 * @throws IOException
	 * @throws ParseException
	 */
	public LabelsIndex(String index, String sparse) throws IOException, ParseException
	{
		this(index, sparse, null);
	}

	/**
	 * Constructor for index.
	 * 
	 * @param index On-disk index file (suffix *.ni)
	 * @param sparse On-disk sparse file (suffix *.sp)
	 * @throws IOException
	 * @throws ParseException
	 */
	public LabelsIndex(String index, String sparse, String exceptionList) throws IOException, ParseException
	{
		nbr = new NodesBlockReaderIO(index);
		SparseIndex qsi = CallbackIndexerIO.loadSparseIndex(sparse, TICKS);
		qnbi = new NodesIndex(nbr, qsi);
		if (exceptionList != null)
		{
			loadExceptions(exceptionList);
		}
	}

	private void loadExceptions(String exceptionList) throws IOException
	{
		BufferedReader br = new BufferedReader(new FileReader(exceptionList));
		String line = null;

		exceptions = new HashMap<String, String>();
		while ((line = br.readLine()) != null)
		{
			line = line.trim();

			if (line.startsWith("#"))
				continue;

			String[] exception = line.split("\t");
			if (exception.length != 2 || exception[0].length() <= 2 || exception[1].length() <= 2)
			{
				_log.warn("Unknown exception format : " + exception);
			} else
			{
				String old = exceptions.put(exception[0].substring(1, exception[0].length() - 1),
						exception[1].substring(1, exception[1].length() - 1));

				// System.out.println(exception[0].substring(1, exception[0].length() - 1) + "\t" +
				// exception[1].substring(1, exception[1].length() - 1));

				if (old != null)
				{
					_log.warn("Multiple exceptions found for " + exception[0] + "!");
				}
			}
		}
	}

	/**
	 * Find the subject for an exact label match
	 * 
	 * @param lab Label (e.g., "\"Blah\"@en"
	 * @return Subject of label
	 * @throws IOException
	 * @throws ParseException
	 */
	@SuppressWarnings("deprecation")
	public String getLabel(String lab, EntityType type) throws IOException, ParseException
	{
		lab = lab.replace("_", " ");
		try
		{
			String lab_ = URLDecoder.decode(lab, "UTF-8");
			lab_ = URLUTF8Encoder.unicodeEscape(lab_);

			Node n = NxParser.parseNode(lab_);
			Node[] key = new Node[] { n };
			Iterator<Node[]> triples = qnbi.getIterator(key);

			Node[] first = null;
			boolean isFirst = true;
			boolean hasElements = false;

			while (triples != null && triples.hasNext())
			{
				Node[] triple = triples.next();
				if (isFirst)
				{
					first = triple;
					hasElements = true;
				}

				if (triple[0].equals(n))
				{
					if (type.equals(EntityType.OBJECT)
							&& triple[1].toString().startsWith(LocationConstants.DBPEDIA_RESOURCE))
						return exception(triple[1].toString());
					else if (type.equals(EntityType.PROPERTY)
							&& triple[1].toString().startsWith(LocationConstants.DBPEDIA_PROPERTY))
						return exception(triple[1].toString());
				}
			}
			if (hasElements)
				return first[1].toString();

		} catch (final Exception e)
		{
			_log.error("Illegal character in label: " + lab);
		}

		return null;
	}

	/**
	 * Guess the subject for an exact label match without
	 * using index.
	 * 
	 * @param lab Label (e.g., "\"Blah\"@en"
	 * @return Subject of label
	 * @throws IOException
	 * @throws ParseException
	 */
	@SuppressWarnings("deprecation")
	public String guessLabel(String lab) throws IOException, ParseException
	{
		try
		{
			Node n = NxParser.parseNode(lab);
			if (!(n instanceof Literal))
			{
				return null;
			}

			Literal l = (Literal) n;
			String unescp = l.getUnescapedData();
			unescp = unescp.replaceAll(" ", "_");
			// String percEnc = URLEncoder.encode(unescp, "UTF-8");
			String percEnc = URLUTF8Encoder.unicodeEscape(unescp);

			return exception(LocationConstants.DBPEDIA_RESOURCE + percEnc);
		} catch (Exception e)
		{
			_log.warn("Error guessing label for: " + lab);
		}
		return String.valueOf("");
	}

	private String exception(String out)
	{
		try
		{
			if (exceptions == null)
				return out;
			String exception = exceptions.get(out);
			if (exception == null)
				return out;
			return exception;
		} catch (Exception e)
		{
			_log.warn("Error getting exception for: " + out);
		}
		return String.valueOf("");
	}

	/**
	 * Close the index.
	 * 
	 * @throws IOException
	 */
	public void close() throws IOException
	{
		nbr.close();
	}

	// example and test / remove if not needed
	public static void main(String[] args) throws IOException, ParseException
	{
		String index = LocationConstants.LABELS_INDEX; // "testdata/dbr/labels.ni"; // file ending in .ni
		String sparse = LocationConstants.LABELS_SPARSE; // "testdata/dbr/labels.sp"; // file ending in .sp
		String excep = LocationConstants.LABELS_EXCEPTIONS;

		// may take a few second to load so re-use object!
		LabelsIndex li = new LabelsIndex(index, sparse, excep);

		System.err.println(li.getLabel("\"AlexanderTheGreat\"@en", EntityType.OBJECT));
		// should give http://dbpedia.org/resource/AlexanderTheGreat

		System.err.println(li.getLabel("\"Citro\\u00EBn C2\"@en", EntityType.OBJECT));
		// should give http://dbpedia.org/resource/Citro%C3%ABn_C2

		// the same without index ???
		System.err.println(li.guessLabel("\"AlexanderTheGreat\"@en"));
		System.err.println(li.guessLabel("\"Citro\u00EBn C2\"@en"));

		// String oldString = "\"Citro\u00EBn C2\"@en";
		// String newString = new String(oldString.getBytes("UTF-8"), "UTF-8");
		// System.out.println(newString);
		// System.out.println(newString.equals(oldString));

		System.out.println(li.getLabel("\"Czech\"@en", EntityType.OBJECT));

		String oldString1 = "\"Dayton%2C_Ohio\"@en";
		System.out.println(li.getLabel(oldString1, EntityType.OBJECT));

		// String oldString2 = "\"2. Fu\\u00DFball-Bundesliga\"@en";
		// System.out.println(li.getLabel(oldString2));
		// oldString2 = oldString2.replace("_", " ");
		// // String newString2 = URLDecoder.decode(oldString2, "UTF-8");
		// // System.out.println(newString2);
		// String newString2 = new String(oldString2.getBytes(), "UTF-8");
		// System.out.println("##" + li.getLabel(newString2));

		String oldString2_ = "\"2._Fu%C3%9Fball-Bundesliga\"@en";
		// oldString2_ = oldString2_.replace("_", " ");
		// String newString2_ = URLDecoder.decode(oldString2_, "UTF-8");
		// System.out.println("## " + li.getLabel(unicodeEscape(newString2_)));
		System.out.println(li.getLabel(oldString2_, EntityType.OBJECT));
		// String lab_ = URLDecoder.decode(oldString2_, "UTF-8");
		// System.out.println(URLUTF8Encoder.unicodeEscape(lab_));

		// Test for properties
		// System.err.println(ri.getLabel("\"position\"@en"));
	}
}
