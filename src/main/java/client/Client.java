package main.java.client;

import main.java.utils.Constants;

import javax.net.SocketFactory;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.ByteBuffer;
import java.security.*;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.util.Scanner;

/**
 * Client class that connects to the Money Exchange Server
 */
public class Client {

    public static void main(String[] args) throws IOException {

        Scanner sc = new Scanner(System.in);

        String ip = "";
        int port = Constants.DEFAULT_PORT;
        String truststore = "";
        String keystore = "";
        String keystorePassword = "";
        String user = "";

        // Correct number of args provided
        if (args.length == 5) {

            String serverAddress = args[0];
            truststore = args[1];
            keystore = args[2];
            keystorePassword = args[3];
            user = args[4].toLowerCase();

            // If port is provided
            if (serverAddress.contains(":")) {
                String[] serverAddressSplit = serverAddress.split(":");
                ip = serverAddressSplit[0];
                port = Integer.parseInt(serverAddressSplit[1]);
            } else {
                ip = serverAddress;
            }

        } else {
            System.out.println("Wrong number of parameters!");
            System.out.println("Run Example: java Trokos 127.0.0.1:45678 truststore.server user1Keystore.client 123456 user1");
            System.exit(-1);
        }

        SSLSocket s = null;
        ObjectOutputStream out = null;
        ObjectInputStream in = null;

        try {
            System.setProperty("javax.net.ssl.trustStore", truststore);
            System.setProperty("javax.net.ssl.trustStorePassword", "123456");
            SocketFactory sf = SSLSocketFactory.getDefault();
            s = (SSLSocket) sf.createSocket(ip, port);

            in = new ObjectInputStream(s.getInputStream());
            out = new ObjectOutputStream(s.getOutputStream());

            // Send user id
            out.writeObject(user);

            // Check if user is already active
            checkStatus(in, "\n> User " + user + " is already active.");

            // Check if user is registered
            boolean exists = (boolean) in.readObject();
            System.out.printf("%s user [%s]%n", exists ? "Signing in" : "Registering", user);

            // Receive nonce from server
            long nonce = (long) in.readObject();
            System.out.println("Nonce received!");

            // Register path
            if (!exists) {
                // Send nonce back
                out.writeObject(nonce);
                System.out.println("Nonce sent back!");
                checkStatus(in, "\n> Nonce sent doesn't match!");
            }
            sendEncryptedNonce(exists, keystore, keystorePassword, nonce, out);
            checkStatus(in, "\n> Error checking for credentials");

            // From here, user is logged in
            displayMenu();

            while (true) {

                System.out.print("\nSelect An Option > ");
                // Read command from user
                String command = sc.nextLine().toLowerCase();
                String[] commandParts = command.split(" ");
                // Get command type
                String receivingUser = "";
                String value = "";
                boolean validTransaction = false;

                // Save receiving user and value if makepayment
                if (isValidMakePayment(commandParts)) {
                    validTransaction = true;
                    // We don't know if this user exists
                    receivingUser = commandParts[1];
                    value = commandParts[2];
                    // Send v to server and find payment id, so that you can then store receiving user and value
                } else if (isValidPayRequest(commandParts)) {
                    out.writeObject("v");
                    String[] requests = ((String) in.readObject()).split("\n");

                    for (String request : requests) {
                        String[] requestParts = request.split(" ");
                        // If it's our id, save
                        if (requestParts[1].equals(commandParts[1])) {
                            validTransaction = true;
                            receivingUser = requestParts[8];
                            value = requestParts[4];
                            break;
                        }
                    }
                }

                // Send command
                out.writeObject(command);

                // Wait for information if command is confirmqrcode
                if (isValidConfirmQRCode(commandParts)) {
                    String[] qrCodeInformation = ((String) in.readObject()).split("-");
                    if (!qrCodeInformation[0].equals("null")) {
                        validTransaction = true;
                        receivingUser = qrCodeInformation[0];
                        value = qrCodeInformation[1];
                    }
                }

                // If it's transaction and valid, sign and send
                if (isTransaction(commandParts)) {

                    if (!validTransaction) {
                        System.out.println("Error: There was an issue with this transaction. Make sure the id or user exists!");
                        out.writeObject("invalid");
                    } else {
                        out.writeObject("valid");
                        signAndSendString(keystore, keystorePassword, receivingUser, value, out);
                        checkStatus(in, "Error performing transaction");
                        System.out.println("Signed [" + receivingUser + "-" + value + "]");
                    }

                }

                if (command.equals("e") || command.equals("exit"))
                    break;
                System.out.println("\n" + in.readObject());
            }
            out.close();
            in.close();
            s.close();

        } catch (InterruptedException e) {
            out.close();
            in.close();
            s.close();
            System.out.println("Client Killed");
            Thread.currentThread().interrupt();

        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }

    private static boolean isTransaction(String[] commandParts) throws InterruptedException {
        String type = commandParts[0];
        try {
            if ((type.equals("m") || type.equals("makepayment")) && commandParts.length == 3) {
                Integer.parseInt(commandParts[2]);
                return true;
            } else if ((type.equals("p") || type.equals("payrequest") || (type.equals("c") || type.equals("confirmqrode"))) && commandParts.length == 2) {
                Integer.parseInt(commandParts[1]);
                return true;
            }
            return false;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private static boolean isValidConfirmQRCode(String[] commandParts) throws InterruptedException {
        String type = commandParts[0];
        try {
            if ((type.equals("c") || type.equals("confirmqrcode")) && commandParts.length == 2) {
                Integer.parseInt(commandParts[1]);
                return true;
            }
            return false;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private static boolean isValidPayRequest(String[] commandParts) throws InterruptedException {
        String type = commandParts[0];
        try {
            if ((type.equals("p") || type.equals("payrequest")) && commandParts.length == 2) {
                Integer.parseInt(commandParts[1]);
                return true;
            }
            return false;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private static boolean isValidMakePayment(String[] commandParts) throws InterruptedException {
        String type = commandParts[0];
        try {
            if ((type.equals("m") || type.equals("makepayment")) && commandParts.length == 3) {
                Integer.parseInt(commandParts[2]);
                return true;
            }
            return false;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private static void signAndSendString(String keystore, String keystorePassword, String receivingUser, String value, ObjectOutputStream out) throws IOException, KeyStoreException, CertificateException, NoSuchAlgorithmException, UnrecoverableKeyException, InvalidKeyException, SignatureException, InterruptedException {

        try (FileInputStream kfile = new FileInputStream(keystore)) {
            KeyStore kstore = KeyStore.getInstance("JCEKS");
            kstore.load(kfile, keystorePassword.toCharArray());

            // Get private key
            PrivateKey myPrivateKey = (PrivateKey) kstore.getKey(Constants.PRIVATE_KEY_PROP, keystorePassword.toCharArray());

            // Create signedObject
            SignedObject signedObject = new SignedObject(receivingUser + "-" + value, myPrivateKey, Signature.getInstance("SHA256withRSA"));

            // Send signed input
            out.writeObject(signedObject);
        }

    }

    private static void sendEncryptedNonce(boolean exists, String keystore, String keystorePassword, long nonce, ObjectOutputStream out) throws IOException, UnrecoverableKeyException, KeyStoreException, NoSuchAlgorithmException, CertificateException, InvalidKeyException, SignatureException, InterruptedException {
        // Get keystore
        try (FileInputStream kfile = new FileInputStream(keystore)) {
            KeyStore kstore = KeyStore.getInstance("JCEKS");
            kstore.load(kfile, keystorePassword.toCharArray());

            // Get private key
            PrivateKey myPrivateKey = (PrivateKey) kstore.getKey(Constants.PRIVATE_KEY_PROP, keystorePassword.toCharArray());

            // Sign nonce
            Signature s = Signature.getInstance("SHA256withRSA");
            s.initSign(myPrivateKey);
            byte[] buf = longToBytes(nonce);
            s.update(buf);

            // Send signed nonce
            out.writeObject(s.sign());
            System.out.println("Sent signature");
            if (!exists) {
                Certificate cert = kstore.getCertificate(Constants.PRIVATE_KEY_PROP);
                // Send certificate
                out.writeObject(cert);
            }
        }

    }

    private static void checkStatus(ObjectInputStream in, String errorMessage) throws IOException, ClassNotFoundException, InterruptedException {
        if (((int) in.readObject()) == -1) {
            System.out.println(errorMessage);
            System.exit(-1);
        }
    }

    public static byte[] longToBytes(long x) {
        ByteBuffer buffer = ByteBuffer.allocate(Long.BYTES);
        buffer.putLong(x);
        return buffer.array();
    }

    // Display options menu
    private static void displayMenu() throws InterruptedException {
        System.out.println("OPTIONS MENU");
        System.out.println(Constants.DELIMITER);
        System.out.println("[B]alance");
        System.out.println("[M]akePayment [userID] [amount]");
        System.out.println("[R]equest Payment [userID] [amount]");
        System.out.println("[V]iewRequests");
        System.out.println("[P]ayRequest [reqID]");
        System.out.println("[O]btainQRcode [amount]");
        System.out.println("[C]onfirmQRcode [QRcode]");
        System.out.println("[N]ewGroup [groupID]");
        System.out.println("[A]ddU [userID] [groupID]");
        System.out.println("[G]roups");
        System.out.println("[D]ividePayment [groupID] [amount]");
        System.out.println("[S]tatusPayments [groupID]");
        System.out.println("[H]istory [groupID]");
        System.out.println(Constants.DELIMITER);
        System.out.println("[E]xit");
        System.out.println(Constants.DELIMITER);
    }

}
