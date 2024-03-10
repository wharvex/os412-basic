import java.util.UUID;

public class Pong extends UserlandProcess {
  public Pong() {
    super(UUID.randomUUID().toString().substring(24), "pong");
  }

  @Override
  void main() {
    int i = 0;
    while (true) {
      Output.print("Hello from Pong " + getDebugPid() + " (times printed: " + (++i) + ")");
      ThreadHelper.threadSleep(1000);
      cooperate();
    }
  }
}
