import java.util.UUID;

public class VirtMemTestA extends UserlandProcess {
  public VirtMemTestA() {
    super(UUID.randomUUID().toString().substring(24), "virtMemA");
  }

  @Override
  void main() {
    int i = 0;
    while (true) {
      OutputHelper.print(
          "Hello from VirtMemTestA " + getDebugPid() + " (times printed: " + (++i) + ")");
      switch (i) {
      }
      ThreadHelper.threadSleep(1000);
      cooperate();
    }
  }
}
