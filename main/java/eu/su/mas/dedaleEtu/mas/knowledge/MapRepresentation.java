package eu.su.mas.dedaleEtu.mas.knowledge;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.graphstream.algorithm.Dijkstra;
import org.graphstream.graph.Edge;
import org.graphstream.graph.EdgeRejectedException;
import org.graphstream.graph.Graph;
import org.graphstream.graph.Node;
import org.graphstream.graph.implementations.SingleGraph;
import org.graphstream.ui.fx_viewer.FxViewer;
import org.graphstream.ui.view.Viewer;
import org.graphstream.ui.view.Viewer.CloseFramePolicy;

import dataStructures.serializableGraph.*;
import dataStructures.tuple.Couple;
import eu.su.mas.dedale.env.Observation;
import eu.su.mas.dedaleEtu.mas.agents.dummies.ExploreSoloAgent;
import eu.su.mas.dedaleEtu.mas.knowledge.MapRepresentation.MapAttribute;
import javafx.application.Platform;

/**
 * <pre>
 * This simple topology representation only deals with the graph, not its content.
 * The knowledge representation is not well written (at all), it is just given as a minimal example.
 * The viewer methods are not independent of the data structure, and the dijkstra is recomputed every-time.
 * </pre>
 * @author hc
 */
public class MapRepresentation implements Serializable {

	/**
	 * A node is open, closed, or agent
	 * @author hc
	 *
	 */

	public enum MapAttribute {
		agent,open,closed
	}

	private static final long serialVersionUID = -1333959882640838272L;

	/*********************************
	 * Parameters for graph rendering
	 ********************************/

	private String defaultNodeStyle = "node {"+"fill-color: black;"+" size-mode:fit;text-alignment:under; text-size:14;text-color:white;text-background-mode:rounded-box;text-background-color:black;}";
	private String nodeStyle_open = "node.agent {"+"fill-color: forestgreen;"+"}";
	private String nodeStyle_agent = "node.open {"+"fill-color: blue;"+"}";
	private String nodeStyle = defaultNodeStyle+nodeStyle_agent+nodeStyle_open;

	private Graph g; //data structure non serializable
	private Viewer viewer; //ref to the display,  non serializable
	private Integer nbEdges;//used to generate the edges ids

	private SerializableSimpleGraph<String, MapAttribute> sg;//used as a temporary dataStructure during migration
	private List <String> nodeSmellList=  new ArrayList<String>();


	public MapRepresentation() {
		//System.setProperty("org.graphstream.ui.renderer","org.graphstream.ui.j2dviewer.J2DGraphRenderer");
		System.setProperty("org.graphstream.ui", "javafx");
		this.g = new SingleGraph("My world vision");
		this.g.setAttribute("ui.stylesheet", nodeStyle);

		Platform.runLater(() -> {
			openGui();
		});
		//this.viewer = this.g.display();

		this.nbEdges = 0;
	}

	/**
	 * Add or replace a node and its attribute 
	 * @param id Id of the node
	 * @param mapAttribute associated state of the node
	 */
	public void addNode(String id, MapAttribute mapAttribute, long time_visited, String ID_agent, boolean present, double proba_golem){
		Node n;
		if (this.g.getNode(id)==null){
			n=this.g.addNode(id);
		}else{
			n=this.g.getNode(id);
		}
		n.clearAttributes();
		n.setAttribute("ui.class", mapAttribute.toString());
		//System.out.println("map attribut : " + mapAttribute.toString());
		n.setAttribute("ui.label",id); //id node
		n.setAttribute("lastVisited", time_visited); // the last time for visited the node
		n.setAttribute("agent_present", ID_agent); // ID agent -1 if not agent
		n.setAttribute("golem_scent", present); // true or false
		n.setAttribute("proba_golem_present", proba_golem); // present probability of golem
	}

	/**
	 * Add the edge if not already existing.
	 * @param idNode1 one side of the edge
	 * @param idNode2 the other side of the edge
	 */
	public void addEdge(String idNode1, String idNode2){
		try {
			this.nbEdges++;
			this.g.addEdge(this.nbEdges.toString(), idNode1, idNode2);
		}catch (EdgeRejectedException e){
			//Do not add an already existing one
			this.nbEdges--;
		}

	}

	/**
	 * Compute the shortest Path from idFrom to IdTo. The computation is currently not very efficient
	 * 
	 * @param idFrom id of the origin node
	 * @param idTo id of the destination node
	 * @return the list of nodes to follow
	 */
	public List<String> getShortestPath(String idFrom,String idTo){
		List<String> shortestPath=new ArrayList<String>();

		Dijkstra dijkstra = new Dijkstra();//number of edge
		dijkstra.init(g);
		dijkstra.setSource(g.getNode(idFrom));
		dijkstra.compute();//compute the distance to all nodes from idFrom
		List<Node> path=dijkstra.getPath(g.getNode(idTo)).getNodePath(); //the shortest path from idFrom to idTo
		Iterator<Node> iter=path.iterator();
		while (iter.hasNext()){
			shortestPath.add(iter.next().getId());
		}
		dijkstra.clear();
		shortestPath.remove(0);//remove the current position
		//System.out.println("envoyer par getShortmachin : " + shortestPath);
		return shortestPath;
	}

