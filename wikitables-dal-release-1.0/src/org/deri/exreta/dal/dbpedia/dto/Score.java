package org.deri.exreta.dal.dbpedia.dto;

public class Score
{
	private int		potentialRel;
	private int		maxPotentialRel;
	private double	score;
	private int		uniqPotRel;

	public Score()
	{
		this.potentialRel = 0;
		this.maxPotentialRel = 0;
		this.score = 0.0;
		this.uniqPotRel = 0;
	}

	public Score(int potential, int max, double score, int unique)
	{
		this.potentialRel = potential;
		this.maxPotentialRel = max;
		this.score = score;
		this.uniqPotRel = unique;
	}

	public Score(double score)
	{
		this.score = score;
	}

	public int getPotentialRel()
	{
		return potentialRel;
	}

	public void setPotentialRel(int potentialRel)
	{
		this.potentialRel = potentialRel;
	}

	public int getMaxPotentialRel()
	{
		return maxPotentialRel;
	}

	public void setMaxPotentialRel(int maxPotentialRel)
	{
		this.maxPotentialRel = maxPotentialRel;
	}

	public double getScore()
	{
		return score;
	}

	public void setScore(double score)
	{
		this.score = score;
	}

	public int getUniqPotRel()
	{
		return uniqPotRel;
	}

	public void setUniqPotRel(int uniqPotRel)
	{
		this.uniqPotRel = uniqPotRel;
	}

	@Override
	public String toString()
	{
		return String.valueOf(this.score);
	}
}
