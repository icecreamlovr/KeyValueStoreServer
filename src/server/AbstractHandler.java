package server;

public abstract class AbstractHandler implements Runnable {
  private final KeyValue database;
  
  public AbstractHandler() {
    database = new KeyValue();
  }

  protected Response textHandler(String text) {
    String[] textArray = text.split(" ");

    String msg = "";

    if (textArray.length != 2 && textArray.length != 3) {
      return Response.invalidInput(text);
    }

    String method = textArray[0].toLowerCase();
    if (!method.equals("put") && !method.equals("get") && !method.equals("delete")) {
      return Response.unknownMethod(method);
    }

    String key = textArray[1].toLowerCase();

    if (method.equals("put")) {
      if (textArray.length != 3) {
        return Response.invalidInput(text);
      }
      String value = textArray[2].toLowerCase();
      database.put(key, value);
      msg = String.format("key=%s & value=%s has been added!", key, value);
      return Response.success(msg);
    }

    if (!database.containsKey(key)) {
      return Response.keyNotExist(key);
    }
    if (method.equals("get")) {
      String value = database.get(key);
      msg = String.format("value of key=%s is: %s", key, value);
    }
    if (method.equals("delete")) {
      database.remove(key);
      msg = String.format("KeyValue pair of key=%s has been deleted!", key);
    }
    return Response.success(msg);
  }
}
