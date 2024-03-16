package kvstore.client;

import kvstore.util.Checksum;

import java.net.*;
import java.io.*;

// TCPClient class extends AbstractClient
public class TCPClient extends AbstractClient {
  private PrintWriter writer;
  private BufferedReader reader;

  // Constructor for TCPClient
  public TCPClient(String serverIp, int serverPort) {
    super(serverIp, serverPort);
    try {
      Socket socket = new Socket(serverIp, serverPort);
      socket.setSoTimeout(AbstractClient.TIMEOUT);
      writer = new PrintWriter(socket.getOutputStream(), true);
      reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
    } catch (IOException e) {
      ClientLogger.error(e.getMessage());
      System.exit(1);
    }
  }

  // Override method to send request and get response
  @Override
  public String sendRequestAndGetResponse(String userInput) {
    // send message to server
    String msg = Checksum.buildMsgWithChecksum(userInput);
    writer.println(msg);

    // read message from server
    String response;
    try {
      response = reader.readLine();
    } catch (SocketTimeoutException timeoutException) {
      return "Timeout";
    } catch (IOException e) {
      ClientLogger.error("UNEXPECTED IOException: " + e.getMessage());
      return "IOException";
    }
    return response;
  }
}
