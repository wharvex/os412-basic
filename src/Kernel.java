import java.util.concurrent.Semaphore;
import java.util.stream.IntStream;

/** KERNELLAND */
public class Kernel implements Stoppable, Runnable, Device {
  private final Semaphore semaphore;
  private final Thread thread;
  private final Scheduler scheduler;
  private final VFS vfs;
  private final int[] intArr;

  public Kernel() {
    semaphore = new Semaphore(0);
    thread = new Thread(this, "kernelThread");
    scheduler = new Scheduler();
    vfs = new VFS();
    intArr = IntStream.generate(() -> -1).limit(10).toArray();
  }

  // ------------------------------------- STOPPABLE OVERRIDES -------------------------------------

  @Override
  public Semaphore getSemaphore() {
    return semaphore;
  }

  @Override
  public Thread getThread() {
    return thread;
  }

  // --------------------------------------- HELPER METHODS ---------------------------------------

  public PCB getCurrentlyRunningSafe() {
    return getScheduler().getCurrentlyRunningSafe();
  }

  public Scheduler getScheduler() {
    return scheduler;
  }

  private PCB createPCB(UserlandProcess up, Scheduler.PriorityType pt) {
    // Create a new PCB.
    PCB pcb = new PCB(up, pt);
    OutputHelper.debugPrint(up.getThreadName() + " has now been created");

    // Add newly created PCB to the hashmap that finds a PCB by its pid.
    getScheduler().addToPcbByPidComplete(pcb, pcb.getPid());

    // Return the created PCB.
    return pcb;
  }

  // -------------------------------------- CALLTYPE METHODS --------------------------------------

  private void startupCreateProcess() {
    UserlandProcess processCreator = (UserlandProcess) OS.getParam(0);
    Scheduler.PriorityType pt = (Scheduler.PriorityType) OS.getParam(1);
    PCB pcb = createPCB(processCreator, pt);
    pcb.init();
    getScheduler().preSetCurrentlyRunning(pcb);
    OS.setRetValOnOS(pcb.getPid());
    getScheduler().startTimer();
  }

  private void createProcess() {
    UserlandProcess up = (UserlandProcess) OS.getParam(0);
    Scheduler.PriorityType pt = (Scheduler.PriorityType) OS.getParam(1);
    PCB pcb = createPCB(up, pt);
    pcb.init();
    getScheduler().addToWQ(pcb);
    OS.setRetValOnOS(pcb.getPid());
    getScheduler().switchProcess();
  }

  private void switchProcess() {
    OS.setRetValOnOS(null);
    getScheduler().switchProcess();
  }

  private void sendMessage() {
    // Get the message from Userland.
    KernelMessage userlandKM = (KernelMessage) OS.getParam(0);

    // Set sender pid on the message.
    userlandKM.setSenderPid(getScheduler().getPidByName(getCurrentlyRunningSafe().getThreadName()));

    // Add message to Scheduler's waiting messages queue.
    getScheduler().addToWaitingMessages(new KernelMessage(userlandKM));

    // Set context switch ret val.
    OS.setRetValOnOS(null);

    // Give another process a chance to run.
    getScheduler().switchProcess();
  }

  private void waitForMessage() {
    // Get message waiter PCB.
    var pcb =
        getScheduler()
            .getFromPcbByPidComplete(
                getScheduler().getPidByName(getCurrentlyRunningSafe().getThreadName()));
    OutputHelper.debugPrint("Message waiter thread: " + pcb.getThreadName());

    // Add message waiter PCB to the waitingRecipients queue.
    getScheduler().addToWaitingRecipients(pcb);

    // Set currentlyRunning to null so switchProcess does not add it to the WQ.
    getScheduler().preSetCurrentlyRunning(null);

    // Set context switch ret val.
    OS.setRetValOnOS(null);

    // Give another process a chance to run.
    getScheduler().switchProcess();
  }

  // ------------------------------------- FILESYSTEM METHODS -------------------------------------

  public VFS getVfs() {
    return vfs;
  }

  private int getEmptyPosition() {
    for (int i = 0; i < intArr.length; i++) {
      if (intArr[i] != -1) {
        return i;
      }
    }
    throw new RuntimeException("No empty spots left in Kernel's intArr.");
  }

  @Override
  public int open(String s) {
    // TODO: Map this to the VFS ID somehow.
    int userlandID = getCurrentlyRunningSafe().getPid();

    int emptyPosition = getEmptyPosition();
    int vfsID = vfs.open(s);
    if (vfsID == -1) {
      throw new RuntimeException("VFS ID was -1: fail.");
    }
    intArr[emptyPosition] = vfsID;
    return emptyPosition;
  }

  @Override
  public void close(int id) {}

  @Override
  public byte[] read(int id, int size) {
    return new byte[0];
  }

  @Override
  public void seek(int id, int to) {}

  @Override
  public int write(int id, byte[] data) {
    return 0;
  }

  // ----------------------------------------- RUN METHOD -----------------------------------------

  @Override
  public void run() {
    OutputHelper.debugPrint("Initting");
    while (true) {
      stop();

      // Announce the call type and context switcher.
      var ct = OS.getCallType();
      UnprivilegedContextSwitcher ucs = OS.getContextSwitcher();
      OutputHelper.debugPrint("Handling CallType " + ct + " from " + ucs.getThreadName());

      // Main run loop.
      switch (ct) {
        case STARTUP_CREATE_PROCESS -> startupCreateProcess();
        case CREATE_PROCESS -> createProcess();
        case SWITCH_PROCESS -> switchProcess();
        case SEND_MESSAGE -> sendMessage();
        case WAIT_FOR_MESSAGE -> waitForMessage();
      }

      // Start the new currentlyRunning.
      PCB newCurRun = getCurrentlyRunningSafe();
      OutputHelper.debugPrint("Start the new currentlyRunning");
      newCurRun.start();

      // Check if we should start the UCS.
      OutputHelper.debugPrint(
          """


                      If contextSwitcher is not the new curRun, start the contextSwitcher.
                      We ensure this because if it is the new curRun, it was already started""");
      if (ucs != newCurRun.getUserlandProcess()) {
        OutputHelper.debugPrint("OS.contextSwitcher is not the new curRun, starting the former...");
        ucs.start();
      } else {
        OutputHelper.debugPrint("OS.contextSwitcher is the new curRun, so it's already started...");
      }
    }
  }
}
