package eu.su.mas.dedaleEtu.mas.behaviours;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Iterator;
import java.util.Map.Entry;

import dataStructures.serializableGraph.SerializableSimpleGraph;
import eu.su.mas.dedale.mas.AbstractDedaleAgent;
import eu.su.mas.dedaleEtu.mas.agents.dummies.ExploreSoloAgent;
import eu.su.mas.dedaleEtu.mas.knowledge.MapRepresentation;
import eu.su.mas.dedaleEtu.mas.knowledge.MapRepresentation.MapAttribute;
import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.SimpleBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.lang.acl.UnreadableException;

public class CoalitionBehaviour extends SimpleBehaviour{
	
	private static final long serialVersionUID = 2L;
	private boolean finished;
	private boolean log = true;
	
	private static final int wait = 2000;
	private String id_Coal;//id de la coalition (utilisé pour les echange de message)
	
	//Liste des membre de la coalition avec leurs role, nom et position
	//"Name agent" : ["Role","Position"]
	private HashMap<String, List <String>> members = new HashMap <String, List <String>> ();//(dans update)
	//nombre max d'agent possible dans la coalition
	private int maxAgent;//(dans update)
	//signale si la coalition est pleine
	private boolean candidatAgentOpen;// (dans update)
	//Donne la position la plus probable du golem
	private String golemLocalisation;//(dans update)
	//Sert de numérotation pour indiquer l'ordre des message pour les ordres de la coalition (demande de données et ordre de placement)
	private int stepMSG;
	//Sert dee numérotation au update envoyer
	private int numUpdate;//(dans update)
	//quel est mon pére? (pas uttiliser pour le moment)
	private String fatherLastMSG;
	//quel sont mes fils (pas utiliser pour le moment)
	private List<String> sonLastMSG = new ArrayList<String>();
	//temps d'attente max avant de sortir de la coalition si aucun nouveaux messages
	private long timerWaitMessageCoalition;
	//variable a true tant que tous les agents de la coalition non pas envoyé leur données sur la vision du golem (utiliser par le leader)
	private boolean waitDataGolemAllAgent;
	//déclanche la chasse au golem
	private boolean huntGolem;
	//déclanche la fase de calcule pour attraper le golem
	private boolean huntGolemProcess;
	//stocke les valeur de position probable du golem envoyer par chaque agent (stack les valeurs de même position) (utiliser par le leader)
	private HashMap<String, Double> dataPositionGolem;
	//list des agents dont on attend la réponse des données sur le golem (utiliser par le leader)
	private List<String> waitAgentDataReturn = new ArrayList <String> ();
	//Position données a chaque agent pour attraper le golem (utiliser par le leader)
	private HashMap <String, String> positionAgent;
	//hashmap de boolean pour signaler si l'agent est arrivé à la position demander pour attraper le golem (utiliser par le leader)
	private HashMap <String, Boolean> verificationPositionAgent = new HashMap <String, Boolean> ();
	//position ou l'agent doit aller (propre a chaque agent)
	private String goPosition;
	//Signifie si le golem est attraper ou non
	private Boolean golemCatch;//(dans update)
	//Si la coalition est active
	private Boolean activeCoalition;
	
	private long timer;
	
	public CoalitionBehaviour(final Agent myagent, String id) {
		super(myagent);

		this.id_Coal = id;

		if(((ExploreSoloAgent)this.myAgent).getLeaderCoalition()) {
			List <String> dataMyAgent = new ArrayList<String>();
			dataMyAgent.add("Leader");
			dataMyAgent.add(((AbstractDedaleAgent)this.myAgent).getCurrentPosition());
			this.members.put(this.myAgent.getLocalName(), dataMyAgent);
			this.candidatAgentOpen = true;
			this.stepMSG = 1;
			//commencer à remplir dataPositionGolem
			this.dataPositionGolem = ((ExploreSoloAgent)this.myAgent).getMap().listNodeGolemProba();
		}
		else {
			this.stepMSG = 0;
		}
		
		this.numUpdate = 0;
		this.timerWaitMessageCoalition = 10000;
		this.waitDataGolemAllAgent = true;
		this.huntGolem = false;
		this.golemCatch = false;
		this.activeCoalition = true;
		this.huntGolemProcess = false;
		
		((ExploreSoloAgent) this.myAgent).setMoving(false);//je ne bouge que sur ordre de la coalition
		
		// Nombre agent max dans coalition
		this.maxAgent = ((ExploreSoloAgent)this.myAgent).getMap().getMaxDegree(); // degré max du graphe
		//this.maxAgent = ((ExploreSoloAgent)this.myAgent).getMap().getAvDegree(); // degré moyen du graphe
		//this.maxAgent = 2; //pour test
		
		
	}
	
