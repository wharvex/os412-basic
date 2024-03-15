import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.stream.IntStream;

/** KERNELLAND */
public class KernelMessage {
  private final int targetPid;
  private final int messageType;
  private final byte[] messageContent;
  private Integer senderPid;

  public KernelMessage(int targetPid, int messageType, String messageContent) {
    this.targetPid = targetPid;
    this.messageType = messageType;
    this.messageContent = new byte[messageContent.length() + 1];
    IntStream.range(0, messageContent.length())
        .forEach(i -> this.messageContent[i] = (byte) messageContent.charAt(i));
    this.messageContent[messageContent.length()] = '\0';
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

  public Integer getSenderPid() {
    try {
      if (senderPid == null) {
        throw new RuntimeException(
            Output.getErrorString("Tried to get senderPid but it was null."));
      }
    } catch (RuntimeException e) {
      Output.writeToFile(e.toString());
      throw e;
    }
    return senderPid;
  }

  public void setSenderPid(Integer senderPid) {
    this.senderPid = senderPid;
  }

  public int getMessageType() {
    return messageType;
  }

  public byte[] getMessageContent() {
    return messageContent;
  }

  public String getMessageContentString() {
    IntStream a = IntStream.range(0, getMessageContent().length).map(i -> getMessageContent()[i]);
    StringWriter b = new StringWriter();
    a.forEach(b::write);
    return b.toString();
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
