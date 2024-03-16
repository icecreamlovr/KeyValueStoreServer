package kvstore.server;

// Response class to represent responses from the server
public class Response {
  private final int statusCode;
  private final String message;
  private static final String GENERAL_ERR_MSG = "Please refer to Readme for accepted input format.";

  // Constructor for Response class
  public Response(int statusCode, String message) {
    this.statusCode = statusCode;
    this.message = message;
  }

  // Method to create response for malformed packet
  public static Response malformedPacket(String text) {
    String msg = "Received malformed request of length " + text.length() + ": " + text;
    return new Response(1, msg);
  }

  // Method to create response for unknown method
  public static Response unknownMethod(String method) {
    String msg = "Unknown method type: " + method + ". " + GENERAL_ERR_MSG;
    return new Response(1, msg);
  }

  // Method to create response for invalid input
  public static Response invalidInput(String text) {
    String msg = "Invalid input: " + text + ". " + GENERAL_ERR_MSG;
    return new Response(1, msg);
  }

  // Method to create response for success
  public static Response success(String msg) {
    return new Response(0, msg);
  }

  // Method to create response for key not exist
  public static Response keyNotExist(String key) {
    String msg = String.format("Key=%s doesn't exist", key);
    return new Response(1, msg);
  }

  // Method to convert response to string
  @Override
  public String toString() {
    return statusCode + " " + message;
  }
}
