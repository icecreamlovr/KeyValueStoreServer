package server;

public class Response {
  private final int statusCode;
  private final String message;
  private static final String GENERAL_ERR_MSG = "Please refer to Readme for accepted input format.";

  public Response(int statusCode, String message) {
    this.statusCode = statusCode;
    this.message = message;
  }

  public static Response malformedPacket(String text) {
    String msg = "Received malformed request of length " + text.length() + ": " + text;
    return new Response(1, msg);
  }

  public static Response unknownMethod(String method) {
    String msg = "Unknown method type: " + method + ". " + GENERAL_ERR_MSG;
    return new Response(1, msg);
  }

  public static Response invalidInput(String text) {
    String msg = "Invalid input: " + text + ". " + GENERAL_ERR_MSG;
    return new Response(1, msg);
  }

  public static Response success(String msg) {
    return new Response(0, msg);
  }

  public static Response keyNotExist(String key) {
    String msg = String.format("Key=%s doesn't exist", key);
    return new Response(1, msg);
  }

  @Override
  public String toString() {
    return statusCode + " " + message;
  }
}
