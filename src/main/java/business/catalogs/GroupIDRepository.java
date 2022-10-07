package main.java.business.catalogs;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.io.*;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.ArrayList;

public class GroupIDRepository {

    private static final String GROUP_IDS_DATA_FILENAME = "./resources/groupIDs_data.txt";
    private static final String PARAM_NAME = "groupsID_data.param";
    private ArrayList<Integer> groupIDs;

    public boolean exists(int groupID) {
        return groupIDs.contains(groupID);
    }

    public void add(int groupID) {
        groupIDs.add(groupID);
    }

    // Get data from backup file
    public void saveToFile(String password) throws IOException, NoSuchPaddingException, IllegalBlockSizeException, NoSuchAlgorithmException, InvalidKeySpecException, BadPaddingException, InvalidKeyException {
        // Save updated users data array to file
        // Get user data list from file
        FileOutputStream updateGroupDataFileStream = new FileOutputStream(GROUP_IDS_DATA_FILENAME);
        ObjectOutputStream updateGroupDataObjectStream = new ObjectOutputStream(updateGroupDataFileStream);

        // Clean
        updateGroupDataObjectStream.reset();

        // Encrypt and save data
        CatalogUtils.encryptAndSaveData(password, groupIDs, updateGroupDataObjectStream, PARAM_NAME);

        // Close Streams
        updateGroupDataObjectStream.flush();
        updateGroupDataObjectStream.close();
        updateGroupDataFileStream.close();

    }

    // Update list to file to keep the backup updated
    public void getFromFile(String password) throws IOException, ClassNotFoundException, InvalidAlgorithmParameterException, NoSuchPaddingException, IllegalBlockSizeException, NoSuchAlgorithmException, InvalidKeySpecException, BadPaddingException, InvalidKeyException {

        File groupIDsDataFile = new File(GROUP_IDS_DATA_FILENAME);

        if (groupIDsDataFile.length() == 0) {

            // If file doesn't exist
            if (!groupIDsDataFile.exists()) {
                groupIDsDataFile.createNewFile();
            }

            groupIDs = new ArrayList<>();
        } else {

            FileInputStream groupDataFileStream = new FileInputStream(GROUP_IDS_DATA_FILENAME);
            ObjectInputStream groupDataObjectStream = new ObjectInputStream(groupDataFileStream);

            // Get decrypted data from file
            groupIDs = (ArrayList<Integer>) CatalogUtils.getDecryptedData(password, groupDataObjectStream, PARAM_NAME);

            // Close Streams
            groupDataFileStream.close();
            groupDataObjectStream.close();
        }
    }

}