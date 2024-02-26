import java.util.concurrent.Semaphore;

public class Kernel implements IStoppable, Runnable {
  private final Semaphore semaphore;
  private final Thread thread;

  public Kernel() {
    semaphore = new Semaphore(0);
    thread = new Thread(this, "kernelThread");
  }

  @Override
  public Semaphore getSemaphore() {
    return null;
  }

  @Override
  public Thread getThread() {
    return null;
  }

  @Override
  public void run() {}
}
