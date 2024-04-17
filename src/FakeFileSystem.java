import java.io.FileNotFoundException;
import java.io.RandomAccessFile;

public class FakeFileSystem implements Device {
  private final RandomAccessFile[] files;

  public FakeFileSystem() {
    files = new RandomAccessFile[OS.DEVICE_CONTENTS_SIZE];
  }

  public RandomAccessFile[] getFiles() {
    return files;
  }

  public RandomAccessFile getFromFiles(int idx) {
    return getFiles()[idx];
  }

  public int addToFiles(RandomAccessFile raf) {
    // Look for a null (free) index in files.
    int idx = MiscHelper.findNonNullIndex(this::getFromFiles, OS.DEVICE_CONTENTS_SIZE);

    // If there is no free index, return the error code.
    if (idx < 0) {
      return idx;
    }

    // Store the given RAF in files at the found index.
    getFiles()[idx] = raf;

    // Return the found index.
    return idx;
  }

  @Override
  public int open(String filename) {
    MiscHelper.enforceNonNullNonEmptyNonBlankString(filename);
    return addToFiles(createRAF(filename));
  }

  public RandomAccessFile createRAF(String filename) {
    try {
      return new RandomAccessFile(filename, "rw");
    } catch (FileNotFoundException e) {
      OutputHelper.writeToFile(OutputHelper.getErrorStringCatch(e));
      throw new RuntimeException(OutputHelper.getErrorStringThrow("File not found (see log)."));
    }
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
