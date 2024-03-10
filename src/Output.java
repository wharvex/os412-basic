import static java.nio.file.StandardOpenOption.APPEND;
import static java.nio.file.StandardOpenOption.CREATE;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/** MAINLAND */
public class Output {
  private static StackWalker.StackFrame getFrame() {
    return StackWalker.getInstance()
        .walk(
            stream ->
                stream
                    .skip(2)
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("No such stack frame")));
  }

  public static void debugPrint(String baseStr) {
    var frame = getFrame();
    String printStr =
        Thread.currentThread().getName()
            + " in "
            + frame.getClassName()
            + "."
            + frame.getMethodName()
            + ": "
            + baseStr;
    //    System.out.println(printStr);
    writeToFile(printStr);
  }

  public static void debugPrint(DebugOutputType dot) {
    var frame = getFrame();
    String printStr =
        Thread.currentThread().getName()
            + " in "
            + frame.getClassName()
            + "."
            + frame.getMethodName()
            + ": "
            + dot.getPrintStr();
    //    System.out.println(printStr);
    writeToFile(printStr);
  }

  public static void debugPrint(DebugOutputType dot, String dotAddon) {
    var frame = getFrame();
    String printStr =
        Thread.currentThread().getName()
            + " in "
            + frame.getClassName()
            + "."
            + frame.getMethodName()
            + ": "
            + dot.getPrintStr()
            + " "
            + dotAddon;
    //    System.out.println(printStr);
    writeToFile(printStr);
  }

  public static void print(String baseStr) {
    String printStr = "*** OUTPUT: " + baseStr + " ***";
    System.out.println(printStr + "\n");
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
          s + "\n\n",
          CREATE,
          APPEND);

    } catch (IOException e) {
      System.out.println("Logging error -- exiting");
      System.exit(-1);
    }
  }

  public enum DebugOutputType {
    STOP("Stopping"),
    START("Starting"),
    INIT("Initting"),
    SYNC_BEFORE_ENTER("About to enter synchronized block. Synced on: "),
    SYNC_ENTER("Just entered synchronized block. Synced on: "),
    SYNC_LEAVE("Just left synchronized block. Synced on: ");
    private final String printStr; // price of each apple

    // Constructor
    DebugOutputType(String printStr) {
      this.printStr = printStr;
    }

    String getPrintStr() {
      return printStr;
    }
  }
}
