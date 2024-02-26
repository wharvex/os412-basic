public class OS {
  private static IStoppable kernel;
  private static ISwitchable switcher;

  public static PidGroup startup(
      ISwitchable switcher, UserlandProcess init, UserlandProcess init2, UserlandProcess init3) {
    // Create Kernel and start its thread.
    OS.kernel = new Kernel();
    OS.kernel.initializeThread();

    // Create the initial processes, switch to one of them, return their pids.
    return OS.startupCreateProcess(switcher, init, init2, init3);
  }

  public static void sleep(ISwitchable switcher, long sleepLenInMillis) {}

  public static PidGroup startupCreateProcess(ISwitchable switcher, UserlandProcess... ups) {
    return new PidGroup(1, 2, 3);
  }
}
