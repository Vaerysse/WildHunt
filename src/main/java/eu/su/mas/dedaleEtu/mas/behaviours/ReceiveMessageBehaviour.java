package eu.su.mas.dedaleEtu.mas.behaviours;

import eu.su.mas.dedaleEtu.mas.agents.dummies.ExploreSoloAgent;
import jade.core.Agent;
import jade.core.behaviours.SimpleBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

/**
 * TODO: mettre à jour la doc
 * This behaviour is a one Shot.
 * It receives a message tagged with an inform performative, print the content in the console and destroy itlself
 * 
 * @author Cédric Herpson
 *
 */
public class ReceiveMessageBehaviour extends SimpleBehaviour{

	private static final long serialVersionUID = 9088209402507795289L;

	private boolean finished = false;

	/**
	 * 
	 * It receives a message tagged with an inform performative, print the content in the console and destroy itself
	 * @param myagent
	 */
	public ReceiveMessageBehaviour(final Agent myagent) {
		super(myagent);

	}


	public void action() {
		//1) receive the message
		final MessageTemplate msgTemplate = MessageTemplate.and(MessageTemplate.MatchPerformative(ACLMessage.INFORM), 
																MessageTemplate.MatchProtocol("UselessProtocol"));			

		final ACLMessage msg = this.myAgent.receive(msgTemplate);
		
		System.out.println("Coucou, tu veux voir ma b.... belle reception de message?");
		if (msg != null) {	
			System.out.println(this.myAgent.getLocalName()+"<----Result received from "+msg.getSender().getLocalName()+" ,content= "+msg.getContent());
			//((ExploreSoloAgent)this.myAgent).addAgentBlackList(msg.getSender().getLocalName());
			((ExploreSoloAgent)this.myAgent).setMoving(false);
			this.myAgent.addBehaviour(new PrivateChannelBehaviour(this.myAgent, msg.getSender().getLocalName()));
			
			//création canal privé
		}else{
			block();// the behaviour goes to sleep until the arrival of a new message in the agent's Inbox.
		}
	}

	public boolean done() {
		return finished;
	}

}

