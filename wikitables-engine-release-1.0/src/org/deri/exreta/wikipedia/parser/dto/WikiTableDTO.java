package org.deri.exreta.wikipedia.parser.dto;

import gnu.trove.set.hash.THashSet;

/**
 * Class with the features extracted for a given table.
 * 
 * @author Emir Muñoz <emir@emunoz.org>
 * 
 */
public class WikiTableDTO
{
	private String				title;
	private int					nColumns;
	private int					nRows;
	private int					headersRow;
	private THashSet<String>	anchors;
	private String				caption;
	private int					externarLinks;
	private int					firstColumnLinks;
	private int					secondColumnLinks;
	private int					samePageLinks;

	public WikiTableDTO()
	{
		this.title = "";
		this.nColumns = 0;
		this.nRows = 0;
		this.headersRow = -1;
		this.anchors = new THashSet<String>();
		this.caption = "";
		this.externarLinks = 0;
		this.firstColumnLinks = 0;
		this.secondColumnLinks = 0;
		this.samePageLinks = 0;
	}

	public String getTitle()
	{
		return title;
	}

	public void setTitle(String title)
	{
		this.title = title;
	}

	public int getnColumns()
	{
		return nColumns;
	}

	public void setnColumns(int nColumns)
	{
		this.nColumns = nColumns;
	}

	public int getnRows()
	{
		return nRows;
	}

	public void setnRows(int nRows)
	{
		this.nRows = nRows;
	}

	public int getHeadersCol()
	{
		return headersRow;
	}

	public void setHeadersRow(int headersRow)
	{
		this.headersRow = headersRow;
	}

	public THashSet<String> getAnchors()
	{
		return anchors;
	}

	public void setAnchors(THashSet<String> anchors)
	{
		this.anchors = anchors;
	}

	public String getCaption()
	{
		return caption;
	}

	public void setCaption(String caption)
	{
		this.caption = caption;
	}

	public int getExternarLinks()
	{
		return externarLinks;
	}

	public void setExternarLinks(int externarLinks)
	{
		this.externarLinks = externarLinks;
	}

	public int getFirstColumnLinks()
	{
		return firstColumnLinks;
	}

	public void setFirstColumnLinks(int fColumnLinks)
	{
		this.firstColumnLinks = fColumnLinks;
	}

	public int getSecondColumnLinks()
	{
		return secondColumnLinks;
	}

	public void setSecondColumnLinks(int sColumnLinks)
	{
		this.secondColumnLinks = sColumnLinks;
	}

	public int getSamePageLinks()
	{
		return samePageLinks;
	}

	public void setSamePageLinks(int samePageLinks)
	{
		this.samePageLinks = samePageLinks;
	}

	@Override
	public String toString()
	{
		StringBuffer sf = new StringBuffer();
		sf.append(this.title + "\t" + this.nColumns + "\t" + this.nRows + "\t" + this.headersRow + "\t"
				+ this.firstColumnLinks + "\t" + this.secondColumnLinks + "\t" + this.externarLinks + "\t"
				+ this.samePageLinks + "\t" + (this.caption.isEmpty() ? 0 : 1) + "\t" + this.anchors + "\n");
		sf.trimToSize();
		return sf.toString();
	}

	public boolean isEmpty()
	{
		if (this.title != null)
			return this.title.isEmpty();
		else
			return true;
	}
}
