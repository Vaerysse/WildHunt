package eu.su.mas.dedaleEtu.mas.behaviours;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.Set;

import org.graphstream.graph.Node;
import org.graphstream.graph.Graph;
import org.graphstream.algorithm.Toolkit;

import dataStructures.tuple.Couple;
import eu.su.mas.dedale.env.Observation;
import eu.su.mas.dedale.mas.AbstractDedaleAgent;
import eu.su.mas.dedaleEtu.mas.agents.dummies.ExploreSoloAgent;
import eu.su.mas.dedaleEtu.mas.knowledge.MapRepresentation;
import eu.su.mas.dedaleEtu.mas.knowledge.MapRepresentation.MapAttribute;
import jade.core.behaviours.Behaviour;
import jade.core.behaviours.SimpleBehaviour;


/**
 * This behaviour allows an agent to explore the environment and learn the associated topological map.
 * The algorithm is a pseudo - DFS computationally consuming because its not optimised at all.</br>
 * 
 * When all the nodes around him are visited, the agent randomly select an open node and go there to restart its dfs.</br> 
 * This (non optimal) behaviour is done until all nodes are explored. </br> 
 * 
 * Warning, this behaviour does not save the content of visited nodes, only the topology.</br> 
 * Warning, this behaviour is a solo exploration and does not take into account the presence of other agents (or well) and indefinitely tries to reach its target node
 * @author hc
 *
 */
public class ExploSoloBehaviour extends SimpleBehaviour {

	private static final long serialVersionUID = 8567689731496787661L;

	private boolean finished = false;

	/**
	 * Current knowledge of the agent regarding the environment
	 */
	private MapRepresentation myMap;

	/**
	 * Nodes known but not yet visited
	 */
	private List<String> openNodes;
	/**
	 * Visited nodes
	 */
	private Set<String> closedNodes;


	public ExploSoloBehaviour(final AbstractDedaleAgent myagent, MapRepresentation myMap) {
		super(myagent);
		this.myMap = myMap;
		this.openNodes = new ArrayList<String>();
		this.closedNodes = new HashSet<String>();
	}

