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
	private String id_Coal;
	
	private HashMap<String, String> members = new HashMap<String, String>();
	private int maxAgent;
	
	
	private long timer;
	
	public CoalitionBehaviour(final Agent myagent, String id) {
		super(myagent);

		this.id_Coal = id;
		this.members.put(this.myAgent.getLocalName(),"Leader");
		//TODO definir nombre agent max dans coalition
		this.maxAgent = 4;
		
	}
	
	@Override
	public void action() {
		
		System.out.println("Behaviour de coalition " + this.id_Coal + " actif");
		
		final MessageTemplate msgTemplate = MessageTemplate.and(MessageTemplate.MatchPerformative(ACLMessage.INFORM), 
				MessageTemplate.MatchProtocol(this.id_Coal));			

		final ACLMessage msg = this.myAgent.receive(msgTemplate);
		
		if (msg != null){
			if (msg.getContent().equals("RequestEntry?")) {
				this.addAgentCoalition(msg);
			}
		}
		
	}
	
	public void addAgentCoalition(ACLMessage msg) {
		if(this.members.size() < this.maxAgent) {
			System.out.println("envoi message coalition ok");
			ACLMessage msgRespond=new ACLMessage(ACLMessage.INFORM);
			msgRespond.setSender(this.myAgent.getAID());
			msgRespond.setProtocol("AnswerEntry");
			msgRespond.setContent("ok");
			msgRespond.addReceiver(new AID(msg.getSender().getLocalName(),AID.ISLOCALNAME));
			this.members.put(msg.getSender().getLocalName(),"Other");
		}
		else {
			System.out.println("envoi message coalition non");
			ACLMessage msgRespond=new ACLMessage(ACLMessage.INFORM);
			msgRespond.setSender(this.myAgent.getAID());
			msgRespond.setProtocol("AnswerEntry");
			msgRespond.setContent("no");
			msgRespond.addReceiver(new AID(msg.getSender().getLocalName(),AID.ISLOCALNAME));
		}
	}
	
	
	public boolean done() {
		return finished;
	}
	
}
