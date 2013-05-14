package org.deri.exreta.dal.dbpedia.dto;

/**
 * Structure that represents a cell within a table.
 * 
 * @author Emir Munoz <emir@emunoz.org>
 * @version 0.1
 * @since 2012-09-24
 * 
 */
public class TableCell
{
	/** Resource contained in the cell */
	private Resource	resource;
	/** Row position */
	private int			nRow;
	/** Column position */
	private int			nCol;

	public TableCell()
	{
	}

	public TableCell(Resource resource, int nRow, int nCol)
	{
		this.resource = resource;
		this.nRow = nRow;
		this.nCol = nCol;
	}

	public Resource getResource()
	{
		return resource;
	}

	public void setResource(Resource resource)
	{
		this.resource = resource;
	}

	public int getRow()
	{
		return nRow;
	}

	public void setRow(int row)
	{
		this.nRow = row;
	}

	public int getCol()
	{
		return nCol;
	}

	public void setCol(int nCol)
	{
		this.nCol = nCol;
	}

	@Override
	public String toString()
	{
		StringBuilder str = new StringBuilder();
		str.append("{").append(this.resource).append(" (").append(this.nCol).append(",").append(this.nRow).append(")}");
		return str.toString();
	}
}
