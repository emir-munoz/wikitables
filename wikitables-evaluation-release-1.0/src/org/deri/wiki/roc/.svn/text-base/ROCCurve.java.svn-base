package org.deri.wiki.roc;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ROCCurve
{
	private static List<Double>	predictions	= new ArrayList<Double>();
	private static List<Double>	actuals		= new ArrayList<Double>();
	private static String		rocFile		= "./data/ROC-2";

	public static void readROCData(String resultsFile) throws IOException
	{
		BufferedReader br = new BufferedReader(new FileReader(resultsFile + ".dat"));
		String line = null;
		while ((line = br.readLine()) != null)
		{
			line = line.trim();
			if (!line.isEmpty())
			{
				String[] split = line.split("\t");
				if (split.length < 2)
				{
					throw new IOException("Cannot read line (expecting four or five columns) " + line);
				}
				predictions.add(new Double(split[0]));
				actuals.add(new Double(split[1]));
			}
		}

	}

	private static void getROCCurveSVM() throws IOException
	{
		readROCData(rocFile);
		BufferedWriter out = new BufferedWriter(new FileWriter(rocFile + ".curve"));

		for (int i = 0; i < predictions.size(); i++)
		{
			Double threshold = predictions.get(i) - 0.00000001;
			int prediction = 0;
			int true_positive = 0;
			int true_negative = 0;
			int false_positive = 0;
			int false_negative = 0;
			for (int j = 0; j < predictions.size(); j++)
			{
				if (predictions.get(j) > threshold)
				{
					prediction = 1;
				}
				if (prediction == 1 && actuals.get(j) == 1)
					true_positive++;
				if (prediction == 1 && actuals.get(j) == -1)
					false_positive++;
				if (prediction == 0 && actuals.get(j) == 1)
					false_negative++;
				if (prediction == 0 && actuals.get(j) == -1)
					true_negative++;
			}

			System.out.println(predictions.get(i) + "\t" + actuals.get(i) + "\t" + true_positive + "\t"
					+ false_positive + "\t" + false_negative + "\t" + true_negative + "\t"
					+ new Double(true_positive / ((true_positive + false_negative) * 1.0)) + "\t"
					+ new Double(false_positive / ((false_positive + true_negative) * 1.0)));

			out.write(predictions.get(i) + "\t" + actuals.get(i) + "\t" + true_positive + "\t" + false_positive + "\t"
					+ false_negative + "\t" + true_negative + "\t"
					+ new Double(true_positive / ((true_positive + false_negative) * 1.0)) + "\t"
					+ new Double(false_positive / ((false_positive + true_negative) * 1.0)) + "\n");
		}
		out.close();
	}

	public static void main(String[] args) throws IOException
	{
		getROCCurveSVM();
	}
}
