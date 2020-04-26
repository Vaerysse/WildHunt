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
	private boolean candidatAgentOpen;
	private String golemLocalitation;
	
	
	private long timer;
	
	public CoalitionBehaviour(final Agent myagent, String id) {
		super(myagent);

		this.id_Coal = id;
		this.members.put(this.myAgent.getLocalName(),"Leader");
		this.candidatAgentOpen = true;
		
		// Nombre agent max dans coalition
		this.maxAgent = ((ExploreSoloAgent)this.myAgent).getMap().getMaxDegree(); // degré max du graphe
		//this.maxAgent = ((ExploreSoloAgent)this.myAgent).getMap().getAvDegree(); // degré moyen du graphe
		
	}
	
	@Override
	public void action() {
		
		System.out.println(this.myAgent.getLocalName() + " : Behaviour de coalition " + this.id_Coal + " actif");
		
		final MessageTemplate msgTemplate = MessageTemplate.and(MessageTemplate.MatchPerformative(ACLMessage.INFORM), 
				MessageTemplate.MatchProtocol(this.id_Coal));			

		final ACLMessage msg = this.myAgent.receive(msgTemplate);
		
		final MessageTemplate msgTemplateGolemPosition = MessageTemplate.and(MessageTemplate.MatchPerformative(ACLMessage.INFORM), 
				MessageTemplate.MatchProtocol(this.id_Coal + ": golem position"));			

		final ACLMessage msgGolemPosition = this.myAgent.receive(msgTemplateGolemPosition);		
		
		final MessageTemplate msgTemplatenbAgent = MessageTemplate.and(MessageTemplate.MatchPerformative(ACLMessage.INFORM), 
				MessageTemplate.MatchProtocol(this.id_Coal + ": nb Agent"));			

		final ACLMessage msgnbAgent = this.myAgent.receive(msgTemplatenbAgent);	
		
		
		// partie réservé au leader
		if (((ExploreSoloAgent)this.myAgent).getLeaderCoalition()){
			
			//de nouveau agent peuvent encore venir dans la coalition
			if(this.candidatAgentOpen) {
				if(msg != null) {
					//reception d'une demande d'entré dans la coalition
					if (msg.getContent().equals("RequestEntry?")) {
						this.addAgentCoalition(msg);
					}
				}
				
				//reception d'une demande de localisation de golem
				if (msgGolemPosition != null) {
					this.golemPosition(msg);
				}
			
				//reception d'une demande de nombre d'agent
				if (msgnbAgent != null) {
					this.nbAgentCoalition(msg);
				}
			
				//si la coalition est pleine
				if (this.members.size() >= this.maxAgent) {
					//TODO
					//stop message sayGolem (envoi à tous les agent aussi)
					//lancer calcule pour choper le golem (calcule fait dans l'agent leader) (envoie de la possition à chaque agent)
				}
			}
			else {
				//TODO
			}
		}	
		//TODO
		//je reçoit un message du leader a transmetre à mes fils (dans la coalition)
		//je reçoit un message du leader qui me donne un position pour choper le golem (et donner leur position à mes fils)
		//je recoit un message de mon fils de coalition
		//je recoit un message une demande de rentré dans la coalition (a transmetre à mon leader/pére)			
		//je recoit de mon fils les données ou il seent le golem (transmetre au leader/pére)
		//je recoit de mon fils qu'il est arriver en position X (transmetre au leader/pére)
		//je transmet au leader/pére là ou je sent le golem
		//je transmet ma position au leader/pére
	}
	
	//ajout d'un agent dans la coalition
	private  void addAgentCoalition(ACLMessage msg) {
		ACLMessage msgRespond = new ACLMessage(ACLMessage.INFORM);
		msgRespond.setSender(this.myAgent.getAID());
		msgRespond.setProtocol("AnswerEntry");
		if(this.members.size() < this.maxAgent) {
			System.out.println("envoi message coalition ok");
			msgRespond.setContent("ok");
			this.members.put(msg.getSender().getLocalName(),"Other");
		}
		else {
			System.out.println("envoi message coalition non");
			msgRespond.setContent("no");
		}
		msgRespond.addReceiver(new AID(msg.getSender().getLocalName(),AID.ISLOCALNAME));
		((AbstractDedaleAgent)this.myAgent).sendMessage(msgRespond);
		
	}	
	
	//envoie du nombre d'agent dans la coalition
	private void nbAgentCoalition(ACLMessage msg) {
		ACLMessage msgRespond = new ACLMessage(ACLMessage.INFORM);
		msgRespond.setSender(this.myAgent.getAID());
		msgRespond.setProtocol(this.id_Coal + ": golem position");
		msgRespond.setContent("" + this.members.size());
		msgRespond.addReceiver(new AID(msg.getSender().getLocalName(),AID.ISLOCALNAME));
		((AbstractDedaleAgent)this.myAgent).sendMessage(msgRespond);
	}
	
	//envoi de la position probablee du golem suivis paar la coalition
	private void golemPosition(ACLMessage msg) {
		ACLMessage msgRespond = new ACLMessage(ACLMessage.INFORM);
		msgRespond.setSender(this.myAgent.getAID());
		msgRespond.setProtocol(this.id_Coal + ": nb Agent");
		msgRespond.setContent(this.golemLocalitation);
		msgRespond.addReceiver(new AID(msg.getSender().getLocalName(),AID.ISLOCALNAME));
		((AbstractDedaleAgent)this.myAgent).sendMessage(msgRespond);
	}
	
	public boolean done() {
		return finished;
	}
	
}
