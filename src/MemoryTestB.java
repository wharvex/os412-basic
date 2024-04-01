import java.util.UUID;

public class MemoryTestB extends UserlandProcess {
  public MemoryTestB() {
    super(UUID.randomUUID().toString().substring(24), "memB");
  }

  @Override
  void main() {
    int i = 0;
    while (true) {
      Output.print("Hello from MemoryTestB " + getDebugPid() + " (times printed: " + (++i) + ")");
      ThreadHelper.threadSleep(1000);
      cooperate();
    }
  }
}
