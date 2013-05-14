package org.deri.exreta.wikitable.rank;

import gnu.trove.map.hash.THashMap;
import gnu.trove.set.hash.THashSet;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import org.apache.log4j.Logger;
import org.deri.exreta.dal.collections.CustomEntry;
import org.deri.exreta.dal.collections.MapFunctions;
import org.deri.exreta.dal.dbpedia.dto.Resource;
import org.deri.exreta.dal.dbpedia.dto.Score;
import org.deri.exreta.dal.dbpedia.dto.TableRelation;
import org.deri.exreta.dal.main.LocationConstants;

import cl.yahoo.webtables.utils.MathUtils;

/**
 * Ranking of relations extracted based on its frequency.
 * 
 * @author Emir Munoz <emir@emunoz.org>
 * @version 1.1
 * @since 2012-09-21
 * 
 */
public class RankRelation
{
	/** Options of ranking */
	public static enum RankOpt
	{
		RANK_Naive, RANK_wt, RANK_freq, RANK_distance, RANK_header
	}

	private final Logger				_log				= Logger.getLogger(RankRelation.class);
	private final double				factor				= 0.8;
	private List<TableRelation>			relationList		= null;
	private THashMap<String, Double>	relationHistogram	= null;
	private Set<String>					keySet				= null;
	private THashMap<String, Score>		ranking				= null;
	private List<CustomEntry>			filteredList		= null;

	public RankRelation(List<TableRelation> relations)
	{
		relationList = relations;
	}

	/**
	 * Create a histogram for the current relations.
	 * 
	 * @return Corresponding histogram.
	 */
	private void buildHistogram()
	{
		// count frequency for each relation
		relationHistogram = new THashMap<String, Double>();

		// System.out.println(relationList);

		// Count the frequency of each relation in the list
		for (TableRelation relation : relationList)
		{
			// comparison based on the predicate not in the subject or object.
			// considering the columns involved in the relation
			String source = (relation.getCol1() == -1 || relation.getCol2() == -1) ? "h" : "b";
			String auxKey = relation.getCellRelation().getRelation().getURI() + "#" + relation.getCol1() + "#"
					+ relation.getCol2() + "#" + source;
			if (!relationHistogram.containsKey(auxKey))
				relationHistogram.put(auxKey, new Double(1.0));
			else
			{
				double pastFreq = (Double) relationHistogram.get(auxKey).doubleValue();
				relationHistogram.remove(auxKey);
				relationHistogram.put(auxKey, new Double(pastFreq + 1.0));
			}
		}

		// Set the keys of the histogram
		keySet = relationHistogram.keySet();
	}

