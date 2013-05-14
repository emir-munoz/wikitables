package org.deri.exreta.dal.main;

/**
 * List of Constants
 * 
 * @author Emir Munoz <emir@emunoz.org>
 * @version 1.1
 * @since 2012-07-01
 *        Changelog:
 *        - 2013-03-07 add server and local configurations
 */
public interface LocationConstants
{
	// ==== location of the maxent corpus
	public String	CORPUS_PATH			= "./maxent/wtt_corpus/";
	// ==== location of the maxent models
	public String	MODELS_PATH			= "./maxent/wtt_models/";
	public String	MODEL_1_PATH		= "./maxent/wtt_models/layout.model";
	// ==== location of the web-tables
	public String	WTT_PATH			= "./corpus/yahoo/";
	// ==== location of wikidumps
	public String	WIKI_BZ2			= "./wikibz2/";
	public String	WIKI_HTML			= "./wikihtml/";
	// ==== Endpoints configuration
	public String	ENCODING			= "UTF-8";
	public String	DBPEDIA_ENDPOINT	= "http://dbpedia.org/sparql";
	public String	BING_ENDPOINT		= "http://";
	public String	FREEBASE_ENDPOINT	= "";
	// ==== Constraints
	public int		MAX_LENGTH_TXT		= 150;
	public int		MAX_TRIES			= 3;
	// ==== Formats
	public String	LANGUAGE_EN			= "@en";
	public String	FORMAT_RESPONSE		= "JSON";
	public String	DBPEDIA_RESOURCE	= "http://dbpedia.org/resource/";
	public String	DBPEDIA_PROPERTY	= "http://dbpedia.org/property/";
	// ==== Mongo configuration
	public String	MONGO_URL			= "mongodb://localhost:27017";
	public String	MONGO_DBNAME		= "wikitables";
	// ==== Abbreviation configuration
	public String	ABBRUID				= "2300";
	public String	ABBRTOKENID			= "uxC39awWi8PRCw4T";
	// ==== Time out for connections
	public int		TIMEOUT_SEC			= 20;
	// ==== Index path for local and server configurations
	public String	SERVER_FOLDER		= "/data/wikitables-demo";
	public String	SERVER_TEST			= "/data/emir";
	public String	LOCAL_FOLDER		= "/home/emir/Work/engineering/wikipedia-tables";
	// ==== Redirects index
	public String	REDIRECT_INDEX		= "/db38/redirects.ni";
	public String	REDIRECT_SPARSE		= "/db38/redirects.sp";
	// ==== Labels index
	public String	LABELS_INDEX		= "/db38/labels.ni";
	public String	LABELS_SPARSE		= "/db38/labels.sp";
	// ==== Exceptional cases
	public String	LABELS_EXCEPTIONS	= "/db38/exceptions";
	// ==== DBpedia38 index
	public String	DBPEDIA38_INDEX		= "/db38/dbpedia38.en.ni";
	public String	DBPEDIA38_SPARSE	= "/db38/dbpedia38.en.sp";
	// ==== DBpedia predicates
	public String	PREDICATES_STAT		= "/db38/pred-stats.dat";
	// ==== Index constants
	public int		PRED_MAX_INSTA		= 143482445;
	public int		PRED_MAX_SUBJ		= 11547187;
	public int		PRED_MAX_OBJ		= 13595147;
	public int		DECIMAL_PLACE		= 6;
	// ==== Titles index
	public String	TITLE_INDEX			= "/index/index-title";
}
