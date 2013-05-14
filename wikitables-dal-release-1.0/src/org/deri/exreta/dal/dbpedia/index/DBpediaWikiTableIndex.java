package org.deri.exreta.dal.dbpedia.index;

import java.io.IOException;
import java.util.HashSet;
import java.util.Iterator;

import org.apache.log4j.Logger;
import org.deri.exreta.dal.main.LocationConstants;
import org.semanticweb.nxindex.NodesIndex;
import org.semanticweb.nxindex.block.NodesBlockReaderIO;
import org.semanticweb.nxindex.block.util.CallbackIndexerIO;
import org.semanticweb.nxindex.sparse.SparseIndex;
import org.semanticweb.yars.nx.Node;
import org.semanticweb.yars.nx.Nodes;
import org.semanticweb.yars.nx.Resource;
import org.semanticweb.yars.nx.parser.ParseException;
import org.semanticweb.yars.nx.util.NxUtil;
import org.semanticweb.yars.util.LRUMapCache;

public class DBpediaWikiTableIndex
{
	private static final Logger					_log			= Logger.getLogger(DBpediaWikiTableIndex.class
																		.getSimpleName());
	private static final int					TICKS			= 1000000;
	public static final int						CACHE_SIZE		= 200;
	/** Nodes Block Reader */
	private NodesBlockReaderIO					nbr				= null;
	/** Nodes Block Index */
	private NodesIndex							qnbi			= null;

	private LRUMapCache<Nodes, HashSet<String>>	relationCache	= new LRUMapCache<Nodes, HashSet<String>>(CACHE_SIZE);
	private LRUMapCache<Nodes, Boolean>			askCache		= new LRUMapCache<Nodes, Boolean>(CACHE_SIZE);

	/**
	 * Constructor for index.
	 * 
	 * @param index On-disk index file (suffix *.ni)
	 * @param sparse On-disk sparse file (suffix *.sp)
	 * @throws IOException
	 * @throws ParseException
	 */
	public DBpediaWikiTableIndex(String index, String sparse) throws IOException, ParseException
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
	public String getRedirect(final String uri) throws IOException
	{
		if (uri.isEmpty())
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

			// Node[] triple = triples.next();
			//
			// if (triple[0].equals(source))
			// {
			// // if (triples.hasNext() && triples.next()[0].equals(source))
			// // {
			// // _log.warning("More than one redirect target for source");
			// // }
			// // return triple[1].toString();
			//
			// if (triple[1].toString().startsWith("http://dbpedia.org"))
			// return triple[1].toString();
			// else
			// {
			// while (triples.hasNext() && triples.next()[0].equals(source))
			// {
			// triple = triples.next();
			// if (triple[1].toString().startsWith("http://dbpedia.org"))
			// return triple[1].toString();
			// }
			// }
			// }

			while (triples.hasNext())
			{
				Node[] triple = triples.next();
				if (triples.next()[0].equals(source) && triple[1].toString().startsWith("http://dbpedia.org"))
					return triple[1].toString();
			}
		} catch (Exception e)
		{
			_log.error("Error getting redirect of URI: " + uri);
		}

		return null;
	}

	/**
	 * Find relations between two things in the index.
	 * 
	 * @param subjUri subject for the triple
	 * @param objUri object for the triple
	 * @return Relations from that subject to that object
	 * @throws IOException
	 */
	public HashSet<String> getRelations(String subjUri, String objUri) throws IOException
	{
		if (subjUri != null && objUri != null)
		{
			Resource s = new Resource(NxUtil.escapeForNx(subjUri));
			Resource o = new Resource(NxUtil.escapeForNx(objUri));
			return getRelations(s, o);
		} else
		{
			return new HashSet<String>();
		}
	}

	/**
	 * Find relations between two things in the index.
	 * 
	 * @param s subject for the triple
	 * @param o object for the triple
	 * @return Relations from that subject to that object
	 * @throws IOException
	 */
	public HashSet<String> getRelations(Node s, Node o) throws IOException
	{
		Node[] key = new Node[] { s, o };
		Nodes so = new Nodes(key);
		HashSet<String> relations = relationCache.get(so);

		if (relations == null)
		{
			Iterator<Node[]> triples = qnbi.getIterator(key);

			if (triples == null || !triples.hasNext())
			{
				return null;
			}

			relations = new HashSet<String>();
			while (triples.hasNext())
			{
				Node[] next = triples.next();
				if (next[0].equals(s) && next[1].equals(o))
				{
					relations.add(next[2].toString());
				}
			}
			relationCache.put(so, relations);
		}
		return relations;
	}

	/**
	 * Find if triple exists in the index
	 * 
	 * @param subjUri subject for the triple
	 * @param predUri predicate for the triple
	 * @param objUri object for the triple
	 * @return true if triple in index, false otherwise
	 * @throws IOException
	 */
	public boolean exists(String subjUri, String predUri, String objUri) throws IOException
	{
		Resource s = new Resource(NxUtil.escapeForNx(subjUri));
		Resource p = new Resource(NxUtil.escapeForNx(predUri));
		Resource o = new Resource(NxUtil.escapeForNx(objUri));
		return exists(s, p, o);
	}

	/**
	 * Find if triple exists in the index
	 * 
	 * @param s subject for the triple
	 * @param p predicate for the triple
	 * @param o object for the triple
	 * @return true if triple in index, false otherwise
	 * @throws IOException
	 */
	public boolean exists(Node s, Node p, Node o) throws IOException
	{
		Node[] key = new Node[] { s, o };
		Nodes spo = new Nodes(s, p, o);
		Boolean exists = askCache.get(spo);

		// if triple is not in cache
		if (exists == null)
		{
			Iterator<Node[]> triples = qnbi.getIterator(key);
			exists = false;

			if (triples == null || !triples.hasNext())
			{
				// System.out.println("There is no triples");
				return exists;
			} else
			{
				while (triples.hasNext())
				{
					Node[] next = triples.next();
					if (next[0].equals(s) && next[1].equals(o))
					{
						// System.out.println("I found " + next[0] + "###" + next[1] + "###" + next[2]);
						if (next[2].equals(p))
						{
							exists = true;
							break;
						}
					}
				}
				// only if exists add to cache
				if (exists)
				{
					askCache.put(spo, exists);
					return true;
				}
			}
		}
		return exists.booleanValue();
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

	public static void main(String[] args)
	{
		String index = LocationConstants.DBPEDIA38_INDEX;
		String sparse = LocationConstants.DBPEDIA38_SPARSE;
		try
		{
			DBpediaWikiTableIndex dbp = new DBpediaWikiTableIndex(index, sparse);

			System.out.println("Testing relations");
			HashSet<String> rels = dbp.getRelations("http://dbpedia.org/resource/San_Jose,_California",
					"http://dbpedia.org/resource/United_States");

			if (rels != null)
				for (String rel : rels)
				{
					if (!rel.equals("http://dbpedia.org/ontology/wikiPageWikiLink"))
						System.err.println(rel);
				}
			else
				System.out.println("There is no relation");

			System.out.println("Testing redirect");
			System.err.println(dbp.getRedirect("http://dbpedia.org/resource/Paul_Henderson_(Australian_politician)"));

			System.out.println("Testing exists");
			System.out.println(dbp.exists("http://dbpedia.org/resource/San_Jose,_California",
					"http://dbpedia.org/property/subdivisionName", "http://dbpedia.org/resource/United_States"));

		} catch (IOException e)
		{
			_log.error("Error using index");
		} catch (ParseException e)
		{
			_log.error("Error reading index");
		}
	}
}
