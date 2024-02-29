import java.util.*;

/**
 * OSLAND
 *
 * <p>Models the gateway between userland and kernelland.
 */
public class OS {
  private static final List<Object> params = new ArrayList<>();
  private static Kernel kernel;
  private static UnprivilegedContextSwitcher contextSwitcher;
  private static Object retVal;
  private static CallType callType;

  /**
   * Only called by Bootloader thread.
   *
   * @param cs
   * @return
   */
  public static int startup(UnprivilegedContextSwitcher cs) {
    // Create Kernel and start its thread.
    kernel = new Kernel();
    getKernel().init();

    // Create the ProcessCreator process; switch to it; return its pid.
    return startupCreateProcess(cs, new ProcessCreator(), PriorityType.REALTIME);
  }

  /**
   * Use Objects.requireNonNull for this method and other getters and setters on OS because OS has
   * no constructor and therefore more null danger.
   *
   * @return the kernel, duh
   */
  private static Kernel getKernel() {
    Objects.requireNonNull(kernel, "Tried to get OS.kernel but it was null.");
    return kernel;
  }

  public static void sleep(UnprivilegedContextSwitcher cs, long sleepLenInMillis) {}

  public static int startupCreateProcess(
      UnprivilegedContextSwitcher cs, UserlandProcess processCreator, PriorityType pt) {
    switchContextAntechamber(cs, CallType.STARTUP_CREATE_PROCESS, processCreator, pt);
    return (int)
        getRetVal()
            .orElseThrow(
                () ->
                    new RuntimeException(
                        Output.getErrorString("Expected int retVal from createProcess")));
  }

  /**
   * The point of this method is to have one central location where all the data sharing needed for
   * a particular context switch occurs.
   *
   * <p>It is synchronized so no other context switch can occur at the same time.
   *
   * @param cs
   * @param callType
   * @param params
   */
  public static synchronized void switchContext(
      UnprivilegedContextSwitcher cs, CallType callType, Object... params) {
    // Announce our arrival.
    Output.debugPrint(Thread.currentThread().getName() + " just entered switchContext");

    // Store a reference on OS to the Runnable whose thread is calling this method.
    setContextSwitcher(cs);

    // Set the call type for this context switch.
    setCallType(callType);

    // Clear current params; set new ones.
    setParams(params);

    // Start Kernel; stop contextSwitcher.
    startKernel();

    // Save the value returned from the Kernel to the context switcher.
    getRetVal().ifPresent(cs::setContextSwitchRet);
  }

  private static void setParams(Object... newParams) {
    // The params list will be empty after this call returns.
    params.clear();

    // Check if any of the newParams are null.
    if (Arrays.stream(newParams).anyMatch(Objects::isNull)) {
      throw new RuntimeException(Output.getErrorString("Cannot add any null elements to params."));
    }

    // Add new params to params.
    params.addAll(List.of(newParams));
  }

  /**
   * Only called by Kernel thread.
   *
   * @param idx
   * @return
   */
  public static Object getParam(int idx) {
    if (idx < 0 || idx >= params.size()) {
      throw new RuntimeException(Output.getErrorString("Param index " + idx + " out of range."));
    }
    Object param = params.get(idx);
    Objects.requireNonNull(
        param, Output.getErrorString("Tried to get param at index " + idx + " but it was null."));
    return param;
  }

  /**
   * If retVal is null, it means the kernelland method's return type was void, and we do not save it
   * to the contextSwitcher.
   *
   * @return
   */
  public static Optional<Object> getRetVal() {
    return Optional.ofNullable(retVal);
  }

  /** Only called by contextSwitcher thread. */
  private static void startKernel() {
    getKernel().start();
    getContextSwitcher().stop();
  }

  public static void startKernelOnly() {
    getKernel().start();
  }

  /** Only called by Kernel thread. */
  public static void startContextSwitcher() {
    getContextSwitcher().start();
  }

  /** Where you take off your coat and wait to be let into the switchContext method. */
  public static void switchContextAntechamber(
      UnprivilegedContextSwitcher ucs, CallType callType, Object... params) {
    Output.debugPrint(Thread.currentThread().getName() + " is about to enter switchContext");
    switchContext(ucs, callType, params);
  }

  /**
   * Only called by Kernel thread. Set to null if the kernelland method has a void return type.
   *
   * @param rv The kernelland function return value.
   */
  public static void setRetValOnOS(Object rv) {
    retVal = rv;
  }

  public static UnprivilegedContextSwitcher getContextSwitcher() {
    Objects.requireNonNull(
        contextSwitcher, Output.getErrorString("Tried to get OS.contextSwitcher but it was null"));
    return contextSwitcher;
  }

  public static void setContextSwitcher(UnprivilegedContextSwitcher cs) {
    Objects.requireNonNull(cs, Output.getErrorString("Cannot set OS.contextSwitcher to null"));
    contextSwitcher = cs;
  }

  public static CallType getCallType() {
    Objects.requireNonNull(
        callType, Output.getErrorString("Tried to get OS.callType but it was null"));
    return callType;
  }

  public static void setCallType(CallType ct) {
    Objects.requireNonNull(ct, Output.getErrorString("Cannot set OS.callType to null."));
    callType = ct;
  }

  public enum CallType {
    STARTUP_CREATE_PROCESS,
    CREATE_PROCESS,
    SWITCH_PROCESS,
    SLEEP
  }

  public enum PriorityType {
    REALTIME,
    INTERACTIVE,
    BACKGROUND
  }
}
