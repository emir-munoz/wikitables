package org.deri.exreta.dal.dbpedia.query;

import org.deri.exreta.dal.dbpedia.dto.Resource;
import org.deri.exreta.dal.main.LocationConstants;

import cl.em.utils.string.URLUTF8Encoder;

/**
 * Builder for SPARQL queries over different end-points.
 * 
 * @author Emir Munoz <emir@emunoz.org>
 * @version 0.1
 * @since 2012-08-07
 * 
 */
public class QueryBuilder
{
	/** Fixed source end-point. */
	private Source	source;
	/** Fixed language. */
	private String	lang;

	public QueryBuilder(Source dbpedia, String lang)
	{
		this.source = dbpedia;
		this.lang = lang;
	}

	/** Type of query */
	public static enum QueryType
	{
		GET_RESOURCE, GET_RELATION, GET_SUBJECT
	}

	/** Type of entity */
	public static enum EntityType
	{
		OBJECT, PROPERTY
	}

	/** Type of source */
	public static enum Source
	{
		DBpedia, Bing;

		@Override
		public String toString()
		{
			if (this == DBpedia)
				return LocationConstants.DBPEDIA_ENDPOINT;
			else if (this == Bing)
				return LocationConstants.BING_ENDPOINT;
			else
				return "";
		}
	}

	/**
	 * Function to build a SPARQL query to get a URI for a given resource.
	 * 
	 * @param terms Terms for search.
	 * @param format Format for the answer.
	 * @return The corresponding SPARQL query.
	 * 
	 *         SELECT DISTINCT ?s WHERE {
	 *         ?s <http://www.w3.org/2000/01/rdf-schema#label> "Saint Helena"@en .
	 *         FILTER (!regex(str(?s), '^http://dbpedia.org/resource/Category:')).
	 *         FILTER (!regex(str(?s), '^http://sw.opencyc.org/')).
	 *         }
	 */
	public String getURIOfResourceStr(String terms, String format)
	{
		String graph = "&default-graph-uri=http://dbpedia.org";
		String forma = "&format=" + format; // HTML, XML, RDF/XML, CSV, NTriples
		String query = "SELECT DISTINCT ?s WHERE { " + "?s <http://www.w3.org/2000/01/rdf-schema#label> \"%s\"%s ."
				+ "FILTER (!regex(str(?s), '^http://dbpedia.org/resource/Category:'))."
				+ "FILTER (!regex(str(?s), '^http://sw.opencyc.org/'))." + "}";
		if (terms.contains("\"") || terms.contains("%22"))
			query = String.format(query, URLUTF8Encoder.encode(terms), lang); // URLUTF8Encoder.unescape(terms)
		else
			query = String.format(query, URLUTF8Encoder.unescape(terms), lang);
		String completeUrl = source + "?query=" + URLUTF8Encoder.encode(query) + graph + forma;

		return completeUrl;
	}

	/**
	 * Function to build a SPARQL query to get a URI for a given resource avoiding disambiguation.
	 * 
	 * @param terms Terms for search.
	 * @param format Format for the answer.
	 * @return The corresponding SPARQL query.
	 * 
	 *         SELECT DISTINCT ?s ?o WHERE {
	 *         ?s <http://www.w3.org/2000/01/rdf-schema#label> "Czech"@en .
	 *         FILTER (!regex(str(?s), '^http://dbpedia.org/resource/Category:')).
	 *         FILTER (!regex(str(?s), '^http://sw.opencyc.org/')).
	 *         OPTIONAL {
	 *         ?s dbpedia-owl:wikiPageDisambiguates ?o .
	 *         ?o a dbpedia-owl:Country .
	 *         OPTIONAL {
	 *         ?o dbpedia-owl:dissolutionDate ?date .
	 *         }
	 *         FILTER(!bound(?date)).
	 *         }
	 *         }
	 *         ORDER BY DESC(?s)
	 * 
	 *         The use of order by is because we want the resources before the properties.
	 */
	public String getURIOfResourceDisCountryStr(String terms, String format)
	{
		String graph = "&default-graph-uri=http://dbpedia.org";
		String forma = "&format=" + format; // HTML, XML, RDF/XML, CSV, NTriples
		String query = "SELECT DISTINCT ?s ?o WHERE { " + "?s <http://www.w3.org/2000/01/rdf-schema#label> \"%s\"%s ."
				+ "FILTER (!regex(str(?s), '^http://dbpedia.org/resource/Category:'))."
				+ "FILTER (!regex(str(?s), '^http://sw.opencyc.org/'))."
				+ "FILTER (!regex(str(?s), '^http://dbpedia.org/class/yago/'))."
				+ "OPTIONAL { ?s dbpedia-owl:wikiPageDisambiguates ?o . ?o a dbpedia-owl:Country ."
				+ "OPTIONAL { ?o dbpedia-owl:dissolutionDate ?date . }" + "FILTER(!bound(?date)). }"
				+ "} ORDER BY DESC(?s)";
		if (terms.contains("\"") || terms.contains("%22"))
			query = String.format(query, URLUTF8Encoder.encode(terms), lang); // URLUTF8Encoder.unescape(terms)
		else
			query = String.format(query, URLUTF8Encoder.unescape(terms), lang);
		String completeUrl = source + "?query=" + URLUTF8Encoder.encode(query) + graph + forma;

		return completeUrl;
	}

