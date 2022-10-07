package main.java.business.catalogs;

import javax.crypto.*;
import javax.crypto.spec.PBEKeySpec;
import java.io.*;
import java.security.AlgorithmParameters;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;

public class CatalogUtils {

    private static final String PARAMS_PATH = "./resources/";

    public static void encryptAndSaveData(String password, Object o, ObjectOutputStream updateUserDataObjectStream, String filename) throws NoSuchAlgorithmException, InvalidKeySpecException, NoSuchPaddingException, InvalidKeyException, IOException, IllegalBlockSizeException, BadPaddingException {
        // Generate the key based on the password
        SecretKey key = generateKeys(password);

        // Encrypt
        Cipher c = Cipher.getInstance("PBEWithHmacSHA256AndAES_128");
        c.init(Cipher.ENCRYPT_MODE, key);

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(bos);
        oos.writeObject(o);
        oos.flush();

        byte[] encryptedData = c.doFinal(bos.toByteArray());

        // Write params to file
        byte[] params = c.getParameters().getEncoded();
        CatalogUtils.saveParamsToFile(params, PARAMS_PATH + filename);

        // Insert updated array
        updateUserDataObjectStream.writeObject(encryptedData);

    }

    public static Object getDecryptedData(String password, ObjectInputStream ois, String filename) throws NoSuchAlgorithmException, InvalidKeySpecException, IOException, ClassNotFoundException, NoSuchPaddingException, InvalidAlgorithmParameterException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException {
        // Generate the key based on the password
        SecretKey key = generateKeys(password);

        // Get params from file
        byte[] params = getParamsFromFile(PARAMS_PATH + filename);

        // Decrypt
        byte[] encryptedData = (byte[]) ois.readObject();
        AlgorithmParameters p = AlgorithmParameters.getInstance("PBEWithHmacSHA256AndAES_128");
        p.init(params);
        Cipher c = Cipher.getInstance("PBEWithHmacSHA256AndAES_128");
        c.init(Cipher.DECRYPT_MODE, key, p);
        byte[] dec = c.doFinal(encryptedData);

        // Byte[] to HashMap
        ByteArrayInputStream in = new ByteArrayInputStream(dec);
        ObjectInputStream is = new ObjectInputStream(in);
        return is.readObject();
    }

    public static void saveParamsToFile(byte[] params, String path) throws IOException {
        FileOutputStream paramsDataFileStream = new FileOutputStream(path);
        ObjectOutputStream paramsUserDataObjectStream = new ObjectOutputStream(paramsDataFileStream);
        paramsUserDataObjectStream.writeObject(params);
    }

    public static byte[] getParamsFromFile(String path) throws IOException, ClassNotFoundException {
        // Read params from file
        FileInputStream paramsDataFileStream = new FileInputStream(path);
        ObjectInputStream paramsUserDataObjectStream = new ObjectInputStream(paramsDataFileStream);
        byte[] params = (byte[]) paramsUserDataObjectStream.readObject();

        return params;

    }

    private static SecretKey generateKeys(String password) throws NoSuchAlgorithmException, InvalidKeySpecException {
        // Generate the key based on the password
        PBEKeySpec keySpec = new PBEKeySpec(password.toCharArray(), new byte[8], 20);
        SecretKeyFactory kf = SecretKeyFactory.getInstance("PBEWithHmacSHA256AndAES_128");
        SecretKey key = kf.generateSecret(keySpec);
        return key;
    }

}