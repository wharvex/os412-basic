import java.util.Scanner;

/** MAINLAND */
public class Main {
  public static void main(String[] args) {
    Thread.currentThread().setName("mainThread");
    Output.debugPrint("BEGIN EXECUTION TRACE");
    var bl = new Bootloader();
    bl.init();
    Scanner sc = new Scanner(System.in);
    String userInput = "not x";
    while (!userInput.equals("x")) {
      userInput = sc.nextLine();
      Output.debugPrint("USER INPUT: " + userInput);
      if (userInput.equals("x")) {
        Output.debugPrint("END EXECUTION TRACE");
      }
    }
    System.exit(0);
  }
}
