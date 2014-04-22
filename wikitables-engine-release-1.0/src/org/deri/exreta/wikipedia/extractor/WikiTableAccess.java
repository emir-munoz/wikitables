package org.deri.exreta.wikipedia.extractor;

import gnu.trove.set.hash.THashSet;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.deri.exreta.dal.dbpedia.dto.CellResources;
import org.deri.exreta.dal.dbpedia.dto.Resource;
import org.deri.exreta.dal.dbpedia.dto.TableCell;
import org.deri.exreta.dal.dbpedia.dto.TableHeader;
import org.deri.exreta.dal.dbpedia.query.DAOInterface;
import org.deri.exreta.dal.dbpedia.query.QueryBuilder.EntityType;
import org.deri.exreta.dal.main.LocationConstants;
import org.deri.exreta.wikipedia.parser.dto.CellType;
import org.jsoup.Jsoup;

import websphinx.Element;
import websphinx.Tag;
import cl.em.utils.string.EqualUtils;
import cl.em.utils.string.URLUTF8Encoder;
import cl.yahoo.webtables.features.FeaturesExtractorYData;

/**
 * Class to access and extract features froma a table.
 * 
 * @author Emir Muñoz <emir@emunoz.org>
 * @version 0.2.2
 * 
 */
public class WikiTableAccess
{
	private static final Logger			_log			= Logger.getLogger(WikiTableAccess.class);
	private DAOInterface				sparql			= null;
	private org.jsoup.nodes.Document	docu			= null;
	private int							realColNumber	= 0;
	public boolean						captionPresent	= false;

	/**
	 * Constructor.
	 * 
	 * @param sp Access to knowledge-base.
	 */
	public WikiTableAccess(DAOInterface sp)
	{
		this.sparql = sp;
	}

	/**
	 * Set real number of columns without colspan.
	 */
	public void setRealColNumber(int count)
	{
		this.realColNumber = count;
	}

	/**
	 * @return Get real number of columns without colspan.
	 */
	public int getRealColNumber()
	{
		return realColNumber;
	}

	/**
	 * Get values from the table headers.
	 * 
	 * @param <V>
	 * 
	 * @param extractor Pointer to the table.
	 * @return List with sets of resources corresponding to headers
	 */
	public List<TableHeader> getHeaders(final FeaturesExtractorYData extractor)
	{
		boolean isHeader = true;
		captionPresent = false;
		int row = 0;
		int equalCells = 0;

		try
		{
			// Check if the first row is header
			// Also check if the cells in the row have same content (e.g., a caption with colspan)
			Element firstCell = extractor.getHTMLElement(0, 0);
			Element eleChild = null;

			for (int col = 0; col < extractor.getColumns() && isHeader != false; col++)
			{
				eleChild = extractor.getHTMLElement(col, 0);

				if (!this.isHeader(eleChild))
					isHeader = false;
				// Check if current element is equals to the first cell
				if (EqualUtils.areEqual(eleChild.toText(), firstCell.toText()))
					equalCells += 1;
			}

			// If the ratio of equal cells is higher than 0.6
			if ((equalCells / extractor.getColumns()) > 0.6)
			{
				captionPresent = true;
				isHeader = false;
			}

			// If first row is not header, check second row
			if (!isHeader)
			{
				isHeader = true;
				eleChild = null;

				for (int col = 0; col < extractor.getColumns() && isHeader != false; col++)
				{
					eleChild = extractor.getHTMLElement(col, 1);
					if (!this.isHeader(eleChild))
						isHeader = false;
				}

				if (isHeader)
					row = 1;
			}
			_log.debug("Is there a caption present in the table? ==> " + captionPresent);
		} catch (Exception ex)
		{
			isHeader = false;
		}

		if (isHeader)
		{
			// return this.getHeaderResources(extractor, row, CellType.THEADER);
			return this.getHeaderString(extractor, row);
		} else
		{
			_log.debug("Error getting column headers.");
			return null;
		}
	}

