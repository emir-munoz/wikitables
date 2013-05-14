package org.deri.exreta.demo.controller;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.deri.exreta.autocomplete.search.SearchIndexLucene;
import org.deri.exreta.dal.enums.ModelSchema;
import org.deri.exreta.dal.enums.RDFFormat;
import org.deri.exreta.dal.weka.model.FVTripleDTO;
import org.deri.exreta.dal.weka.model.PredictionTriple;
import org.deri.exreta.demo.model.Response;
import org.deri.exreta.demo.wrapper.WrapperRDFTriple;
import org.deri.exreta.ml.demo.MakePredictionDemo;
import org.deri.exreta.wikipedia.extractor.WikiTableTriplesDemo;
import org.semanticweb.yars.util.LRUMapCache;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import cl.em.utils.performance.TimeWatch;
import cl.em.utils.string.StringUtils;

/**
 * Controller for ExReTa services.
 * 
 * @author Emir Muñoz <emir@emunoz.org>
 * @version 0.2.1
 * @since 2013-03-04
 * 
 */

@Controller
public class ProcessWikipageController
{
	private static final Logger					_log				= Logger.getLogger(ProcessWikipageController.class);
	private final int							CACHE_SIZE			= 100;
	private LRUMapCache<String, FVTripleDTO>	articleCache		= new LRUMapCache<String, FVTripleDTO>(CACHE_SIZE);
	private TimeWatch							watch				= null;
	private ArrayList<PredictionTriple>			triplesWithClass	= null;
	private static int							NUMBER_OF_TITLES	= 25;
	private static String						WORKSPACE			= null;
	private static String						TITLE_INDEX			= null;
	private static WikiTableTriplesDemo			triples				= null;
	private static MakePredictionDemo			predictor			= null;
	private static SearchIndexLucene			searcherLucene		= null;

	public ProcessWikipageController() throws IOException
	{
		// loading properties
		_log.info("Setting properties from wikitables.properties");
		InputStream inputStream = this.getClass().getClassLoader().getResourceAsStream("../wikitables.properties");
		Properties properties = new Properties();
		properties.load(inputStream);

		// reading properties
		WORKSPACE = properties.getProperty("SERVER_FOLDER");
		TITLE_INDEX = properties.getProperty("TITLE_INDEX");
		NUMBER_OF_TITLES = Integer.valueOf((String) properties.get("NUM_TITLES"));

		// creating instances and loading indexes
		triples = new WikiTableTriplesDemo(properties);
		searcherLucene = new SearchIndexLucene(WORKSPACE + TITLE_INDEX, NUMBER_OF_TITLES);
		predictor = new MakePredictionDemo();
	}

	/**
	 * Display index page.
	 */
	@RequestMapping(value = "/index", method = RequestMethod.GET, headers = "Accept=*/*")
	public String getIndex() throws Exception
	{
		return "broker";
	}

	/**
	 * Service to process a query and return the set of extracted triples.
	 * 
	 * @param query Terms in user's query.
	 * @param model Model used to predict the relations.
	 * @return A response object with a message and found relations, if there is some.
	 */
	@RequestMapping(value = "/search", method = RequestMethod.GET, produces = "application/json")
	public @ResponseBody
	Response processQuery(@RequestParam("query") String query, @RequestParam("model") String modelval, Model model)
	{
		Response resp = new Response();
		boolean error = true;
		watch = TimeWatch.start(); // start timing
		try
		{
			// ============================
			// Zero, check cache for articles
			// ============================
			FVTripleDTO triplesExtracted = articleCache.get(query.trim());
			if (triplesExtracted == null)
			{

				// ============================
				// First, extraction process
				// ============================
				triples.init(query);
				_log.info(String.format("##END## extraction for article: %s, Elapsed time: %s", query,
						watch.elapsedTime()));

				// save triples in cache
				triplesExtracted = triples.getCandidateTriples();
				articleCache.put(query, triplesExtracted);
			}
			_log.info(String.format("We found %d relations for article: %s", triplesExtracted.getCandidateTriples()
					.size(), query));

			// if we found more than 0 candidate triples, then run predictor
			if (triplesExtracted.getCandidateTriples().size() > 0)
			{
				error = false;
				// ===============================
				// Second, the prediction process
				// ===============================
				predictor.setInstancesFV(StringUtils.toUTF8InputStream(triplesExtracted.getFeatureVector()));
				predictor.setInstancesTriple(triplesExtracted.getCandidateTriples());

				String[] modelConf = getModelConfiguration(ModelSchema.fromString(modelval));
				predictor.setModelAndThreshold(modelConf[0], Double.valueOf(modelConf[1]));
				predictor.makePredictions();

				// retrieve instances of triples with predicted class and confidence
				triplesWithClass = predictor.getInstancesTriple();

				// ============================
				// Third, build a response
				// ============================
				if (triplesWithClass.size() > 0)
				{
					resp.setType("ok");
					resp.setText("success");
					resp.setTriples(triplesWithClass);
					resp.setElapsed(watch.elapsedSeconds());
				}
			}
			if (error)
			{
				resp.setType("error");
				resp.setText(String.format("We did not found any new relation in %s article", query));
			}
			_log.info(String.format("##END## query processing for %s  with error=%s, Elapsed time: %s", query, error,
					watch.elapsedTime()));
		} catch (Exception e)
		{
			_log.info(String.format("##ERROR## processing query: %s, Message: ", query, e.getMessage()));
			resp.setType("error");
			resp.setText(e.getMessage());
		}

		return resp;
	}

