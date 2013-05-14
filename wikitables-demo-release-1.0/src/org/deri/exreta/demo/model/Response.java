package org.deri.exreta.demo.model;

import java.util.ArrayList;

import org.deri.exreta.dal.weka.model.PredictionTriple;

/**
 * Class to transfer the response to the interface.
 * 
 * @author Emir Muñoz <emir@emunoz.org>
 * 
 */
public class Response
{
	private String						type;
	private String						text;
	private String						elapsed;
	private ArrayList<PredictionTriple>	triples;

	public String getType()
	{
		return type;
	}

	public void setType(String type)
	{
		this.type = type;
	}

	public String getText()
	{
		return text;
	}

	public void setText(String text)
	{
		this.text = text;
	}

	public String getElapsed()
	{
		return elapsed;
	}

	public void setElapsed(String elapsed)
	{
		this.elapsed = elapsed;
	}

	public ArrayList<PredictionTriple> getTriples()
	{
		return triples;
	}

	public void setTriples(ArrayList<PredictionTriple> triples)
	{
		this.triples = triples;
	}
}
