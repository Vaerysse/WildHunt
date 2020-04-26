package eu.su.mas.dedaleEtu.mas.behaviours;

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
	
	public ReceiveMessageSayGolemBehaviour(final Agent myagent) {
		super(myagent);

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
														MessageTemplate.MatchProtocol(msg.getContent() + ": golem position"));			

		final ACLMessage msgSameGolem = this.myAgent.receive(msgTemplateSameGolem);
	
		//System.out.println("Coucou, tu veux voir ma b.... belle reception de message de SayGolem?");
	
		//2)if msg no null and is not the same agent
		if ((msgRequest != null) || ((msg != null) && !(msg.getSender().getLocalName().equals(this.myAgent.getLocalName())))) {
			// agent is in a coalition?
			if (!((ExploreSoloAgent)this.myAgent).getInCoalition()) { // si l'agent n'est pas dans une coalition
				if (msg != null) {
				((ExploreSoloAgent)this.myAgent).setMoving(false);
				//1) je demande de rentrer dans la coalition
				if (this.requestEnterCoalition){
					// Sending a message to ask for the opening of a private channel
					ACLMessage msgSend = new ACLMessage(ACLMessage.INFORM);
					msgSend.setSender(this.myAgent.getAID());
					msgSend.setProtocol(msg.getContent());
					msgSend.setContent("RequestEntry?");// a définir dans le future si besoin
					msgSend.addReceiver(new AID(msg.getSender().getLocalName(), AID.ISLOCALNAME));
					((AbstractDedaleAgent)this.myAgent).sendMessage(msgSend);
					this.requestEnterCoalition = false;
					this.respondEnterCoalition = true;
					this.timer = System.currentTimeMillis();
					if (log) {
						System.out.println(this.myAgent.getLocalName() + ": I want to enter the coalition ");
					}
						
				}}
				//2) attente de la réponse
				if (this.respondEnterCoalition && msgRequest != null) {
					System.out.println("test send msgRequest");
					//2)a) je rentre dans la coalition
					if (!msgRequest.getContent().equals("no")) {
						//TODO rentré dans coalition
						//ajout behaviour coalition
						this.respondEnterCoalition = false;
						((ExploreSoloAgent)this.myAgent).setInCoalition(true);
						if (log) {
							System.out.println(this.myAgent.getLocalName() + ": enters the coalition ");
						}
					}
					else {//2)b) je ne rentre pas dans la coalition
						this.requestEnterCoalition = true;
						this.respondEnterCoalition = false;
						if (log) {
							System.out.println(this.myAgent.getLocalName() + ": Reject enters the coalition ");
						}
					}
				}
				//2)c) timer out			
				if(System.currentTimeMillis() - this.timer >= wait){
					((ExploreSoloAgent)this.myAgent).setMoving(true);
					if (log) {
						System.out.println(this.myAgent.getLocalName() + ": timeOut enter in the coalition ");
					}
				}
			}
			else { // si l'agent est dans une coalition
				
				if (msg != null && this.sendGolemPosition) {
					
					// On vérifie que l'agent n'est pas déjà dans la coalition de l'émetteur
					// (s'il est dans la même coalition, il ne doit pas répondre)
					// Si ce n'est pas la même coalition et qu'il est leader
                    if (!((ExploreSoloAgent)this.myAgent).getIDCoalition().equals(msg.getContent()) && ((ExploreSoloAgent)this.myAgent).getLeaderCoalition()) {
						
                    	int maxDegree = ((ExploreSoloAgent)this.myAgent).getMap().getMaxDegree(); // maximal degree of the graph
                    	
                    	//1) Envoi de la localisation du golem
                    	ACLMessage msgSend = new ACLMessage(ACLMessage.INFORM);
    					msgSend.setSender(this.myAgent.getAID());
    					msgSend.setProtocol(msg.getContent() + ": golem position");
    					String golemPosition = ""; // TODO: remplacer par la méthode permettant d'obtenir l'info
    					msgSend.setContent(golemPosition); 
    					msgSend.addReceiver(new AID(msg.getSender().getLocalName(), AID.ISLOCALNAME));
    					((AbstractDedaleAgent)this.myAgent).sendMessage(msgSend);
    					this.sendGolemPosition = false;
    					this.sendCoalitionSize = true;
    					//this.timer = System.currentTimeMillis();
    					if (log) {
    						System.out.println(this.myAgent.getLocalName() + ": sending my golem position " + golemPosition);
    					}
             
    					//2) Message retour
    					//msgSameGolem
    						//regarder si même golem
                            // si mêeme golem                    
                            //envoie de nombre de personne dans ma coalition 
                            //message retour
                                // si fusion possible
                    	
                    	
                    	
                    	
						 
						// en théorie, si les agents sont tous les 2 dans une coalition différente, c'est que au moins 2 agents ont vu un golem
						// la question est : est-ce que c'est le même golem ? Comment obtenir cette information
						// si oui, il faut fusionner les coalitions afin d'en avoir au moins une de remplie
						// sinon ?
						
						
						
						
						// coalition de l'agent pas full
						if(!((ExploreSoloAgent)this.myAgent).getInCoalitionFull()) {
							
							((ExploreSoloAgent)this.myAgent).setMoving(false);	
						
						
						
						
						}
						
						// coalition de l'agent full
						else {
							
							
							
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
