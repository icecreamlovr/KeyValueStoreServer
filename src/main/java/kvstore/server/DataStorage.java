package kvstore.server;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

// KeyValue class to store key-value pairs
public class DataStorage {
  private final Map<String, String> store;
  private final ConcurrentHashMap<String, ReentrantReadWriteLock> locks = new ConcurrentHashMap<>();

  // Constructor for KeyValue class
  public DataStorage() {
    store = new HashMap<>();
  }

  /**
   * Non-blocking call to acquire a write-lock associated with a particular key. If lock is available, acquire and
   * return true. If lock is not available, return false without waiting.
   */
  public boolean tryWriteLock(String key) {
    ReentrantReadWriteLock lock = locks.computeIfAbsent(key, k -> new ReentrantReadWriteLock());
    boolean locked = lock.writeLock().tryLock();
    if (locked) {
      ServerLogger.info("LOCKED (write) " + key);
    } else {
      ServerLogger.error("FAILED TO LOCK (write) " + key);
    }
    return locked;
  }

  // Unlock the write-lock of the given key
  public void writeUnlock(String key) {
    ReentrantReadWriteLock lock = locks.get(key);
    if (lock != null) {
      ServerLogger.info("UNLOCKING (write) " + key);
      lock.writeLock().unlock();
    } else {
      ServerLogger.error("FAILED TO UNLOCK (write) " + key);
    }
  }

  /**
   * Blocking call to acquire a read-lock associated with a particular key. This is used when processing GET. When lock
   * is not available, wait till it acquires the read lock.
   */
  public void blockingReadLock(String key) {
    ReentrantReadWriteLock lock = locks.computeIfAbsent(key, k -> new ReentrantReadWriteLock());
    lock.readLock().lock();
    ServerLogger.info("LOCKED (read) " + key);
  }

  // Unlock the write-lock of the given key
  public void readUnlock(String key) {
    ReentrantReadWriteLock lock = locks.get(key);
    if (lock != null) {
      ServerLogger.info("UNLOCKING (read) " + key);
      lock.readLock().unlock();
    } else {
      ServerLogger.error("FAILED TO UNLOCK (read) " + key);
    }
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
