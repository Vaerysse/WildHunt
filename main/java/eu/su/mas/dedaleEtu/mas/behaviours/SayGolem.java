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

	private static final long serialVersionUID = 5L;
	private String idCoal;
	
	/**
	 * An agent tries to contact its friends and to give them the position of the golem it has seen.
	 * @param myagent the agent who possess the behaviour
	 * @param myPosition the agent's position, when he has detected a golem
	 */
	public SayGolem(final Agent myagent, String id_Coal) {
		super(myagent, 2000); // TODO: ajuster le timer
		this.idCoal = id_Coal;

	}

	@Override
	public void onTick() {
		

		// Update position
		String myPosition = ((ExploreSoloAgent)this.myAgent).getCurrentPosition();
					//A message is defined by : a performative, a sender, a set of receivers, (a protocol),(a content (and/or contentOBject))
		ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
		msg.setSender(this.myAgent.getAID());			
		msg.setProtocol("GolemFoundProtocol");
		
		if (myPosition != "") {
			((ExploreSoloAgent)this.myAgent).cleanAgentPositionList();				
			System.out.println("GOLEM !!!!!!!!!!! Agent "+ this.myAgent.getLocalName() + " has seen a golem when at " + myPosition);
			msg.setContent(this.idCoal);
				
			// TODO: passer par les pages jaunes pr un broadcast
			msg.addReceiver(new AID("Explo1", AID.ISLOCALNAME));
			msg.addReceiver(new AID("Explo2", AID.ISLOCALNAME));
	
			//Mandatory to use this method (it takes into account the environment to decide if someone is reachable or not)
			((AbstractDedaleAgent)this.myAgent).sendMessage(msg);
		}
	}
}
