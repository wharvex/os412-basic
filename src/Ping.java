import java.util.List;
import java.util.UUID;

public class Ping extends UserlandProcess {
  public Ping() {
    super(UUID.randomUUID().toString().substring(24), "ping");
  }

  @Override
  void main() {
    int i = 0;
    while (true) {
      Output.print("Hello from Ping " + getDebugPid() + " (times printed: " + (++i) + ")");
      ThreadHelper.threadSleep(1000);
      cooperate();
    }
  }

  @Override
  public List<KernelMessage> getMessages() {
    return null;
  }
}