	/**
	 * Before the migration we kill all non serializable components and store their data in a serializable form
	 */
	public void prepareMigration(){
		this.sg= new SerializableSimpleGraph<String,MapAttribute>();
		Iterator<Node> iter=this.g.iterator();
		while(iter.hasNext()){
			Node n=iter.next();
			sg.addNode(n.getId(),(MapAttribute)n.getAttribute("ui.class"));
		}
		Iterator<Edge> iterE=this.g.edges().iterator();
		while (iterE.hasNext()){
			Edge e=iterE.next();
			Node sn=e.getSourceNode();
			Node tn=e.getTargetNode();
			sg.addEdge(e.getId(), sn.getId(), tn.getId());
		}

		closeGui();

		this.g=null;

	}

	/**
	 * After migration we load the serialized data and recreate the non serializable components (Gui,..)
	 */
	public void loadSavedData(){

		this.g= new SingleGraph("My world vision");
		this.g.setAttribute("ui.stylesheet",nodeStyle);

		openGui();

		Integer nbEd=0;
		for (SerializableNode<String, MapAttribute> n: this.sg.getAllNodes()){
			this.g.addNode(n.getNodeId()).setAttribute("ui.class", n.getNodeContent().toString());
			for(String s:this.sg.getEdges(n.getNodeId())){
				this.g.addEdge(nbEd.toString(),n.getNodeId(),s);
				nbEd++;
			}
		}
		System.out.println("Loading done");
	}

	/**
	 * Method called before migration to kill all non serializable graphStream components
	 */
	private void closeGui() {
		//once the graph is saved, clear non serializable components
		if (this.viewer!=null){
			try{
				this.viewer.close();
			}catch(NullPointerException e){
				System.err.println("Bug graphstream viewer.close() work-around - https://github.com/graphstream/gs-core/issues/150");
			}
			this.viewer=null;
		}
	}

	/**
	 * Method called after a migration to reopen GUI components
	 */
	private void openGui() {
		this.viewer =new FxViewer(this.g, FxViewer.ThreadingModel.GRAPH_IN_ANOTHER_THREAD);////GRAPH_IN_GUI_THREAD);
		viewer.enableAutoLayout();
		viewer.setCloseFramePolicy(FxViewer.CloseFramePolicy.CLOSE_VIEWER);
		viewer.addDefaultView(true);
		g.display();
	}
		
	/**
	 * Create a serializable data structure containing all the map data that needs to be sent to another agent
	 * First key: element type (ex: node, edge etc.)
	 * Second key: element id (ex: for node 1_0 etc.)
	 * Value: String list attribute's (object) Attribute (first key) NANI?
	 * @return the data structure that needs to be sent
	 */
	public HashMap<String, HashMap<String, ArrayList<String>>> prepareSendMap() {
		
		HashMap<String, HashMap<String, ArrayList<String>>> serialMap = new HashMap<String, HashMap<String, ArrayList<String>>>();
		
		//Nodes
		Iterator<Node> iter=this.g.iterator();
		HashMap<String, ArrayList<String>> nodeList = new HashMap<String, ArrayList<String>>();
		while(iter.hasNext()){
			Node n=iter.next();
			ArrayList<String> attributList = new ArrayList<String>();
			attributList.add(n.getAttribute("ui.class").toString());
			attributList.add(n.getAttribute("lastVisited").toString());
			attributList.add(n.getAttribute("agent_present").toString());
			attributList.add(n.getAttribute("golem_scent").toString());
			attributList.add(n.getAttribute("proba_golem_present").toString());
			nodeList.put(n.getId(), attributList);
		}
		serialMap.put("Nodes", nodeList);

		//Edges
		Iterator<Edge> iterE=this.g.edges().iterator();
		HashMap<String, ArrayList<String>> edgeList = new HashMap<String, ArrayList<String>>();
		while (iterE.hasNext()){
			Edge e=iterE.next();
			ArrayList<String> attributList = new ArrayList<String>();
			attributList.add(e.getSourceNode().getId());
			attributList.add(e.getTargetNode().getId());
			edgeList.put(e.getId(), attributList);
		}
		serialMap.put("Edges", edgeList);
		
		return serialMap;
	}
	
