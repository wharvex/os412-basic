import java.util.Scanner;

/** MAINLAND */
public class Main {
  public static void main(String[] args) {
    Thread.currentThread().setName("mainThread");
    Output.debugPrint("BEGIN EXECUTION TRACE");
    var bl = new Bootloader();
    bl.init();
    Scanner sc = new Scanner(System.in);
    while (!sc.nextLine().equals("x")) {}
    Output.debugPrint("END EXECUTION TRACE");
    System.exit(0);
  }
}