	@Override
	public void action() {
		//si la coalition est active
		if(this.activeCoalition) {
			System.out.println(this.myAgent.getLocalName() + " : Behaviour de coalition " + this.id_Coal + " actif");

			//Template message neutre, utilisé actuellement pour:
			//Les demande et reponse pour entré dans la coalition
			final MessageTemplate msgTemplate = MessageTemplate.and(MessageTemplate.MatchPerformative(ACLMessage.INFORM), 
					MessageTemplate.MatchProtocol(this.id_Coal));			
			final ACLMessage msg = this.myAgent.receive(msgTemplate);

			//Template de reception des demandes pour entré dans la coalition
			final MessageTemplate msgTemplateEnterCoalition = MessageTemplate.and(MessageTemplate.MatchPerformative(ACLMessage.INFORM), 
					MessageTemplate.MatchProtocol("Request entry coalition"));			
			final ACLMessage msgEnterCoalition = this.myAgent.receive(msgTemplateEnterCoalition);

			//Template message concernant la position probable du golem vu par la coalition utilisé actuellement pour:
			//demander et repondre à la question "Où est le golem?"
			final MessageTemplate msgTemplateGolemPosition = MessageTemplate.and(MessageTemplate.MatchPerformative(ACLMessage.INFORM), 
					MessageTemplate.MatchProtocol(this.id_Coal + ": golem position"));			
			final ACLMessage msgGolemPosition = this.myAgent.receive(msgTemplateGolemPosition);		

			//Template message concernant le golem utilisé actuellement pour:
			//leader : demande au autre agent si il sente aussi les golem et où
			//leader : envoie a chaque agent ou ils doivent se placer
			//autre : répondre au leader
			final MessageTemplate msgTemplateGolemHuntStrat = MessageTemplate.and(MessageTemplate.MatchPerformative(ACLMessage.INFORM), 
					MessageTemplate.MatchProtocol(this.id_Coal + ": golem hunt strat"));			
			final ACLMessage msgGolemHuntStrat = this.myAgent.receive(msgTemplateGolemHuntStrat);

			//Template message nombre d'agent dans la coalition, utilisé actuellement pour:
			// demander et répondre à la question "Nb d'agent  dans la coalition?"
			final MessageTemplate msgTemplatenbAgent = MessageTemplate.and(MessageTemplate.MatchPerformative(ACLMessage.INFORM), 
					MessageTemplate.MatchProtocol(this.id_Coal + ": nb Agent"));			
			final ACLMessage msgnbAgent = this.myAgent.receive(msgTemplatenbAgent);	

			//Template message pour l'update des données du behaviour de la coalition, utilisé actuellement pour:
			//leader: envoyé les données updaté du behaviour à l'ensemble de la coalition
			final MessageTemplate msgTemplateUpdate = MessageTemplate.and(MessageTemplate.MatchPerformative(ACLMessage.INFORM), 
					MessageTemplate.MatchProtocol(this.id_Coal + ": Update behaviour"));			
			final ACLMessage msgUpdate = this.myAgent.receive(msgTemplateUpdate);	

			//Template message pour stoper le recrutement dans la coalition, utilisé actuellement pour:
			//leader: envoyé à l'ensemble de la coalition d'arréter le recrutement
			final MessageTemplate msgTemplateCoalitionFull = MessageTemplate.and(MessageTemplate.MatchPerformative(ACLMessage.INFORM), 
					MessageTemplate.MatchProtocol(this.id_Coal + ": Coalition full"));			
			final ACLMessage msgCoalitionFull = this.myAgent.receive(msgTemplateCoalitionFull);

			//Template message pour dire que je suis sur la position définie par la leader
			final MessageTemplate msgTemplateInPosition = MessageTemplate.and(MessageTemplate.MatchPerformative(ACLMessage.INFORM), 
					MessageTemplate.MatchProtocol(this.id_Coal + ": In position"));			
			final ACLMessage msgInPosition = this.myAgent.receive(msgTemplateInPosition);
			
			//Template message pour dire que je dois dissoudre ma coalition et transférer tous mes agents dans une autre coalition
			final MessageTemplate msgTemplateDissolveAndChangeCoal = MessageTemplate.and(MessageTemplate.MatchPerformative(ACLMessage.INFORM), 
					MessageTemplate.MatchProtocol(this.id_Coal + ": dissolve and go to new coalition"));			
			final ACLMessage msgDissolveAndChangeCoal = this.myAgent.receive(msgTemplateDissolveAndChangeCoal);
			
			//Template message pour dire que je dois transférer un certain nombre de mes agents dans une autre coalition
			final MessageTemplate msgTemplateChangeCoal = MessageTemplate.and(MessageTemplate.MatchPerformative(ACLMessage.INFORM), 
					MessageTemplate.MatchProtocol(this.id_Coal + ": change coalition"));			
			final ACLMessage msgChangeCoal = this.myAgent.receive(msgTemplateChangeCoal);
			
			

			// partie réservé au leader
			if (((ExploreSoloAgent)this.myAgent).getLeaderCoalition()){
				//de nouveau agent peuvent encore venir dans la coalition
				if(this.candidatAgentOpen) {
					if(msgEnterCoalition != null && !msgEnterCoalition.getSender().getLocalName().equals(this.myAgent.getAID().getLocalName())) {									
						if(log) {
							System.out.println("Reception d'une demande d'entré dans la coalition");
						}
						//reception d'une demande d'entré dans la coalition
						this.addAgentCoalition(msgEnterCoalition);
					}			
					//reception d'une demande de localisation de golem
					if (msgGolemPosition != null && !msgGolemPosition.getSender().getLocalName().equals(this.myAgent.getAID().getLocalName())) {
						if(log) {
							System.out.println("Reception d'une demande de localisation du golem");
						}
						this.golemPosition(msg);
					}			
					//reception d'une demande de nombre d'agent
					if (msgnbAgent != null && !msgnbAgent.getSender().getLocalName().equals(this.myAgent.getAID().getLocalName())) {
						if(log) {
							System.out.println("Reception d'une demande du nombre d'agent dans la coalition");
						}
						this.nbAgentCoalition(msg);
					}			
					//si la coalition est pleine
					if (this.members.size() >= this.maxAgent) {
						if(log) {
							System.out.println("La coalition est pleine.");
						}
						//on ferme le droit d'inscription
						this.candidatAgentOpen = false;
						((ExploreSoloAgent)this.myAgent).setInCoalitionFull(true);
						//dit aux autre agents d'arréter le recrutement
						this.sendCoalitionFull();
						//Demande à tous les agents de la coalition si ils voient un golem actuellement et récup data
						this.giveDataGolem();
						this.waitDataGolemAllAgent = true;
					}
					//réception d'une demande de dissolution et changement de coalition
					if (msgDissolveAndChangeCoal != null) {
						this.dissolveAndChangeCoal(msgDissolveAndChangeCoal);
						if (log) {
							System.out.println("Reception d'une demande de dissolution de la coalition et de migration vers une autre");
						}
					}
					//réception d'une demande de changement de coalition
					if (msgChangeCoal != null) {
						this.changeCoal(msgChangeCoal);
						if (log) {
							System.out.println("Reception d'une demande de migration de quelques agents vers une autre coalition");
						}
					}
					
				}//si pas de nouveau possible dans la coalition on passe en chasse
				else {
					if(msgGolemHuntStrat != null) {
						//si l'on attend des data d'agent concernant la position du golem
						if(waitDataGolemAllAgent) {
							//traitement des messages reçues
							HashMap<String, String> data;
							try {
								data = (HashMap<String, String>) msgGolemHuntStrat.getContentObject();
							} catch (UnreadableException e) {
								System.out.println("Problem msgGolemHuntStrat.getContentObject(), no format HashMap <String, String>");
								e.printStackTrace();
								data = new HashMap <String, String> ();
								data.put("data", "-1");
							}
							//si il y a des donnée valable pour la chasse au golem
							if(data.get("data").equals("2")){
								//faire une boucle sur les key pour récupéré les double (sous forme de string) a intégré a chaque noeud
								for(String nodeID : data.keySet()) {
									//permet de prendre uniquement les id node
									if(nodeID != "data" && nodeID != "agent") {
										if(log) {
											System.out.println("Le leader regarde les données de localisation de golem envoyer par un agent : " + nodeID + " , " + data.get(nodeID));
										}
										this.addValueDataPositionGolem(nodeID, Double.parseDouble(data.get(nodeID)));
									}
								}
							}
							//retirer l'agent de la liste d'attente de retour de donnée
							this.waitAgentDataReturn.remove(data.get("agent"));

							//si la liste est vide alors plus de données en attente de reception
							if(this.waitAgentDataReturn.isEmpty()) {
								this.waitDataGolemAllAgent = false;
							}
							else {
								//Demande à tous les agents de la coalition si ils voient un golem actuellement et récup data
								this.giveDataGolem();
							}
						}
					}
					//si plus de message en attente on calcule ou peut se trouver le golem
					if(!waitDataGolemAllAgent) {
						if(log) {
							System.out.println("il n'y a plus de donnée de localisation de golem à attendre");
						}
						this.huntGolemProcess = true;
						this.huntGolem = true;
					}
					if(huntGolemProcess) {
						//definir ou se trouve le golem et mettre a jour
						this.golemLocalisation = this.bestPositionGolem();
						//envoie de l'update du behaviour
						this.sendUpdateDataBehaviour();
						//calcule de la postion a donnée a chaque agent
						this.calculPositionCaptureGolem();
						if(log) {
							System.out.println("go position leader : " + this.goPosition);
						}
						//envoie de la position que doit avoir les agents pour capturer le golem
						this.sendPositionAgentHuntGolem();
						this.huntGolemProcess = false;
						if(!this.goPosition.equals(((AbstractDedaleAgent)this.myAgent).getCurrentPosition())){
							this.followPathNewPosition(this.goPosition);
						}
					}
					//je recoit des message d'agent qu'ils disent qu'il sont en position
					if(msgInPosition != null && !this.golemCatch) {
						this.sendAgentVerifPosition(msgInPosition);				
						}
				}
				if(this.golemCatch) {
					System.out.println("Golem en " + this.golemLocalisation + " attrapé!!");
				}
			}//non leader
			else {
				//je recoit une update des données du behaviour (penser a sup say golem si candidatAgentOpen=false)
				if(msgUpdate != null) {
					this.receivedUpdateDataBehaviour(msgUpdate);
				}
				//je reçoit un message du leader qui me donne un position pour choper le golem ou me demande ou il est
				if(msgGolemHuntStrat != null) {
					//demande ou il est
					this.receiveRequestGolemPosition(msgGolemHuntStrat);
					//me dit ou aller
					this.receivePositionCatchGolem(msgGolemHuntStrat);		
				}
				//réception d'une demande de dissolution et changement de coalition
				if (msgDissolveAndChangeCoal != null) {
					this.dissolveAndChangeCoal(msgDissolveAndChangeCoal);
					if (log) {
						System.out.println("Reception d'une demande de dissolution de la coalition et de migration vers une autre");
					}
				}
				//réception d'une demande de changement de coalition
				if (msgChangeCoal != null) {
					this.changeCoal(msgChangeCoal);
					if (log) {
						System.out.println("Reception d'une demande de migration de quelques agents vers une autre coalition");
					}
				}
			}
			
			//si je suis arriver sur la position que l'on m'a attribuer
			if(this.huntGolem && goPosition.equals(((AbstractDedaleAgent)this.myAgent).getCurrentPosition())) {
				//envoyer a tous le monde que l'agent est sur position
				this.sendMessageStringCoalition(this.id_Coal + ": In position", this.members, ((AbstractDedaleAgent)this.myAgent).getCurrentPosition());
				((ExploreSoloAgent) this.myAgent).setMoving(false);
				if(log) {
					System.out.println(this.myAgent.getAID().getLocalName() + " : Je suis arrivé sur ma position - " + ((AbstractDedaleAgent)this.myAgent).getCurrentPosition());
				}
			}


			//Autre agent et leader	
			//TODO
			//je recoit un message de mon fils de coalition		
			//je recoit un message une demande de rentré dans la coalition (a transmetre à mon leader/pére)
			//je recoit de mon fils les données ou il sent le golem (transmetre au leader/pére)
			// recoit une demande d'entré dans la coalition, l'envoie au leader
		}
	}


