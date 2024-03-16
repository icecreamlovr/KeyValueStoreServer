package kvstore.client;

import kvstore.util.Checksum;

import java.io.*;
import java.net.*;

// UDPClient class extends AbstractClient
public class UDPClient extends AbstractClient {
  private DatagramSocket datagramSocket;
  private InetAddress serverAddress;

  // Constructor for UDPClient
  public UDPClient(String serverIp, int serverPort) {
    super(serverIp, serverPort);

    try {
      datagramSocket = new DatagramSocket();
      datagramSocket.setSoTimeout(AbstractClient.TIMEOUT);
      serverAddress = InetAddress.getByName(serverIp);
    } catch (SocketException | UnknownHostException e) {
      ClientLogger.error(e.getMessage());
      System.exit(1);
    }
  }

  // Override method to send request and get response
  @Override
  public String sendRequestAndGetResponse(String userInput) {
    DatagramPacket reply = null;
    // create a buffer to send the data
    try {
      String msg = Checksum.buildMsgWithChecksum(userInput);
      byte[] request = msg.getBytes();
      // preparing the packet and send the packet to server
      DatagramPacket packet = new DatagramPacket(request, request.length, serverAddress, serverPort);
      datagramSocket.send(packet);

      // Receive reply from the server
      byte[] bufferIn = new byte[1000];
      reply = new DatagramPacket(bufferIn, bufferIn.length);
      datagramSocket.receive(reply);
    } catch (SocketTimeoutException timeoutException) {
      return "Timeout";
    } catch (IOException e) {
      ClientLogger.error("UNEXPECTED IOException: " + e.getMessage());
      return "IOException";
    }
    return new String(reply.getData());
  }
}
