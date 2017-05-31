
var simulation;

//Form submission handle
$(document).ready(function() {
	$( "#searchForm" ).submit(function( e ) {
		e.preventDefault();
		ajaxSubmitForm();
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

	$("#step-btn").prop('disabled', true);
	$("#reheat-btn").prop('disabled', true);
	$("#reset-btn").click(resetVisualization);
});

//Initialize visualization
successFn = function(data){

	//Enable step button
	$("#step-btn").prop('disabled', false);
	$("#step-btn").off();
	$("#step-btn").click(nextVisualizationStep);
	
	//Reheat visualization
	$("#reheat-btn").click(reheat);
	
	//Disable click event onto table rows
	$(".documents-table table tbody tr").off();

	let step = 0;

	let margin = {top: 100, right: 100, bottom: 100, left: 100};
	let svg = d3.select("svg"),
	width = $("svg").width();
	height = $("svg").height(),
	svg.attr('width', width);
	svg.attr('height', height);

	//Zoom
	transform = d3.zoomIdentity;

	//Clear previous visualization
	svg.selectAll('g').remove();
	svg.selectAll('defs').remove();
	$(".documents-table table tbody tr").remove();

	let padding = 3, // separation between same-color nodes
	clusterPadding = 6, // separation between different-color nodes
	pointPadding = .5,
	maxRadius = width * 0.05;
	minRadius = width * 0.005;
//	maxRadius = 70;
//	minRadius = 7;

	let n = data.documents.length, // total number of documents
	m = data.nclusters; // number of distinct clusters
	
	// If no documents, return.
	if ( n == 0 ){
		return;
	}

	// Cluster colors
	var color = d3.scaleSequential(d3.interpolateRainbow)
	.domain([0, m]);

	// Get max/min relevance
	var maxRev = data.documents[0].relevance,
	minRev = data.documents[data.documents.length-1].relevance;

	// Radius interpolator based on document relevance
	var radiusInterpolator = d3.scaleLinear()
	.domain([minRev, maxRev])
	.range([minRadius, maxRadius])
	.interpolate(d3.interpolateRound);

	// The largest node for each cluster.
	var clusters = new Array(m);
	var links = [];

	// Transform x,y MDP coordinates to range [-1,1]
	var x = d3.scaleLinear().domain([-1,1]).range([0, width]);
	var y = d3.scaleLinear().domain([-1,1]).range([0, height]);

	var maxArea = width * height,
	sumArea = Math.PI * clusterPadding * clusterPadding * m,
	maxRevToShow = 0;

	//Create nodes
	var nodes = $.map(data.documents, function(doc, index){
		if (doc.x != undefined && doc.y != undefined ){
			var radius = radiusInterpolator(doc.relevance);
			
			sumArea += 4 * (radius + padding)  * (radius + padding); //aprox. to square
			if ( sumArea <= maxArea)
				maxRevToShow = doc.relevance;
			
			d = {
					docId: doc.docId,
					cluster: doc.cluster,
					r: sumArea <= maxArea ? radius : 1,
					x: x(doc.x),
					y: y(doc.y),
					title: doc.title,
					authors: doc.authors,
					doi: doc.doi,
					year: doc.publicationDate,
					keywords: doc.keywords,
					relevance: doc.relevance
			};

			//Set links
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
			
			//Add to documents table
			var row = '<tr><td class="doc-index">' + index + '</td><td class="doc-title">' + doc.title +
				'</td><td class="doc-authors">';
			if ( doc.authors )
				row += doc.authors;
			row += '</td><td class="doc-year">';
			if ( doc.publicationDate )
				row += doc.publicationDate;
			row += '</td><td class="doc-doi">'
			if ( doc.doi )
				row += '<a href="https://dx.doi.org/' + d.doi + '" target="_blank">' + 
				doc.doi + '</a>';
			
			row += '</td><td class="doc-relevance">' + (doc.relevance * 100 ).toFixed(3) + '</td><td class="doc-cluster">' + 
				'<svg><circle cx="15" cy="15" r="10" stroke-width="0" fill="' + color(doc.cluster) + '"/></svg>'  +
				'</td></tr>';
			$('.documents-table .table tbody').append(row);

			//Update cluster medoid
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

	// Create points
	var g = svg.append("g");
	let points = g
	.datum(nodes)
//	.order()
	.selectAll('.circle')
	.data(d => d)
	.enter().append('circle')
	.attr('r', (d) => d.r)
	.attr('cx', (d) => d.x)
	.attr('cy', (d) => d.y)
	.attr('fill', (d) => color(d.cluster))
	.attr('stroke', 'black')
	.attr('stroke-width', 1)
//	.call(d3.drag() 
//	.on("start", dragstarted)
//	.on("drag", dragged)
//	.on("end", dragended));

	let circles = points
	.filter((d,i) => d.relevance >= maxRevToShow);

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

	//Zoom
	svg.call(d3.zoom() 
			.extent([[0,0], [width,height]])
//			.translateExtent([[0,0],[width,height]])
			.scaleExtent([ 1 / 2, 8])
			.on("zoom", zoomed));

	++step;

	// ###### Finish first step ######

	// Second step: set circle's radius
	function secondStep(){
		circles
		.attr('r', (d) => d.r)
		.attr('fill', (d) => color(d.cluster))
		.attr('fill-opacity', (d) => 0.3);
	}

	//Third step: initialize force scheme
	function thirdStep(){
		//Collision force
		let forceCollide = d3.forceCollide()
		.radius((d) => d.r > 1 ? d.r + padding : d.r + pointPadding)
		.strength(0.9)
		.iterations(1);

		//Link force (citations)
		var forceLink = d3.forceLink()
		.id((d) => d.docId)
		.links(links)
		.strength(0)
		.distance(0);

		//Initialize simulation
		simulation = d3.forceSimulation(nodes)
		.force("center", d3.forceCenter( width / 2, height / 2))
		.force("cluster", forceCluster)
		.force("gravity", d3.forceManyBody(30))
		.force("collide2", forceCollide)
		.force("x", d3.forceX(width/2).strength(.5))
		.force("y", d3.forceY(height/2).strength(.5))
		.force('link', forceLink)
		.force('edge-left', edgeForce('x', 0, 1000))
		.force('edge-right', edgeForce('x', width, 1000))
		.force('edge-top', edgeForce('y', 0, 1000))
		.force('edge-bottom', edgeForce('y', height, 1000))
		.on("tick", ticked)
		.on("end", endSimulation);
	}

	// Simulation has finished
	function endSimulation(){
		circles
		.attr("fill-opacity", (d) => 1)
		.on("mouseover", showTip)
		.on("mouseout", hideTip)
		.on("click", toggleLinks);

		$("#reheat-btn").prop('disabled', false);
		
		//Make rows clickable
		$(".documents-table table tbody tr").click(selectRow);
		$(".documents-table table tbody tr").on('mouseover', function(){
			var point = getPoint(this);
			point.dispatch('mouseover');
		});
		$(".documents-table table tbody tr").on('mouseout', function(){
			var point = getPoint(this);
			point.dispatch('mouseout');
		});
		
	}

	// Handle zoom
	function zoomed(){
		g.attr("transform", d3.event.transform)
		.selectAll("path")
		.attr('stroke-width', 1.5 *  1 / d3.event.transform.k);
		g.selectAll("circle")
		.style("stroke-width", 4 * 1 / d3.event.transform.k);
	}

	// Edge force to maintain disks within boundaries
	function edgeForce(axis, origin, strength) {
		var nodes;

		function force(alpha) {
			nodes.forEach(function(node) {
				Math.max(Math.abs(origin - node[axis]), node.r)


				var delta = strength / (origin - node[axis]) * alpha;
				var repulsion = node.r * strength / 10000

				node[axis] -= delta;
			})
		}

		force.initialize = function(_) {
			nodes = _;
		}

		return force;
	}

	// Cluster force
	function forceCluster(alpha) {
		for (var i = 0, n = nodes.length, node, cluster, k = alpha * 1.2; i < n; ++i) {
			node = nodes[i];
			cluster = clusters[node.cluster];
			node.vx -= (node.x - cluster.x) * k;
			node.vy -= (node.y - cluster.y) * k;
		}
	}

	// Execute at each simulation iteration
	function ticked(){

		const width = $("svg").width();
		const height = $("svg").height();

		points
		.attr("cx", (d) => d.x = Math.max(d.r, Math.min(width - d.r, d.x)))
		.attr("cy", (d) => d.y = Math.max(d.r, Math.min(height - d.r, d.y)));

		path
		.attr("d", linkArc);
	}

	// Show tips when mouse over
	function showTip(d){
		tip.transition()
		.duration(200)
		.style("opacity", 0.9)
		.style("display", "block");

		var tipHtml = '<a href="https://dx.doi.org/' + d.doi + '" target="_blank"><p>';
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

	// Hide tips
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

	//Toggle links (citations) when selected
	function toggleLinks(d,index){
		var i;
		var paths = d3.selectAll('path.link').nodes();
		for(i = 0; i < links.length; i++){
			if (links[i].source == d){
				var l = d3.select(paths[links[i].index]);
				l.classed('active', !l.classed('active'));
			}
		}
		
		// Set row as active
		var row = $(".documents-table table tbody tr")[index];
		$(row).toggleClass('success');
	}
	
	function selectRow(){
		var point = getPoint(this);
		point.dispatch('click', {detail: this});
	}
	
	function getPoint(row){
		var index = parseInt($(row).children('td.doc-index').html());
		return points.filter(function (d, i) { return i === index;});
	}

	// Show arc (links)
	function linkArc(l) {
		let dx = l.target.x - l.source.x,
		dy = l.target.y - l.source.y,
		dr = Math.sqrt(dx * dx + dy * dy);
		return "M" + l.source.x + "," + l.source.y + "A" + dr + "," + dr + " 0 0,1 " + l.target.x + "," + l.target.y;
	}

	// Start dragging
	function dragstarted(d) {
		if (!d3.event.active) simulation.alphaTarget(0.3).restart();
		d.fx = d.x;
		d.fy = d.y;
		d3.select(this).raise().classed("active", true);
	}

	// Dragging
	function dragged(d) {
		d.fx = d3.event.x;
		d.fy = d3.event.y;
	}

	// End dragging
	function dragended(d) {
		if (!d3.event.active) simulation.alphaTarget(0);
		d.fx = null;
		d.fy = null;
		d3.select(this).classed("active", false);
	}

	function nextVisualizationStep(){
		if ( step == 1){
			secondStep();
			++step;
		}
		else if ( step == 2){
			thirdStep();
			++step;
			$('#step-btn').prop('disabled',true);
		}
	}

	function reheat(){
		simulation
		.alpha(0.01)
		.alphaTarget(0.001)
		.restart();
	}
}

//Submission error 
errorFn = function(err){
	console.debug("Error:");
	console.debug(err);
}

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
}