	//ajout d'un agent dans la coalition
	private  void addAgentCoalition(ACLMessage msg) {
		ACLMessage msgRespond = new ACLMessage(ACLMessage.INFORM);
		msgRespond.setSender(this.myAgent.getAID());
		msgRespond.setProtocol("AnswerEntry");
		//si la coalition n'est pas encore pleine
		if(this.members.size() < this.maxAgent) {
			if (log) {
				System.out.println("envoi message coalition ok");
			}
			try {
				List<String> dataAgent = new ArrayList <String> ();
				dataAgent.add("agent");
				dataAgent.add(((List<String>) msg.getContentObject()).get(0));
				this.members.put(msg.getSender().getLocalName(), dataAgent);
				msgRespond.setContent("ok");
			} catch (UnreadableException e) {
				System.out.println("Problem reception agent data for entry coalition");
				msgRespond.setContent("no");
				e.printStackTrace();
			}
		}//sinon si pleine
		else {
			msgRespond.setContent("no");
			if(log) {
				System.out.println("envoi message coalition non");
			}
		}
		msgRespond.addReceiver(new AID(msg.getSender().getLocalName(),AID.ISLOCALNAME));
		((AbstractDedaleAgent)this.myAgent).sendMessage(msgRespond);	
	}	
	
	//envoie du nombre d'agent dans la coalition
	private void nbAgentCoalition(ACLMessage msg) {
		ACLMessage msgRespond = new ACLMessage(ACLMessage.INFORM);
		msgRespond.setSender(this.myAgent.getAID());
		msgRespond.setProtocol(this.id_Coal + ": nb Agent");
		msgRespond.setContent("" + this.members.size());
		msgRespond.addReceiver(new AID(msg.getSender().getLocalName(),AID.ISLOCALNAME));
		((AbstractDedaleAgent)this.myAgent).sendMessage(msgRespond);
	}
	
