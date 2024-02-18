package server;

import util.Checksum;

import java.io.*;
import java.net.*;


public class TCPHandler extends AbstractHandler {
  private ServerSocket serverSocket;
  private static final String PROTOCOL = "TCP";

  public TCPHandler(int port) {
    try {
      serverSocket = new ServerSocket(port);
      ServerLogger.info("Server is listening on: " + port, PROTOCOL);
    } catch (IOException e) {
      ServerLogger.error(e.getMessage(), PROTOCOL);
      System.exit(1);
    }
  }

  @Override
  public void run() {
    while (true) {
      String clientIp = "";
      int clientPort;
      InputStream inputStream = null;
      OutputStream outputStream = null;

      try {
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

      while (true) {
        String receivedMsg = "";
        try {
          receivedMsg = reader.readLine();
        } catch (IOException e) {
          ServerLogger.error("Encountered issue while reading from client. Disconnecting... " + e.getMessage(), PROTOCOL);
          break;
        }

        if (receivedMsg == null) {
          ServerLogger.info("Client from " + clientIp + " disconnected", PROTOCOL);
          break;
        }

        ServerLogger.info("[Received from " + clientIp + ":" + clientPort + "] " + receivedMsg, PROTOCOL);
        String response = textHandler(receivedMsg).toString();

        String msg = Checksum.buildMsgWithChecksum(response);
        writer.println(msg);
        ServerLogger.info("[Sent to " + clientIp + ":" + clientPort + "] " + msg, PROTOCOL);
      }
    }
  }
}
