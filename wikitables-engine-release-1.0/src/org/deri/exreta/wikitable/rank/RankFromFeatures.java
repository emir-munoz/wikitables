package org.deri.exreta.wikitable.rank;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;

public class RankFromFeatures
{
	public static final String[]	COMMON_FNS	= new String[] { "./triples/triples-all-uniq-new-0001-500-shuf-01",
			"./triples/triples-all-uniq-new-0001-500-shuf-02", "./triples/triples-all-uniq-new-0001-500-shuf-03",
			"./triples/triples-all-uniq-new-0001-500-shuf-00" };

	/** Options of ranking */
	public static enum RankOpt
	{
		RANK_Naive, RANK_wt, RANK_freq, RANK_Distance, RANK_header
	}

	public static List<String[]> readFeaturesData(String resultsFile) throws IOException
	{
		List<String[]> featureList = new ArrayList<String[]>();

		BufferedReader br = new BufferedReader(new FileReader(resultsFile));
		String line = null;
		while ((line = br.readLine()) != null)
		{
			line = line.trim();
			if (!line.isEmpty())
			{
				String[] split = line.split("\t");
				if (split.length < 14)
				{
					throw new IOException("Cannot read line (expecting 14 columns) " + line);
				}
				featureList.add(split);
			}
		}
		return featureList;
	}

	private static double getRankScore(String[] features, RankOpt rankOpt, Properties props)
	{
		double rank = 0.0;
		// Naive ranking function
		if (rankOpt.equals(RankOpt.RANK_Naive))
		{
			double rank_tmp = Double.valueOf(features[12]).doubleValue();
			double rows = Double.valueOf(features[3]).doubleValue();
			// System.out.println(rank + "\t" + rows + "\t" + rank / (rows * 1.0));
			rank = rank_tmp / (rows * 1.0);
		}
		if (rankOpt.equals(RankOpt.RANK_Distance))
		{
			String value = "";
			String triple = features[11];
			String pred = triple.split("> <")[1].toString();
			value = features[6].substring(1, features[6].length() - 1); // remove '{' and '}'
			String[] resources = value.split(", ");
			HashSet<String> resource_set = new HashSet<String>();

			double rank_naive = Double.valueOf(features[12]).doubleValue();
			double rows = Double.valueOf(features[3]).doubleValue();
			// System.out.println(rank + "\t" + rows + "\t" + rank / (rows * 1.0));
			rank_naive = rank_naive / (rows * 1.0);
			rank = rank_naive;

			boolean exit = false;
			for (String resource : resources)
			{
				if (resource.equals("#PAGE_TITLE#"))
				{
					rank = rank_naive;
					exit = true;
					break; // nothing to found
				}
				String[] res_parts = resource.split(" <");
				if (res_parts.length == 2 && !res_parts[1].isEmpty() && res_parts[1].length() > 1)
					resource_set.add(res_parts[1].substring(0, res_parts[1].length() - 1));
			}
			float similarity = 0.0f;
			float max_simi = 0.0f;
			if (!exit && resource_set.size() > 0)
			{
				for (String reso : resource_set)
				{
					similarity = Similarity.distanceStr(reso, pred);
					if (similarity > max_simi)
						max_simi = similarity;
				}
				double factor = (Double) props.get("factor");
				// System.out.println(rank_naive + "\t" + max_simi);
				rank = factor * rank_naive + (1 - factor) * max_simi;
			}
		}
		return rank;
	}

	private static void getRanking(RankOpt rankOpt, Properties props) throws IOException
	{
		for (String resultsFile : COMMON_FNS)
		{
			List<String[]> featureList = readFeaturesData(resultsFile);

			BufferedWriter out = new BufferedWriter(new FileWriter(resultsFile + ".curve"));
			// System.out.println(featureList.size() + " lines of features readed");

			for (String[] featureLine : featureList)
			{
				double rankScore = getRankScore(featureLine, rankOpt, props);
				System.out.println(rankScore);
				out.write(rankScore + "\n");
			}

			out.flush();
			out.close();
		}
	}

	public static void main(String[] args) throws IOException
	{
		double threshold = 0.0;
		Properties props = new Properties();
		props.put("threshold", threshold);
		props.put("factor", 0.75);

		getRanking(RankOpt.RANK_Distance, props);
	}
}
