package org.deri.exreta.dal.dbpedia.dto;

import gnu.trove.set.hash.THashSet;

/**
 * Resources found in a given column cell.
 * 
 * @author Emir Muñoz <emir@emunoz.org>
 * 
 */
public class CellResources
{
	/** Set of resources found */
	private THashSet<Resource>	resoSet;
	/** Column where were found */
	private int					column;

	public CellResources(THashSet<Resource> resources, int col)
	{
		this.resoSet = resources;
		this.column = col;
	}

	public THashSet<Resource> getResoSet()
	{
		return resoSet;
	}

	public void setResoSet(THashSet<Resource> resoSet)
	{
		this.resoSet = resoSet;
	}

	public int getColumn()
	{
		return column;
	}

	public void setColumn(int col)
	{
		this.column = col;
	}
}
