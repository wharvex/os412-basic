import java.util.UUID;

public class VirtMemTestB extends UserlandProcess {
  public VirtMemTestB() {
    super(UUID.randomUUID().toString().substring(24), "virtMemB");
  }

  @Override
  void main() {
    int i = 0;
    while (true) {
      OutputHelper.print(
          "Hello from VirtMemTestB " + getDebugPid() + " (times printed: " + (++i) + ")");
      switch (i) {
      }
      ThreadHelper.threadSleep(1000);
      cooperate();
    }
  }
}
