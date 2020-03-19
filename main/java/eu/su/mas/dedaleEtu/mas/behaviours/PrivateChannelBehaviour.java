package eu.su.mas.dedaleEtu.mas.behaviours;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import dataStructures.serializableGraph.SerializableSimpleGraph;
import eu.su.mas.dedale.mas.AbstractDedaleAgent;
import eu.su.mas.dedaleEtu.mas.agents.dummies.ExploreSoloAgent;
import eu.su.mas.dedaleEtu.mas.knowledge.MapRepresentation;
import eu.su.mas.dedaleEtu.mas.knowledge.MapRepresentation.MapAttribute;
import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.SimpleBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.lang.acl.UnreadableException;

public class PrivateChannelBehaviour extends SimpleBehaviour{

	private static final long serialVersionUID = 9088209402507795292L;
	private static final int wait = 2000;
	
	private boolean finished;
	
	/**
	 * To control the incoming communication flow
	 */
	private boolean connection, exchange, ACKmap, sendConnection, sendMap, sendACKmap;
	
	/**
	 * To control the outgoing communication flow
	 */
	private int stepProtocol;
	
	/**
	 * Name of the agent to communicate with
	 */
	private String receiverName;
	
	/**
	 * Current knowledge of the agent regarding the environment
	 */
	private MapRepresentation myMap;
	
	
	private long timer;
	
	/**
	 * 
	 * Private communication channel to exchange information between 2 agents.
	 * @param myagent
	 * @param receiverName The local name of the agent myagent tries to communicate with
	 */
	public PrivateChannelBehaviour(final Agent myagent, String receiverName) {
		super(myagent);
		
		this.myMap = ((ExploreSoloAgent)this.myAgent).getMap();
		this.receiverName = receiverName;
		this.finished = false;
		this.connection = true;
		this.sendConnection = true;
		this.exchange = false;
		this.sendMap = false;
		this.ACKmap = false;
		this.sendACKmap = false;
		this.stepProtocol = 1;
		this.timer = System.currentTimeMillis();
		//ajouter un timer pour éviter les attentes à l'infini
		
	}
	
