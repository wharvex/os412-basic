import java.util.Objects;
import java.util.function.IntBinaryOperator;
import java.util.function.IntFunction;
import java.util.function.IntUnaryOperator;
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

  public static int findNullIndex(IntFunction<Object> f, int size) {
    return IntStream.range(0, size).filter(i -> Objects.isNull(f.apply(i))).findFirst().orElse(-1);
  }

  public static int findNegativeIndex(IntUnaryOperator f, int size) {
    return IntStream.range(0, size).filter(i -> f.applyAsInt(i) < 0).findFirst().orElse(-1);
  }

  public static int findNegativeIndex(IntBinaryOperator f, int size, int src) {
    return IntStream.range(0, size).filter(i -> f.applyAsInt(src, i) < 0).findFirst().orElse(-1);
  }

  public static int[] makeIntArr(int size) {
    return IntStream.generate(() -> -1).limit(size).toArray();
  }
}
