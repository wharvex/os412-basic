/** Maps calls to the other devices */
public class VFS implements Device {
  private final FakeFileSystem ffs;
  private final RandomDevice rd;

  public VFS() {
    ffs = new FakeFileSystem();
    rd = new RandomDevice();
  }

  public RandomDevice getRd() {
    return rd;
  }

  public FakeFileSystem getFfs() {
    return ffs;
  }

  @Override
  public int open(String s) {
    var openCodeAndArg = s.split(" ");
    MiscHelper.enforceArrayLength(openCodeAndArg, 2);
    var openCode = openCodeAndArg[0];
    var openArg = openCodeAndArg[1];
    MiscHelper.enforceNonNullNonEmptyNonBlankString(openCode);
    MiscHelper.enforceNonNullNonEmptyNonBlankString(openArg);
    return switch (openCode) {
      case "file" -> getFfs().open(openArg);
      case "random" -> getRd().open(openArg);
      default -> -1;
    };
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
