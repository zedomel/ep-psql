
var simulation;
$(document).ready(function() {
	$( "#searchForm" ).submit(function( e ) {
		e.preventDefault();
		ajaxSubmitForm();
	});
	
	$("#advanced-search-btn").click(function(e){
		if ( $(".advanced-search").hasClass("enabled")){
			$(".advanced-search").hide(400, function(){
				$("#advanced-search-btn").removeClass("glyphicon-chevron-up");
				$("#advanced-search-btn").addClass("glyphicon-chevron-down");
				$(".advanced-search").removeClass("enabled");
			});
		}
		else{
			$(".advanced-search").show(400, function(){
				$("#advanced-search-btn").removeClass("glyphicon-chevron-down");
				$("#advanced-search-btn").addClass("glyphicon-chevron-up");
				$(".advanced-search").addClass("enabled");
			});
		}
	});
	
	$(".advanced-search").hide();
	
	$("#reset-btn").click(resetVisualization);
});

successFn = function(data){
	let margin = {top: 100, right: 100, bottom: 100, left: 100};
	let svg = d3.select("svg"),
	width = $("svg").width();
	height = $("svg").height(),
	svg.attr('width', width);
	svg.attr('height', height);
	
	transform = d3.zoomIdentity;

	//Clear previous visualization
	svg.selectAll('g').remove();
	svg.selectAll('defs').remove();

	let padding = 3, // separation between same-color nodes
	clusterPadding = 6, // separation between different-color nodes
	maxRadius = width * 0.05;
	minRadius = width * 0.005;

	let n = data.documents.length, // total number of documents
	m = data.nclusters; // number of distinct clusters

	svg = svg.append("g").attr('transform', 'translate(' + width / 2 + ',' + height / 2 + ')');

	var color = d3.scaleSequential(d3.interpolateRainbow)
	.domain([0, m]);

	var minRev = data.documents[0].relevance,
	maxRev = minRev;

	var i = 0;
	for(i = 1; i < n; i++){
		var rev = data.documents[i].relevance;
		if (rev < minRev)
			minRev = rev;
		if (rev > maxRev)
			maxRev = rev;
	}

	var radiusInterpolator = d3.scaleLinear()
	.domain([minRev, maxRev])
	.range([minRadius, maxRadius])
	.interpolate(d3.interpolateRound);

	// The largest node for each cluster.
	var clusters = new Array(m);
	var links = [];

	var nodes = $.map(data.documents, function(doc, index){
		if (doc.x != undefined && doc.y != undefined ){
//			var neighbors = cluster.attributes.neighbors;
			var radius = radiusInterpolator(doc.relevance);
			d = {
					docId: doc.docId,
					cluster: doc.cluster,
					r: radius,
					x: doc.x,
					y: doc.y,
//					nb: neighbors[docIndex],
					title: doc.title,
//					url: doc.url,
					authors: doc.authors,
					doi: doc.doi,
					year: doc.publicationDate,
					keywords: doc.keywords
			};

			if ( doc.references ){
				var i;
				var references = doc.references;
				for(i = 0; i < references.length; i++){
					links.push({	
						source: doc.docId,
						target: references[i]
					});
				}
			}

			if (!clusters[ doc.cluster ] || (radius > clusters[ doc.cluster ].r)) clusters[ doc.cluster ] = d;
			return d;
		}
	});
	
	// Tooltip on mouse over
	let tip = d3.select("body").append("div")
	.attr("class", "tooltip")
	.style("opacity", 0);

	// Per-type markers, as they don't inherit styles.
	svg.append("defs").selectAll("marker")
	.data(["link"])
	.enter().append("marker")
	.attr("id", function(d) { return d; })
	.attr("viewBox", "0 -5 10 10")
	.attr("refX", 8)
	.attr("refY", 0)
	.attr("markerWidth", 6)
	.attr("markerHeight", 6)
	.attr("orient", "auto")
	.append("path")
	.attr("d", "M0,-5L10,0L0,5");

	var g = svg.append("g");
	let circles = g
	.datum(nodes)
	.selectAll('.circle')
	.data(d => d)
	.enter().append('circle')
	.attr('r', (d) => d.r)
	.attr('cx', (d) => d.x)
	.attr('cy', (d) => d.y)
	.attr('fill', (d) => color(d.cluster))
	.attr('stroke', 'black')
	.attr('stroke-width', 1)
	.on("mouseover", showTip)
	.on("mouseout", hideTip)
	.on("click", toggleLinks);
//	.call(d3.drag() 
//	.on("start", dragstarted)
//	.on("drag", dragged)
//	.on("end", dragended));

	let path = g
	.selectAll("path")
	.data(links)
	.enter().append("path")
	.attr("class", "link")
	.attr('stroke-width', 1.5)
//	.call(d3.drag()
//	.on("start", dragstarted)
//	.on("drag", dragged)
//	.on("end", dragended))
	.attr("marker-end", function(d) { return "url(#link)"; });
	
	svg.call(d3.zoom() 
			.scaleExtent([ 1 / 2, 8])
			.on("zoom", zoomed));

	let forceCollide = d3.forceCollide()
	.radius(function(d) { return d.r + padding; } )
	.iterations(1);

	var forceLink = d3.forceLink()
	.id(function(d){
		return d.docId;
	})
	.links(links)
	.strength(0)
	.distance(0);
	
	simulation = d3.forceSimulation(nodes)
//	.force("center", d3.forceCenter( width / 2, height / 2))
	.force("collide", forceCollide)
	.force("cluster", forceCluster)
	.force("gravity", d3.forceManyBody(30))
	.force("x", d3.forceX().strength(.7))
	.force("y", d3.forceY().strength(.7))
	.force('link', forceLink)
	.on("tick", ticked);

	function zoomed(){
		g.attr("transform", d3.event.transform)
			.selectAll("path")
			.attr('stroke-width', 1.5 *  1 / d3.event.transform.k);
		g.selectAll("circle")
			.style("stroke-width", 4 * 1 / d3.event.transform.k);
	}
	
	function forceCluster(alpha) {
		for (var i = 0, n = nodes.length, node, cluster, k = alpha * 1; i < n; ++i) {
			node = nodes[i];
			cluster = clusters[node.cluster];
			node.vx -= (node.x - cluster.x) * k;
			node.vy -= (node.y - cluster.y) * k;
		}
	}

	function ticked(){
		circles
		.attr('cx', (d) => d.x)
		.attr('cy', (d) => d.y);
		
		path
			.attr("d", linkArc);
	}

	function showTip(d){
		tip.transition()
		.duration(200)
		.style("opacity", 0.9)
		.style("display", "block");

		var tipHtml = '<a href="https://dx.doi.org/" target="_blank"><p>';
		if (d.title)
			tipHtml += "<strong>" + d.title + "</strong>";
		if ( d.authors )
			tipHtml += ", " + d.authors;
		if ( d.year )
			tipHtml +=  ", " + d.year;
		tipHtml += "</p></a>";
		tip.html(tipHtml)
		.style("left", (d3.event.pageX + d.r + 2) + "px")
		.style("top", (d3.event.pageY + d.r + 2) + "px");
		d3.select(this).style("stroke-opacity", 1);
	}

	function hideTip(d){
		d3.select(this).style("stroke-opacity", 0);
		// User has moved off the visualization and onto the tool-tip
		tip.on("mouseover", function (t) {
			tip.on("mouseleave", function (t) {
				tip.transition().duration(500)
				.style("opacity", 0)
				.style("display", "none");

			});
		});
	}

	function toggleLinks(d){
		var i;
		var paths = d3.selectAll('path.link').nodes();
		for(i = 0; i < links.length; i++){
			if (links[i].source == d){
				var l = d3.select(paths[links[i].index]);
				l.classed('active', !l.classed('active'));
			}
		}
	}

	function linkArc(l) {
		let dx = l.target.x - l.source.x,
		dy = l.target.y - l.source.y,
		dr = Math.sqrt(dx * dx + dy * dy);
		return "M" + l.source.x + "," + l.source.y + "A" + dr + "," + dr + " 0 0,1 " + l.target.x + "," + l.target.y;
	}

	function dragstarted(d) {
		if (!d3.event.active) simulation.alphaTarget(0.3).restart();
		d.fx = d.x;
		d.fy = d.y;
		d3.select(this).raise().classed("active", true);
	}

	function dragged(d) {
		d.fx = d3.event.x;
		d.fy = d3.event.y;
	}

	function dragended(d) {
		if (!d3.event.active) simulation.alphaTarget(0);
		d.fx = null;
		d.fy = null;
		d3.select(this).classed("active", false);
	}
}

errorFn = function(err){
	console.debug("Error:");
	console.debug(err);
}

function resetVisualization(e){
	d3.selectAll('path.link').classed('active', false);
}

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
}
