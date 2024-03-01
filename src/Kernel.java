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

  private void startupCreateProcess() {
    UserlandProcess processCreator = (UserlandProcess) OS.getParam(7);
    OS.PriorityType pt = (OS.PriorityType) OS.getParam(1);
    PCB pcb = new PCB(processCreator, pt);
    pcb.init();
    pcb.start();
    OS.setRetValOnOS(pcb.getPid());
    getScheduler().preSetCurrentlyRunning(pcb);
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
    int userlandID = getScheduler().preGetCurrentlyRunning().orElseThrow().getPid();

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
}
