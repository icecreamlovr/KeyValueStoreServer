package server;

import util.Checksum;

import java.io.IOException;
import java.net.*;

public class UDPHandler extends AbstractHandler {
  private DatagramSocket datagramSocket;
  private static final String PROTOCOL = "UDP";

  public UDPHandler(int port) {
    try {
      datagramSocket = new DatagramSocket(port);
      ServerLogger.info("Server is listening on: " + port, PROTOCOL);
    } catch (IOException e) {
      ServerLogger.error(e.getMessage(), PROTOCOL);
      System.exit(1);
    }
  }

  @Override
  public void run() {
    while (true) {
      // establish the connection
      byte[] bufferIn = new byte[1024];
      DatagramPacket receivedPacket = new DatagramPacket(bufferIn, bufferIn.length);

      try {
        datagramSocket.receive(receivedPacket);
      } catch (IOException e) {
        ServerLogger.error(e.getMessage(), PROTOCOL);
        continue;
      }

      InetAddress clientAddress = receivedPacket.getAddress();
      int clientPort = receivedPacket.getPort();

      String receivedText = new String(bufferIn, 0, receivedPacket.getLength());
      ServerLogger.info("[Received from " + clientAddress + ":" + clientPort + "] " + receivedText, PROTOCOL);

      String response = textHandler(receivedText).toString();
      String msg = Checksum.buildMsgWithChecksum(response);
      byte[] bufferOut = msg.getBytes();
      DatagramPacket replyPacket = new DatagramPacket(bufferOut,
              bufferOut.length,
              clientAddress,
              clientPort);
      try {
        datagramSocket.send(replyPacket);
        ServerLogger.info("[Sent to " + clientAddress + ":" + clientPort + "] " + msg, PROTOCOL);
      } catch (IOException e) {
        ServerLogger.error(e.getMessage(), PROTOCOL);
      }
    }
  }
}
