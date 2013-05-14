package org.deri.exreta.dal.dbpedia.dto;

import cl.em.utils.string.EqualUtils;

/**
 * Structure that represents a relation between two dbpedia resources.
 * 
 * @author Emir Munoz <emir@emunoz.org>
 * @version 0.1
 * @since 2012-09-24
 * 
 */
public class Relation
{
	/** First resource involve. */
	private Resource	resource1;
	/** Second resource involve. */
	private Resource	resource2;
	/** Relation between resource1 and resource2. */
	private Resource	relation;

	public Relation()
	{
		this.resource1 = new Resource();
		this.resource2 = new Resource();
		this.relation = new Resource();
	}

	public Relation(Resource res1, Resource res2, Resource rel)
	{
		this.resource1 = res1;
		this.resource2 = res2;
		this.relation = rel;
	}

	public Relation(String name)
	{
		this.relation = new Resource(name, name);
		this.resource1 = new Resource();
		this.resource2 = new Resource();
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

	public Resource getRelation()
	{
		return relation;
	}

	public void setRelation(Resource relation)
	{
		this.relation = relation;
	}

	public String getRelationName()
	{
		return this.relation.getName();
	}

	public String getRelationURI()
	{
		if (this.relation != null)
			return this.relation.getURI();
		else
			return "##NO_URI##";
	}

	@Override
	public String toString()
	{
		StringBuilder sb = new StringBuilder();
		sb.append("<").append(this.resource1.getURI()).append(">").append(" ");
		sb.append("<").append(this.getRelation().getURI()).append(">").append(" ");
		sb.append("<").append(this.resource2.getURI()).append(">").append(" .");
		sb.trimToSize();

		return sb.toString();
	}

	@Override
	public boolean equals(Object aThat)
	{
		// check for self-comparison
		if (this == aThat)
			return true;

		// use instanceof instead of getClass here for two reasons
		// 1. if need be, it can match any supertype, and not just one class;
		// 2. it renders an explicit check for "that == null" redundant, since
		// it does the check for null already - "null instanceof [type]" always
		// returns false. (See Effective Java by Joshua Bloch.)
		if (!(aThat instanceof Relation))
			return false;

		// cast to native object is now safe
		Relation that = (Relation) aThat;

		// now a proper field-by-field evaluation can be made
		return EqualUtils.areEqual(this.resource1, that.resource1)
				&& EqualUtils.areEqual(this.resource2, that.resource2)
				&& EqualUtils.areEqual(this.relation, that.relation);
	}

}
