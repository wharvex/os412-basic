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
  private final List<KernelMessage> messages;
  private boolean shouldStopFromTimeout;
  private Boolean shouldStopAfterContextSwitch;

  public UserlandProcess(String debugPid, String threadNameBase) {
    this.debugPid = debugPid;
    thread = new Thread(this, threadNameBase + "Thread_" + debugPid);
    semaphore = new Semaphore(0);
    csRets = new ArrayList<>();
    messages = new ArrayList<>();
    shouldStopFromTimeout = false;
  }

  private static int[][] getTlb() {
    return TLB;
  }

  /** Only called by Timer thread via PCB. */
  public void requestStop() {
    preSetStopRequested(true);
  }

  public synchronized boolean isStopRequested() {
    Output.debugPrint(Output.DebugOutputType.SYNC_ENTER, this.toString());
    Output.debugPrint("stopRequested is " + shouldStopFromTimeout);
    return shouldStopFromTimeout;
  }

  public synchronized void setStopRequested(boolean isRequested) {
    Output.debugPrint(Output.DebugOutputType.SYNC_ENTER, this.toString());
    Output.debugPrint("Setting stopRequested to " + isRequested);
    shouldStopFromTimeout = isRequested;
  }

  public void preSetStopRequested(boolean isRequested) {
    Output.debugPrint(Output.DebugOutputType.SYNC_BEFORE_ENTER, this.toString());
    Output.debugPrint("About to enter setStopRequested");
    setStopRequested(isRequested);
    Output.debugPrint(Output.DebugOutputType.SYNC_LEAVE, this.toString());
  }

  public boolean preIsStopRequested() {
    Output.debugPrint(Output.DebugOutputType.SYNC_BEFORE_ENTER, this.toString());
    var ret = isStopRequested();
    Output.debugPrint(Output.DebugOutputType.SYNC_LEAVE, this.toString());
    return ret;
  }

  public void cooperate() {
    Output.debugPrint("Cooperating...");
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
    Output.debugPrint(Output.DebugOutputType.INIT);
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
    Output.debugPrint("Setting shouldStopAfterContextSwitch to " + shouldStopAfterContextSwitch);
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
      Output.debugPrint("Waiting for " + getThreadName() + " to stop from request");
      ThreadHelper.threadSleep(10);
    }
  }

  private int getFromTlb(int vOrP, int zOrF) {
    return getTlb()[vOrP][zOrF];
  }

  private void setOnTlb(int vOrP, int zOrF, int val) {
    getTlb()[vOrP][zOrF] = val;
  }

  public byte read(int address) {
    int virtualPageNumber = address / OS.getPageSize();
    int pageOffset = address % OS.getPageSize();
    return 0;
  }

  public void write(int address, byte value) {}

  private int getVirtZerothFromTlb() {
    return getFromTlb(0, 0);
  }

  private void setVirtZerothOnTlb(int vz) {
    setOnTlb(0, 0, vz);
  }

  private int getVirtFirstFromTlb() {
    return getFromTlb(0, 1);
  }

  private void setVirtFirstOnTlb(int vf) {
    setOnTlb(0, 1, vf);
  }

  private int getPhysZerothFromTlb() {
    return getFromTlb(1, 0);
  }

  private void setPhysZerothOnTlb(int pz) {
    setOnTlb(1, 0, pz);
  }

  private int getPhysFirstFromTlb() {
    return getFromTlb(1, 1);
  }

  private void setPhysFirstOnTlb(int pf) {
    setOnTlb(1, 1, pf);
  }

  private OptionalInt matchAndReturnPhys(int virtualPageNumber) {
    if (getVirtZerothFromTlb() == virtualPageNumber) {
      return OptionalInt.of(getPhysZerothFromTlb());
    } else if (getVirtFirstFromTlb() == virtualPageNumber) {
      return OptionalInt.of(getPhysFirstFromTlb());
    } else {
      return OptionalInt.empty();
    }
  }
}
