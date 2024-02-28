import java.time.Instant;
import java.util.Optional;

public class PCB {

  private static int nextPid = 0;
  private final UserlandProcess userlandProcess;
  private final int pid;
  private PriorityType priorityType;
  private Instant wakeupAfter;
  private int timeoutsCounter;

  public PCB(UserlandProcess up, PriorityType pt) {
    this.userlandProcess = up;
    this.priorityType = pt;
    // For each new PCB, it gets the current nextPid and increments nextPid for the next PCB.
    this.pid = PCB.nextPid++;
    this.timeoutsCounter = 0;
  }

  public int getTimeoutsCounter() {
    return timeoutsCounter;
  }

  public void setTimeoutsCounter(int timeoutsCounter) {
    this.timeoutsCounter = timeoutsCounter;
  }

  public String getThreadName() {
    return this.getUserlandProcess().getThread().getName();
  }

  public Thread.State getThreadState() {
    return this.getUserlandProcess().getThread().getState();
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
    this.getUserlandProcess().requestStop();
    this.incrementTimeoutsCounter();
  }

  /** Only called by timer thread. */
  private void incrementTimeoutsCounter() {
    this.setTimeoutsCounter(this.getTimeoutsCounter() + 1);
    if (this.getTimeoutsCounter() > 4) {
      this.demote();
    }
  }

  public boolean isDone() {
    return getUserlandProcess().isDone();
  }

  private void demote() {
    if (this.getPriorityType() == PriorityType.REALTIME) {
      this.setPriorityType(PriorityType.INTERACTIVE);
    } else if (this.getPriorityType() == PriorityType.INTERACTIVE) {
      this.setPriorityType(PriorityType.BACKGROUND);
    }
  }

  public void start() {
    this.getUserlandProcess().start();
  }

  public Optional<Instant> getWakeupAfter() {
    return Optional.ofNullable(wakeupAfter);
  }

  public void setWakeupAfter(Instant wakeupAfter) {
    this.wakeupAfter = wakeupAfter;
  }

  public void resetTimeoutsCounter() {
    this.setTimeoutsCounter(0);
  }
}
