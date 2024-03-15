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
      // Initial announcement.
      Output.print("Hello from ProcessCreator. Times printed: " + (++i));

      // Choose what to do based on iteration counter.
      if (i == 1) {
        // Create Ping.
        Output.print("ProcessCreator creating Ping");
        OS.createProcess(
            this,
            new Ping(),
            Scheduler.PriorityType.INTERACTIVE,
            (ucs, pid) -> ((ProcessCreator) ucs).addToPingPids((int) pid));
      } else if (i == 2) {
        // Create Pong.
        Output.print("ProcessCreator creating Pong");
        OS.createProcess(
            this,
            new Pong(),
            Scheduler.PriorityType.INTERACTIVE,
            (ucs, pid) -> ((ProcessCreator) ucs).addToPongPids((int) pid));
      } else if (i == 3) {
        // Send a message.
        Output.print("ProcessCreator sending message to ping with content: baba/booie");
        OS.sendMessage(this, new KernelMessage(getPingPids().get(0), 2, "baba/booie"));
      } else {
        // Done.
        Output.print("ProcessCreator done creating processes for now...");
        Output.print("ProcessCreator says: All my ping pids: " + getPingPids().toString());
        Output.print("ProcessCreator says: All my pong pids: " + getPongPids().toString());
        Output.print("ProcessCreator says: All my messages: " + getMessages().toString());
      }

      // Debug print PC's view of other threads.
      Output.debugPrint("Bootloader is " + ThreadHelper.getThreadStateString("bootloaderThread"));
      Output.debugPrint("Main is " + ThreadHelper.getThreadStateString("mainThread"));
      Output.debugPrint("Kernel is " + ThreadHelper.getThreadStateString("kernelThread"));
      Output.debugPrint("Timer is " + ThreadHelper.getThreadStateString("timerThread"));

      // Sleep and cooperate.
      ThreadHelper.threadSleep(1000);
      cooperate();
    }
  }

  public List<Integer> getPingPids() {
    return pingPids;
  }

  public List<Integer> getPongPids() {
    return pongPids;
  }

  public void addToPingPids(int pid) {
    Output.debugPrint("Adding " + pid + " to pingPids");
    getPingPids().add(pid);
    Output.debugPrint("Contents of pingPids: " + getPingPids());
  }

  public void addToPongPids(int pid) {
    Output.debugPrint("Adding " + pid + " to pongPids");
    getPongPids().add(pid);
    Output.debugPrint("Contents of pongPids: " + getPongPids());
  }
}
