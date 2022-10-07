package main.java.business.domain;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;

public class Group implements Serializable {

    private int id;
    private User owner;
    private ArrayList<User> members;
    private HashMap<Integer, GroupPayment> activePayments;
    private HashMap<Integer, GroupPayment> finalizedPayments;

    // Constructors

    public Group(int groupID, User owner) {
        this.id = groupID;
        this.owner = owner;
        this.members = new ArrayList<>();
        this.activePayments = new HashMap<>();
        this.finalizedPayments = new HashMap<>();
    }

    // Methods

    public void addMember(User user) {
        this.members.add(user);
        // Add group to users participant groups
        user.addParticipantGroup(this);
    }

    public void groupPaymentFinished(GroupPayment gp) {
        activePayments.remove(gp.getID());
        finalizedPayments.put(gp.getID(), gp);
    }

    public void addActivePayment(GroupPayment groupPayment) {
        activePayments.put(groupPayment.getID(), groupPayment);
    }

    // Getters

    public int getID() {
        return this.id;
    }

    public User getOwner() {
        return this.owner;
    }

    public ArrayList<User> getMembers() {
        return this.members;
    }

    public HashMap<Integer, GroupPayment> getActivePayments() {
        return this.activePayments;
    }

    public HashMap<Integer, GroupPayment> getFinalizedPayments() {
        return this.finalizedPayments;
    }

}
