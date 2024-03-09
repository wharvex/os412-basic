import java.util.UUID;

public class Pong extends UserlandProcess {
  public Pong() {
    super(UUID.randomUUID().toString().substring(24), "pong");
  }

  @Override
  void main() {}
}
