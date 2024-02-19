package server;

import java.time.LocalDateTime;

// ServerLogger class for logging server messages
public class ServerLogger {
  // Method to log error messages without protocol information
  public static void error(String text) {
    System.out.println(LocalDateTime.now() + " [ERROR] " + text);
  }

  // Method to log error messages with protocol information
  public static void error(String text, String protocol) {
    System.out.println(LocalDateTime.now() + " [ERROR-" + protocol + "] " + text);
  }

  // Method to log informational messages without protocol information
  public static void info(String text) {
    System.out.println(LocalDateTime.now() + " [INFO] " + text);
  }

  // Method to log informational messages with protocol information
  public static void info(String text, String protocol) {
    System.out.println(LocalDateTime.now() + " [INFO-" + protocol + "] " + text);
  }
}
