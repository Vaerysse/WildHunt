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
		
		//System.out.println("Coucou, tu veux voir ma b.... belle reception de message?");
		
		//2)if msg no null
		if (msg != null){
			//add sender agent in AgentZoneList
			if (!((ExploreSoloAgent)this.myAgent).getAgentZoneList().contains(msg.getSender().getLocalName())){
				((ExploreSoloAgent)this.myAgent).addAgentZoneList(msg.getSender().getLocalName());
			}
			// verification sender name is not in agentBlackList
			if (((ExploreSoloAgent)this.myAgent).getAgentBlackList().contains(msg.getSender().getLocalName()) == false) {	
				System.out.println(this.myAgent.getLocalName()+"<----Result received from "+msg.getSender().getLocalName()+" ,content= "+msg.getContent());
				//add sender name in agentBlackList
				((ExploreSoloAgent)this.myAgent).addAgentBlackList(msg.getSender().getLocalName());
				//Stop the move
				((ExploreSoloAgent)this.myAgent).setMoving(false);
				//Open the private communication between this agent and sender agent
				this.myAgent.addBehaviour(new PrivateChannelBehaviour(this.myAgent, msg.getSender().getLocalName()));
			}
		}else{
			block();// the behaviour goes to sleep until the arrival of a new message in the agent's Inbox.
		}
	}

	public boolean done() {
		return finished;
	}

}