	/**
	 * The number of row that contains the headers or -1 otherwise.
	 * 
	 * @param extractor Pointer to the table.
	 * @return Numer of row that contains the headers; -1 otherwise.
	 */
	public int headersRow(final FeaturesExtractorYData extractor)
	{
		boolean isHeader = true;
		int row = 0;
		// Check if the first row is header
		Element eleChild = null;
		for (int col = 0; col < extractor.getColumns() && isHeader != false; col++)
		{
			eleChild = extractor.getHTMLElement(col, 0);
			if (!this.isHeader(eleChild))
				isHeader = false;
		}
		// If first row is not header, check second row
		if (!isHeader)
		{
			isHeader = true;
			eleChild = null;
			for (int col = 0; col < extractor.getColumns() && isHeader != false; col++)
			{
				eleChild = extractor.getHTMLElement(col, 1);
				if (!this.isHeader(eleChild))
					isHeader = false;
			}
			if (isHeader)
				row = 1;
		}
		if (isHeader)
			return row;
		else
			return -1;
	}

	/**
	 * Get the list with sets of resources in a given number of row.
	 * 
	 * @param extractor Pointer to the table.
	 * @param row The given row to analyze.
	 * @param cellType Cell type to extract the content (e.g., Header, Body).
	 * @return A list with set of resources found in each cell.
	 */
	public List<CellResources> getRowResources(final FeaturesExtractorYData extractor, int row, CellType cellType)
	{
		List<CellResources> columnCellValues = new ArrayList<CellResources>();
		int colSize = extractor.getColumns();
		int realColsize = 0;

		Element eleChild = null;
		int skipColspan = 0;

		// System.out.println("COLS=" + colSize);
		for (int col = 0; col < colSize; col++)
		{
			// System.out.println("COL=" + col + "#ROW=" + row + "#ELE=" + extractor.getHTMLElement(col, row));

			// skip the following cells in the same row if there is a colspan
			if (skipColspan > 0)
			{
				skipColspan--;
				continue;
			}
			eleChild = extractor.getHTMLElement(col, row);
			realColsize++; // increase real column counter

			// try to get the colspan size if it exists
			try
			{
				skipColspan = Integer.valueOf(eleChild.getHTMLAttribute("colspan"));
				skipColspan--; // skip the next n-1 columns
			} catch (Exception ex)
			{
			} finally
			{
				columnCellValues.add(new CellResources(this.getCellContent(eleChild, cellType), col));
			}
		}
		this.setRealColNumber(realColsize);

		return columnCellValues;
	}

	/**
	 * Get the list with sets of resources in the header.
	 * 
	 * @param extractor Pointer to the table.
	 * @param row The given header row to analyze.
	 * @param cellType Cell type to extract the content (e.g., Header, Body)
	 * @return A list with set of resources found in each cell of the header.
	 */
	public List<THashSet<Resource>> getHeaderResources(final FeaturesExtractorYData extractor, int row,
			CellType cellType)
	{
		List<THashSet<Resource>> columnValues = new ArrayList<THashSet<Resource>>();
		int colSize = extractor.getColumns();

		Element eleChild = null;
		for (int col = 0; col < colSize; col++)
		{
			eleChild = extractor.getHTMLElement(col, row);
			columnValues.add(this.getCellContent(eleChild, cellType));
		}
		this.setRealColNumber(colSize);
		return columnValues;
	}

	/**
	 * Get the list of headers as string.
	 * 
	 * @param extractor Pointer to the table.
	 * @param row The given header row to analyze.
	 * @return A list of strings present in the table headers.
	 */
	public List<TableHeader> getHeaderString(final FeaturesExtractorYData extractor, int row)
	{
		List<TableHeader> columnValues = new ArrayList<TableHeader>();
		int colSize = extractor.getColumns();
		int skipColspan = 0;

		Element eleChild = null;
		for (int col = 0; col < colSize; col++)
		{
			// skip the following cells in the same row if there is a colspan
			if (skipColspan > 0)
			{
				skipColspan--;
				continue;
			}
			eleChild = extractor.getHTMLElement(col, row);

			// try to get the colspan size if it exists
			try
			{
				skipColspan = Integer.valueOf(eleChild.getHTMLAttribute("colspan"));
				skipColspan--;
			} catch (Exception ex)
			{
			} finally
			{
				columnValues.add(new TableHeader(eleChild.toText(), col));
			}
		}
		return columnValues;
	}

