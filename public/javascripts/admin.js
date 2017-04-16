$(document).ready(function() {
	$( "#update-mdp" ).click(function( e ) {
		ajaxSubmit("mdp");
	});
	
	$( "#update-pagerank" ).click(function( e ){
		ajaxSubmit("pagerank");
	});
});

function ajaxSubmit(type){
	var r = jsRoutes.controllers.AdminController.update();
	$.ajax({url: r.url, type: r.type, 
		data: {
			update: type
		}, 
		success: successFn, 
		error: errorFn
	});
}

successFn = function(data){
	
}

errorFn = function(data){
	
}