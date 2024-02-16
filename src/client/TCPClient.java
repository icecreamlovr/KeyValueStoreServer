package client;

import java.net.*;
import java.io.*;

public class TCPClient extends AbstractClient {
  private PrintWriter writer;
  private BufferedReader reader;

  public TCPClient(String serverIp, int serverPort) {
    super(serverIp, serverPort);
    try {
      Socket socket = new Socket(serverIp, serverPort);
      writer = new PrintWriter(socket.getOutputStream(), true);
      reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
    } catch (IOException e) {
      ClientLogger.error(e.getMessage());
      System.exit(1);
    }
  }

  @Override
  public String sendRequest(String userInput) {
    // send message to server
    writer.println(userInput);

    // read message from server
    String response = null;
    try {
      response = reader.readLine();
    } catch (IOException e) {
      ClientLogger.error(e.getMessage());
      System.exit(1);
    }
    return response;
  }
}
