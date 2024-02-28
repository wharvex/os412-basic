public class ProcessCreator extends UserlandProcess {
  public ProcessCreator() {
    super("0", "processCreator");
  }

  @Override
  void main() {
    int i = 0;
    while (true) {
      System.out.println("Hello from ProcessCreator. Times printed: " + (++i));
      System.out.println(
          "Bootloader thread state: " + ThreadHelper.getThreadStateString("bootloaderThread"));
      ThreadHelper.threadSleep(1000);
    }
  }
}
