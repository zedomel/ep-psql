var simulation;

//Form submission handle
$(document).ready(function() {
	$( "#searchForm" ).submit(function( e ) {
		e.preventDefault();
		ajaxSubmitForm();
	});

	$('#collision-force').slider({
		formatter: function(value) {
			return value;
		}
	});
	
	$('#manybody-force').slider({
		formatter: function(value) {
			return value;
		}
	});
	
	$("#viz-settings-btn").click(function(e){
		if ( $(".viz-settings").hasClass("enabled")){
			$(".viz-settings").hide(400, function(){
				$(".viz-settings").removeClass("enabled");
			});
		}
		else{
			$(".viz-settings").show(400, function(){
				$(".viz-settings").addClass("enabled");
			});
		}
	});
	
	$("#advanced-search-btn").click(function(e){
		if ( $(".advanced-search").hasClass("enabled")){
			$(".advanced-search").hide(400, function(){
				$("#advanced-search-btn span").removeClass("glyphicon-chevron-up");
				$("#advanced-search-btn span").addClass("glyphicon-chevron-down");
				$(".advanced-search").removeClass("enabled");
			});
		}
		else{
			$(".advanced-search").show(400, function(){
				$("#advanced-search-btn span").removeClass("glyphicon-chevron-down");
				$("#advanced-search-btn span").addClass("glyphicon-chevron-up");
				$(".advanced-search").addClass("enabled");
			});
		}
	});

	$(".advanced-search").hide();
	$(".viz-settings").hide();

	$("#step-btn").prop('disabled', true);
	$("#reheat-btn").prop('disabled', true);
	$("#reset-btn").click(resetVisualization);
});

//Submission error 
errorFn = function(err){
	$('#loading').removeClass('hidden');
	console.debug("Error:");
	console.debug(err);
};

//Reset visualization
function resetVisualization(e){
	d3.selectAll('path.link').classed('active', false);
	$(".documents-table table tbody tr").removeClass('success');
}

//Ajax form submission
function ajaxSubmitForm(){
	var t = $("#terms").val(),
	op = $("#operator").val(),
	author = $("#author").val(),
	yearS = $("#year-start").val(),
	yearE = $("#year-end").val(),
	numClusters = $("#num-clusters").val();

	var r = jsRoutes.controllers.HomeController.search();
	$.ajax({url: r.url, type: r.type, data: {
		terms: t, 
		operator: op,
		author: author,
		yearStart:  yearS,
		yearEnd: yearE,
		numClusters: numClusters
	}, 
	success: successFn, error: errorFn, dataType: "json"});
	
	//Waiting...
	$('#loading').removeClass('hidden');
}
