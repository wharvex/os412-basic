import java.util.UUID;

public class UnsleepyProcess extends UserlandProcess {
  public UnsleepyProcess() {
    super(UUID.randomUUID().toString().substring(24), "unsleepy");
  }

  @Override
  void main() {
    int i = 0;
    while (true) {
      System.out.println(
          "Hello from Unsleepy " + getDebugPid() + " (times printed: " + (++i) + ")");
      cooperate();
      ThreadHelper.threadSleep(500);
    }
  }
}
