package eu.su.mas.dedaleEtu.mas.behaviours;

import java.util.ArrayList;
import java.util.HashSet;

import eu.su.mas.dedale.mas.AbstractDedaleAgent;
import eu.su.mas.dedaleEtu.mas.agents.dummies.ExploreSoloAgent;
import eu.su.mas.dedaleEtu.mas.knowledge.MapRepresentation;
import jade.core.Agent;
import jade.core.behaviours.SimpleBehaviour;

public class PatrolSoloBehaviour extends SimpleBehaviour{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private MapRepresentation myMap;
	
	public PatrolSoloBehaviour(final ExploreSoloAgent myAgent, MapRepresentation myMap) {
		super(myAgent);
		this.myMap = myMap;
	}

	@Override
	public void action() {
		
	}

	@Override
	public boolean done() {
		return false;
	}

}
