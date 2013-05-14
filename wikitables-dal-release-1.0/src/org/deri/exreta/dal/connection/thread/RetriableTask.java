package org.deri.exreta.dal.connection.thread;

import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import org.apache.log4j.Logger;

/**
 * This class is a wrapper for a Callable that adds retry functionality.
 * The user supplies an existing Callable, a maximum number of tries,
 * and optionally a Logger to which exceptions will be logged. Calling
 * the call() method of RetriableTask causes the wrapped object's call()
 * method to be called, and any exceptions thrown from the inner call()
 * will cause the entire inner call() to be repeated from scratch, as
 * long as the maximum number of tries hasn't been exceeded.
 * InterruptedException and CancellationException are allowed to
 * propogate instead of causing retries, in order to allow cancellation
 * by an executor service etc.
 * 
 * @param <T> the return type of the call() method
 */
public class RetriableTask<T> implements Callable<T>
{
	private final Callable<T>	_wrappedTask;
	private final int			_tries;
	private final Logger		_log;

	/**
	 * Creates a new RetriableTask around an existing Callable. Supplying
	 * zero or a negative number for the tries parameter will allow the
	 * task to retry an infinite number of times -- use with caution!
	 * 
	 * @param taskToWrap the Callable to wrap
	 * @param tries the max number of tries
	 * @param log a Logger to log exceptions to (null == no logging)
	 */
	public RetriableTask(final Callable<T> taskToWrap, final int tries, final Logger log)
	{
		_wrappedTask = taskToWrap;
		_tries = tries;
		_log = log;
	}

	/**
	 * Invokes the wrapped Callable's call method, optionally retrying
	 * if an exception occurs. See class documentation for more detail.
	 * 
	 * @return the return value of the wrapped call() method
	 */
	public T call() throws Exception
	{
		int triesLeft = _tries;
		while (true)
		{
			try
			{
				return _wrappedTask.call();
			} catch (final InterruptedException e)
			{
				// We don't attempt to retry these
				throw e;
			} catch (final CancellationException e)
			{
				// We don't attempt to retry these either
				throw e;
			} catch (final Exception e)
			{
				triesLeft--;

				// Are we allowed to try again?
				if (triesLeft == 0) // No -- rethrow
					throw e;

				// Yes -- log and allow to loop
				if (_log != null)
					_log.error("Caught exception, retrying... Error was: " + e.getMessage());
			}
		}
	}
}
