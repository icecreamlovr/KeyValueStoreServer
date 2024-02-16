package server;

import java.time.LocalDateTime;

public class ServerLogger {
  // wrong! when is this calculated???
  static String timestamp = LocalDateTime.now().toString();

  public static void error(String text) {
    System.out.println(timestamp + " [ERROR] " + text);
  }

  public static void error(String text, String protocol) {
    String timestamp = LocalDateTime.now().toString();
    System.out.println(timestamp + " [ERROR-" + protocol + "] " + text);
  }

  public static void info(String text) {
    System.out.println(timestamp + " [INFO] " + text);
  }

  public static void info(String text, String protocol) {
    System.out.println(timestamp + " [INFO-" + protocol + "] " + text);
  }


}
