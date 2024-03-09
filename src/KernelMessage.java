import java.nio.charset.StandardCharsets;

/** KERNELLAND */
public class KernelMessage {
  private final int senderPid;
  private final int targetPid;
  private final int messageType;
  private final byte[] messageContent;

  public KernelMessage(int senderPid, int targetPid, int messageType, String messageContent) {
    this.senderPid = senderPid;
    this.targetPid = targetPid;
    this.messageType = messageType;
    this.messageContent = messageContent.getBytes(StandardCharsets.UTF_8);
  }

  /**
   * Copy constructor for preserving the OS wall.
   *
   * @param km the Userland KernelMessage
   */
  public KernelMessage(KernelMessage km) {
    senderPid = km.getSenderPid();
    targetPid = km.getTargetPid();
    messageType = km.getMessageType();
    messageContent = km.getMessageContent();
  }

  public int getTargetPid() {
    return targetPid;
  }

  public int getSenderPid() {
    return senderPid;
  }

  public int getMessageType() {
    return messageType;
  }

  public byte[] getMessageContent() {
    return messageContent;
  }

  @Override
  public String toString() {
    return "Target pid: "
        + getTargetPid()
        + "; Sender pid: "
        + getSenderPid()
        + "; Message type: "
        + getMessageType()
        + "; Message content: "
        + new String(getMessageContent(), StandardCharsets.UTF_8);
  }
}
