import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Semaphore;

/** USERLAND */
public abstract class UserlandProcess implements Runnable, UnprivilegedContextSwitcher {
  private final String debugPid;
  private final Semaphore semaphore;
  private final Thread thread;
  private final List<Object> csRets;
  private boolean stopRequested;

  public UserlandProcess(String debugPid, String threadNameBase) {
    this.debugPid = debugPid;
    thread = new Thread(this, threadNameBase + "Thread_" + debugPid);
    semaphore = new Semaphore(0);
    csRets = new ArrayList<>();
    stopRequested = false;
  }

  /** Only called by Timer thread via PCB. */
  public void requestStop() {
    setStopRequested(true);
    // Wait here for process to stop.
  }

  public synchronized boolean isStopRequested() {
    return stopRequested;
  }

  private synchronized void setStopRequested(boolean isRequested) {
    stopRequested = isRequested;
  }

  public void cooperate() {}

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
    Output.debugPrint(getThreadName() + " initting");
    stop();
    main();
  }

  abstract void main();

  public String getDebugPid() {
    return debugPid;
  }

  @Override
  public Object csRetsGet(int idx) {
    return csRets.get(idx);
  }

  @Override
  public void csRetsAdd(Object ret) {
    csRets.add(ret);
  }
}
