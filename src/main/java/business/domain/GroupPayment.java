package main.java.business.domain;

import java.io.Serializable;
import java.util.ArrayList;

public class GroupPayment implements Serializable {

    private int id;
    private float totalAmount;
    private Group group;

    private ArrayList<PaymentRequest> paymentRequests;

    private ArrayList<User> members;
    private ArrayList<User> owingMembers;

    // Constructors

    public GroupPayment(int id, float totalAmount, Group group, ArrayList<User> members) {
        this.id = id;
        this.totalAmount = totalAmount;
        this.group = group;
        this.members = (ArrayList<User>) members.clone();
        this.owingMembers = (ArrayList<User>) members.clone();
    }

    // Getters

    public float getTotalAmount() {
        return this.totalAmount;
    }

    public void setTotalAmount(float totalAmount) {
        this.totalAmount = totalAmount;
    }

    public int getID() {
        return this.id;
    }

    public Group getGroup() {
        return this.group;
    }

    public void setGroup(Group group) {
        this.group = group;
    }

    // Setters

    public ArrayList<User> getOwingMembers() {
        return this.owingMembers;
    }

    public void setOwingMembers(ArrayList<User> owingMembers) {
        this.owingMembers = owingMembers;
    }

    public void setid(int id) {
        this.id = id;
    }

    public ArrayList<User> getMembers() {
        return this.members;
    }

    public void setMembers(ArrayList<User> members) {
        this.members = members;
    }

    public String dividePayment(int id, float amount) {

        group.addActivePayment(this);

        StringBuilder sb = new StringBuilder();
        sb.append(String.format("Payment of %.2f€ successfully divided between the group members!", amount));

        float dividedAmount = amount / (this.group.getMembers().size() + 1);

        for (int i = 0; i < owingMembers.size(); i++) {
            User user = owingMembers.get(i);
            user.addPendingPayment(id + i, dividedAmount, this.group.getOwner(), this);
            sb.append(String.format("\nSent Payment request ID: %d of %.2f€ to %s", id + i, dividedAmount, user.getID()));
        }

        return sb.toString();
    }

    // Functions

    public void removeUser(User user) {
        this.owingMembers.remove(user);

        if (this.owingMembers.isEmpty()) {
            this.group.groupPaymentFinished(this);
        }
    }
}
