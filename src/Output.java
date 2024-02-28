public class Output {
  public static void debugPrint(String printStr) {
    System.out.println("\nDEBUG OUTPUT: " + printStr + ".");
  }

  public static void print(String printStr) {
    System.out.println("\n*** OUTPUT: " + printStr + ". ***");
  }

  public static void errorPrint(String printStr) {
    System.out.println("\nERROR OUTPUT: " + printStr + ".");
  }

  public static String getErrorString(String strBase) {
    return "\nERROR: " + strBase + ".";
  }
}
