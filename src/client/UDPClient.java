package client;

import java.io.*;
import java.net.*;

public class UDPClient {
  public static void main(String[] args) throws IOException {
    String serverIp = args[0];
    int serverPort = Integer.parseInt(args[1]);
    InetAddress ip = InetAddress.getByName(serverIp);


    DatagramSocket datagramSocket = null;
    try {
      datagramSocket = new DatagramSocket();
    } catch (SocketException e) {
      System.out.println("Socket: " + e.getMessage());
      datagramSocket.close();
    }

    while (true) {
      // read user input
      BufferedReader input = new BufferedReader(new InputStreamReader(System.in));
      System.out.print("Enter text using format of '<method> <key> <value>': ");
      String text = input.readLine();
      if (text.length() > 80) {
        throw new IllegalArgumentException("Input text size needs to be smaller than 80");
      }

      // create a buffer to send the data
      byte[] request = text.getBytes();

      // preparing the packet and send the packet to server
      DatagramPacket packet = new DatagramPacket(request, request.length, ip, serverPort);
      datagramSocket.send(packet);

      byte[] bufferIn = new byte[1000];
      DatagramPacket reply = new DatagramPacket(bufferIn, bufferIn.length);
      datagramSocket.receive(reply);

      System.out.println("Response from server: " + new String(reply.getData()));
    }
  }
}
