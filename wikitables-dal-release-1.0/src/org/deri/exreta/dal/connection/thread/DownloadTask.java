package org.deri.exreta.dal.connection.thread;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.concurrent.Callable;

import org.apache.log4j.Logger;

/**
 * Download a given URL and retrieve the <code>InputStream</code>.
 * 
 * @author Emir Munoz <emir@emunoz.org>
 * @version 0.1
 * @since 2012-09-25
 * 
 */
public class DownloadTask implements Callable<InputStream>
{
	private static final Logger	_log	= Logger.getLogger(DownloadTask.class);
	/** URL to download. */
	private String				loadURL;

	/**
	 * Constructor for DownloadTask class.
	 * 
	 * @param URL URL to download.
	 */
	public DownloadTask(final String URL)
	{
		this.loadURL = URL;
	}

	/**
	 * Method call for the thread.
	 */
	@Override
	public InputStream call() throws Exception
	{
		// perform the long running task here
		URL url;
		InputStream inStrm = null;
		try
		{
			url = new URL(loadURL);
			URLConnection urlConnect = url.openConnection();
			inStrm = urlConnect.getInputStream();
		} catch (MalformedURLException e)
		{
			_log.error("Malformed URL: " + loadURL + ", Message: " + e.getMessage());
		} catch (IOException e)
		{
			_log.error("Error trying to download file: " + loadURL + ", Message: " + e.getMessage());
		}

		return inStrm;
	}
}
