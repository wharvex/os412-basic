public class ProcessCreator extends UserlandProcess {
  public ProcessCreator() {
    super("0", "processCreator");
  }

  @Override
  void main() {
    int i = 0;
    while (true) {
      Output.print("Hello from ProcessCreator. Times printed: " + (++i));
      Output.debugPrint(
          "Bootloader thread state: " + ThreadHelper.getThreadStateString("bootloaderThread"));
      Output.debugPrint("Bootloader isDone: " + OS.getContextSwitcher().isDone());
      ThreadHelper.threadSleep(1000);
    }
  }
}
