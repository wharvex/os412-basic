import java.util.Random;
import java.util.stream.IntStream;

public class RandomDevice implements Device {
  private final Random[] randArr;

  public RandomDevice() {
    randArr = new Random[10];
  }

  @Override
  public int open(String s) {
    int seed = Integer.parseInt(s);
    randArr[
            IntStream.range(0, randArr.length)
                .filter(i -> randArr[i] == null)
                .findFirst()
                .orElseThrow(() -> new RuntimeException("randArr full"))] =
        new Random(seed);
    return -1;
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
