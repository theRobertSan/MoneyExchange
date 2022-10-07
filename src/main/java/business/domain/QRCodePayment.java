package main.java.business.domain;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

public class QRCodePayment implements Serializable {

    private int id;
    private float amount;
    private String creatorID;

    public QRCodePayment(int id, float amount, String creatorID) {
        this.id = id;
        this.amount = amount;
        this.creatorID = creatorID;
        testingQRCode(id);
    }

    // Create QR code
    public void testingQRCode(int id) {

        // data for QRcode
        String data = Integer.toString(id);

        // The name of the image which will get saved
        String path = Integer.toString(id) + ".png";

        // Encoding charset
        String charset = "UTF-8";

        Map<EncodeHintType, ErrorCorrectionLevel> hashMap
                = new HashMap<EncodeHintType, ErrorCorrectionLevel>();

        hashMap.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.L);

        // Create the QR code and save in the specified folder as a png file
        createQRcode(data, path, charset, hashMap, 200, 200);

        System.out.println("Generated QR Code for the reqID: " + data);
    }

    public void createQRcode(String data, String path, String charset, Map<EncodeHintType, ErrorCorrectionLevel> hashMap,
                             int height, int width) {

        BitMatrix matrix;

        try {

            File qrCodesFolder = new File("./resources/qrcodes");

            qrCodesFolder.mkdir();

            matrix = new MultiFormatWriter().encode(new String(data.getBytes(charset), charset),
                    BarcodeFormat.QR_CODE, width, height);
            Path pathToSaveQRCode = Paths.get("./resources/qrcodes/" + path);

            MatrixToImageWriter.writeToPath(matrix, path.substring(path.lastIndexOf('.') + 1), pathToSaveQRCode);

        } catch (IOException | WriterException e) {
            e.printStackTrace();
        }

    }

    // Getters & Setters

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

    public String getCreatorID() {
        return this.creatorID;
    }

    public void setCreatorID(String creatorID) {
        this.creatorID = creatorID;
    }

}
