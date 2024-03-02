import java.util.concurrent.Semaphore;

/** MAINLAND */
public interface Stoppable {

  default void init() {
    Output.debugPrint("Initting " + getThreadName());
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
            Output.getErrorString("Parking space reserved for " + getThreadName()));
      }
    } catch (RuntimeException e) {
      Output.writeToFile(e.toString());
      throw e;
    }
    Output.debugPrint("Stopping");
    try {
      getSemaphore().acquire();
    } catch (InterruptedException e) {
      Output.errorPrint(getThreadName() + " interrupted while parked at its semaphore");
      Thread.currentThread().interrupt();
    }
    Output.debugPrint("Starting");
  }

  Semaphore getSemaphore();

  Thread getThread();

  default boolean isStopped() {
    return getSemaphore().hasQueuedThreads();
  }

  default void waitUntilStopped() {
    while (!isStopped()) {
      Output.debugPrint("Waiting for " + getThreadName() + " to stop");
      ThreadHelper.threadSleep(10);
    }
  }

  default void start() {
    // Wait until what we want to start is stopped.
    waitUntilStopped();

    // Ensure semaphore remains binary.
    if (getSemaphore().availablePermits() < 1) {
      Output.debugPrint("Starting " + getThreadName());
      getSemaphore().release();
    } else {
      Output.debugPrint(
          "Did not release "
              + getThreadName()
              + "'s semaphore because its available permits are not less than 1");
    }
  }
}
