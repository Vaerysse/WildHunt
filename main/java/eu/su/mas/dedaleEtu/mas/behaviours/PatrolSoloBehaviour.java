package eu.su.mas.dedaleEtu.mas.behaviours;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

import dataStructures.tuple.Couple;
import eu.su.mas.dedale.env.Observation;
import eu.su.mas.dedale.mas.AbstractDedaleAgent;
import eu.su.mas.dedaleEtu.mas.agents.dummies.ExploreSoloAgent;
import eu.su.mas.dedaleEtu.mas.knowledge.MapRepresentation;
import eu.su.mas.dedaleEtu.mas.knowledge.MapRepresentation.MapAttribute;
import jade.core.Agent;
import jade.core.behaviours.SimpleBehaviour;
import eu.su.mas.dedaleEtu.princ.Coalition;

public class PatrolSoloBehaviour extends SimpleBehaviour{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private MapRepresentation myMap;
	private boolean finished = false;
	private String bestPath;
	private int step;
	
	public PatrolSoloBehaviour(final ExploreSoloAgent myAgent, MapRepresentation myMap) {
		super(myAgent);
		this.myMap = myMap;
		this.step = 0;
	}

	@Override
	public void action() {
		
		//Problem, map no explore
		if (this.myMap == null) {
			this.finished = true;
		}
		
		//0) Retrieve the current position
		String myPosition = ((AbstractDedaleAgent)this.myAgent).getCurrentPosition();
		
		
		if (myPosition != null){
			//List of observable from the agent's current position
			List<Couple<String,List<Couple<Observation,Integer>>>> lobs=((AbstractDedaleAgent)this.myAgent).observe();//myPosition
			//System.out.println(this.myAgent.getLocalName() + " : " +lobs);
			
			/**
			 * Just added here to let you see what the agent is doing, otherwise he will be too quick
			 */
			try {
				this.myAgent.doWait(500);
			} catch (Exception e) {
				e.printStackTrace();
			}
			
			//1) remove the current node from openlist and add it to closedNodes.

			this.myMap.addNode(myPosition,MapAttribute.closed, System.currentTimeMillis(), "-1", false, 0.0); //attention 4éme argument actuellement a false car pas de detection de golme actuel
			this.myMap.resetPourcentGolem();//nouveau tour du behaviour donc l'agent et le golem se sont peut êtré déplacer, il faut remettre le pourcentage des golems à 0
			
			/**
			 * JE REGARDE SI JE SENS UN GOLEM ET REAGIS EN CONSEQUENCE
			 */
			//2) je regarde si je sens un golem
			boolean Golem_Present = false;
			Iterator<Couple<String, List<Couple<Observation, Integer>>>> iter = lobs.iterator();
			while(iter.hasNext()){
				Couple<String, List<Couple<Observation, Integer>>> temp = iter.next();
				List<Couple<Observation, Integer>> couple = temp.getRight();
				String ID_node = temp.getLeft();
				//System.out.println("id, couple");
				//System.out.println(ID_node);
				//System.out.println(couple);
				//si je sens le golem
				if(couple.size() > 0) {
					Golem_Present = true;
					// je mets a jour le noeud pour dire que je le sens
					//System.out.println("J'attends les ordres !!!");
					//System.out.println("je sens");
					this.myMap.setGolemDetection(ID_node, true, myPosition);
				}
				else {
					//System.out.println("je ne le sens pas");
					this.myMap.setGolemDetection(ID_node, false, myPosition);
				}
			}
			//3) si j'ai senti un golem
			if(Golem_Present) {
				if (!((ExploreSoloAgent)this.myAgent).isInPursuit()) { // si je ne suis pas déjà entrain de poursuivre un golem
					//JE LANCE LA PROCEDURE DE COALITION + ATTRAPAGE DE GOLEM MOUHAHAHAHAHA
					System.out.println("GOLEM - " + this.myAgent.getLocalName());
					Coalition coal = new Coalition((ExploreSoloAgent)this.myAgent);
					this.myAgent.addBehaviour(new SayGolem(((ExploreSoloAgent)this.myAgent), myPosition));
					((ExploreSoloAgent)this.myAgent).setInPursuit(true);
				}
				else {
					System.out.println("Je poursuis déjà - " + this.myAgent.getLocalName());
				}
				
			}
			else{//3) sinon calcule objectif et chemin (si besoin)
				this.bestPath = this.myMap.bestReward(lobs);
				//System.out.println("New objectif : " + this.bestPath);


				//4) move
				//is agent move is true
				if (((ExploreSoloAgent) this.myAgent).isMoving()){
					//on enregistre le dernier noeud où on était avant de bouger
					((ExploreSoloAgent)this.myAgent).setLastVisitedNode(myPosition);
					//si l'agent n'est pas a destination
					//System.out.println(this.myAgent.getLocalName() + " my position : " + myPosition + ", je doit aller en : " + this.bestPath);
					((AbstractDedaleAgent)this.myAgent).moveTo(this.bestPath);//agent move 1 node
					//si agent bloqué
					if(myPosition.equals(((AbstractDedaleAgent)this.myAgent).getCurrentPosition())) {
						Random rand = new Random();
						int nb = rand.nextInt(lobs.size());
						//System.out.println(this.myAgent.getLocalName() + " moveTo : " + lobs.get(nb).getLeft());						
						((AbstractDedaleAgent)this.myAgent).moveTo(lobs.get(nb).getLeft().toString());
					}
				}	

					/**
					//Delete from the blackList the agent not present into the ray of communication on the previous node
					for(int a = 0; a < ((ExploreSoloAgent)this.myAgent).getAgentBlackList().size(); ) {
						if (!((ExploreSoloAgent)this.myAgent).getAgentZoneList().contains(((ExploreSoloAgent)this.myAgent).getAgentBlackList().get(a)) && !((ExploreSoloAgent)this.myAgent).getAgentBlackList().get(a).equals(this.myAgent.getLocalName())){
							((ExploreSoloAgent)this.myAgent).supAgentBlackList(((ExploreSoloAgent)this.myAgent).getAgentBlackList().get(a));
						}
						else {
							a++;
						}
					}
					((ExploreSoloAgent)this.myAgent).supAgentZoneList();
					 **/
				else {
					//System.out.println("Ho là là, je suis si fatigué!");
				}
			}
		}
	}


	@Override
	public boolean done() {
		return finished;
	}
	
	

}
