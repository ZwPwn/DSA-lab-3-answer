package graphSearch;

import java.util.*;

import graphClient.XGraphClient;

public class GraphSearch {

	public int afm = 58105;
	public String firstname = "{REDUCTED / CHANGE-ME}";
	public String lastname = "{REDUCTED / CHANGE-ME}";

	XGraphClient xgraph;

	public GraphSearch(XGraphClient xgraph) {
		this.xgraph = xgraph;
	}

    public Result findResults() {
        Result res = null;

        res = new Result();
        
        SGraph localGraph = new SGraph();
        
        long firstNode = xgraph.firstNode();
        
        // DFS (LIFO) -> Stack
        // Εδώ μπορώ να έχω κοινή δομή για να ελέγχω αν έχω επισκευτεί ή όχι τον κόμβο (παραλείπω το visited)
        Stack<Long> stack = new Stack<Long>();
        stack.push(firstNode);
        
        while(!stack.isEmpty()) {
        	long currentNode = stack.pop();
        	if (!res.dfsNodeSequence.contains(currentNode)) {
        		localGraph.addNode(currentNode);
        		res.dfsNodeSequence.add(currentNode);
        		long[] neighbours = xgraph.getNeighborsOf(currentNode);
        		Arrays.sort(neighbours);
        		
        		for (int i = neighbours.length - 1; i >= 0; i--) {
        			long dist = xgraph.getEdgeWeight(currentNode, neighbours[i]);
        			localGraph.addEdge(new SEdge(currentNode, neighbours[i], dist));
        			stack.push(neighbours[i]);
        		}
        	}
        }
        
        // BFS (FIFO) -> Queue
        // Check for Cycles
        // MaxDistance from source
        // Θέλω την σειρά, οπότε για να μειώσω τον χρόνο εκτέλεσης κάνω recall locally
        Queue<Long> queue = new LinkedList<Long>();
        Set<Long> visited = new HashSet<Long>();
        Map<Long, Long> parent = new HashMap<>();
        
        queue.add(firstNode);
        visited.add(firstNode);
        parent.put(firstNode, -1L);
        
        boolean hasCycle = false;
        
        while(!queue.isEmpty()) {
        	long currentNode = queue.poll();
    		res.bfsNodeSequence.add(currentNode);
    		
    		Long[] neighbours = localGraph.getNeighborsOf(currentNode);
    		Arrays.sort(neighbours);
    		
    		for (long neighbor : neighbours) {
    			if (!visited.contains(neighbor))
    			{
    				visited.add(neighbor);
    				queue.add(neighbor);
    				parent.put(neighbor, currentNode);
    			} else if (neighbor != parent.get(currentNode)) {
    				hasCycle = true;
    			}
    		}
        }

        // Dijkstra -> Priority Queue
        visited.clear();
        res.maxDistance = Long.MIN_VALUE;
        Map<Long, Long> dist = new HashMap<>();
        
        for (long v: localGraph.V) {
            dist.put(v, Long.MAX_VALUE);
        }

        PriorityQueue<Long> pq = new PriorityQueue<Long>((n1, n2) -> Long.compare(dist.get(n1), dist.get(n2)));
        
        pq.add(firstNode);
        dist.put(firstNode, 0L);
        
        while(!pq.isEmpty()) {
            long currentNode = pq.poll();
            
            if (!visited.contains(currentNode)) {
                visited.add(currentNode);
                Long[] neighbors = localGraph.getNeighborsOf(currentNode);
                
                for (long neighbor: neighbors) {
                    long weight = localGraph.getEdge(currentNode, neighbor).getWeight();
                    
                    if (dist.get(currentNode) != Long.MAX_VALUE && dist.get(currentNode) + weight < dist.get(neighbor)) {
                        dist.put(neighbor, dist.get(currentNode) + weight);
                        pq.add(neighbor);
                    }
                }
            }
        }
        
        long furthestNode = firstNode;
        for (Map.Entry<Long, Long> entry : dist.entrySet()) {
            long val = entry.getValue();
        	if (val != Long.MAX_VALUE && val > res.maxDistance) {
        		res.maxDistance = val;
                furthestNode = entry.getKey();
        	}
        }
        
        if (hasCycle) {
        	// Αν δεν έχει κύκλους τότε μπορώ O(V(V+E))
        	// All-Pairs Longest Path (NP-hard)
        	// Ειδικό Case για περαταίρω μείωση του χρόνου αν έχω Hamiltonian-Cycles
        	for (long startNode : localGraph.V) {
                visited.clear();
                pq.clear();
                for (long v : localGraph.V) {
                    dist.put(v, Long.MAX_VALUE);
                }
                
                dist.put(startNode, 0L);
                pq.add(startNode);
                
                while(!pq.isEmpty()) {
                    long currentNode = pq.poll();
                    
                    if (!visited.contains(currentNode)) {
                        visited.add(currentNode);
                        Long[] neighbors = localGraph.getNeighborsOf(currentNode);
                        
                        for (long neighbor : neighbors) {
                            long weight = localGraph.getEdge(currentNode, neighbor).weight;
                            
                            if (dist.get(currentNode) != Long.MAX_VALUE && dist.get(currentNode) + weight < dist.get(neighbor)) {
                                dist.put(neighbor, dist.get(currentNode) + weight);
                                pq.add(neighbor);
                            }
                        }
                    }
                }
                
                for (long val : dist.values()) {
                    if (val != Long.MAX_VALUE && val > res.graphDiameter) {
                        res.graphDiameter = val;
                    }
                }
            }
        } else {
        	// Αν έχει κύκλους τότε μπορώ να χρησιμοποιήσω O(V+E)
            // Δεύτερο πέρασμα από το πιο απομακρυσμένο κόμβο ως αφετηρία (διαφορετικά μπορούσαμε και Bellman-Ford εξ'αρχής, ο οποίος λειτουργεί και για αρνητικά βάρη)
            visited.clear();
            pq.clear();
            res.graphDiameter = Long.MIN_VALUE;
            
            for (long v: localGraph.V) {
            	dist.put(v, Long.MAX_VALUE);
            }
            
            pq.add(furthestNode);
            dist.put(furthestNode, 0L);
            
            while(!pq.isEmpty()) {
            	long currentNode = pq.poll();
            	
            	if (!visited.contains(currentNode)) {
            		visited.add(currentNode);
            		Long[] neighbors = localGraph.getNeighborsOf(currentNode);
            		
    	        	for (long neighbor: neighbors) {
    	        		long weight = localGraph.getEdge(currentNode, neighbor).weight;
    	        		
    	        		if (dist.get(currentNode) != Long.MAX_VALUE && dist.get(currentNode) + weight < dist.get(neighbor)) {
    	        			dist.put(neighbor, dist.get(currentNode) + weight);
    	        			pq.add(neighbor);
    	        		}
    	        	}
            	}
            }
            
            for (long val: dist.values()) {
            	if (val != Long.MAX_VALUE && val > res.graphDiameter) {
            		res.graphDiameter = val;
            	}
            }
        }
        
        System.out.println("The graph has Cycles = " + hasCycle);
        
        res.n = localGraph.V.size();
        res.m = localGraph.E.size();
        
        return res;
    }
}