	/**
	 * Retrieve the configurations for a given model.
	 * 
	 * @param modelValue ModelSchema value passed from the interface.
	 * @return An array with model path and threshold.
	 */
	private String[] getModelConfiguration(ModelSchema modelValue)
	{
		String[] resp = new String[2];

		switch (modelValue)
		{
			case NAIVE_BAYES:
				resp[0] = WORKSPACE + "/model/naive-bayes-final.model";
				resp[1] = "0.4317"; // "0.8351";
				break;
			case DECISION_TREES:
				resp[0] = WORKSPACE + "/model/bagging-decision-trees-final.model";
				resp[1] = "0.4931"; // "0.4695";
				break;
			case RANDOM_FOREST:
				resp[0] = WORKSPACE + "/model/random-forest-final.model";
				resp[1] = "0.6"; // "0.5";
				break;
			case SIMPLE_LOGISTIC:
				resp[0] = WORKSPACE + "/model/simple-logistic-final.model";
				resp[1] = "0.5083"; // "0.3854";
				break;
			case SVM:
				resp[0] = WORKSPACE + "/model/svm-final.model";
				resp[1] = "0.98"; // "0.4585";
				break;
			case RATIO_32:
				resp[0] = WORKSPACE + "/model/ratio-32-final.model";
				resp[1] = "0.6986";
				break;
			case RATIO_34:
				resp[0] = WORKSPACE + "/model/ratio-34-final.model";
				resp[1] = "0.7586";
				break;
			case RATIO_36:
				resp[0] = WORKSPACE + "/model/ratio-36-final.model";
				resp[1] = "0.7018";
				break;
			case DEFAULT:
				resp[0] = WORKSPACE + "/model/bagging-decision-trees-final.model";
				resp[1] = "0.6"; // "0.5";
		}
		return resp;
	}

	/**
	 * Service to download a given set of extracted triples.
	 * 
	 * @param format Format to serialization. e.g. N3, RDF/XML, JSON.
	 * @param cut Double to use as threshold to filter triples.
	 * @param response HttpServletResponse
	 * @throws IOException
	 */
	@RequestMapping(value = "/download", produces = "application/rdf+xml")
	@ResponseBody
	public void getFile(@RequestParam("format") String format, @RequestParam("cut") String cut,
			final HttpServletResponse response) throws IOException
	{
		// parse threshold
		double threshold = 0.0;
		if (!cut.isEmpty() && !cut.equalsIgnoreCase("undefined"))
			threshold = Double.parseDouble(cut);

		if (triplesWithClass != null)
		{
			try
			{
				// export according parameter format
				InputStream fileIn = null;

				switch (RDFFormat.valueOf(format))
				{
					case n3:
						fileIn = StringUtils.toUTF8InputStream(WrapperRDFTriple.getN3Notation(triplesWithClass,
								threshold));
						break;
					case xml:
						fileIn = StringUtils.toUTF8InputStream(WrapperRDFTriple.getXMLNotation(triplesWithClass,
								threshold));
						break;
					case json:
						fileIn = StringUtils.toUTF8InputStream(WrapperRDFTriple.getJSONNotation(triplesWithClass,
								threshold));
						break;
					default:
						fileIn = StringUtils.toUTF8InputStream(WrapperRDFTriple.getN3Notation(triplesWithClass,
								threshold));
						break;
				}

				// define response
				if (fileIn != null)
				{
					response.setContentType("application/force-download");
					response.setHeader("Content-Transfer-Encoding", "binary");
					response.setContentLength((int) fileIn.available());
					response.setHeader("Content-Disposition", "attachment; filename=\"triples." + format + "\"");

					IOUtils.copy(fileIn, response.getOutputStream());
					response.flushBuffer();
				}
			} catch (Exception ex)
			{
				_log.info("Error writing file to output stream.");
			}
		}
	}

	/**
	 * Service to give suggestions of titles for autocomplete box.
	 * 
	 * @param terms Letters typed by the user in the interface.
	 * @return A list of Wikipedia titles that contains those letters.
	 */
	@RequestMapping(value = "/get_title", method = RequestMethod.GET, headers = "Accept=*/*")
	public @ResponseBody
	List<String> getTitleList(@RequestParam("term") String terms)
	{
		ArrayList<String> result = new ArrayList<String>();
		try
		{
			result = searcherLucene.searchFor(terms);
		} catch (IOException e)
		{
			_log.error("Error querying index for titles");
		}
		return result;
	}
}
