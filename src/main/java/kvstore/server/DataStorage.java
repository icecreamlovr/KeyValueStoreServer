package kvstore.server;

import java.util.HashMap;
import java.util.Map;

// KeyValue class to store key-value pairs
public class DataStorage {
  private final Map<String, String> store;

  // Constructor for KeyValue class
  public DataStorage() {
    store = new HashMap<>();
  }

  // Method to put a key-value pair into the store
  public void put(String key, String value) {
    store.put(key, value);
  }

  // Method to get the value associated with a key
  public String get(String key) {
    return store.get(key);
  }

  // Method to remove a key-value pair from the store
  public String delete(String key) {
    return store.remove(key);
  }

  // Method to check if the store contains a specific key
  public boolean containsKey(String key) {
    return store.containsKey(key);
  }
}
