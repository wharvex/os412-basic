public class DeviceToIds {
  private final Device device;
  private final int[] ids;

  public DeviceToIds(Device device) {
    this.device = device;
    ids = new int[OS.DEVICE_CONTENTS_SIZE];
  }

  public Device getDevice() {
    return device;
  }

  public int[] getIds() {
    return ids;
  }
}
