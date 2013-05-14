package org.deri.exreta.wikipedia.parser.test;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.nio.charset.Charset;
import java.util.HashSet;
import java.util.Iterator;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;

import cl.yahoo.webtables.utils.MemoryUtils;

public class ParseTriplesOutput
{
	// logger
	private static final Logger	_log	= Logger.getLogger(ParseTriplesOutput.class.getSimpleName());

	public static void main(String[] args)
	{
		ParseTriplesOutput parse = new ParseTriplesOutput();
		String input_path = args[0];
		// String input_path = "./triples/triples-all-uniq-new-0001-500-shuf-03";
		String output_path = input_path + "-normalized";
		try
		{
			String[] dataParts = null;
			StringBuilder withFVLine = new StringBuilder();
			int lineCounter = 0;
			String line = null;
			String fv = null;
			BufferedReader input = new BufferedReader(new FileReader(input_path));
			BufferedWriter output = new BufferedWriter(new FileWriter(output_path), 32768);
			// Read and convert format for all the features extracted for a triple
			while ((line = input.readLine()) != null)
			{
				lineCounter++;
				dataParts = line.trim().split("\t");
				// Extract and convert the features to SVM_light format
				fv = parse.convertFormat(dataParts);
				// Write new FV to output file
				if (fv != null && !fv.isEmpty())
				{
					String classe = "+1";
					withFVLine.append(classe).append(" ");
					withFVLine.append(fv);
					withFVLine.append(lineCounter).append("\n");

					// Print to the file every 500 lines
					if (lineCounter % 500 == 0)
					{
						// System.out.println(lineCounter + " lines processed");
						output.write(withFVLine.toString());
						output.flush();

						withFVLine.delete(0, withFVLine.length());
						withFVLine.trimToSize();
						// MemoryUtils.freeMemory();
					}
				}
			}
			// Write remaining lines
			System.out.println(lineCounter + " triples processed");
			_log.info(lineCounter + " triples processed");
			output.write(withFVLine.toString().trim());
			output.write("\n");
			output.flush();
			_log.info("### End of the FV generation ###");

			// Freeing memory
			withFVLine.delete(0, withFVLine.length() - 1);
			withFVLine.trimToSize();
			output.close();
			input.close();
			MemoryUtils.freeMemory();
		} catch (Exception e)
		{
			_log.warn("Error reading triples file");
		}
	}

