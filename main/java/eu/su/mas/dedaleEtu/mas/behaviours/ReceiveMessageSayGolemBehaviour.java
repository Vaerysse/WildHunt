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

		final MessageTemplate msgTemplatenbAgent = MessageTemplate.and(MessageTemplate.MatchPerformative(ACLMessage.INFORM), 
				MessageTemplate.MatchProtocol(this.numCoalition + ": nb Agent"));			
		final ACLMessage msgnbAgent = this.myAgent.receive(msgTemplatenbAgent);	
		

		//2)if msg no null and is not the same agent
		if ((msgRequest != null) || ((msg != null) && !(msg.getSender().getLocalName().equals(this.myAgent.getLocalName())))) {

			// agent is in a coalition?
			if (!((ExploreSoloAgent)this.myAgent).getInCoalition()) { // si l'agent n'est pas dans une coalition

				if (msg != null  && !((ExploreSoloAgent)this.myAgent).getWaitEnterCoalition()) {
					((ExploreSoloAgent)this.myAgent).setMoving(false);

					//1) je demande à entrer dans la coalition
					if (this.requestEnterCoalition){
						((ExploreSoloAgent)this.myAgent).setWaitEnterCoalition(true);
						this.numCoalition = msg.getContent(); // enregistrement de l'identifiant de la coalition
						// Sending a message to ask for the opening of a private channel
						ACLMessage msgSend = new ACLMessage(ACLMessage.INFORM);
						msgSend.setSender(this.myAgent.getAID());
						msgSend.setProtocol("Request entry coalition");
						List <String> data = new ArrayList <String> ();
						data.add(((AbstractDedaleAgent)this.myAgent).getCurrentPosition()); // j'ajoute ma position pour que la coalition sache où je suis
						try {
							msgSend.setContentObject((Serializable) data);// envoie sa position (list)
						} catch (IOException e) {
							System.out.println("problem send list for entry coalition");
							e.printStackTrace();
						}
						msgSend.addReceiver(new AID(msg.getSender().getLocalName(), AID.ISLOCALNAME));
						((AbstractDedaleAgent)this.myAgent).sendMessage(msgSend);
						this.requestEnterCoalition = false; // pas de nouvelle demande pour entrer dans une coalition
						this.respondEnterCoalition = true; // attente de la réponse
						this.timer = System.currentTimeMillis(); // lancement du timer pour l'attente de la réponse
						this.timerStart = true;
						if (log) {
							System.out.println(this.myAgent.getLocalName() + ": I want to enter the coalition ");
						}	
					}
				}

				//2) attente de la réponse
				if (this.respondEnterCoalition && msgRequest != null) {

					System.out.println("test send msgRequest");
					this.respondEnterCoalition = false; // je n'attends plus de réponse
					//2)a) je rentre dans la coalition
					if (!msgRequest.getContent().equals("no")) {

						// Rentrer dans la coalition
						((ExploreSoloAgent)this.myAgent).entreNewCoalition(this.numCoalition);
						((ExploreSoloAgent)this.myAgent).addBehaviour(new CoalitionBehaviour(this.myAgent, this.numCoalition));
						if (log) {
							System.out.println(this.myAgent.getLocalName() + ": enters the coalition ");
						}
					}
					else { //2)b) je ne rentre pas dans la coalition
						((ExploreSoloAgent)this.myAgent).setWaitEnterCoalition(false);
						this.requestEnterCoalition = true; // je repasse en mode de demande de rentrer dans une coalition
						if (log) {
							System.out.println(this.myAgent.getLocalName() + ": Reject enters the coalition ");
						}
						((ExploreSoloAgent)this.myAgent).setMoving(true);
					}
				}

				//2)c) timer out			
				if (this.timerStart && System.currentTimeMillis() - this.timer >= wait) {
					((ExploreSoloAgent)this.myAgent).setMoving(true);
					((ExploreSoloAgent)this.myAgent).setWaitEnterCoalition(false);
					if (log) {
						this.requestEnterCoalition = true; // je repasse en mode de demande de rentrer dans une coalition
						this.respondEnterCoalition = false; // je n'attends plus de réponse
						this.timerStart = false;
						System.out.println(this.myAgent.getLocalName() + ": timeOut enter in the coalition ");
					}
				}
			}			
			/*
			// si l'agent est dans une coalition non remplie et que ce n'est pas un message de ça coalition
			else if (((ExploreSoloAgent)this.myAgent).getInCoalition() && !((ExploreSoloAgent)this.myAgent).getInCoalitionFull() && !((ExploreSoloAgent)this.myAgent).getIDCoalition().equals(msg.getContent()) ) {

				if (msg != null) {

					// On vérifie que l'agent n'est pas déjà dans la coalition de l'émetteur
					// (s'il est dans la même coalition, il ne doit pas répondre)
					// Si ce n'est pas la même coalition et qu'il est leader
					if (!((ExploreSoloAgent)this.myAgent).getIDCoalition().equals(msg.getContent()) && ((ExploreSoloAgent)this.myAgent).getLeaderCoalition()) {

						// 1) Envoi du nb de personnes dans la coalition de l'agent afin 
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
					
					// Si ce n'est pas la même coalition et qu'il n'est PAS leader
					else if (!((ExploreSoloAgent)this.myAgent).getIDCoalition().equals(msg.getContent()) && !((ExploreSoloAgent)this.myAgent).getLeaderCoalition()) {
						// L'agent doit alors transmettre l'info au leader de sa coalition
						// TODO: comment gérer ce cas là ?
						System.out.println("What should I dooooooooooo TTuTT");
					}
				}

				//2) Message retour sur le nb d'agents dans la coalition
				if (msgnbAgent != null) {

					int myCoalSize = ((ExploreSoloAgent)this.myAgent).getCoalitionSize();
					int otherCoalSize = Integer.parseInt(msgnbAgent.getContent());
					
					// On se base sur l'ordre lexicographique pour savoir quelle coalition absorbe l'autre
					String myAID = String.valueOf(this.myAgent.getAID());
					String otherAID = String.valueOf(this.myAgent.getAID());

					// Récupération du critère de fusion des coalitions
					int criteria = ((ExploreSoloAgent)this.myAgent).getMap().getMaxDegree(); // degré max du graphe
					//int criteria = ((ExploreSoloAgent)this.myAgent).getMap().getAvDegree(); // degré moyen du graphe

					if (myCoalSize + otherCoalSize <= criteria) { // juste assez d'agents pour faire une coalition
						// Dans ce cas, on veut fusionner les deux coalitions
												
						// TODO: On dissoud la coalition et tout le monde passe chez l'autre coalition
						// Dans CoalitionBehaviour, lorsque le message matchant ce protocol est reçu,
						// on appelle une méthode dissolveAndGoToCoal qui prévient tous les agents de la coalition
						// qu'il faut aller dans la nouvelle coal
						ACLMessage msgSend = new ACLMessage(ACLMessage.INFORM);
						msgSend.setSender(this.myAgent.getAID());
						msgSend.setProtocol(msg.getContent() + ": dissolve and go to new coalition"); 
						
						// On définit le message (leader de la coalition à rejoindre) puis le destinataire (leader de la coalition à dissoudre)
						if (myAID.compareTo(otherAID) <= 0) { // je m'envoie le message à moi-même
							// Tout le monde doit aller dans l'autre coalition
							msgSend.setContent(otherAID);
							msgSend.addReceiver(this.myAgent.getAID());
						}
						else { // j'envoie le message à l'autre leader
							// Tout le monde doit venir dans ma coalition
							msgSend.setContent(myAID);
							msgSend.addReceiver(new AID(msg.getSender().getLocalName(), AID.ISLOCALNAME));
						}
						
						// On envoie le message
						((AbstractDedaleAgent)this.myAgent).sendMessage(msgSend);
					}

					else { // suffisamment d'agents pour faire 2 coalitions, dont une remplie
						
						ACLMessage msgSend = new ACLMessage(ACLMessage.INFORM);
						msgSend.setSender(this.myAgent.getAID());
						msgSend.setProtocol(msg.getContent() + ": change coalition");
						
						// On remplit d'abord la plus pleine afin de limiter les transferts d'agents
						if (myCoalSize >= otherCoalSize) { // c'est ma coalition la plus pleine et je vais récupérer des agents
							int nbTransferedAgents = criteria-myCoalSize;
							msgSend.setContent(myAID + " " + nbTransferedAgents);
							msgSend.addReceiver(new AID(msg.getSender().getLocalName(), AID.ISLOCALNAME));

						}
						else { // ma coalition doit transférer des agents dans l'autre coalition
							int nbTransferedAgents = criteria-otherCoalSize;
							msgSend.setContent(otherAID + " " + nbTransferedAgents);
							msgSend.addReceiver(this.myAgent.getAID());
						}
					}
				}    	
			}  
			*/
		}
	}

	@Override
	public boolean done() {
		return finished;
	}


}
