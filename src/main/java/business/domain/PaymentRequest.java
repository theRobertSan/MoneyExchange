package main.java.business.domain;

import java.io.Serializable;

public class PaymentRequest implements Serializable {

    private int id;
    private float amount;
    private User creator;
    private GroupPayment groupPayment;

    // Constructor

    public PaymentRequest(int id, float amount, User creator, GroupPayment groupPayment) {
        this.id = id;
        this.amount = amount;
        this.creator = creator;
        this.groupPayment = groupPayment;
    }

    // Getters

    public int getId() {
        return this.id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public float getAmount() {
        return this.amount;
    }

    public void setAmount(int amount) {
        this.amount = amount;
    }

    // Setters

    public User getCreator() {
        return this.creator;
    }

    public void setCreator(User creator) {
        this.creator = creator;
    }

    public GroupPayment getGroupPayment() {
        return this.groupPayment;
    }

    public void setGroupPayment(GroupPayment groupPayment) {
        this.groupPayment = groupPayment;
    }

    // Functions

    public boolean isGroupPaymentRequest() {
        return !(groupPayment == null);
    }

}