	@Override
	public void action() {

		if (this.myMap == null) {
			//System.out.println("Map null");
			//this.myMap = ((ExploreSoloAgent)this.myAgent).getMap();
			this.myMap = new MapRepresentation();
			((ExploreSoloAgent)this.myAgent).setMap(this.myMap);
		}
		
		//0) Retrieve the current position
		String myPosition = ((AbstractDedaleAgent)this.myAgent).getCurrentPosition();
	
		if (myPosition != null){
			//List of observable from the agent's current position
			List<Couple<String,List<Couple<Observation,Integer>>>> lobs=((AbstractDedaleAgent)this.myAgent).observe();//myPosition
			
			//System.out.println(this.myAgent.getLocalName() + " : " +lobs);
			
			/**
			 * Just added here to let you see what the agent is doing, otherwise he will be too quick
			 */
			try {
				this.myAgent.doWait(500);
			} catch (Exception e) {
				e.printStackTrace();
			}

			//1) remove the current node from openlist and add it to closedNodes.
			this.closedNodes.add(myPosition);
			this.openNodes.remove(myPosition);

			this.myMap.addNode(myPosition, MapAttribute.closed, System.currentTimeMillis(), "-1", false, 0.0); //attention 4éme argument actuellement a false car pas de detection de golme actuel

					
			//2) get the surrounding nodes and, if not in closedNodes, add them to open nodes.
			String nextNode=null;
			Iterator<Couple<String, List<Couple<Observation, Integer>>>> iter=lobs.iterator();
			while(iter.hasNext()){
				String nodeId = iter.next().getLeft();
				if (!this.closedNodes.contains(nodeId)){
					if (!this.openNodes.contains(nodeId)){
						this.openNodes.add(nodeId);
						this.myMap.addNode(nodeId, MapAttribute.open, ((ExploreSoloAgent)this.myAgent).getStartDate(), "-1", false, 0.0);
						this.myMap.addEdge(myPosition, nodeId);
					}else{
						//the node exist, but not necessarily the edge
						this.myMap.addEdge(myPosition, nodeId);
					}
					if (nextNode==null) nextNode=nodeId;
				}
			}

			//3) while openNodes is not empty, continues.
			if (this.openNodes.isEmpty()){
				//Explo finished
				this.myAgent.addBehaviour(new PatrolSoloBehaviour(((ExploreSoloAgent)this.myAgent), this.myMap));
				
				if (myMap.getMaxDegree() == 0 || myMap.getAvDegree() == 0) { // no need to compute graph degree if already done by another agent
					myMap.setMaxDegree(); // compute the max degree in the graph
					myMap.setAvDegree(); // compute the average degree in the graph
				}
				finished = true;
				System.out.println("Exploration successufully done, behaviour removed.");
			}else{
				//4) select next move.
				//4.1 If there exist one open node directly reachable, go for it,
				//	 otherwise choose one from the openNode list, compute the shortestPath and go for it
				if (nextNode==null){
					//no directly accessible openNode
					//chose one, compute the path and take the first step.
					nextNode=this.myMap.getShortestPath(myPosition, this.openNodes.get(0)).get(0);
				}
				
				
				
				/***************************************************
				** 		ADDING the API CALL to illustrate their use **
				*****************************************************/

				//list of observations associated to the currentPosition
				List<Couple<Observation,Integer>> lObservations= lobs.get(0).getRight();
				//System.out.println(this.myAgent.getLocalName()+" - State of the observations : "+lobs);
				
				//example related to the use of the backpack for the treasure hunt
				Boolean b=false;
				for(Couple<Observation,Integer> o:lObservations){
					switch (o.getLeft()) {
					case DIAMOND:case GOLD:

						System.out.println(this.myAgent.getLocalName()+" - My treasure type is : "+((AbstractDedaleAgent) this.myAgent).getMyTreasureType());
						System.out.println(this.myAgent.getLocalName()+" - My current backpack capacity is:"+ ((AbstractDedaleAgent) this.myAgent).getBackPackFreeSpace());
						System.out.println(this.myAgent.getLocalName()+" - My expertise is: "+((AbstractDedaleAgent) this.myAgent).getMyExpertise());
						System.out.println(this.myAgent.getLocalName()+" - I try to open the safe: "+((AbstractDedaleAgent) this.myAgent).openLock(Observation.GOLD));
						System.out.println(this.myAgent.getLocalName()+" - Value of the treasure on the current position: "+o.getLeft() +": "+ o.getRight());
						System.out.println(this.myAgent.getLocalName()+" - The agent grabbed : "+((AbstractDedaleAgent) this.myAgent).pick());
						System.out.println(this.myAgent.getLocalName()+" - the remaining backpack capacity is: "+ ((AbstractDedaleAgent) this.myAgent).getBackPackFreeSpace());
						b=true;
						break;
					default:
						break;
					}
				}

				//If the agent picked (part of) the treasure
				if (b){
					List<Couple<String,List<Couple<Observation,Integer>>>> lobs2=((AbstractDedaleAgent)this.myAgent).observe();//myPosition
					System.out.println(this.myAgent.getLocalName()+" - State of the observations after picking "+lobs2);
					
					//Trying to store everything in the tanker
					System.out.println(this.myAgent.getLocalName()+" - My current backpack capacity is:"+ ((AbstractDedaleAgent)this.myAgent).getBackPackFreeSpace());
					System.out.println(this.myAgent.getLocalName()+" - The agent tries to transfer is load into the Silo (if reachable); succes ? : "+((AbstractDedaleAgent)this.myAgent).emptyMyBackPack("Silo"));
					System.out.println(this.myAgent.getLocalName()+" - My current backpack capacity is:"+ ((AbstractDedaleAgent)this.myAgent).getBackPackFreeSpace());
					
				}
				
				//Trying to store everything in the tanker
				//System.out.println(this.myAgent.getLocalName()+" - My current backpack capacity is:"+ ((AbstractDedaleAgent)this.myAgent).getBackPackFreeSpace());
				//System.out.println(this.myAgent.getLocalName()+" - The agent tries to transfer is load into the Silo (if reachable); succes ? : "+((AbstractDedaleAgent)this.myAgent).emptyMyBackPack("Silo"));
				//System.out.println(this.myAgent.getLocalName()+" - My current backpack capacity is:"+ ((AbstractDedaleAgent)this.myAgent).getBackPackFreeSpace());


				/************************************************
				 * 				END API CALL ILUSTRATION
				 *************************************************/
				//is agent move is true
				if (((ExploreSoloAgent) this.myAgent).isMoving()){
					//on enregistre le dernier noeud où on été avant dde bouger
					((ExploreSoloAgent)this.myAgent).setLastVisitedNode(myPosition);
					((AbstractDedaleAgent)this.myAgent).moveTo(nextNode);//agent move 1 node
					//si agent bloqué
					if(myPosition.equals(((AbstractDedaleAgent)this.myAgent).getCurrentPosition())) {
						Random rand = new Random();
						int nb = rand.nextInt(lobs.size());
						//System.out.println(this.myAgent.getLocalName() + " moveTo : " + lobs.get(nb).getLeft());						
						((AbstractDedaleAgent)this.myAgent).moveTo(lobs.get(nb).getLeft().toString());
					}
					/**
					//Delete from the blackList the agent not present into the ray of communication on the previous node
					for(int a = 0; a < ((ExploreSoloAgent)this.myAgent).getAgentBlackList().size(); ) {
						if (!((ExploreSoloAgent)this.myAgent).getAgentZoneList().contains(((ExploreSoloAgent)this.myAgent).getAgentBlackList().get(a)) && !((ExploreSoloAgent)this.myAgent).getAgentBlackList().get(a).equals(this.myAgent.getLocalName())){
							((ExploreSoloAgent)this.myAgent).supAgentBlackList(((ExploreSoloAgent)this.myAgent).getAgentBlackList().get(a));
						}
						else {
							a++;
						}
					}
					((ExploreSoloAgent)this.myAgent).supAgentZoneList();
					**/
					
				}
				else {
					//System.out.println("Ho là là, je suis si fatigué!");
				}
			}

		}
	}

	@Override
	public boolean done() {
		return finished;
	}

}
