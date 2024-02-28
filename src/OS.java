import java.util.*;

/** Models the gateway between userland and kernelland. */
public class OS {
  private static final List<Object> params = new ArrayList<>();
  private static Kernel kernel;
  private static ContextSwitcher contextSwitcher;
  private static Object retVal;
  private static CallType callType;

  /**
   * Only called by Bootloader thread.
   *
   * @param cs
   * @return
   */
  public static int startup(ContextSwitcher cs) {
    // Create Kernel and start its thread.
    kernel = new Kernel();
    getKernel().init();

    // Create the ProcessCreator process; switch to it; return its pid.
    return startupCreateProcess(cs, new ProcessCreator());
  }

  private static Kernel getKernel() {
    Objects.requireNonNull(kernel, "Tried to get OS.kernel but it was null.");
    return kernel;
  }

  public static void sleep(ContextSwitcher cs, long sleepLenInMillis) {}

  public static int startupCreateProcess(ContextSwitcher cs, UserlandProcess processCreator) {
    switchContext(cs, CallType.STARTUP_CREATE_PROCESS, processCreator);
    return (int)
        getRetVal()
            .orElseThrow(() -> new RuntimeException("Expected int retVal from createProcess."));
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
      ContextSwitcher cs, CallType callType, Object... params) {
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
      throw new RuntimeException("Cannot add any null elements to params.");
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
      throw new RuntimeException("Param index " + idx + " out of range.");
    }
    Object param = params.get(idx);
    Objects.requireNonNull(param, "Tried to get param at index " + idx + " but it was null.");
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

  /** Only called by Kernel thread. */
  public static void stopKernel() {
    getContextSwitcher().start();
    getKernel().stop();
  }

  /**
   * Only called by Kernel thread. Set to null if the kernelland method has a void return type.
   *
   * @param rv The kernelland function return value.
   */
  public static void setRetValOnOS(Object rv) {
    retVal = rv;
  }

  public static ContextSwitcher getContextSwitcher() {
    Objects.requireNonNull(contextSwitcher, "Tried to get OS.contextSwitcher but it was null.");
    return contextSwitcher;
  }

  public static void setContextSwitcher(ContextSwitcher cs) {
    Objects.requireNonNull(cs, "Cannot set OS.contextSwitcher to null.");
    contextSwitcher = cs;
  }

  public static CallType getCallType() {
    Objects.requireNonNull(callType, "Tried to get OS.callType but it was null.");
    return callType;
  }

  public static void setCallType(CallType ct) {
    Objects.requireNonNull(ct, "Cannot set OS.callType to null.");
    callType = ct;
  }

  public enum CallType {
    STARTUP_CREATE_PROCESS,
    CREATE_PROCESS,
    SWITCH_PROCESS,
    SLEEP
  }
}
