package server;

import java.io.*;
import java.net.*;
import java.util.*;


public class TCPHandler {
  public static void main(String[] args) throws IOException {
    if (args.length != 1) {
      throw new IllegalArgumentException("Please provide server port");
    }

    // establish the connection
    int port = Integer.parseInt(args[0]);
    System.out.println("server is listening on: " + port);
    ServerSocket serverSocket = new ServerSocket(port);
    Socket socket = serverSocket.accept();
    String clientIp = socket.getInetAddress().toString();
    System.out.println(String.format("client from %s connected", clientIp));

    // temporary
    Map<String, String> map = new HashMap<>();

    while (true) {
      // read message from client
      InputStream input = socket.getInputStream();
      BufferedReader in = new BufferedReader(new InputStreamReader(input));
      PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
      String text = in.readLine();

      if (text != null) {
        System.out.println("Server received: " + text);
        String responseStr = textHandler(map, text);
        out.println(responseStr);
      }
    }
  }

  private static String textHandler(Map<String, String> map, String text) {
    String[] textArray = text.split(" ");

    if (textArray.length != 2 && textArray.length != 3) {
      return "[ERROR] Invalid input. Please refer to Readme for accepted input format.";
    }

    String method = textArray[0].toLowerCase();
    if (!method.equals("put") && !method.equals("get") && !method.equals("delete")) {
      return "[ERROR] Unknown method type. Please refer to Readme for accepted input format.";
    }

    String key = textArray[1].toLowerCase();

    if (method.equals("put")) {
      if (textArray.length != 3) {
        return "[ERROR] Invalid input. Please refer to Readme for accepted input format.";
      }
      String value = textArray[2].toLowerCase();
      map.put(key, value);
      return String.format("key=%s & value=%s has been added!", key, value);
    }

    if (!map.containsKey(key)) {
      return String.format("Key=%s doesn't exist", key);
    }
    if (method.equals("get")) {
      String value = map.get(key);
      return String.format("value of key=%s is: %s", key, value);
    }
    if (method.equals("delete")) {
      map.remove(key);
      return String.format("KeyValue pair of key=%s has been deleted!", key);
    }
    return "[ERROR] Invalid input. Please refer to Readme for accepted input format.";
  }
}
