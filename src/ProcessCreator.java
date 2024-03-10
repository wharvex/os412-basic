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
      if (i == 1) {
        Output.print("Creating Ping");
        OS.createProcess(this, new Ping(), Scheduler.PriorityType.INTERACTIVE);
      } else if (i == 2) {
        Output.print("Creating Pong");
        OS.createProcess(this, new Pong(), Scheduler.PriorityType.INTERACTIVE);
      } else {
        Output.print("Done creating processes for now...");
      }
      Output.debugPrint("Bootloader is " + ThreadHelper.getThreadStateString("bootloaderThread"));
      Output.debugPrint("Main is " + ThreadHelper.getThreadStateString("mainThread"));
      Output.debugPrint("Kernel is " + ThreadHelper.getThreadStateString("kernelThread"));
      Output.debugPrint("Timer is " + ThreadHelper.getThreadStateString("timerThread"));
      cooperate();
      ThreadHelper.threadSleep(1000);
    }
  }
}
