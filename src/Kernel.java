import java.util.concurrent.Semaphore;

/** KERNELLAND */
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

  private void startupCreateProcess() {
    UserlandProcess processCreator = (UserlandProcess) OS.getParam(0);
    processCreator.init();
    processCreator.start();
    OS.setRetValOnOS(1);
  }

  @Override
  public void run() {
    Output.debugPrint(getThreadName() + " initting now");
    while (true) {
      stop();
      switch (OS.getCallType()) {
        case OS.CallType.STARTUP_CREATE_PROCESS -> startupCreateProcess();
      }
      OS.startContextSwitcher();
    }
  }
}