	//envoi de la position probablee du golem suivis par la coalition
	private void golemPosition(ACLMessage msg) {
		ACLMessage msgRespond = new ACLMessage(ACLMessage.INFORM);
		msgRespond.setSender(this.myAgent.getAID());
		msgRespond.setProtocol(this.id_Coal + ": golem position");
		msgRespond.setContent(this.golemLocalisation);
		msgRespond.addReceiver(new AID(msg.getSender().getLocalName(),AID.ISLOCALNAME));
		((AbstractDedaleAgent)this.myAgent).sendMessage(msgRespond);
	}
	
	//envoie message string a une liste spécifique d'agent
	private void sendMessageStringCoalition(String protocol, HashMap <String, List <String>> agents, String content) {
		ACLMessage msgSend = new ACLMessage(ACLMessage.INFORM);
		msgSend.setSender(this.myAgent.getAID());
		msgSend.setProtocol(protocol);
		msgSend.setContent(content);		
		for(String agent : agents.keySet()) {
			msgSend.addReceiver(new AID(agent, AID.ISLOCALNAME));
		}		
		((AbstractDedaleAgent)this.myAgent).sendMessage(msgSend);		
	}
	
	//envoie message object a une liste spécifique d'agent
	private void sendMessageObjectCoalition(String protocol, HashMap <String, List <String>> agents, Object content) {
		ACLMessage msgSend = new ACLMessage(ACLMessage.INFORM);
		msgSend.setSender(this.myAgent.getAID());
		msgSend.setProtocol(protocol);
		try {
			msgSend.setContentObject((Serializable) content);
		} catch (IOException e) {
			msgSend.setContent("-1");
			System.out.println("Send message problem");
			e.printStackTrace();
		}		
		for(String agent : agents.keySet()) {
			msgSend.addReceiver(new AID(agent, AID.ISLOCALNAME));
		}		
		((AbstractDedaleAgent)this.myAgent).sendMessage(msgSend);		
	}
	
