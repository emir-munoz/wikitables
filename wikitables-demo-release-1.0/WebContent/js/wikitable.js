/**
 * JS Wikipedia tables.
 * 
 * @author Emir Muñoz <emir@emunoz.org>
 */

$(document).ready(function() {
	$('#tabularResponse').hide();

	// check name availability on focus lost
	$('#wikiquery').keypress(function(e) {
		if ((e.which && e.which == 13) || (e.keyCode && e.keyCode == 13)) {
			checkAvailability();
		}
	});

	// search option
	$('.buttonSubmit').click(function() {
		checkAvailability();
	});

	// default message on input
	$('.autofocus').focus(function(srcc) {
		if ($(this).val() == $(this)[0].title) {
			$(this).removeClass("autofocus");
			$(this).val("");
		}
	});

	$('.autofocus').blur(function() {
		if ($(this).val() == "") {
			$(this).addClass("autofocus");
			$(this).val($(this)[0].title);
		}
	});

	$(".autofocus").blur();

	// help message
	$('#helpBtn').click(function(e) {
		e.preventDefault();
		$('#helpModal').reveal();
	});

	// wikipedia title autocomplete
	$("#wikiquery").autocomplete({
		/* delay : 0, */
		sortResults : true,
		source : 'get_title.html'
	});

	// export result
	$('a.export').live("click", function() {
		var min = $('#min').val();
		var form = $(this).text();
		window.location.href = "download.html?format=" + form + "&cut=" + min;
	});

	// welcome message
	welcome();
});

// loading effect
function loading() {
	$.blockUI({
		css : {
			border : 'none',
			padding : '15px',
			backgroundColor : '#000',
			'-webkit-border-radius' : '10px',
			'-moz-border-radius' : '10px',
			opacity : .6,
			color : '#fff',
			top : '20%'
		},
		message : '<h1><div class="circle"></div><div class="circle1"></div> Just a moment...</h1>',
		/* theme : true, */
		draggable : false, // draggable option requires jquery UI
		// z-index for the blocking overlay
		baseZ : 5000
	});
}

// clear response div
function cleanResponse() {
	$('#responseDiv').empty();
	$('#responseDiv').append('<div id="message"></div>');
	$('#responseDiv')
			.append(
					'<div id="tabularResponse"><div style="padding: 0px 0px 0.5em;"><div class="left"></div>'
							+ '<div class="right">export as <a href="#" class="export" data-container="target">n3</a></div></div><br/>'
							+ '<table id="responseTable" class="display" style="width:100%;"><thead><tr><th>Class</th><th>Conf.</th><th>Subject</th><th>Predicate</th><th>Object</th></tr></thead><tbody></tbody></table></div>');
}

// send query to server and display response
function checkAvailability() {
	$('#responseDiv').hide();
	var terms = $('#wikiquery').val();
	var modelval = $('#modelSelect').find(":selected").val();
	if (terms && terms != "some article's name") {
		loading();
		$.getJSON("search.html", {
			query : terms,
			model : modelval
		}, function(availability) {
			$.unblockUI();
			if (availability.type == "ok") {
				cleanResponse();
				var count = fillResponse(availability.triples, availability.elapsed);
				$('#responseDiv').show();
				if (count > 0) {
					noty({
						text : 'Success extraction. See retrieved RDF relations.',
						type : 'success',
						layout : 'top',
						closeWith : [ 'hover' ],
						timeout : 5000
					});
				}
			} else {
				$('#message').html('<p style="color: red;">We didn\'t found RDF triples using this model :(</p>');
				noty({
					text : availability.text,
					type : 'warning',
					layout : 'top',
					closeWith : [ 'hover' ],
					timeout : 5000
				});
			}
		});
	}
}