	/**
	 * Function to build a SPARQL query to get the types for a given resource.
	 * 
	 * @param URI The resource URI.
	 * @param format Format for the answer.
	 * @return The corresponding SPARQL query.
	 * 
	 *         SELECT DISTINCT ?type WHERE { <http://dbpedia.org/page/Czech_Republic> rdf:type ?type }
	 */
	public String getTypeOfResourceStr(String URI, String format)
	{
		String graph = "&default-graph-uri=http://dbpedia.org";
		String forma = "&format=" + format; // HTML, XML, RDF/XML, CSV, NTriples
		String query = "SELECT DISTINCT ?type WHERE { %s rdf:type ?type }";
		query = String.format(query, URI);
		String completeUrl = source + "?query=" + URLUTF8Encoder.encode(query) + graph + forma;

		return completeUrl;
	}

	/**
	 * Function to build a SPARQL query to get the subjects for a given resource.
	 * 
	 * @param URI The resource URI.
	 * @param format Format for the answer.
	 * @return The corresponding SPARQL query.
	 * 
	 *         SELECT DISTINCT ?subject WHERE { <http://dbpedia.org/resource/United_States> dcterms:subject ?subject. }
	 */
	public String getSubjectOfResourceStr(String URI, String format)
	{
		String graph = "&default-graph-uri=http://dbpedia.org";
		String forma = "&format=" + format; // HTML, XML, RDF/XML, CSV, NTriples
		String query = "SELECT DISTINCT ?s WHERE { <%s> dcterms:subject ?s }";
		query = String.format(query, URI);
		String completeUrl = source + "?query=" + URLUTF8Encoder.encode(query) + graph + forma;

		return completeUrl;
	}

	/**
	 * Function to build a SPARQL query to retrieve relations between two resources.
	 * 
	 * @param resource1 First resource involved.
	 * @param resource2 Second resource involved.
	 * @param format Format for the answer.
	 * @return The corresponding SPARQL query.
	 * 
	 *         SELECT DISTINCT ?p1 ?p2 {
	 *         { <> ?p1 <> }
	 *         UNION
	 *         { <> ?p2 <> }
	 *         }
	 */
	public String getRelationBetweenStr(Resource resource1, Resource resource2, String format)
	{
		String graph = "&default-graph-uri=http://dbpedia.org";
		String forma = "&format=" + format; // JSON, HTML, XML, RDF/XML, CSV, NTriples
		String query = "SELECT DISTINCT ?p1 ?p2 { { <%s> ?p1 <%s> } " + "UNION" + " { <%s> ?p2 <%s> } }";
		query = String.format(query, resource1.getURI(), resource2.getURI(), resource2.getURI(), resource1.getURI());
		String completeUrl = source + "?query=" + URLUTF8Encoder.encode(query) + graph + forma;

		return completeUrl;
	}

	/**
	 * Function to determine is a given triple already exists in DBpedia.org
	 * 
	 * @param resource1 First resource involved.
	 * @param resource2 Second resource involved.
	 * @param relation Relation between the resources.
	 * @param format Format for the answer.
	 * @return Query to determine whether there exists a given relation.
	 * 
	 *         ASK WHERE
	 *         {
	 *         <http://dbpedia.org/resource/San_Jose,_California>
	 *         <http://dbpedia.org/ontology/country>
	 *         <http://dbpedia.org/resource/United_States> .
	 *         }
	 */
	public String existsTipleStr(String resource1, String resource2, String relation, String format)
	{
		String graph = "&default-graph-uri=http://dbpedia.org";
		String forma = "&format=" + format; // JSON, HTML, XML, RDF/XML, CSV, NTriples
		String query = "ASK WHERE { <%s> <%s> <%s> . }";
		query = String.format(query, resource1, relation, resource2);
		String completeUrl = source + "?query=" + URLUTF8Encoder.encode(query) + graph + forma;

		return completeUrl;
	}
}
