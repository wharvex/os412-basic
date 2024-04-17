import java.util.Objects;
import java.util.function.Function;
import java.util.stream.IntStream;

public class MiscHelper {
  public static void enforceNonNullNonEmptyNonBlankString(String s) {
    try {
      if (s == null || s.isEmpty() || s.isBlank()) {
        throw new RuntimeException(
            OutputHelper.getErrorStringThrow("Expected non-null, non-empty, non-blank string."));
      }
    } catch (RuntimeException e) {
      OutputHelper.writeToFile(OutputHelper.getErrorStringCatch(e));
      throw e;
    }
  }

  public static void enforceArrayLength(Object[] arr, int len) {
    try {
      if (arr.length != len) {
        throw new RuntimeException(
            OutputHelper.getErrorStringThrow("Expected array length to be " + len));
      }
    } catch (RuntimeException e) {
      OutputHelper.writeToFile(OutputHelper.getErrorStringCatch(e));
      throw e;
    }
  }

  public static int findNonNullIndex(Function<Integer, Object> f, int size) {
    return IntStream.range(0, size).filter(i -> Objects.isNull(f.apply(i))).findFirst().orElse(-1);
  }
}
