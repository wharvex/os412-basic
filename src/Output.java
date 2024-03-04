import static java.nio.file.StandardOpenOption.APPEND;
import static java.nio.file.StandardOpenOption.CREATE;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/** MAINLAND */
public class Output {
  public static void debugPrint(String baseStr) {
    String printStr = "\nDEBUG OUTPUT from " + Thread.currentThread().getName() + ": " + baseStr;
    //    System.out.println(printStr);
    writeToFile(printStr);
  }

  public static void print(String baseStr) {
    String printStr = "\n*** OUTPUT: " + baseStr + " ***";
    System.out.println(printStr);
    writeToFile(printStr);
  }

  public static void errorPrint(String baseStr) {
    String printStr = "\nERROR OUTPUT: " + baseStr;
    System.out.println(printStr);
    writeToFile(printStr);
  }

  public static String getErrorString(String baseStr) {
    return "\nERROR: " + baseStr;
  }

  /**
   * Adapted from: https://stackoverflow.com/a/1625263/16458003
   *
   * @param s
   */
  public static void writeToFile(final String s) {
    try {
      Files.writeString(
          Path.of(System.getProperty("java.io.tmpdir"), "os412_output.txt"),
          s + System.lineSeparator(),
          CREATE,
          APPEND);

    } catch (IOException e) {
      System.out.println("Logging error -- exiting");
      System.exit(-1);
    }
  }
}
