import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class VirtMemTestA extends UserlandProcess {
  private final List<Integer> fileDescriptors = new ArrayList<>();

  public VirtMemTestA() {
    super(UUID.randomUUID().toString().substring(24), "virtMemA");
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
    while (true) {
      OutputHelper.print(
          "Hello from VirtMemTestA " + getDebugPid() + " (times printed: " + (++i) + ")");
      switch (i) {
        case 1:
          // Create swapfile.
          OutputHelper.print("VirtMemTestA says: attempting to create/open swap file.");
          OS.open(this, id -> this.addToFileDescriptors((int) id), "file swap");
          break;
        case 2:
          // Print swapfile FD.
          OutputHelper.print("VirtMemTestA says: swapfile FD: " + getFromFileDescriptors(0));
          break;
        case 3:
          // Lazy allocate.
          OutputHelper.print(
              "VirtMemTestA says: attempting to allocate "
                  + allocationSizeInPages
                  + " pages of memory.");
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

        default:
          OutputHelper.print("VirtMemTestA says: done testing.");
      }
      ThreadHelper.threadSleep(1000);
      cooperate();
    }
  }
}
