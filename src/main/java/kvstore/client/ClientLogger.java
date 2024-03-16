package kvstore.client;

import java.time.LocalDateTime;

// Class to handle logging messages
public class ClientLogger {
  // Method to log error messages
  public static void error(String text) {
    System.out.println(LocalDateTime.now() + " [ERROR] " + text);
  }

  // Method to log informational messages
  public static void info(String text) {
    System.out.println(LocalDateTime.now() + " [INFO] " + text);
  }
}
