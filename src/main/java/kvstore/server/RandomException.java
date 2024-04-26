package kvstore.server;

import java.util.Random;

public class RandomException {
  private static final double EXCEPTION_RATIO = 0.2;

  public static void randomlyThrowException() {
    int threshold = (int)(100 * EXCEPTION_RATIO);

    // Generate a random number between 0 and 99
    int randomNumber = new Random().nextInt(100);

    // Check if the random number falls within the threshold
    if (randomNumber < threshold) {
      // Throw an exception
      ServerLogger.error("!!!Randomly throwing exception!!!");
      throw new RuntimeException("Randomly generated exception");
    }
  }
}
