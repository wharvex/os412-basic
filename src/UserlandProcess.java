import java.util.ArrayList;
import java.util.List;
import java.util.OptionalInt;
import java.util.concurrent.Semaphore;

/** USERLAND */
public abstract class UserlandProcess implements Runnable, UnprivilegedContextSwitcher {
  private static final int[][] TLB = new int[OS.getTlbSize()][OS.getTlbSize()];
  private static final byte[] PHYSICAL_MEMORY = new byte[OS.getPageSize() * OS.getPageSize()];
  private final String debugPid;
  private final Semaphore semaphore;
  private final Thread thread;
  private final List<Object> csRets;
  private final List<Object> swiProRets;
  private final List<KernelMessage> messages;
  private boolean shouldStopFromTimeout;
  private Boolean shouldStopAfterContextSwitch;

  public UserlandProcess(String debugPid, String threadNameBase) {
    this.debugPid = debugPid;
    thread = new Thread(this, threadNameBase + "Thread_" + debugPid);
    semaphore = new Semaphore(0);
    csRets = new ArrayList<>();
    swiProRets = new ArrayList<>();
    messages = new ArrayList<>();
    shouldStopFromTimeout = false;
  }

  private static synchronized int[][] getTlb() {
    return TLB;
  }

  private static synchronized int getFromTlb(int vOrP, int zOrF) {
    var ret = getTlb()[vOrP][zOrF];
    OutputHelper.debugPrint("TLB[" + vOrP + "][" + zOrF + "] is " + ret);
    return ret;
  }

  private static synchronized void setOnTlb(int vOrP, int zOrF, int val) {
    OutputHelper.debugPrint("Setting TLB[" + vOrP + "][" + zOrF + "] to " + val);
    getTlb()[vOrP][zOrF] = val;
  }

  public static synchronized int preGetFromTlb(int vOrP, int zOrF) {
    OutputHelper.debugPrint(
        OutputHelper.DebugOutputType.SYNC_BEFORE_ENTER, UserlandProcess.class.toString());
    var ret = getFromTlb(vOrP, zOrF);
    OutputHelper.debugPrint(
        OutputHelper.DebugOutputType.SYNC_LEAVE, UserlandProcess.class.toString());
    return ret;
  }

  public static synchronized void preSetOnTlb(int vOrP, int zOrF, int val) {
    OutputHelper.debugPrint(
        OutputHelper.DebugOutputType.SYNC_BEFORE_ENTER, UserlandProcess.class.toString());
    setOnTlb(vOrP, zOrF, val);
    OutputHelper.debugPrint(
        OutputHelper.DebugOutputType.SYNC_LEAVE, UserlandProcess.class.toString());
  }

  /** Only called by Timer thread via PCB. */
  public void requestStop() {
    preSetStopRequested(true);
  }

  public synchronized boolean isStopRequested() {
    OutputHelper.debugPrint(OutputHelper.DebugOutputType.SYNC_ENTER, this.toString());
    OutputHelper.debugPrint("stopRequested is " + shouldStopFromTimeout);
    return shouldStopFromTimeout;
  }

  public synchronized void setStopRequested(boolean isRequested) {
    OutputHelper.debugPrint(OutputHelper.DebugOutputType.SYNC_ENTER, this.toString());
    OutputHelper.debugPrint("Setting stopRequested to " + isRequested);
    shouldStopFromTimeout = isRequested;
  }

  public void preSetStopRequested(boolean isRequested) {
    OutputHelper.debugPrint(OutputHelper.DebugOutputType.SYNC_BEFORE_ENTER, this.toString());
    OutputHelper.debugPrint("About to enter setStopRequested");
    setStopRequested(isRequested);
    OutputHelper.debugPrint(OutputHelper.DebugOutputType.SYNC_LEAVE, this.toString());
  }

  public boolean preIsStopRequested() {
    OutputHelper.debugPrint(OutputHelper.DebugOutputType.SYNC_BEFORE_ENTER, this.toString());
    var ret = isStopRequested();
    OutputHelper.debugPrint(OutputHelper.DebugOutputType.SYNC_LEAVE, this.toString());
    return ret;
  }

  public void cooperate() {
    OutputHelper.debugPrint("Cooperating...");
    if (preIsStopRequested()) {
      OS.switchProcess(this);
    }
  }

  @Override
  public Semaphore getSemaphore() {
    return semaphore;
  }

  @Override
  public Thread getThread() {
    return thread;
  }

  @Override
  public void run() {
    OutputHelper.debugPrint(OutputHelper.DebugOutputType.INIT);
    stop();
    main();
  }

  abstract void main();

  public String getDebugPid() {
    return debugPid;
  }

  public Boolean getShouldStopAfterContextSwitch() {
    return shouldStopAfterContextSwitch;
  }

  public void setShouldStopAfterContextSwitch(boolean shouldStopAfterContextSwitch) {
    OutputHelper.debugPrint(
        "Setting shouldStopAfterContextSwitch to " + shouldStopAfterContextSwitch);
    this.shouldStopAfterContextSwitch = shouldStopAfterContextSwitch;
  }

  @Override
  public List<Object> getCsRets() {
    return csRets;
  }

  @Override
  public List<KernelMessage> getMessages() {
    return messages;
  }

  public void addAllToMessages(List<KernelMessage> kms) {
    getMessages().addAll(kms);
  }

  public void waitUntilStoppedByRequest() {
    while (preIsStopRequested()) {
      OutputHelper.debugPrint("Waiting for " + getThreadName() + " to stop from request");
      ThreadHelper.threadSleep(10);
    }
  }

  public byte read(int virtualAddress) {
    int virtualPageNumber = virtualAddress / OS.getPageSize();
    OutputHelper.debugPrint("virtualPageNumber: " + virtualPageNumber);
    int pageOffset = virtualAddress % OS.getPageSize();
    OutputHelper.debugPrint("offset: " + pageOffset);
    int physicalPageNumber = matchAndReturnPhys(virtualPageNumber).orElse(-1);
    OutputHelper.debugPrint("physicalPageNumber: " + physicalPageNumber);
    int physicalAddress = (physicalPageNumber * OS.getPageSize()) + pageOffset;
    OutputHelper.debugPrint("physicalAddress: " + physicalAddress);
    return 0;
  }

  public void write(int address, byte value) {}

  protected OptionalInt matchAndReturnPhys(int virtualPageNumber) {
    if (preGetFromTlb(0, 0) == virtualPageNumber) {
      return OptionalInt.of(preGetFromTlb(1, 0));
    } else if (preGetFromTlb(0, 1) == virtualPageNumber) {
      return OptionalInt.of(preGetFromTlb(1, 1));
    } else {
      return OptionalInt.empty();
    }
  }
}