	//permet de définir ou mettre les agents pour capturer le golem
	private void calculPositionCaptureGolem() {
		// definir les voisins du noeud ou on pense ou se trouve le golem
		List <String> neighborNodeGolem = ((ExploreSoloAgent)this.myAgent).getMap().neighborNode(golemLocalisation);
		//repartir les noeuds entre les agents de la liste en définissant le chemin le plus court pour chacun
		//copy de la list des agent pour éviter de donnée 2 noeud à un même agent
		List <String> copyMembers = new ArrayList <String> ();
		for(String agent : this.members.keySet()) {
			copyMembers.add(agent);
		}	
		this.positionAgent = new HashMap <String, String> ();
		//pour chaque noeud
		for(int i = 0; i < neighborNodeGolem.size(); i++ ) {
			//regarder quel agent est le plus proche
			String bestAgent = "";
			int distancePath = 100000;
				//tester si liste vide
			if(!copyMembers.isEmpty()) {
				List <String> path;
				for(int j= 0; j < copyMembers.size(); j++) {
					path = ((ExploreSoloAgent)this.myAgent).getMap().getShortestPath(this.members.get(copyMembers.get(j)).get(1),neighborNodeGolem.get(i));//definit le meilleur chemin entre la  position de l'agent et le noeud à aller
					if(path.size() < distancePath) {
						distancePath = path.size();
						bestAgent = copyMembers.get(j);
					}
				}
				copyMembers.remove(bestAgent);
				if(bestAgent.equals(this.myAgent.getLocalName())) {
					this.goPosition = neighborNodeGolem.get(i);
				}
				else {
					this.positionAgent.put(bestAgent, neighborNodeGolem.get(i));
					this.verificationPositionAgent.put(bestAgent, false);
				}
			}		
		}	
	}
	
	//envoie de la position au agent de la coalition
	private void sendPositionAgentHuntGolem() {
		//on ajout le numéro de message
		this.positionAgent.put("Step", "" + this.stepMSG);
		this.positionAgent.put("data","3");
		//on envoie le message
		this.sendMessageObjectCoalition(this.id_Coal + ": golem hunt strat", this.members, this.positionAgent);
		//on incremente l'étape de chasse
		this.stepMSG++;
		if(log) {
			System.out.println("envoie leur position au agent : " + this.positionAgent);
			System.out.println("Le leader se positionne en : " + this.goPosition);
			System.out.println("Le golem est en : " + this.golemLocalisation);
		}
	}
	
	//determine quelle node a la meilleur "proba" de contenir le golem
	private String bestPositionGolem() {
		String bestNode = "";
		Double proba = 0.0;
		for(String nodeID : this.dataPositionGolem.keySet()) {
			if(this.dataPositionGolem.get(nodeID) > proba) {
				proba = this.dataPositionGolem.get(nodeID);
				bestNode = nodeID;
			}
		}
		return bestNode;
	}
	
	//ajoute un noeud et une valeur dans dataPositionGolem
	private void addValueDataPositionGolem(String Id_node, Double value) {
		//si le noeud existe déjà dans les données
		if(this.dataPositionGolem.containsKey(Id_node)) {
			this.dataPositionGolem.put(Id_node, this.dataPositionGolem.get(Id_node) + value);
		}//sinon l'ajouter
		else {
			this.dataPositionGolem.put(Id_node, value);
		}
	}
	
	//envoie la liste des noeud ou l'agent pense trouver le golem et les proba associer
	private void respondDataProbaGolem(ACLMessage msg) {
		
		//demande la HashMap des données
		HashMap <String, Double> data_golem = ((ExploreSoloAgent)this.myAgent).getMap().listNodeGolemProba();
		
		HashMap <String, String> data = new HashMap <String, String> ();
		//si data_golem cotient des données convertie les doubles en String
		if(data_golem != null) {
			for(String nodeID : data_golem.keySet()) {
				data.put(nodeID,data_golem.get(nodeID).toString());
			}
			data.put("data", "1");
		}
		else {
			data.put("data", "-1");
		}
		data.put("agent", this.myAgent.getLocalName());
		data.put("position", ((AbstractDedaleAgent)this.myAgent).getCurrentPosition());
		
		//envoie des données
		ACLMessage msgSend = new ACLMessage(ACLMessage.INFORM);
		msgSend.setSender(this.myAgent.getAID());
		msgSend.setProtocol(this.id_Coal + ": golem hunt strat");
		try {
			msgSend.setContentObject((Serializable) data);
		} catch (IOException e) {
			msgSend.setContent("-1");
			System.out.println("Send message respondDataProbaGolem problem");
			e.printStackTrace();
		}		
		msgSend.addReceiver(new AID(msg.getSender().getLocalName(),AID.ISLOCALNAME));	
		((AbstractDedaleAgent)this.myAgent).sendMessage(msgSend);
	}
	
