package client;

import util.Checksum;

import java.io.*;

public abstract class AbstractClient implements Client {
  protected String serverIp;
  protected int serverPort;
  private final BufferedReader inputReader;
  protected static final int TIMEOUT = 5000;

  public AbstractClient(String serverIp, int serverPort) {
    this.serverIp = serverIp;
    this.serverPort = serverPort;
    inputReader = new BufferedReader(new InputStreamReader(System.in));
  }

  public void handleUserRequests() {
    while (true) {
      String userInput = readUserInput();
      if (!verifyUserInput(userInput)) {
        continue;
      }

      String response = sendRequestAndGetResponse(userInput);
      String trimmedResponse = response;
      if (response.isEmpty()) {
        ClientLogger.error("Empty response.");
        continue;
      }
      if (response.equals("Timeout")) {
        ClientLogger.error("Timeout: No response received from the server within " + TIMEOUT + " milliseconds.");
        continue;
      }
      if (response.equals("IOException")) {
        continue;
      }
      if (!Checksum.verifyChecksum(response)) {
        ClientLogger.error("Received malformed request of length " + response.length() + ": " + response);
        continue;
      } else {
        trimmedResponse = Checksum.dropChecksum(response);
      }
      if (!verifyResponse(trimmedResponse)) {
        ClientLogger.error("Server returned invalid response: " + response);
        continue;
      }
      if (isResponseSuccess(trimmedResponse)) {
        ClientLogger.info(getResponseMessage(response));
      } else {
        ClientLogger.error(getResponseMessage(response));
      }
    }
  }

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

  public abstract String sendRequestAndGetResponse(String userInput);

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

  private static boolean isResponseSuccess(String responseStr) {
    char status = responseStr.charAt(0);
    return status == '0';
  }


  private static String getResponseMessage(String responseStr) {
    return responseStr.substring(2);
  }
}
