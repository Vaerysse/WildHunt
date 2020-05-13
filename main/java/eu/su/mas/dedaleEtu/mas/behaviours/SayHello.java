package eu.su.mas.dedaleEtu.mas.behaviours;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import eu.su.mas.dedale.mas.AbstractDedaleAgent;
import eu.su.mas.dedaleEtu.mas.agents.dummies.ExploreSoloAgent;
import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.TickerBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.SearchConstraints;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;

/**
 * This example behaviour try to send a hello message (every 3s maximum) to agents Collect2 Collect1
 * @author hc
 *
 */
public class SayHello extends TickerBehaviour{

	/**
	 * 
	 */
	private static final long serialVersionUID = -2058134622078521998L;
	private List<AID> agentsAID = new ArrayList<>(); // liste des agents présents sur la map (sauf moi)

	/**
	 * An agent tries to contact its friend and to give him its current position
	 * @param myagent the agent who posses the behaviour
	 *  
	 */
	public SayHello (final Agent myagent) {
		super(myagent, 1000);
		//super(myagent);

		DFAgentDescription dfd = new DFAgentDescription();
        ServiceDescription sd = new ServiceDescription();
        sd.setType("agent");
        dfd.addServices(sd);
        
        // Contrainte permettant de récupérer tous les agents
        SearchConstraints ALL = new SearchConstraints();
        ALL.setMaxResults(new Long(-1));
        
        // Recherche des agents dans les pages jaunes
        DFAgentDescription[] result = null;
		try {
			result = DFService.search(this.myAgent, dfd);
		} catch (FIPAException e) {
			e.printStackTrace();
		}
        
		// Récupération des AID uniquement
		for (DFAgentDescription dfad : result) {
			if (!(this.myAgent.getAID().equals(dfad.getName()))) {
				this.agentsAID.add(dfad.getName());
			}
		}
	
		// Affichage des résultats
		//System.out.println(this.myAgent.getName() + " : PAGES JAUNES");
        if (this.agentsAID.size() > 0) {
        	for (AID aid : this.agentsAID) {
        		System.out.println(aid);
        	}
        }
        //System.out.println(this.agentsAID.size() + " résultats" );
	}

	@Override
	public void onTick() {
		String myPosition=((AbstractDedaleAgent)this.myAgent).getCurrentPosition();

		//A message is defined by : a performative, a sender, a set of receivers, (a protocol),(a content (and/or contentOBject))
		ACLMessage msg=new ACLMessage(ACLMessage.INFORM);
		msg.setSender(this.myAgent.getAID());
		msg.setProtocol("UselessProtocol");

		if (myPosition!="" && !((ExploreSoloAgent)this.myAgent).isInPursuit()){
			((ExploreSoloAgent)this.myAgent).cleanAgentPositionList();
			//System.out.println("Agent "+this.myAgent.getLocalName()+ " is trying to reach its friends");
			msg.setContent(myPosition);
			
			// Broadcast à tous les agents
			for (AID aid : this.agentsAID) {
				msg.addReceiver(aid);
			}

			//Mandatory to use this method (it takes into account the environment to decide if someone is reachable or not)
			((AbstractDedaleAgent)this.myAgent).sendMessage(msg);
		}
	}
}