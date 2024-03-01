public class VFS implements Device {

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
