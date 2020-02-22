package eu.su.mas.dedaleEtu.mas.behaviours;

import java.io.IOException;

import eu.su.mas.dedale.mas.AbstractDedaleAgent;
import eu.su.mas.dedaleEtu.mas.knowledge.MapRepresentation;
import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.SimpleBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

public class PrivateCanalBehaviour extends SimpleBehaviour{

	private static final long serialVersionUID = 9088209402507795292L;
	
	private boolean finished;
	
	private boolean exchange;
	private boolean connection;
	private boolean ACKmap;
	
	private int stepProtocol;
	
	private String receiverName;
	
	/**
	 * Current knowledge of the agent regarding the environment
	 */
	private MapRepresentation myMap;
	
	/**
	 * 
	 * Canal priver pour échange d'information entre deux agents
	 * @param myagent
	 * @param receiverName The local name of the receiver agent
	 */
	public PrivateCanalBehaviour(final Agent myagent, String receiverName, MapRepresentation myMap) {
		super(myagent);
		
		this.myMap = myMap;
		this.receiverName = receiverName;
		this.finished = false;
		this.connection = true;
		this.exchange = false;
		this.ACKmap = false;
		this.stepProtocol = 1;
		//ajouter un timer pour éviter les attentes à l'infini
		
	}
	
	public void action() {
		if (this.stepProtocol == 1){
			//J'envoie un message de demande de transmission d'information
			ACLMessage msg=new ACLMessage(ACLMessage.INFORM);
			msg.setSender(this.myAgent.getAID());
			msg.setProtocol("ExchangeProtocol");
			msg.setContent("connection");
			msg.addReceiver(new AID(this.receiverName,AID.ISLOCALNAME));
			
			((AbstractDedaleAgent)this.myAgent).sendMessage(msg);
			System.out.println("Envoie message connection");
		}
		else if (this.stepProtocol == 2) {
			ACLMessage msg=new ACLMessage(ACLMessage.INFORM);
			msg.setSender(this.myAgent.getAID());
			msg.setProtocol("ExchangeProtocol");
			msg.addReceiver(new AID(this.receiverName,AID.ISLOCALNAME));
			try {
				msg.setContentObject(this.myMap);
			} catch (IOException e) {
				msg.setContent("-1");
				System.out.println("Pb serialisation map");
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			((AbstractDedaleAgent)this.myAgent).sendMessage(msg);
			System.out.println("envoie message map");
		}
		else if (this.stepProtocol == 3) {
			ACLMessage msg=new ACLMessage(ACLMessage.INFORM);
			msg.setSender(this.myAgent.getAID());
			msg.setProtocol("ExchangeProtocol");
			msg.setContent("ACKmap");
			msg.addReceiver(new AID(this.receiverName,AID.ISLOCALNAME));
			
			((AbstractDedaleAgent)this.myAgent).sendMessage(msg);
			System.out.println("envoie message ACK");
		}
			
		if(this.connection) {
			MessageTemplate msgTemplate = MessageTemplate.and(MessageTemplate.and(MessageTemplate.MatchPerformative(ACLMessage.INFORM), 
					MessageTemplate.MatchSender(new AID(this.receiverName, AID.ISLOCALNAME))),
					MessageTemplate.MatchProtocol("ExchangeProtocol"));			

			ACLMessage msgReceived = this.myAgent.receive(msgTemplate);
			System.out.println("regarde la reception message connection");
			if (msgReceived != null) {
				System.out.println("pk?");
				System.out.println(msgReceived.getContent());
				if (msgReceived.getContent().equals("connection")) {
					this.connection = false;
					this.exchange = true;
					this.stepProtocol += 1;
					System.out.println("message connection ok");
				}
				else if (msgReceived.getContent() == "-1" ) {
					System.out.println("Probleme connection");
					this.finished = true;
				}
			}
		}
		else if (this.exchange) {
			MessageTemplate msgTemplate = MessageTemplate.and(MessageTemplate.and(MessageTemplate.MatchPerformative(ACLMessage.INFORM), 
					MessageTemplate.MatchSender(new AID(this.receiverName, AID.ISLOCALNAME))),
					MessageTemplate.MatchProtocol("ExchangeProtocol"));			

			ACLMessage msgReceived = this.myAgent.receive(msgTemplate);
			System.out.println("Recu carte");
			this.exchange = false;
			this.stepProtocol += 1;
			this.ACKmap = true;
			if (msgReceived != null) {
				if (msgReceived.getContent() == "-1" ) {
					System.out.println("Probleme recu carte");
					this.finished = true;
				}
			}
		}
		else if (this.ACKmap && this.stepProtocol == 3) {
			MessageTemplate msgTemplate = MessageTemplate.and(MessageTemplate.and(MessageTemplate.MatchPerformative(ACLMessage.INFORM), 
					MessageTemplate.MatchSender(new AID(this.receiverName, AID.ISLOCALNAME))),
					MessageTemplate.MatchProtocol("ExchangeProtocol"));			

			ACLMessage msgReceived = this.myAgent.receive(msgTemplate);
			System.out.println(this.myAgent.getLocalName() + " : regarde la receptioon message ACK : " + this.receiverName);
			if (msgReceived != null) {
				if (msgReceived.getContent().equals("ACKmap") ) {
					System.out.println(this.myAgent.getLocalName() + " : ACK ok : " + this.receiverName);
					this.finished = true;
				}
				else if (msgReceived.getContent() == "-1" ) {
					System.out.println("Probleme recu ACKmap");
					this.finished = true;
				}
			}
		}
		
	}
	
	public boolean done() {
		return finished;
	}
}
