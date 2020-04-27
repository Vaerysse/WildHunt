package eu.su.mas.dedaleEtu.mas.behaviours;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

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
	private boolean log = false;
	
	private static final int wait = 2000;
	private String id_Coal;//id de la coalition (utilisé pour les echange de message)
	
	private List<String> members = new ArrayList <String> ();
	private int maxAgent;//max agent possible dans la coalition (dans update)
	private boolean candidatAgentOpen;// de nouveau agent peuvent entré dans la coalition (dans update)
	private String golemLocalisation;//ou se situe le plus probablement le golem pisté par la coalistion (dans update)
	private int stepMSG;//à quel numéro de message en est la coalition (ordre de placement) (dans update)
	private int numUpdate;//à quel update en est-on (dans update)
	private String fatherLastMSG;//quel est mon pére (rien pour le leader) (propre a chaque agent)
	private List<String> sonLastMSG = new ArrayList<String>();//quel sont mes fils (propre a chaque agent)
	private long timerWaitMessageCoalition;//temps d'attente max avant de sortir de la coalition si aucun nouveaux messages (propre a chaque agent)
	private boolean waitDataGolemAllAgent;//variable a true tant que tous les agents de la coalition non pas envoyé leur données sur la vision du golem (propre au leader)
	private HashMap<String, Double> dataPositionGolem;//stocke les valeur de position probable du golem envoyer par chaque agent (stack les valeurs de même position) (propre au leader)
	private List<String> waitAgentDataReturn = new ArrayList <String> ();//list des agents dont on attend la réponse (propre au leader)
	
	private long timer;
	
	public CoalitionBehaviour(final Agent myagent, String id) {
		super(myagent);

		this.id_Coal = id;
		this.members.add(this.myAgent.getLocalName());
		this.candidatAgentOpen = true;
		this.stepMSG = 1;
		this.numUpdate = 0;
		this.timerWaitMessageCoalition = 10000;
		this.waitDataGolemAllAgent = false;
		
		// Nombre agent max dans coalition
		this.maxAgent = ((ExploreSoloAgent)this.myAgent).getMap().getMaxDegree(); // degré max du graphe
		//this.maxAgent = ((ExploreSoloAgent)this.myAgent).getMap().getAvDegree(); // degré moyen du graphe
		
		//commencer à remplir dataPositionGolem
		dataPositionGolem = ((ExploreSoloAgent)this.myAgent).getMap().listNodeGolemProba();
		
	}
	
	@Override
	public void action() {
		
		System.out.println(this.myAgent.getLocalName() + " : Behaviour de coalition " + this.id_Coal + " actif");
		
		//Template message neutre, utilisé actuellement pour:
		//Les demande et reponse pour entré dans la coalition
		final MessageTemplate msgTemplate = MessageTemplate.and(MessageTemplate.MatchPerformative(ACLMessage.INFORM), 
				MessageTemplate.MatchProtocol(this.id_Coal));			
		final ACLMessage msg = this.myAgent.receive(msgTemplate);
		
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
		
		
		// partie réservé au leader
		if (((ExploreSoloAgent)this.myAgent).getLeaderCoalition()){
			//de nouveau agent peuvent encore venir dans la coalition
			if(this.candidatAgentOpen) {
				if(msg != null) {
					//reception d'une demande d'entré dans la coalition
					if (msg.getContent().equals("RequestEntry?")) {
						this.addAgentCoalition(msg);
					}
				}			
				//reception d'une demande de localisation de golem
				if (msgGolemPosition != null) {
					this.golemPosition(msg);
				}			
				//reception d'une demande de nombre d'agent
				if (msgnbAgent != null) {
					this.nbAgentCoalition(msg);
				}			
				//si la coalition est pleine
				if (this.members.size() >= this.maxAgent) {
					//on ferme le droit d'inscription
					this.candidatAgentOpen = false;
					((ExploreSoloAgent)this.myAgent).setInCoalitionFull(true);
					//dit aux autre agents d'arréter le recrutement
					this.sendCoalitionFull();
					//Demande à tous les agents de la coalition si ils voient un golem actuellement et récup data
					this.giveDataGolem();
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
						//si il y a des donnée valable
						if(!data.get("data").equals("-1")){
							//faire une boucle sur les key pour récupéré les double (sous forme de string) a intégré a chaque noeud
							for(String nodeID : data.keySet()) {
								//permet de prendre uniquement les id node
								if(nodeID != "data" && nodeID != "agent") {
									this.addValueDataPositionGolem(nodeID,(Double) parseDouble(data.get(nodeID)));
								}
							}
						}
						//retirer l'agent de la liste d'attente de retour de donnée
						this.waitAgentDataReturn.remove(data.get("agent"));
					}
				}
				//si plus de message en attente on calcule ou peut se trouver le golem
				if(!waitDataGolemAllAgent) {
					//definir ou se trouve le golem et mettre a jour
					//TODO
					//envoie de l'update du behaviour
					this.sendUpdateDataBehaviour();
					//calcule de la postion a donnée a chaque agent
					//TODO
					//envoie de la position que doit avoir les agents pour capturer le golem
					//TODO
				}
			}
		}
		
		//Autre agent et leader	
		//TODO
		//je recoit une update des données du behaviour (penser a sup say golem si candidatAgentOpen=false)
		//je reçoit un message du leader a transmetre à mes fils (dans la coalition)
		//je reçoit un message du leader qui me donne un position pour choper le golem (et donner leur position à mes fils)
		//je recoit un message de mon fils de coalition
		//je recoit un message une demande de rentré dans la coalition (a transmetre à mon leader/pére)			
		//je recoit de mon fils les données ou il seent le golem (transmetre au leader/pére)
		//je recoit de mon fils qu'il est arriver en position X (transmetre au leader/pére)
		//je transmet au leader/pére là ou je sent le golem
		//je transmet ma position au leader/pére
		
	}
	
	private Double parseDouble(String string) {
		// TODO Auto-generated method stub
		return null;
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
			msgRespond.setContent("ok");
			this.members.add(msg.getSender().getLocalName());
		}//sinon si pleine
		else {
			if(log) {
				System.out.println("envoi message coalition non");
			}
			msgRespond.setContent("no");
		}
		msgRespond.addReceiver(new AID(msg.getSender().getLocalName(),AID.ISLOCALNAME));
		((AbstractDedaleAgent)this.myAgent).sendMessage(msgRespond);	
	}	
	
	//envoie du nombre d'agent dans la coalition
	private void nbAgentCoalition(ACLMessage msg) {
		ACLMessage msgRespond = new ACLMessage(ACLMessage.INFORM);
		msgRespond.setSender(this.myAgent.getAID());
		msgRespond.setProtocol(this.id_Coal + ": golem position");
		msgRespond.setContent("" + this.members.size());
		msgRespond.addReceiver(new AID(msg.getSender().getLocalName(),AID.ISLOCALNAME));
		((AbstractDedaleAgent)this.myAgent).sendMessage(msgRespond);
	}
	
	//envoi de la position probablee du golem suivis par la coalition
	private void golemPosition(ACLMessage msg) {
		ACLMessage msgRespond = new ACLMessage(ACLMessage.INFORM);
		msgRespond.setSender(this.myAgent.getAID());
		msgRespond.setProtocol(this.id_Coal + ": nb Agent");
		msgRespond.setContent(this.golemLocalisation);
		msgRespond.addReceiver(new AID(msg.getSender().getLocalName(),AID.ISLOCALNAME));
		((AbstractDedaleAgent)this.myAgent).sendMessage(msgRespond);
	}
	
	//envoie message string a une liste spécifique d'agent
	private void sendMessageStringCoalition(String protocol, List<String> agents, String content) {
		ACLMessage msgSend = new ACLMessage(ACLMessage.INFORM);
		msgSend.setSender(this.myAgent.getAID());
		msgSend.setProtocol(protocol);
		msgSend.setContent(content);		
		for(int i = 0; i< agents.size(); i++) {
			msgSend.addReceiver(new AID(agents.get(i),AID.ISLOCALNAME));
		}		
		((AbstractDedaleAgent)this.myAgent).sendMessage(msgSend);		
	}
	
	//envoie message object a une liste spécifique d'agent
	private void sendMessageObjectCoalition(String protocol, List<String> agents, Object content) {
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
		for(int i = 0; i< agents.size(); i++) {
			msgSend.addReceiver(new AID(agents.get(i),AID.ISLOCALNAME));
		}		
		((AbstractDedaleAgent)this.myAgent).sendMessage(msgSend);		
	}
	
	//permet de définir ou mettre les agents pour capturer le golem
	private void calculPositionCaptureGolem() {
		//TODO
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
		update.put("members", this.members);
		
		List<String> updatemaxAgent = new ArrayList<String>();
		updatemaxAgent.add(""+this.maxAgent);
		update.put("maxAgent", updatemaxAgent);
		
		List<String> updatecandidatAgentOpen = new ArrayList<String>();
		updatemaxAgent.add(""+this.candidatAgentOpen);
		update.put("candidatAgentOpen", updatecandidatAgentOpen);

		List<String> updategolemLocalisation = new ArrayList<String>();
		updatemaxAgent.add(""+this.golemLocalisation);
		update.put("golemLocalisation", updategolemLocalisation);

		List<String> updatestepMSG = new ArrayList<String>();
		updatemaxAgent.add(""+this.stepMSG);
		update.put("stepMSG", updatestepMSG);
		
		this.numUpdate += 1;
		List<String> updatenumUpdate = new ArrayList<String>();
		updatemaxAgent.add(""+this.numUpdate);
		update.put("numUpdate", updatenumUpdate);
		
		this.sendMessageObjectCoalition(this.id_Coal + ": Update behaviour", this.members, update);
	}
	
	//demande aux autre agents ou ils sentent le golem
	private void giveDataGolem() {
		this.sendMessageStringCoalition(this.id_Coal + ": golem hunt strat", this.members, "Give your data golem!!!!");
	}
	
	//dit aux autre agents d'arréter le recrutement
	private void sendCoalitionFull() {
		this.sendMessageStringCoalition(this.id_Coal + ": Coalition full", this.members, "stop SayGolem");
	}
	
	public boolean done() {
		return finished;
	}
	
}
