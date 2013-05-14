package org.deri.exreta.dal.dbpedia.dto;

public class TableHeader
{
	/** Header text */
	private String	hText;
	/** Column position */
	private int		hColumn;

	public TableHeader(String text, int col)
	{
		this.hText = text;
		this.hColumn = col;
	}

	public String gethText()
	{
		return hText;
	}

	public void sethText(String hText)
	{
		this.hText = hText;
	}

	public int gethColumn()
	{
		return hColumn;
	}

	public void sethColumn(int hColumn)
	{
		this.hColumn = hColumn;
	}

	@Override
	public String toString()
	{
		return this.hText + "@" + this.hColumn;
	}
}
