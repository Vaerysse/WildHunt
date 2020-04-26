package eu.su.mas.dedaleEtu.mas.behaviours;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

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
	
	private List<String> members = new ArrayList <String> ();
	private int maxAgent;
	private boolean candidatAgentOpen;
	private String golemLocalitation;
	private int stepMSG;
	private int numUpdate;
	private String fatherLastMSG;
	private List<String> sonLastMSG = new ArrayList<String>();
	
	
	private long timer;
	
	public CoalitionBehaviour(final Agent myagent, String id) {
		super(myagent);

		this.id_Coal = id;
		this.members.add(this.myAgent.getLocalName());
		this.candidatAgentOpen = true;
		this.stepMSG = 1;
		this.numUpdate = 0;
		
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
		
		final MessageTemplate msgTemplatenUpdate = MessageTemplate.and(MessageTemplate.MatchPerformative(ACLMessage.INFORM), 
				MessageTemplate.MatchProtocol(this.id_Coal + ": Update behaviour"));			

		final ACLMessage msgUpdate = this.myAgent.receive(msgTemplatenUpdate);	
		
		
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
					// update donnée map et envoie coalition
					//supprimé behaviour SayGolem (et passer l'info au autre membre de la coalition) 
					//lancer calcule pour choper le golem (calcule fait dans l'agent leader) (envoie de la possition à chaque agent)
				}
			}
			else {
				//TODO
			}
		}	
		//TODO
		//je recoit une update des données du behaviour 
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
			this.members.add(msg.getSender().getLocalName());
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
	
	//envoie message string a une liste spécifique d'agent
	private void sendMessageStringCoalition(String protocol, List<String> agents, String content) {
		ACLMessage msgSend = new ACLMessage(ACLMessage.INFORM);
		msgSend.setSender(this.myAgent.getAID());
		msgSend.setProtocol(protocol);
		msgSend.setContent(content);
		
		for(int i = 0; i< agents.size(); i++) {
			msgSend.addReceiver(new AID(agents.get(i),AID.ISLOCALNAME));
		}
		
		((AbstractDedaleAgent)this.myAgent).sendMessage(msgSend);
		
	}
	
	//envoie message object a une liste spécifique d'agent
	private void sendMessageStringCoalition(String protocol, List<String> agents, Object content) {
		ACLMessage msgSend = new ACLMessage(ACLMessage.INFORM);
		msgSend.setSender(this.myAgent.getAID());
		msgSend.setProtocol(protocol);
		try {
			msgSend.setContentObject((Serializable) content);
		} catch (IOException e) {
			msgSend.setContent("-1");
			System.out.println("Send message problem");
			e.printStackTrace();
		}
		
		for(int i = 0; i< agents.size(); i++) {
			msgSend.addReceiver(new AID(agents.get(i),AID.ISLOCALNAME));
		}
		
		((AbstractDedaleAgent)this.myAgent).sendMessage(msgSend);
		
	}
	
	//permet de définir ou mettre les agents pour capturer le golem
	private void calculPositionCaptureGolem() {
		
	}
	
	//envoie la mise a jours des données du behaviour
	private void sendUpdateDataBehaviour() {
		HashMap<String, List <String>> update = new HashMap<String, List <String>>();
		update.put("members", this.members);
		
		List<String> updatemaxAgent = new ArrayList<String>();
		updatemaxAgent.add(""+this.maxAgent);
		update.put("maxAgent", updatemaxAgent);
		
		List<String> updatecandidatAgentOpen = new ArrayList<String>();
		updatemaxAgent.add(""+this.candidatAgentOpen);
		update.put("candidatAgentOpen", updatecandidatAgentOpen);

		List<String> updategolemLocalitation = new ArrayList<String>();
		updatemaxAgent.add(""+this.golemLocalitation);
		update.put("golemLocalitation", updategolemLocalitation);

		List<String> updatestepMSG = new ArrayList<String>();
		updatemaxAgent.add(""+this.stepMSG);
		update.put("stepMSG", updatestepMSG);
		
		List<String> updatenumUpdate = new ArrayList<String>();
		updatemaxAgent.add(""+this.numUpdate);
		update.put("numUpdate", updatenumUpdate);
		
		this.sendMessageStringCoalition(this.id_Coal + ": Update behaviour", this.members, update);
	}
	
	public boolean done() {
		return finished;
	}
	
}
