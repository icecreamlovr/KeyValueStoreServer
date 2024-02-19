package server;

import util.Checksum;

// AbstractHandler class implementing Runnable interface
public abstract class AbstractHandler implements Runnable {
  private final KeyValue database;

  // Constructor for AbstractHandler
  public AbstractHandler() {
    database = new KeyValue();
  }

  // Method to handle text-based requests
  protected Response textHandler(String text) {
    // Verify checksum of the text
    if (!Checksum.verifyChecksum(text)) {
      return Response.malformedPacket(text);
    } else {
      text = Checksum.dropChecksum(text);
    }

    // Split text into method, key, and value (if applicable)
    String[] textArray = text.split(" ");
    if (textArray.length != 2 && textArray.length != 3) {
      return Response.invalidInput(text);
    }

    // Extract method and key
    String method = textArray[0].toLowerCase();
    String key = textArray[1].toLowerCase();

    // Handle different methods: PUT, GET, DELETE
    if (method.equals("put")) {
      if (textArray.length != 3) {
        return Response.invalidInput(text);
      }
      return putHandler(key, textArray[2].toLowerCase());
    } else if (method.equals("get")) {
      return getHandler(textArray[1].toLowerCase());
    } else if (method.equals("delete")) {
      return deleteHandler(textArray[1].toLowerCase());
    } else {
      return Response.unknownMethod(method);
    }
  }

  // Method to handle PUT request
  private Response putHandler(String key, String value) {
    database.put(key, value);
    String msg = String.format("{%s : %s} has been added!", key, value);
    return Response.success(msg);
  }

  // Method to handle GET request
  private Response getHandler(String key) {
    if (!database.containsKey(key)) {
      return Response.keyNotExist(key);
    }
    String value = database.get(key);
    String msg = String.format("Value of \"%s\" is: \"%s\"", key, value);
    return Response.success(msg);
  }

  // Method to handle DELETE request
  private Response deleteHandler(String key) {
    if (!database.containsKey(key)) {
      return Response.keyNotExist(key);
    }
    String value = database.remove(key);
    if (value.equals("")) {
      return null;
    }
    String msg = String.format("{%s : %s} has been deleted!", key, value);
    return Response.success(msg);
  }
}
