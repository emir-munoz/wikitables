package org.deri.exreta.wikitable.rank;

import net.ricecode.similarity.DiceCoefficientStrategy;
import net.ricecode.similarity.JaroWinklerStrategy;
import net.ricecode.similarity.SimilarityStrategy;
import net.ricecode.similarity.StringSimilarityService;
import net.ricecode.similarity.StringSimilarityServiceImpl;

/**
 * Similarity between two strings.
 * 
 * @author Emir Muñoz <emir@emunoz.org>
 * 
 */
public class Similarity
{
	public static float distanceStr(String A, String B)
	{
		SimilarityStrategy strategy = new JaroWinklerStrategy();
		StringSimilarityService service1 = new StringSimilarityServiceImpl(strategy);

		SimilarityStrategy strategy2 = new DiceCoefficientStrategy();
		StringSimilarityService service2 = new StringSimilarityServiceImpl(strategy2);

		float resultservice1 = (float) service1.score(A, B);
		float resultservice2 = (float) service2.score(A, B);

		return Math.min(resultservice2, resultservice1);
	}
}
