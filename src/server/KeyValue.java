package server;

import java.util.HashMap;
import java.util.Map;

public class KeyValue {
  private final Map<String, String> store;

  public KeyValue() {
    store = new HashMap<>();
  }

  public void put(String key, String value) {
    store.put(key, value);
  }

  public String get(String key) {
    if (!store.containsKey(key)) {
      String msg = String.format("Key=%s doesn't exist", key);
      ServerLogger.error(msg);
      return "";
    }
    return store.get(key);

  }

  public void remove(String key) {
    if (!store.containsKey(key)) {
      String msg = String.format("Key=%s doesn't exist", key);
      ServerLogger.error(msg);
      return;
    }
    store.remove(key);
  }

  public boolean containsKey(String key) {
    return store.containsKey(key);
  }
}
