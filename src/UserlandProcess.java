import java.util.concurrent.Semaphore;

public abstract class UserlandProcess implements Runnable, ISwitchable {
  private final String debugPid;
  private final Semaphore semaphore;
  private final Thread thread;

  public UserlandProcess(String debugPid, String threadNameBase) {
    this.debugPid = debugPid;
    thread = new Thread(this, threadNameBase + "_" + debugPid);
    semaphore = new Semaphore(0);
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
  public AbsContextSwitchRet getContextSwitchRet(int idx) {
    return null;
  }

  @Override
  public void setContextSwitchRet(AbsContextSwitchRet ret) {}

  @Override
  public void run() {}

  abstract void main();

  public String getDebugPid() {
    return debugPid;
  }
}
