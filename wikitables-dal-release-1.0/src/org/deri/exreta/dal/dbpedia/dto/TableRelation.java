package org.deri.exreta.dal.dbpedia.dto;

/**
 * Structure that represents a relation between two columns within a table.
 * 
 * @author Emir Munoz <emir@emunoz.org>
 * @version 0.1
 * @since 2012-09-24
 * 
 */
public class TableRelation implements Comparable<Object>
{
	/** Description of the relation found. */
	private Relation	cellRelation;
	/** First column involve. */
	private int			col1;
	/** Second column involve. */
	private int			col2;
	/** Score of the relation */
	private Score		score;
	/** Row where was found */
	private int			row;

	public TableRelation()
	{
		this.cellRelation = null;
		this.col1 = -1;
		this.col2 = -1;
		this.score = new Score();
		this.row = -1;
	}

	public TableRelation(Relation relation, int col1, int col2, int row)
	{
		this.cellRelation = relation;
		this.col1 = col1;
		this.col2 = col2;
		this.score = new Score();
		this.row = row;
	}

	public Relation getCellRelation()
	{
		if (this.cellRelation != null)
			return this.cellRelation;
		else
			return new Relation();
	}

	public void setCellRelation(Relation cellRelation)
	{
		this.cellRelation = cellRelation;
	}

	public int getCol1()
	{
		return col1;
	}

	public void setCol1(int col1)
	{
		this.col1 = col1;
	}

	public int getCol2()
	{
		return col2;
	}

	public void setCol2(int col2)
	{
		this.col2 = col2;
	}

	public Score getScore()
	{
		return score;
	}

	public void setScore(Score score)
	{
		this.score = score;
	}

	public int getRow()
	{
		return row;
	}

	public void setRow(int row)
	{
		this.row = row;
	}

	@Override
	public String toString()
	{
		StringBuilder str = new StringBuilder();
		str.append("{").append("[").append(this.row).append("#").append(this.col1).append("/").append(this.col2).append("] ")
				.append(this.cellRelation).append("}");
		str.trimToSize();
		return str.toString();
	}

	@Override
	public int compareTo(Object o)
	{
		TableRelation aThat = (TableRelation) o;
		if (this.cellRelation.getRelation().equals(aThat.getCellRelation().getRelation()) && this.col1 == aThat.col1
				&& this.col2 == aThat.col2)
			return 1;
		return 0;
	}

	/**
	 * Convert a given relation name into a TableRelation object.
	 * @param str Relation name (wit parameters).
	 * @param score Score of the relation.
	 * @return A TableRelation object.
	 */
	public TableRelation convertRelation(String str, Score score)
	{
		if (str != null)
		{
			String[] parts = str.split("#");
			try
			{
				if (parts.length != 4)
					return new TableRelation();

				this.cellRelation = new Relation(parts[0]);
				this.col1 = Integer.parseInt(parts[1]);
				this.col2 = Integer.parseInt(parts[2]);
				this.score = score;
			} catch (NumberFormatException ex)
			{
				return new TableRelation();
			}
			return this;
		} else
		{
			return new TableRelation();
		}
	}
}
