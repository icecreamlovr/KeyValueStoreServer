package client;

import java.net.*;
import java.io.*;

public class TCPClient {
  public static void main(String[] args) throws IOException {
    if (args.length != 2) {
      throw new IllegalArgumentException("Please provide server IP and port");
    }

    String serverIp = args[0];
    int serverPort = Integer.parseInt(args[1]);
    Socket socket = new Socket(serverIp, serverPort);

    while (true) {
      // read user input
      BufferedReader input = new BufferedReader(new InputStreamReader(System.in));
      System.out.print("Enter text using format of '<method> <key> <value>': ");
      String text = input.readLine();
      if (text.length() > 80) {
        throw new IllegalArgumentException("Input text size needs to be smaller than 80");
      }

      // send message to server
      PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
      out.println(text);

      // read message from server
      BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
      String response = in.readLine();
      System.out.println("Response from server: " + response);
    }
  }
}
