package main.java.business.catalogs;

import main.java.business.domain.QRCodePayment;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.io.*;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.HashMap;

public class QRCodeRepository {

    private static final String QRCODES_DATA_FILENAME = "./resources/QRCodes_data.txt";
    private static final String PARAM_NAME = "QRCodes_data.param";
    private HashMap<Integer, QRCodePayment> qrCodePayments;

    public QRCodePayment getQRCodePayment(Integer id) {
        return qrCodePayments.get(id);
    }

    public void addQRCodePayment(QRCodePayment qrCode) {
        qrCodePayments.put(qrCode.getId(), qrCode);
    }

    // Get data from backup file
    public void saveToFile(String password) throws IOException, NoSuchPaddingException, IllegalBlockSizeException, NoSuchAlgorithmException, InvalidKeySpecException, BadPaddingException, InvalidKeyException {
        // Save updated users data array to file
        // Get user data list from file
        FileOutputStream updateQRCodeDataFileStream = new FileOutputStream(QRCODES_DATA_FILENAME);
        ObjectOutputStream updateQRCodeDataObjectStream = new ObjectOutputStream(updateQRCodeDataFileStream);

        // Clean
        updateQRCodeDataObjectStream.reset();

        // Encrypt and save data
        CatalogUtils.encryptAndSaveData(password, qrCodePayments, updateQRCodeDataObjectStream, PARAM_NAME);

        // Close Streams
        updateQRCodeDataObjectStream.flush();
        updateQRCodeDataFileStream.close();
        updateQRCodeDataObjectStream.close();

    }

    // Update map to file to keep the backup updated
    public void getFromFile(String password) throws IOException, ClassNotFoundException, InvalidAlgorithmParameterException, NoSuchPaddingException, IllegalBlockSizeException, NoSuchAlgorithmException, InvalidKeySpecException, BadPaddingException, InvalidKeyException {

        // Get user data list from file
        File qrCodeDataFile = new File(QRCODES_DATA_FILENAME);

        // If file doesn't exist or is empty
        if (qrCodeDataFile.length() == 0) {

            // If file doesn't exist
            if (!qrCodeDataFile.exists()) {
                qrCodeDataFile.createNewFile();
            }

            qrCodePayments = new HashMap<>();
        } else {
            FileInputStream qrCodeDataFileStream = new FileInputStream(QRCODES_DATA_FILENAME);
            ObjectInputStream qrCodeDataObjectStream = new ObjectInputStream(qrCodeDataFileStream);

            // Get decrypted data from file
            qrCodePayments = (HashMap<Integer, QRCodePayment>) CatalogUtils.getDecryptedData(password, qrCodeDataObjectStream, PARAM_NAME);

            // Close Streams
            qrCodeDataFileStream.close();
            qrCodeDataObjectStream.close();
        }
    }

    public void removeQRCodePayment(int id) {
        qrCodePayments.remove(id);
    }
}
