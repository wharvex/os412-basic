public class VFS implements Device {
  private final FakeFileSystem ffs;

  public VFS() {
    ffs = new FakeFileSystem();
  }

  @Override
  public int open(String s) {
    if (s.equals("swap")) {}
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
