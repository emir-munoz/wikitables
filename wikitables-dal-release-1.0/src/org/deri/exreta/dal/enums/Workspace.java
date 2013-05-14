package org.deri.exreta.dal.enums;

/**
 * Enum for workspaces to use.
 * 
 * @author Emir Muñoz <emir@emunoz.org>
 * @since 2013-03-11
 * 
 */
public enum Workspace
{
	SERVER("server"), LOCAL("local");

	private final String	type;

	Workspace(String type)
	{
		this.type = type;
	}

	public String getValue()
	{
		return type;
	}
}