	public void makeRanking(RankOpt rank, Properties props)
	{
		buildHistogram();
		ranking = new THashMap<String, Score>();

		if (rank.equals(RankOpt.RANK_Naive))
		{
			/* Ranking based in the number of rows */
			// update the rank dividing by the number of rows
			// Set<String> keySet = relationHistogram.keySet();
			for (String key : keySet)
			{
				// System.out.println(key + "\t" + relationHistogram.get(key));
				double freqNow = relationHistogram.get(key).doubleValue(); // frequency of the relation
				int n_t = (Integer) props.get("n_t"); // number of rows
				double score = new Double(freqNow / n_t);
				ranking.put(key, new Score(score));
			}
		} else if (rank.equals(RankOpt.RANK_wt))
		{
			// Set<String> keySet = relationHistogram.keySet();
			for (String key : keySet)
			{
				double f_r_t = relationHistogram.get(key).doubleValue(); // frequency of the relation
				int n_t = (Integer) props.get("n_t"); // number of rows
				int C_t = relationList.size(); // total number of relations in table t
				int R_t = relationHistogram.size(); // total number of different relations in table t
				int P_r_t = freqRelationRows(key); // number of rows where relation r exists in table t
				double score = 0.0;
				if (R_t != 1)
					score = new Double((f_r_t * C_t) / (R_t * n_t * n_t * P_r_t * (C_t - f_r_t)));
				else
					score = new Double(C_t / (R_t * n_t * n_t * P_r_t));
				ranking.put(key, new Score(score));
			}
		} else if (rank.equals(RankOpt.RANK_freq))
		{
			// Set<String> keySet = relationHistogram.keySet();
			int max = 0;
			// int n_t = (Integer) props.get("n_t"); // number of rows
			// compute the maximum ratio: frequency / number_of_rows
			int[] allPotentialArr = new int[keySet.size()];
			int i = 0;
			for (String key : keySet)
			{
				// System.out.println("Evaluating key " + key);
				// double P_r_t = new Double(freqRelationRows(key) / (double) n_t); // ratio relation in rows
				int _potential = potentialRelationsHeld(key);
				allPotentialArr[i++] = _potential;
				if (_potential > max)
					max = _potential;
			}

			i = 0;
			for (String key : keySet)
			{
				// double P_r_t = allPotentialArr[i++]; // ratio relation in rows
				int potential = allPotentialArr[i++];
				// double score = new Double(1 / (1 + Math.abs(Math.log10(P_r_t / (double) max))));
				double score = 0.0;
				if (max > 0.0)
					score = new Double(potential / (double) max);
				ranking.put(key, new Score(potential, max, MathUtils.round(score, LocationConstants.DECIMAL_PLACE), 0));
			}
		} else if (rank.equals(RankOpt.RANK_distance))
		{
			// Set<String> keySet = relationHistogram.keySet();
			for (String key : keySet)
			{
				double f_r_t = relationHistogram.get(key).doubleValue();
				int count = 0;
				@SuppressWarnings("unchecked")
				List<THashSet<Resource>> tHeaders = (List<THashSet<Resource>>) props.get("headers");
				if (tHeaders != null)
				{
					for (THashSet<Resource> setResource : tHeaders)
					{
						String[] split = key.split("#");
						String relName = split[0];
						for (Resource resource : setResource)
							if (resource.getURI().equals(relName))
								count++;
					}
				}
				double score = 0.0;
				score = count * f_r_t;
				ranking.put(key, new Score(score));
			}
		} else if (rank.equals(RankOpt.RANK_header))
		{
			// Set<String> keySet = relationHistogram.keySet();
			double max = 0.0;
			int n_t = (Integer) props.get("n_t"); // number of rows
			// compute the maximum ratio: frequency / number_of_rows
			for (String key : keySet)
			{
				int freq = freqRelationRows(key);
				double P_r_t = new Double(freq / (double) n_t); // ratio relation in rows
				if (P_r_t > max)
					max = P_r_t;
			}

			// Compute the distance edition between the relation and the headers of both columns
			String relName = "";
			int col1 = 0, col2 = 0;
			double score = 0.0;
			double P_r_t = 0.0;
			float similarity = 0.0f, max_simi = 0.0f;
			for (String key : keySet)
			{
				// System.out.println(key);
				String[] split = key.split("#");
				try
				{
					if (split.length != 5 || split[1].equals("-1") || split[2].equals("-1"))
						continue; // score will be 0.0 in case of error
					else
					{
						relName = split[0]; // name of the relation
						col1 = Integer.parseInt(split[1]); // first column involve in the relation
						col2 = Integer.parseInt(split[2]); // second column involve in the relation
						P_r_t = new Double(freqRelationRows(key) / (double) n_t); // ratio relation in rows
						similarity = 0.0f;
						max_simi = 0.0f;
						@SuppressWarnings("unchecked")
						List<THashSet<Resource>> tHeaders = (List<THashSet<Resource>>) props.get("headers");
						if (tHeaders != null)
						{
							// recover resources from both columns
							THashSet<Resource> setResource = tHeaders.get(col1);
							setResource.addAll(tHeaders.get(col2));

							for (Resource resource : setResource)
							{
								if (!resource.isEmpty())
								{
									similarity = Similarity.distanceStr(resource.getURI(), relName);
									if (similarity > max_simi)
										max_simi = similarity;
									// System.out.println("Similarity local: " + similarity);
								}
							}
							// similarity = new Float(similarity / (double) setResource.size()); // avg similarity
							// System.out.println("Max similarity: " + max_simi);
						}

						// System.out.println("Relation: " + key);
						// System.out.println("Score components: P_r_t:" + P_r_t + " max:" + max + " similarity:" +
						// max_simi);
						score = new Double(factor * (1 / (double) (1 + Math.abs(Math.log10(P_r_t / (double) max))))
								+ (1 - factor) * max_simi);
					}
				} catch (NumberFormatException ex)
				{
					_log.error("Error ranking function for key \"" + key + "\"");
				}
				ranking.put(key, new Score(score)); // save current score
			}
		}
		// order and filter the elements
		double threshold = (Double) props.get("threshold");
		sortAndFilterRelations(threshold);
	}

