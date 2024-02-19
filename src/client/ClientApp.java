package client;

// Main class to start the client application
public class ClientApp {
  public static void main(String[] args) {
    CliFlags flags = parseCli(args);
    // Initialize client based on protocol specified
    Client client = null;
    if (flags.protocol.equals("TCP")) {
      client = new TCPClient(flags.serverIp, flags.serverPort);
    } else if (flags.protocol.equals("UDP")) {
      client = new UDPClient(flags.serverIp, flags.serverPort);
    } else {
      ClientLogger.error("Invalid protocol input: " + flags.protocol);
      System.exit(1);
    }
    client.prePopulateRequests();
    client.handleUserRequests();

  }

  // Method to parse command-line arguments
  private static CliFlags parseCli(String[] args) {
    if (args.length != 3) {
      ClientLogger.error("ClientApp <server-ip> <server-port> <protocol> are not specified");
      System.exit(1);
    }

    String serverIp = args[0];

    int serverPort = Integer.parseInt(args[1]);
    if (serverPort < 0 || serverPort > 65535) {
      ClientLogger.error("Invalid port number: " + serverPort);
      System.exit(1);
    }

    String protocol = args[2].toUpperCase();
    if (!protocol.equals("TCP") && !protocol.equals("UDP")) {
      ClientLogger.error("Invalid protocol: " + protocol);
      System.exit(1);
    }
    return new CliFlags(serverIp, serverPort, protocol);
  }

  // Inner class to hold parsed command-line arguments
  private static class CliFlags {
    private final String serverIp;
    private final int serverPort;
    private final String protocol;

    // Constructor for CliFlags
    public CliFlags(String serverIp, int serverPort, String protocol) {
      this.serverIp = serverIp;
      this.serverPort = serverPort;
      this.protocol = protocol;
    }
  }
}
