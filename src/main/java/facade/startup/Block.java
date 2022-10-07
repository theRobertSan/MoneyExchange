package main.java.facade.startup;

import java.io.*;
import java.nio.ByteBuffer;
import java.security.*;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Block implements Serializable {

    public byte[] hash;
    public long blockNum;
    public long transactionsNum;
    public List<String> transactions;
    public List<SignedObject> transactionSignedObjects;
    public byte[] signature;

    public Block() {
        this.hash = new byte[32];
        this.blockNum = 1;
        this.transactionsNum = 0;
        this.transactions = new ArrayList<>();
        this.transactionSignedObjects = new ArrayList<>();
        this.signature = new byte[32];
    }

    public byte[] getHash() {
        return this.hash;
    }

    public void setHash(byte[] data) throws NoSuchAlgorithmException, IOException {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        this.hash = md.digest(data);
    }

    public long getBlockNum() {
        return this.blockNum;
    }

    public long getTransactionsNum() {
        return transactionsNum;
    }

    public List<String> getTransactions() {
        return this.transactions;
    }

    public void setSignature(PrivateKey myPrivateKey) throws IOException, NoSuchAlgorithmException, InvalidKeyException, SignatureException {
        // Sign block data
        Signature s = Signature.getInstance("SHA256withRSA");
        s.initSign(myPrivateKey);

        // Get byte array of block data
        byte[] buf = blockDataToByteArray();
        s.update(buf);

        // Save signature
        this.signature = s.sign();
    }

    public void addTransaction(String sendingUser, String value, String receivingUser, SignedObject signedObject, String keyStore, String keyStorePassword) throws UnrecoverableKeyException, CertificateException, IOException, KeyStoreException, NoSuchAlgorithmException, SignatureException, InvalidKeyException {
        this.transactions.add(String.format("%s sent %s€ to %s", sendingUser, value, receivingUser));
        // Add respective signedObject
        this.transactionSignedObjects.add(signedObject);
        this.transactionsNum++;

        if (this.transactionsNum == 5) {
            saveCompleteBlock(keyStore, keyStorePassword);
        } else {
            saveBlock(keyStore, keyStorePassword);
        }

    }

    public boolean verifyHash(byte[] otherHash) {
        return Arrays.equals(hash, otherHash);
    }

    public void verifyBlockIntegrity(String keystore, String keystorePassword) throws UnrecoverableKeyException, CertificateException, KeyStoreException, NoSuchAlgorithmException, IOException, SignatureException, InvalidKeyException {
        if (this.transactionsNum == 5) {
            // Reset object
            resetBlock();
        }
    }

    public boolean verifySignature(PublicKey myPublicKey) throws NoSuchAlgorithmException, InvalidKeyException, SignatureException, IOException {

        if (transactionsNum != 5) {
            return true;
        }

        // Sign block data
        Signature s = Signature.getInstance("SHA256withRSA");
        s.initVerify(myPublicKey);

        // Get byte array of block data
        byte[] buf = blockDataToByteArray();
        s.update(buf);

        // Save signature
        return s.verify(signature);
    }

    public byte[] calculateHash() throws NoSuchAlgorithmException, IOException {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        return md.digest(blockToByteArray(this));
    }

    public void resetBlock() {
        this.blockNum++;
        this.transactionsNum = 0;
        this.transactions = new ArrayList<>();
        this.transactionSignedObjects = new ArrayList<>();
        this.signature = new byte[32];
    }

    public boolean allTransactionsValid() throws NoSuchAlgorithmException, IOException, ClassNotFoundException, CertificateException, SignatureException, InvalidKeyException {

        for (int i = 0; i < transactionSignedObjects.size(); i++) {
            SignedObject signedObject = transactionSignedObjects.get(i);
            String sendingUser = transactions.get(i).split(" ")[0];

            FileInputStream fis = new FileInputStream("./certificates/" + sendingUser + "Certificate.cer");
            CertificateFactory cf = CertificateFactory.getInstance("X509");

            PublicKey pk = cf.generateCertificate(fis).getPublicKey();
            if (!signedObject.verify(pk, Signature.getInstance("SHA256withRSA"))) {
                return false;
            }

        }

        return true;
    }

    private void saveBlock(String keystore, String keystorePassword) throws IOException, KeyStoreException, CertificateException, NoSuchAlgorithmException, UnrecoverableKeyException, InvalidKeyException, SignatureException {
        // Get keystore
        FileInputStream kfile = new FileInputStream(keystore);
        KeyStore kstore = KeyStore.getInstance("JCEKS");
        kstore.load(kfile, keystorePassword.toCharArray());

        byte[] data = blockToByteArray(this);

        // Save log in log file
        String filename = String.format("./logs/block_%d.blk", getBlockNum());
        File outputFile = new File(filename);
        try (FileOutputStream outputStream = new FileOutputStream(outputFile)) {
            outputStream.write(data);
        }
        System.out.println(String.format("Block file %s updated", filename));

    }

    private void saveCompleteBlock(String keyStore, String keyStorePassword) throws UnrecoverableKeyException, KeyStoreException, NoSuchAlgorithmException, IOException, SignatureException, InvalidKeyException, CertificateException {
        // Get keystore
        FileInputStream kfile = new FileInputStream(keyStore);
        KeyStore kstore = KeyStore.getInstance("JCEKS");
        kstore.load(kfile, keyStorePassword.toCharArray());

        // Get private key
        PrivateKey myPrivateKey = (PrivateKey) kstore.getKey("myServer", keyStorePassword.toCharArray());

        // Sign block data
        setSignature(myPrivateKey);

        // Calculate hash
        byte[] data = blockToByteArray(this);
        setHash(data);

        // Save block in log file
        String filename = String.format("./logs/block_%d.blk", getBlockNum());
        File outputFile = new File(filename);
        try (FileOutputStream outputStream = new FileOutputStream(outputFile)) {
            outputStream.write(data);
        }
        System.out.println(String.format("Complete block file %s created", filename));

        // Reset object
        resetBlock();
    }

    private byte[] blockDataToByteArray() throws IOException {
        byte[] hash = this.getHash();
        byte[] blockNum = longToBytes(this.getBlockNum());
        byte[] transactionsNum = longToBytes(this.getTransactionsNum());
        byte[] transactions = objectToByteArray(this.getTransactions());
        byte[] transactionSignedObjects = objectToByteArray(this.transactionSignedObjects);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        outputStream.write(hash);
        outputStream.write(blockNum);
        outputStream.write(transactionsNum);
        outputStream.write(transactions);
        outputStream.write(transactionSignedObjects);
        return outputStream.toByteArray();
    }

    private byte[] longToBytes(long x) {
        ByteBuffer buffer = ByteBuffer.allocate(Long.BYTES);
        buffer.putLong(x);
        return buffer.array();
    }

    private byte[] objectToByteArray(Object o) {
        byte[] byteArray;
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos;
        try {
            oos = new ObjectOutputStream(baos);
            oos.writeObject(o);
        } catch (IOException e) {
            e.printStackTrace();
        }
        byteArray = baos.toByteArray();
        return byteArray;
    }

    private byte[] blockToByteArray(Block block) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(bos);
        oos.writeObject(block);
        oos.flush();
        byte[] data = bos.toByteArray();
        return data;
    }


}
