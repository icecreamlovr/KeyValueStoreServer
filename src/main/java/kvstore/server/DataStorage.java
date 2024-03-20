package kvstore.server;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

// KeyValue class to store key-value pairs
public class DataStorage {
  // TODO: 1.add lock; 2.Delete files; 3. add comments; 4.write summary; 5.update ReadMe
  private final Map<String, String> store;
  private final Lock readLock;
  private final Lock writeLock;

  // Constructor for KeyValue class
  public DataStorage() {
    store = new HashMap<>();
    ReentrantReadWriteLock rwl = new ReentrantReadWriteLock();
    readLock = rwl.readLock();
    writeLock = rwl.writeLock();
  }

  // Method to put a key-value pair into the store
  public void put(String key, String value) {
    writeLock.lock();
    try {
      store.put(key, value);
    } finally {
      writeLock.unlock();
    }
  }

  // Method to get the value associated with a key
  public String get(String key) {
    readLock.lock();
    try {
      return store.get(key);
    } finally {
      readLock.unlock();
    }
  }

  // Method to remove a key-value pair from the store
  public String delete(String key) {
    writeLock.lock();
    try {
      return store.remove(key);
    } finally {
      writeLock.unlock();
    }
  }
}