	/**
	 * Function to compute number of rows where relation relName exists in a table.
	 * 
	 * @param relName Relation name.
	 * @return Number of occurrences in table.
	 */
	public int freqRelationRows(String relName)
	{
		String[] split = relName.split("#");
		if (split.length != 4)
			return 0;

		relName = split[0];
		int col1 = Integer.parseInt(split[1]); // first column involve in the relation
		int col2 = Integer.parseInt(split[2]); // second column involve in the relation
		int count = 0;
		for (TableRelation relation : relationList)
			if (relation.getCellRelation().getRelation().getURI().equals(relName) && relation.getCol1() == col1
					&& relation.getCol2() == col2)
				count++;
		return count;
	}

	/**
	 * Count the potential relations between two columns.
	 * 
	 * @param relName Relation name.
	 * @return Number of relations between col1 and col2.
	 */
	public int potentialRelationsHeld(String relName)
	{
		String[] split = relName.split("#");
		if (split.length != 4)
			return 0;

		int col1 = Integer.parseInt(split[1]); // first column involve in the relation
		int col2 = Integer.parseInt(split[2]); // second column involve in the relation
		int count = 0;
		for (TableRelation relation : relationList)
			if (relation.getCol1() == col1 && relation.getCol2() == col2)
				count++;
		return count;
	}

	public int uniquePotentialRelationsHeld(String relName)
	{
		String[] split = relName.split("#");
		if (split.length != 4)
			return 0;

		relName = split[0];
		int col1 = Integer.parseInt(split[1]); // first column involve in the relation
		int col2 = Integer.parseInt(split[2]); // second column involve in the relation
		THashSet<String> appSet = new THashSet<String>();
		for (TableRelation relation : relationList)
		{
			if (!appSet.contains(relName + "#" + col1 + "#" + col2 + "#" + relation.getRow())
					&& relation.getCellRelation().getRelation().getURI().equals(relName) && relation.getCol1() == col1
					&& relation.getCol2() == col2)
			{
				// System.out.println(relName + "#" + col1 + "#" + col2 + "#" + relation.getRow());
				appSet.add(String.valueOf(relName + "#" + col1 + "#" + col2 + "#" + relation.getRow()));
			}
		}
		return appSet.size();
	}

	public int uniqueRelationsHeld(String relName)
	{
		String[] split = relName.split("#");
		if (split.length != 4)
			return 0;

		relName = split[0];
		int col1 = Integer.parseInt(split[1]); // first column involve in the relation
		int col2 = Integer.parseInt(split[2]); // second column involve in the relation
		THashSet<String> appSet = new THashSet<String>();
		for (TableRelation relation : relationList)
		{
			if (relation.getCellRelation().getRelation().getURI().equals(relName) && relation.getCol1() == col1
					&& relation.getCol2() == col2)
				appSet.add(String.valueOf(relation.getCellRelation().getResource1() + "#" + relName + "#"
						+ relation.getCellRelation().getResource2() + "#" + col1 + "#" + col2));
		}
		return appSet.size();
	}

	@SuppressWarnings("unchecked")
	private void sortAndFilterRelations(double threshold)
	{
		List<CustomEntry> orderedHist = MapFunctions.convertMapToList(ranking);
		Collections.sort(orderedHist);

		filteredList = new ArrayList<CustomEntry>();

		// Here manage the threshold for select the relations to extend DBpedia.
		for (CustomEntry entry : orderedHist)
		{
			Score thisScore = (Score) entry.getEntry().getValue();
			if (thisScore.getScore() >= threshold)
			{
				filteredList.add(entry);
			}
		}
	}

	/**
	 * Get sorted and filtered list of relations.
	 * 
	 * @return Retrieve the sorted and filtered list of relations.
	 */
	public List<CustomEntry> getFilteredRelations()
	{
		return filteredList;
	}

	// propose other rankings based on weights. For example if the header is included in the resource.
}
