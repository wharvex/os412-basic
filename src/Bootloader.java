import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Semaphore;

/** MAINLAND */
public class Bootloader implements UnprivilegedContextSwitcher, Runnable {
  private final Semaphore semaphore;
  private final Thread thread;
  private final List<Object> csRets;

  public Bootloader() {
    semaphore = new Semaphore(0);
    thread = new Thread(this, "bootloaderThread");
    csRets = new ArrayList<>();
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
  public Object csRetsGet(int idx) {
    return csRets.get(idx);
  }

  @Override
  public void csRetsAdd(Object ret) {
    csRets.add(ret);
  }

  @Override
  public boolean isStopped() {
    return UnprivilegedContextSwitcher.super.isStopped();
  }

  @Override
  public void run() {
    Output.debugPrint(getThreadName() + " initting");
    setContextSwitchRet(OS.startup(this));
  }
}
