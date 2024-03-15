import java.util.List;
import java.util.UUID;

public class Ping extends UserlandProcess {
  public Ping() {
    super(UUID.randomUUID().toString().substring(24), "ping");
  }

  private void addAllToMessages(List<KernelMessage> kms) {
    getMessages().addAll(kms);
  }

  @Override
  void main() {
    int i = 0;
    while (true) {
      Output.print("Hello from Ping " + getDebugPid() + " (times printed: " + (++i) + ")");
      if (i == 4) {
        Output.print("Ping waiting for message");
        OS.waitForMessage(this);
        addAllToMessages(OS.getMessagesAndClear());
        Output.print(
            "Ping's received message content: " + getMessages().get(0).getMessageContentString());
      }
      ThreadHelper.threadSleep(1000);
      cooperate();
    }
  }
}
