package org.deri.exreta.dal.collections;

import java.util.Map;

import org.deri.exreta.dal.dbpedia.dto.Score;

@SuppressWarnings("rawtypes")
public class CustomEntry implements Comparable
{
	private Map.Entry	entry;

	public CustomEntry(Map.Entry entry)
	{
		this.entry = entry;
	}

	public Map.Entry getEntry()
	{
		return this.entry;
	}

	public int compareTo(CustomEntry anotherEntry)
	{
		Score thisScore = (Score) this.getEntry().getValue();
		//Double thisDoubleVal = (Double) (this.getEntry().getValue());
		Double thisDoubleVal = thisScore.getScore();
		double thisVal = thisDoubleVal.doubleValue();
		Score otherScore = (Score) anotherEntry.getEntry().getValue();
		//Double anotherDoubleVal = (Double) (anotherEntry.getEntry().getValue());
		Double anotherDoubleVal = otherScore.getScore();
		double anotherVal = anotherDoubleVal.doubleValue();
		return (thisVal < anotherVal ? 1 : (thisVal == anotherVal ? 0 : -1));
	}

	public int compareTo(Object o)
	{
		return compareTo((CustomEntry) o);
	}

	@Override
	public String toString()
	{
		StringBuilder str = new StringBuilder();
		str.append(this.getEntry().getKey()).append("=").append(this.getEntry().getValue().toString());
		return str.toString();
	}
}
