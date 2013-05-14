package org.deri.exreta.dal.dbpedia.dto;

/**
 * Class that represents a triple <s, p, o>.
 * 
 * @author Emir Munoz <emir@emunoz.org>
 * @version 0.1
 * @since 2012-10-24
 * 
 */
public class Triple
{
	private Resource	resource1;
	private Resource	relation;
	private Resource	resource2;

	public Triple()
	{
		this.resource1 = new Resource();
		this.resource2 = new Resource();
		this.relation = new Resource();
	}

	public Triple(Resource res1, Resource res2, Resource rel)
	{
		this.resource1 = res1;
		this.resource2 = res2;
		this.relation = rel;
	}

	public Resource getRelation()
	{
		return relation;
	}

	public void setRelation(Resource relation)
	{
		this.relation = relation;
	}

	public Resource getResource1()
	{
		return resource1;
	}

	public void setResource1(Resource resource1)
	{
		this.resource1 = resource1;
	}

	public Resource getResource2()
	{
		return resource2;
	}

	public void setResource2(Resource resource2)
	{
		this.resource2 = resource2;
	}

	@Override
	public String toString()
	{
		StringBuilder sb = new StringBuilder();
		if (this.resource1 == null || this.resource2 == null || this.resource1.isFullEmpty()
				|| this.resource2.isFullEmpty())
			return ""; // empty

		if (!this.resource1.isEmpty())
			sb.append("<").append(this.resource1.getURI()).append(">").append(" ");
		else
			sb.append(this.resource1.getName()).append(" ");
		sb.append("<").append(this.relation.getURI()).append(">").append(" ");
		if (!this.resource2.isEmpty())
			sb.append("<").append(this.resource2.getURI()).append(">").append(" .");
		else
			sb.append(this.resource2.getName()).append(" .");
		sb.trimToSize();

		return sb.toString();
	}
}
