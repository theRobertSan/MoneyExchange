package main.java.utils;

public final class Constants {

    public static final int DEFAULT_PORT = 45678;
    public static final int DEFAULT_STARTING_BALANCE = 100;
    public static final String INCORRECT_NUM_ARGS_MESSAGE = "Provide a correct number of arguments!";
    public static final String RESOURCES_FOLDER = "./resources/";
    public static final String CERTIFICATES_FOLDER = "./certificates/";
    public static final String LOGS_FOLDER = "./logs/";
    public static final String USERS_FILENAME = RESOURCES_FOLDER + "/users.txt";
    public static final String REQ_ID_FILENAME = RESOURCES_FOLDER + "/reqid.txt";
    public static final String SERVER_CERTIFICATE_FILENAME = "certServer.cer";
    public static final String DELIMITER = "----------------------------------------------";
    public static final String PRIVATE_KEY_PROP = "privateKey";

    private Constants() {
    }
}
