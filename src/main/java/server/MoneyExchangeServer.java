package main.java.server;

import main.java.business.catalogs.GroupIDRepository;
import main.java.business.catalogs.QRCodeRepository;
import main.java.business.catalogs.UserRepository;
import main.java.business.domain.QRCodePayment;
import main.java.business.domain.User;
import main.java.facade.exceptions.ApplicationException;
import main.java.facade.startup.Block;
import main.java.facade.startup.MoneyExchangeApp;
import main.java.utils.Constants;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.net.ServerSocketFactory;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import java.io.*;
import java.net.Socket;
import java.security.*;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.spec.InvalidKeySpecException;
import java.util.*;

public class MoneyExchangeServer {

    private final UserRepository usersData;
    private final GroupIDRepository groupIDs;
    private final QRCodeRepository qrCodePayments;
    private final HashMap<String, Integer> activeUsers;
    private final int paymentRequestID;

    private PublicKey serverPublicKey;

    private Block block;

    public MoneyExchangeServer() {

        // Create needed directories
        ServerUtilities.createFolder(Constants.RESOURCES_FOLDER);
        ServerUtilities.createFolder(Constants.CERTIFICATES_FOLDER);
        ServerUtilities.createFolder(Constants.LOGS_FOLDER);

        usersData = new UserRepository();
        groupIDs = new GroupIDRepository();
        qrCodePayments = new QRCodeRepository();
        activeUsers = new HashMap<>();
        paymentRequestID = 0;
    }

    public static void main(String[] args) throws IOException, ClassNotFoundException {

        System.out.println("Money Exchange Server Started.");

        int port = 0;
        String cipherPassword = "";
        String keystore = "";
        String keystorePassword = "";

        // Fill the attributes and start server
        try {
            // No port provided. Selecting default port
            if (args.length == 3) {
                port = Constants.DEFAULT_PORT;
                cipherPassword = args[0];
                keystore = args[1];
                keystorePassword = args[2];
            } else if (args.length == 4) {
                port = Integer.parseInt(args[0]);
                cipherPassword = args[1];
                keystore = args[2];
                keystorePassword = args[3];

                // In case of something else
            } else {
                System.out.println("Wrong number of arguments!");
                System.exit(-1);
            }
        } catch (NumberFormatException e) {
            System.out.println("Wrong type of arguments!");
        }

        MoneyExchangeServer server = new MoneyExchangeServer();
        server.startServer(port, cipherPassword, keystore, keystorePassword);
    }

