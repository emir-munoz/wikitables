package org.deri.exreta.demo.wrapper;

import java.util.ArrayList;

import org.deri.exreta.dal.weka.model.PredictionTriple;

/**
 * Export a set of predicted triples using different notations.
 * 
 * @author Emir Muñoz <emir@emunoz.org>
 * @since 2013-03-15
 * 
 */
public class WrapperRDFTriple
{
	/**
	 * Export using N3 Notation.
	 * 
	 * @param triples List of RDF triples to export.
	 * @param threshold Minimum threshold to satisfy.
	 * @return An N3 representation of given triples.
	 */
	public static String getN3Notation(ArrayList<PredictionTriple> triples, double threshold)
	{
		StringBuilder str = new StringBuilder();
		for (PredictionTriple tr : triples)
		{
			if (tr.getPclass() == -1.0 || tr.getConf() < threshold)
				continue;
			str.append(tr.getTriple().toString());
			str.append("\n");
		}
		str.trimToSize();
		return str.toString();
	}

	// TODO: to complete this
	public static String getXMLNotation(ArrayList<PredictionTriple> triples, double threshold)
	{
		StringBuilder str = new StringBuilder();
		str.append("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n<rdf:RDF\n xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\"\n xmlns:si=\"http://www.w3schools.com/rdf/\">\n");
		str.append("<rdf:Description rdf:about=\"\">\n");
		// here xml content

		str.append("</rdf:Description>\n");
		str.append("</rdf:RDF>\n");
		return str.toString();
	}

	// TODO: to complete this
	public static String getJSONNotation(ArrayList<PredictionTriple> triples, double threshold)
	{
		StringBuilder str = new StringBuilder();

		return str.toString();
	}
}
