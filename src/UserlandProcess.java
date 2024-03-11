import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Semaphore;

/** USERLAND */
public abstract class UserlandProcess implements Runnable, UnprivilegedContextSwitcher {
  private final String debugPid;
  private final Semaphore semaphore;
  private final Thread thread;
  private final List<Object> csRets;
  private boolean shouldStopFromTimeout;
  private Boolean shouldStopAfterContextSwitch;

  public UserlandProcess(String debugPid, String threadNameBase) {
    this.debugPid = debugPid;
    thread = new Thread(this, threadNameBase + "Thread_" + debugPid);
    semaphore = new Semaphore(0);
    csRets = new ArrayList<>();
    shouldStopFromTimeout = false;
  }

  /** Only called by Timer thread via PCB. */
  public void requestStop() {
    preSetStopRequested(true);
  }

  public synchronized boolean isStopRequested() {
    Output.debugPrint("stopRequested is " + shouldStopFromTimeout);
    return shouldStopFromTimeout;
  }

  public synchronized void setStopRequested(boolean isRequested) {
    Output.debugPrint("Setting stopRequested to " + isRequested);
    shouldStopFromTimeout = isRequested;
  }

  public void preSetStopRequested(boolean isRequested) {
    Output.debugPrint("About to enter setStopRequested");
    setStopRequested(isRequested);
  }

  public boolean preIsStopRequested() {
    Output.debugPrint("About to enter isStopRequested on " + getThreadName());
    return isStopRequested();
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
    this.shouldStopAfterContextSwitch = shouldStopAfterContextSwitch;
  }

  @Override
  public List<Object> getCsRets() {
    return csRets;
  }

  public void waitUntilStoppedByRequest() {
    while (preIsStopRequested()) {
      Output.debugPrint("Waiting for " + getThreadName() + " to stop from request");
      ThreadHelper.threadSleep(10);
    }
  }
}
