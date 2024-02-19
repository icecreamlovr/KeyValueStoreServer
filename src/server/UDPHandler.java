package server;

import util.Checksum;

import java.io.IOException;
import java.net.*;

// UDPHandler class to handle UDP connections
public class UDPHandler extends AbstractHandler {
  private DatagramSocket datagramSocket;
  private static final String PROTOCOL = "UDP";

  // Constructor for UDPHandler
  public UDPHandler(int port) {
    try {
      datagramSocket = new DatagramSocket(port);
      ServerLogger.info("Server is listening on: " + port, PROTOCOL);
    } catch (IOException e) {
      ServerLogger.error(e.getMessage(), PROTOCOL);
      System.exit(1);
    }
  }

  // Override run method to handle incoming datagrams
  @Override
  public void run() {
    // Continuously listen for incoming datagrams
    while (true) {
      // establish the connection
      byte[] bufferIn = new byte[1024];
      DatagramPacket receivedPacket = new DatagramPacket(bufferIn, bufferIn.length);

      try {
        // Receive incoming datagram
        datagramSocket.receive(receivedPacket);
      } catch (IOException e) {
        ServerLogger.error(e.getMessage(), PROTOCOL);
        continue;
      }

      InetAddress clientAddress = receivedPacket.getAddress();
      int clientPort = receivedPacket.getPort();

      // Extract received text from the datagram
      String receivedText = new String(bufferIn, 0, receivedPacket.getLength());
      ServerLogger.info("[Received from " + clientAddress + ":" + clientPort + "] " + receivedText, PROTOCOL);

      // Handle the received message and get the response
      String response = textHandler(receivedText).toString();
      // Add checksum to the response
      String msg = Checksum.buildMsgWithChecksum(response);
      byte[] bufferOut = msg.getBytes();
      DatagramPacket replyPacket = new DatagramPacket(bufferOut,
              bufferOut.length,
              clientAddress,
              clientPort);
      try {
        // Send reply datagram to the client
        datagramSocket.send(replyPacket);
        ServerLogger.info("[Sent to " + clientAddress + ":" + clientPort + "] " + msg, PROTOCOL);
      } catch (IOException e) {
        ServerLogger.error(e.getMessage(), PROTOCOL);
      }
    }
  }
}
