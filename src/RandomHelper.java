import java.util.concurrent.ThreadLocalRandom;

public class RandomHelper {
  public static int getVirtPageNum() {
    return ThreadLocalRandom.current().nextInt(0, OS.getMemoryMapSize());
  }

  public static int getPhysPageNum() {
    return ThreadLocalRandom.current().nextInt(0, OS.getFreeSpaceSize());
  }

  public static int getAddress() {
    return ThreadLocalRandom.current().nextInt(OS.getPageSize(), OS.getPageSize() * 8);
  }
}
