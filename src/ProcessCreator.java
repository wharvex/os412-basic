import java.util.ArrayList;
import java.util.List;

/** USERLAND */
public class ProcessCreator extends UserlandProcess {
  private final List<Integer> pingPids;
  private final List<Integer> pongPids;
  private final List<Integer> memTestAPids;
  private final List<Integer> memTestBPids;

  public ProcessCreator() {
    super("0", "processCreator");
    pingPids = new ArrayList<>();
    pongPids = new ArrayList<>();
    memTestAPids = new ArrayList<>();
    memTestBPids = new ArrayList<>();
  }

  public List<Integer> getMemTestAPids() {
    return memTestAPids;
  }

  public List<Integer> getMemTestBPids() {
    return memTestBPids;
  }

  private void debugPrintOtherThreads() {
    Output.debugPrint("Bootloader is " + ThreadHelper.getThreadStateString("bootloaderThread"));
    Output.debugPrint("Main is " + ThreadHelper.getThreadStateString("mainThread"));
    Output.debugPrint("Kernel is " + ThreadHelper.getThreadStateString("kernelThread"));
    Output.debugPrint("Timer is " + ThreadHelper.getThreadStateString("timerThread"));
  }

  private void testMessages(int iterationCounter) {
    // Choose what to do based on iteration counter.
    switch (iterationCounter) {
      case 1:
        // Create Ping.
        Output.print("ProcessCreator creating Ping");
        OS.createProcess(
            this,
            new Ping(),
            Scheduler.PriorityType.INTERACTIVE,
            (ucs, pid) -> ((ProcessCreator) ucs).addToPingPids((int) pid));
        break;
      case 2:
        // Create Pong.
        Output.print("ProcessCreator creating Pong");
        OS.createProcess(
            this,
            new Pong(),
            Scheduler.PriorityType.INTERACTIVE,
            (ucs, pid) -> ((ProcessCreator) ucs).addToPongPids((int) pid));
        break;
      case 3:
        // Send Pong's pid to Ping.
        Output.print("ProcessCreator sending message to ping with content: Pong's pid");
        OS.sendMessage(
            this,
            new KernelMessage(getPingPids().getFirst(), 1, getPongPids().getFirst().toString()));
      case 4:
        // Send Ping's pid to Pong.
        Output.print("ProcessCreator sending message to pong with content: Ping's pid");
        OS.sendMessage(
            this,
            new KernelMessage(getPongPids().getFirst(), 1, getPingPids().getFirst().toString()));
      default:
        // Done.
        Output.print("ProcessCreator done testing messages.");
        debugPrintOtherThreads();
    }
  }

  private void testMemory(int iterationCounter) {
    // Choose what to do based on iteration counter.
    switch (iterationCounter) {
      case 1:
        // Create MemoryTestA.
        Output.print("ProcessCreator creating MemoryTestA");
        OS.createProcess(
            this,
            new MemoryTestA(),
            Scheduler.PriorityType.INTERACTIVE,
            (ucs, pid) -> ((ProcessCreator) ucs).addToMemTestAPids((int) pid));
        break;
      case 2:
        // Create MemoryTestA.
        Output.print("ProcessCreator creating MemoryTestB");
        OS.createProcess(
            this,
            new MemoryTestB(),
            Scheduler.PriorityType.INTERACTIVE,
            (ucs, pid) -> ((ProcessCreator) ucs).addToMemTestBPids((int) pid));
        break;
      default:
        // Done.
        Output.print("ProcessCreator done testing memory.");
        debugPrintOtherThreads();
    }
  }

  @Override
  void main() {
    int i = 0;
    while (true) {
      // Initial announcement.
      Output.print("Hello from ProcessCreator. Times printed: " + (++i));

      // Test messages.
      // testMessages(i);

      // Test memory.
      testMemory(i);

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

  public void addToMemTestAPids(int pid) {
    Output.debugPrint("Adding " + pid + " to memTestAPids");
    getMemTestAPids().add(pid);
    Output.debugPrint("Contents of memTestAPids: " + getMemTestAPids());
  }

  public void addToMemTestBPids(int pid) {
    Output.debugPrint("Adding " + pid + " to memTestBPids");
    getMemTestBPids().add(pid);
    Output.debugPrint("Contents of memTestBPids: " + getMemTestBPids());
  }
}
