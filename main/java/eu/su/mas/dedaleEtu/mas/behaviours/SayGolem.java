package eu.su.mas.dedaleEtu.mas.behaviours;

import eu.su.mas.dedale.mas.AbstractDedaleAgent;
import eu.su.mas.dedaleEtu.mas.agents.dummies.ExploreSoloAgent;
import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.TickerBehaviour;
import jade.lang.acl.ACLMessage;

public class SayGolem extends TickerBehaviour{
	/**
	 * Behaviour déclenché lorsqu'un agent détecte un golem (cf PatrolBehaviour).
	 * L'agent broadcast alors la position à laquelle il a détecté le golem et essaye de recruter d'autres agents afin de former une coalition. 
	 */

	private boolean first;
	private static final long serialVersionUID = 5L;
	
	/**
	 * The agent's position, when he has detected a golem.
	 */
	private String myPosition;

	/**
	 * An agent tries to contact its friends and to give them the position of the golem it has seen.
	 * @param myagent the agent who possess the behaviour
	 * @param myPosition the agent's position, when he has detected a golem
	 */
	public SayGolem(final Agent myagent, String myPosition) {
		super(myagent, 2000); // TODO: ajuster le timer
		this.myPosition = myPosition;
		this.first = true;

	}

	@Override
	public void onTick() {

		//A message is defined by : a performative, a sender, a set of receivers, (a protocol),(a content (and/or contentOBject))
		ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
		msg.setSender(this.myAgent.getAID());
		msg.setProtocol("GolemFoundProtocol");
		
		// TODO: voir si on peut mettre à jour la position que l'agent broadcast, s'il détecte le golem ailleurs
		if (myPosition != ""){
			((ExploreSoloAgent)this.myAgent).cleanAgentPositionList();
			if (this.first) {
				System.out.println("FIRST - " + this.myAgent.getLocalName());
				this.first = false;
			}
			System.out.println("GOLEM !!!!!!!!!!! Agent "+ this.myAgent.getLocalName() + " has seen a golem when at " + myPosition);
			msg.setContent(myPosition);
			
			// TODO: passer par les pages jaunes pr un broadcast
			msg.addReceiver(new AID("Explo1", AID.ISLOCALNAME));
			msg.addReceiver(new AID("Explo2", AID.ISLOCALNAME));

			//Mandatory to use this method (it takes into account the environment to decide if someone is reachable or not)
			((AbstractDedaleAgent)this.myAgent).sendMessage(msg);
		}
	}
}
