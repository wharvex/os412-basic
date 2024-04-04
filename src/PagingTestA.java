import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class PagingTestA extends UserlandProcess {
  private final List<Integer> allocationIndices = new ArrayList<>();

  public PagingTestA() {
    super(UUID.randomUUID().toString().substring(24), "memA");
  }

  @Override
  void main() {
    int i = 0;
    while (true) {
      OutputHelper.print(
          "Hello from PagingTestA " + getDebugPid() + " (times printed: " + (++i) + ")");
      // Allocate.
      OS.allocateMemory(this, (ucs, idx) -> allocationIndices.add((int) idx), 5);
      OutputHelper.debugPrint("allocationIndices: " + allocationIndices);
      // Write.
      // Read.
      int readAddress = RandomHelper.getAddress();
      OutputHelper.print("PagingTestA reading from address " + readAddress);
      read(readAddress);
      ThreadHelper.threadSleep(1000);
      cooperate();
    }
  }
}