function fillResponse(triples, elapsedTime) {
	var length = triples.length, element = null, counter = 0;
	for ( var i = 0; i < length; i++) {
		element = triples[i];
		// filter only predicted as positives
		if (element.pclass == '-1')
			continue;
		counter++;
		// TODO: consider feedback <div class="feedback"><a class="mini-listing
		// gray button" href="#"></a></div>
		// http://jsfiddle.net/dTLaF/1/
		if (element.exists) {
			$('#responseTable > tbody:last').append(
					'<tr class="oldtriple"><td>' + element.pclass + '</td><td>' + element.conf + '</td><td>'
							+ escapeHtml(element.triple.subj) + '</td><td>' + escapeHtml(element.triple.pred)
							+ '</td><td><span class="dbpedia" title="already in DBpedia">'
							+ escapeHtml(element.triple.obj) + '</span></td></tr>');
		} else {
			$('#responseTable > tbody:last').append(
					'<tr class="newtriple"><td>' + element.pclass + '</td><td>' + element.conf + '</td><td>'
							+ escapeHtml(element.triple.subj) + '</td><td>' + escapeHtml(element.triple.pred)
							+ '</td><td><span>' + escapeHtml(element.triple.obj) + '</span></td></tr>');
		}
	}
	// shows table if we found more than one triple
	if (counter > 0) {
		$('#message').html('<p style="color: green;">We found ' + counter + ' RDF triples ' + elapsedTime + '</p>');
		var oTable = $('#responseTable').dataTable({
			"bJQueryUI" : true,
			"sPaginationType" : "full_numbers",
			"iDisplayLength" : 50,
			"bDestroy" : true,
			"bRetrieve" : true,
			"aaSorting" : [ [ 1, "desc" ] ],
			/* "bProcessing" : true, */
			"aLengthMenu" : [ [ 50, 100, 500, -1 ], [ 50, 100, 500, "All" ] ],
			"aoColumnDefs" : [ {
				"bVisible" : false,
				"aTargets" : [ 0 ]
			} ],
			"sDom" : '<"top"<"thresholdFilter">filp<"clear">>rt<"bottom"ip<"clear">>'
		});

		$("div.thresholdFilter").html(
				'<span>Min. confidence:</span> <input id="min" type="text" value="0.0" name="min">');

		/* Add highlight to old triples */
		oTable.$('tr').hover(function() {
			oTable.$('tr').addClass('highlighted');
		}, function() {
			oTable.$('tr.highlighted').removeClass('highlighted');
		});

		/* Add event listener to the minimum threshold filter */
		$('#min').keyup(function() {
			oTable.fnDraw();
		});

		$('#tabularResponse').show();
	} else {
		$('#message').html('<p style="color: red;">We didn\'t found RDF triples using this model :(</p>');
		$('#tabularResponse').remove();
	}
	return counter;
}

/*
 * Custom filtering function which will filter data in column one according
 * minimum
 */
$.fn.dataTableExt.afnFiltering.push(function(oSettings, aData, iDataIndex) {
	var minvalue = $('#min').val();
	if (typeof minvalue == "undefined")
		return true;

	var threshold = minvalue * 1;
	var conf = aData[1] == "" ? 0 : aData[1] * 1;
	if (threshold == "") {
		return true;
	} else if (threshold <= conf) {
		return true;
	}
	return false;
});

var entityMap = {
	"&" : "&amp;",
	"<" : "&lt;",
	">" : "&gt;",
	'"' : '&quot;',
	"'" : '&#39;',
	"/" : '&#x2F;'
};

function escapeHtml(string) {
	return String(string).replace(/[&<>"'\/]/g, function(s) {
		return entityMap[s];
	});
}

function welcome() {
	noty({
		text : '<strong>Welcome to ExReTa demo!</strong> <br /> Just search for a Wikipedia article, and <strong>ExReTa</strong> will do its best extracting RDF triples from article\'s tables (Wikitables). Each extracted triple will have a confidence according the selected model.',
		type : 'alert',
		layout : 'topLeft',
		closeWith : [ 'button' ]
	// ['click', 'button', 'hover']
	});
}