	//envoie la mise a jours des données du behaviour
	private void sendUpdateDataBehaviour() {
		HashMap<String, List <String>> update = new HashMap<String, List <String>>();
		
		List<String> listAgent = new ArrayList<String>();
		for(String agent : this.members.keySet()) {
			listAgent.add(agent);
		}
		update.put("listAgent", listAgent);
		
		List<String> updatemaxAgent = new ArrayList<String>();
		updatemaxAgent.add(""+this.maxAgent);
		update.put("maxAgent", updatemaxAgent);
		
		List<String> updatecandidatAgentOpen = new ArrayList<String>();
		updatecandidatAgentOpen.add(""+this.candidatAgentOpen);
		update.put("candidatAgentOpen", updatecandidatAgentOpen);

		List<String> updategolemLocalisation = new ArrayList<String>();
		updategolemLocalisation.add(""+this.golemLocalisation);
		update.put("golemLocalisation", updategolemLocalisation);
		
		this.numUpdate ++;
		List<String> updatenumUpdate = new ArrayList<String>();
		updatenumUpdate.add(""+this.numUpdate);
		update.put("numUpdate", updatenumUpdate);
		
		List<String> golemCathing = new ArrayList<String>();
		golemCathing.add(""+this.golemCatch);
		update.put("golemCatch", golemCathing);
		
		if(log) {
			System.out.println("envoie update : " + update);
		}
		
		this.sendMessageObjectCoalition(this.id_Coal + ": Update behaviour", this.members, update);
	}
	
	//reception dee la mise a jours est envoie en ping pour si agent de coalition non a porter du leader
	private void receivedUpdateDataBehaviour(ACLMessage msg) {
		HashMap<String, List <String>> update;
		try {
			update = (HashMap<String, List<String>>) msg.getContentObject();
		} catch (UnreadableException e) {
			System.out.println("Problem reception update");
			update = new HashMap<String, List <String>> ();
			List <String> temp = new ArrayList <String> ();
			temp.add("-1");
			update.put("numUpdate", temp);
			e.printStackTrace();
		}
		
		if(log) {
			System.out.println("Reception de l'udpdate : ");
			System.out.println("Num : " + update.get("numUpdate").get(0));
			System.out.println("listAgent : " + update.get("listAgent"));
			System.out.println("maxAgent : " + update.get("maxAgent").get(0));
			System.out.println("candidatAgentOpen : " + update.get("candidatAgentOpen").get(0));
			System.out.println("golemLocalisation : " + update.get("golemLocalisation").get(0));
			System.out.println("golemCatch : " + update.get("golemCatch").get(0));
		}
		
		//si le numéro d'update est supérieur au dernier update
		if(Integer.parseInt(update.get("numUpdate").get(0)) > this.numUpdate) {
			//envoie du message pour agent plus loin (ping)
			this.sendMessageObjectCoalition(msg.getProtocol(), this.members, update);
			
			//MAJ DATA
			this.maxAgent = Integer.parseInt(update.get("maxAgent").get(0));
			this.candidatAgentOpen = Boolean.parseBoolean(update.get("candidatAgentOpen").get(0));
			this.golemLocalisation = update.get("golemLocalisation").get(0);
			this.numUpdate = Integer.parseInt(update.get("numUpdate").get(0));
			this.golemCatch = Boolean.parseBoolean(update.get("golemCatch").get(0));
			
			this.members = new HashMap <String, List <String>> ();
			for(int i = 0; i < update.get("listAgent").size(); i++) {
				List <String> temp = new ArrayList <String> ();
				this.members.put(update.get("listAgent").get(i), temp);
			}
			
		}
	}
	
	private void receiveRequestGolemPosition(ACLMessage msg) {
		HashMap <String, String> request;
		try {
			request =(HashMap<String, String>) msg.getContentObject();
		} catch (UnreadableException e) {
			request = new HashMap <String, String> ();
			e.printStackTrace();
		}		
		
		if(log) {
			System.out.println("Demande de donnée pour la localisation du golem :");
			System.out.println("Step :" + request.get("Step"));
			System.out.println("Data :" + request.get("data"));
		}
		
		if((Integer.parseInt(request.get("Step")) > this.stepMSG) && (Integer.parseInt(request.get("data")) == 1)) {
			//brodcast le message
			this.sendMessageObjectCoalition(msg.getProtocol(), this.members, request);
			//mettre a jour le step
			this.stepMSG = Integer.parseInt(request.get("Step"));
			//calcule de la position golem à envoyé
			HashMap<String, Double> data_Position_Golem = ((ExploreSoloAgent)this.myAgent).getMap().listNodeGolemProba();
			//envoie vers le mec qui ma envoyer le message
			HashMap<String, String> data_Position_Golem_send = new HashMap<String, String> ();
			if(!data_Position_Golem.isEmpty()) {
				for(String node : data_Position_Golem.keySet()) {
					data_Position_Golem_send.put(node, "" + data_Position_Golem.get(node));
				}		
				if(log) {
					System.out.println("Demande de donnée pour la localisation du golem : data à envoyé : " + data_Position_Golem_send);
				}
			}			
			data_Position_Golem_send.put("data", "2");
			data_Position_Golem_send.put("agent", this.myAgent.getLocalName());
			if(log) {
				System.out.println("Demande de donnée pour la localisation du golem : envoie : " + data_Position_Golem_send);
			}
			this.sendMessageObjectCoalition(msg.getProtocol(), this.members, data_Position_Golem);
		}
		
	}
	
