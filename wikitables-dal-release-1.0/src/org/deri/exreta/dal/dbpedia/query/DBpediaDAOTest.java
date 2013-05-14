package org.deri.exreta.dal.dbpedia.query;

import java.io.IOException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import org.apache.log4j.Logger;
import org.deri.exreta.dal.dbpedia.dto.Relation;
import org.deri.exreta.dal.dbpedia.dto.Resource;
import org.deri.exreta.dal.dbpedia.dto.TableCell;
import org.deri.exreta.dal.dbpedia.dto.TableRelation;
import org.deri.exreta.dal.dbpedia.dto.Triple;
import org.deri.exreta.dal.dbpedia.index.DBpediaWikiTableIndex;
import org.deri.exreta.dal.dbpedia.index.LabelsIndex;
import org.deri.exreta.dal.dbpedia.index.RedirectsIndex;
import org.deri.exreta.dal.dbpedia.query.QueryBuilder.EntityType;
import org.deri.exreta.dal.main.LocationConstants;
import org.semanticweb.yars.nx.parser.ParseException;

/**
 * Class to make request to DBpedia end-point.
 * 
 * @author Emir Munoz <emir@emunoz.org>
 * @version 0.1
 * @since 2012-08-07
 *        Changelog:
 *        - 2013-03-07 Update to work with different workspaces
 */
public class DBpediaDAOTest implements DAOInterface
{
	private static final Logger				_log		= Logger.getLogger(DBpediaDAOTest.class);
	/** Redirects Index */
	@SuppressWarnings("unused")
	private static RedirectsIndex			rindex		= null;
	/** Labels Index */
	@SuppressWarnings("unused")
	private static LabelsIndex				lindex		= null;
	/** Full DBpedia38 Index */
	@SuppressWarnings("unused")
	private static DBpediaWikiTableIndex	dbp			= null;
	private static String					workspace	= "";

	public DBpediaDAOTest(String _workspace)
	{
		if (_workspace != null)
		{
			if (_workspace.equalsIgnoreCase("local"))
				workspace = LocationConstants.LOCAL_FOLDER;
			else if (_workspace.equalsIgnoreCase("server"))
				workspace = LocationConstants.SERVER_FOLDER;
			else
				workspace = LocationConstants.SERVER_TEST;

		} else
		{
			// default
			workspace = LocationConstants.SERVER_TEST;
		}

		try
		{
			rindex = new RedirectsIndex(workspace + LocationConstants.REDIRECT_INDEX, workspace
					+ LocationConstants.REDIRECT_SPARSE);
			lindex = new LabelsIndex(workspace + LocationConstants.LABELS_INDEX, workspace
					+ LocationConstants.LABELS_SPARSE);
			dbp = new DBpediaWikiTableIndex(workspace + LocationConstants.DBPEDIA38_INDEX, workspace
					+ LocationConstants.DBPEDIA38_SPARSE);
		} catch (IOException e)
		{
			_log.error("Error initializing indexes. Message: " + e.getMessage());
		} catch (ParseException e)
		{
			_log.error("Error initializing indexes. Message: " + e.getMessage());
		}
		_log.info("Finished load of indexes for workspace " + _workspace);
	}

	/**
	 * Query to DBpedia to obtain the URI for terms, avoiding disambiguation.
	 * 
	 * @param terms Terms or concept for which we want to know its URI.
	 * @param type Type of entity.
	 * @return Resource with the name and URI.
	 */
	public Resource getResourceURI(String terms, EntityType type)
	{
		Resource resource = null;
		String terms_ = parseWikilink(terms);
		// If terms has a length more that 100 characters, discard
		if (terms_.length() > LocationConstants.MAX_LENGTH_TXT)
		{
			resource = new Resource();
			return resource;
		}
		// If terms is not a property and does not contains % symbol use the guess label function
		if (!type.equals(EntityType.PROPERTY) && !terms_.contains("%"))
		{
			// resource = new Resource(lindex.guessLabel("\"" + terms_ + "\"" + LocationConstants.LANGUAGE_EN), terms_);
			// TEST
			resource = new Resource("http://dbpedia.org/resource/TEST", "TEST");

			// If we found a resource retrieve it
			if (!resource.isEmpty())
				return resource;
			else
				_log.debug("None URI found! For: " + terms_);
		} else
			// If terms is not a property and contains % symbol use the index function
			// If terms is a property
			terms_ = terms_.trim();
		if (type.equals(EntityType.PROPERTY))
			terms_ = terms_.toLowerCase();
		if (!terms_.isEmpty() && terms_.length() > 1 && !terms_.equals("%"))
			// resource = new Resource(lindex.getLabel("\"" + terms_ + "\"" + LocationConstants.LANGUAGE_EN,
			// type), terms_);
			// TEST
			resource = new Resource("http://dbpedia.org/resource/TEST", "TEST");

		// If we found a resource retrieve it
		if (resource != null && !resource.isEmpty())
			return resource;
		else
			_log.debug("None URI found! For: " + terms_);
		return resource;
	}

