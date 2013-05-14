package org.deri.exreta.ml.demo;

import java.io.InputStream;
import java.util.ArrayList;

import org.apache.log4j.Logger;
import org.deri.exreta.dal.weka.model.PredictionTriple;

import weka.classifiers.Evaluation;
import weka.classifiers.evaluation.NominalPrediction;
import weka.classifiers.meta.FilteredClassifier;
import weka.core.FastVector;
import weka.core.Instances;
import weka.core.converters.SVMLightLoader;
import weka.filters.Filter;
import weka.filters.unsupervised.attribute.NumericToNominal;
import cl.em.utils.performance.TimeWatch;

/**
 * Make predictions over extracted RDF triples.
 * 
 * @author Emir Munoz <emir@emunoz.org>
 * @version 0.2.3
 * @since 2013-03-05
 * 
 */
public class MakePredictionDemo
{
	private static final Logger			_log			= Logger.getLogger(MakePredictionDemo.class);
	private static TimeWatch			watch;
	private FilteredClassifier			cls;
	private InputStream					instancesFV;
	private ArrayList<PredictionTriple>	instancesTriple;
	private String						model;
	private double						threshold;
	private Instances					data			= null;
	private Instances					filteredData	= null;

	/**
	 * Set the feature vectors corresponding to the instances.
	 * 
	 * @param instancesFV
	 */
	public void setInstancesFV(InputStream instancesFV)
	{
		this.instancesFV = instancesFV;
	}

	/**
	 * Set list of instances to be predicted.
	 * 
	 * @param instancesTriple
	 */
	public void setInstancesTriple(ArrayList<PredictionTriple> instancesTriple)
	{
		this.instancesTriple = instancesTriple;
	}

	/**
	 * Get list of predicted instances.
	 * 
	 * @return
	 */
	public ArrayList<PredictionTriple> getInstancesTriple()
	{
		return this.instancesTriple;
	}

	/**
	 * Set the ml model and its threshold.
	 * 
	 * @param model Model to be used.
	 * @param threshold Threshold to be used.
	 */
	public void setModelAndThreshold(String model, double threshold)
	{
		this.model = model;
		this.threshold = threshold;
	}

	/**
	 * Parse the inputstream in SVM format, generate instances and apply filters to the attributes.
	 * 
	 * @throws Exception
	 */
	private void preprocessData() throws Exception
	{
		// load instances from input stream
		SVMLightLoader svmloader = new SVMLightLoader();
		svmloader.setSource(this.instancesFV);
		data = svmloader.getDataSet();
		this.instancesFV.close();

		// setting class attribute
		if (data.classIndex() == -1)
			data.setClassIndex(data.numAttributes() - 1);

		// ISWC paper version
		NumericToNominal nominal = new NumericToNominal();
		nominal.setAttributeIndices("26,27,37,38");
		nominal.setInputFormat(data);
		filteredData = Filter.useFilter(data, nominal);
	}

	/**
	 * Perform the predictions over the given data.
	 * 
	 * @throws Exception
	 */
	public void makePredictions() throws Exception
	{
		_log.info("Starting prediction process using model " + model + " and threshold: " + threshold);

		// start timing
		watch = TimeWatch.start();

		// pre-process data before prediction
		this.preprocessData();
		
		if (data == null || filteredData == null)
			throw new Exception("Something went wrong processing data. Please try again.");

		// load classifier
		cls = (FilteredClassifier) weka.core.SerializationHelper.read(this.model);

		// evaluation
		Evaluation evaluation = new Evaluation(filteredData);
		evaluation.evaluateModel(cls, filteredData);

		FastVector fv = evaluation.predictions();
		double prediction = 0.0;

		for (int i = 0; i < filteredData.numInstances(); i++)
		{
			// classify instance
			NominalPrediction nom = (NominalPrediction) fv.elementAt(i);
			prediction = Double.parseDouble(filteredData.classAttribute().value((int) nom.predicted()));

			instancesTriple.get(i).setPclass(prediction);
			if (nom.distribution()[0] >= nom.distribution()[1])
				instancesTriple.get(i).setConf(nom.distribution()[0]);
			else
				instancesTriple.get(i).setConf(nom.distribution()[1]);
		}
		_log.info("Finished prediction, Elapsed time: " + watch.elapsedTime());
	}
}
