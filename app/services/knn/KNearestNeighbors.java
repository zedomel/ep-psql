package services.knn;

import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import services.quadtree.Node;
import services.quadtree.NodeType;
import services.quadtree.QuadTree;

public class KNearestNeighbors<T> {

	private DistanceFunction distFunction;

	public KNearestNeighbors() {
		this(new EuclideanDistance());
	}

	public KNearestNeighbors(DistanceFunction distanceFunction) {
		this.distFunction = distanceFunction;
	}

	public void kNearest(LinkedList<Node<T>> bestqueue, LinkedList<Node<T>>resultqueue, double x, double y, int k) {

		for( Node<T> n : bestqueue)
			calculateDistance(n,x,y);
		
		// add minidst to nodes if not there already
		bestqueue.sort((a,b) -> {
			return Double.compare(a.getMinDist(), b.getMinDist());
		});

		// add nearest leafs if any
		Iterator<Node<T>> iter = bestqueue.iterator();
		while( iter.hasNext() ){
			Node<T> elem = iter.next();
			if(elem.getNodeType() == NodeType.LEAF){
				iter.remove();
				resultqueue.push(elem);
			}else{
				break;
			}
			if(resultqueue.size() >=k){
				break;
			}
		}

		// check if enough points found
		if(resultqueue.size() >=k || bestqueue.size() == 0){
			// return if k neighbors found
			return;
		}else{
			// add child nodes to bestqueue and recurse
			Node<T> visitednode = bestqueue.pop();
			// add nodes to queue
			if ( visitednode.getSe() != null ){
				bestqueue.push(visitednode.getSe());
			}
			if ( visitednode.getSw() != null )
				bestqueue.push(visitednode.getSw());
			if ( visitednode.getNe() != null )
				bestqueue.push(visitednode.getNe());
			if ( visitednode.getNw() != null )
				bestqueue.push(visitednode.getNw());
			
			

			// recursion
			kNearest(bestqueue, resultqueue, x, y, k);
		}
	}
	
	private void calculateDistance(Node<T> n, double x, double y) {
		if ( n.getMinDist() < 0){
			if ( n.getNodeType() == NodeType.LEAF){
				n.setMinDist( distFunction.distance(x, y, n.getPoint().getX(), n.getPoint().getY()));
			}
			else{
				n.setMinDist(distFunction.minDist(x, y, n.getX(), n.getY(), 
						n.getX() + n.getW(), n.getY() + n.getH()));
			}
		}
	}

	public static void main(String[] args) {
		QuadTree<String> tree = new QuadTree<>(0, 0, 100, 100);
		
		tree.add(85,15, "Atlanta");
		tree.add(62,60, "Test");
		tree.add(82,65, "Buffalo");
		tree.add(35,42, "Chicago");
		tree.add(5,45, "Denver");
		tree.add(52,10, "Mobile");
		tree.add(27,35, "Omaha");
		tree.add(62,77, "Toronto");
		tree.add(90,5, "Miami");
		
		KNearestNeighbors<String> knn = new KNearestNeighbors<>();
		
		LinkedList<Node<String>> bestQueue = new LinkedList<>(), 
				resultQueue = new LinkedList<>();
		bestQueue.add(tree.getRootNode());
		knn.kNearest(bestQueue, resultQueue, 65, 62, 1);
		
		
		for(Node<String> city : resultQueue){
			System.out.println("Result: " + city.getPoint().getValue() + " " + 
		city.getPoint().getX() + " " + city.getPoint().getY() + " " + city.getMinDist());
		}
	}

}
