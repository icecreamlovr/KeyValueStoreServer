package server;

import client.ClientLogger;
import util.Checksum;

public abstract class AbstractHandler implements Runnable {
  private final KeyValue database;

  public AbstractHandler() {
    database = new KeyValue();
  }

  protected Response textHandler(String text) {
    if (!Checksum.verifyChecksum(text)) {
      return Response.malformedPacket(text);
    } else {
      text = Checksum.dropChecksum(text);
    }

    String[] textArray = text.split(" ");
    if (textArray.length != 2 && textArray.length != 3) {
      return Response.invalidInput(text);
    }

    String method = textArray[0].toLowerCase();
    String key = textArray[1].toLowerCase();

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

  private Response putHandler(String key, String value) {
    database.put(key, value);
    String msg = String.format("key=%s & value=%s has been added!", key, value);
    return Response.success(msg);
  }

  private Response getHandler(String key) {
    if (!database.containsKey(key)) {
      return Response.keyNotExist(key);
    }
    String value = database.get(key);
    String msg = String.format("value of key=%s is: %s", key, value);
    return Response.success(msg);
  }

  private Response deleteHandler(String key) {
    if (!database.containsKey(key)) {
      return Response.keyNotExist(key);
    }
    database.remove(key);
    String msg = String.format("KeyValue pair of key=%s has been deleted!", key);
    return Response.success(msg);
  }
}
