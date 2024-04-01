import java.util.UUID;

public class MemoryTestA extends UserlandProcess {
  public MemoryTestA() {
    super(UUID.randomUUID().toString().substring(24), "memA");
  }

  @Override
  void main() {
    int i = 0;
    while (true) {
      Output.print("Hello from MemoryTestA " + getDebugPid() + " (times printed: " + (++i) + ")");
      ThreadHelper.threadSleep(1000);
      cooperate();
    }
  }
}
