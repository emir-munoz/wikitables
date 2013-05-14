package org.deri.exreta.dal.dbpedia.dto;

/**
 * Structure that represents a dbpedia resource.
 * 
 * @author Emir Munoz <emir@emunoz.org>
 * @version 0.1
 * @since 2012-09-24
 * 
 */

public class Resource
{
	/** Unified Resource Identifier (URI). */
	private String	URI;
	/** Name of the resource. */
	private String	name;

	public Resource()
	{
		this.URI = "";
		this.name = "";
	}

	public Resource(String URI, String name)
	{
		this.URI = URI;
		this.name = name;
	}

	public String getURI()
	{
		return URI;
	}

	public void setURI(String URI)
	{
		this.URI = URI;
	}

	public String getName()
	{
		return name;
	}

	public void setName(String name)
	{
		this.name = name;
	}

	@Override
	public String toString()
	{
		return this.name + " <" + this.URI + ">";
	}

	@Override
	public boolean equals(Object obj)
	{
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Resource other = (Resource) obj;
		if (URI == null)
		{
			if (other.URI != null)
				return false;
		} else if (!URI.equals(other.URI))
			return false;
		if (name == null)
		{
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		return true;
	}

	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = 1;
		result = prime * result + ((URI == null) ? 0 : URI.hashCode());
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		return result;
	}

	/**
	 * Check whether this resources has assigned an URI.
	 * 
	 * @return true if URI is null; false otherwise.
	 */
	public boolean isEmpty()
	{
		if (this.URI != null)
			return this.URI.isEmpty();
		else
			return true;
	}

	public boolean isFullEmpty()
	{
		if ((this.URI == null || this.URI.isEmpty()) && this.name.isEmpty())
			return true;
		else
			return false;
	}
}
