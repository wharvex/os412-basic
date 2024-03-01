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
          "View from ProcessCreator -- Main thread is "
              + ThreadHelper.getThreadStateString("mainThread"));
      Output.debugPrint(
          "View from ProcessCreator -- Kernel thread is "
              + ThreadHelper.getThreadStateString("kernelThread"));
      Output.debugPrint(
          "View from ProcessCreator -- Timer thread is "
              + ThreadHelper.getThreadStateString("timerThread"));
      Output.debugPrint("View from ProcessCreator -- stopRequested is " + preIsStopRequested());
      cooperate();
      ThreadHelper.threadSleep(1000);
    }
  }
}