	/**
	 * Add new map data to the MapRepresentation
	 * @param mapData the new map data to merge into the MapRepresentation
	 */
	public void mergeMapData(HashMap<String, HashMap<String, ArrayList<String>>> mapData) {
		
		// Adding unknown nodes to the MapRepresentation
		HashMap<String, ArrayList<String>> nodes = (HashMap<String, ArrayList<String>>) mapData.get("Nodes");
		//System.out.println(nodes);
		for(String nodeID : nodes.keySet()) {
			if (this.g.getNode(nodeID) == null) { // node unknown
				addNode(nodeID, MapAttribute.valueOf(nodes.get(nodeID).get(0)), Long.parseLong(nodes.get(nodeID).get(1)), nodes.get(nodeID).get(2), Boolean.parseBoolean(nodes.get(nodeID).get(3)), Double.parseDouble(nodes.get(nodeID).get(4)));
			}
			else { // node known, just updating the open/closed attribute if necessary
				// TODO: voir comment traiter le cas si MapAttribute = agent (pb de datation de l'info)
				// Et à déplacer potentiellement dans addNode pour éviter un double check d'existence du noeud
				if ((nodes.get(nodeID).get(0).equals("closed")) && (this.g.getNode(nodeID).getAttribute("ui.class").toString().equals("open"))) {
					//si le time du noeud envoyé et plus récent
					if (Long.parseLong(nodes.get(nodeID).get(1)) < Long.parseLong(this.g.getNode(nodeID).getAttribute("lastVisited").toString())) {
						addNode(nodeID, MapAttribute.valueOf(nodes.get(nodeID).get(0)), Long.parseLong(nodes.get(nodeID).get(1)), nodes.get(nodeID).get(2), Boolean.parseBoolean(nodes.get(nodeID).get(3)), Double.parseDouble(nodes.get(nodeID).get(4)));
					}
					else {
						
					}
				}
			}
		}
		
		// Adding unknown edges to the MapRepresentation
		HashMap<String, ArrayList<String>> edges = (HashMap<String, ArrayList<String>>) mapData.get("Edges");
		//System.out.println(edges);
		for(String edgeID : edges.keySet()) {
			addEdge(edges.get(edgeID).get(0), edges.get(edgeID).get(1)); // addEge checks himself if the edge already exist
		}
		
		System.out.println("MAP MERGED");
	
	}
	
	
	/**
	 * Retourne le noeud ayant la plus grande valeur (visite la plus ancienne)
	 * @param nodeStart noeud ou se trouve l'agent
	 */
	/**
	public List<String> bestReward2(String nodeStart) {
		List<String> bestNode = new ArrayList<String>();
		bestNode.add(nodeStart);
		long bestValue = System.currentTimeMillis();
		Iterator<Node> iter=this.g.iterator();
		//cherche le/les noeuds à la plus haute valeur
		while(iter.hasNext()){
			Node n=iter.next();
			//System.out.println("bestNode : " + bestNode);
			//System.out.println("bestValue : " + bestValue + ", valueNode : " + Long.parseLong(n.getAttribute("lastVisited").toString()));
			if (bestValue > Long.parseLong(n.getAttribute("lastVisited").toString())) {
				//System.out.println("ok nouvelle valeur valeur pour bestValue");
				bestValue = Long.parseLong(n.getAttribute("lastVisited").toString());
				bestNode = new ArrayList<String>();
				bestNode.add(n.getId());
			}
			else if (bestValue == Long.parseLong(n.getAttribute("lastVisited").toString())) {
				//System.out.println("valeur egale pour bestValue");
				bestNode.add(n.getId());
			}
		}
		List<String> bestPath = this.getShortestPath(nodeStart,bestNode.get(0));
		//System.out.println("bestPath avant comparaison : " + bestPath);
		for(int i = 1; i<bestNode.size(); i++) {
			List<String> pathTemp = this.getShortestPath(nodeStart,bestNode.get(i));
			//System.out.println("pathTemp : " + pathTemp);
			if(pathTemp.size() < bestPath.size()) {
				bestPath = pathTemp;
			}
			else if(pathTemp.size() == bestPath.size()) {
				long valuePathTemp = 0;
				long valueBestPath = 0;
				for (int j=0; j<pathTemp.size(); j++) {
					valuePathTemp = valuePathTemp + Long.parseLong(this.g.getNode(pathTemp.get(j)).getAttribute("lastVisited").toString());
				}
				for (int j=0; j<bestPath.size(); j++) {
					valueBestPath = valueBestPath + Long.parseLong(this.g.getNode(pathTemp.get(j)).getAttribute("lastVisited").toString());
				}
				if(valueBestPath < valuePathTemp) {
					bestPath = pathTemp;
				}
			}
		}
		//System.out.println("bestPath avant envoie : " + bestPath);
		return bestPath;
	}
	**/
	
