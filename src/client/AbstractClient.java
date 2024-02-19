package client;

import util.Checksum;

import java.io.*;
import java.util.Arrays;
import java.util.List;

// Abstract class to represent a client
public abstract class AbstractClient implements Client {
  protected String serverIp;
  protected int serverPort;
  private final BufferedReader inputReader;
  protected static final int TIMEOUT = 5000;

  // Constructor for AbstractClient
  public AbstractClient(String serverIp, int serverPort) {
    this.serverIp = serverIp;
    this.serverPort = serverPort;
    inputReader = new BufferedReader(new InputStreamReader(System.in));
  }

  // Method to pre-populate requests with sample data
  public void prePopulateRequests() {
    System.out.println("\n=====Pre-populated Key-Value Store=====\n");

    // Pre-populating PUT requests
    System.out.println("\n=====5 PUTs=====\n");
    List<String> puts = Arrays.asList("put watermelon Summer", "PUT apple fruit", "Put apple fall", "pUt email 123@gmail.com", "PUT blueberry summer");
    for (String put : puts) {
      ClientLogger.info("Automated input: " + put);
      if (!verifyUserInput(put)) {
        continue;
      }
      getResponse(put);
      System.out.println();
    }

    // Pre-populating GET requests
    System.out.println("\n=====5 GETs=====\n");
    List<String> gets = Arrays.asList("get watermelon", "GET apple", "Get email", "gEt orange", "get blueberry");
    for (String get : gets) {
      ClientLogger.info("Automated input: " + get);
      if (!verifyUserInput(get)) {
        continue;
      }
      getResponse(get);
      System.out.println();
    }

    // Pre-populating DELETE requests
    System.out.println("\n=====5 DELETEs=====\n");
    List<String> deletes = Arrays.asList("DELETE watermelon", "delETe APPLE", "Delete Email", "delete orange", "delete blueberry");
    for (String delete : deletes) {
      ClientLogger.info("Automated input: " + delete);
      if (!verifyUserInput(delete)) {
        continue;
      }
      getResponse(delete);
      System.out.println();
    }
    System.out.println();
  }

  // Method to handle user requests
  public void handleUserRequests() {
    while (true) {
      String userInput = readUserInput();
      ClientLogger.info("User input: " + userInput);
      if (!verifyUserInput(userInput)) {
        continue;
      }
      getResponse(userInput);
    }
  }

  // Method to get response for a given user input
  private void getResponse(String userInput) {
    String response = sendRequestAndGetResponse(userInput);
    if (response.isEmpty()) {
      ClientLogger.error("Empty response.");
      return;
    }
    if (response.equals("Timeout")) {
      ClientLogger.error("Timeout: No response received from the server within " + TIMEOUT + " milliseconds.");
      return;
    }
    if (response.equals("IOException")) {
      return;
    }
    if (!Checksum.verifyChecksum(response)) {
      ClientLogger.error("Received malformed request of length " + response.length() + ": " + response);
      return;
    }

    // Dropping checksum from response
    String trimmedResponse = Checksum.dropChecksum(response);
    if (!verifyResponse(trimmedResponse)) {
      ClientLogger.error("Server returned invalid response: " + response);
      return;
    }
    if (!isResponseSuccess(trimmedResponse)) {
      ClientLogger.error("Received from server: " + getResponseMessage(response));
      System.out.println(getResponseMessage(trimmedResponse));
      return;
    }
    ClientLogger.info("Received from server: " + getResponseMessage(response));
    System.out.println(getResponseMessage(trimmedResponse));
  }

  // Method to read user input
  private String readUserInput() {
    System.out.print("Enter text using format of '<method> <key> <value>': ");
    String text = null;
    try {
      text = inputReader.readLine();
    } catch (IOException e) {
      ClientLogger.error(e.getMessage());
      System.exit(1);
    }
    return text;
  }

  // Method to verify user input format
  private boolean verifyUserInput(String text) {
    String[] textArray = text.split(" ");

    if (textArray.length < 2) {
      ClientLogger.error("Invalid input. Please refer to Readme for accepted input format.");
      return false;
    }

    String method = textArray[0].toLowerCase();
    if (!method.equals("put") && !method.equals("get") && !method.equals("delete")) {
      ClientLogger.error("Unknown method type. Please refer to Readme for accepted input format.");
      return false;
    }
    if ((method.equals("get") || method.equals("delete")) && textArray.length != 2 || method.equals("put") && textArray.length != 3) {
      ClientLogger.error("Invalid input. Please refer to Readme for accepted input format.");
      return false;
    }
    return true;
  }

  // Abstract method to send request and get response
  public abstract String sendRequestAndGetResponse(String userInput);

  // Method to verify server response
  private static boolean verifyResponse(String responseStr) {
    if (responseStr.length() <= 3) {
      return false;
    }
    if (responseStr.charAt(1) != ' ') {
      return false;
    }
    char status = responseStr.charAt(0);
    return status == '0' || status == '1';
  }

  // Method to check if response is successful
  private static boolean isResponseSuccess(String responseStr) {
    char status = responseStr.charAt(0);
    return status == '0';
  }

  // Method to get message from server response
  private static String getResponseMessage(String responseStr) {
    return responseStr.substring(2);
  }
}
