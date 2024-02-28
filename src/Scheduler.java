import java.util.ArrayList;
import java.util.Optional;
import java.util.Timer;
import java.util.TimerTask;

/** Models an operating system process scheduler. */
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

  public Optional<PCB> getCurrentlyRunning() {
    return Optional.ofNullable(currentlyRunning);
  }

  public void setCurrentlyRunning(PCB currentlyRunning) {
    this.currentlyRunning = currentlyRunning;
  }

  private void startTimer() {
    this.timer.schedule(
        new TimerTask() {
          @Override
          public void run() {
            getCurrentlyRunning().ifPresent(PCB::stop);
          }
        },
        1000,
        1000);
  }
}
