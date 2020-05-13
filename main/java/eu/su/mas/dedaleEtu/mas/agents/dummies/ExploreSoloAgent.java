package eu.su.mas.dedaleEtu.mas.agents.dummies;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import eu.su.mas.dedale.mas.AbstractDedaleAgent;
import eu.su.mas.dedale.mas.agent.behaviours.startMyBehaviours;
import eu.su.mas.dedaleEtu.mas.behaviours.CoalitionBehaviour;
import eu.su.mas.dedaleEtu.mas.behaviours.ExploSoloBehaviour;
import eu.su.mas.dedaleEtu.mas.behaviours.ReceiveMessageBehaviour;
import eu.su.mas.dedaleEtu.mas.behaviours.ReceiveMessageSayGolemBehaviour;
import eu.su.mas.dedaleEtu.mas.behaviours.SayGolem;
import eu.su.mas.dedaleEtu.mas.behaviours.SayHello;
import eu.su.mas.dedaleEtu.mas.knowledge.MapRepresentation;
import jade.core.behaviours.Behaviour;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;

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
	private static final int max_rand = 999999999;
	private MapRepresentation myMap;
	private boolean moving = true;
	private boolean inPursuit = false; // true if the agent is in pursuit of a golem
	private List<String> blackListMap = new ArrayList<String>();
	private List<String> agentZoneList = new ArrayList<String>();
	private List<String> agentPositionList = new ArrayList<String>();
	private long startDate = System.currentTimeMillis();
	private boolean needObj = true;
	private String lastVisitedNode = "";
	private boolean inCoalition = false;
	private boolean inCoalitionFull = false;
	private boolean waitEnterCoalition = false;
	private boolean leaderCoalition = false; // TODO: est-ce qu'on a vraiment besoin de garder ça maintenant qu'on a le leader directement ac leaderAID
	private String coalitionId;
	private String leaderAID = ""; // identifiant du leader
	private int coalitionSize = 1; // taille de la coalition 
	private boolean SayGolemOK;

	/**
	 * This method is automatically called when "agent".start() is executed.
	 * Consider that Agent is launched for the first time. 
	 * 			1) set the agent attributes 
	 *	 		2) add the behaviours
	 *          
	 */
	protected void setup() {

		super.setup();

		// Enregistrement de l'agent dans les pages jaunes
		DFAgentDescription dfd = new DFAgentDescription();
		dfd.setName(getAID()); 
		ServiceDescription sd = new ServiceDescription();
		sd.setType("agent");
		sd.setName(getLocalName());
		dfd.addServices(sd);
		this.inCoalitionFull = false;
		this.SayGolemOK = true;
		try {  
			DFService.register(this, dfd);  
		}
		catch (FIPAException fe) {
			fe.printStackTrace(); 
		}

		List<Behaviour> lb=new ArrayList<Behaviour>();

		/************************************************
		 * 
		 * ADD the behaviours of the Dummy Moving Agent
		 * 
		 ************************************************/

		lb.add(new ExploSoloBehaviour(this,this.myMap));
		lb.add(new ReceiveMessageBehaviour(this));
		lb.add(new SayHello(this));
		lb.add(new ReceiveMessageSayGolemBehaviour(this));

		/***

		 * MANDATORY TO ALLOW YOUR AGENT TO BE DEPLOYED CORRECTLY
		 */


		addBehaviour(new startMyBehaviours(this,lb));

		System.out.println("the agent "+this.getLocalName()+ " is started");

	}
	
	public boolean isMoving() {
		return this.moving;
	}
	
	public void setMoving(boolean value) {
		this.moving = value;
	}
	
	public boolean isInPursuit() {
		return this.inPursuit;
	}
	
	public void setInPursuit(boolean value) {
		this.inPursuit = value;
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
		return this.myMap;
	}
	
	public void setMap(MapRepresentation map) {
		this.myMap = map;
	}
	
	public long getStartDate() {
		return startDate;
	}
	
	public boolean getNeedObj() {
		return this.needObj;
	}
	
	public void setNeedObj(boolean bool) {
		this.needObj = bool;
	}
	
	public void cleanAgentPositionList() {
		this.agentPositionList = new ArrayList<String>();
	}
	
	public List<String> getAgentPositionList(){
		return this.agentPositionList;
	}
	
	public void addAgentPosittionList(String pos) {
		this.agentPositionList.add(pos);
	}
	
	public void setLastVisitedNode(String ID_node) {
		this.lastVisitedNode = ID_node;
	}
	
	public String getLastVisitedNode() {
		return this.lastVisitedNode;
	}
	
	public boolean getInCoalition() {
		return this.inCoalition;
	}
	
	public void setInCoalition(boolean value) {
		this.inCoalition = value;
	}
	
	public boolean getInCoalitionFull() {
		return this.inCoalitionFull;
	}
	
	public void setInCoalitionFull(boolean value) {
		this.inCoalitionFull = value;
	}
	
	public boolean getLeaderCoalition() {
		return this.leaderCoalition;
	}
	
	public void setLeaderCoalition(boolean value) {
		this.leaderCoalition = value;
	}
		
	public String idBehaviourCreation() {
		Random rand = new Random();
		this.coalitionId = "C" + rand.nextInt(max_rand); //id creation
		return this.coalitionId;
	}
	
	public void setIDCoalition(String idcoal) {
		this.coalitionId = idcoal;
	}
	
	public String getIDCoalition() {
		return this.coalitionId;
	}
	
	public void setLeaderAID(String leaderAID) {
		this.leaderAID = leaderAID;
	}
	
	public String getLeaderAID() {
		return this.leaderAID;
	}
	
	public void setCoalitionSize(int nbAgents) {
		this.coalitionSize = nbAgents;
	}
	
	public int getCoalitionSize() {
		return this.coalitionSize;
	}
	
	public void entreNewCoalition(String numcoal) {
		this.setInCoalition(true);
		this.setIDCoalition(numcoal);
		this.setInPursuit(true);
		this.setWaitEnterCoalition(false);
	}
	
	public void setWaitEnterCoalition(boolean value) {
		this.waitEnterCoalition = value;
	}
	
	public boolean getWaitEnterCoalition(){
		return this.waitEnterCoalition;
	}
	
	public boolean getSayGolemOK(){
		return this.SayGolemOK;
	}
	
	public void setSayGolemOK(boolean value){
		this.SayGolemOK =  value;
	}
}
