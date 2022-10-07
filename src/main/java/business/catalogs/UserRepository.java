package main.java.business.catalogs;

import main.java.business.domain.User;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.io.*;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.HashMap;

public class UserRepository {

    private static final String USERS_DATA_FILENAME = "./resources/users_data.txt";
    private static final String PARAM_NAME = "users_data.param";

    private HashMap<String, User> users;

    public User getUser(String userID) {
        return users.get(userID);
    }

    public void addUser(User user) {
        users.put(user.getID(), user);
    }

    // Get data from backup file
    public void saveToFile(String password) throws IOException, NoSuchAlgorithmException, InvalidKeySpecException, NoSuchPaddingException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException {
        // Save updated users data array to file
        // Get user data list from file
        FileOutputStream updateUserDataFileStream = new FileOutputStream(USERS_DATA_FILENAME);
        ObjectOutputStream updateUserDataObjectStream = new ObjectOutputStream(updateUserDataFileStream);

        // Clean
        updateUserDataObjectStream.reset();

        // Encrypt and save data
        CatalogUtils.encryptAndSaveData(password, users, updateUserDataObjectStream, PARAM_NAME);

        // Close Streams
        updateUserDataObjectStream.flush();
        updateUserDataFileStream.close();
        updateUserDataObjectStream.close();

    }

    // Update map to file to keep the backup updated
    public void getFromFile(String password) throws IOException, ClassNotFoundException, NoSuchAlgorithmException, InvalidKeySpecException, NoSuchPaddingException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException, InvalidAlgorithmParameterException {

        // Get user data list from file
        File usersDataFile = new File(USERS_DATA_FILENAME);

        // If file doesn't exist or is empty
        if (usersDataFile.length() == 0) {

            // If file doesn't exist
            if (!usersDataFile.exists()) {
                usersDataFile.createNewFile();
            }

            users = new HashMap<>();
        } else {

            FileInputStream userDataFileStream = new FileInputStream(USERS_DATA_FILENAME);
            ObjectInputStream userDataObjectStream = new ObjectInputStream(userDataFileStream);

            // Get decrypted data from file
            users = (HashMap<String, User>) CatalogUtils.getDecryptedData(password, userDataObjectStream, PARAM_NAME);

            // Close Streams
            userDataFileStream.close();
            userDataObjectStream.close();
        }
    }

}
