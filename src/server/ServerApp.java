package server;

public class ServerApp {
  public static void main(String[] args) {
    // parse cli
    CliFlags flags = parseCli(args);

    AbstractHandler tcpHandler = new TCPHandler(flags.tcpPort);
    Thread tcpThread = new Thread(tcpHandler);
    tcpThread.start();

    AbstractHandler udpHandler = new UDPHandler(flags.udpPort);
    Thread udpThread = new Thread(udpHandler);
    udpThread.start();
  }

  private static CliFlags parseCli(String[] args) {
    if (args.length != 2) {
      ServerLogger.error("ServerApp <tcp-port-number> <udp-port-number> are not specified");
      System.exit(1);
    }

    int tcpPort = Integer.parseInt(args[0]);
    if (tcpPort < 0 || tcpPort > 65535) {
      ServerLogger.error("Invalid TCP port number: " + tcpPort);
      System.exit(1);
    }
    int udpPort = Integer.parseInt(args[1]);
    if (udpPort < 0 || udpPort > 65535) {
      ServerLogger.error("Invalid UDP port number: " + udpPort);
      System.exit(1);
    }
    return new CliFlags(tcpPort, udpPort);
  }

  private static class CliFlags {
    private final int tcpPort;
    private final int udpPort;

    public CliFlags(int tcpPort, int udpPort) {
      this.tcpPort = tcpPort;
      this.udpPort = udpPort;
    }
  }
}
