
function forceAttractive() {
	var nodes,
	node,
	alpha

	function force(_) {
		for (var i = 0, n = nodes.length, node, k = _ * 0.1; i < n; ++i) {
			node = nodes[i];
//			var mass_i = Math.PI * node.radius * node.radius;
			var F_x = 0, F_y = 0;
			for(var j = 0; j < node.nb.length; j++){
				var nb = node.nb[j];
				if (nb >= 0){
					neighbor = nodes[nb];
					var mass_j = Math.PI * neighbor.radius * neighbor.radius;

					var delta_x = node.x - neighbor.x;
					var delta_y = node.y - neighbor.y;

					var norm2 = Math.sqrt( delta_x * delta_x + delta_y * delta_y );

					var dij = Math.max(0, norm2) - (node.radius + neighbor.radius) ;

					var diff_x = delta_x/norm2;
					var diff_y = delta_y/norm2;

					F_x += mass_j * dij * diff_x;
					F_y += mass_j * dij * diff_y;
				}
			}

			node.vx += F_x * k;
			node.vy += F_y * k;
		}
	}

	function initialize() {
		if (!nodes) return;
		var i, n = nodes.length, node;
	}

	force.initialize = function(_) {
		nodes = _;
		initialize();
	};

	return force;
}

//Resolves collisions between d and all other circles.
function forceCollide() {
	var nodes;
	var quadtree;

	function force(alpha) {
		nodes.forEach(function(d) {
			var r = d.r + maxRadius + Math.max(padding, clusterPadding),
			nx1 = d.x - r,
			nx2 = d.x + r,
			ny1 = d.y - r,
			ny2 = d.y + r;
			quadtree.visit(function(quad, x1, y1, x2, y2) {
				if ( quad.data && (quad.data !== d)){
					var x = d.x - quad.data.x,
					y = d.y - quad.data.y,
					l = Math.sqrt(x * x + y * y) + 0.0001,
					r = d.r + quad.data.r + (d.cluster === quad.data.cluster ? //padding : clusterPadding);
							(d.r > 1 || quad.data.r > 1 ? padding : pointPadding) : clusterPadding);
					if (l < r) {
						l = (l - r) / l * alpha;
						d.vx -= x *= l;
						d.vy -= y *= l;
						quad.data.vx += x;
						quad.data.vy += y;
					}
				}
				return x1 > nx2 || x2 < nx1 || y1 > ny2 || y2 < ny1;
			});
		});
	}

	force.initialize = function(_) {
		var width = $("svg").width(),
		height = $("svg").height();

		nodes = _;
		quadtree = d3.quadtree()
		.extent([[-1,-1], [width+1,height+1]])
		.x((d) => d.x)
		.y((d) => d.y)
		.addAll(nodes);
	}

	return force;
}
