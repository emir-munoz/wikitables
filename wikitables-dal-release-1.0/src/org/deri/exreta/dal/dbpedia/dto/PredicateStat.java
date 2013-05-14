package org.deri.exreta.dal.dbpedia.dto;

/**
 * Structure that store the information about a predicate in DBpedia.
 * 
 * @author Emir Munoz <emir@emunoz.org>
 * @version 0.1
 * @since 2013-01-28
 * 
 */
public class PredicateStat
{
	private String	predicate;
	private int		times;
	private int		uniqSubj;
	private int		uniqObj;

	public PredicateStat()
	{
		predicate = "";
		times = 0;
		uniqSubj = 0;
		uniqObj = 0;
	}

	public PredicateStat(String entry)
	{
		String[] split = entry.split("\t");
		if (split.length != 4)
			new PredicateStat();
		else
		{
			this.predicate = split[0];
			this.times = Integer.parseInt(split[1]);
			this.uniqSubj = Integer.parseInt(split[2]);
			this.uniqObj = Integer.parseInt(split[3]);
		}
	}

	public String getPredicate()
	{
		return predicate;
	}

	public void setPredicate(String predicate)
	{
		this.predicate = predicate;
	}

	public int getTimes()
	{
		return times;
	}

	public void setTimes(int times)
	{
		this.times = times;
	}

	public int getUniqSubj()
	{
		return uniqSubj;
	}

	public void setUniqSubj(int uniqSubj)
	{
		this.uniqSubj = uniqSubj;
	}

	public int getUniqObj()
	{
		return uniqObj;
	}

	public void setUniqObj(int uniqObj)
	{
		this.uniqObj = uniqObj;
	}

	@Override
	public String toString()
	{
		return String.format("%s %s %s %s", this.predicate, this.times, this.uniqObj, this.uniqSubj);
	}
}
