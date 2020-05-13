package eu.su.mas.dedaleEtu.mas.behaviours;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.util.Iterator;
import java.util.Map.Entry;

import dataStructures.serializableGraph.SerializableSimpleGraph;
import dataStructures.tuple.Couple;
import eu.su.mas.dedale.env.Observation;
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
	//nombre max d'agents possible dans la coalition
	private int maxAgent;//(dans update)
	//signale si la coalition est pleine
	private boolean candidatAgentOpen;// (dans update)
	//Donne la position la plus probable du golem
	private String golemLocalisation;//(dans update)
	//Donne la nouvelle position la plus probable du golem
	private String oldGolemLocalisation;//(dans update)
	//donne la liste des noeud du golem (sert pour la vérification qu'il soit attraper)
	private List <String> neighborNodeGolem;
	//Sert de numérotation pour indiquer l'ordre des messages pour les ordres de la coalition (demande de données et ordre de placement)
	private int stepMSG;
	//Sert de numérotation au update envoyer
	private int numUpdate;//(dans update)
	//quel est mon pére? Utiliser pour faire le relais de message
	private HashMap<String, String> fatherLastMSG = new HashMap <String, String> ();;
	//quels sont mes fils (pas utilisé pour le moment)
	private List<String> sonLastMSG = new ArrayList<String>();
	//temps d'attente max avant de sortir de la coalition si aucun nouveau message
	private long timerWaitMessageCoalition;
	//variable a true tant que tous les agents de la coalition n'ont pas envoyé leurs données sur la vision du golem (utilisé par le leader)
	private boolean waitDataGolemAllAgent;
	//déclenche la chasse au golem
	private boolean huntGolem;
	//déclenche la phase de calcul pour attraper le golem
	private boolean huntGolemProcess;
	//chasse en cours
	private boolean huntGolemNow;
	//stocke les valeurs de position probable du golem envoyées par chaque agent (stack les valeurs de même position) (utilisé par le leader)
	private HashMap<String, Double> dataPositionGolem;
	//liste des agents dont on attend la réponse des données sur le golem (utilisé par le leader)
	private List<String> waitAgentDataReturn = new ArrayList <String> ();
	//Position donnée a chaque agent pour attraper le golem (utilisé par le leader)
	private HashMap <String, String> positionAgent = new HashMap <String, String> ();
	//hashmap de boolean pour signaler si l'agent est arrivé à la position demandée pour attraper le golem (utilisé par le leader)
	private HashMap <String, Boolean> verificationPositionAgent = new HashMap <String, Boolean> ();
	//agent arrivé a bon port (à envoyé)
	private HashMap <String, String> agentinPosition = new HashMap <String, String> ();
	//position où l'agent doit aller (propre à chaque agent)
	private String goPosition;
	//Signifie si le golem est attrapé ou non
	private Boolean golemCatch;//(dans update)
	//Si la coalition est active
	private Boolean activeCoalition;
	//le golem a bouger
	private boolean golemMove;
	//position du leader
	private String posLeader;
	//timer pour l'attente de reption de donnée
	private long timerData;
	//temp d'attente max pour le timerData
	private long waitTimerData;
	
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
		this.waitDataGolemAllAgent = false;
		this.huntGolem = false;
		this.golemCatch = false;
		this.activeCoalition = true;
		this.huntGolemProcess = false;
		this.huntGolemNow = false;
		this.agentinPosition.put("list", "1");
		this.agentinPosition.put(this.myAgent.getLocalName(), "");
		this.goPosition = "0";
		
		this.fatherLastMSG.put(this.id_Coal + ": golem hunt strat 1", "");
		this.fatherLastMSG.put(this.id_Coal + ": golem hunt strat 3", "");
		this.fatherLastMSG.put(this.id_Coal + ": In position", "");
		this.golemMove = false;
		this.posLeader = ((AbstractDedaleAgent)this.myAgent).getCurrentPosition();
		this.waitTimerData = 150;
		
		((ExploreSoloAgent) this.myAgent).setMoving(false);//je ne bouge que sur ordre de la coalition
		//definir où se trouve le golem et mettre à jour si leader
		if(((ExploreSoloAgent)this.myAgent).getLeaderCoalition()) {
			this.golemLocalisation = this.bestPositionGolem();
			this.oldGolemLocalisation = this.golemLocalisation;
			this.neighborNodeGolem = new ArrayList <String>();
		}
		
		// Nombre agent max dans coalition
		this.maxAgent = ((ExploreSoloAgent)this.myAgent).getMap().getMaxDegree(); // degré max du graphe
		//this.maxAgent = ((ExploreSoloAgent)this.myAgent).getMap().getAvDegree(); // degré moyen du graphe
		//this.maxAgent = 2; //pour test
		
		System.out.println(this.myAgent.getLocalName() + " : Behaviour de coalition " + this.id_Coal + " actif");
		
		
	}
	
	@Override
	public void action() {
		//si la coalition est active
		if(this.activeCoalition) {

			//System.out.println(this.myAgent.getLocalName() + " : Behaviour de coalition " + this.id_Coal + " actif");

			//Template message neutre, utilisé actuellement pour:
			//Les demandes et réponses pour entrer dans la coalition
			final MessageTemplate msgTemplate = MessageTemplate.and(MessageTemplate.MatchPerformative(ACLMessage.INFORM), 
					MessageTemplate.MatchProtocol(this.id_Coal));			
			final ACLMessage msg = this.myAgent.receive(msgTemplate);

			//Template de reception des demandes pour entrer dans la coalition
			final MessageTemplate msgTemplateEnterCoalition = MessageTemplate.and(MessageTemplate.MatchPerformative(ACLMessage.INFORM), 
					MessageTemplate.MatchProtocol("Request entry coalition"));			
			final ACLMessage msgEnterCoalition = this.myAgent.receive(msgTemplateEnterCoalition);

			//Template message concernant la position probable du golem vu par la coalition utilisé actuellement pour:
			//demander et repondre à la question "Où est le golem?"
			final MessageTemplate msgTemplateGolemPosition = MessageTemplate.and(MessageTemplate.MatchPerformative(ACLMessage.INFORM), 
					MessageTemplate.MatchProtocol(this.id_Coal + ": golem position"));			
			final ACLMessage msgGolemPosition = this.myAgent.receive(msgTemplateGolemPosition);		

			//Template message concernant le golem utilisé actuellement pour:
			//leader : demande aux autres agents si ils sentent aussi les golems et où
			//leader : envoie à chaque agent où ils doivent se placer
			//autre : répondre au leader
			final MessageTemplate msgTemplateGolemHuntStrat = MessageTemplate.and(MessageTemplate.MatchPerformative(ACLMessage.INFORM), 
					MessageTemplate.MatchProtocol(this.id_Coal + ": golem hunt strat"));			
			final ACLMessage msgGolemHuntStrat = this.myAgent.receive(msgTemplateGolemHuntStrat);

			//Template message nombre d'agent dans la coalition, utilisé actuellement pour:
			// demander et répondre à la question "Nb d'agents dans la coalition?"
			final MessageTemplate msgTemplatenbAgent = MessageTemplate.and(MessageTemplate.MatchPerformative(ACLMessage.INFORM), 
					MessageTemplate.MatchProtocol(this.id_Coal + ": nb Agent"));			
			final ACLMessage msgnbAgent = this.myAgent.receive(msgTemplatenbAgent);	

			//Template message pour l'update des données du behaviour de la coalition, utilisé actuellement pour:
			//leader: envoyer les données updatées du behaviour à l'ensemble de la coalition
			final MessageTemplate msgTemplateUpdate = MessageTemplate.and(MessageTemplate.MatchPerformative(ACLMessage.INFORM), 
					MessageTemplate.MatchProtocol(this.id_Coal + ": Update behaviour"));			
			final ACLMessage msgUpdate = this.myAgent.receive(msgTemplateUpdate);	

			//Template message pour stoper le recrutement dans la coalition, utilisé actuellement pour:
			//leader: envoyer à l'ensemble de la coalition d'arréter le recrutement
			final MessageTemplate msgTemplateCoalitionFull = MessageTemplate.and(MessageTemplate.MatchPerformative(ACLMessage.INFORM), 
					MessageTemplate.MatchProtocol(this.id_Coal + ": Coalition full"));			
			final ACLMessage msgCoalitionFull = this.myAgent.receive(msgTemplateCoalitionFull);

			//Template message pour dire que je suis sur la position définie par le leader
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
				
				//je regarde autour de moi si le golem à bougé si je ne suis pas en attente de message
				if(!waitDataGolemAllAgent) {
					this.updateViewAgent();
				}
				//de nouveaux agents peuvent encore venir dans la coalition
				if(this.candidatAgentOpen) {
					if(msgEnterCoalition != null && !msgEnterCoalition.getSender().getLocalName().equals(this.myAgent.getAID().getLocalName())) {									
						if(log) {
							System.out.println("Reception d'une demande d'entré dans la coalition");
						}
						//reception d'une demande d'entrer dans la coalition
						this.addAgentCoalition(msgEnterCoalition);
					}			
					//reception d'une demande de localisation de golem
					if (msgGolemPosition != null && !msgGolemPosition.getSender().getLocalName().equals(this.myAgent.getAID().getLocalName())) {
						if(log) {
							System.out.println("Reception d'une demande de localisation du golem");
						}
						this.golemPosition(msgGolemPosition);
					}			
					//reception d'une demande de nombre d'agent
					if (msgnbAgent != null && !msgnbAgent.getSender().getLocalName().equals(this.myAgent.getAID().getLocalName())) {
						if(log) {
							System.out.println("Reception d'une demande du nombre d'agent dans la coalition");
						}
						this.nbAgentCoalition(msgnbAgent);
					}
					//si la coalition est pleine
					if (this.members.size() >= this.maxAgent) {
						if(log) {
							System.out.println("La coalition est pleine.");
							System.out.println("Coalition " + this.id_Coal + " : " + this.members);
						}
						//on ferme le droit d'inscription
						this.candidatAgentOpen = false;
						((ExploreSoloAgent)this.myAgent).setInCoalitionFull(true);
						//dit aux autre agents d'arréter le recrutement
						this.sendCoalitionFull();
						//Demande à tous les agents de la coalition si ils voient un golem actuellement et récup data
						this.giveDataGolem();
					}
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
				
				
				//si j'attend des données
				if(this.waitDataGolemAllAgent) {	
					if(log) {
						System.out.println(this.myAgent.getLocalName() + " : En attente de reception de données");
					}
					if(msgGolemHuntStrat != null) {
						//traitement des messages reçus
						HashMap<String, String> data;
						try {
							data = (HashMap<String, String>) msgGolemHuntStrat.getContentObject();
						} catch (UnreadableException e) {
							System.out.println("Problem msgGolemHuntStrat.getContentObject(), no format HashMap <String, String>");
							e.printStackTrace();
							data = new HashMap <String, String> ();
							data.put("data", "-1");
						}
						if(log) {
							System.out.println(this.myAgent.getLocalName() + " : msgGolemHuntStrat recue : " + data);
						}
						//si il y a des données valables pour la chasse au golem
						if(data.get("data").equals("2") && Integer.parseInt(data.get("Step")) == this.stepMSG-1){
							//faire une boucle sur les key pour récupérer les double (sous forme de string) à intégrer a chaque noeud
							for(String nodeID : data.keySet()) {
								//permet de prendre uniquement les id node
								if(!nodeID.equals("data") && !nodeID.equals("agent")) {
									if(log) {
										System.out.println("Le leader regarde les données de localisation de golem envoyer par un agent : " + nodeID + " , " + data.get(nodeID));
									}
									this.addValueDataPositionGolem(nodeID, Double.parseDouble(data.get(nodeID)));
								}
							}
						}
						//retirer l'agent de la liste d'attente de retour de données
						this.waitAgentDataReturn.remove(data.get("agent"));	
					}	
					
					//si le timer de donnée est terminé
					if(System.currentTimeMillis() - this.timerData > this.waitTimerData ) {
						this.waitDataGolemAllAgent = false;
					}
				}
				
				//si la liste est vide alors plus de données en attente de réception
				if(this.waitAgentDataReturn.isEmpty()) {
					if(log) {
						System.out.println("Plus de données en attante de reception");
					}
					this.waitDataGolemAllAgent = false;//on arréte l'attente de donnée
					this.huntGolemProcess = true;//on lance le calcule
				}
				
				if(this.huntGolemProcess) {
					//definir où se trouve le golem et mettre à jour
					this.golemLocalisation = this.bestPositionGolem();
					if(log) {
						System.out.println(this.myAgent.getLocalName() + " : golem localisé en : " + this.golemLocalisation);
					}
					//envoi de l'update du behaviour
					this.sendUpdateDataBehaviour();
					//calcul de la postion à donner à chaque agent
					this.calculPositionCaptureGolem();
					//envoie des données au agent concernèe
					this.sendPositionAgentHuntGolem();
					//si la position est pas null
					if(this.goPosition != null) {
						//mis en position du leader
						this.followPathNewPosition(this.goPosition);
					}
					//fin du processe pour chasse au golem
					this.huntGolemProcess = false;
					//début de l'attente de la bonne position des agents
					this.huntGolem = true;
				}
				
				//if(this.huntGolem) {
					//je recois des message d'agents qui me disent qu'ils sont en position
					if(msgInPosition != null && !this.golemCatch && !msgInPosition.getSender().getLocalName().equals(this.myAgent.getLocalName())) {
						this.sendAgentVerifPosition(msgInPosition);				
						}
				//}	
				//le golem est attrapé
				if(this.golemCatch) {
					this.huntGolem = false;
					System.out.println("Le golem est attrapé!!!");
				}
			}//non leader
			else {
				//je recois une update des données du behaviour (penser a sup say golem si candidatAgentOpen=false)
				if(msgUpdate != null) {
					this.receivedUpdateDataBehaviour(msgUpdate);
				}
				//je reçois un message du leader qui me donne une position pour choper le golem ou me demande où il est et ce n'est pas moi
				if(msgGolemHuntStrat != null && !msgGolemHuntStrat.getSender().getLocalName().equals(this.myAgent.getLocalName())) {					
					//si le message reçu est une réponse (data = 2) alors l'envoyer à celui qui m'a posé la question
					HashMap <String, String> msgGolemStrat;
					try {
						msgGolemStrat =(HashMap<String, String>) msgGolemHuntStrat.getContentObject();
					} catch (UnreadableException e) {
						msgGolemStrat = new HashMap <String, String> ();
						msgGolemStrat.put("Step", "0");
						msgGolemStrat.put("data", "-1");
						System.out.println("Erreur reception message protocol : msgGolemHuntStrat");
						e.printStackTrace();
					}		
					
					if(log) {
						System.out.println("Message msgGolemHuntStrat protocol :");
						System.out.println("Step :" + msgGolemStrat.get("Step"));
						System.out.println("Data :" + msgGolemStrat.get("data"));
					}
					//vérifie qu'il n'y a pas d'erreur
					if(!msgGolemStrat.get("data").equals("-1")) {
						//Si data = 2 c'est une réponse envoyée par mon fils, je dois la redonner à mon pére
						if(msgGolemStrat.get("data").equals("2")) {
							HashMap <String, List <String>> father = new HashMap <String, List <String>> ();
							father.put(fatherLastMSG.get(this.id_Coal + ": golem hunt strat 1"), new ArrayList<String> ());
							this.sendMessageObjectCoalition(msgGolemHuntStrat.getProtocol(), father , msgGolemStrat);
						}//sinon
						else {
							//j'ajoute le protocole et le sender aux données
							msgGolemStrat.put("proto", msgGolemHuntStrat.getProtocol());
							msgGolemStrat.put("father", msgGolemHuntStrat.getSender().getLocalName());
							if(msgGolemStrat.get("data").equals("1")) {
								//demande où il est
								this.receiveRequestGolemPosition(msgGolemStrat);
							}
							if(msgGolemStrat.get("data").equals("3")) {
								//me dit où aller
								this.receivePositionCatchGolem(msgGolemStrat);	
							}
						}
					}
				}
				
				//réception d'une demande de dissolution et changement de coalition
				if (msgDissolveAndChangeCoal != null && !msgDissolveAndChangeCoal.getSender().getLocalName().equals(this.myAgent.getLocalName())) {
					this.dissolveAndChangeCoal(msgDissolveAndChangeCoal);
					if (log) {
						System.out.println("Reception d'une demande de dissolution de la coalition et de migration vers une autre");
					}
				}
				//réception d'une demande de changement de coalition
				if (msgChangeCoal != null && !msgChangeCoal.getSender().getLocalName().equals(this.myAgent.getLocalName())) {
					this.changeCoal(msgChangeCoal);
					if (log) {
						System.out.println("Reception d'une demande de migration de quelques agents vers une autre coalition");
					}
				}
				
				//si ma position n'est pas null
				if(this.goPosition != null) {

					//si je suis arrivé sur la position que l'on m'a attribuée
					if(goPosition.equals(((AbstractDedaleAgent)this.myAgent).getCurrentPosition())) {
						//envoyer à tout le monde que l'agent est sur position
						if(!this.agentinPosition.get(this.myAgent.getLocalName()).equals(((AbstractDedaleAgent)this.myAgent).getCurrentPosition())) {
							this.agentinPosition.remove(this.myAgent.getLocalName());//j'enlève l'ancienne position
							this.agentinPosition.put(this.myAgent.getLocalName(), ((AbstractDedaleAgent)this.myAgent).getCurrentPosition());//je rajoute la nouvelle position
						}
						if(log) {
							System.out.println("Envoie localisation ok : " + this.agentinPosition);
						}
						this.sendMessageObjectCoalition(this.id_Coal + ": In position", this.members, this.agentinPosition);
						((ExploreSoloAgent) this.myAgent).setMoving(false);
						if(log) {
							System.out.println(this.myAgent.getAID().getLocalName() + " : Je suis arrivé sur ma position - " + ((AbstractDedaleAgent)this.myAgent).getCurrentPosition());
						}
					}
				}
				if(!this.golemCatch && msgInPosition != null && msgInPosition.getSender().getLocalName().equals(this.myAgent.getLocalName())) {
					HashMap <String, String> position;
					try {
						position = (HashMap<String, String>) msgInPosition.getContentObject();
					} catch (UnreadableException e) {
						position = new HashMap <String, String> ();
						position.put("list", "0");
						System.out.println("Erreur reception message protocol : msgGolemHuntStrat");
						e.printStackTrace();
					}		
					if(this.agentinPosition.get("list").equals("1")){
						//si j'ai éjà enregistré l'agent
						if(this.agentinPosition.containsKey(msgInPosition.getSender().getLocalName())) {
							//si la position de l'agent a changé
							if(!this.agentinPosition.get(msgInPosition.getSender().getLocalName()).equals(position.get(msgInPosition.getSender().getLocalName())) ) {
								this.agentinPosition.remove(msgInPosition.getSender().getLocalName());
								this.agentinPosition.put(msgInPosition.getSender().getLocalName(), position.get(msgInPosition.getSender().getLocalName()));
							}
						}//sinon je l'enregistre dans ma liste
						else {
							this.agentinPosition.put(msgInPosition.getSender().getLocalName(), position.get(msgInPosition.getSender().getLocalName()));
						}
						if(log) {
							System.out.println("Envoie localisation ok (transfert) : " + this.agentinPosition);
						}
						this.sendMessageObjectCoalition(this.id_Coal + ": In position", this.members, this.agentinPosition);
					}
				}
			}
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
		System.out.println(this.myAgent.getLocalName() + " : envoie nb agent à " + msg.getSender().getLocalName());
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
		this.neighborNodeGolem = ((ExploreSoloAgent)this.myAgent).getMap().neighborNode(golemLocalisation);
		//repartir les noeuds entre les agents de la liste en définissant le chemin le plus court pour chacun
		//copy de la list des agent pour éviter de donnée 2 noeud à un même agent
		List <String> copyMembers = new ArrayList <String> ();
		for(String agent : this.members.keySet()) {
			copyMembers.add(agent);
		}	
		this.positionAgent = new HashMap <String, String> ();
		//pour chaque noeud
		for(int i = 0; i < copyMembers.size(); i++ ) {
			//regarder quel agent est le plus proche
			String bestNode = "";
			int distancePath = 100000;
				//tester si liste vide
			if(!neighborNodeGolem.isEmpty()) {
				List <String> path;
				for(int j= 0; j < neighborNodeGolem.size(); j++) {
					path = ((ExploreSoloAgent)this.myAgent).getMap().getShortestPath(this.members.get(copyMembers.get(i)).get(1),neighborNodeGolem.get(j));//definit le meilleur chemin entre la  position de l'agent et le noeud à aller
					if(path.size() < distancePath) {
						distancePath = path.size();
						bestNode = neighborNodeGolem.get(j);
					}
				}
				neighborNodeGolem.remove(bestNode);
				if(copyMembers.get(i).equals(this.myAgent.getLocalName())) {
					this.goPosition = bestNode;
					if(log) {
						System.out.println("Position leader mise a jour : " + this.goPosition);
					}
				}
				else {
					this.positionAgent.put(copyMembers.get(i), bestNode);
					this.verificationPositionAgent.put(copyMembers.get(i), false);
				}
			}		
		}	
	}
	
	//envoie de la position au agent de la coalition
	private void sendPositionAgentHuntGolem() {
		//on ajout le numéro de message
		this.positionAgent.put("Step", "" + this.stepMSG);
		this.positionAgent.put("data","3");
		//création de liste des agents concerné
		HashMap<String, List <String>> listAgent = new HashMap <String, List <String>> ();
		for(String agent : this.positionAgent.keySet()) {
			if(!agent.equals("data") && !agent.equals("Step")) {
				listAgent.put(agent, new ArrayList <String>());
			}
		}
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
			data.put("data", "2");
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
	
	private void receiveRequestGolemPosition(HashMap<String, String> request) {
		//vérification si bien étape suivante (et non vieux message) et que c'est la demande de position de l'odeur du golem (data = 1)
		if((Integer.parseInt(request.get("Step")) > this.stepMSG) && (Integer.parseInt(request.get("data")) == 1)) {
			//je note de qui j'obtien le message
			this.fatherLastMSG.put(this.id_Coal + ": golem hunt strat 1", "" + request.get("father"));
			//brodcast le message
			this.sendMessageObjectCoalition(request.get("father"), this.members, request);
			//mettre a jour le step
			this.stepMSG = Integer.parseInt(request.get("Step"));
			//je regarde autour de moi si le golem à bougé
			this.updateViewAgent();
			//calcule de la position golem à envoyé
			HashMap<String, Double> data_Position_Golem = ((ExploreSoloAgent)this.myAgent).getMap().listNodeGolemProba();
			//envoie vers le mec qui ma envoyer le message
			HashMap<String, String> data_Position_Golem_send = new HashMap<String, String> ();
			if(!data_Position_Golem.isEmpty()) {
				for(String node : data_Position_Golem.keySet()) {
					data_Position_Golem_send.put(node, "" + data_Position_Golem.get(node));
				}		
			}	
			if(log) {
				System.out.println("Demande de donnée pour la localisation du golem : " + data_Position_Golem);
				System.out.println("Demande de donnée pour la localisation du golem : data à envoyé : " + data_Position_Golem_send);
			}
			data_Position_Golem_send.put("data", "2");
			data_Position_Golem_send.put("agent", this.myAgent.getLocalName());
			data_Position_Golem_send.put("Step", "" + this.stepMSG);
			if(log) {
				System.out.println("Demande de donnée pour la localisation du golem : envoie : " + data_Position_Golem_send);
			}
			HashMap <String, List <String>> father = new HashMap <String, List <String>> ();
			father.put(request.get("father"), new ArrayList<String> ());
			this.sendMessageObjectCoalition(request.get("proto"), father , data_Position_Golem_send);
		}
		
	}
	
	private void receivePositionCatchGolem(HashMap <String, String> allPosition) {
		if(log) {
			System.out.println(this.myAgent.getAID().getLocalName() + " : Reception position pour attraper le golem :" + allPosition);
		}
		//si nouvelle étape de positionnement
		if((Integer.parseInt(allPosition.get("Step")) > this.stepMSG) && (Integer.parseInt(allPosition.get("data")) == 3)) {
			//je note de qui j'obtien le message
			this.fatherLastMSG.put(this.id_Coal + ": golem hunt strat 3", "" + allPosition.get("father"));
			HashMap<String, List <String>> listAgent = new HashMap <String, List <String>> ();
			for(String agent : allPosition.keySet()) {
				if(!agent.equals("data") && !agent.equals("Step")) {
					listAgent.put(agent, new ArrayList <String>());
				}
			}
			//envoie du message pour agent plus loin (ping)
			this.sendMessageObjectCoalition(allPosition.get("proto"), listAgent, allPosition);
			//je regarde ou me positionner
			this.goPosition = allPosition.get(this.myAgent.getLocalName());
			if(log) {
				System.out.println(this.myAgent.getLocalName() + " : go position : " + this.goPosition);
			}
			//demarrer la chasse
			this.huntGolem = true;
			//mettre a jour le step
			this.stepMSG = Integer.parseInt(allPosition.get("Step"));
			if(this.goPosition != null) {
				if(!this.goPosition.equals("0")) {
					//je lance le calcule du chemin pour la nouvelle position et mis rend
					if(!this.goPosition.equals(((AbstractDedaleAgent)this.myAgent).getCurrentPosition())){
						this.followPathNewPosition(this.goPosition);
					}
				}
			}
			
			if(log) {
				System.out.println(this.myAgent.getLocalName() + " reception position pour attraper le golem : nouvelle posittion : " + this.goPosition);
			}
		}
	}
	
	//verifie que tous les agent sont en place a chaque message recu par un agent
	private void sendAgentVerifPosition(ACLMessage msg) {
		HashMap <String, String> position;
		
		try {
			position = (HashMap<String, String>) msg.getContentObject();
		} catch (UnreadableException e) {
			position = new HashMap <String, String> ();
			position.put("list", "-1");
			System.out.println("Send verification position problem");
			e.printStackTrace();
		}	
		if(!position.get("list").equals("-1")) {
			for(String agent : position.keySet()) {
				if(!agent.equals("list")) {
					if(log) {
						System.out.println("Regarde la position d'un agent : " + position.get(agent));
						System.out.println("Normalement c'est : " + this.positionAgent.get(agent));
					}
					//je vérifie que c'est bien la bonne position
					if(this.positionAgent.get(agent) != null && this.positionAgent.get(agent).equals(position.get(agent))) {
						this.verificationPositionAgent.remove(agent);
						this.verificationPositionAgent.put(agent, true);
					}
				}
			}
		}
		
		if(log) {
			System.out.println("positionAgent : " + this.positionAgent.size() + " : neighborNodeGolem : " + this.neighborNodeGolem.size());
		}
		//si le nombre d'agent positionner égal au nombre de noeud voisin alors il y a assez d'agent pour l'attraper et donc on procéde a la vérification des positions
		if(this.positionAgent.size() == this.neighborNodeGolem.size()-1) {
			if(log) {
				System.out.println("Bon nombre d'agent pour le bon nombre de noeud autour du golem");
			}
			this.golemCatch = true;
			for(String verifAgent : this.verificationPositionAgent.keySet()) {
				if(!this.verificationPositionAgent.get(verifAgent)) {
					this.golemCatch = false;
				}
			}
		}

		
		if(log) {
			System.out.println("Reception bonne position agent : " + msg.getSender().getLocalName());
			System.out.println("Vérification position agent : " + this.verificationPositionAgent);
		}
		
		if(this.golemCatch) {
			System.out.println("Golem en " + this.golemLocalisation + " attrapé!!");
			//envoie de l'update du behaviour
			this.sendUpdateDataBehaviour();
		}
	}
	
	private void followPathNewPosition(String node) {	
		List <String> oldPath = new ArrayList <String>();
		//oldPath.add(((AbstractDedaleAgent)this.myAgent).getCurrentPosition());
		String posTemp = ((AbstractDedaleAgent)this.myAgent).getCurrentPosition();
		int stopWhile = 0;
			//on fait bouger l'agent jusqu'à là bas
			while(!((AbstractDedaleAgent)this.myAgent).getCurrentPosition().equals(this.goPosition) && stopWhile < 20) {
				stopWhile ++;
				// en établie le meilleur chemin
				List <String> path = ((ExploreSoloAgent)this.myAgent).getMap().getShortestPath(((AbstractDedaleAgent)this.myAgent).getCurrentPosition() , node);
				if(log) {
					System.out.println(this.myAgent.getLocalName() + " chemin calculeé : " + path);
				}
				/*
				try {
					this.myAgent.doWait(500);
				} catch (Exception e) {
					e.printStackTrace();
				}
				*/
				//si le chemin n'est pas vide
				if(!path.isEmpty()) {
					if(log) {
						System.out.println(this.myAgent.getLocalName() + " : ma position : " + ((AbstractDedaleAgent)this.myAgent).getCurrentPosition());
						System.out.println(this.myAgent.getLocalName() + " : je doit aller en  : " + (path.get(0)));
					}
					//je bouge
					((AbstractDedaleAgent)this.myAgent).moveTo(path.get(0));
					if(log) {
						System.out.println(this.myAgent.getLocalName() + " : nouvelle position : " + ((AbstractDedaleAgent)this.myAgent).getCurrentPosition());
					}
					//si je n'est pas bouger (currentposition est égal a l'ancienne position
					if(posTemp.equals(((AbstractDedaleAgent)this.myAgent).getCurrentPosition())) {
						//alors je bouge aléatoirement
						if(log) {
							System.out.println(this.myAgent.getLocalName() + " : Je n'est pas bouger je me déplace donc aléatoirement");
						}
						//je regarde les voisins
						List <String> neighbor = ((ExploreSoloAgent)this.myAgent).getMap().neighborNode(((AbstractDedaleAgent)this.myAgent).getCurrentPosition());
						//je vire les noeuds ou je suis déjà allé
						/*
						for(int i = 0; i < neighbor.size();) {
							for(int j = 0; j < oldPath.size(); j++) {
								//si un vieux node est dan la liste des voisins, la viré des voisins
								if(oldPath.get(j).equals(neighbor.get(i))) {
									neighbor.remove(i);
								}
								else {
									i++;
								}
							}
						}
						*/
						//je bouge aléatoirement sur les voisins
						Random rand = new Random();
						int nb = rand.nextInt(neighbor.size());
						String nodeRand = neighbor.get(nb);
						((AbstractDedaleAgent)this.myAgent).moveTo(nodeRand);
						if(log) {
							System.out.println(this.myAgent.getLocalName() + " : Déplacmenet aléatoire en : " + nodeRand);
						}					
					}
				}
			}
		if(log) {
			System.out.println(this.myAgent.getLocalName() + " : je suis en  : " + ((AbstractDedaleAgent)this.myAgent).getCurrentPosition());
		}
	}
	
	//demande aux autre agents ou ils sentent le golem
	private void giveDataGolem() {
		this.waitAgentDataReturn = new ArrayList <String>();
		for(String agent : this.members.keySet()) {
			if(!agent.equals(this.myAgent.getLocalName())) {
				this.waitAgentDataReturn.add(agent);
			}
		}
		HashMap <String, String> msg = new HashMap <String, String> ();
		msg.put("data", "1");
		msg.put("msg", "Give your data golem!!!!");
		msg.put("Step", "" + this.stepMSG);
		this.sendMessageObjectCoalition(this.id_Coal + ": golem hunt strat", this.members, msg);
		this.stepMSG ++;
		this.waitDataGolemAllAgent = true;
		this.huntGolem = false;
		if(log) {
			System.out.println(this.myAgent.getLocalName() + " : demande d'info au autre agent : " + msg);
		}
		//départ timer reception
		this.timerData = System.currentTimeMillis();
	}
	
	//dit aux autre agents d'arréter le recrutement
	private void sendCoalitionFull() {
		this.sendMessageStringCoalition(this.id_Coal + ": Coalition full", this.members, "stop SayGolem");
	}
	
	private void dissolveAndChangeCoal(ACLMessage msg) {
		// Le message contient un string de l'AID du leader de la coalition à rejoindre
		String otherCoalLeader = msg.getContent();
		
		// TODO: Est-ce qu'il est nécessaire de faire un sendCoalitionFull() d'abord ?
		
		// Si je suis le leader
		if (((ExploreSoloAgent)this.myAgent).getLeaderCoalition()) {
			
			// 1) Je préviens tous les agents de la coalition qu'ils doivent en changer
			HashMap <String, List <String>> agents = new HashMap <String, List <String>>(this.members);
			agents.remove(this.myAgent.getAID().getLocalName()); // je me retire de la liste des agents à qui transmettre le message
			this.sendMessageStringCoalition(msg.getContent() + ": dissolve and go to new coalition", agents, otherCoalLeader.toString());
			
			this.requestEntryCoalition(otherCoalLeader);
						
			// 2) Prévenir le leader que des agents arrivent ? est-ce vraiment utile
			
			// 3) Désactiver la coalition
		    // TODO: est-ce qu'il faut attendre que tous les agents se soit transférés avant de transférer le leader ?
		    // parce que si un relais de messages entre en jeu et que ça repose sur le mécaisme de coalition on ne 
		    //peut pas la dissoudre direct car destruction du relais
		}
		
		// Si je ne suis pas le leader
		else {
			// Je demande à l'autre leader (dont l'AID a été transmise par message) d'entrer dans sa coalition			
			this.requestEntryCoalition(otherCoalLeader);
		}	
	}
	
	private void changeCoal(ACLMessage msg) {
		// Le message contient un string de l'AID du leader de la coalition à rejoindre
		// ainsi que le nb d'agents à transférer
		
		// Si je suis le leader
		if (((ExploreSoloAgent)this.myAgent).getLeaderCoalition()) {
			
			// Choix des agents à transférer par ordre de parcours de la hashmap des membres
			String[] tokens = msg.getContent().split("[ ]");
			String otherCoalLeader = tokens[0]; // string de l'AID du leader de la coalition à rejoindre
			int nbTransferedAgents = Integer.parseInt(tokens[1]); // nb d'agents à transférer
			if (log) {
				System.out.println("Je dois transférer " + nbTransferedAgents + "agent(s) dans la coalition de" + otherCoalLeader);
			}
			List <String> content = new ArrayList <String>();
			content.add(otherCoalLeader);// le premier élément de la liste correspond au leader de la coalition à rejoindre
			HashMap <String, List <String>> agents = new HashMap <String, List <String>>(this.members);
			agents.remove(this.myAgent.getAID().getLocalName()); // on ne veut pas transférer le leader ni lui envoyer de message (c'est moi !!)
			int nb = 0; // nb d'agents transférés
			Iterator<Entry<String, List<String>>> it = agents.entrySet().iterator();
			while(it.hasNext() && nb<nbTransferedAgents) {
		    	Entry<String, List<String>> e = it.next();
		    	content.add(e.getKey());
		    	nb++;
			}
			
			// Envoie du message à tous les agents de la coalition (sauf le leader)
			this.sendMessageObjectCoalition(msg.getContent() + ": change coalition", agents, content);
		}
		
		// Si je ne suis pas le leader
		else {
			// Je regarde si je fais partie des agents qui doivent changer de coalition
			List<String> agentsToChangeCoal = new ArrayList<String>();
			try {
				agentsToChangeCoal = (List<String>) msg.getContentObject();
			} catch (UnreadableException e) {
				e.printStackTrace();
			}
			// Si oui, je demande au leader (dont l'AID a été transmise par message) d'entrer dans sa coalition
			if (agentsToChangeCoal.contains(this.myAgent.getAID().getLocalName())) { 
				this.requestEntryCoalition(agentsToChangeCoal.get(0));
			}
			// Sinon, je fais juste suivre le message
			else { 				
				// TODO
				
			}
		}	
	}
	
	private void requestEntryCoalition(String leader) {
		// Demande au leader passé en paramètre d'entrer dans sa coalition
		((ExploreSoloAgent)this.myAgent).setInCoalition(false);
		((ExploreSoloAgent)this.myAgent).setLeaderCoalition(false);
		
		ACLMessage msgSend = new ACLMessage(ACLMessage.INFORM);
		msgSend.setSender(this.myAgent.getAID());
		msgSend.setProtocol("Request entry coalition");
		
		List <String> data = new ArrayList <String> ();
		data.add(((AbstractDedaleAgent)this.myAgent).getCurrentPosition()); // j'ajoute ma position pour que la coalition sache où je suis
		try {
			msgSend.setContentObject((Serializable) data);// envoie sa position (list)
		} catch (IOException e) {
			System.out.println("problem send list for entry coalition");
			e.printStackTrace();
		}
		
		msgSend.addReceiver(new AID(leader, AID.ISLOCALNAME));
		((AbstractDedaleAgent)this.myAgent).sendMessage(msgSend);	
		
		//suspention coalition
		this.activeCoalition = false;
	}
	
	//MAJ de se que vois l'agent
	private void updateViewAgent() {
		this.golemMove = true;
		//je reset mes donnée sur le golem (map)
		((ExploreSoloAgent)this.myAgent).getMap().resetPourcentGolem();
		List<Couple<String,List<Couple<Observation,Integer>>>> lobs=((AbstractDedaleAgent)this.myAgent).observe();//j'observe myPosition
		if(log) {
			System.out.println(this.myAgent.getLocalName() + " : Behaviour coalition vue par agent : " + lobs);
		}
		//je vais selectionner les node qui on un senteur de golem et mettre a jour la map
		Iterator<Couple<String, List<Couple<Observation, Integer>>>> iter = lobs.iterator();

		List <String> node_sent = new ArrayList<String>();
		while(iter.hasNext()){
			Couple<String, List<Couple<Observation, Integer>>> temp = iter.next();
			List<Couple<Observation, Integer>> couple = temp.getRight();
			String ID_node = temp.getLeft();
			//si je sens le golem
			if(couple.size() > 0) {
				node_sent.add(ID_node);
				// je mets a jour le noeud pour dire que je le sens							
				((ExploreSoloAgent)this.myAgent).getMap().setGolemDetection(ID_node, true, ((AbstractDedaleAgent)this.myAgent).getCurrentPosition());
				//si la senteur est sur ma case
				if(((AbstractDedaleAgent)this.myAgent).getCurrentPosition().equals(ID_node)) {
					//alors le golem n'a pas bougé
					this.golemMove = false;
				}
			}
			else {
				// je ne fait rien
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
				((ExploreSoloAgent)this.myAgent).getMap().setGolemDetection(ID_node2, false, ((AbstractDedaleAgent)this.myAgent).getCurrentPosition());
			}
		}
		//leader: met a jour (dans la coalition) la localisation de "senteur"
		if(((ExploreSoloAgent)this.myAgent).getLeaderCoalition()) {
			this.dataPositionGolem = ((ExploreSoloAgent)this.myAgent).getMap().listNodeGolemProba();//MAJ
			if(this.golemMove) {
				if(log) {
					System.out.println(this.myAgent.getLocalName() + " : proba golem leader: " + this.dataPositionGolem);
					System.out.println(this.myAgent.getLocalName() + " : Le golem à bougé!!!");
				}
				this.giveDataGolem();
			}
		}
	}
	
	public boolean done() {
		return finished;
	}
	
}
