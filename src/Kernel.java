import java.util.concurrent.Semaphore;

public class Kernel implements Stoppable, Runnable {
  private final Semaphore semaphore;
  private final Thread thread;

  public Kernel() {
    semaphore = new Semaphore(0);
    thread = new Thread(this, "kernelThread");
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
    stop();
    UserlandProcess processCreator = (UserlandProcess) OS.getParam(0);
    processCreator.init();
    processCreator.start();
    OS.setRetValOnOS(1);
    OS.stopKernel();
  }
}
