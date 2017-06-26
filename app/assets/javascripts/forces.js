//Initialize visualization
successFn = function(data){

	$('#loading').addClass('hidden');
	
	//Enable step button
	$("#step-btn").prop('disabled', false);
	$("#step-btn").off();
	$("#step-btn").click(nextVisualizationStep);

	//Reheat visualization
	$("#reheat-btn").click(reheat);

	//Disable click event onto table rows
	$(".documents-table table tbody tr").off();

	var step = 0;

	var margin = {top: 100, right: 100, bottom: 100, left: 100};
	var svg = d3.select("svg"),
	width = $("svg").width(),
	height = $("svg").height();
	
	svg.attr('width', width);
	svg.attr('height', height);

	//Clear previous visualization
	svg.selectAll('g').remove();
	svg.selectAll('defs').remove();
	$(".documents-table table tbody tr").remove();

	var padding = 3, // separation between same-color nodes
	clusterPadding = 6, // separation between different-color nodes
	pointPadding = 1,
	maxRadius = width * 0.05,
	minRadius = width * 0.005;

	var n = data.documents.length, // total number of documents
	m = data.nclusters; // number of distinct clusters

	// If no documents, return.
	if ( n === 0 ){
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

	// Transform x,y MDP coordinates from range [-1,1] to [0, width/height]
	var x = d3.scaleLinear().domain([-1,1]).range([0, width]);
	var y = d3.scaleLinear().domain([-1,1]).range([0, height]);

	var maxArea = width * height,
	sumArea = Math.PI * clusterPadding * clusterPadding * m,
	maxRevToShow = 0;

	//Create nodes
	var nodes = createNodes(data.documents);

	// Tooltip on mouse over
	var tip = d3.select("body").append("div")
	.attr("class", "node-tooltip")
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


	var quadtree = d3.quadtree()
	.extent([[-1,-1], [width+1,height+1]])
	.x(function(d) { return  d.x;})
	.y(function(d) { return  d.y;})
	.addAll(nodes);

	// Create points
	var g = svg.append("g");
	var points = g
	.datum(nodes)
	.selectAll('.circle')
	.data(function(d) { return d;})
	.enter().append('circle')
	.attr('r', function(d) { return minRadius; })
	.attr('cx', function(d) { return d.x;})
	.attr('cy', function(d) { return  d.y;})
	.attr('fill', function(d) { return  color(d.cluster);})
	.attr('stroke', 'black')
	.attr('stroke-width', 1);

	var circles = points
	.filter(function(d,i) { return d.data.relevance >= maxRevToShow;});

	var path = g
	.selectAll("path")
	.data(links)
	.enter().append("path")
	.attr("class", "link")
	.attr('stroke-width', 1.5)
	.attr("marker-end", function(d) { return "url(#link)"; });

	++step;

	// ###### Finish first step ######

	// Second step: set circle's radius
	function secondStep(){
		circles
		.attr('r', function(d){ return  d.r; })
		.attr('fill', function(d) { return color(d.cluster);})
		.attr('fill-opacity', function(d) { return 0.3; });
	}

	//Third step: initialize force scheme
	function thirdStep(){

		var collideStrength = parseFloat( $("#collision-force").val() ),
		manyBodyStrength = parseFloat($("#manybody-force").val());

		var clusteringForceOn = $("#clustering-force").prop('checked');

		//Collision force
		var forceCollide = d3.forceCollide()
		.radius(function(d) { return d.r > minRadius ? d.r + padding : d.r + pointPadding; })
		.strength(collideStrength)
		.iterations(1);

		//Link force (citations)
		var forceLink = d3.forceLink()
		.id(function(d) { return d.docId; })
		.links(links)
		.strength(0)
		.distance(0);

		var forceGravity = d3.forceManyBody()
		.strength( manyBodyStrength );

		//Initialize simulation
		simulation = d3.forceSimulation(nodes)
		.force('link', forceLink)
		.force("collide", forceCollide)
		.force("gravity", forceGravity)
//		.force("attraction", attractionForce())
//		.force("center", d3.forceCenter( width / 2, height / 2))
//		.force("x", d3.forceX(width/2).strength(.0005))
//		.force("y", d3.forceY(height/2).strength(.0005))
//		.force('edge-left', edgeForce('x', 0, .2000))
//		.force('edge-right', edgeForce('x', width, .2000))
//		.force('edge-top', edgeForce('y', 0, .2000))
//		.force('edge-bottom', edgeForce('y', height, .2000))
		.on("tick", ticked)
		.on("end", endSimulation);

		if ( clusteringForceOn ){
			simulation.force("cluster", forceCluster());
		}

	}

	// Simulation has finished
	function endSimulation(){
		circles
		.attr("fill-opacity", 1)
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

	// Create nodes
	function createNodes(documents){
		return $.map(documents, function(doc, index){
			if (doc.x !== undefined && doc.y !== undefined ){
				var radius = radiusInterpolator(doc.relevance);

				sumArea += 4 * (radius + padding)  * (radius + padding);
				sumArea += Math.PI*(radius + padding)*(radius + padding);
				if ( sumArea <= maxArea)
					maxRevToShow = doc.relevance;

				d = {
						docId: doc.docId,
						cluster: doc.cluster,
						r: sumArea <= maxArea ? radius : minRadius,
								x: x(doc.x),
								y: y(doc.y),
								data: doc
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
				addDocumentToTable(index, doc);

				//Update cluster medoid
				if (!clusters[ doc.cluster ] || (radius > clusters[ doc.cluster ].r)) clusters[ doc.cluster ] = d;
				return d;
			}
		});
	}

	// Add document to table
	function addDocumentToTable(index, doc){
		var row = '<tr><td class="doc-index">' + index + '</td><td class="doc-title">' + doc.title +
		'</td><td class="doc-authors">';
		if ( doc.authors && doc.authors.length > 0){
			row += doc.authors[0].name;
			for(var i = 1; i < doc.authors.length; i++){
				row += "; " + doc.authors[i].name;
			}
		}
		row += '</td><td class="doc-year">';
		if ( doc.publicationDate )
			row += doc.publicationDate;
		row += '</td><td class="doc-doi">';
			if ( doc.doi )
				row += '<a href="https://dx.doi.org/' + d.doi + '" target="_blank">' + 
				doc.doi + '</a>';

		row += '</td><td class="doc-relevance">' + (doc.relevance * 100 ).toFixed(3) + '</td><td class="doc-cluster">' + 
		'<svg><circle cx="15" cy="15" r="10" stroke-width="0" fill="' + color(doc.cluster) + '"/></svg>'  +
		'</td></tr>';
		$('.documents-table .table tbody').append(row);
	}

	// Execute at each simulation iteration
	function ticked(){

		var width = $("svg").width();
		var height = $("svg").height();

		points
		.attr("cx", function(d) { return (d.x = Math.max(d.r, Math.min(width - d.r, d.x)));})
		.attr("cy", function(d) { return (d.y = Math.max(d.r, Math.min(height - d.r, d.y)));});

		path
		.attr("d", linkArc);
	}

	// Show tips when mouse over
	function showTip(n){
		tip.transition()
		.duration(500)
		.style("opacity", 0);
		
		tip.transition()
		.duration(200)
		.style("opacity", 0.9)
		.style("display", "block");

		var d = n.data;
		var tipHtml = '<a href="https://dx.doi.org/' + d.doi + '" target="_blank"><p>';
		if (d.title)
			tipHtml += "<strong>" + d.title + "</strong>";
		if ( d.authors && d.authors.length > 0){
			tipHtml +=", " + d.authors[0].name;
			for(var i = 1; i < d.authors.length; i++){
				tipHtml += "; " + d.authors[i].name;
			}
		}
		if ( d.publicationDate )
			tipHtml +=  ", " + d.publicationDate;
		tipHtml += "</p></a>";
		tip.html(tipHtml)
		.style("left", (n.x + n.r/2) + "px")
		.style("top", (n.y + n.r/2) + "px");
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
		var dx = l.target.x - l.source.x,
		dy = l.target.y - l.source.y,
		dr = Math.sqrt(dx * dx + dy * dy);
		return "M" + l.source.x + "," + l.source.y + "A" + dr + "," + dr + " 0 0,1 " + l.target.x + "," + l.target.y;
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
		var alpha = simulation.alpha();
		alpha += 0.01;
		simulation
		.alpha(alpha)
		.alphaTarget(0.001)
		.restart();
	}

	function forceCluster(){

		var nodes;

		function force(alpha) {
			nodes.forEach(function(d) {
				var cluster = clusters[d.cluster];
				if (cluster === d) return;
				var x = d.x - cluster.x,
				y = d.y - cluster.y,
				l = Math.sqrt(x * x + y * y),
				r = d.r + cluster.r;
				if (l !== r) {
					l = (l - r) / l * alpha;
					d.x -= x *= l;
					d.y -= y *= l;
					cluster.x += x;
					cluster.y += y;
				}  
			});
		}

		force.initialize = function(_) {
			var width = $("svg").width(),
			height = $("svg").height();

			nodes = _;
		};

		return force;
	}

	function attractionForce(){

		var nodes;

		function force(alpha) {
			nodes.forEach(function(d){
				var strength_x = 0, strength_y = 0;
				var mi = Math.PI * d.r * d.r;
				var neighbors = d.data.nb;
				neighbors.forEach(function(val, idx){
					var mj = Math.PI * val.r * val.r,
						dx = d.x - val.x,
						dy = d.y - val.y,
						dist = Math.sqrt(dx*dx + dy*dy),
						dij = Math.max(0, dist - (d.r + val.r));
						
						if ( dist > 0 ){
							strength_x += (d.r * val.r) / (d.r + val.r) * dij * dx / dist;
							strength_y += (d.r * val.r) / (d.r + val.r) * dij * dy / dist;
						}
				});
				
				d.x -= strength_x * alpha  / 10000;
				d.y -= strength_y * alpha  / 10000;
			});
		}

		force.initialize = function(_) {
			nodes = _;
			nodes.forEach(function(n){
				var neighbors = [];
				if (n.data.nb){
					n.data.nb.forEach(function(nb){
						var i = findNodeByDocId(nodes, nb);
						neighbors.push(i);
					});
				}
				n.data.nb = neighbors;
			});
		};

		return force;
	}
	
	function findNodeByDocId(nodes , docId){
		for(var i = 0; i < nodes.length; i++){
			if ( nodes[i].docId == docId){
				return nodes[i];
			}
		}
	}

//	Edge force to maintain disks within boundaries
	function edgeForce(axis, origin, strength) {
		var nodes;

		function force(alpha) {
			nodes.forEach(function(node) {
				var delta = strength * 10000 / (origin - node[axis]) * alpha;
				var repulsion = node.r * strength;

				node[axis] -= delta;
			});
		}

		force.initialize = function(_) {
			nodes = _;
		};

		return force;
	}
};