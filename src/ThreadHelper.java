import java.util.Arrays;

public class ThreadHelper {

  public static Thread getThreadByName(String threadName) {
    return Thread.getAllStackTraces().keySet().stream()
        .filter(t -> t.getName().equals(threadName))
        .findFirst()
        .orElseThrow(() -> new RuntimeException("Thread " + threadName + " not found."));
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
