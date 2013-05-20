<%@taglib uri="http://www.springframework.org/tags" prefix="spring"%>
<%@taglib uri="http://www.springframework.org/tags/form" prefix="form"%>

<!-- needed -->
<%@ page contentType="text/html; charset=UTF-8"%>
<%@ page language="java" pageEncoding="UTF-8"%>

<html>
<head>
<title>Extracting RDF Triples from Wikipedia's Tables</title>
<meta http-equiv="Content-Type" content="text/html; charset=utf-8">
<meta content="IE=edge,chrome=1" http-equiv="X-UA-Compatible">
<meta content="width=device-width,initial-scale=1" name="viewport">
<meta content="Emir Muñoz" name="author">
<meta
	content="RDF, Wikipedia, DBpedia, Relation Extraction, Tables, Webtables"
	name="keywords">
<meta content="DERI RDF Relations Extraction from Wikipedia's Tables"
	name="description">
<style type="text/css" title="currentStyle">
@import "css/style.css";

@import "css/animation.css";

@import "js/reveal/reveal.css";

@import "js/DataTables/css/datatable_jui.css";

/* @import "js/DataTables/css/themes/smoothness/jquery-ui-1.8.4.custom.css"
	; */
@import "css/flick/jquery-ui-1.10.2.custom.css";
</style>
</head>

<body>
	<div id="bcontainer">
		<div id="body">
			<div class="title home">
				<h1>DRETa</h1>
				<div>DERI RDF Relations Extraction from Wikipedia's Tables</div>
			</div>
			<div class="search">
				<span>http://en.wikipedia.org/wiki/</span> <input id="wikiquery"
					class="home autofocus" type="text" size="24"
					title="some article's name"> <input type="button"
					class="buttonSubmit" value="extraction">
			</div>
			<div class="note examples">
				<small> examples of articles: Manchester United F.C., Johnny
					Depp, Galway, 2010 Chile earthquake, Comparison of IDEs</small><br> <select
					id="modelSelect">
					<option value="naive-bayes">Naive Bayes (Threshold:
						0.4317)</option>
					<option selected="selected" value="bagging-dt">Bagging
						decision trees (Threshold: 0.4931)</option>
					<option value="random-forest">Random forest (Threshold:
						0.6)</option>
					<option value="svm">SVM (Threshold: 0.98)</option>
					<option value="simple-logistic">SimpleLogistic (Threshold:
						0.5083)</option>
					<option value="ratio-32">Ratio 32 (Threshold: 0.6986)</option>
					<option value="ratio-34">Ratio 34 (Threshold: 0.7586)</option>
					<option value="ratio-36">Ratio 36 (Threshold: 0.7018)</option>
				</select>
			</div>
			<br>
			<div id="responseDiv">
				<div id="message"></div>
				<div id="tabularResponse">
					<table id="responseTable" class="display" style="width: 100%;">
						<thead>
							<tr>
								<th>Class</th>
								<th>Conf.</th>
								<th>RDF Triple</th>
							</tr>
						</thead>
						<tbody></tbody>
					</table>
				</div>
			</div>
		</div>
		<div id="footer">
			<a id="nuig-logo" title="National University of Ireland, Galway"
				href="http://www.nuigalway.ie/"> <img alt=""
				src="css/images/nuig-logo-32px.png">
			</a> <a id="deri-logo" title="Digital Enterprise Research Institute"
				href="http://deri.ie/"> <img alt=""
				src="css/images/deri-logo-32px.png">
			</a>
		</div>
	</div>
	<!-- help button -->
	<div id="help">
		<a href="" id="helpBtn" type="button"><img border="0" width="45"
			src="css/images/help.png" alt="Help"></a> <br> <a
			href="http://emir-munoz.github.com/wikitables" target="_blank"
			id="codeBtn" type="button"><img border="0" width="40"
			src="css/images/github.png" alt="Source code"></a>
	</div>
	<!-- HELP WINDOW -->
	<div id="helpModal" class="reveal-modal" style="overflow: auto;">
		<h1>Extracting Relations from Wikipedia's Tables - DRETa</h1>
		<p>
			DRETa is a system that mine relational HTML tables in Wikipedia's
			articles to extract RDF triples. Using <a
				href="http://wiki.dbpedia.org/Downloads38" target="_blank">DBpedia
				3.8</a> as knowledge-base, we identify resources at a row level and then
			relations among those resources. Applying some machine-learning
			models we filter and display the best candidates.
		</p>
		<h2>How to use DRETa?</h2>
		<p>
			You can <strong>search</strong> for any title of a Wikipedia's article
			which contains relational tables (<i>wikitables</i>); <strong>select</strong>
			a machine-learning model or use default; and DRETa will display the
			extracted triples. Also you can <strong>filter</strong> by confidence
			and even <strong>export</strong> the results.
		</p>
		<h2>How DRETa works?</h2>
		<p>
			For a given <i>wikitable</i>: map table cells to DBpedia resources
			and, for cells in the same row, identify relationships based on
			existing information in DBpedia for other rows. We state that if some
			relationship is held for most rows with certain features, should be
			held by all rows.<br> <b>You can take a look into our <a
				href="https://github.com/emir-munoz/wikitables" target="_blank">code</a>
				and see the documentation in our <a
				href="http://emunoz.org/wikitables/" target="_blank">web-page</a></b>.
		</p>
		<a class="close-reveal-modal">&#215;</a>
		<p align="center">
			<img width="130" src="css/images/DERI_brandmark_rgb.jpg">
		</p>
	</div>
	<!-- jQuery Library Files -->
	<script type="text/javascript"
		src="http://ajax.googleapis.com/ajax/libs/jquery/1.7.2/jquery.min.js"></script>
	<script type="text/javascript"
		src="https://ajax.googleapis.com/ajax/libs/jqueryui/1.8.18/jquery-ui.min.js"></script>
	<!-- notifications -->
	<script type="text/javascript" src="js/noty/jquery.noty.js"></script>
	<script type="text/javascript" src="js/noty/layouts/top.js"></script>
	<script type="text/javascript" src="js/noty/layouts/topLeft.js"></script>
	<script type="text/javascript" src="js/noty/themes/default.js"></script>
	<!-- tables -->
	<script type="text/javascript"
		src="js/DataTables/jquery.dataTables.min.js"></script>
	<!-- pop-ups -->
	<script type="text/javascript" src="js/reveal/jquery.reveal.js"></script>
	<script type="text/javascript" src="js/blockUI/jquery.blockUI.js"></script>
	<!-- javascript -->
	<script type="text/javascript" src="js/wikitable.js"></script>
</body>
</html>
