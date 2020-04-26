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
	private String IDCoalition; 
	
	public ReceiveMessageSayGolemBehaviour(final Agent myagent) {
		super(myagent);

	}

	@Override
	public void action() {
		//1) receive the message
		final MessageTemplate msgTemplate = MessageTemplate.and(MessageTemplate.MatchPerformative(ACLMessage.INFORM), 
															MessageTemplate.MatchProtocol("GolemFoundProtocol"));			

		final ACLMessage msg = this.myAgent.receive(msgTemplate);
		
		final MessageTemplate msgTemplateRequest = MessageTemplate.and(MessageTemplate.MatchPerformative(ACLMessage.INFORM), 
															MessageTemplate.MatchProtocol("AnswerEntry"));			

		final ACLMessage msgRequest = this.myAgent.receive(msgTemplateRequest);
	
		//System.out.println("Coucou, tu veux voir ma b.... belle reception de message de SayGolem?");
	
		//2)if msg no null and is not the same agent
		if ( (msgRequest != null) || ((msg != null) && !(msg.getSender().getLocalName().equals(this.myAgent.getLocalName())))) {
			// agent is in a coalition?
			if(!((ExploreSoloAgent)this.myAgent).getInCoalition()) { // si l'agent n'est pas dans une coalition
				if((msg != null)) {
				((ExploreSoloAgent)this.myAgent).setMoving(false);
				//1) je demande de rentrer dans la coalition
				if (this.requestEnterCoalition){
					// Sending a message to ask for the opening of a private channel
					this.IDCoalition = msg.getContent();
					ACLMessage msgSend = new ACLMessage(ACLMessage.INFORM);
					msgSend.setSender(this.myAgent.getAID());
					msgSend.setProtocol(this.IDCoalition);
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
				if(this.respondEnterCoalition && msgRequest != null) {
					System.out.println("test send msgRequest");
					//2)a) je rentre dans la coalition
					if (!msgRequest.getContent().equals("no")) {						
						this.myAgent.addBehaviour(new CoalitionBehaviour( this.myAgent, this.IDCoalition)); //open behaviour special coalition
						this.respondEnterCoalition = false;
						((ExploreSoloAgent)this.myAgent).setInCoalition(true);
						((ExploreSoloAgent)this.myAgent).setIDCoalition(this.IDCoalition);
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
				
				if(msg != null){
					// On vérifie que l'agent n'est pas déjà dans la coalition de l'émetteur
					// (s'il est dans la même coalition, il ne doit pas répondre)
					//Si ce n'est pas la même coalition et qu'il est leader
					if (!((ExploreSoloAgent)this.myAgent).getCoalitionId().equals(msg.getContent()) && ((ExploreSoloAgent)this.myAgent).getLeaderCoalition()) {
						// envoie de la localiation du golem
						// message retour: regarder si même golem
							// si mêeme golem					
							//envoie de nombre de personne dans ma coalition 
							//message retour
								// si fusion possible
						
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
