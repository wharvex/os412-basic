import java.util.Arrays;

/** Coordinates calls to the secondary devices. */
public class VFS implements Device {
  private final DeviceToIds[] deviceToIdsColl;

  public VFS() {
    deviceToIdsColl = new DeviceToIds[OS.EXISTING_SECONDARY_DEVICES];
    addToDeviceToIdsColl(new FakeFileSystem());
    addToDeviceToIdsColl(new RandomDevice());
  }

  public DeviceToIds[] getDeviceToIdsColl() {
    return deviceToIdsColl;
  }

  public DeviceToIds getFromDeviceToIdsCollByClass(Class<?> c) {
    return Arrays.stream(getDeviceToIdsColl())
        .filter(dti -> c.isInstance(dti.getDevice()))
        .findFirst()
        .orElseThrow(() -> new RuntimeException(c.getName() + " DTI not found."));
  }

  public DeviceToIds getFromDeviceToIdsCollByIndex(int idx) {
    return getDeviceToIdsColl()[idx];
  }

  public void addToDeviceToIdsColl(Device d) {
    int idx =
        MiscHelper.findNullIndex(
            this::getFromDeviceToIdsCollByIndex, OS.EXISTING_SECONDARY_DEVICES);
    if (idx < 0) {
      throw new RuntimeException("Too many devices!");
    }
    getDeviceToIdsColl()[idx] = new DeviceToIds(d);
  }

  public FakeFileSystem getFfs() {
    return (FakeFileSystem) getFromDeviceToIdsCollByClass(FakeFileSystem.class).getDevice();
  }

  public RandomDevice getRd() {
    return (RandomDevice) getFromDeviceToIdsCollByClass(RandomDevice.class).getDevice();
  }

  public int getFromRdIds(int idx) {
    return getFromDeviceToIdsCollByClass(RandomDevice.class).getIds()[idx];
  }

  public int getFromFfsIds(int idx) {
    return getFromDeviceToIdsCollByClass(FakeFileSystem.class).getIds()[idx];
  }

  public int addToFfsIds(int id) {
    int idx = MiscHelper.findNegativeIndex(this::getFromFfsIds, OS.DEVICE_CONTENTS_SIZE);
    if (idx < 0) {
      return idx;
    }
    getFromDeviceToIdsCollByClass(FakeFileSystem.class).getIds()[idx] = id;
    return idx;
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
      case "file" -> {
        int result = getFfs().open(openArg);
        if (result < 0) {
          throw new RuntimeException("fail");
        }
        yield result;
      }
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
