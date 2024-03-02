import java.util.ArrayList;
import java.util.Optional;
import java.util.Timer;
import java.util.TimerTask;

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

  public synchronized Optional<PCB> getCurrentlyRunning() {
    Output.debugPrint(
        "View from "
            + Thread.currentThread().getName()
            + " -- Scheduler.currentlyRunning is "
            + (currentlyRunning != null ? currentlyRunning.getThreadName() : "null"));
    return Optional.ofNullable(currentlyRunning);
  }

  public synchronized void setCurrentlyRunning(PCB currentlyRunning) {
    Output.debugPrint(
        Thread.currentThread().getName()
            + " setting Scheduler.currentlyRunning to "
            + (currentlyRunning != null ? currentlyRunning.getThreadName() : "null"));
    this.currentlyRunning = currentlyRunning;
  }

  public Optional<PCB> preGetCurrentlyRunning() {
    Output.debugPrint(
        Thread.currentThread().getName() + " about to enter Scheduler.getCurrentlyRunning");
    return getCurrentlyRunning();
  }

  public void preSetCurrentlyRunning(PCB currentlyRunning) {
    Output.debugPrint(
        Thread.currentThread().getName() + " about to enter Scheduler.setCurrentlyRunning");
    setCurrentlyRunning(currentlyRunning);
  }

  public void startTimer() {
    this.timer.schedule(
        new TimerTask() {
          @Override
          public void run() {
            Output.debugPrint(Thread.currentThread().getName() + " starting");
            preGetCurrentlyRunning()
                .ifPresentOrElse(
                    cr -> {
                      Output.debugPrint("View from Timer -- currentlyRunning is " + cr);
                      Output.debugPrint(
                          "View from Timer -- curRun stopRequested is " + cr.isStopRequested());
                      cr.stop();
                      preSetCurrentlyRunning(null);
                    },
                    () -> {
                      Output.debugPrint(
                          Thread.currentThread().getName() + " did not find currentlyRunning");
                      Output.debugPrint(
                          "View from Timer -- Bootloader thread is "
                              + ThreadHelper.getThreadStateString("bootloaderThread"));
                      Output.debugPrint(
                          "View from Timer -- Main thread is "
                              + ThreadHelper.getThreadStateString("mainThread"));
                      Output.debugPrint(
                          "View from Timer -- Kernel thread is "
                              + ThreadHelper.getThreadStateString("kernelThread"));
                    });
          }
        },
        1000,
        1000);
  }
}
