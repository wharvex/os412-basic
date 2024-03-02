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

  public void startTimer() {
    this.timer.schedule(
        new TimerTask() {
          @Override
          public void run() {
            Output.debugPrint("Starting");
            preGetCurrentlyRunning()
                .ifPresentOrElse(
                    cr -> {
                      Output.debugPrint("Scheduler.currentlyRunning is " + cr);
                      Output.debugPrint(
                          "currentlyRunning.stopRequested is " + cr.isStopRequested());
                      cr.stop();
                      preSetCurrentlyRunning(null);
                    },
                    () -> {
                      Output.debugPrint("Scheduler.currentlyRunning is null");
                      Output.debugPrint(
                          "bootloaderThread is "
                              + ThreadHelper.getThreadStateString("bootloaderThread"));
                      Output.debugPrint(
                          "mainThread is " + ThreadHelper.getThreadStateString("mainThread"));
                      Output.debugPrint(
                          "kernelThread is " + ThreadHelper.getThreadStateString("kernelThread"));
                    });
          }
        },
        1000,
        1000);
  }
}
