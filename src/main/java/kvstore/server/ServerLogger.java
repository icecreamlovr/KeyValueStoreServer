package kvstore.server;

import java.time.LocalDateTime;

// ServerLogger class for logging server messages
public class ServerLogger {
  // Port number to distinguish logs from different server replicas.
  private static int serverPort = -1;

  public static void setPort(int port) {
    serverPort = port;
  }

  // Method to log error messages without protocol information
  public static void error(String text) {
    LocalDateTime time = LocalDateTime.now();
    String identifier = serverPort == -1 ? "[ERROR]" : String.format("[%d][ERROR]", serverPort);
    System.err.println(String.format("%s %s %s", time, identifier, text));
  }

  // Method to log informational messages without protocol information
  public static void info(String text) {
    LocalDateTime time = LocalDateTime.now();
    String identifier = serverPort == -1 ? "[INFO]" : String.format("[%d][INFO]", serverPort);
    System.out.println(String.format("%s %s %s", time, identifier, text));
  }
}
