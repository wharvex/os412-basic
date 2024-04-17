public class VirtualToPhysicalMapping {
    private int physicalPageNumber;
    private int onDiskPageNumber;

    public VirtualToPhysicalMapping() {
        physicalPageNumber = -1;
        onDiskPageNumber = -1;
    }

    public int getPhysicalPageNumber() {
        return physicalPageNumber;
    }

    public void setPhysicalPageNumber(int physicalPageNumber) {
        this.physicalPageNumber = physicalPageNumber;
    }

    public int getOnDiskPageNumber() {
        return onDiskPageNumber;
    }

    public void setOnDiskPageNumber(int onDiskPageNumber) {
        this.onDiskPageNumber = onDiskPageNumber;
    }

}
