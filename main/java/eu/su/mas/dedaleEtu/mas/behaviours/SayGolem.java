package eu.su.mas.dedaleEtu.mas.behaviours;

import java.util.ArrayList;
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

public class SayGolem extends TickerBehaviour{
	/**
	 * Behaviour déclenché lorsqu'un agent détecte un golem (cf PatrolBehaviour).
	 * L'agent broadcast alors la position à laquelle il a détecté le golem et essaye de recruter d'autres agents afin de former une coalition. 
	 */

	private static final long serialVersionUID = 5L;
	private String idCoal;
	private List<AID> agentsAID = new ArrayList<>(); // liste des agents présents sur la map (sauf moi)
	
	/**
	 * An agent tries to contact its friends and to give them the position of the golem it has seen.
	 * @param myagent the agent who possess the behaviour
	 * @param myPosition the agent's position, when he has detected a golem
	 */
	public SayGolem(final Agent myagent, String id_Coal) {
		super(myagent, 1000); // TODO: ajuster le timer
		this.idCoal = id_Coal;
		
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
		/*
		System.out.println(this.myAgent.getName() + " : PAGES JAUNES");
        if (this.agentsAID.size() > 0) {
        	for (AID aid : this.agentsAID) {
        		System.out.println(aid);
        	}
        }
        */
        //System.out.println(this.agentsAID.size() + " résultats" );

	}

	@Override
	public void onTick() {
		
		if (!((ExploreSoloAgent)this.myAgent).getInCoalitionFull() && ((ExploreSoloAgent)this.myAgent).getSayGolemOK()) {
			System.out.println(this.myAgent.getLocalName() + ": say golem!");
			// Update position
			String myPosition = ((ExploreSoloAgent)this.myAgent).getCurrentPosition();
					//A message is defined by : a performative, a sender, a set of receivers, (a protocol),(a content (and/or contentOBject))
			ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
			msg.setSender(this.myAgent.getAID());			
			msg.setProtocol("GolemFoundProtocol");
		
			if (myPosition != "") {
				((ExploreSoloAgent)this.myAgent).cleanAgentPositionList();				
				//System.out.println("GOLEM !!!!!!!!!!! Agent "+ this.myAgent.getLocalName() + " has seen a golem when at " + myPosition);
				msg.setContent(this.idCoal);
				
				// Broadcast à tous les agents
				for (AID aid : this.agentsAID) {
					msg.addReceiver(aid);
				}
	
				//Mandatory to use this method (it takes into account the environment to decide if someone is reachable or not)
				((AbstractDedaleAgent)this.myAgent).sendMessage(msg);
			}
		}
	}
}