	/**
	 * Get the list with sets of resources in a given number of row.
	 * 
	 * @param extractor Pointer to the table.
	 * @param row The given row to analyze.
	 * @param cellType Cell type to extract the content (e.g., Header, Body).
	 * @return A list with set of resources found in each cell.
	 */
	public int[] getInnerCellContent(final FeaturesExtractorYData extractor, int row, int col, CellType cellType)
	{
		// when resource is the article title
		if (col == -1)
			return new int[] { -1, 0, 0, 0, 0 };

		Element eleChild = extractor.getHTMLElement(col, row);

		// when there is something wrong with the cell
		if (eleChild == null || eleChild.toHTML() == null)
			return new int[] { 0, 0, 0, 0, 0 };

		int bullets = 0;
		int length = 0;
		int resources = 0;
		int hasFormat = 0;
		int multipleLine = 0;
		try
		{
			// Analyze the number of resources
			String html = eleChild.toHTML().replace("|", ""); // StringUtils.trimToEmpty(eleChild.toHTML());
			docu = (org.jsoup.nodes.Document) Jsoup.parse(html);
			for (org.jsoup.nodes.Element link : docu.select("a"))
			{
				if (link != null)
				{
					// external link
					if (link.hasClass("externallink"))
						continue; // skip
					else if (link.hasClass("external text"))
						continue; // skip
					else if (link.hasClass("external"))
						continue; // skip

					// Clean URL/path
					String linkHref = filterLink(link.attr("href"));

					// add as resource only if linkHref is not empty
					if (!StringUtils.isEmpty(linkHref) && linkHref.length() < LocationConstants.MAX_LENGTH_TXT)
						resources++;
				}
			}
			// count the list
			bullets = bullets + docu.select("ul").size();
			// count the enumerations
			bullets = bullets + docu.select("ol").size();
			// count font tags
			hasFormat = hasFormat + docu.select("font").size();
			hasFormat = hasFormat + docu.select("b").size();
			hasFormat = hasFormat + docu.select("i").size();
			hasFormat = hasFormat + docu.select("th").size();
			hasFormat = hasFormat + docu.select("small").size();
			// count multiple-lines
			multipleLine = multipleLine + docu.select("br").size();
		} catch (Exception ex)
		{
			_log.debug("Error getting properties from the cell");
		}
		try
		{
			// Tex length
			String text = StringUtils.trimToEmpty(eleChild.toText());
			length = text.length();
		} catch (Exception ex)
		{
			_log.debug("Error getting properties from the cell");
		}

		// System.out.println("### " + eleChild.toHTML() + "--" + resources + "--" + length + "--" + bullets);
		return new int[] { resources, length, bullets, hasFormat, multipleLine };
	}

	/**
	 * Function to determine whether a cell is a header (TH tag) or not.
	 * 
	 * @param eleChild Pointer to the cell.
	 * @return true if eleChild is a TH tag; false otherwise.
	 */
	public boolean isHeader(final Element eleChild)
	{
		if (eleChild.getTagName() == Tag.TH)
			return true;
		else if (eleChild.getTagName() == Tag.TD && eleChild.getChild().getTagName() == Tag.B)
			return true;
		else
			return false;
	}

