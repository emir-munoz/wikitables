package org.deri.exreta.dal.weka.model;

import org.deri.exreta.dal.dbpedia.dto.TripleString;

/**
 * Object used to send information to the interface.
 * 
 * @author Emir Munoz <emir@emunoz.org>
 * @version 0.1
 * @since 2013-03-05
 * 
 */
public class PredictionTriple
{
	/** RDF Triple */
	private TripleString	triple;
	/** Exists this triple in DBpedia? */
	private boolean			exists;
	/** Prediced class */
	private double			pclass;
	/** Confidence */
	private double			conf;

	public PredictionTriple(TripleString triple)
	{
		this.triple = triple;
	}

	public TripleString getTriple()
	{
		return triple;
	}

	public boolean isExists()
	{
		return exists;
	}

	public void setExists(boolean exists)
	{
		this.exists = exists;
	}

	public double getPclass()
	{
		return pclass;
	}

	public void setPclass(double pclass)
	{
		this.pclass = pclass;
	}

	public double getConf()
	{
		return conf;
	}

	public void setConf(double conf)
	{
		this.conf = conf;
	}

	@Override
	public String toString()
	{
		StringBuilder str = new StringBuilder();
		str.append("triple: ");
		str.append("&lt;" + this.triple.getSubj() + "&gt; ");
		str.append("&lt;" + this.triple.getPred() + "&gt; ");
		str.append("&lt;" + this.triple.getObj() + "&gt;");
		str.append(", exists: " + this.exists + ", class: " + this.pclass + ", conf: " + this.conf);
		return str.toString();
	}
}
