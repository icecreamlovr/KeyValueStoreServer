package kvstore.server;

import io.grpc.*;
import java.util.ArrayList;
import java.util.List;

// ServerApp class to start the server application
public class ServerApp {
  public static void main(String[] args) throws InterruptedException {
    // Parse command-line arguments
    CliFlags flags = CliFlags.parseCli(args);
    ServerLogger.setPort(flags.port);
    RPCServer rpcServer = new RPCServer(flags.port, flags.allReplicaPorts);
    rpcServer.start();
    rpcServer.blockUntilShutdown();
  }

  // Inner class to hold parsed command-line arguments
  private static class CliFlags {
    private static final List<Integer> DEFAULT_ALL_REPLICA_PORTS = List.of(3333,3334,3335,3336,3337);
    private static final int NUMBER_OF_REPLICAS = 5;

    private final int port;
    private final List<Integer> allReplicaPorts;

    // Private constructor. Only to be invoked by parseCli().
    private CliFlags(int port, List<Integer> allReplicaPorts) {
      this.port = port;
      this.allReplicaPorts = allReplicaPorts;
    }

    /**
     * Parse CLI flags. Perform validations.
     */
    public static CliFlags parseCli(String[] args) {
      if (args.length < 1) {
        printUsage();
        ServerLogger.error("<port-number> is not specified");
        System.exit(1);
      }

      if (args.length == 1) {
        ServerLogger.info(String.format("Running server with argument: port=%s", args[0]));
      } else {
        ServerLogger.info(String.format("Running server with argument: port=%s, all-replicas-port-number=%s", args[0], args[1]));
      }

      int port = parsePortOrFail(args[0]);

      List<Integer> allReplicaPorts;
      if (args.length == 1) {
        // Default replica ports
        ServerLogger.info("Using default replica ports: " + DEFAULT_ALL_REPLICA_PORTS);
        allReplicaPorts = DEFAULT_ALL_REPLICA_PORTS;
      } else {
        String[] allReplicaPortsString = args[1].split(",");
        // Check number of replicas
        if (allReplicaPortsString.length != NUMBER_OF_REPLICAS) {
          printUsage();
          ServerLogger.error(
              String.format(
                  "Invalid number of replicas. Must be %d. Got %d.",
                  NUMBER_OF_REPLICAS,
                  allReplicaPortsString.length));
          System.exit(1);
        }
        allReplicaPorts = new ArrayList<>(NUMBER_OF_REPLICAS);
        for (String portStr : allReplicaPortsString) {
          int onePort = parsePortOrFail(portStr);
          // Check duplicates
          if (allReplicaPorts.contains(onePort)) {
            printUsage();
            ServerLogger.error("Duplicate replica port number: " + onePort);
            System.exit(1);
          }
          allReplicaPorts.add(onePort);
        }
      }
      // Check that server port must be included in replica ports
      if (!allReplicaPorts.contains(port)) {
        printUsage();
        ServerLogger.error("Server port must be included in the replica port numbers");
        System.exit(1);
      }

      return new CliFlags(port, allReplicaPorts);
    }

    private static void printUsage() {
      String usage = "Usage: ServerApp <port-number> [<all-replicas-port-number>]\n"
              + "  <port-number>: Port number of this replica. Must be between 0 and 65535\n"
              + "  <all-replicas-port-number>: Port numbers of the other replicas. Specify as comma-separated integers\n"
              + "      Optional flag. If unspecified, will use default value " + DEFAULT_ALL_REPLICA_PORTS;
      System.out.println(usage);
    }

    //  Parses a port number from a string and
    //  exits with an error if parsing fails or the port number is invalid.
    private static int parsePortOrFail(String portStr) {
      int parsed = 0;
      try {
        parsed = Integer.parseInt(portStr);
      } catch(Exception e) {
        ServerLogger.error(String.format("Unable to parse port \"%s\" as integer. Got:\n%s", portStr, e.getMessage()));
        System.exit(1);
      }
      if (parsed < 0 || parsed > 65535) {
        printUsage();
        ServerLogger.error("Invalid server port number. Must be between 0 and 65536. Got: " + parsed);
        System.exit(1);
      }
      return parsed;
    }
  }
}