	public void action() {
		
		// Sending messages
		if (this.sendConnection){
			// Sending a message to ask for the opening of a private channel
			ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
			msg.setSender(this.myAgent.getAID());
			msg.setProtocol("ExchangeProtocol");
			msg.setContent("connection");
			msg.addReceiver(new AID(this.receiverName,AID.ISLOCALNAME));
			((AbstractDedaleAgent)this.myAgent).sendMessage(msg);
			System.out.println(this.myAgent.getLocalName() + ": I want to open a private channel with " + this.receiverName);
		}
		else if (this.sendMap) {
			// Sending the map representation
			ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
			msg.setSender(this.myAgent.getAID());
			msg.setProtocol("MapProtocol");
			msg.addReceiver(new AID(this.receiverName,AID.ISLOCALNAME));
			try {
				msg.setContentObject(this.myMap.prepareSendMap());
			} catch (IOException e) {
				msg.setContent("-1");
				System.out.println("Map serialization problem");
				e.printStackTrace();
			}
			((AbstractDedaleAgent)this.myAgent).sendMessage(msg);
			System.out.println(this.myAgent.getLocalName() + ": I've sent my map to " + this.receiverName);
		}
		else if (this.sendACKmap) {
			// Acknowledging the map reception
			ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
			msg.setSender(this.myAgent.getAID());
			msg.setProtocol("ExchangeProtocol");
			msg.setContent("ACKmap");
			msg.addReceiver(new AID(this.receiverName,AID.ISLOCALNAME));
			((AbstractDedaleAgent)this.myAgent).sendMessage(msg);
			System.out.println(this.myAgent.getLocalName() + ": I've sent a map ACK to " + this.receiverName);
		}
			
		// Receiving messages
		if (this.connection) {
			// Receiving a request for the opening of a private channel
			MessageTemplate msgTemplate = MessageTemplate.and(MessageTemplate.and(
										  MessageTemplate.MatchPerformative(ACLMessage.INFORM), 
										  MessageTemplate.MatchSender(new AID(this.receiverName, AID.ISLOCALNAME))),
										  MessageTemplate.MatchProtocol("ExchangeProtocol"));			
			ACLMessage msgReceived = this.myAgent.receive(msgTemplate);
			//System.out.println("En attente de récéption message de demande de com priver");
			if (msgReceived != null) {
				System.out.println(this.myAgent.getLocalName() + ": I've received a request for a private channel from " + this.receiverName);
				if (msgReceived.getContent().equals("connection")) {
					this.connection = false;
					this.exchange = true;
					this.sendConnection = false;
					this.sendMap = true;
					System.out.println(this.myAgent.getLocalName() + ": I've opened a private channel with " + this.receiverName);
				}
				else if (msgReceived.getContent() == "-1" ) {
					System.out.println(this.myAgent.getLocalName() + ": I have a connection problem with " + this.receiverName);
					this.finished = true;
				}
			}
			else {
				if(System.currentTimeMillis() - this.timer >= wait){
					((ExploreSoloAgent)this.myAgent).setMoving(true);
					this.finished = true;
				}
			}
		}
		else if (this.exchange) {
			// Receiving a map representation
			MessageTemplate msgTemplate = MessageTemplate.and(MessageTemplate.and(
										  MessageTemplate.MatchPerformative(ACLMessage.INFORM), 
										  MessageTemplate.MatchSender(new AID(this.receiverName, AID.ISLOCALNAME))),
										  MessageTemplate.MatchProtocol("MapProtocol"));			

			ACLMessage msgReceived = this.myAgent.receive(msgTemplate);
			System.out.println(this.myAgent.getLocalName() + ": I've received a map from " + this.receiverName);
			this.exchange = false;
			this.stepProtocol += 1;
			this.ACKmap = true;
			this.sendACKmap = true;
			this.sendMap = false;
			
			// TODO: ligne debug pr print Map es-tu là ?
			((ExploreSoloAgent)this.myAgent).getMap();
			
			if (msgReceived != null) {
				if (msgReceived.getContent() == "-1" ) {
					System.out.println(this.myAgent.getLocalName() + " - MAP RECEPTION PROBLEM");
					this.finished = true;
				}
				else {
					try {
						this.myMap.mergeMapData((HashMap<String, HashMap<String, ArrayList<String>>>) msgReceived.getContentObject());
					} catch (UnreadableException e) {
						System.out.println(this.myAgent.getLocalName() + " - MAP RECEPTION PROBLEM");
						System.out.println(this.myAgent.getLocalName() + "dans l'ouverture de map");
						e.printStackTrace();
						this.finished = true;
					}
				
				/*
				try {
					System.out.println(msgReceived.getContentObject());
				} catch (UnreadableException e1) {
					// TODO Auto-generated catch block
					System.out.println("pb print obj");
					e1.printStackTrace();
				}
				if (msgReceived.getContent() == "-1" ) {
					System.out.println("Map reception problem");
					this.finished = true;
				}
				else {

					try {
						MapRepresentation otherMap = new MapRepresentation();
						System.out.println("1");					
						otherMap.receptionMap((HashMap) msgReceived.getContentObject());

					} catch (UnreadableException e) {
						System.out.println("Map reception problem");
						this.finished = true;
						e.printStackTrace();
					}
					*/
				
				}
				((ExploreSoloAgent)this.myAgent).setNeedObj(true);
			}
			else {
				if(System.currentTimeMillis() - this.timer >= wait){
					((ExploreSoloAgent)this.myAgent).setMoving(true);
					this.finished = true;
				}
			}
		}
		else if (this.ACKmap && this.sendACKmap) {
			// Receiving an ACK for the map reception
			MessageTemplate msgTemplate = MessageTemplate.and(MessageTemplate.and(
										  MessageTemplate.MatchPerformative(ACLMessage.INFORM), 
										  MessageTemplate.MatchSender(new AID(this.receiverName, AID.ISLOCALNAME))),
										  MessageTemplate.MatchProtocol("ExchangeProtocol"));			

			ACLMessage msgReceived = this.myAgent.receive(msgTemplate);
			if (msgReceived != null) {
				if (msgReceived.getContent().equals("ACKmap") ) {
					System.out.println(this.myAgent.getLocalName() + ": I've received an ACK map from " + this.receiverName);
					this.finished = true;
					((ExploreSoloAgent)this.myAgent).setMoving(true);
				}
				else if (msgReceived.getContent() == "-1" ) {
					System.out.println("ACK map reception problem");
				}
			}
			else {
				if(System.currentTimeMillis() - this.timer >= wait){
					((ExploreSoloAgent)this.myAgent).setMoving(true);
					this.finished = true;
				}
			}
		}
		
	}
	
	public boolean done() {
		return finished;
	}
}
