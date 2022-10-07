package main.java.server;

import main.java.utils.Constants;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;

public class ServerUtilities {

    /**
     * Given a pathname, create a folder if one doesn't exist already
     *
     * @param pathname - The folder's pathname
     */
    static void createFolder(String pathname) {
        // Create needed directory
        File folder = new File(pathname);
        boolean created = folder.mkdir();
        String name = pathname.split("/")[1];
        if (created) {
            System.out.printf("Created %s folder%n.", name);
        }
    }

    /**
     * Turns a long to an array of bytes
     *
     * @param x - Long to be transformed
     * @return the array of bytes corresponding to the long
     */
    static byte[] longToBytes(long x) {
        ByteBuffer buffer = ByteBuffer.allocate(Long.BYTES);
        buffer.putLong(x);
        return buffer.array();
    }

    /**
     * Writes data to a file
     *
     * @param fileName - Filename
     * @param data     - Data to be written
     * @param append   - Boolean corresponding to the intent of appending or not
     * @throws IOException
     */
    static void writeToFile(String fileName, String data, boolean append) throws IOException {
        FileWriter writer;
        if (append) {
            writer = new FileWriter(fileName, true);
            writer.append(data);
        } else {
            writer = new FileWriter(fileName);
            writer.write(data);
        }
        writer.close();
    }

    /**
     * Given a username, creates a certificate file for that username
     *
     * @param username - Username
     * @param cert     - Certificate Object
     * @throws CertificateEncodingException
     * @throws IOException
     */
    static void createCertificateFile(String username, Certificate cert) throws CertificateEncodingException, IOException {
        byte[] certBytes = cert.getEncoded();
        String filename = Constants.CERTIFICATES_FOLDER + username + "Certificate.cer";
        File outputFile = new File(filename);
        try (FileOutputStream outputStream = new FileOutputStream(outputFile)) {
            outputStream.write(certBytes);
        }
    }

}
