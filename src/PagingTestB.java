import java.util.UUID;

public class PagingTestB extends UserlandProcess {
  public PagingTestB() {
    super(UUID.randomUUID().toString().substring(24), "pagingB");
  }

  @Override
  void main() {
    int i = 0;
    while (true) {
      OutputHelper.print(
          "Hello from PagingTestB " + getDebugPid() + " (times printed: " + (++i) + ")");
      ThreadHelper.threadSleep(1000);
      cooperate();
    }
  }
}
