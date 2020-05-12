package eu.su.mas.dedaleEtu.mas.behaviours;

import java.lang.reflect.Array;
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

public class PatrolSoloBehaviour extends SimpleBehaviour{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private MapRepresentation myMap;
	private boolean finished = false;
	private String bestPath;
	private int step;
	private String posSmelGolem;
	
	public PatrolSoloBehaviour(final ExploreSoloAgent myAgent, MapRepresentation myMap) {
		super(myAgent);
		this.myMap = myMap;
		this.step = 0;
		this.posSmelGolem = "";
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
			List <String> node_sent = new ArrayList<String>();
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
					node_sent.add(ID_node);
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
				
			//puis je marque autour de tous les node qui ne sent pas qu'il ne peux pas y avoir de golem (réduit les posibilité de placement du golem)
			Iterator<Couple<String, List<Couple<Observation, Integer>>>> iter2 = lobs.iterator();
			while(iter2.hasNext()){
				Couple<String, List<Couple<Observation, Integer>>> temp2 = iter2.next();
				List<Couple<Observation, Integer>> couple2 = temp2.getRight();
				String ID_node2 = temp2.getLeft();
				//System.out.println("id, couple");
				//System.out.println(ID_node);
				//System.out.println(couple);
				//si je sens le golem
				if(couple2.size() > 0) {
					//il ne se passe rien
				}
				else {
					//System.out.println("je ne le sens pas");
					//je demande les voisins du node
					List <String> listNode = this.myMap.neighborNode(ID_node2);
					for(int i = 0; i < listNode.size(); i++) {
						this.myMap.setGolemDetection(listNode.get(i), false, myPosition);
					}
				}
			}

			
			this.myMap.setNodeSmell(node_sent);
			//3) si j'ai senti un golem ou que je suis danss une coalition
			if(((ExploreSoloAgent)this.myAgent).isInPursuit()) {
				System.out.println(this.myAgent.getLocalName() + " : Je suis en train de poursuivre le golem : " + lobs);
				List <String> neighborNode = ((ExploreSoloAgent)this.myAgent).getMap().neighborNode(((AbstractDedaleAgent)this.myAgent).getCurrentPosition());//determine les neouds voisins
				neighborNode.add(((AbstractDedaleAgent)this.myAgent).getCurrentPosition());
				boolean move = true;
				int i = 0;
				String nodeMove = "";
				//System.out.println("neighborNode : " + neighborNode + " -- et node_sent : " + node_sent);
				while(move && i < neighborNode.size()) {
					int j = 0;
					while(move && j < node_sent.size()) {
						//System.out.println("neighborNode : " + neighborNode.get(i) + " -- et node_sent : " + node_sent.get(j));
						if(neighborNode.get(i).equals(node_sent.get(j))) {
							//System.out.println("ok je note le node ou aller : " + node_sent.get(j));
							move = false;
							nodeMove = node_sent.get(j);
						}
						j++;
					}
					i++;
				}
				System.out.println(this.myAgent.getLocalName() + " : je suis sur le node " + ((AbstractDedaleAgent)this.myAgent).getCurrentPosition() + " et je move sur le " + nodeMove);
				//si le node trouvé n'est pas null et que le golem a bougé
				if(!nodeMove.equals("") && !nodeMove.equals(this.posSmelGolem)) {
					((AbstractDedaleAgent)this.myAgent).moveTo(nodeMove);//l'agent bouge prés du golem	
					this.posSmelGolem = nodeMove;
				}
				Golem_Present = true;
			}
			if(!((ExploreSoloAgent)this.myAgent).isInPursuit()) {
				if(Golem_Present && !((ExploreSoloAgent)this.myAgent).getWaitEnterCoalition()) {
					if (!((ExploreSoloAgent)this.myAgent).isInPursuit()) { // si je ne suis pas déjà entrain de poursuivre un golem
						//JE LANCE LA PROCEDURE DE COALITION + ATTRAPAGE DE GOLEM MOUHAHAHAHAHA
						System.out.println("GOLEM - " + this.myAgent.getLocalName());
						((ExploreSoloAgent)this.myAgent).setInPursuit(true);// je passe en mode poursuite
						((ExploreSoloAgent)this.myAgent).setLeaderCoalition(true);//je deviens leader de ma coalition
						String id_Behaviour = ((ExploreSoloAgent)this.myAgent).idBehaviourCreation();//je crée un identifiant de coalition
						this.myAgent.addBehaviour(new CoalitionBehaviour(((ExploreSoloAgent)this.myAgent), id_Behaviour));//je lance le behaviour de coalition
						this.myAgent.addBehaviour(new SayGolem(((ExploreSoloAgent)this.myAgent), id_Behaviour));
						((ExploreSoloAgent)this.myAgent).setMoving(false);	
						
						List <String> neighborNode = ((ExploreSoloAgent)this.myAgent).getMap().neighborNode(((AbstractDedaleAgent)this.myAgent).getCurrentPosition());//determine les neouds voisins
						neighborNode.add(((AbstractDedaleAgent)this.myAgent).getCurrentPosition());
						boolean move = true;
						int i = 0;
						String nodeMove = "";
						//System.out.println("neighborNode : " + neighborNode + " -- et node_sent : " + node_sent);
						while(move && i < neighborNode.size()) {
							int j = 0;
							while(move && j < node_sent.size()) {
								//System.out.println("neighborNode : " + neighborNode.get(i) + " -- et node_sent : " + node_sent.get(j));
								if(neighborNode.get(i).equals(node_sent.get(j))) {
									//System.out.println("ok je note le node ou aller : " + node_sent.get(j));
									move = false;
									nodeMove = node_sent.get(j);
								}
								j++;
							}
							i++;
						}
						System.out.println(this.myAgent.getLocalName() + " : je suis sur le node " + ((AbstractDedaleAgent)this.myAgent).getCurrentPosition() + " et je move sur le " + nodeMove);
						((AbstractDedaleAgent)this.myAgent).moveTo(nodeMove);//l'agent bouge prés du golem	
						this.posSmelGolem = nodeMove;
						this.finished = true;
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
					if (((ExploreSoloAgent) this.myAgent).isMoving() && !((ExploreSoloAgent)this.myAgent).isInPursuit()){
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
	}


	@Override
	public boolean done() {
		return finished;
	}
	
	

}
