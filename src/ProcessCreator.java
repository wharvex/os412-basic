public class ProcessCreator extends UserlandProcess {
  public ProcessCreator() {
    super("0", "processCreator");
  }

  @Override
  void main() {
    System.out.println("Hello from ProcessCreator");
  }
}
