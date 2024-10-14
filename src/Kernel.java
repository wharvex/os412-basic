import java.util.Arrays;
import java.util.HashMap;
import java.util.Objects;
import java.util.concurrent.Semaphore;
import java.util.stream.IntStream;

/** KERNELLAND */
public class Kernel implements Stoppable, Runnable, Device {
  private final Semaphore semaphore;
  private final Thread thread;
  private final Scheduler scheduler;
  private final VFS vfs;
  private final boolean[] freeSpace = new boolean[OS.getFreeSpaceSize()];
  private final HashMap<Integer, int[]> pidToDevice;

  public Kernel() {
    semaphore = new Semaphore(0);
    thread = new Thread(this, "kernelThread");
    scheduler = new Scheduler();
    vfs = new VFS();
    IntStream.range(0, 3).forEach(i -> freeSpace[i] = false);
    IntStream.range(3, 6).forEach(i -> freeSpace[i] = true);
    IntStream.range(6, 9).forEach(i -> freeSpace[i] = false);
    IntStream.range(9, 14).forEach(i -> freeSpace[i] = true);
    IntStream.range(14, OS.getFreeSpaceSize()).forEach(i -> freeSpace[i] = false);

    Arrays.fill(UserlandProcess.PHYSICAL_MEMORY, (byte) -1);
    pidToDevice = new HashMap<>();
  }

  public HashMap<Integer, int[]> getPidToDevice() {
    return pidToDevice;
  }

  public int[] getDeviceFromPidToDevice(int pid) {
    if (!getPidToDevice().containsKey(pid)) {
      throw new RuntimeException("No such pid in here.");
    }
    return getPidToDevice().get(pid);
  }

  public int getFromDeviceInPidToDevice(int pid, int idx) {
    return getDeviceFromPidToDevice(pid)[idx];
  }

  public int addToDeviceInPidToDevice(int pid, int id) {
    int idx =
        MiscHelper.findNegativeIndex(
            this::getFromDeviceInPidToDevice, OS.DEVICE_CONTENTS_SIZE, pid);
    if (idx < 0) {
      return idx;
    }
    getDeviceFromPidToDevice(pid)[idx] = id;
    return idx;
  }

  public void addDeviceToPidToDevice(int pid) {
    if (Objects.nonNull(
        getPidToDevice().putIfAbsent(pid, MiscHelper.makeIntArr(OS.DEVICE_CONTENTS_SIZE)))) {
      throw new RuntimeException("Pid already in here.");
    }
  }

  public boolean[] getFreeSpace() {
    return freeSpace;
  }

  public boolean getFromFreeSpace(int idx) {
    return getFreeSpace().length > idx && getFreeSpace()[idx];
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

    // Add key for new pid (with an array full of -1 as its associated value) to the device-by-pid
    // map.
    addDeviceToPidToDevice(pcb.getPid());

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
    getScheduler().populateTlbRand();
  }

  private void createProcess() {
    UserlandProcess up = (UserlandProcess) OS.getParam(0);
    Scheduler.PriorityType pt = (Scheduler.PriorityType) OS.getParam(1);
    PCB pcb = createPCB(up, pt);
    pcb.init();
    getScheduler().addToWQ(pcb);
    OS.setRetValOnOS(pcb.getPid());
    getScheduler().switchProcess(getScheduler()::getRandomProcess);
  }

  private void switchProcess() {
    OS.setRetValOnOS(null);
    getScheduler().switchProcess(getScheduler()::getRandomProcess);
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
    getScheduler().switchProcess(getScheduler()::getRandomProcess);
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
    getScheduler().switchProcess(getScheduler()::getRandomProcess);
  }

  private void steal() {
    // 0. Get stealee pid.
    int stealeePid =
        getPidToDevice().keySet().stream()
            .filter(k -> k != getCurrentlyRunningSafe().getPid())
            .findFirst()
            .orElseThrow(() -> new RuntimeException("No other processes found to steal from."));

    // 1. Write out stealee's data to swapfile.
    int offset =
        getVfs()
            .getFromDeviceToIdsCollByClass(FakeFileSystem.class)
            .getDevice()
            .write(0, UserlandProcess.PHYSICAL_MEMORY);

    // 2. Record where we write it in the swapfile (offset).
    // 3. Update v2p of stealee.
    // 4. Reuse physical memory (may need to "load from disk").
    // 5. TLB.
  }

  /** New approach for Virtual Memory assignment: lazy allocation. */
  private void allocateMemory() {
    // Get Allocation Size In Bytes.
    int allocationSizeInBytes = (int) OS.getParam(0);
    OutputHelper.debugPrint(
        "Attempting to translate allocation size request "
            + allocationSizeInBytes
            + " from bytes to pages for "
            + getCurrentlyRunningSafe().getThreadName());

    // Enforce that ASIB is a multiple of pageSize.
    if (allocationSizeInBytes % OS.getPageSize() != 0) {
      throw new RuntimeException(
          OutputHelper.getErrorStringThrow(
              "Failed to translate due to invalid size value (not a multiple of OS.PAGE_SIZE)."));
    }

    // Get Allocation Size In Pages.
    int allocationSizeInPages = allocationSizeInBytes / OS.getPageSize();

    // Find a block run of length ASIP in curRun's device.
    int[] curRunDev = getDeviceFromPidToDevice(getCurrentlyRunningSafe().getPid());
    var runArr =
        IntStream.range(0, OS.DEVICE_CONTENTS_SIZE).filter(i -> curRunDev[i] < 0).toArray();

    // If curRun doesn't have enough space, steal.
    if (runArr.length < allocationSizeInPages) {
      steal();
    }

    // TODO: Use values in runArr as the indices in curRun.v2ps to fill with v2ps.
    var v2p = new VirtualToPhysicalMapping();

    // TODO: Do this only if we didn't steal.
    OS.setRetValOnOS(runArr.length > 0 ? runArr[0] : -1);

    getScheduler().switchProcess(getScheduler()::getRandomProcess);
  }

  private void freeMemory() {
    // TODO: Write this method.
    throw new RuntimeException("freeMemory not implemented yet");
  }

  private void getMapping() {
    // TODO: Write this method.
    throw new RuntimeException("getMapping not implemented yet");
  }

  // ------------------------------------- FILESYSTEM METHODS -------------------------------------

  public VFS getVfs() {
    return vfs;
  }

  @Override
  public int open(String s) {
    int pid = getCurrentlyRunningSafe().getPid();

    int vfsRet = getVfs().open((String) OS.getParam(0));
    if (vfsRet < 0) {
      throw new RuntimeException("VFS ID was -1: fail.");
    }

    int pidToDeviceRet = addToDeviceInPidToDevice(pid, vfsRet);
    if (pidToDeviceRet < 0) {
      throw new RuntimeException("pidToDevice ret was -1: fail.");
    }

    OS.setRetValOnOS(getCurrentlyRunningSafe().addToDeviceContentsIds(pidToDeviceRet));
    getScheduler().switchProcess(getScheduler()::getRandomProcess);
    return 0;
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
        case FREE_MEMORY -> freeMemory();
        case GET_MAPPING -> getMapping();
        case OPEN -> open("");
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
