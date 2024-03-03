import java.util.*;

/**
 * KERNELLAND
 *
 * <p>Models an operating system process scheduler.
 */
public class Scheduler {

  // The priority-appropriate waiting queues.

  private final ArrayList<PCB> wqRealtime;
  private final ArrayList<PCB> wqInteractive;
  private final ArrayList<PCB> wqBackground;

  // The sleeping queue.

  private final ArrayList<PCB> sleepingQueue;

  // The hidden queue, where currentlyRunnings who shouldn't be given a chance to run again
  // immediately after a context switch go.

  private final ArrayList<PCB> hiddenQueue;

  // The timer, which simulates the hardware-based timer that interrupts the CPU and makes switching
  // between processes possible.

  private final Timer timer;

  // The currently running process.

  private PCB currentlyRunning;

  public Scheduler() {
    this.timer = new Timer("timerThread");
    this.wqRealtime = new ArrayList<>();
    this.wqInteractive = new ArrayList<>();
    this.wqBackground = new ArrayList<>();
    this.sleepingQueue = new ArrayList<>();
    this.hiddenQueue = new ArrayList<>();
  }

  public void switchProcess() {
    preGetCurrentlyRunning().ifPresent(this::wqAdd);
    preSetCurrentlyRunning(wqGetRand());
  }

  public synchronized Optional<PCB> getCurrentlyRunning() {
    Output.debugPrint(
        "Scheduler.currentlyRunning is "
            + (currentlyRunning != null ? currentlyRunning.getThreadName() : "null"));
    return Optional.ofNullable(currentlyRunning);
  }

  public synchronized void setCurrentlyRunning(PCB currentlyRunning) {
    Output.debugPrint(
        "Setting Scheduler.currentlyRunning to "
            + (currentlyRunning != null ? currentlyRunning.getThreadName() : "null"));
    this.currentlyRunning = currentlyRunning;
  }

  public Optional<PCB> preGetCurrentlyRunning() {
    Output.debugPrint("About to enter Scheduler.getCurrentlyRunning");
    return getCurrentlyRunning();
  }

  public void preSetCurrentlyRunning(PCB currentlyRunning) {
    Output.debugPrint("About to enter Scheduler.setCurrentlyRunning");
    setCurrentlyRunning(currentlyRunning);
  }

  public void wqAdd(PCB pcb) {
    Output.debugPrint("Adding " + pcb.getThreadName() + " to wq");
    wqBackground.add(pcb);
  }

  public PCB wqGetRand() {
    Random r = new Random();
    return wqBackground.get(r.nextInt(wqBackground.size()));
  }

  public void startTimer() {
    this.timer.schedule(
        new TimerTask() {
          @Override
          public void run() {
            Output.debugPrint("Starting");
            preGetCurrentlyRunning()
                .ifPresentOrElse(
                    PCB::stop,
                    () -> {
                      Output.debugPrint(
                          "Bootloader is " + ThreadHelper.getThreadStateString("bootloaderThread"));
                      Output.debugPrint(
                          "Main is " + ThreadHelper.getThreadStateString("mainThread"));
                      Output.debugPrint(
                          "Kernel is " + ThreadHelper.getThreadStateString("kernelThread"));
                    });
          }
        },
        1000,
        1000);
  }
}
