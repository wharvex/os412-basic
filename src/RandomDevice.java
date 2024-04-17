import java.util.Objects;
import java.util.Random;
import java.util.stream.IntStream;

public class RandomDevice implements Device {
  private static final int GENERATORS_SIZE = 50;
  private final Random[] generators;

  public RandomDevice() {
    generators = new Random[GENERATORS_SIZE];
  }

  public Random[] getGenerators() {
    return generators;
  }

  public Random getFromGenerators(int idx) {
    return getGenerators()[idx];
  }

  public int addToGenerators(Random r) {
    int idx =
        IntStream.range(0, GENERATORS_SIZE)
            .filter(i -> Objects.isNull(getFromGenerators(i)))
            .findFirst()
            .orElse(-1);
    if (idx < 0) {
      return idx;
    }
    getGenerators()[idx] = r;
    return idx;
  }

  @Override
  public int open(String s) {
    int seed = Integer.parseInt(s);
    return addToGenerators(new Random(seed));
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