	public String convertFormat(String[] line)
	{
		SortedMap<Integer, String> outcome = new TreeMap<Integer, String>();
		StringBuilder format = new StringBuilder();
		try
		{
			int feature_counter = 1;
			String value = "";
			String triple = line[11];
			// Discard those triples that contains literals
			String pattern = "<[^>]*> <[^>]*> <[^>]*> .";
			Pattern tripPattern = Pattern.compile(pattern);
			Matcher matcher = tripPattern.matcher(triple);
			if (!matcher.find())
				return null;
			String pred = triple.split("> <")[1].toString();
			for (int i = 1; i < line.length; i++)
			{
				// which columns number the subject/object came from
				if (i == 5)
				{
					String[] cols = line[i].split("#");
					if (cols.length == 2)
					{
						value = cols[0];
						outcome.put(feature_counter++, Float.valueOf(value).toString());

						value = cols[1];
						outcome.put(feature_counter, Float.valueOf(value).toString());
					}
				} // end if (i == 5)
				else if (i == 6)
				{
					// (set of) headers for subject
					// {"Drivers"^^xsd:string <>, car <http://dbpedia.org/property/car>, engine
					// <http://dbpedia.org/property/engine>}
					value = line[i].substring(1, line[i].length() - 1); // remove '{' and '}'
					String[] resources = value.split(", ");
					HashSet<String> resource_set = new HashSet<String>();
					boolean exit = false;
					for (String resource : resources)
					{
						if (resource.equals("#PAGE_TITLE#"))
						{
							outcome.put(feature_counter, "-1.0");
							break; // nothing to found
						}
						String[] res_parts = resource.split(" <");
						if (res_parts.length == 2 && !res_parts[1].isEmpty() && res_parts[1].length() > 1)
							resource_set.add(res_parts[1].substring(0, res_parts[1].length() - 1));
					}
					// Check if the set contains the predicate
					// float similarity = 0.0f;
					// float max_simi = 0.0f;
					if (!exit && resource_set.size() > 0)
					{
						boolean exit1 = false;
						for (String reso : resource_set)
						{
							// similarity = Similarity.distanceStr(reso, pred);
							// if (similarity > max_simi)
							// max_simi = similarity;
							if (reso.equals(pred))
							{
								outcome.put(feature_counter, "1.0"); // true
								exit1 = true;
								break;
							}
						}
						if (!exit1)
							outcome.put(feature_counter, "0.0"); // false
						// System.out.println(pred + " " + similarity);
						// outcome.put(feature_counter, String.valueOf(max_simi));
					} else
						outcome.put(feature_counter, "0.0"); // false
				} // end if (i == 6)
				else if (i == 7)
				{
					// (set of) headers for subject
					// feature_counter--;
					value = line[i].substring(1, line[i].length() - 1); // remove '{' and '}'
					String[] resources = value.split(", ");
					HashSet<String> resource_set = new HashSet<String>();
					boolean exit = false;
					for (String resource : resources)
					{
						if (resource.equals("#PAGE_TITLE#"))
						{
							outcome.put(feature_counter, "-1.0");
							exit = true;
							break; // nothing to found
						}
						String[] res_parts = resource.split(" <");
						if (res_parts.length == 2 && !res_parts[1].isEmpty() && res_parts[1].length() > 1)
							resource_set.add(res_parts[1].substring(0, res_parts[1].length() - 1));
					}
					// Check if the set contains the predicate
					// float similarity = 0.0f;
					// float max_simi = 0.0f;
					if (!exit && resource_set.size() > 0)
					{
						boolean exit1 = false;
						for (String reso : resource_set)
						{
							// similarity = Similarity.distanceStr(reso, pred);
							// if (similarity > max_simi)
							// max_simi = similarity;
							if (reso.equals(pred))
							{
								outcome.put(feature_counter, "1.0"); // true
								exit1 = true;
								break;
							}
						}
						if (!exit1)
							outcome.put(feature_counter, "0.0"); // false
						// System.out.println(pred + " " + similarity);
						// outcome.put(feature_counter, String.valueOf(max_simi));
					} else
						outcome.put(feature_counter, "0.0"); // false
				} else if (i == 0)
				{
					;
				} else if (i == 9)
				{
					// // System.out.println(line[i]);
					// if (line[i].equals("o"))
					// outcome.put(feature_counter, "2.0");
					// else if (line[i].equals("n"))
					// outcome.put(feature_counter, "1.0");
					feature_counter--;
				} else if (i == 10)
				{
					// System.out.println(line[i]);
					if (line[i].equals("h"))
						outcome.put(feature_counter, "1.0");
					else if (line[i].equals("b"))
						outcome.put(feature_counter, "2.0");
				} else if (i == 11 || i == 13)
					feature_counter--;
				else if (i == 12)
				{
					// Normalize for how many rows the relation held for in this table
					outcome.put(feature_counter, new String("" + (Float.valueOf(line[i]) / Float.valueOf(line[3]))));
				} else
				{
					outcome.put(feature_counter, Float.valueOf(line[i]).toString());
				}
				feature_counter++;
			}
			String[] title = line[0].split(" ");
			String tripl = new String(line[11].getBytes(), Charset.forName("UTF-8"));
			tripl = convert(tripl);
			System.out.println(tripl);
			format.append(featureVectorToString(outcome))
					.append(" " + feature_counter + ":" + Float.valueOf(title[0]).toString())
					.append(" # " + title[1] + " " + tripl + " ");
			format.trimToSize();
		} catch (Exception e)
		{
			System.out.println("Exception: ");
			e.printStackTrace();
			return null;
		}
		return format.toString();
	}

	public static String featureVectorToString(SortedMap<Integer, String> featureMap)
	{
		Iterator<Integer> iterator = featureMap.keySet().iterator();
		StringBuilder featureVector = new StringBuilder();

		while (iterator.hasNext())
		{
			Object key = iterator.next();
			featureVector.append(key).append(":").append(featureMap.get(key)).append(" ");
		}
		featureVector.trimToSize();
		return featureVector.toString().trim();
	}

	public String convert(String str)
	{
		str = str.replace("\\u2013", "-").replace("\\u2019", "%27").replace("\\u00E9", "%C3%A9")
				.replace("\\u00F3", "%C3%B3").replace("\\u0107", "%C4%87").replace("\\u015F", "%C8%99")
				.replace("\\u00ED", "%C3%AD").replace("\\u00FC", "%C3%BC").replace("\\u00F6", "%C3%B6")
				.replace("\\u00C5", "%C3%85").replace("\\u00E2", "%C3%A2").replace("\\u00E1", "%C3%A1")
				.replace("\\u00F1", "%C3%B1").replace("\\u017E", "%C5%BE").replace("\\u00E3", "%C3%A3")
				.replace("\\u00FA", "%C3%BA").replace("\\u0105", "%C4%85").replace("\\u010C", "%C4%8C")
				.replace("\\u011B", "%C4%9B").replace("\\u00DA", "%C3%9A").replace("\\u0148", "%C5%88")
				.replace("\\u0160", "%C5%A0").replace("\\u0111", "%C4%91").replace("\\u00E0", "%C3%A0")
				.replace("\\u0101", "%C4%81").replace("\\u00E4", "%C3%A4").replace("\\u00EA", "%C3%8A")
				.replace("\\u00C6", "%C3%86").replace("\\u00E6", "%C3%A6").replace("\\u016B", "%C5%AB")
				.replace("\\u014C", "%C5%8C");
		return str;
	}
}
