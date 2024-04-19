import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;

/**
 * KERNELLAND
 *
 * <p>Process Control Block
 */
public class PCB {

  private static int nextPid = 0;
  private final int[] deviceContentsIds;
  private final VirtualToPhysicalMapping[] v2ps;
  private final UserlandProcess userlandProcess;
  private final int pid;
  private final int[] memoryMap = new int[OS.getMemoryMapSize()];
  private List<KernelMessage> messages;
  private Scheduler.PriorityType priorityType;
  // The Instant before which we should not wake up this PCB if it is sleeping.
  private Instant wakeupAfter;
  // How many times the Timer has stopped this PCB.
  private int timeoutsCounter;

  public PCB(UserlandProcess up, Scheduler.PriorityType pt) {
    userlandProcess = up;
    priorityType = pt;

    // For a new PCB, we set pid to the current nextPid and then increment nextPid for the next PCB.
    pid = PCB.nextPid++;

    timeoutsCounter = 0;
    messages = new ArrayList<>();
    IntStream.range(0, OS.getMemoryMapSize()).forEach(i -> getMemoryMap()[i] = -1);
    deviceContentsIds = MiscHelper.makeIntArr(OS.DEVICE_CONTENTS_SIZE);
    v2ps = new VirtualToPhysicalMapping[OS.DEVICE_CONTENTS_SIZE];
  }

  public VirtualToPhysicalMapping[] getV2ps() {
    return v2ps;
  }

  public VirtualToPhysicalMapping getFromV2ps(int idx) {
    return getV2ps()[idx];
  }

  public void addToV2ps(int idx, VirtualToPhysicalMapping v2p) {
    getV2ps()[idx] = v2p;
  }

  public int[] getDeviceContentsIds() {
    return deviceContentsIds;
  }

  public int getFromDeviceContentsIds(int idx) {
    return getDeviceContentsIds()[idx];
  }

  public int addToDeviceContentsIds(int id) {
    int idx = MiscHelper.findNegativeIndex(this::getFromDeviceContentsIds, OS.DEVICE_CONTENTS_SIZE);
    if (idx < 0) {
      return idx;
    }
    getDeviceContentsIds()[idx] = id;
    return idx;
  }

  public int[] getMemoryMap() {
    return memoryMap;
  }

  public int getTimeoutsCounter() {
    return timeoutsCounter;
  }

  public void setTimeoutsCounter(int timeoutsCounter) {
    this.timeoutsCounter = timeoutsCounter;
  }

  public PCB addAllToMessagesAndReturnThis(List<KernelMessage> kms) {
    getMessages().addAll(kms);
    return this;
  }

  public String getThreadName() {
    return getUserlandProcess().getThreadName();
  }

  public Thread.State getThreadState() {
    return getUserlandProcess().getThreadState();
  }

  /**
   * Priority type can be accessed by timer thread or kernel thread.
   *
   * @return
   */
  public synchronized Scheduler.PriorityType getPriorityType() {
    return priorityType;
  }

  /**
   * Priority type can be accessed by timer thread or kernel thread.
   *
   * @param priorityType
   */
  public synchronized void setPriorityType(Scheduler.PriorityType priorityType) {
    this.priorityType = priorityType;
  }

  public UserlandProcess getUserlandProcess() {
    return userlandProcess;
  }

  public int getPid() {
    return pid;
  }

  /** Only the Timer thread should use this method. */
  public void stop() {
    getUserlandProcess().requestStop();
    getUserlandProcess().waitUntilStoppedByRequest();
    OutputHelper.debugPrint("Successfully stopped " + getThreadName());
    incrementTimeoutsCounter();
  }

  /** Only called by timer thread. */
  private void incrementTimeoutsCounter() {
    setTimeoutsCounter(getTimeoutsCounter() + 1);
    if (getTimeoutsCounter() > 4) {
      demote();
    }
  }

  public boolean isDone() {
    return getUserlandProcess().isDone();
  }

  private void demote() {
    if (getPriorityType() == Scheduler.PriorityType.REALTIME) {
      setPriorityType(Scheduler.PriorityType.INTERACTIVE);
    } else if (getPriorityType() == Scheduler.PriorityType.INTERACTIVE) {
      setPriorityType(Scheduler.PriorityType.BACKGROUND);
    }
  }

  public void init() {
    getUserlandProcess().init();
  }

  public void start() {
    getUserlandProcess().start();
  }

  /**
   * TODO: What does it mean if this is null?
   *
   * @return
   */
  public Optional<Instant> getWakeupAfter() {
    return Optional.ofNullable(wakeupAfter);
  }

  public void setWakeupAfter(Instant wakeupAfter) {
    this.wakeupAfter = wakeupAfter;
  }

  public void resetTimeoutsCounter() {
    setTimeoutsCounter(0);
  }

  public List<KernelMessage> getMessages() {
    return messages;
  }

  public void setMessages(List<KernelMessage> messages) {
    this.messages = messages;
  }
}