	private void receivePositionCatchGolem(ACLMessage msg) {
		HashMap <String, String> allPosition;
		try {
			allPosition =(HashMap<String, String>) msg.getContentObject();
		} catch (UnreadableException e) {
			allPosition = new HashMap <String, String> ();
			allPosition.put("Step", "0");
			allPosition.put("data", "-1");
			e.printStackTrace();
		}
		if(log) {
			System.out.println(this.myAgent.getAID().getLocalName() + " : Reception position pour attraper le golem :" + allPosition);
		}
		//si nouvelle étape de positionnement
		if((Integer.parseInt(allPosition.get("Step")) > this.stepMSG) && (Integer.parseInt(allPosition.get("data")) == 3)) {
			//envoie du message pour agent plus loin (ping)
			this.sendMessageObjectCoalition(msg.getProtocol(), this.members, allPosition);
			//je regarde ou me positionner
			this.goPosition = allPosition.get(this.myAgent.getLocalName());
			//demarrer la chasse
			this.huntGolem = true;
			//je lance le calcule du chemin pour la nouvelle position et mis rend
			if(!this.goPosition.equals(((AbstractDedaleAgent)this.myAgent).getCurrentPosition())){
				this.followPathNewPosition(this.goPosition);
			}
			//mettre a jour le step
			this.stepMSG = Integer.parseInt(allPosition.get("Step"));
			
			if(log) {
				System.out.println("Reception position pour attraper le golem : nouvelle posittion : " + this.goPosition);
			}
		}
	}
	
	//verifie que tous les agent sont en place a chaque message recu par un agent
	private void sendAgentVerifPosition(ACLMessage msg) {
		String agent = msg.getSender().getLocalName();
		//se n'est pas moi et si je n'avait pas encore valider le positionnement de l'agent
		if(!this.myAgent.getAID().getLocalName().equals(agent) && !this.verificationPositionAgent.get(agent)) {
			//je vérifie que c'est bien la bonne position
			if(this.positionAgent.get(agent).equals(msg.getContent())) {
				this.verificationPositionAgent.remove(agent);
				this.verificationPositionAgent.put(agent, true);
			}
		}
		
		this.golemCatch = true;
		for(String verifAgent : this.verificationPositionAgent.keySet()) {
			if(!this.verificationPositionAgent.get(verifAgent)) {
				this.golemCatch = false;
			}
		}
		
		if(this.golemCatch) {
			System.out.println("Golem en " + this.golemLocalisation + " attrapé!!");
			//envoie de l'update du behaviour
			this.sendUpdateDataBehaviour();
		}
	}
	
	private void followPathNewPosition(String node) {
		// en établie le meilleur chemin
		List <String> path = ((ExploreSoloAgent)this.myAgent).getMap().getShortestPath(((AbstractDedaleAgent)this.myAgent).getCurrentPosition() , node);	
		if(log) {
			System.out.println(this.myAgent.getLocalName() + " : ma position : " + ((AbstractDedaleAgent)this.myAgent).getCurrentPosition());
			System.out.println(this.myAgent.getLocalName() + " : chemin pour aller coincer le golem : " + path);
		}
		String posTemp = ((AbstractDedaleAgent)this.myAgent).getCurrentPosition();
		while(((AbstractDedaleAgent)this.myAgent).getCurrentPosition().equals(posTemp)) {
			((AbstractDedaleAgent)this.myAgent).moveTo(path.get(0));
		}
		//on fait bouger l'agent jusqu'à là bas
		for(int i = 1; i < path.size(); i++) {
			//si la position temp n'est pas égal a la position de l'agent alors cela signifie qu'il c'est déplacer et peux donc avant de nouveau
			if(!((AbstractDedaleAgent)this.myAgent).getCurrentPosition().equals(posTemp)){
				posTemp = ((AbstractDedaleAgent)this.myAgent).getCurrentPosition();
				if(log) {
					System.out.println(this.myAgent.getLocalName() + " : ma position : " + ((AbstractDedaleAgent)this.myAgent).getCurrentPosition());
					System.out.println(this.myAgent.getLocalName() + " : je doitt aller en  : " + (path.get(i)));
				}
				((AbstractDedaleAgent)this.myAgent).moveTo(path.get(i));
			}
		}
	}
	
	//demande aux autre agents ou ils sentent le golem
	private void giveDataGolem() {
		HashMap <String, String> msg = new HashMap <String, String> ();
		msg.put("data", "1");
		msg.put("msg", "Give your data golem!!!!");
		msg.put("Step", "" + this.stepMSG);
		this.sendMessageObjectCoalition(this.id_Coal + ": golem hunt strat", this.members, msg);
		this.stepMSG ++;
	}
	
