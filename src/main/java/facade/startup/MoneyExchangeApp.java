package main.java.facade.startup;

import main.java.business.catalogs.GroupIDRepository;
import main.java.business.catalogs.QRCodeRepository;
import main.java.business.catalogs.UserRepository;
import main.java.business.domain.*;
import main.java.facade.exceptions.ApplicationException;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Scanner;

public class MoneyExchangeApp {
    private static final String REQ_ID_FILENAME = "./resources/reqid.txt";

    private UserRepository usersData;
    private GroupIDRepository groupIDs;
    private QRCodeRepository qrCodePayments;

    private int paymentRequestID;

    // Current user
    private User user;

    public MoneyExchangeApp(String username, UserRepository usersData, GroupIDRepository groupIDs, QRCodeRepository qrCodePayments, int paymentRequestID) {
        // Get user based on username and file
        user = usersData.getUser(username);

        this.usersData = usersData;
        this.groupIDs = groupIDs;
        this.qrCodePayments = qrCodePayments;
        this.paymentRequestID = paymentRequestID;
    }

    /**
     * Get the current user's balance
     *
     * @return a string message indicating the balance
     */
    public String getBalance() {
        return String.format("Current Balance: %.2f €", user.getBalance());
    }

    /**
     * Transfer money to another user
     *
     * @param userID - Receiving user id
     * @param amount - Amount to transfer
     * @return a string message indicating the success of the operation
     * @throws ApplicationException
     */
    public String makePayment(String userID, float amount) throws ApplicationException {

        if (amount < 0) {
            throw new ApplicationException("Error: Can't make a payment of 0 or less");
        }

        if (userID.equals(user.getID())) {
            throw new ApplicationException("Error: Can't make a payment to yourself!");
        }

        // Get user who'll receive the money
        User receivingUser = usersData.getUser(userID);

        // User doesn't exist
        if (receivingUser == null) {
            throw new ApplicationException(String.format("Error: User %s not found.", userID));
        }

        // User doesn't have funds
        if (user.getBalance() < amount) {
            throw new ApplicationException(String.format("Error: Not enough funds to perform payment to user %s.", userID));
        }

        user.makePayment(receivingUser, amount);
        return String.format("Payment of %.2f € to user %s was successful! Current Balance: %.2f €", amount, userID, user.getBalance());
    }

    /**
     * Send a payment request to another user
     *
     * @param userID       - Receiving user id
     * @param amount       - Amount for the request
     * @param groupPayment - Associated group payment, if exists
     * @return a string message indicating the success of the operation
     * @throws IOException
     * @throws ApplicationException
     */
    public String requestPayment(String userID, float amount, GroupPayment groupPayment) throws IOException, ApplicationException {

        if (amount < 0) {
            throw new ApplicationException("Error: Can't make a request of 0 or less");
        }

        if (user.getID().equals(userID)) {
            throw new ApplicationException("Error: Can't make request to self");
        }

        // Get user who'll receive the request
        User payingUser = usersData.getUser(userID);

        if (payingUser == null) {
            throw new ApplicationException(String.format("Error: User %s not found.", userID));
        }

        getReqID(REQ_ID_FILENAME);
        payingUser.addPendingPayment(paymentRequestID, amount, user, groupPayment);
        paymentRequestID++;
        updateReqID();

        return String.format("Payment request of %.2f € sent to %s successfully!", amount, userID);
    }

    /**
     * View pending requests
     *
     * @return a string containing all pending requests
     */
    public String viewRequests() {
        HashMap<Integer, PaymentRequest> pendingPayments = user.getPendingPayments();

        if (pendingPayments.isEmpty()) {
            return "There are no pending payments.";
        }

        StringBuilder sb = new StringBuilder();

        sb.append("Pending payments:");
        for (PaymentRequest payment : pendingPayments.values()) {
            sb.append(String.format("\nID: %d | Amount: %.2f € | Receiver: %s", payment.getId(), payment.getAmount(), payment.getCreator().getID()));
        }

        return sb.toString();
    }

    /**
     * Pay a request previously received
     *
     * @param reqID - request id
     * @return a string message indicating the success of the operation
     * @throws ApplicationException
     */
    public String payRequest(int reqID) throws ApplicationException {

        PaymentRequest payment = user.getPendingPayment(reqID);

        if (payment == null) {
            throw new ApplicationException(String.format("Error: Request %d not found.", reqID));
        }

        float amount = payment.getAmount();

        // User doesn't have enough funds
        if (user.getBalance() < amount) {
            throw new ApplicationException(String.format("Error: Insufficient funds to pay payment request %d.", reqID));
        }

        user.payRequest(reqID);
        return String.format("Payment request of %.2f € to user %s was successful! Current Balance: %.2f €", amount, payment.getCreator().getID(), user.getBalance());
    }

