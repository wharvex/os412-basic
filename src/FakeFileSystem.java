import java.io.RandomAccessFile;

public class FakeFileSystem implements Device {
  private static final int FILES_SIZE = 50;
  private final RandomAccessFile[] files;

  public FakeFileSystem() {
    files = new RandomAccessFile[FILES_SIZE];
  }

  public RandomAccessFile[] getFiles() {
    return files;
  }

  @Override
  public int open(String filename) {
    try {
      if (filename == null || filename.isEmpty() || filename.isBlank()) {
        throw new RuntimeException(
            OutputHelper.getErrorString(
                "FakeFileSystem constructor expected non-null, non-empty, non-blank filename"
                    + " string."));
      }
    } catch (RuntimeException e) {
      OutputHelper.writeToFile(e.toString());
      throw e;
    }
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