	//dit aux autre agents d'arréter le recrutement
	private void sendCoalitionFull() {
		this.sendMessageStringCoalition(this.id_Coal + ": Coalition full", this.members, "stop SayGolem");
	}
	
	private void dissolveAndChangeCoal(ACLMessage msg) {
		// Le message contient un string de l'AID du leader de la coalition à rejoindre
		AID otherCoalLeader = new AID(msg.getContent(), AID.ISLOCALNAME);
		
		// TODO: Est-ce qu'il est nécessaire de faire un sendCoalitionFull() d'abord ?
		
		// Si je suis le leader
		if (((ExploreSoloAgent)this.myAgent).getLeaderCoalition()) {
		
			// 1) Je préviens tous les agents de la coalition qu'ils doivent en changer
			Iterator<Entry<String, List<String>>> it = members.entrySet().iterator();
		    while(it.hasNext()) {
		    	Entry<String, List<String>> e = it.next();
		    	
		    	// on ne prévient pas le leader puisque c'est moi
		    	if (!(e.getValue().get(0).equals("0"))) {
			    	ACLMessage msgSend = new ACLMessage(ACLMessage.INFORM);
					msgSend.setSender(this.myAgent.getAID());
					msgSend.setProtocol(msg.getContent() + ": dissolve and go to new coalition");
					msgSend.setContent(otherCoalLeader.toString());
					msgSend.addReceiver(new AID(e.getKey(), AID.ISLOCALNAME));
					((AbstractDedaleAgent)this.myAgent).sendMessage(msgSend);
		    	}
		    }
			
			// 2) Prévenir le leader que des agents arrivent ? est-ce vraiment utile
			
			// 3) Désactiver la coalition
		    // TODO: est-ce qu'il faut attendre que tous les agents se soit transférés avant de transférer le leader ?
		    // parce que si un relais de messages entre en jeu et que ça repose sur le mécaisme de coalition on ne 
		    //peut pas la dissoudre direct car destruction du relais
		}
		
		// Si je ne suis pas le leader
		else {
			// Je demande au leader du message d'entrer dans sa coalition
			// TODO: est-ce qu'on peut se rebrancher directement sur ReceiveMessageSayGolem
			// ou est-ce qu'on copie colle la partie concernée ici ?
			
			ACLMessage msgSend = new ACLMessage(ACLMessage.INFORM);
			msgSend.setSender(this.myAgent.getAID());
			msgSend.setProtocol("Request entry coalition");
			//msgSend.setContent();
			msgSend.addReceiver(otherCoalLeader);
			((AbstractDedaleAgent)this.myAgent).sendMessage(msgSend);
		}	
	}
	
	private void changeCoal(ACLMessage msg) {
		// Le message contient un string de l'AID du leader de la coalition à rejoindre
		// ainsi que le nb d'agents à transférer
		String[] tokens = msg.getContent().split("[ ]");
		String otherCoalLeader = tokens[0];
		int nbTransferedAgents = Integer.parseInt(tokens[1]);
		if (log) {
			System.out.println("Je dois transférer " + nbTransferedAgents + "agent(s) dans la coalition de" + otherCoalLeader);
		}
		
		// Si je suis le leader
		if (((ExploreSoloAgent)this.myAgent).getLeaderCoalition()) {

			// Choix des agents à transférer par ordre de parcours de la hashmap des membres
			int nb = 0; // nb d'agents transférés
			Iterator<Entry<String, List<String>>> it = members.entrySet().iterator();
			while(it.hasNext() && nb<nbTransferedAgents) {
		    	Entry<String, List<String>> e = it.next();
		    	
		    	// on ne transfère pas le leader (en plus c'est moi) 
		    	if (!(e.getValue().get(0).equals("0"))) {
			    	ACLMessage msgSend = new ACLMessage(ACLMessage.INFORM);
					msgSend.setSender(this.myAgent.getAID());
					msgSend.setProtocol(msg.getContent() + ": change coalition");
					msgSend.setContent(otherCoalLeader);
					msgSend.addReceiver(new AID(e.getKey(), AID.ISLOCALNAME));
					((AbstractDedaleAgent)this.myAgent).sendMessage(msgSend);
					nb++;
		    	}
		    }
		}
		
		// Si je ne suis pas le leader
		else {
			// Je demande au leader du message d'entrer dans sa coalition
			// TODO: est-ce qu'on peut se rebrancher directement sur ReceiveMessageSayGolem
			// ou est-ce qu'on copie colle la partie concernée ici ?
			
			ACLMessage msgSend = new ACLMessage(ACLMessage.INFORM);
			msgSend.setSender(this.myAgent.getAID());
			msgSend.setProtocol("Request entry coalition");
			//msgSend.setContent();
			msgSend.addReceiver(new AID(otherCoalLeader, AID.ISLOCALNAME));
			((AbstractDedaleAgent)this.myAgent).sendMessage(msgSend);	
		}	
	}
	
	public boolean done() {
		return finished;
	}
	
}
