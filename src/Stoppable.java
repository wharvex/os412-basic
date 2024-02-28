import java.util.concurrent.Semaphore;

public interface Stoppable {

  default void init() {
    getThread().start();
  }

  default String getThreadName() {
    return getThread().getName();
  }

  default void stop() {
    if (Thread.currentThread() != getThread()) {
      throw new RuntimeException("Parking space reserved for " + getThreadName() + ".");
    }
    try {
      getSemaphore().acquire();
    } catch (InterruptedException e) {
      System.out.println(getThreadName() + " interrupted while parked at its semaphore.");
      Thread.currentThread().interrupt();
    }
  }

  Semaphore getSemaphore();

  Thread getThread();

  default boolean isStopped() {
    return getSemaphore().hasQueuedThreads();
  }

  default void start() {
    // Wait until what we want to start is stopped.
    while (!isStopped()) {
      ThreadHelper.threadSleep(10);
    }

    // Ensure semaphore remains binary.
    if (getSemaphore().availablePermits() < 1) {
      System.out.println("Releasing semaphore at which " + getThreadName() + " is parked.");
      getSemaphore().release();
    } else {
      System.out.println("Semaphore for " + getThreadName() + " not released.");
    }
  }
}