    private static boolean isValidConfirmQRCode(String[] commandParts) {
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

    private static void sendStatus(boolean success, ObjectOutputStream out) throws IOException {

        if (success) {
            out.writeObject(1);
        } else {
            out.writeObject(-1);
            System.out.println("Trokos Server Thread has ended");
            Thread.currentThread().stop();
        }

    }

    private static SignedObject verifySignature(ObjectInputStream in, ObjectOutputStream out, PublicKey pk) throws IOException, ClassNotFoundException, NoSuchAlgorithmException, InvalidKeyException, SignatureException {
        SignedObject signedObject = (SignedObject) in.readObject();
        sendStatus(signedObject.verify(pk, Signature.getInstance("SHA256withRSA")), out);
        return signedObject;
    }

    private static boolean isTransaction(String[] commandParts) {
        String type = commandParts[0];
        try {
            if ((type.equals("m") || type.equals("makepayment")) && commandParts.length == 3) {
                Integer.parseInt(commandParts[2]);
                return true;
            } else if ((type.equals("p") || type.equals("payrequest")) && commandParts.length == 2) {
                Integer.parseInt(commandParts[1]);
                return true;
            } else if ((type.equals("c") || type.equals("confirmqrode")) && commandParts.length == 2) {
                Integer.parseInt(commandParts[1]);
                return true;
            }
            return false;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    public void startServer(int port, String cypherPassword, String keystore, String keystorePassword) {

        try {

            System.setProperty("javax.net.ssl.keyStore", keystore);
            System.setProperty("javax.net.ssl.keyStorePassword", keystorePassword);

            ServerSocketFactory ssf = SSLServerSocketFactory.getDefault();
            SSLServerSocket ss = (SSLServerSocket) ssf.createServerSocket(port);

            getServerPublicKey();

            // Check log files for data
            fetchLogData(keystore, keystorePassword);

            // Get data from file, in case it exists
            usersData.getFromFile(cypherPassword);
            groupIDs.getFromFile(cypherPassword);
            qrCodePayments.getFromFile(cypherPassword);

            while (true) {
                ServerThread newServerThread = new ServerThread(ss.accept(), keystore, keystorePassword, cypherPassword);
                newServerThread.start();
            }

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void getServerPublicKey() throws FileNotFoundException, CertificateException {
        FileInputStream fis = new FileInputStream(Constants.SERVER_CERTIFICATE_FILENAME);
        CertificateFactory cf = CertificateFactory.getInstance("X509");
        serverPublicKey = cf.generateCertificate(fis).getPublicKey();
    }

    private void fetchLogData(String keystore, String keystorePassword) throws IOException, ClassNotFoundException, UnrecoverableKeyException, CertificateException, KeyStoreException, NoSuchAlgorithmException, SignatureException, InvalidKeyException {

        File logsFolder = new File("./logs");
        File[] blockFiles = logsFolder.listFiles();

        if (blockFiles.length == 0) {
            block = new Block();
            return;
        }

        List<String> filenames = new ArrayList<>();
        for (File file : blockFiles) {
            filenames.add(file.getName());
        }

        Collections.sort(filenames);

        byte[] previousHash = new byte[32];

        for (int i = 0; i < filenames.size(); i++) {
            String filename = filenames.get(i);

            try (FileInputStream blockDataFileStream = new FileInputStream(Constants.LOGS_FOLDER + filename);
                 ObjectInputStream blockDataObjectStream = new ObjectInputStream(blockDataFileStream)) {
                Block currentBlock = (Block) blockDataObjectStream.readObject();

                if (!currentBlock.verifyHash(previousHash)) {
                    System.out.println("Blockchain integrity corrupted! Error found in block hashes. Server shutting down...");
                    System.exit(-1);
                }

                if (!currentBlock.verifySignature(serverPublicKey)) {
                    System.out.println("Blockchain integrity corrupted! Error found in block signature. Server shutting down...");
                    System.exit(-1);
                }

                if (currentBlock.getBlockNum() != (i + 1)) {
                    System.out.println("Blockchain integrity corrupted! Error found in block number. Server shutting down...");
                    System.exit(-1);
                }

                if (!currentBlock.allTransactionsValid()) {
                    System.out.println("Blockchain integrity corrupted! Error found in transaction signature. Server shutting down...");
                    System.exit(-1);
                }

                previousHash = currentBlock.calculateHash();
            }

        }

        String maxFilename = filenames.get(filenames.size() - 1);

        System.out.println("Fetched block: " + maxFilename);

        FileInputStream blockDataFileStream = new FileInputStream(Constants.LOGS_FOLDER + maxFilename);
        ObjectInputStream blockDataObjectStream = new ObjectInputStream(blockDataFileStream);

        block = (Block) blockDataObjectStream.readObject();

        // Check if block is already full
        block.verifyBlockIntegrity(keystore, keystorePassword);
    }

    private void storeUserCertificate(String username) throws IOException {
        String certificateFileName = Constants.CERTIFICATES_FOLDER + username + "Certificate.cer";
        ServerUtilities.writeToFile(Constants.USERS_FILENAME, String.format("%s:%s\n", username, certificateFileName), true);
    }

    private Certificate getCertificate(String username, File usersFile) throws FileNotFoundException, CertificateException {

        Scanner sc = new Scanner(usersFile);
        String certificateFilename = null;
        while (sc.hasNextLine()) {

            // ["username", "certificateFilename"]
            String[] keyValuePair = sc.nextLine().split(":");

            if (username.equals(keyValuePair[0])) {
                certificateFilename = keyValuePair[1];
                break;
            }
        }

        if (certificateFilename == null) {
            return null;
        }

        FileInputStream fis = new FileInputStream(certificateFilename);
        CertificateFactory cf = CertificateFactory.getInstance("X509");
        return cf.generateCertificate(fis);
    }

    private long sendNonce(ObjectOutputStream out) throws IOException {
        long nonce = new Random().nextLong();
        // Send nonce
        out.writeObject(nonce);
        System.out.println("Sent nonce");
        return nonce;
    }

    private void verifyNonce(long nonce, ObjectInputStream in, ObjectOutputStream out) throws IOException, ClassNotFoundException {
        // Verify nonce
        long clientNonce = (long) in.readObject();
        System.out.println("Client nonce received!");
        sendStatus(clientNonce == nonce, out);
    }

    private PublicKey verifyEncryptedNonce(String username, long nonce, Certificate userCertificate, ObjectInputStream in, ObjectOutputStream out) throws IOException, ClassNotFoundException, NoSuchAlgorithmException, InvalidKeyException, CertificateEncodingException, SignatureException {
        // Get signed nonce
        byte[] signedNonce = (byte[]) in.readObject();
        System.out.println("Received signed nonce");
        // If no certificate associated to user, read one from the user
        if (userCertificate == null) {
            userCertificate = (Certificate) in.readObject();
            // Creating certificate file
            ServerUtilities.createCertificateFile(username, userCertificate);
        }

        PublicKey pk = userCertificate.getPublicKey();

        // Verify signature
        Signature s = Signature.getInstance("SHA256withRSA");
        s.initVerify(pk);
        byte[] buf = ServerUtilities.longToBytes(nonce);
        s.update(buf);
        // If decrypted nonce is not equal to nonce, leave
        sendStatus(s.verify(signedNonce), out);

        return pk;
    }

    private String action(MoneyExchangeApp app, String fullCommand, String cypherPassword) throws ClassNotFoundException, IOException, NoSuchAlgorithmException, InvalidKeySpecException, NoSuchPaddingException, IllegalBlockSizeException, BadPaddingException, InvalidKeyException, InvalidAlgorithmParameterException {

        // Update catalogs
        app.update(cypherPassword);

        try {

            // Ex: Take makepayment 123 500 and store
            String[] commandArgs = fullCommand.split(" ");

            // Ex: Save option makepayment
            String option = commandArgs[0];

            String arg1 = null;
            String arg2 = null;

            if (commandArgs.length == 2 || commandArgs.length == 3) {
                arg1 = commandArgs[1];
            }

            if (commandArgs.length == 3) {
                arg2 = commandArgs[2];
            }

            // Return balance
            if (option.equals("b") || option.equals("balance")) {

                if (commandArgs.length != 1) {
                    return Constants.INCORRECT_NUM_ARGS_MESSAGE;
                } else {
                    return app.getBalance();
                }
            } else if (option.equals("m") || option.equals("makepayment")) {

                if (commandArgs.length != 3) {
                    return Constants.INCORRECT_NUM_ARGS_MESSAGE;
                } else {
                    return app.makePayment(arg1, Float.parseFloat(arg2));
                }
            } else if (option.equals("r") || option.equals("requestpayment")) {

                if (commandArgs.length != 3) {
                    return Constants.INCORRECT_NUM_ARGS_MESSAGE;
                } else {
                    return app.requestPayment(arg1, Float.parseFloat(arg2), null);
                }
            } else if (option.equals("v") || option.equals("viewrequests")) {

                if (commandArgs.length != 1) {
                    return Constants.INCORRECT_NUM_ARGS_MESSAGE;
                } else {
                    return app.viewRequests();
                }
            } else if (option.equals("p") || option.equals("payrequest")) {

                if (commandArgs.length != 2) {
                    return Constants.INCORRECT_NUM_ARGS_MESSAGE;
                } else {
                    return app.payRequest(Integer.parseInt(arg1));
                }
            } else if (option.equals("o") || option.equals("obtainqrcode")) {

                if (commandArgs.length != 2) {
                    return Constants.INCORRECT_NUM_ARGS_MESSAGE;
                } else {
                    return app.obtainQRCode(Float.parseFloat(arg1));
                }
            } else if (option.equals("c") || option.equals("confirmqrcode")) {
                if (commandArgs.length != 2) {
                    return Constants.INCORRECT_NUM_ARGS_MESSAGE;
                } else {
                    return app.confirmQRCode(Integer.parseInt(arg1));
                }
            } else if (option.equals("n") || option.equals("newgroup")) {
                if (commandArgs.length != 2) {
                    return Constants.INCORRECT_NUM_ARGS_MESSAGE;
                } else {
                    return app.createGroup(Integer.parseInt(arg1));
                }
            } else if (option.equals("a") || option.equals("addu")) {

                if (commandArgs.length != 3) {
                    return Constants.INCORRECT_NUM_ARGS_MESSAGE;
                } else {
                    return app.addUser(arg1, Integer.parseInt(arg2));
                }
            } else if (option.equals("g") || option.equals("groups")) {

                if (commandArgs.length != 1) {
                    return Constants.INCORRECT_NUM_ARGS_MESSAGE;
                } else {
                    return app.displayGroups();
                }
            } else if (option.equals("d") || option.equals("dividepayment")) {

                if (commandArgs.length != 3) {
                    return Constants.INCORRECT_NUM_ARGS_MESSAGE;
                } else {
                    return app.dividePayment(Integer.parseInt(arg1), Float.parseFloat(arg2));
                }
            } else if (option.equals("s") || option.equals("statuspayments")) {

                if (commandArgs.length != 2) {
                    return Constants.INCORRECT_NUM_ARGS_MESSAGE;
                } else {
                    return app.statusPayments(Integer.parseInt(arg1));
                }
            } else if (option.equals("h") || option.equals("history")) {

                if (commandArgs.length != 2) {
                    return Constants.INCORRECT_NUM_ARGS_MESSAGE;
                } else {
                    return app.history(Integer.parseInt(arg1));
                }
            }
            return "Error: Please insert a valid command.";

        } catch (NumberFormatException e) {
            return "Error: Please insert the correct type of arguments!";
        } catch (ApplicationException e) {
            return e.getMessage();
        }
    }

    class ServerThread extends Thread {
        private final Socket socket;
        private final String keystore;
        private final String keystorePassword;

        private final String cypherPassword;

        ServerThread(Socket inSoc, String keystore, String keystorePassword, String cypherPassword) {
            socket = inSoc;
            this.keystore = keystore;
            this.keystorePassword = keystorePassword;
            this.cypherPassword = cypherPassword;
            System.out.println("Money Exchange Server Thread Created for client.");
        }

        public void run() {

            String username = "";

            try {
                ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
                ObjectInputStream in = new ObjectInputStream(socket.getInputStream());

                // Get user id
                username = (String) in.readObject();

                // Check if someone with that user id is active
                sendStatus(!activeUsers.containsKey(username), out);

                File usersFile = new File(Constants.USERS_FILENAME);

                // If file doesn't exist, create
                if (!usersFile.exists()) {
                    usersFile.createNewFile();
                }

                Certificate userCertificate = getCertificate(username, usersFile);
                boolean userExists = userCertificate != null;

                // Send bool that represents if user has certificate associated
                out.writeObject(userExists);

                long nonce = sendNonce(out);

                // Login path
                PublicKey pk;
                if (userExists) {
                    pk = verifyEncryptedNonce(username, nonce, userCertificate, in, out);

                    // Register path
                } else {
                    verifyNonce(nonce, in, out);
                    pk = verifyEncryptedNonce(username, nonce, null, in, out);
                    storeUserCertificate(username);

                    // Create User
                    User user = new User(username, Constants.DEFAULT_STARTING_BALANCE);

                    // Save User
                    synchronized (usersData) {
                        usersData.addUser(user);
                        usersData.saveToFile(cypherPassword);
                    }
                }

                activeUsers.put(username, 0);

                File reqIDFile = new File(Constants.REQ_ID_FILENAME);

                // If file doesn't exist, create
                if (!reqIDFile.exists()) {
                    reqIDFile.createNewFile();
                    ServerUtilities.writeToFile(Constants.REQ_ID_FILENAME, String.valueOf(0), false);
                }

                MoneyExchangeApp app = new MoneyExchangeApp(username, usersData, groupIDs, qrCodePayments, paymentRequestID);

                while (true) {

                    System.out.printf("Starting Transaction for user [%s]%n", username);
                    String clientCommand = ((String) in.readObject()).toLowerCase();
                    String[] commandParts = clientCommand.split(" ");

                    if (clientCommand.equals("e") || clientCommand.equals("exit"))
                        break;

                    // If correct confirmqurdcode command was sent
                    if (isValidConfirmQRCode(commandParts)) {

                        QRCodePayment payment = qrCodePayments.getQRCodePayment(Integer.parseInt(commandParts[1]));
                        if (payment != null) {
                            out.writeObject(qrCodePayments.getQRCodePayment(Integer.parseInt(commandParts[1])).getCreatorID() + "-" +
                                    qrCodePayments.getQRCodePayment(Integer.parseInt(commandParts[1])).getAmount());
                        } else {
                            out.writeObject("null-null");
                        }
                        System.out.println("Sending additional information");
                    }

                    boolean isTransaction = isTransaction(commandParts);
                    SignedObject signedObject = null;
                    if (isTransaction) {

                        if (in.readObject().equals("valid")) {
                            System.out.println("Starting sign verification for command " + clientCommand);
                            signedObject = verifySignature(in, out, pk);
                        } else {
                            System.out.println("Invalid transaction");
                        }

                    } else {
                        System.out.println("Not a transaction");
                    }
                    String response;
                    synchronized (usersData) {
                        synchronized (groupIDs) {
                            synchronized (qrCodePayments) {
                                // Perform the actual work
                                System.out.println(Constants.DELIMITER);
                                System.out.printf("> User [%s] sent command: %s%n", username, clientCommand);
                                response = action(app, clientCommand, cypherPassword);
                                System.out.printf(">> Server response to user [%s]%n%s%n", username, response);
                                System.out.println(Constants.DELIMITER);

                                // Send response
                                out.writeObject(response);

                                // Save to files
                                app.save(cypherPassword);
                            }
                        }
                    }

                    // Add transaction to log block
                    if (isTransaction && !response.split(" ")[0].equals("Error:")) {

                        String[] userAndValue = ((String) signedObject.getObject()).split("-");
                        if (userAndValue.length != 0) {
                            block.addTransaction(username, userAndValue[1], userAndValue[0], signedObject, keystore, keystorePassword);
                        }

                    }

                    System.out.printf("Ending Transaction for user [%s]%n", username);
                }
                System.out.println("Closing Client Thread");
                activeUsers.remove(username);
                out.close();
                in.close();
                socket.close();

            } catch (IOException e) {
                activeUsers.remove(username);
                System.out.println("Client thread has been killed by client");
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

}
