package client;

import java.time.LocalDateTime;

public class ClientLogger {
  public static void error(String text) {
    String timestamp = LocalDateTime.now().toString();
    System.out.println(timestamp + " [ERROR] " + text);
  }

  public static void info(String text) {
    String timestamp = LocalDateTime.now().toString();
    System.out.println(timestamp + " [INFO] " + text);
  }
}
