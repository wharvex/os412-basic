import java.util.ArrayList;
import java.util.List;

/** USERLAND */
public class ProcessCreator extends UserlandProcess {
  private final List<Integer> pingPids;
  private final List<Integer> pongPids;
  private final List<Integer> pagingTestAPids;
  private final List<Integer> pagingTestBPids;
  private final List<Integer> virtMemTestAPids;
  private final List<Integer> virtMemTestBPids;

  public ProcessCreator() {
    super("0", "processCreator");
    pingPids = new ArrayList<>();
    pongPids = new ArrayList<>();
    pagingTestAPids = new ArrayList<>();
    pagingTestBPids = new ArrayList<>();
    virtMemTestAPids = new ArrayList<>();
    virtMemTestBPids = new ArrayList<>();
  }

  public List<Integer> getVirtMemTestAPids() {
    return virtMemTestAPids;
  }

  public List<Integer> getVirtMemTestBPids() {
    return virtMemTestBPids;
  }

  public List<Integer> getPagingTestAPids() {
    return pagingTestAPids;
  }

  public List<Integer> getPagingTestBPids() {
    return pagingTestBPids;
  }

  private void debugPrintOtherThreads() {
    OutputHelper.debugPrint(
        "Bootloader is " + ThreadHelper.getThreadStateString("bootloaderThread"));
    OutputHelper.debugPrint("Main is " + ThreadHelper.getThreadStateString("mainThread"));
    OutputHelper.debugPrint("Kernel is " + ThreadHelper.getThreadStateString("kernelThread"));
    OutputHelper.debugPrint("Timer is " + ThreadHelper.getThreadStateString("timerThread"));
  }

  private void testMessages(int iterationCounter) {
    // Choose what to do based on iteration counter.
    switch (iterationCounter) {
      case 1:
        // Create Ping.
        OutputHelper.print("ProcessCreator creating Ping");
        OS.createProcess(
            this,
            new Ping(),
            Scheduler.PriorityType.INTERACTIVE,
            (ucs, pid) -> ((ProcessCreator) ucs).addToPingPids((int) pid));
        break;
      case 2:
        // Create Pong.
        OutputHelper.print("ProcessCreator creating Pong");
        OS.createProcess(
            this,
            new Pong(),
            Scheduler.PriorityType.INTERACTIVE,
            (ucs, pid) -> ((ProcessCreator) ucs).addToPongPids((int) pid));
        break;
      case 3:
        // Send Pong's pid to Ping.
        OutputHelper.print("ProcessCreator sending message to ping with content: Pong's pid");
        OS.sendMessage(
            this,
            new KernelMessage(getPingPids().getFirst(), 1, getPongPids().getFirst().toString()));
      case 4:
        // Send Ping's pid to Pong.
        OutputHelper.print("ProcessCreator sending message to pong with content: Ping's pid");
        OS.sendMessage(
            this,
            new KernelMessage(getPongPids().getFirst(), 1, getPingPids().getFirst().toString()));
      default:
        // Done.
        OutputHelper.print("ProcessCreator done testing messages.");
        debugPrintOtherThreads();
    }
  }

  private void testPaging(int iterationCounter) {
    // Choose what to do based on iteration counter.
    switch (iterationCounter) {
      case 1:
        // Create PagingTestA.
        OutputHelper.print("ProcessCreator creating PagingTestA");
        OS.createProcess(
            this,
            new PagingTestA(),
            Scheduler.PriorityType.INTERACTIVE,
            (ucs, pid) -> ((ProcessCreator) ucs).addToPagingTestAPids((int) pid));
        break;
      case 2:
        // Create PagingTestB.
        OutputHelper.print("ProcessCreator creating PagingTestB");
        OS.createProcess(
            this,
            new PagingTestB(),
            Scheduler.PriorityType.INTERACTIVE,
            (ucs, pid) -> ((ProcessCreator) ucs).addToPagingTestBPids((int) pid));
        break;
      default:
        // Done.
        OutputHelper.print("ProcessCreator done testing paging.");
        OutputHelper.print(
            "ProcessCreator says: PagingTestA pid: " + getPagingTestAPids().getFirst());
        OutputHelper.print(
            "ProcessCreator says: PagingTestB pid: " + getPagingTestBPids().getFirst());
        debugPrintOtherThreads();
    }
  }

  private void testVirtualMemory(int iterationCounter) {
    // Choose what to do based on iteration counter.
    switch (iterationCounter) {
      case 1:
        // Create VirtMemTestA
        OutputHelper.print("ProcessCreator creating VirtMemTestA");
        OS.createProcess(
            this,
            new VirtMemTestA(),
            Scheduler.PriorityType.INTERACTIVE,
            (ucs, pid) -> ((ProcessCreator) ucs).addToVirtMemTestAPids((int) pid));
        break;
      case 2:
        // Create VirtMemTestB.
        OutputHelper.print("ProcessCreator creating VirtMemTestB");
        OS.createProcess(
            this,
            new VirtMemTestB(),
            Scheduler.PriorityType.INTERACTIVE,
            (ucs, pid) -> ((ProcessCreator) ucs).addToVirtMemTestBPids((int) pid));
        break;
      default:
        // Done.
        OutputHelper.print("ProcessCreator says: I'm done setting up virtual memory testing.");
        OutputHelper.print(
            "ProcessCreator says: PagingTestA pid: " + getVirtMemTestAPids().getFirst());
        OutputHelper.print(
            "ProcessCreator says: PagingTestB pid: " + getVirtMemTestBPids().getFirst());
        debugPrintOtherThreads();
    }
  }

  @Override
  void main() {
    int i = 0;
    while (true) {
      // Initial announcement.
      OutputHelper.print("Hello from ProcessCreator. Times printed: " + (++i));

      // Test messages.
      // testMessages(i);

      // Test paging.
      // testPaging(i);

      // Test virtual memory.
      testVirtualMemory(i);

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
    OutputHelper.debugPrint("Adding " + pid + " to pingPids");
    getPingPids().add(pid);
    OutputHelper.debugPrint("Contents of pingPids: " + getPingPids());
  }

  public void addToPongPids(int pid) {
    OutputHelper.debugPrint("Adding " + pid + " to pongPids");
    getPongPids().add(pid);
    OutputHelper.debugPrint("Contents of pongPids: " + getPongPids());
  }

  public void addToPagingTestAPids(int pid) {
    OutputHelper.debugPrint("Adding " + pid + " to pagingTestAPids");
    getPagingTestAPids().add(pid);
    OutputHelper.debugPrint("Contents of pagingTestAPids: " + getPagingTestAPids());
  }

  public void addToPagingTestBPids(int pid) {
    OutputHelper.debugPrint("Adding " + pid + " to pagingTestBPids");
    getPagingTestBPids().add(pid);
    OutputHelper.debugPrint("Contents of pagingTestBPids: " + getPagingTestBPids());
  }

  public void addToVirtMemTestAPids(int pid) {
    OutputHelper.debugPrint("Adding " + pid + " to virtMemTestAPids");
    getVirtMemTestAPids().add(pid);
    OutputHelper.debugPrint("Contents of virtMemTestAPids: " + getVirtMemTestAPids());
  }

  public void addToVirtMemTestBPids(int pid) {
    OutputHelper.debugPrint("Adding " + pid + " to virtMemTestBPids");
    getVirtMemTestBPids().add(pid);
    OutputHelper.debugPrint("Contents of virtMemTestBPids: " + getVirtMemTestBPids());
  }
}
