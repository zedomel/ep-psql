
function forceAttractive() {
  var nodes,
      node,
      alpha

  function force(_) {
	  for (var i = 0, n = nodes.length, node, k = _ * 0.1; i < n; ++i) {
		  node = nodes[i];
//		  var mass_i = Math.PI * node.radius * node.radius;
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
