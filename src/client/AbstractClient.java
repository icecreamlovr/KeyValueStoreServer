package client;

import java.io.*;
import java.util.Map;

public abstract class AbstractClient implements Client {
  protected String serverIp;
  protected int serverPort;
  private final BufferedReader inputReader;

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
      String response = sendRequest(userInput);
      if (!verifyResponse(response)) {
        ClientLogger.error("Server returned malformed response: " + response);
        continue;
      }
      if (isResponseSuccess(response)) {
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

  public abstract String sendRequest(String userInput);

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
