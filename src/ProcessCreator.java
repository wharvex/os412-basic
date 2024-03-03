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
      Output.debugPrint("Bootloader is " + ThreadHelper.getThreadStateString("bootloaderThread"));
      Output.debugPrint("Main is " + ThreadHelper.getThreadStateString("mainThread"));
      Output.debugPrint("Kernel is " + ThreadHelper.getThreadStateString("kernelThread"));
      Output.debugPrint("Timer is " + ThreadHelper.getThreadStateString("timerThread"));
      OS.createProcess(this, new SleepyProcess(), OS.PriorityType.INTERACTIVE);
      cooperate();
      ThreadHelper.threadSleep(1000);
    }
  }
}
