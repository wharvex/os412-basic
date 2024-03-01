/** KERNELLAND */
public interface Device {
  int open(String s);

  void close(int id);

  byte[] read(int id, int size);

  void seek(int id, int to);

  int write(int id, byte[] data);
}
