import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class PagingTestA extends UserlandProcess {
  private final List<Integer> allocationIndices = new ArrayList<>();

  public PagingTestA() {
    super(UUID.randomUUID().toString().substring(24), "pagingA");
  }

  @Override
  void main() {
    int i = 0;
    int allocationSizeInPages = 5;
    byte writeByte = 33;
    int virtualAddress = -1;
    while (true) {
      OutputHelper.print(
          "Hello from PagingTestA " + getDebugPid() + " (times printed: " + (++i) + ")");
      switch (i) {
        case 1:
          // Allocate.
          OutputHelper.print(
              "PagingTestA attempting to allocate " + allocationSizeInPages + " pages of memory.");
          OS.allocateMemory(
              this,
              (ucs, idx) -> allocationIndices.add((int) idx),
              allocationSizeInPages * OS.getPageSize());
          virtualAddress = allocationIndices.getFirst();
          if (virtualAddress >= 0) {
            OutputHelper.print(
                "PagingTestA successfully allocated "
                    + allocationSizeInPages
                    + " pages of memory starting at virtual address "
                    + virtualAddress);
          } else {
            OutputHelper.print("PagingTestA failed to allocate.");
          }
          break;
        case 2:
          // Write.
          if (virtualAddress >= 0) {
            OutputHelper.print(
                "PagingTestA attempting to write "
                    + writeByte
                    + " to virtual address "
                    + virtualAddress);
            write(virtualAddress, writeByte);
          } else {
            OutputHelper.print(
                "PagingTestA not attempting case 2 write due to case 1 allocation failure.");
          }
          break;
        case 3:
          // Read.
          if (virtualAddress >= 0) {
            OutputHelper.print(
                "PagingTestA attempting to read from virtual address " + virtualAddress);
            byte readByte = read(virtualAddress);
            OutputHelper.print(
                "PagingTestA read "
                    + readByte
                    + " from virtual address "
                    + virtualAddress
                    + "; expected "
                    + writeByte);
          } else {
            OutputHelper.print(
                "PagingTestA not attempting case 3 read due to case 1 allocation failure.");
          }
          break;
        case 4:
          // TODO: Free.
          break;
        case 5:
          // TODO: Test more allocation/read/write scenarios.
          break;
        default:
          OutputHelper.print("PagingTestA done testing.");
      }
      OutputHelper.debugPrint("allocationIndices: " + allocationIndices);
      ThreadHelper.threadSleep(1000);
      cooperate();
    }
  }
}
