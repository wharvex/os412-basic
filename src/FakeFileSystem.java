import java.io.RandomAccessFile;

public class FakeFileSystem implements Device {
  private final RandomAccessFile[] randAccArr;
  private final String filename;

  public FakeFileSystem(String filename) {
    try {
      if (filename.isEmpty() || filename.isBlank() || filename == null) {
        throw new RuntimeException(
            Output.getErrorString(
                "FakeFileSystem constructor expected non-null, non-blank, non-empty filename string."));
      }
    } catch (RuntimeException e) {
      Output.writeToFile(e.toString());
      throw e;
    }
    this.filename = filename;
    randAccArr = new RandomAccessFile[10];
  }

  @Override
  public int open(String s) {
    return 0;
  }

  @Override
  public void close(int id) {}

  @Override
  public byte[] read(int id, int size) {
    return new byte[0];
  }

  @Override
  public void seek(int id, int to) {}

  @Override
  public int write(int id, byte[] data) {
    return 0;
  }
}
