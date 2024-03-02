import java.util.concurrent.Semaphore;

/** MAINLAND */
public interface Stoppable {

  default void init() {
    Output.debugPrint(Thread.currentThread().getName() + " initting " + getThreadName());
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
    try {
      Output.debugPrint(getThreadName() + " stopping");
      getSemaphore().acquire();
      Output.debugPrint(getThreadName() + " starting");
    } catch (InterruptedException e) {
      Output.errorPrint(getThreadName() + " interrupted while parked at its semaphore");
      Thread.currentThread().interrupt();
    }
  }

  Semaphore getSemaphore();

  Thread getThread();

  default boolean preIsStopped() {
    Output.debugPrint(Thread.currentThread().getName() + " about to enter isStopped");
    return isStopped();
  }

  default boolean isStopped() {
    //    Output.debugPrint(Thread.currentThread().getName() + " just entered isStopped");
    return getSemaphore().hasQueuedThreads();
  }

  default void waitUntilStopped() {
    while (!isStopped()) {
      Output.debugPrint(
          Thread.currentThread().getName() + " waiting for " + getThreadName() + " to stop");
      ThreadHelper.threadSleep(10);
    }
  }

  default void start() {
    // Wait until what we want to start is stopped.
    waitUntilStopped();

    // Ensure semaphore remains binary.
    if (getSemaphore().availablePermits() < 1) {
      Output.debugPrint(Thread.currentThread().getName() + " starting " + getThreadName());
      getSemaphore().release();
    } else {
      Output.debugPrint(
          Thread.currentThread().getName()
              + " did not release "
              + getThreadName()
              + "'s semaphore because its available permits are not less than 1");
    }
  }
}
