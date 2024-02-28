import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Semaphore;

public abstract class UserlandProcess implements Runnable, ContextSwitcher {
  private final String debugPid;
  private final Semaphore semaphore;
  private final Thread thread;
  private final List<Object> csRets;
  private boolean stopRequested;

  public UserlandProcess(String debugPid, String threadNameBase) {
    this.debugPid = debugPid;
    thread = new Thread(this, threadNameBase + "_" + debugPid);
    semaphore = new Semaphore(0);
    csRets = new ArrayList<>();
    stopRequested = false;
  }

  public void requestStop() {
    stopRequested = true;
  }

  public boolean isStopRequested() {
    return stopRequested;
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

  public boolean isDone() {
    return false;
  }
}
