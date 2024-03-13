import java.util.ArrayList;
import java.util.List;

/** USERLAND */
public class ProcessCreator extends UserlandProcess {
  private final List<Integer> pingPids;
  private final List<Integer> pongPids;

  public ProcessCreator() {
    super("0", "processCreator");
    pingPids = new ArrayList<>();
    pongPids = new ArrayList<>();
  }

  @Override
  void main() {
    int i = 0;
    while (true) {
      Output.print("Hello from ProcessCreator. Times printed: " + (++i));
      if (i == 1) {
        Output.print("Creating Ping");
        // Send in a lambda to use for the return (save) procedure?
        OS.createProcess(
            this,
            new Ping(),
            Scheduler.PriorityType.INTERACTIVE,
            (ucs, pid) -> ((ProcessCreator) ucs).getPingPids().add((int) pid));
      } else if (i == 2) {
        Output.print("Creating Pong");
        OS.createProcess(
            this,
            new Pong(),
            Scheduler.PriorityType.INTERACTIVE,
            (ucs, pid) -> ((ProcessCreator) ucs).getPongPids().add((int) pid));
      } else if (i == 3) {
        Output.print("Sending message");
        OS.sendMessage(
            this, new KernelMessage(getPingPids().get(0), 2, getPongPids().get(0).toString()));
      } else {
        Output.print("Done creating processes for now...");
        Output.print("All my ping pids: " + getPingPids().toString());
        Output.print("All my pong pids: " + getPongPids().toString());
        Output.print("All my messages: " + getMessages().toString());
      }
      Output.debugPrint("Bootloader is " + ThreadHelper.getThreadStateString("bootloaderThread"));
      Output.debugPrint("Main is " + ThreadHelper.getThreadStateString("mainThread"));
      Output.debugPrint("Kernel is " + ThreadHelper.getThreadStateString("kernelThread"));
      Output.debugPrint("Timer is " + ThreadHelper.getThreadStateString("timerThread"));
      cooperate();
      ThreadHelper.threadSleep(1000);
    }
  }

  public List<Integer> getPingPids() {
    return pingPids;
  }

  public List<Integer> getPongPids() {
    return pongPids;
  }
}
