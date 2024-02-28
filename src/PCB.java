import java.time.Instant;
import java.util.Optional;

/** Process Control Block */
public class PCB {

  private static int nextPid = 0;
  private final UserlandProcess userlandProcess;
  private final int pid;
  private PriorityType priorityType;

  // The Instant before which we should not wake up this PCB if it is sleeping.

  private Instant wakeupAfter;

  // How many times the Timer has stopped this PCB.

  private int timeoutsCounter;

  public PCB(UserlandProcess up, PriorityType pt) {
    userlandProcess = up;
    priorityType = pt;

    // For a new PCB, we set pid to the current nextPid and then increment nextPid for the next PCB.
    pid = PCB.nextPid++;

    timeoutsCounter = 0;
  }

  public int getTimeoutsCounter() {
    return timeoutsCounter;
  }

  public void setTimeoutsCounter(int timeoutsCounter) {
    this.timeoutsCounter = timeoutsCounter;
  }

  public String getThreadName() {
    return getUserlandProcess().getThreadName();
  }

  public Thread.State getThreadState() {
    return getUserlandProcess().getThreadState();
  }

  /**
   * Priority type can be accessed by timer thread or kernel thread.
   *
   * @return
   */
  public synchronized PriorityType getPriorityType() {
    return priorityType;
  }

  /**
   * Priority type can be accessed by timer thread or kernel thread.
   *
   * @param priorityType
   */
  public synchronized void setPriorityType(PriorityType priorityType) {
    this.priorityType = priorityType;
  }

  public UserlandProcess getUserlandProcess() {
    return userlandProcess;
  }

  public int getPid() {
    return pid;
  }

  /** Only the Timer thread should use this method. */
  public void stop() {
    getUserlandProcess().requestStop();
    incrementTimeoutsCounter();
  }

  /** Only called by timer thread. */
  private void incrementTimeoutsCounter() {
    setTimeoutsCounter(getTimeoutsCounter() + 1);
    if (getTimeoutsCounter() > 4) {
      demote();
    }
  }

  public boolean isDone() {
    return getUserlandProcess().isDone();
  }

  private void demote() {
    if (getPriorityType() == PriorityType.REALTIME) {
      setPriorityType(PriorityType.INTERACTIVE);
    } else if (getPriorityType() == PriorityType.INTERACTIVE) {
      setPriorityType(PriorityType.BACKGROUND);
    }
  }

  public void start() {
    getUserlandProcess().start();
  }

  /**
   * TODO: What does it mean if this is null?
   *
   * @return
   */
  public Optional<Instant> getWakeupAfter() {
    return Optional.ofNullable(wakeupAfter);
  }

  public void setWakeupAfter(Instant wakeupAfter) {
    this.wakeupAfter = wakeupAfter;
  }

  public void resetTimeoutsCounter() {
    setTimeoutsCounter(0);
  }
}
