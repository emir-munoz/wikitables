package org.deri.exreta.dal.dbpedia.index;

import java.io.IOException;
import java.util.Iterator;
import java.util.logging.Logger;

import org.deri.exreta.dal.main.LocationConstants;
import org.semanticweb.nxindex.NodesIndex;
import org.semanticweb.nxindex.block.NodesBlockReaderIO;
import org.semanticweb.nxindex.block.util.CallbackIndexerIO;
import org.semanticweb.nxindex.sparse.SparseIndex;
import org.semanticweb.yars.nx.Node;
import org.semanticweb.yars.nx.Resource;
import org.semanticweb.yars.nx.parser.ParseException;
import org.semanticweb.yars.nx.util.NxUtil;


public class RedirectsIndex
{
	public static final int	TICKS	= 1000000;
	static final Logger		_log	= Logger.getLogger(RedirectsIndex.class.getSimpleName());

	NodesBlockReaderIO		nbr		= null;
	NodesIndex				qnbi	= null;

	/**
	 * Constructor for index.
	 * 
	 * @param index On-disk index file (suffix *.ni)
	 * @param sparse On-disk sparse file (suffix *.sp)
	 * @throws IOException
	 * @throws ParseException
	 */
	public RedirectsIndex(String index, String sparse) throws IOException, ParseException
	{
		nbr = new NodesBlockReaderIO(index);
		SparseIndex qsi = CallbackIndexerIO.loadSparseIndex(sparse, TICKS);
		qnbi = new NodesIndex(nbr, qsi);
	}

	/**
	 * Find a redirect target in the index.
	 * 
	 * @param uri Source of redirect
	 * @return Target of redirect or null if not found
	 * @throws IOException
	 */
	public String getRedirect(String uri) throws IOException
	{
		if (uri == null || uri.isEmpty())
			return null;
		try
		{
			Resource source = new Resource(NxUtil.escapeForNx(uri));
			Node[] key = new Node[] { source };
			Iterator<Node[]> triples = qnbi.getIterator(key);

			if (triples == null || !triples.hasNext())
			{
				return null;
			}

			Node[] triple = triples.next();

			if (triple[0].equals(source))
			{
				if (triples.hasNext() && triples.next()[0].equals(source))
				{
					_log.warning("More than one redirect target for source");
				}
				return triple[2].toString();
			}
		} catch (Exception e)
		{
			_log.warning("Error escaping: " + uri);
		}
		return null;
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
		// may take a few second to load so re-use object!
		RedirectsIndex ri = new RedirectsIndex(LocationConstants.REDIRECT_INDEX, LocationConstants.REDIRECT_SPARSE);

		System.err.println(ri.getRedirect("http://dbpedia.org/resource/Paul_Henderson_(Australian_politician)"));
		// should give http://dbpedia.org/resource/Paul_Henderson_(politician)
		System.err.println(ri.getRedirect("http://dbpedia.org/resource/Bighorn/Sherwood"));
		// should give http://dbpedia.org/resource/Monday_Night_Golf
		System.err.println(ri.getRedirect("http://dbpedia.org/resource/Does_Not_exisT"));
		// should give null
		System.err.println(ri.getRedirect("http://dbpedia.org/resource/AsWeMayThink"));
	}
}
