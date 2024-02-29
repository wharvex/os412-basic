import java.util.concurrent.Semaphore;

/** KERNELLAND */
public class Kernel implements Stoppable, Runnable {
  private final Semaphore semaphore;
  private final Thread thread;
  private final Scheduler scheduler;

  public Kernel() {
    semaphore = new Semaphore(0);
    thread = new Thread(this, "kernelThread");
    scheduler = new Scheduler();
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
    OS.PriorityType pt = (OS.PriorityType) OS.getParam(1);
    PCB pcb = new PCB(processCreator, pt);
    pcb.init();
    pcb.start();
    OS.setRetValOnOS(pcb.getPid());
    getScheduler().setCurrentlyRunning(pcb);
    getScheduler().startTimer();
  }

  @Override
  public void run() {
    Output.debugPrint(getThreadName() + " initting");
    while (true) {
      stop();
      switch (OS.getCallType()) {
        case OS.CallType.STARTUP_CREATE_PROCESS -> startupCreateProcess();
      }
      OS.startContextSwitcher();
    }
  }

  public Scheduler getScheduler() {
    return scheduler;
  }
}
