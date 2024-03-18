package kvstore.client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.List;

// Main class to start the client application
public class ClientApp {
  public static void main(String[] args) {
//    CliFlags flags = parseCli(args);
    // Initialize client based on protocol specified
    // TODO: add try-catch in 1) create stub; 2) while loop
    RPCClient client = new RPCClient();
//    RPCClient client = new RPCClient(flags.serverIp, flags.serverPort);
    prePopulateRequests(client);
    handleUserInputLoop(client);
  }

  // Inner class to hold parsed command-line arguments
  private static class CliFlags {
    private final String serverIp;
    private final int serverPort;

    // Constructor for CliFlags
    public CliFlags(String serverIp, int serverPort) {
      this.serverIp = serverIp;
      this.serverPort = serverPort;
    }
  }

  // Method to parse command-line arguments
  private static CliFlags parseCli(String[] args) {
    if (args.length < 2) {
      ClientLogger.error("ClientApp <server-ip> <server-port> are not specified");
      System.exit(1);
    }

    String serverIp = args[0];

    int serverPort = Integer.parseInt(args[1]);
    if (serverPort < 0 || serverPort > 65535) {
      ClientLogger.error("Invalid port number: " + serverPort);
      System.exit(1);
    }
    return new CliFlags(serverIp, serverPort);
  }

  // Method to pre-populate requests with sample data
  public static void prePopulateRequests(RPCClient client) {
    System.out.println("\n=====Pre-populated Key-Value Store=====\n");

    // Pre-populating PUT requests
    System.out.println("\n=====5 PUTs=====\n");
    List<String> puts = Arrays.asList(
            "put watermelon Summer",
            "PUT apple fruit",
            "Put apple fall",
            "pUt email 123@gmail.com",
            "PUT blueberry summer");
    for (String put : puts) {
      ClientLogger.info("Automated input: " + put);
      if (!verifyUserInput(put)) {
        continue;
      }
      sendRequest(put, client);
      System.out.println();
    }

    // Pre-populating GET requests
    System.out.println("\n=====5 GETs=====\n");
    List<String> gets = Arrays.asList(
            "get watermelon",
            "GET apple",
            "Get email",
            "gEt orange",
            "get blueberry");
    for (String get : gets) {
      ClientLogger.info("Automated input: " + get);
      if (!verifyUserInput(get)) {
        continue;
      }
      sendRequest(get, client);
      System.out.println();
    }

    // Pre-populating DELETE requests
    System.out.println("\n=====5 DELETEs=====\n");
    List<String> deletes = Arrays.asList(
            "DELETE watermelon",
            "delETe APPLE",
            "Delete Email",
            "delete orange",
            "delete blueberry");
    for (String delete : deletes) {
      ClientLogger.info("Automated input: " + delete);
      if (!verifyUserInput(delete)) {
        continue;
      }
      sendRequest(delete, client);
      System.out.println();
    }
    System.out.println();
  }

  // Handle user inputs and send the corresponding RPC requests in a loop
  public static void handleUserInputLoop(RPCClient client) {
    BufferedReader inputReader = new BufferedReader(new InputStreamReader(System.in));
    while (true) {
      String userInput = readUserInput(inputReader);
      ClientLogger.info("User input: " + userInput);
      if (!verifyUserInput(userInput)) {
        continue;
      }
      sendRequest(userInput, client);
    }
  }

  // Method to read user input
  private static String readUserInput(BufferedReader inputReader) {
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
  private static boolean verifyUserInput(String text) {
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

  // Method to send different RPC requests based on user inputs
  private static void sendRequest(String request, RPCClient client) {
    String[] arr = request.split(" ");
    String method = arr[0].toLowerCase();
    String key = arr[1];
    if (method.equals("put")) {
      client.put(key, arr[2]);
    } else if (method.equals("get")) {
      client.get(key);
    } else if (method.equals("delete")) {
      client.delete(key);
    }
  }
}
