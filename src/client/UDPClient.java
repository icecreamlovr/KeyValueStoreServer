package client;

import java.io.*;
import java.net.*;

public class UDPClient extends AbstractClient {
  private DatagramSocket datagramSocket;
  private InetAddress serverAddress;

  public UDPClient(String serverIp, int serverPort) {
    super(serverIp, serverPort);

    try {
      datagramSocket = new DatagramSocket();
      serverAddress = InetAddress.getByName(serverIp);
    } catch (SocketException | UnknownHostException e) {
      System.err.println("[Error]" + e.getMessage());
      System.exit(1);
    }
  }

  @Override
  public String sendRequest(String userInput) {
    DatagramPacket reply = null;
    // create a buffer to send the data
    try {
      byte[] request = userInput.getBytes();
      // preparing the packet and send the packet to server
      DatagramPacket packet = new DatagramPacket(request, request.length, serverAddress, serverPort);
      datagramSocket.send(packet);

      byte[] bufferIn = new byte[1000];
      reply = new DatagramPacket(bufferIn, bufferIn.length);
      datagramSocket.receive(reply);
    } catch (IOException e) {
      ClientLogger.error(e.getMessage());
      System.exit(1);
    }
    return new String(reply.getData());
  }
}
