import java.util.HashMap;
import java.util.concurrent.Semaphore;
import java.util.function.IntBinaryOperator;
import java.util.function.IntFunction;
import java.util.stream.IntStream;

/** KERNELLAND */
public class Kernel implements Stoppable, Runnable, Device {
  private final Semaphore semaphore;
  private final Thread thread;
  private final Scheduler scheduler;
  private final VFS vfs;
  private final int[] intArr;
  private final boolean[] freeSpace = new boolean[OS.getFreeSpaceSize()];

  public Kernel() {
    semaphore = new Semaphore(0);
    thread = new Thread(this, "kernelThread");
    scheduler = new Scheduler();
    vfs = new VFS();
    intArr = IntStream.generate(() -> -1).limit(10).toArray();
    IntStream.range(0, OS.getFreeSpaceSize()).forEach(i -> freeSpace[i] = true);
  }

  public boolean[] getFreeSpace() {
    return freeSpace;
  }

  public boolean getFromFreeSpace(int idx) {
    return getFreeSpace()[idx];
  }

  public void setOnFreeSpace(int idx, boolean val) {
    getFreeSpace()[idx] = val;
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
    getScheduler().switchProcess(getScheduler()::getRandFromWQ);
  }

  private void switchProcess() {
    OS.setRetValOnOS(null);
    getScheduler().switchProcess(getScheduler()::getRandFromWQ);
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
    getScheduler().switchProcess(getScheduler()::getRandFromWQ);
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
    getScheduler().switchProcess(getScheduler()::getRandFromWQ);
  }

  private int recordSize(HashMap<Integer, Integer> sizeIndices, int idx, int size) {
    sizeIndices.put(idx, size);
    return size;
  }

  private void allocateMemory() {
    int amountToAllocate = (int) OS.getParam(0);
    OutputHelper.debugPrint("Allocating " + amountToAllocate + " bytes of memory");
    // Search UserlandProcess.PHYSICAL_MEMORY via Kernel.freeMemory for a contiguous block of size
    // amountToAllocate. OS.retVal should then be set to the starting index of this block.
    int[] maxSize = {0};
    IntBinaryOperator saveSizeAndReturnAcc =
        (acc, ms) -> {
          maxSize[0] = ms;
          return acc;
        };
    IntFunction<Integer> zeroSizeAndReturnIdx =
        idx -> {
          maxSize[0] = 0;
          return idx;
        };
    int startingIdx =
        IntStream.range(0, OS.getFreeSpaceSize())
            .takeWhile(i -> maxSize[0] < amountToAllocate)
            .reduce(
                0,
                (acc, idx) ->
                    getFromFreeSpace(idx)
                        ? saveSizeAndReturnAcc.applyAsInt(acc, maxSize[0] + 1)
                        : zeroSizeAndReturnIdx.apply(idx));
    OS.setRetValOnOS(startingIdx);
    getScheduler().switchProcess(this::getCurrentlyRunningSafe);
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
        case ALLOCATE_MEMORY -> allocateMemory();
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
