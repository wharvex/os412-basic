public class KernelMessage {
  private final int senderPid;
  private final int targetPid;

  public KernelMessage(int senderPid, int targetPid) {
    this.senderPid = senderPid;
    this.targetPid = targetPid;
  }

  public int getTargetPid() {
    return targetPid;
  }

  public int getSenderPid() {
    return senderPid;
  }
}
