import java.util.UUID;

public class MemoryTestA extends UserlandProcess {
  public MemoryTestA() {
    super(UUID.randomUUID().toString().substring(24), "memA");
  }

  @Override
  void main() {
    int i = 0;
    while (true) {
      OutputHelper.print(
          "Hello from MemoryTestA " + getDebugPid() + " (times printed: " + (++i) + ")");
      int readAddress = RandomHelper.getAddress();
      OutputHelper.print("MemoryTestA reading from address " + readAddress);
      read(readAddress);
      ThreadHelper.threadSleep(1000);
      cooperate();
    }
  }
}
