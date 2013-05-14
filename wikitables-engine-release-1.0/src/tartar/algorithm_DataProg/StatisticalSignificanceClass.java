package tartar.algorithm_DataProg;

// KRISTINE LERMAN - DATAPROG algorithm implementation
// JAIR 2003 article

public class StatisticalSignificanceClass
{
	private final static double	p			= 0.47047;
	private final static double	a_1			= 0.34802;
	private final static double	a_2			= -0.09587;
	private final static double	a_3			= 0.74785;
	private final static double	TRESHOLD	= 0.05;	// Threshold of 0.05 corresponds to a
														// 3-sigma event.

	private static double erf(double x)
	{
		double t = 1. / (1. + p * x);
		double e = Math.exp(-x * x);
		e = 1 - (a_1 * t + a_2 * t * t + a_3 * t * t * t) * e;
		return e;
	}

	// Calculates probability of observing number of events in the range // mu + z*sigma and mu -
	// z*sigma, where z is as defined below.
	@SuppressWarnings("unused")
	private static double intervalProb(double mu, double sigma, double num_observed)
	{
		if (sigma == 0.)
			sigma = 0.00005; // to avoid div/0
		double z = (num_observed - mu) / sigma;
		double f = 1.;
		if (z < 0)
			f = -1.;
		return erf(f * z / Math.sqrt(2.));
	}

	private static double cumulativeProb(double mu, double sigma, double num_observed)
	{
		if (sigma == 0.)
			sigma = 0.00000001; // to avoid div/0
		double z = (num_observed - mu) / sigma;
		double f = 1.;
		if (z < 0)
			f = -1.;
		return (0.5 * (1 - f * erf(f * z / Math.sqrt(2.))));
	}

	private static double cumulative_prob2(int num_tries, double prob_class, int num_observed)
	{
		double mu = (double) num_tries * prob_class;
		double sigma = Math.sqrt(mu * (1. - prob_class));
		return cumulativeProb(mu, sigma, (double) num_observed);
	}

	public static boolean significant(int num_tries, // number of candidates
			int num_observed, // number of instances of observed class
			double prob_class) // probability of wildcard occurrence
	{
		return significant(num_tries, num_observed, prob_class, TRESHOLD);
	}

	/**
	 * Is the observed number of instances of a class a significant occurrence? Using binomial
	 * distribution measure of significance at a given significance level. Threshold of 0.05
	 * corresponds to a 3-sigma event.
	 */
	public static boolean significant(int num_tries, // number of candidates
			int num_observed, // number of instances of observed class
			double prob_class, // probability of wildcard occurrence
			double threshold) // significant threshold
	{
		if (num_observed == 0)
			return false;
		double Q_X = cumulative_prob2(num_tries, prob_class, num_observed);
		// if probability of num_observed purely by chance is small, the events are significant
		if (Q_X < threshold)
			return true;
		return false;
	}

	public static boolean isSignificant(int num_tries, // number of extracts
			int num_observed, // number of instances of observed class
			double prob_class, // probability of wildcard occurrence
			double threshold)
	{ // significant threshold

		if (num_observed == 0)
			return false;
		if (num_observed == num_tries)
			return true;
		// Steve said to subtract one here to account for reduction in
		// the number of degrees of freedom:
		--num_observed;

		double Q_X = cumulative_prob2(num_tries, prob_class, num_observed);
		// if probability of num_observed purely by chance is small,
		// the events are significant:
		if (Q_X < threshold)
			return true;
		else
			return false;
	}
}
