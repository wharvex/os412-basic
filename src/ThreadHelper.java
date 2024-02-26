import java.util.Arrays;
import java.util.Optional;

public class ThreadHelper {

  public static Optional<Thread> getThreadByName(String threadName) {
    return Thread.getAllStackTraces().keySet().stream()
        .filter(t -> t.getName().equals(threadName))
        .findFirst();
  }

  public static void printAllThreads() {
    Thread.getAllStackTraces()
        .keySet()
        .forEach(
            t -> {
              System.out.println("Thread: " + t.getName());
              System.out.println("Status: " + t.getState());
              System.out.println("Stack trace: " + Arrays.toString(t.getStackTrace()));
            });
  }

  public static void threadSleep(long millis) {
    try {
      Thread.sleep(millis);
    } catch (InterruptedException e) {
      System.out.println("Sleep interrupted.");
    }
  }
}