	/**
	 * Find for relations between the main resource and the other resources.
	 * 
	 * @param resourceList List of resources.
	 * @return List of relations found within a table between pairs of elements from resourceList.
	 */
	public List<TableRelation> getDBpediaMainRelations(List<TableCell> resourceList)
	{
		List<TableRelation> relationList = new ArrayList<TableRelation>();
		List<Relation> relationFound = null;
		// We consider the first resource as the main
		for (int i = 1; i < resourceList.size(); i++)
		{
			if (resourceList.get(0) == null || resourceList.get(i) == null || resourceList.get(0).getResource() == null
					|| resourceList.get(i).getResource() == null)
				continue;
			if (resourceList.get(0).getResource().isEmpty() && resourceList.get(i).getResource().isEmpty())
				continue;
			// if (resourceList.get(0).getResource().getURI().isEmpty()
			// || resourceList.get(i).getResource().getURI().isEmpty())
			// ; // do nothing
			else
			{
				// System.out.println("RELATION BETWEEN: " + resourceList.get(i).getResource() + " ### "
				// + resourceList.get(j).getResource());
				try
				{
					// Look for relations between the main entity and other resource
					relationFound = this.getRelations(resourceList.get(0).getResource(), resourceList.get(i)
							.getResource());
					if (relationFound != null && !relationFound.isEmpty())
					{
						for (Relation relation : relationFound)
						{
							relationList.add(new TableRelation(relation, resourceList.get(0).getCol(), resourceList
									.get(i).getCol(), resourceList.get(i).getRow()));
						}
					}
					// Look for relations between other resource and the main entity
					relationFound = this.getRelations(resourceList.get(i).getResource(), resourceList.get(0)
							.getResource());
					if (relationFound != null && !relationFound.isEmpty())
					{
						for (Relation relation : relationFound)
						{
							relationList.add(new TableRelation(relation, resourceList.get(i).getCol(), resourceList
									.get(0).getCol(), resourceList.get(i).getRow()));
						}
					}
				} catch (IOException e)
				{
					_log.error("Error getDBpediaMainRelations(" + resourceList.get(0) + "--" + resourceList.get(i)
							+ ") " + e.getMessage());
				}
			}
		}
		return relationList;
	}

	/**
	 * Find for relations between pairs of resources.
	 * 
	 * @param resourceList List of resources.
	 * @return List of relations found within a table between pairs of elements from resourceList.
	 */
	public List<TableRelation> getDBpediaRelationsCell(List<TableCell> resourceList)
	{
		List<TableRelation> relationList = new ArrayList<TableRelation>();
		List<Relation> relationFound = null;
		for (int i = 0; i < resourceList.size() - 1; i++)
		{
			for (int j = i + 1; j < resourceList.size(); j++)
			{
				if (resourceList.get(i).getResource() == null || resourceList.get(i).getResource().isEmpty()
						|| resourceList.get(j).getResource().isEmpty())
					; // do nothing
				else if (resourceList.get(i).getCol() == resourceList.get(j).getCol())
					; // do nothing same resource
				else
				{
					// System.out.println("RELATION BETWEEN: " + i + "-" + resourceList.get(i).getResource() + " ### " +
					// j + "-" + resourceList.get(j).getResource());
					try
					{
						// Look for relations between resource_i and resource_j
						relationFound = this.getRelations(resourceList.get(i).getResource(), resourceList.get(j)
								.getResource());
						if (relationFound != null && !relationFound.isEmpty())
						{
							for (Relation relation : relationFound)
							{
								relationList.add(new TableRelation(relation, resourceList.get(i).getCol(), resourceList
										.get(j).getCol(), resourceList.get(i).getRow()));
							}
						}
						// Look for relations between resource_j and resource_i
						relationFound = this.getRelations(resourceList.get(j).getResource(), resourceList.get(i)
								.getResource());
						if (relationFound != null && !relationFound.isEmpty())
						{
							for (Relation relation : relationFound)
							{
								relationList.add(new TableRelation(relation, resourceList.get(j).getCol(), resourceList
										.get(i).getCol(), resourceList.get(j).getRow()));
							}
						}
					} catch (IOException e)
					{
						_log.error("getDBpediaRelationsCell(" + resourceList.get(i) + "--" + resourceList.get(j) + ") "
								+ e.getMessage());
					}
				}
			}
		}
		return relationList;
	}

