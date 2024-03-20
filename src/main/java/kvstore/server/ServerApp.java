package kvstore.server;

import io.grpc.*;

// ServerApp class to start the server application
public class ServerApp {
  public static void main(String[] args) throws InterruptedException {
    // Parse command-line arguments
    CliFlags flags = parseCli(args);
    RPCServer rpcServer = new RPCServer(flags.port);
    rpcServer.start();
    rpcServer.blockUntilShutdown();
  }

  // Method to parse command-line arguments
  private static CliFlags parseCli(String[] args) {
    if (args.length < 1) {
      ServerLogger.error("ServerApp <port-number> is not specified");
      System.exit(1);
    }
    int port = Integer.parseInt(args[0]);
    if (port < 0 || port > 65535) {
      ServerLogger.error("Invalid RPC port number: " + port);
      System.exit(1);
    }
    return new CliFlags(port);
  }

  // Inner class to hold parsed command-line arguments
  private static class CliFlags {
    private final int port;

    // Constructor for CliFlags
    public CliFlags(int port) {
      this.port = port;
    }
  }
}
