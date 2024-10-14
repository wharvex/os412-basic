import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class VirtMemTestB extends UserlandProcess {
  private final List<Integer> fileDescriptors = new ArrayList<>();
  private final List<Integer> allocationIndices = new ArrayList<>();

  public VirtMemTestB() {
    super(UUID.randomUUID().toString().substring(24), "virtMemB");
  }

  public List<Integer> getFileDescriptors() {
    return fileDescriptors;
  }

  public void addToFileDescriptors(int id) {
    getFileDescriptors().add(id);
  }

  public int getFromFileDescriptors(int idx) {
    return getFileDescriptors().get(idx);
  }

  @Override
  void main() {
    int i = 0;
    int allocationSizeInPages = 500000000;
    byte writeByte = 33;
    int virtualAddress = -1;
    while (true) {
      OutputHelper.print(
          "Hello from VirtMemTestB " + getDebugPid() + " (times printed: " + (++i) + ")");
      switch (i) {
        case 1:
          // Do nothing.
          OutputHelper.print("VirtMemTestB says: I'm doing nothing for now.");
          break;
        case 2:
          // Do nothing again.
          OutputHelper.print("VirtMemTestB says: I'm still doing nothing for now.");
          break;
        case 3:
          // Lazy allocate an unreasonable amount, so we'll need to steal.
          OutputHelper.print(
              "VirtMemTestB says: attempting to allocate "
                  + allocationSizeInPages
                  + " pages of memory.");
          OS.allocateMemory(
              this,
              (ucs, idx) -> allocationIndices.add((int) idx),
              allocationSizeInPages * OS.getPageSize());
          virtualAddress = allocationIndices.getFirst();
          if (virtualAddress >= 0) {
            OutputHelper.print(
                "VirtMemTestB successfully allocated "
                    + allocationSizeInPages
                    + " pages of memory starting at virtual address "
                    + virtualAddress);
          } else {
            OutputHelper.print("VirtMemTestB failed to allocate.");
          }
          break;

        default:
          OutputHelper.print("VirtMemTestB says: done testing.");
      }
      ThreadHelper.threadSleep(1000);
      cooperate();
    }
  }
}
