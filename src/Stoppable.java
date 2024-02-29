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
    if (Thread.currentThread() != getThread()) {
      throw new RuntimeException(
          Output.getErrorString("Parking space reserved for " + getThreadName()));
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

  default void start() {
    // Wait until what we want to start is stopped.
    while (!isStopped()) {
      Output.debugPrint(
          Thread.currentThread().getName() + " waiting for " + getThreadName() + " to stop");
      ThreadHelper.threadSleep(10);
    }

    // Ensure semaphore remains binary.
    if (getSemaphore().availablePermits() < 1) {
      Output.debugPrint(
          Thread.currentThread().getName()
              + " releasing semaphore at which "
              + getThreadName()
              + " is parked");
      getSemaphore().release();
    } else {
      Output.debugPrint(
          "Semaphore for "
              + getThreadName()
              + " not released by "
              + Thread.currentThread().getName());
    }
  }
}
