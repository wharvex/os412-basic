import java.util.Arrays;
import java.util.Optional;

public class ThreadHelper {

  public static Optional<Thread> getThreadByName(String threadName) {
    return Thread.getAllStackTraces().keySet().stream()
        .filter(t -> t.getName().equals(threadName))
        .findFirst();
  }

  public static String getThreadStateString(String threadName) {
    return getThreadByName(threadName).map(t -> t.getState().toString()).orElse("no thread");
  }

  public static void printAllThreads() {
    Thread.getAllStackTraces()
        .keySet()
        .forEach(
            t -> {
              Output.debugPrint("Thread: " + t.getName());
              Output.debugPrint("Status: " + t.getState());
              Output.debugPrint("Stack trace: " + Arrays.toString(t.getStackTrace()));
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