	/**
	 * Determine le noeud voisin ayant la plus basse valeur (la date de visite la plus vielle)
	 * @param obs list contenant l'observation de l'agent
	 * @return l'id du noeud ayant la plus basse valeur
	 */
	public String bestReward(List<Couple<String,List<Couple<Observation,Integer>>>> obs) {
		long bestValue = System.currentTimeMillis();
		String bestNode = obs.get(0).getLeft();
		for(int i = 1; i<obs.size(); i++) {
			//System.out.println(Long.parseLong(this.g.getNode(obs.get(i).getLeft()).getAttribute("lastVisited").toString()));
			if(bestValue > Long.parseLong(this.g.getNode(obs.get(i).getLeft()).getAttribute("lastVisited").toString())) {
				//System.out.println("ok valide meilleur : " + Long.parseLong(this.g.getNode(obs.get(i).getLeft()).getAttribute("lastVisited").toString()));
				bestValue = Long.parseLong(this.g.getNode(obs.get(i).getLeft()).getAttribute("lastVisited").toString());
				bestNode = obs.get(i).getLeft();
			}
		}
		return bestNode;
	}
	
	public void addOtherAgentPosition(String Id_agent, String ID_node){
		//if node is not exit
		if (this.g.getNode(ID_node)==null){
			this.addNode(ID_node,MapAttribute.open, System.currentTimeMillis(), Id_agent, false, 0.0);
		}
		else {
			//delete agent present in other node
			Iterator<Node> iter=this.g.iterator();
			HashMap<String, ArrayList<String>> nodeList = new HashMap<String, ArrayList<String>>();
			while(iter.hasNext()){
				Node n=iter.next();
				if (n.getAttribute("agent_present").equals(Id_agent)) {
					n.setAttribute("agent_present", -1);
				}
			}
			// Update agent present
			this.g.getNode(ID_node).setAttribute("agent_present", Id_agent);
		}
	}
	
	public void sentGolem(String ID_node, String myPosition) {
		//liste des noeuds autour du noeud où l'on sent
		List<String> node_arround = this.neighborNode(ID_node);
		for(int i = 0 ; i < node_arround.size(); ) {
			//si le noeud contient un agent, est égal à myPosition
			if (node_arround.get(i).equals(myPosition) || !this.g.getNode(node_arround.get(i)).getAttribute("agent_present").equals("-1")) {
				System.out.println("ici");
				node_arround.remove(i);
			}
			else {
				i++;
			}
		}
		//si il reste des voisins après le tri
		if(node_arround.size() > 0) {
			double pourcent = 100/node_arround.size();
	
			//on met à jour les probas de trouver un golem sur les noeuds alentour
			for(String n : node_arround) {
				if(!n.equals(myPosition) && !n.equals(ID_node)){
					this.g.getNode(n).setAttribute("proba_golem_present", (Double) this.g.getNode(n).getAttribute("proba_golem_present") + pourcent);
				}
			}		
		}
	}
	
	public void setGolemDetection(String ID_node, boolean sent, String myPosition) {
		this.g.getNode(ID_node).setAttribute("golem_scent", sent);
		if(!sent) {
			//si je ne sent rien sur ma position
			if(ID_node.contentEquals(myPosition)) {
				this.g.getNode(ID_node).setAttribute("proba_golem_present", 0.0);
			}
		}
		else {
			//si la senteur est sur ma position pas possible qu'il y ai le golem
			if(ID_node.contentEquals(myPosition)) {
				this.g.getNode(ID_node).setAttribute("proba_golem_present", 0.0);
			}
			//sinon calcule de proba
			this.sentGolem(ID_node, myPosition);
		}
	}
	
	/**
	 * Function return a neighbour's list of specific node
	 * @param node : (String) id node
	 * @return List <String> neighbor of node (param)
	 */
	public List <String> neighborNode(String node){
		List <String> neighbor = new ArrayList<String>();
		Iterator<Edge> iterE=this.g.edges().iterator();
		while (iterE.hasNext()){
			Edge e=iterE.next();
			if (e.getSourceNode().getId().equals(node)) {
				neighbor.add(e.getTargetNode().getId());
			}
			else if (e.getTargetNode().getId().equals(node)) {
				neighbor.add(e.getSourceNode().getId());
			}
		}		
		
		return neighbor;
	}
	
	/**
	 * This function reset proba_golem_present attribute at 0.0 for all nodes
	 */
	public void resetPourcentGolem() {
		Iterator<Node> iter=this.g.iterator();
		while(iter.hasNext()){
			Node n=iter.next();
			n.setAttribute("proba_golem_present", 0.0);
		}
	}
	
	public void setNodeSmell(List <String> node_sent) {
		this.nodeSmellList = node_sent;
	}
	
	public List <String> getNodeSmell(){
		return this.nodeSmellList;
	}
	
}