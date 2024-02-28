import java.util.UUID;

/** USERLAND */
public class SleepyProcess extends UserlandProcess {
  public SleepyProcess() {
    super(UUID.randomUUID().toString().substring(24), "sleepy");
  }

  @Override
  void main() {
    int i = 0;
    while (true) {
      Output.print("Hello from Sleepy " + getDebugPid() + " (times printed: " + (++i) + ")");
      OS.sleep(this, 500);
      cooperate();
    }
  }
}