	/**
	 * Given two resources we want to know whether there exists relations between them.
	 * 
	 * @param resource1 First resource.
	 * @param resource2 Second resource.
	 * @return List of relations found between both resources.
	 * @throws IOException
	 */
	public List<Relation> getRelations(Resource resource1, Resource resource2) throws IOException
	{
		// Before resolve the redirects
		// String redir1 = rindex.getRedirect(resource1.getURI());
		// if (redir1 != null && !redir1.isEmpty())
		// resource1.setURI(redir1);
		// String redir2 = rindex.getRedirect(resource2.getURI());
		// if (redir2 != null && !redir2.isEmpty())
		//	resource2.setURI(redir2);

		List<Relation> relations = new ArrayList<Relation>();

		if (resource1.isEmpty() || resource2.isEmpty())
			return relations;

		// Look for relation between the pair
		// HashSet<String> relSet = dbp.getRelations(resource1.getURI(), resource2.getURI());
		// TEST
		HashSet<String> relSet = new HashSet<String>();
		relSet.add("http://dbpedia.org/ontology/producer");
		relSet.add("http://dbpedia.org/property/producer");
		relSet.add("http://dbpedia.org/ontology/director");
		relSet.add("http://dbpedia.org/property/director");

		if (relSet != null)
		{
			Resource relation = null;
			Relation predicate = null;
			for (String rel : relSet)
			{
				if (!rel.equals("http://dbpedia.org/ontology/wikiPageWikiLink"))
				{
					String[] split = rel.split("/");
					String relName = split[split.length - 1];
					relation = new Resource(rel, relName);
					predicate = new Relation(resource1, resource2, relation);
					relations.add(predicate);
				}
			}
		}

		if (relations == null || relations.size() == 0)
			_log.debug("None relation found! For: " + resource1.getURI() + " -- " + resource2.getURI());

		return relations;
	}

	/**
	 * Function to determine whether a triple already exists in DBpedia.
	 * 
	 * @param triple RDF triple with subject, predicate and object
	 * @return True if the relation exists; False otherwise.
	 */
	public boolean existsTriple(Triple triple)
	{
		// try
		// {
		// return dbp.exists(triple.getResource1().getURI(), triple.getRelation().getURI(), triple.getResource2()
		// .getURI());
		// } catch (IOException e)
		// {
		// _log.error("Error determining if exists triple " + triple + " Message: " + e.getMessage());
		// }
		return false;
	}

	/**
	 * Repair some links from HTML files.
	 * 
	 * @param link Link to repair.
	 * @return Link without rare symbols.
	 */
	public static String parseWikilink(String link)
	{
		String link_ = link;
		if (link.matches("\\[\\w*\\]"))
			return String.valueOf("");

		if (link.contains("#"))
			link_ = link_.substring(0, link_.indexOf("#"));

		link_ = link_.replace("_", " ");
		link_ = link_.replace("%27", "%");
		link_ = link_.replace("'", "%");
		try
		{
			link_ = URLDecoder.decode(link_, LocationConstants.ENCODING);
		} catch (Exception e)
		{
			_log.debug("Imposible unescape URL " + link_);
			return String.valueOf("");
		}

		return link_;
	}

