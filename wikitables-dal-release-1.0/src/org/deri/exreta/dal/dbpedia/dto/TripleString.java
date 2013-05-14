package org.deri.exreta.dal.dbpedia.dto;

/**
 * Class that represents a triple <s, p, o> using strings.
 * 
 * @author Emir Munoz <emir@emunoz.org>
 * @version 0.1
 * @since 2013-03-21
 * 
 */
public class TripleString
{
	private String	subj;
	private String	pred;
	private String	obj;

	public TripleString()
	{
		this.subj = "";
		this.pred = "";
		this.obj = "";
	}

	public TripleString(String s, String p, String o)
	{
		this.subj = s;
		this.pred = p;
		this.obj = o;
	}

	public TripleString(TripleString triple)
	{
		this.subj = triple.subj;
		this.pred = triple.pred;
		this.obj = triple.obj;
	}

	public String getSubj()
	{
		return subj;
	}

	public void setSubj(String subj)
	{
		this.subj = subj;
	}

	public String getPred()
	{
		return pred;
	}

	public void setPred(String pred)
	{
		this.pred = pred;
	}

	public String getObj()
	{
		return obj;
	}

	public void setObj(String obj)
	{
		this.obj = obj;
	}

	@Override
	public String toString()
	{
		StringBuilder str = new StringBuilder();
		str.append("<" + this.getSubj() + "> ");
		str.append("<" + this.getPred() + "> ");
		str.append("<" + this.getObj() + "> .");
		return str.toString();
	}
}
