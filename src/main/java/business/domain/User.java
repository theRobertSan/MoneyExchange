package main.java.business.domain;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;

public class User implements Serializable {

    private String name;
    private float balance;
    private HashMap<Integer, PaymentRequest> pendingPayments;
    private ArrayList<QRCodePayment> createdQRCodes;
    private ArrayList<Group> ownedGroups;
    private ArrayList<Group> participantGroups;

    // Constructors

    public User(String name, float balance) {
        this.name = name;
        this.balance = balance;
        this.pendingPayments = new HashMap<>();
        this.createdQRCodes = new ArrayList<>();
        this.ownedGroups = new ArrayList<>();
        this.participantGroups = new ArrayList<>();
    }

    public User(String name, float balance, HashMap<Integer, PaymentRequest> pendingPayments,
                ArrayList<QRCodePayment> createdQRCodes, ArrayList<Group> ownedGroups, ArrayList<Group> participantGroups) {
        this.name = name;
        this.balance = balance;
        this.pendingPayments = pendingPayments;
        this.createdQRCodes = createdQRCodes;
        this.ownedGroups = ownedGroups;
        this.participantGroups = participantGroups;
    }

    // Methods

    public void makePayment(User receivingUser, float amount) {
        this.balance -= amount;
        receivingUser.increaseBalance(amount);
    }

    public void increaseBalance(float amount) {
        this.balance += amount;
    }

    public void addPendingPayment(int id, float amount, User creator, GroupPayment groupPayment) {
        PaymentRequest newRequest = new PaymentRequest(id, amount, creator, groupPayment);
        pendingPayments.put(id, newRequest);
    }

    public void removeRequest(int reqID) {
        pendingPayments.remove(reqID);
    }

    public void payRequest(int reqID) {
        PaymentRequest pr = getPendingPayment(reqID);

        makePayment(pr.getCreator(), pr.getAmount());
        removeRequest(reqID);

        // If there is a group payment attached to the request, 
        // eliminate the current user from the owing list
        if (pr.isGroupPaymentRequest()) {
            GroupPayment gp = pr.getGroupPayment();
            gp.removeUser(this);
        }

    }

    public void addOwnedGroup(Group group) {
        ownedGroups.add(group);
    }

    public void addParticipantGroup(Group group) {
        participantGroups.add(group);
    }

    public boolean isOwner(int groupID) {
        boolean result = false;
        for (Group group : ownedGroups) {
            if (group.getID() == groupID) {
                result = true;
            }
        }
        return result;
    }

    public boolean inGroup(int groupID) {
        boolean result = false;
        for (Group group : participantGroups) {
            if (group.getID() == groupID) {
                result = true;
            }
        }
        return result;
    }


    public void addMemberToParticipantGroup(User user, int groupID) {
        Group group = user.getOwnedGroup(groupID);
        this.participantGroups.add(group);
    }

    public void addMemberToGroup(User owner, User userToAdd, int groupID) {
        Group group = owner.getOwnedGroup(groupID);
        group.addMember(userToAdd);
        userToAdd.participantGroups.add(group);
    }

    // Getters

    public String getID() {
        return this.name;
    }

    public void setID(String id) {
        this.name = id;
    }

    public float getBalance() {
        return this.balance;
    }

    public void setBalance(float balance) {
        this.balance = balance;
    }

    public HashMap<Integer, PaymentRequest> getPendingPayments() {
        return this.pendingPayments;
    }

    public void setPendingPayments(HashMap<Integer, PaymentRequest> pendingPayments) {
        this.pendingPayments = pendingPayments;
    }

    public ArrayList<QRCodePayment> getCreatedQRCodes() {
        return this.createdQRCodes;
    }

    public void setCreatedQRCodes(ArrayList<QRCodePayment> createdQRCodes) {
        this.createdQRCodes = createdQRCodes;
    }

    public ArrayList<Group> getOwnedGroups() {
        return this.ownedGroups;
    }

    // Setters

    public void setOwnedGroups(ArrayList<Group> ownedGroups) {
        this.ownedGroups = ownedGroups;
    }

    public ArrayList<Group> getParticipantGroups() {
        return this.participantGroups;
    }

    public void setParticipantGroups(ArrayList<Group> participantGroups) {
        this.participantGroups = participantGroups;
    }

    public Group getOwnedGroup(int groupID) {
        for (Group group : ownedGroups) {
            if (group.getID() == groupID) {
                return group;
            }
        }
        return null;
    }

    public Group getParticipantGroup(int groupID) {
        for (Group group : participantGroups) {
            if (group.getID() == groupID) {
                return group;
            }
        }
        return null;
    }

    public PaymentRequest getPendingPayment(int reqID) {
        return pendingPayments.get(reqID);
    }

}
