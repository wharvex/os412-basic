/** USERLAND */
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
          "View from ProcessCreator -- Bootloader thread is "
              + ThreadHelper.getThreadStateString("bootloaderThread"));
      Output.debugPrint(
          "View from ProcessCreator -- Bootloader isDone? " + OS.getContextSwitcher().isDone());
      Output.debugPrint(
          "View from ProcessCreator -- Main thread is "
              + ThreadHelper.getThreadStateString("mainThread"));
      Output.debugPrint(
          "View from ProcessCreator -- Kernel thread is "
              + ThreadHelper.getThreadStateString("kernelThread"));
      ThreadHelper.threadSleep(1000);
    }
  }
}
