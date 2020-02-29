package eu.su.mas.dedaleEtu.mas.agents.dummies;

import java.util.ArrayList;
import java.util.List;

import eu.su.mas.dedale.mas.AbstractDedaleAgent;
import eu.su.mas.dedale.mas.agent.behaviours.startMyBehaviours;
import eu.su.mas.dedaleEtu.mas.behaviours.ExploSoloBehaviour;
import eu.su.mas.dedaleEtu.mas.behaviours.PrivateChannelBehaviour;
import eu.su.mas.dedaleEtu.mas.behaviours.ReceiveMessageBehaviour;
import eu.su.mas.dedaleEtu.mas.behaviours.SayHello;
import eu.su.mas.dedaleEtu.mas.knowledge.MapRepresentation;
import eu.su.mas.dedaleEtu.mas.agents.dummies.ExploreSoloAgent;
import jade.core.behaviours.Behaviour;

/**
 * <pre>
 * ExploreSolo agent. 
 * It explore the map using a DFS algorithm.
 * It stops when all nodes have been visited.
 *  </pre>
 *  
 * @author hc
 *
 */

public class ExploreSoloAgent extends AbstractDedaleAgent {

	private static final long serialVersionUID = -6431752665590433727L;
	private MapRepresentation myMap;
	private boolean moving = true;
	private List<String> blackList = new ArrayList<String>();
	private List<String> agentZoneList = new ArrayList<String>();
	

	/**
	 * This method is automatically called when "agent".start() is executed.
	 * Consider that Agent is launched for the first time. 
	 * 			1) set the agent attributes 
	 *	 		2) add the behaviours
	 *          
	 */
	protected void setup(){

		super.setup();
		
		//this.myMap = new MapRepresentation();

		List<Behaviour> lb=new ArrayList<Behaviour>();
		
		/************************************************
		 * 
		 * ADD the behaviours of the Dummy Moving Agent
		 * 
		 ************************************************/
		
		lb.add(new ExploSoloBehaviour(this,this.myMap));
		lb.add(new ReceiveMessageBehaviour(this));
		lb.add(new SayHello(this));
		
		
		/***
		 * MANDATORY TO ALLOW YOUR AGENT TO BE DEPLOYED CORRECTLY
		 */
		
		
		addBehaviour(new startMyBehaviours(this,lb));
		
		System.out.println("the  agent "+this.getLocalName()+ " is started");

	}

	public boolean isMoving() {
		return this.moving;
	}
	
	public void setMoving(boolean value) {
		this.moving = value;
	}
	
	public void addAgentBlackList(String agent) {
		this.blackList.add(agent);
	}
	
	public void supAgentBlackList(String agent) {
		this.blackList.remove(agent);
	}
	
	public List<String> getAgentBlackList(){
		return this.blackList;
	}
	
	public void addAgentZoneList(String agent) {
		this.agentZoneList.add(agent);
	}
	
	public void supAgentZoneList() {
		this.agentZoneList = new ArrayList<String>();
	}
	
	public List<String> getAgentZoneList(){
		return this.agentZoneList;
	}
	
	public MapRepresentation getMap() {
		System.out.println("Map es tu là ? " + this.myMap);
		return this.myMap;
	}
	
	public void setMap(MapRepresentation map) {
		this.myMap = map;
	}
	
}
