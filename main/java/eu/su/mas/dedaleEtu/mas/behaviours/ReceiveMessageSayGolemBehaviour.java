package eu.su.mas.dedaleEtu.mas.behaviours;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import eu.su.mas.dedale.mas.AbstractDedaleAgent;
import eu.su.mas.dedaleEtu.mas.agents.dummies.ExploreSoloAgent;
import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.SimpleBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

public class ReceiveMessageSayGolemBehaviour extends SimpleBehaviour{

	private boolean log = true;
	private boolean finished = false;
	private static final int wait = 2000;
	private long timer;
	private boolean requestEnterCoalition = true;
	private boolean respondEnterCoalition = false;
	private boolean sendGolemPosition = true;
	private boolean sendCoalitionSize = false;
	private String numCoalition;
	private boolean timerStart;
	
	public ReceiveMessageSayGolemBehaviour(final Agent myagent) {
		super(myagent);
		
		this.numCoalition = "C0";
		this.timerStart = false;

	}

	@Override
	public void action() {
		//1) receive the message
		final MessageTemplate msgTemplate = MessageTemplate.and(MessageTemplate.MatchPerformative(ACLMessage.INFORM), 
															MessageTemplate.MatchProtocol("GolemFoundProtocol"));			
		final ACLMessage msg = this.myAgent.receive(msgTemplate); // id de la coalition de l'émetteur
		
		final MessageTemplate msgTemplateRequest = MessageTemplate.and(MessageTemplate.MatchPerformative(ACLMessage.INFORM), 
															MessageTemplate.MatchProtocol("AnswerEntry"));			
		final ACLMessage msgRequest = this.myAgent.receive(msgTemplateRequest);
		
		final MessageTemplate msgTemplateSameGolem = MessageTemplate.and(MessageTemplate.MatchPerformative(ACLMessage.INFORM), 
														MessageTemplate.MatchProtocol(this.numCoalition + ": golem position"));			
		final ACLMessage msgSameGolem = this.myAgent.receive(msgTemplateSameGolem);
		
		final MessageTemplate msgTemplatenbAgent = MessageTemplate.and(MessageTemplate.MatchPerformative(ACLMessage.INFORM), 
														MessageTemplate.MatchProtocol(this.numCoalition + ": nb Agent"));			
		final ACLMessage msgnbAgent = this.myAgent.receive(msgTemplatenbAgent);	
	
		//System.out.println("Coucou, tu veux voir ma b.... belle reception de message de SayGolem?");
			
		
		//2)if msg no null and is not the same agent
		if ((msgRequest != null) || ((msg != null) && !(msg.getSender().getLocalName().equals(this.myAgent.getLocalName())))) {
			
			// agent is in a coalition?
			if (!((ExploreSoloAgent)this.myAgent).getInCoalition()) { // si l'agent n'est pas dans une coalition
				
				if (msg != null) {
				((ExploreSoloAgent)this.myAgent).setMoving(false);
				
					//1) je demande de rentrer dans la coalition
					if (this.requestEnterCoalition){
						this.numCoalition = msg.getContent();//eenregistrement de l'identifiant de la coalition
						// Sending a message to ask for the opening of a private channel
						ACLMessage msgSend = new ACLMessage(ACLMessage.INFORM);
						msgSend.setSender(this.myAgent.getAID());
						msgSend.setProtocol("Request enttry coalition");
						List <String> data = new ArrayList <String> ();
						data.add(((AbstractDedaleAgent)this.myAgent).getCurrentPosition());//j'ajoute ma position pour que la coalition sache ou je suis
						try {
							msgSend.setContentObject((Serializable) data);// envoie sa position (list)
						} catch (IOException e) {
							System.out.println("problem send list for entry coalition");
							e.printStackTrace();
						}
						msgSend.addReceiver(new AID(msg.getSender().getLocalName(), AID.ISLOCALNAME));
						((AbstractDedaleAgent)this.myAgent).sendMessage(msgSend);
						this.requestEnterCoalition = false;//pas de nouvelle demande pour entrer dansune coalition
						this.respondEnterCoalition = true;//attente de la réponse
						this.timer = System.currentTimeMillis();//lancement u timer pour l'attente de la réponse
						this.timerStart = true;
						if (log) {
							System.out.println(this.myAgent.getLocalName() + ": I want to enter the coalition ");
						}	
					}
				}
				
				//2) attente de la réponse
				if (this.respondEnterCoalition && msgRequest != null) {
					System.out.println("test send msgRequest");
					this.respondEnterCoalition = false;//je n'attend plus de réponse
					//2)a) je rentre dans la coalition
					if (!msgRequest.getContent().equals("no")) {
						//rentrer dans la coalition
						((ExploreSoloAgent)this.myAgent).entreNewCoalition(this.numCoalition);
						((ExploreSoloAgent)this.myAgent).addBehaviour(new CoalitionBehaviour(this.myAgent, this.numCoalition));
						if (log) {
							System.out.println(this.myAgent.getLocalName() + ": enters the coalition ");
						}
					}
					else {//2)b) je ne rentre pas dans la coalition
						this.requestEnterCoalition = true;//je repase en mode de demande de rentré dans une coalition
						if (log) {
							System.out.println(this.myAgent.getLocalName() + ": Reject enters the coalition ");
						}
					}
					((ExploreSoloAgent)this.myAgent).setMoving(true);
				}
				//2)c) timer out			
				if(this.timerStart && System.currentTimeMillis() - this.timer >= wait){
					((ExploreSoloAgent)this.myAgent).setMoving(true);
					if (log) {
						this.requestEnterCoalition = true;//je repase en mode de demande de rentré dans une coalition
						this.respondEnterCoalition = false;//je n'attend plus de réponse
						this.timerStart = false;
						((ExploreSoloAgent)this.myAgent).setMoving(true);
						System.out.println(this.myAgent.getLocalName() + ": timeOut enter in the coalition ");
					}
				}
			}			
			
			// si l'agent est dans une coalition non remplie et que ce n'est pas un message de ça coalition
			else if (((ExploreSoloAgent)this.myAgent).getInCoalition() && !((ExploreSoloAgent)this.myAgent).getInCoalitionFull() && !((ExploreSoloAgent)this.myAgent).getIDCoalition().equals(msg.getContent()) ) {
				
				String golemPosition = ""; // TODO: remplacer par la méthode permettant d'obtenir l'info
				
				if (msg != null) {
					
					// On vérifie que l'agent n'est pas déjà dans la coalition de l'émetteur
					// (s'il est dans la même coalition, il ne doit pas répondre)
					// Si ce n'est pas la même coalition et qu'il est leader
                    if (!((ExploreSoloAgent)this.myAgent).getIDCoalition().equals(msg.getContent()) && ((ExploreSoloAgent)this.myAgent).getLeaderCoalition()) {
						                    	
                    	//1) Envoi de la localisation du golem
                    	ACLMessage msgSend = new ACLMessage(ACLMessage.INFORM);
    					msgSend.setSender(this.myAgent.getAID());
    					msgSend.setProtocol(msg.getContent() + ": golem position");
    					msgSend.setContent(golemPosition); 
    					msgSend.addReceiver(new AID(msg.getSender().getLocalName(), AID.ISLOCALNAME));
    					((AbstractDedaleAgent)this.myAgent).sendMessage(msgSend);
    					//this.timer = System.currentTimeMillis();
    					if (log) {
    						System.out.println(this.myAgent.getLocalName() + ": sending my golem position " + golemPosition);
    					}
                    }
				}
				
				//2) Message retour sur le golem
				if (msgSameGolem != null) {
									
					// On regarde si les deux coalitions sont sur le même golem
					if (msgSameGolem.getContent().equals(golemPosition)) {
						// Si oui, envoi du nb de personnes dans la coalition de l'agent afin 
						// de déterminer si l'on fusionne les 2 coalitions
						
						ACLMessage msgSend = new ACLMessage(ACLMessage.INFORM);
    					msgSend.setSender(this.myAgent.getAID());
    					msgSend.setProtocol(msg.getContent() + ": nb Agent");
    					String nbAgents = String.valueOf(((ExploreSoloAgent)this.myAgent).getCoalitionSize()); 
    					msgSend.setContent(nbAgents); 
    					msgSend.addReceiver(new AID(msg.getSender().getLocalName(), AID.ISLOCALNAME));
    					((AbstractDedaleAgent)this.myAgent).sendMessage(msgSend);
    					//this.timer = System.currentTimeMillis();
    					if (log) {
    						System.out.println(this.myAgent.getLocalName() + ": sending the size of my coalition " + nbAgents);
    					}
					} 
					
					else { // les 2 coalitions ne sont pas sur le même golem
						// TODO définir ce que l'on fait
						// ex si avDegree atteignable pr chaque coalition, on fait ça
						// sinon on remplie une des 2 coalitions au détriment d'une autre
						
					}
				}
				
				//3) Message retour sur le nb d'agents dans la coalition
				if (msgnbAgent != null) {
					
					int myCoalSize = ((ExploreSoloAgent)this.myAgent).getCoalitionSize();
					int otherCoalSize = Integer.parseInt(msgnbAgent.getContent());
					
					// Récupération du critère de fusion des coalitions
					int criteria = ((ExploreSoloAgent)this.myAgent).getMap().getMaxDegree(); // degré max du graphe
					//int criteria = ((ExploreSoloAgent)this.myAgent).getMap().getAvDegree(); // degré moyen du graphe
					
					if (myCoalSize + otherCoalSize <= criteria) { // juste assez d'agents pour faire une coalition
						// On fusionne les deux coalitions
						// On se base sur l'ordre lexicographique pour savoir quelle coalition absorbe l'autre
						
						String myAID = String.valueOf(this.myAgent.getAID());
						String otherAID = String.valueOf(this.myAgent.getAID());
						
						if (myAID.compareTo(otherAID) <= 0) {
							// TODO: On dissout la coalition et tout le monde passe chez l'autre coalition
						}
						else {
							// TODO: on prévient l'autre leader qu'il doit dissoudre sa coalition
						}	
					}
					
					else { // suffisamment d'agents pour faire 2 coalitions, dont une remplie
						
						// On remplit d'abord la plus pleine afin de limiter les transferts d'agents
						if (myCoalSize >= otherCoalSize) { // c'est ma coalition la plus pleine et je vais récupérer des agents
							int nbTransferedAgents = criteria-myCoalSize;
							// TODO: envoyer un message au leader pour lui dire de transférer nbTransferedAgents agents
							
						}
						else { // ma coalition doit transférer des agents dans l'autre coalition
							int nbTransferedAgents = criteria-otherCoalSize;
							// TODO: prévenir nbTransferedAgents de changer de coalition
						}
						
					}
					
					
			
				}    	
                    	
						 
			}
					
                 
		}
		
		
	}

	@Override
	public boolean done() {
		return finished;
	}
	
	
}
