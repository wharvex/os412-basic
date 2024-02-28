public class Main {
  public static void main(String[] args) {
    Thread.currentThread().setName("mainThread");
    var bl = new Bootloader();
    bl.init();
  }
}
