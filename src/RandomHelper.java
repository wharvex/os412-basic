import java.util.concurrent.ThreadLocalRandom;

public class RandomHelper {
  public static int getVirt() {
    return ThreadLocalRandom.current().nextInt(1, 11);
  }

  public static int getPhys() {
    return ThreadLocalRandom.current().nextInt(11, 21);
  }

  public static int getAddress() {
    return ThreadLocalRandom.current().nextInt(OS.getPageSize(), OS.getPageSize() * 8);
  }
}