    /**
     * Create a QRCode payment request
     *
     * @param amount - Amount associated to QRCode payment request
     * @return a string message indicating the success of the operation
     * @throws IOException
     */
    public String obtainQRCode(float amount) throws IOException {

        getReqID(REQ_ID_FILENAME);
        QRCodePayment qrCodePayment = new QRCodePayment(paymentRequestID, amount, user.getID());
        paymentRequestID++;
        updateReqID();

        qrCodePayments.addQRCodePayment(qrCodePayment);

        return "QR Code created!";
    }

    /**
     * Pay a QRCode payment request
     *
     * @param id - The id obtained from reading the QRCode of a payment request
     * @return a string message indicating the success of the operation
     * @throws ApplicationException
     */
    public String confirmQRCode(int id) throws ApplicationException {

        QRCodePayment qrPayment = qrCodePayments.getQRCodePayment(id);

        if (qrPayment == null) {
            throw new ApplicationException("Error: Code does not represent a QR Code Payment!");
        }

        String result = makePayment(qrPayment.getCreatorID(), qrPayment.getAmount());
        qrCodePayments.removeQRCodePayment(id);

        return result;
    }

    /**
     * Create a new group
     *
     * @param groupID - The new group's group id
     * @return a string message indicating the success of the operation
     */
    public String createGroup(int groupID) throws ApplicationException {
        // Group with groupID already exists
        if (groupIDs.exists(groupID)) {
            throw new ApplicationException(String.format("Error: Group with ID: %d already exists.", groupID));
        }

        Group group = new Group(groupID, user);
        user.addOwnedGroup(group);
        groupIDs.add(groupID);
        return "Created new group successfully!";
    }

    /**
     * Add a user to a group
     *
     * @param userID  - User id of user to add
     * @param groupID - Group id of the group where we want to add user
     * @return a string message indicating the success of the operation
     */
    public String addUser(String userID, int groupID) throws ApplicationException {

        // Adding yourself is not permitted
        if (user.getID().equals(userID)) {
            throw new ApplicationException("Error: You can't add yourself to a group.");
        }

        // Get user who'll receive the money
        User toAddUser = usersData.getUser(userID);

        if (toAddUser == null) {
            throw new ApplicationException(String.format("Error: User %s not found.", userID));
        }

        Group g = user.getOwnedGroup(groupID);

        if (g == null) {

            // User is not owner of group and group exists
            if (groupIDs.exists(groupID)) {
                throw new ApplicationException("Error: You are not the submitted group's owner.");
            }

            // Group does not exist
            throw new ApplicationException(String.format("Error: Group with ID: %d doesn't exist.", groupID));
        }

        User userToAdd = usersData.getUser(userID);

        // User to add is already in this group
        if (userToAdd.inGroup(groupID)) {
            throw new ApplicationException(String.format("Error: User %s is already in group %d.", userID, groupID));
        }

        g.addMember(userToAdd);
        return String.format("User %s successfully added to group %d!", userID, groupID);
    }

    /**
     * Display all groups where the user is in, either as a owner or member
     *
     * @return a string containing the owned groups and participating groups
     */
    public String displayGroups() {
        ArrayList<Group> ownedGroups = user.getOwnedGroups();
        ArrayList<Group> participantGroups = user.getParticipantGroups();

        StringBuilder sb = new StringBuilder();

        sb.append("Owned groups:");
        if (!ownedGroups.isEmpty()) {
            for (Group group : ownedGroups) {
                sb.append(String.format("\nGroup ID %d | Members: ", group.getID()));

                if (group.getMembers().isEmpty()) {
                    sb.append("No members!");
                } else {
                    for (User member : group.getMembers()) {
                        sb.append(String.format("%s - ", member.getID()));
                    }

                }

            }
            sb.append("\n\n");
        } else {
            sb.append("\nYou do not own any groups.\n\n");
        }

        sb.append("Participating groups:");
        if (!participantGroups.isEmpty()) {
            for (Group group : participantGroups) {
                sb.append(String.format("\nGroup ID %d | Group Owner: %s | Members: ", group.getID(), group.getOwner().getID()));

                for (User member : group.getMembers()) {
                    sb.append(String.format("%s - ", member.getID()));
                }
            }
        } else {
            sb.append("\nYou are not in any group as a member.");
        }

        return sb.toString();
    }

