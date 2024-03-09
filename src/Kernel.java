import java.util.concurrent.Semaphore;
import java.util.stream.IntStream;

/** KERNELLAND */
public class Kernel implements Stoppable, Runnable, Device {
  private final Semaphore semaphore;
  private final Thread thread;
  private final Scheduler scheduler;
  private final VFS vfs;
  private final int[] intArr;

  public Kernel() {
    semaphore = new Semaphore(0);
    thread = new Thread(this, "kernelThread");
    scheduler = new Scheduler();
    vfs = new VFS();
    intArr = IntStream.generate(() -> -1).limit(10).toArray();
  }

  @Override
  public Semaphore getSemaphore() {
    return semaphore;
  }

  @Override
  public Thread getThread() {
    return thread;
  }

  private PCB createPCB(UserlandProcess up, Scheduler.PriorityType pt) {
    PCB pcb = new PCB(up, pt);
    Output.debugPrint("Request to create " + up.getThreadName() + " is fulfilled");
    return pcb;
  }

  private void startupCreateProcess() {
    UserlandProcess processCreator = (UserlandProcess) OS.getParam(0);
    Scheduler.PriorityType pt = (Scheduler.PriorityType) OS.getParam(1);
    PCB pcb = createPCB(processCreator, pt);
    pcb.init();
    getScheduler().preSetCurrentlyRunning(pcb);
    OS.setRetValOnOS(pcb.getPid());
    getScheduler().startTimer();
  }

  private void createProcess() {
    UserlandProcess up = (UserlandProcess) OS.getParam(0);
    Scheduler.PriorityType pt = (Scheduler.PriorityType) OS.getParam(1);
    PCB pcb = createPCB(up, pt);
    pcb.init();
    getScheduler().wqAdd(pcb);
    getScheduler().switchProcess();
  }

  public PCB getCurrentlyRunningSafe() {
    return getScheduler().getCurrentlyRunningSafe();
  }

  public Scheduler getScheduler() {
    return scheduler;
  }

  public VFS getVfs() {
    return vfs;
  }

  private int getEmptyPosition() {
    for (int i = 0; i < intArr.length; i++) {
      if (intArr[i] != -1) {
        return i;
      }
    }
    throw new RuntimeException("No empty spots left in Kernel's intArr.");
  }

  @Override
  public int open(String s) {
    // TODO: Map this to the VFS ID somehow.
    int userlandID = getCurrentlyRunningSafe().getPid();

    int emptyPosition = getEmptyPosition();
    int vfsID = vfs.open(s);
    if (vfsID == -1) {
      throw new RuntimeException("VFS ID was -1: fail.");
    }
    intArr[emptyPosition] = vfsID;
    return emptyPosition;
  }

  @Override
  public void close(int id) {}

  @Override
  public byte[] read(int id, int size) {
    return new byte[0];
  }

  @Override
  public void seek(int id, int to) {}

  @Override
  public int write(int id, byte[] data) {
    return 0;
  }

  @Override
  public void run() {
    Output.debugPrint("Initting");
    while (true) {
      stop();
      var ct = OS.getCallType();
      Output.debugPrint("Handling CallType " + ct);
      switch (ct) {
        case OS.CallType.STARTUP_CREATE_PROCESS -> startupCreateProcess();
        case OS.CallType.CREATE_PROCESS -> createProcess();
        case OS.CallType.SWITCH_PROCESS -> getScheduler().switchProcess();
      }
      PCB newCurRun = getCurrentlyRunningSafe();
      Output.debugPrint("Start the new currentlyRunning");
      newCurRun.start();
      UnprivilegedContextSwitcher conSwi = OS.preGetContextSwitcher();
      Output.debugPrint("If contextSwitcher is not the new curRun, start the contextSwitcher");
      if (conSwi != newCurRun.getUserlandProcess()) {
        Output.debugPrint("OS.contextSwitcher is not the new curRun, starting the former...");
        conSwi.start();
      } else {
        Output.debugPrint("OS.contextSwitcher is the new curRun, so it's already started...");
      }
    }
  }
}
