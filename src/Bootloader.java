import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Semaphore;

public class Bootloader implements ISwitchable, Runnable {
  private final Semaphore semaphore;
  private final Thread thread;
  private final List<AbsContextSwitchRet> csRets;

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
  public AbsContextSwitchRet getContextSwitchRet(int idx) {
    return csRets.get(idx);
  }

  @Override
  public void setContextSwitchRet(AbsContextSwitchRet ret) {
    Objects.requireNonNull(
        ret, "Null Context Switch Returns not allowed in the Bootloader's CSR list.");
    csRets.add(ret);
  }

  @Override
  public void run() {
    OS.startup(this, new ProcessCreator(), new SleepyProcess(), new UnsleepyProcess());
  }
}
