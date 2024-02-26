import java.util.concurrent.Semaphore;

public interface IStoppable {

  default void initializeThread() {
    getThread().start();
  }

  default String getThreadName() {
    return getThread().getName();
  }

  default void parkAtSemaphore() {
    if (Thread.currentThread() != this.getThread()) {
      throw new RuntimeException("Parking space reserved for " + this + "'s thread.");
    }
    try {
      getSemaphore().acquire();
    } catch (InterruptedException e) {
      System.out.println(this + " interrupted while parking at its semaphore.");
      Thread.currentThread().interrupt();
    }
  }

  Semaphore getSemaphore();

  Thread getThread();

  default void releaseSemaphore() {
    if (this.getSemaphore().availablePermits() < 1) {
      this.getSemaphore().release();
    }
  }
}
