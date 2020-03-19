package eu.su.mas.dedaleEtu.mas.behaviours;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import dataStructures.tuple.Couple;
import eu.su.mas.dedale.env.Observation;
import eu.su.mas.dedale.mas.AbstractDedaleAgent;
import eu.su.mas.dedaleEtu.mas.agents.dummies.ExploreSoloAgent;
import eu.su.mas.dedaleEtu.mas.knowledge.MapRepresentation;
import eu.su.mas.dedaleEtu.mas.knowledge.MapRepresentation.MapAttribute;
import jade.core.Agent;
import jade.core.behaviours.SimpleBehaviour;

public class PatrolSoloBehaviour extends SimpleBehaviour{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private MapRepresentation myMap;
	private boolean finished = false;
	private List<String> bestPath;
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
			
			System.out.println(this.myAgent.getLocalName() + " : " +lobs);
			
			/**
			 * Just added here to let you see what the agent is doing, otherwise he will be too quick
			 */
			try {
				this.myAgent.doWait(500);
			} catch (Exception e) {
				e.printStackTrace();
			}

			//1) remove the current node from openlist and add it to closedNodes.

			this.myMap.addNode(myPosition,MapAttribute.closed, System.currentTimeMillis());

			//2) calcule objectif et chemin (si besoin)
			if (((ExploreSoloAgent)this.myAgent).getNeedObj()) {
				this.bestPath= this.myMap.bestReward(myPosition);
				System.out.println("New objectif : " + this.bestPath);
				((ExploreSoloAgent)this.myAgent).setNeedObj(false);
				this.step = 0;
			}


			//3) move
			//is agent move is true
			if (((ExploreSoloAgent) this.myAgent).isMoving()){
				//si l'agent n'est pas a destination
				if(this.bestPath.get(this.step) != myPosition){
					System.out.println(this.myAgent.getLocalName() + " my position : " + myPosition + ", je doit aller en : " + this.bestPath.get(this.step));
					((AbstractDedaleAgent)this.myAgent).moveTo(this.bestPath.get(this.step));//agent move 1 node
					//évite que le this.step cree une sortie d'indice de tableau
					if((step + 1) < this.bestPath.size()) {
						this.step++;
					}
				}	
				
				//si apres le mouvement l'agent eest arrivé à destination, alor on calcule une nouvelle destination
				System.out.println("step : " + this.step);
				System.out.println(this.bestPath.get(this.step) + " : " + ((AbstractDedaleAgent)this.myAgent).getCurrentPosition());
				if(this.bestPath.get(this.step).equals(((AbstractDedaleAgent)this.myAgent).getCurrentPosition())){
					System.out.println("demande de new obj");
					((ExploreSoloAgent)this.myAgent).setNeedObj(true);
				}

				
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
					
			}
			else {
				System.out.println("Ho là là, je suis si fatigué!");
			}
		}
	}


	@Override
	public boolean done() {
		return finished;
	}

}
