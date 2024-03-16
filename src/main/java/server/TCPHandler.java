package kvstore.server;

import kvstore.util.Checksum;

import java.io.*;
import java.net.*;

// TCPHandler class to handle TCP connections
public class TCPHandler extends AbstractHandler {
  private ServerSocket serverSocket;
  private static final String PROTOCOL = "TCP";

  // Constructor for TCPHandler
  public TCPHandler(int port) {
    try {
      serverSocket = new ServerSocket(port);
      ServerLogger.info("Server is listening on: " + port, PROTOCOL);
    } catch (IOException e) {
      ServerLogger.error(e.getMessage(), PROTOCOL);
      System.exit(1);
    }
  }

  // Override run method to handle incoming connections
  @Override
  public void run() {
    // Continuously listen for incoming connections
    while (true) {
      String clientIp = "";
      int clientPort;
      InputStream inputStream = null;
      OutputStream outputStream = null;

      try {
        // Accept incoming connection
        Socket socket = serverSocket.accept();
        clientIp = socket.getInetAddress().toString();
        clientPort = socket.getPort();
        ServerLogger.info("Client from " + clientIp + " connected", PROTOCOL);
        inputStream = socket.getInputStream();
        outputStream = socket.getOutputStream();
      } catch (IOException e) {
        ServerLogger.error(e.getMessage(), PROTOCOL);
        continue;
      }

      BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
      PrintWriter writer = new PrintWriter(outputStream, true);

      // Continuously read messages from the client
      while (true) {
        String receivedMsg = "";
        try {
          receivedMsg = reader.readLine();
        } catch (IOException e) {
          ServerLogger.error("Encountered issue while reading from client. Disconnecting... " + e.getMessage(), PROTOCOL);
          break;
        }

        // Check if the client has disconnected
        if (receivedMsg == null) {
          ServerLogger.info("Client from " + clientIp + " disconnected", PROTOCOL);
          break;
        }

        ServerLogger.info("[Received from " + clientIp + ":" + clientPort + "] " + receivedMsg, PROTOCOL);
        // Handle the received message and get the response
        String response = textHandler(receivedMsg).toString();
        // Add checksum to the response
        String msg = Checksum.buildMsgWithChecksum(response);
        // Send the response to the client
        writer.println(msg);
        ServerLogger.info("[Sent to " + clientIp + ":" + clientPort + "] " + msg, PROTOCOL);
      }
    }
  }
}
