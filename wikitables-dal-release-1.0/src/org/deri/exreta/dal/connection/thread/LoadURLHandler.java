package org.deri.exreta.dal.connection.thread;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.apache.log4j.Logger;
import org.deri.exreta.dal.main.LocationConstants;


/**
 * Time out controller for page downloads.
 * 
 * @author Emir Munoz <emir@emunoz.org>
 * @version 0.1
 * @since 2012-09-25
 *        Changelog:
 *        - 2013-03-07 Modification of the logging messages
 * 
 */
public class LoadURLHandler
{
	private static final Logger	_log	= Logger.getLogger(LoadURLHandler.class);

	/**
	 * Function to try to download a given URL once and return the file as InputStream.
	 * 
	 * @param loadURL URL to download.
	 * @param timeoutSecs Set time out.
	 * @return InputStream to the downloaded file.
	 */
	public InputStream downloadURL(final String loadURL, int timeoutSecs)
	{
		InputStream inStrm = null;

		ExecutorService executor = Executors.newSingleThreadExecutor();
		Future<InputStream> future = executor.submit(new DownloadTask(loadURL));

		try
		{
			inStrm = future.get(timeoutSecs, TimeUnit.SECONDS);
		} catch (TimeoutException e)
		{
			_log.error("Download time out for URL: " + loadURL);
		} catch (InterruptedException e)
		{
			_log.error("Download interrupted while downloading URL: " + loadURL + ", Message: " + e.getMessage());
		} catch (ExecutionException e)
		{
			_log.error("Error in the execution of the download of URL: " + loadURL + ", Message: " + e.getMessage());
		}
		executor.shutdownNow();

		return inStrm;
	}

	/**
	 * Function to try to download a given URL as many times as indicated in configuration file and return the file as
	 * InputStream.
	 * 
	 * @param loadURL URL to download.
	 * @param timeoutSecs Set time out.
	 * @return InputStream to the downloaded file.
	 */
	public InputStream downloadURLRetry(final String loadURL, final int timeoutSecs)
	{
		InputStream inStrm = null;
		ExecutorService pool = Executors.newCachedThreadPool();
		DownloadTask download = new DownloadTask(loadURL);

		// Make it try up to three times
		RetriableTask<InputStream> retriable1 = new RetriableTask<InputStream>(download, LocationConstants.MAX_TRIES,
				_log);
		Collection<Callable<InputStream>> tasks = new ArrayList<Callable<InputStream>>();
		// add all the RetriableTasks to a Collection
		tasks.add(retriable1);
		// and execute them all on the thread pool
		List<Future<InputStream>> results = null;
		try
		{
			results = pool.invokeAll(tasks, timeoutSecs, TimeUnit.SECONDS);
		} catch (InterruptedException e)
		{
			_log.error("Download interrupted while downloading URL: " + loadURL + ", Message: " + e.getMessage());
		}

		for (Future<InputStream> result : results)
		{
			// Un-retried exceptions will pop out here
			try
			{
				inStrm = result.get();
			} catch (InterruptedException e)
			{
				_log.error("Download interrupted for URL: " + loadURL + ", Message: " + e.getMessage());
			} catch (ExecutionException e)
			{
				_log.error("Error in the execution downloading URL: " + loadURL + ", Message: " + e.getMessage());
			}
		}
		pool.shutdownNow();

		return inStrm;
	}
}