    /**
     * Split a payment between groups users
     *
     * @param groupID - Group id
     * @param amount  - Amount to be split evenly between members
     * @return a string describing the success of the split and the individual sent payment requests
     * @throws IOException
     */
    public String dividePayment(int groupID, float amount) throws IOException, ApplicationException {

        if (amount < 0) {
            throw new ApplicationException("Error: Can't divide a payment of 0 or less");
        }

        Group g = user.getOwnedGroup(groupID);

        if (g == null) {

            // User is not owner of group and group exists
            if (groupIDs.exists(groupID)) {
                throw new ApplicationException("Error: You are not the submitted group's owner.");
            }

            // Group does not exist
            throw new ApplicationException(String.format("Error: Group with ID: %d doesn't exist.", groupID));
        }

        if (g.getMembers().isEmpty()) {
            throw new ApplicationException("Error: Cannot divide payment because group is empty.");
        }

        getReqID(REQ_ID_FILENAME);
        GroupPayment gp = new GroupPayment(paymentRequestID, amount, g, g.getMembers());
        paymentRequestID++;

        String result = gp.dividePayment(paymentRequestID, amount);
        // Group request has id and every single request generated to every member will also have an id
        paymentRequestID += g.getMembers().size();
        updateReqID();

        return result;
    }

    /**
     * Get pending group payments of a group
     *
     * @param groupID - Group id
     * @return a string containing the pending payments of group with group id
     */
    public String statusPayments(int groupID) throws ApplicationException {

        Group g = user.getOwnedGroup(groupID);

        if (g == null) {

            // User is not owner of group and group exists
            if (groupIDs.exists(groupID)) {
                throw new ApplicationException("Error: You are not the submitted group's owner.");
            }

            // Group does not exist
            throw new ApplicationException(String.format("Error: Group with ID: %d doesn't exist.", groupID));
        }

        HashMap<Integer, GroupPayment> activePayments = g.getActivePayments();

        if (activePayments.isEmpty()) {
            return "No active payments!";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("Status:");

        for (GroupPayment gp : activePayments.values()) {
            sb.append(String.format("\nGroup Payment ID: %d", gp.getID()));
            sb.append("\nHasn't Payed: ");

            for (User u : gp.getOwingMembers()) {
                sb.append(String.format("%s |", u.getID()));
            }
        }

        return sb.toString();
    }

    /**
     * Get finalized group payments of a group
     *
     * @param groupID - Group id
     * @returna string containing the finalized payments of group with group id
     */
    public String history(int groupID) throws ApplicationException {

        Group g = user.getOwnedGroup(groupID);

        if (g == null) {

            // User is not owner of group and group exists
            if (groupIDs.exists(groupID)) {
                throw new ApplicationException("Error: You are not the submitted group's owner.");
            }

            // Group does not exist
            throw new ApplicationException(String.format("Error: Group with ID: %d doesn't exist.", groupID));
        }

        HashMap<Integer, GroupPayment> finalizedPayments = g.getFinalizedPayments();

        if (finalizedPayments.isEmpty()) {
            return "There aren't any finalized payments!";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("Finalized group payments:");

        for (GroupPayment gp : finalizedPayments.values()) {
            sb.append(String.format("\nGroup Payment of ID: %d with amount of %.2f €\nMembers: ", gp.getID(), gp.getTotalAmount()));

            for (User u : gp.getMembers()) {
                sb.append(String.format("%s |", u.getID()));
            }
        }

        return sb.toString();
    }

    // Storage methods

    public void update(String password) throws IOException, ClassNotFoundException, NoSuchAlgorithmException, InvalidKeySpecException, NoSuchPaddingException, IllegalBlockSizeException, BadPaddingException, InvalidKeyException, InvalidAlgorithmParameterException {
        usersData.getFromFile(password);
        groupIDs.getFromFile(password);
        qrCodePayments.getFromFile(password);
        user = usersData.getUser(user.getID());
    }

    public void save(String password) throws IOException, NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeySpecException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException {
        usersData.saveToFile(password);
        groupIDs.saveToFile(password);
        qrCodePayments.saveToFile(password);
    }

    private void getReqID(String fileName) throws FileNotFoundException {
        File reqIDFile = new File(fileName);
        Scanner sc = new Scanner(reqIDFile);
        int reqID = Integer.parseInt(sc.nextLine());
        sc.close();
        paymentRequestID = reqID;
    }

    private void updateReqID() throws IOException {
        FileWriter writer = new FileWriter(REQ_ID_FILENAME);
        writer.write(String.valueOf(paymentRequestID));
        writer.close();
    }

}
