package util;

public class Checksum {
  private static int calculateChecksum(String text) {
    int checksum = 0;
    for (char ch : text.toCharArray()) {
      checksum += ch;
    }
    return checksum;
  }

  public static String buildMsgWithChecksum(String text) {
    int checksum = calculateChecksum(text);
    return text + ";" + checksum + ";";
  }

  public static boolean verifyChecksum(String msg) {
    int checksumEnd = findChecksumEnd(msg);
    int checksumStart = findChecksumStart(msg);

    // add error handling
    int checksum1 = Integer.parseInt(msg.substring(checksumStart + 1, checksumEnd));

    String text = msg.substring(0, checksumStart);
    int checksum2 = calculateChecksum(text);
    return checksum1 == checksum2;
  }


  public static String dropChecksum(String msg) {
    int checksumStart = findChecksumStart(msg);
    return msg.substring(0, checksumStart);
  }

  public static int findChecksumStart(String msg) {
    int checksumEnd = findChecksumEnd(msg);
    int checksumStart = checksumEnd - 1;
    for (; checksumStart >= 0; checksumStart--) {
      if (msg.charAt(checksumStart) == ';') {
        break;
      }
    }
    return checksumStart;
  }

  private static int findChecksumEnd(String msg) {
    int len = msg.length();
    int checksumEnd = len - 1;
    for (; checksumEnd >= 0; checksumEnd--) {
      if (msg.charAt(checksumEnd) == ';') {
        break;
      }
    }
    return checksumEnd;
  }
}
