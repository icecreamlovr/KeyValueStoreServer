package server;

import java.io.*;
import java.net.*;
import java.util.*;

public class UDPHandler {
  public static void main(String[] args) throws IOException {
    if (args.length < 1) {
      System.out.println("Usage: java UDPServer <Port Number>");
      System.exit(1);
    }

    Map<String, String> map = new HashMap<>();

    try {
      // establish the connection
      int port = Integer.parseInt(args[0]);
      System.out.println("server is listening on: " + port);

      DatagramSocket datagramSocket = new DatagramSocket(port);
      byte[] bufferIn = new byte[1024];

      DatagramPacket request = new DatagramPacket(bufferIn, bufferIn.length);

      while (true) {
        datagramSocket.receive(request);
        InetAddress ip = request.getAddress();
        String text = new String(bufferIn, 0, request.getLength());
        System.out.println("Server received text: " + text + " from client " + ip);

        byte[] bufferOut = textHandler(map, text).getBytes();
        DatagramPacket reply = new DatagramPacket(bufferOut,
                bufferOut.length,
                request.getAddress(),
                request.getPort());
        datagramSocket.send(reply);
      }
    } catch (SocketException e) {
      System.out.println("Socket: " + e.getMessage());
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
