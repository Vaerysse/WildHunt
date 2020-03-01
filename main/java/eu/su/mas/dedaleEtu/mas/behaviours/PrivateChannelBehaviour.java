package eu.su.mas.dedaleEtu.mas.behaviours;

import java.io.IOException;

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
	
	private boolean finished;
	
	/**
	 * To control the incoming communication flow
	 */
	private boolean connection, exchange, ACKmap;
	
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
		this.exchange = false;
		this.ACKmap = false;
		this.stepProtocol = 1;
		//ajouter un timer pour éviter les attentes à l'infini
		
	}
	
	public void action() {
		
		// Sending messages
		if (this.stepProtocol == 1){
			// Sending a message to ask for the opening of a private channel
			ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
			msg.setSender(this.myAgent.getAID());
			msg.setProtocol("ExchangeProtocol");
			msg.setContent("connection");
			msg.addReceiver(new AID(this.receiverName,AID.ISLOCALNAME));
			((AbstractDedaleAgent)this.myAgent).sendMessage(msg);
			System.out.println(this.myAgent.getLocalName() + ": I want to open a private channel with " + this.receiverName);
		}
		else if (this.stepProtocol == 2) {
			// Sending the map representation
			ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
			msg.setSender(this.myAgent.getAID());
			msg.setProtocol("ExchangeProtocol");
			msg.addReceiver(new AID(this.receiverName,AID.ISLOCALNAME));
			try {
				System.out.println("Préparation de l'envoi de la map");
				//((ExploreSoloAgent)this.myAgent).getMap().prepareMigration();
				this.myMap.prepareMigration();
				msg.setContentObject(this.myMap);
			} catch (IOException e) {
				msg.setContent("-1");
				System.out.println("Map serialization problem");
				e.printStackTrace();
			}
			((AbstractDedaleAgent)this.myAgent).sendMessage(msg);
			System.out.println(this.myAgent.getLocalName() + ": I've sent my map to " + this.receiverName);
		}
		else if (this.stepProtocol == 3) {
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
			System.out.println("En attente de récéption message de demande de com priver");
			if (msgReceived != null) {
				System.out.println(this.myAgent.getLocalName() + ": I've received a request for a private channel from " + this.receiverName);
				if (msgReceived.getContent().equals("connection")) {
					this.connection = false;
					this.exchange = true;
					this.stepProtocol += 1;
					System.out.println(this.myAgent.getLocalName() + ": I've opened a private channel with " + this.receiverName);
				}
				else if (msgReceived.getContent() == "-1" ) {
					System.out.println(this.myAgent.getLocalName() + ": I have a connection problem with " + this.receiverName);
					this.finished = true;
				}
			}
		}
		else if (this.exchange) {
			// Receiving a map representation
			MessageTemplate msgTemplate = MessageTemplate.and(MessageTemplate.and(
										  MessageTemplate.MatchPerformative(ACLMessage.INFORM), 
										  MessageTemplate.MatchSender(new AID(this.receiverName, AID.ISLOCALNAME))),
										  MessageTemplate.MatchProtocol("ExchangeProtocol"));			

			ACLMessage msgReceived = this.myAgent.receive(msgTemplate);
			System.out.println(this.myAgent.getLocalName() + ": I've received a map from " + this.receiverName);
			this.exchange = false;
			this.stepProtocol += 1;
			this.ACKmap = true;
			System.out.println(((ExploreSoloAgent)this.myAgent).getMap());
			if (msgReceived != null) {
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
						
						otherMap.setSG((SerializableSimpleGraph<String, MapAttribute>) msgReceived.getContentObject());
						System.out.println("2");
						otherMap.loadSavedData();
						System.out.println("3");
						this.myMap.merge(otherMap);
						System.out.println("4");
						
						
					} catch (UnreadableException e) {
						// TODO Auto-generated catch block
						System.out.println("Map reception problem");
						this.finished = true;
						e.printStackTrace();
					}
				
				}
			}
		}
		else if (this.ACKmap && this.stepProtocol == 3) {
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
		}
		
	}
	
	public boolean done() {
		return finished;
	}
}
