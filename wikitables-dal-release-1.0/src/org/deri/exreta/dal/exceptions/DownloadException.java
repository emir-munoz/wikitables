package org.deri.exreta.dal.exceptions;

/**
 * Exceptions for MONGO DB.
 * 
 * @author Emir Munoz <emir@emunoz.org>
 * @version 0.1
 * @since 2013-03-8
 *
 */
public class DownloadException extends Exception
{
	private static final long	serialVersionUID	= -6796307249360977041L;

	public DownloadException()
	{
	}

	public DownloadException(String msg)
	{
		super(msg);
	}
}