	/**
	 * Function to recover the resources within a unique cell.
	 * 
	 * @param eleChild Pointer to the cell.
	 * @param col Column extracted.
	 * @param cellType Type of cell. i.e., header, body.
	 * @return Set of resources in the cell.
	 * @throws MongoDBException
	 */
	@SuppressWarnings({ "deprecation" })
	public THashSet<Resource> getCellContent(final Element eleChild, CellType cellType)
	{
		boolean isLink = false;
		THashSet<Resource> resourceSet = new THashSet<Resource>();

		try
		{
			String html = eleChild.toHTML().replace("|", "");
			String cellText = StringUtils.trimToEmpty(eleChild.toText());

			if (html.isEmpty())
				return resourceSet;

			// List<String> pattern = new ArrayList<String>();
			// pattern.add(cl.em.utils.string.StringUtils.unAccent(eleChild.toText()));
			// this.getDataProg((ArrayList<String>) pattern);

			// Extract the <a href=""...> elements
			Resource resource = null;
			docu = (org.jsoup.nodes.Document) Jsoup.parse(html);
			for (org.jsoup.nodes.Element link : docu.select("a"))
			{
				if (link != null)
				{
					isLink = true;

					// skip external link
					if (link.hasClass("externallink"))
						continue; // skip
					else if (link.hasClass("external text"))
						continue; // skip
					else if (link.hasClass("external"))
						continue; // skip

					// clean URL/path
					String linkHref = filterLink(link.attr("href"));

					// add as resource only if linkHref is not empty
					// ***** has links to the same page
					if (!StringUtils.isEmpty(linkHref) && linkHref.length() < LocationConstants.MAX_LENGTH_TXT)
					{
						// changelog: 2012-09-26 # if resource is empty
						resource = sparql.getResourceURI(linkHref, EntityType.OBJECT);
						// If it is a wikipedia link but there is no page created
						if (resource != null && resource.isEmpty())
							resource = new Resource("", cellText);

						// If resource is not null add to output
						if (resource != null)
							resourceSet.add(resource);
					}
				}
			}
			if (!isLink || resourceSet.isEmpty())
			{
				// To get the dataprog
				cellText = cellText.replace("|", "");
				String cellTextOri = cellText;

				// cellText = cellText.replaceAll("[\t]+", "").replaceAll("[\n]+", "");
				if (!StringUtils.escape(cellText).equals("\\u00A0"))
				{
					cellText = URLUTF8Encoder.encode(cellText.replace("–", "-")).replace("+", " ");
					resource = new Resource();
					if (cellText.length() > 2 && cellType == CellType.THEADER)
						resource = sparql.getResourceURI(cellText, EntityType.PROPERTY);
					if (resource != null && !resource.isEmpty())
						resourceSet.add(resource); // add new resource
					else
						// add literal
						resourceSet.add(new Resource("", "\"" + cellTextOri + "\"^^" + getDataType(cellTextOri)));
				}
			}
		} catch (Exception ex)
		{
			_log.debug("Error reading cell content");
		}
		return resourceSet;
	}

	/**
	 * Filter an href link.
	 * 
	 * @param linkHref Link to filter.
	 * @return Filtered link.
	 */
	public String filterLink(String linkHref)
	{
		// String linkOuterH = link.outerHtml(); //
		// "<a href="http://example.com"><b>example</b></a>"

		if (linkHref.contains("#")) // same page link
			linkHref = linkHref.substring(0, linkHref.indexOf("#"));
		if (linkHref.endsWith(".html"))
			linkHref = linkHref.substring(0, linkHref.length() - 5);
		if (linkHref.startsWith("/wiki/"))
		{
			linkHref = linkHref.substring(6);
			linkHref = linkHref.replace("%", "%27");
		}
		if (linkHref.startsWith("Wikipedia:") || linkHref.startsWith("File:") || linkHref.startsWith("Image'3A"))
			return String.valueOf("");

		if (linkHref.startsWith("http://dbpedia.org/resource//w/index.php?title=")
				|| linkHref.contains("/w/index.php?title=") // || linkHref.contains("en.wikipedia.org/wiki/")
				|| linkHref.contains("https://"))
			return String.valueOf("");

		return linkHref;
	}

	/**
	 * Function to get the data type of a given string.
	 * 
	 * @param text Original text.
	 * @return Data type of the input text.
	 */
	public String getDataType(String text)
	{
		if (text.equals("t") || text.equals("f") || text.equalsIgnoreCase("true") || text.equalsIgnoreCase("false"))
			return "xsd:boolean";
		else if (text.startsWith("http://") || text.startsWith("www."))
			return "xsd:anyURI";
		return "xsd:string";
	}

	/**
	 * Function to filter a list of row cell by column.
	 * 
	 * @param list List to filter.
	 * @param column Column position.
	 * @return
	 */
	public List<TableCell> filterByColumn(List<TableCell> list, int column)
	{
		List<TableCell> filtered = new ArrayList<TableCell>();
		for (TableCell cell : list)
			if (cell.getCol() == column)
				filtered.add(cell);

		return filtered;
	}

	public List<TableCell> filterRowCells(List<TableCell> list)
	{
		List<TableCell> filtered = new ArrayList<TableCell>();

		return filtered;
	}

}
