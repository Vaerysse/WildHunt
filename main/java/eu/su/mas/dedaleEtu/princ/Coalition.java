package eu.su.mas.dedaleEtu.princ;
import java.util.List;

import eu.su.mas.dedaleEtu.mas.agents.dummies.ExploreSoloAgent;
import java.util.HashMap;

public class Coalition {

	private int id;
	
	/**
	 * Chaque agent possède un rôle au sein de la coalition, représenté par un entier :
	 * - 0 => leader
	 * - 1 => le reste
	 */
	//TODO: est-ce qu'il est nécessaire de rajouter des rôles ? Par exemple répétiteur pr propager l'information qd les agents sont trop éloignés ?
	// Est-ce qu'il est nécessaire que l'objet soit sérialisable (est-ce qu'on va avoir besoin de l'envoyer à un moment ?) Si oui changer l'agent en String
	private HashMap<ExploreSoloAgent, Integer> members;

	
	
	public Coalition(ExploreSoloAgent agent) {
		this.members = new HashMap<ExploreSoloAgent, Integer>();
		addMember(agent, 0);
	}
	
	
	// une méthode pour élire le leader ?
	// besoin d'un autre constructeur ?
	// une méthode de fusion de coalition
	// une méthode add/deleteMembers si on en a besoin
	
	
	public int getID() {
		return this.id;
	}
	
	public HashMap<ExploreSoloAgent, Integer> getMembers() {
		return this.members;
	}
	
	public int addMember(ExploreSoloAgent agent, int role) {
		this.members.put(agent, role);
		return members.size();
	}
	
	public int deleteMember(ExploreSoloAgent agent) {
		this.members.remove(agent);
		return members.size();
	}
	
}