	/**
	 * Main
	 * 
	 * @param args
	 * @throws MongoDBException
	 */
//	public static void main(String[] args)
//	{
//		DOMConfigurator.configure("./conf/log4j.xml");
//		DBpediaDAOTest sparql = new DBpediaDAOTest("local");
//
//		// // Test for getResourceURI
//		// String terms = "San Jose";
//		// // Königssee, CS Lewis, Mont Blanc, "Can't Cook, Won't Cook", Craig_Charles
//		// // Robert_A._'22Bob'22_Emerson
//		// // Jos'C3'A9_Luis_Rodr'C3'ADguez_Zapatero
//		// // 'C3'86thelfl'C3'A6d
//		// // 'C3'86lffl'C3'A6d'2C_wife_of_Edward_the_Elder
//		// // Mike'2C_Lu_'26_Og
//		//
//		// Resource reso1 = sparql.getResourceURI(terms, EntityType.THING);
//		// System.out.println(reso1);
//		// //
//		// terms = "United States";
//		// Resource reso2 = sparql.getResourceURI(terms, EntityType.THING);
//		// System.out.println(reso2);
//		// //
//		// List<Relation> rels = sparql.getRelations(reso1, reso2);
//		// for (Relation rel : rels)
//		// System.out.println(rel);
//
//		// // Test for getDBpediaResources
//		THashSet<String> anchorList = new THashSet<String>();
//		anchorList.add("\"USA\"");
//		// //anchorList.add("Big_X");
//		// // anchorList.add("Guszt'C3'A1v_(TV_series)");
//		// // anchorList.add("Jonny_Quest_(TV_series)");
//		// // anchorList.add("DoDo'2C_The_Kid_from_Outer_Space");
//		// // anchorList.add("Magilla_Gorilla");
//		// anchorList.add("Saint Helena");
//		// anchorList.add("Czech");
//		// // anchorList.add(StringUtils.capitalizeFirstLettersTokenizer("UNITED KINGDOM"));
//		// // anchorList.add(StringUtils.capitalizeFirstLettersTokenizer("CZECH"));
//		// List<Resource> resList = sparql.getDBpediaResources(anchorList, EntityType.OBJECT);
//		// System.out.println(resList);
//
//		// // Test subjects
//		// List<Resource> subjects = sparql.getResourceSubjects(new
//		// Resource("http://dbpedia.org/resource/United_States", "United States"));
//		// System.out.println(subjects);
//
//		// // Test subjects histogram
//		// List<Resource> resources = new ArrayList<Resource>();
//		// resources.add(new Resource("http://dbpedia.org/resource/United_Kingdom", "United Kingdom"));
//		// resources.add(new Resource("http://dbpedia.org/resource/People's_Republic_of_China",
//		// "People%27s Republic of China"));
//		// resources.add(new Resource("http://dbpedia.org/resource/Spain", "Spain"));
//		// resources.add(new Resource("http://dbpedia.org/resource/United_States", "United States"));
//		//
//		// THashMap<String, Integer> map = sparql.getSubjectHistogram(resources);
//		//
//		// List<CustomEntry> sortedList = MapFunctions.convertMapToList(map);
//		// Collections.sort(sortedList);
//		// System.out.println(sortedList);
//
//		// System.out.println(sparql.getResourceURI("Fianna F%C3%A1il", EntityType.OBJECT));
//
//		System.out.println("\n** Testing relations **");
//		HashSet<String> rels;
//		try
//		{
//			rels = dbp.getRelations("http://dbpedia.org/resource/Doctor_Who",
//					"http://dbpedia.org/resource/Matt_Smith_(actor)");
//
//			if (rels != null)
//				for (String rel : rels)
//				{
//					if (!rel.equals("http://dbpedia.org/ontology/wikiPageWikiLink"))
//						System.out.println(rel);
//				}
//			else
//				System.out.println("There is no relation");
//
//			System.out.println("\n** Testing redirect **");
//			System.out.println(dbp.getRedirect("http://dbpedia.org/resource/Paul_Henderson_(Australian_politician)"));
//		} catch (IOException e)
//		{
//			e.printStackTrace();
//		}
//
//		System.out.println("\n** Testing relations **");
//		List<TableCell> resourceList = new ArrayList<TableCell>();
//		// new Resource("http://dbpedia.org/resource/San_Jose,_California", "San_Jose,_California")
//		TableCell cell1 = new TableCell(sparql.getResourceURI("San_Jose%2C_California", EntityType.OBJECT), 1, 1);
//		TableCell cell2 = new TableCell(new Resource("http://dbpedia.org/resource/United_States", "Seville"), 1, 2);
//
//		System.out.println(cell1 + "##\t##" + cell2);
//
//		resourceList.add(cell1);
//		resourceList.add(cell2);
//		List<TableRelation> relations = sparql.getDBpediaRelationsCell(resourceList);
//		for (TableRelation rel : relations)
//			System.out.println(rel.getCellRelation());
//
//		// Test for properties
//		System.out.println("\n** Testing properties **");
//		System.out.println(sparql.getResourceURI("position", EntityType.PROPERTY));
//
//		try
//		{
//			sparql.finalize();
//		} catch (Throwable e)
//		{
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
//	}
}
