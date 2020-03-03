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
	public void addNode(String id,MapAttribute mapAttribute){
		Node n;
		if (this.g.getNode(id)==null){
			n=this.g.addNode(id);
		}else{
			n=this.g.getNode(id);
		}
		n.clearAttributes();
		n.setAttribute("ui.class", mapAttribute.toString());
		//System.out.println("map attribut : " + mapAttribute.toString());
		n.setAttribute("ui.label",id);
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
		for(String nodeID : nodes.keySet()) {
			if (this.g.getNode(nodeID) == null) { // node unknown
				addNode(nodeID, MapAttribute.valueOf(nodes.get(nodeID).get(0)));
			}
			else { // node known, just updating the open/closed attribute if necessary
				// TODO: voir comment traiter le cas si MapAttribute = agent (pb de datation de l'info)
				// Et à déplacer potentiellement dans addNode pour éviter un double check d'existence du noeud
				if ((nodes.get(nodeID).get(0).equals("closed")) && 
						(this.g.getNode(nodeID).getAttribute("ui.class").toString().equals("open"))) {
					addNode(nodeID, MapAttribute.valueOf(nodes.get(nodeID).get(0)));
				}
			}
		}
		
		// Adding unknown edges to the MapRepresentation
		HashMap<String, ArrayList<String>> edges = (HashMap<String, ArrayList<String>>) mapData.get("Edges");
		for(String edgeID : edges.keySet()) {
			addEdge(edges.get(edgeID).get(0), edges.get(edgeID).get(1)); // addEge checks himself if the edge already exist
		}
		
		System.out.println("MAP MERGED");
		
		/*
				
		Graph graphToMerge = map.getGraph();
		 
		// Adding unknown nodes to the MapRepresentation
		Iterator<Node> iter = graphToMerge.iterator();
		while(iter.hasNext()) {
			Node n = iter.next();
			if (this.g.getNode(n.getId()) == null) { // node unknown
				this.addNode(n.getId(), (MapAttribute)n.getAttribute("ui.class"));
			}
			else { // node known, just updating the attribute if necessary
				if (!((MapAttribute)n.getAttribute("ui.class")).equals((MapAttribute)this.g.getNode(n.getId()).getAttribute("ui.class"))) { // update of the attribute
					this.addNode(n.getId(), (MapAttribute)n.getAttribute("ui.class"));
				}
			}
		}
		
		// Adding unknown edges to the MapRepresentation
		Iterator<Edge> iterE = graphToMerge.edges().iterator();
		while (iterE.hasNext()) {
			Edge e = iterE.next();
			Node sn = e.getSourceNode();
			Node tn = e.getTargetNode();
			if (this.g.getEdge(e.getId()) == null) { // unknown edge
				this.g.addEdge(e.getId(), sn.getId(), tn.getId());
			}
		}*/
	}
	
	/*
	public void  receptionMap(HashMap serialMap) {
		HashMap<String, ArrayList<String>> nodes = (HashMap<String, ArrayList<String>>) serialMap.get("Nodes");
		//parcourir hashmap node et ajouter chaque node au graphe g
		Iterator iterN = nodes.keySet().iterator();
		for(String mapkey : nodes.keySet()) {
			addNode(mapkey,MapAttribute.valueOf(nodes.get(mapkey).get(0)));
		}
		
		HashMap<String, ArrayList<String>> edges = (HashMap<String, ArrayList<String>>) serialMap.get("Edges");
		// parcourir hashmap edge et ajouter chaque edge au graphe g
		Iterator iterE = edges.keySet().iterator();
		for(String mapkey : edges.keySet()) {
			null;
		}
		
	}
	*/
	
}