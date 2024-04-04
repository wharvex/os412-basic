import java.util.UUID;

public class PagingTestB extends UserlandProcess {
  public PagingTestB() {
    super(UUID.randomUUID().toString().substring(24), "memB");
  }

  @Override
  void main() {
    int i = 0;
    while (true) {
      OutputHelper.print(
          "Hello from PagingTestB " + getDebugPid() + " (times printed: " + (++i) + ")");
      // Allocate.
      // Write.
      // Read.
      int readAddress = RandomHelper.getAddress();
      OutputHelper.print("PagingTestB reading from address " + readAddress);
      read(readAddress);
      ThreadHelper.threadSleep(1000);
      cooperate();
    }
  }
}
