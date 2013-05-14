package org.deri.exreta.ml.weka.controller;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;

import weka.classifiers.Evaluation;
import weka.classifiers.evaluation.NominalPrediction;
import weka.classifiers.meta.FilteredClassifier;
import weka.core.FastVector;
import weka.core.Instances;
import weka.core.converters.SVMLightLoader;
import weka.filters.Filter;
import weka.filters.unsupervised.attribute.NumericToNominal;
import cl.em.utils.compression.gzip.CompressionGZIP;
import cl.em.utils.performance.TimeWatch;

public class MakeLargePrediction
{
	private static Instances		data;
	private static Instances		filteredData;
	private static BufferedWriter	outMatched;
	private static StringBuilder	predict;
	private static int				size			= 200;
	private static int				instanceCounter	= 0;

	public static void readFeatureFile(String input, String output, String model, String threshold)
	{
		// Read already annotated triples
		BufferedReader br = null;
		String line;
		int lineCounter = 1;
		StringBuilder strb = new StringBuilder();
		try
		{
			// Output triples with new features
			FileWriter fstreamLog = new FileWriter(output);
			outMatched = new BufferedWriter(fstreamLog);

			if (input.endsWith(".gz"))
				br = new BufferedReader(CompressionGZIP.readGZFile(input));
			else
				br = new BufferedReader(new FileReader(input));

			while ((line = br.readLine()) != null && line.length() > 0)
			{
				if (lineCounter % size == 0)
				{
					strb.append(line + "\n");

					// make batch prediction
					String predictions = makePrediction(strb.toString(), model, threshold);

					// write into file
					outMatched.write(predictions);
					outMatched.flush();

					// clean string builder
					strb.delete(0, strb.length());
					strb.trimToSize();
				} else
				{
					// add line to string builder
					strb.append(line + "\n");
				}
				lineCounter++;
			}
			// write last batch

			// make batch prediction
			String predictions = makePrediction(strb.toString(), model, threshold);
			System.out.println(predictions);

			// write into file
			outMatched.write(predictions);
			outMatched.flush();

			// clean string builder
			strb.delete(0, strb.length());
			strb.trimToSize();
		} catch (FileNotFoundException ex1)
		{
			System.err.println(String.format("%s (No such file or directory) ", input));
		} catch (IOException ex2)
		{
			System.err.println(String.format("Error reading file %s, Message: %s", input, ex2.getMessage()));
		} catch (Exception ex3)
		{
			System.err.println("Error in makePredictions");
		}
	}

	public static String makePrediction(String inputStr, String model, String threshold) throws Exception
	{
		SVMLightLoader svmloader = new SVMLightLoader();
		InputStream is = new ByteArrayInputStream(inputStr.getBytes());
		svmloader.setSource(is);
		data = svmloader.getDataSet();
		is.close();

		// setting class attribute
		if (data.classIndex() == -1)
			data.setClassIndex(data.numAttributes() - 1);

		// filter NumericToNominal
		NumericToNominal nominal = new NumericToNominal();
		nominal.setAttributeIndices("26,27,37,38");
		nominal.setInputFormat(data);
		filteredData = Filter.useFilter(data, nominal);

		// load classifier
		FilteredClassifier cls = (FilteredClassifier) weka.core.SerializationHelper.read(model);

		// evaluation
		Evaluation evaluation = new Evaluation(filteredData);
		evaluation.evaluateModel(cls, filteredData);

		predict = new StringBuilder();
		double prediction;
		FastVector fv = evaluation.predictions();

		for (int i = 0; i < filteredData.numInstances(); i++)
		{
			instanceCounter++;

			NominalPrediction nom = (NominalPrediction) fv.elementAt(i);
			prediction = Double.parseDouble(filteredData.classAttribute().value((int) nom.predicted()));
			if (nom.distribution()[0] >= Double.valueOf(threshold))
				prediction = 1.0;
			predict.append(instanceCounter + "\t" + prediction + "\t" + nom.distribution()[0] + "\t"
					+ nom.distribution()[1] + "\n");
		}
		return predict.toString();
	}

	public static void main(String[] args) throws Exception
	{
		if (args.length < 3)
		{
			System.out.println("Enter parameter arg1: dataset arg2: model arg3: threshold");
			System.exit(0);
		}

		// Start timing
		TimeWatch watch = TimeWatch.start();
		readFeatureFile(args[0], args[3], args[1], args[2]);
		System.out.println("Elapsed time: " + watch.elapsedTime());
	}
}
