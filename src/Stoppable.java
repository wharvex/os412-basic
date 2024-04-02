import java.util.concurrent.Semaphore;

/** MAINLAND */
public interface Stoppable {

  default void init() {
    OutputHelper.debugPrint("Initting " + getThreadName());
    getThread().start();
  }

  default String getThreadName() {
    return getThread().getName();
  }

  default Thread.State getThreadState() {
    return getThread().getState();
  }

  default void stop() {
    try {
      if (Thread.currentThread() != getThread()) {
        throw new RuntimeException(
            OutputHelper.getErrorString("Parking space reserved for " + getThreadName()));
      }
    } catch (RuntimeException e) {
      OutputHelper.writeToFile(e.toString());
      throw e;
    }
    OutputHelper.debugPrint("Stopping");
    try {
      getSemaphore().acquire();
    } catch (InterruptedException e) {
      OutputHelper.errorPrint(getThreadName() + " interrupted while parked at its semaphore");
      Thread.currentThread().interrupt();
    }
    OutputHelper.debugPrint("Starting");
  }

  Semaphore getSemaphore();

  Thread getThread();

  default boolean isStopped() {
    return getSemaphore().hasQueuedThreads();
  }

  default void waitUntilStopped() {
    while (!isStopped()) {
      OutputHelper.debugPrint("Waiting for " + getThreadName() + " to stop");
      ThreadHelper.threadSleep(10);
    }
  }

  default void start() {
    // Wait until what we want to start is stopped.
    waitUntilStopped();

    // Ensure semaphore remains binary.
    if (getSemaphore().availablePermits() < 1) {
      OutputHelper.debugPrint("Starting " + getThreadName());
      getSemaphore().release();
    } else {
      OutputHelper.debugPrint(
          "Did not release "
              + getThreadName()
              + "'s semaphore because its available permits are not less than 1");
    }
  }
}
