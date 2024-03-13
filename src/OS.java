import java.util.*;
import java.util.function.BiConsumer;

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
  public static void startup(UnprivilegedContextSwitcher cs) {
    // Create Kernel and start its thread.
    kernel = new Kernel();
    getKernel().init();

    // Create the ProcessCreator process; switch to it; save its pid to the bootloader.
    startupCreateProcess(cs, new ProcessCreator(), Scheduler.PriorityType.REALTIME);
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

  public static void startupCreateProcess(
      UnprivilegedContextSwitcher cs, UserlandProcess processCreator, Scheduler.PriorityType pt) {
    switchContext(cs, CallType.STARTUP_CREATE_PROCESS, processCreator, pt);
  }

  public static void createProcess(
      UnprivilegedContextSwitcher cs,
      UserlandProcess up,
      Scheduler.PriorityType pt,
      BiConsumer<UnprivilegedContextSwitcher, Object> retSaver) {
    switchContext(cs, CallType.CREATE_PROCESS, retSaver, up, pt);
  }

  public static void sendMessage(UnprivilegedContextSwitcher cs, KernelMessage km) {
    switchContext(cs, CallType.SEND_MESSAGE, km);
  }

  public static void waitForMessage(UnprivilegedContextSwitcher cs) {
    switchContext(cs, CallType.WAIT_FOR_MESSAGE);
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
    Output.debugPrint(Output.DebugOutputType.SYNC_BEFORE_ENTER, cs.toString());
    Output.debugPrint("Call type: " + callType);
    synchronized (cs) {
      // Announce our arrival.
      Output.debugPrint(Output.DebugOutputType.SYNC_ENTER, cs.toString());

      // Store a reference on OS to the Runnable whose thread is calling this method.
      setContextSwitcher(cs);

      // Set the call type for this context switch.
      setCallType(callType);

      // Clear current params; set new ones.
      setParams(params);

      // Start Kernel; stop contextSwitcher.
      startKernel(cs);

      // Save the value returned from the Kernel to the context switcher.
      getRetVal().ifPresent(cs::addToCsRets);
    }
    Output.debugPrint(Output.DebugOutputType.SYNC_LEAVE, cs.toString());

    // The following is the logic that stops the context switcher if needed.
    // We can't have this in the sync block because then the cs would stop while holding the lock.

    Output.debugPrint(
        """


                    Checking if we should stop the contextSwitcher due to
                    being a UserlandProcess that is not the new currentlyRunning.
                    Checking shouldStopAfterContextSwitch set in Scheduler.switchContext.""");
    if (cs instanceof UserlandProcess) {
      Output.debugPrint("OS.contextSwitcher is a UserlandProcess");
      ((UserlandProcess) cs).preSetStopRequested(false);
      if (((UserlandProcess) cs).getShouldStopAfterContextSwitch()) {
        Output.debugPrint(
            """


                        OS.contextSwitcher.shouldStopAfterContextSwitch is true;
                        setting it to false and stopping the contextSwitcher.
                        This is where a UserlandProcess stops due to timeout.""");
        ((UserlandProcess) cs).setShouldStopAfterContextSwitch(false);
        cs.stop();
      } else if (((UserlandProcess) cs).getShouldStopAfterContextSwitch() == null) {
        // TODO: This might never happen
        Output.debugPrint(
            "OS.contextSwitcher.shouldStopAfterContextSwitch has not been set yet; continuing...");
      } else {
        Output.debugPrint(
            "OS.contextSwitcher.shouldStopAfterContextSwitch is false; continuing...");
      }
    } else {
      Output.debugPrint("OS.contextSwitcher is not a UserlandProcess; continuing...");
    }
  }

  public static void switchContext(
      UnprivilegedContextSwitcher cs,
      CallType callType,
      BiConsumer<UnprivilegedContextSwitcher, Object> retSaver,
      Object... params) {
    Output.debugPrint(Output.DebugOutputType.SYNC_BEFORE_ENTER, cs.toString());
    Output.debugPrint("Call type: " + callType);
    synchronized (cs) {
      // Announce our arrival.
      Output.debugPrint(Output.DebugOutputType.SYNC_ENTER, cs.toString());

      // Store a reference on OS to the Runnable whose thread is calling this method.
      setContextSwitcher(cs);

      // Set the call type for this context switch.
      setCallType(callType);

      // Clear current params; set new ones.
      setParams(params);

      // Start Kernel; stop contextSwitcher.
      startKernel(cs);

      // Save the value returned from the Kernel to the context switcher.
      getRetVal().ifPresent(rv -> cs.setContextSwitchRet(retSaver, rv));
    }
    Output.debugPrint(Output.DebugOutputType.SYNC_LEAVE, cs.toString());

    // The following is the logic that stops the context switcher if needed.
    // We can't have this in the sync block because then the cs would stop while holding the lock.

    Output.debugPrint(
        """


                        Checking if we should stop the contextSwitcher due to
                        being a UserlandProcess that is not the new currentlyRunning.
                        Checking shouldStopAfterContextSwitch set in Scheduler.switchContext.""");
    if (cs instanceof UserlandProcess) {
      Output.debugPrint("OS.contextSwitcher is a UserlandProcess");
      ((UserlandProcess) cs).preSetStopRequested(false);
      if (((UserlandProcess) cs).getShouldStopAfterContextSwitch()) {
        Output.debugPrint(
            """


                            OS.contextSwitcher.shouldStopAfterContextSwitch is true;
                            setting it to false and stopping the contextSwitcher.
                            This is where a UserlandProcess stops due to timeout.""");
        ((UserlandProcess) cs).setShouldStopAfterContextSwitch(false);
        cs.stop();
      } else if (((UserlandProcess) cs).getShouldStopAfterContextSwitch() == null) {
        // TODO: This might never happen
        Output.debugPrint(
            "OS.contextSwitcher.shouldStopAfterContextSwitch has not been set yet; continuing...");
      } else {
        Output.debugPrint(
            "OS.contextSwitcher.shouldStopAfterContextSwitch is false; continuing...");
      }
    } else {
      Output.debugPrint("OS.contextSwitcher is not a UserlandProcess; continuing...");
    }
  }

  private static void setParams(Object... newParams) {
    // The params list will be empty (size = 0) after this call returns.
    params.clear();

    // Throw an exception if any of the new params are null and log the exception.
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
    // Throw an exception if the param index is out of range and log the exception.
    try {
      if (idx < 0 || idx >= params.size()) {
        throw new RuntimeException(Output.getErrorString("Param index " + idx + " out of range."));
      }
    } catch (RuntimeException e) {
      Output.writeToFile(e.toString());
      throw e;
    }

    // Get the param.
    Object param = params.get(idx);

    // Throw an exception if the param is null and log the exception.
    try {
      Objects.requireNonNull(
          param, Output.getErrorString("Tried to get param at index " + idx + " but it was null."));
    } catch (NullPointerException e) {
      Output.writeToFile(e.toString());
      throw e;
    }

    // Return the param.
    return param;
  }

  /**
   * Data field X is guarded by the semaphore whose parking space is reserved for the thread that
   * wants to reliably "see" an update made to X.
   *
   * <p>As such, retVal is guarded by the UnprivilegedContextSwitcher's semaphore (and does not need
   * to be declared synchronized).
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

  /**
   * Only called by Kernel thread. Set to null if the kernelland method has a void return type.
   *
   * @param rv The kernelland function return value.
   */
  public static void setRetValOnOS(Object rv) {
    Output.debugPrint("Setting OS.retVal to " + rv);
    retVal = rv;
  }

  public static UnprivilegedContextSwitcher getContextSwitcher() {
    try {
      Objects.requireNonNull(
          contextSwitcher,
          Output.getErrorString("Tried to get OS.contextSwitcher but it was null"));
    } catch (NullPointerException e) {
      Output.writeToFile(e.toString());
      throw e;
    }
    Output.debugPrint("OS.contextSwitcher is " + contextSwitcher.getThreadName());
    return contextSwitcher;
  }

  public static void setContextSwitcher(UnprivilegedContextSwitcher cs) {
    try {
      Objects.requireNonNull(cs, Output.getErrorString("Cannot set OS.contextSwitcher to null"));
    } catch (NullPointerException e) {
      Output.writeToFile(e.toString());
      throw e;
    }
    Output.debugPrint("Setting OS.contextSwitcher to " + cs.getThreadName());
    contextSwitcher = cs;
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

  public static void switchProcess(UnprivilegedContextSwitcher ucs) {
    switchContext(ucs, CallType.SWITCH_PROCESS);
  }

  public enum CallType {
    STARTUP_CREATE_PROCESS,
    CREATE_PROCESS,
    SWITCH_PROCESS,
    SLEEP,
    WAIT_FOR_MESSAGE,
    SEND_MESSAGE
  }
}
