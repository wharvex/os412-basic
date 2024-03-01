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
   * @return
   */
  private static Kernel getKernel() {
    Objects.requireNonNull(kernel, "Tried to get OS.kernel but it was null.");
    return kernel;
  }

  public static void sleep(UnprivilegedContextSwitcher cs, long sleepLenInMillis) {}

  public static int startupCreateProcess(
      UnprivilegedContextSwitcher cs, UserlandProcess processCreator, PriorityType pt) {
    switchContext(cs, CallType.STARTUP_CREATE_PROCESS, processCreator, pt);
    try {
      return (int)
          getRetVal()
              .orElseThrow(
                  () ->
                      new RuntimeException(
                          Output.getErrorString("Expected int retVal from createProcess")));

    } catch (RuntimeException e) {
      Output.writeToFile(e.toString());
      throw e;
    }
  }

  /**
   * The point of this method is to have one central location where all the data sharing needed for
   * a particular context switch occurs.
   *
   * <p>It is synchronized on the context switcher, so no other context switch can occur at the same
   * time.
   *
   * @param cs
   * @param callType
   * @param params
   */
  public static void switchContext(
      UnprivilegedContextSwitcher cs, CallType callType, Object... params) {
    Output.debugPrint(Thread.currentThread().getName() + " about to enter switchContext");
    synchronized (cs) {
      // Announce our arrival.
      Output.debugPrint(Thread.currentThread().getName() + " just entered switchContext");

      // Store a reference on OS to the Runnable whose thread is calling this method.
      preSetContextSwitcher(cs);

      // Set the call type for this context switch.
      setCallType(callType);

      // Clear current params; set new ones.
      setParams(params);

      // Start Kernel; stop contextSwitcher.
      startKernel(cs);

      // Save the value returned from the Kernel to the context switcher.
      getRetVal().ifPresent(cs::setContextSwitchRet);
    }
  }

  private static void setParams(Object... newParams) {
    // The params list will be empty after this call returns.
    params.clear();

    // Check if any of the newParams are null.
    try {
      if (Arrays.stream(newParams).anyMatch(Objects::isNull)) {
        throw new RuntimeException(
            Output.getErrorString("Cannot add any null elements to params."));
      }
    } catch (RuntimeException e) {
      Output.writeToFile(e.toString());
      throw e;
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
    try {
      if (idx < 0 || idx >= params.size()) {
        throw new RuntimeException(Output.getErrorString("Param index " + idx + " out of range."));
      }
    } catch (RuntimeException e) {
      Output.writeToFile(e.toString());
      throw e;
    }
    Object param = params.get(idx);
    try {
      Objects.requireNonNull(
          param, Output.getErrorString("Tried to get param at index " + idx + " but it was null."));
    } catch (NullPointerException e) {
      Output.writeToFile(e.toString());
      throw e;
    }
    return param;
  }

  /**
   * Data field X is guarded by the semaphore whose parking space is reserved for the thread that
   * wants to reliably "see" an update made to X.
   *
   * <p>As such, retVal is guarded by the UnprivilegedContextSwitcher's semaphore.
   *
   * <p>The UCS's thread is the only thread that needs to "see" updates to retVal. As such, we rely
   * solely on UCS's semaphore for visibility assurance, and leave this method unsynchronized.
   *
   * <p>If retVal is null, it means the kernelland method's return type was void, and we do not save
   * it to the contextSwitcher.
   *
   * @return
   */
  public static Optional<Object> getRetVal() {
    return Optional.ofNullable(retVal);
  }

  /** Only called by contextSwitcher thread. */
  private static void startKernel(UnprivilegedContextSwitcher cs) {
    startKernelOnly();
    cs.stop();
  }

  public static void startKernelOnly() {
    getKernel().start();
  }

  /** Only called by Kernel thread. */
  public static void startContextSwitcher() {
    preGetContextSwitcher().start();
  }

  /**
   * Only called by Kernel thread. Set to null if the kernelland method has a void return type.
   *
   * @param rv The kernelland function return value.
   */
  public static void setRetValOnOS(Object rv) {
    retVal = rv;
  }

  /**
   * Unsynchronized caller of getContextSwitcher to facilitate debugging.
   *
   * @return
   */
  public static UnprivilegedContextSwitcher preGetContextSwitcher() {
    Output.debugPrint(Thread.currentThread().getName() + " about to enter OS.getContextSwitcher");
    return getContextSwitcher();
  }

  /**
   * We do not rely on the Kernel's semaphore to guard contextSwitcher because other threads besides
   * the Kernel's may want to read its contents. So we make its getters/setters synchronized.
   *
   * @return
   */
  public static synchronized UnprivilegedContextSwitcher getContextSwitcher() {
    Output.debugPrint(Thread.currentThread().getName() + " just entered OS.getContextSwitcher");
    try {
      Objects.requireNonNull(
          contextSwitcher,
          Output.getErrorString("Tried to get OS.contextSwitcher but it was null"));
    } catch (NullPointerException e) {
      Output.writeToFile(e.toString());
      throw e;
    }
    return contextSwitcher;
  }

  public static synchronized void setContextSwitcher(UnprivilegedContextSwitcher cs) {
    Output.debugPrint(Thread.currentThread().getName() + " just entered OS.setContextSwitcher");
    try {
      Objects.requireNonNull(cs, Output.getErrorString("Cannot set OS.contextSwitcher to null"));
    } catch (NullPointerException e) {
      Output.writeToFile(e.toString());
      throw e;
    }
    contextSwitcher = cs;
  }

  /**
   * Unsynchronized caller of setContextSwitcher to facilitate debugging.
   *
   * @param cs
   */
  public static void preSetContextSwitcher(UnprivilegedContextSwitcher cs) {
    Output.debugPrint(Thread.currentThread().getName() + " about to enter OS.setContextSwitcher");
    setContextSwitcher(cs);
  }

  public static CallType getCallType() {
    try {
      Objects.requireNonNull(
          callType, Output.getErrorString("Tried to get OS.callType but it was null"));
    } catch (NullPointerException e) {
      Output.writeToFile(e.toString());
      throw e;
    }
    return callType;
  }

  /**
   * This is called prior to calling "release" on the Kernel's semaphore, so it "happens-before" the
   * Kernel's call to getCallType, which follows the Kernel's successful "acquire" of its
   * UCS-released semaphore (see the Oracle docs on Semaphore).
   *
   * <p>In other words, callType is "guarded by" the Kernel's semaphore, ensuring the Kernel can
   * "see" changes made to it by the UCS thread.
   *
   * @param ct
   */
  public static void setCallType(CallType ct) {
    try {
      Objects.requireNonNull(ct, Output.getErrorString("Cannot set OS.callType to null."));
    } catch (NullPointerException e) {
      Output.writeToFile(e.toString());
      throw e;
    }
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
