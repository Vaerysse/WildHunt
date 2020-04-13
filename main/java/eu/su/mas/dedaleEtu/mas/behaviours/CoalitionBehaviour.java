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

public class CoalitionBehaviour extends SimpleBehaviour{
	
	private static final long serialVersionUID = 2L;
	private boolean finished;
	
	private static final int wait = 2000;
	
	
	/**
	 * To control the incoming communication flow
	 */
	private boolean connection, exchange, ACKmap, sendProposal, sendAnswer, sendConfirmation; // sendConnection, sendMap, sendACKmap
	
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
	//private MapRepresentation myMap;
	
	
	private long timer;
	
	public CoalitionBehaviour(final Agent myagent, String receiverName) {
		super(myagent);
		
		//this.myMap = ((ExploreSoloAgent)this.myAgent).getMap();
		this.receiverName = receiverName;
		this.finished = false;
		this.connection = true;
		this.sendProposal = true;
		this.exchange = false;
		this.sendAnswer = false;
		this.ACKmap = false;
		this.sendConfirmation = false;
		this.stepProtocol = 1;
		this.timer = System.currentTimeMillis();
	}
	
	@Override
	public void action() {
		
		/** TODO pour moi faut déjà différencier 2 cas, je fait déjà partie d'une coalition donc j'ignore le message
		*je ne fait pas partie d'une coalition
		*pui ensuite, je sent aussi un golem ou bien je ne sent pas
		*dans les deux cas je demande a rentrer en présisent si oui ou non je sen un golem (plus de chance d'être pris si jamais plusieurs candidat)
		*/
		
		
		// Sending messages
		if (this.sendProposal){
			// Sending a message to propose a coalition
			ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
			msg.setSender(this.myAgent.getAID());
			msg.setProtocol("CoalitionProtocol");
			msg.setContent("coalition proposal");
			msg.addReceiver(new AID(this.receiverName, AID.ISLOCALNAME));
			((AbstractDedaleAgent)this.myAgent).sendMessage(msg);
			System.out.println(this.myAgent.getLocalName() + ": I want to create a coalition with " + this.receiverName);
		}
		else if (this.sendAnswer) {
			
			// TODO: déterminer si intéressant d'entrer ds la coalition ou pas
			String answer = new String(); // acceptation or refusal
			
			// Sending an answer to the coalition proposal
			ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
			msg.setSender(this.myAgent.getAID());
			msg.setProtocol("CoalitionProtocol");
			
			// TODO: déterminer si on répond à tous les agents de la proposition de coalition
			msg.addReceiver(new AID(this.receiverName, AID.ISLOCALNAME));
			
			msg.setContent(answer);
			((AbstractDedaleAgent)this.myAgent).sendMessage(msg);
			System.out.println(this.myAgent.getLocalName() + ": I've sent my answer ("+ answer +") to " + this.receiverName);
		}
		else if (this.sendConfirmation) {
			
			// TODO: déterminer si on confirme la coalition ou non
			String answer = new String();
			// on met la confirmation à ce niveau là ?
			
			// Confirming the coalition creation
			ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
			msg.setSender(this.myAgent.getAID());
			msg.setProtocol("CoalitionProtocol");
			msg.setContent(answer);
			
			// TODO: idem, transmettre au chef ou à tous les agents ?
			msg.addReceiver(new AID(this.receiverName,AID.ISLOCALNAME));
			((AbstractDedaleAgent)this.myAgent).sendMessage(msg);
			System.out.println(this.myAgent.getLocalName() + ": I've sent a map ACK to " + this.receiverName);
		}
		
		
		
	}
	
	
	public boolean done() {
		return finished;
	}
	
}
