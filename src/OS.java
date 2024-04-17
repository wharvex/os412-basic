import java.util.*;
import java.util.function.BiConsumer;

/**
 * OSLAND
 *
 * <p>Models the gateway between userland and kernelland.
 */
public class OS {
  public static final int EXISTING_SECONDARY_DEVICES = 2;
  public static final int DEVICE_CONTENTS_SIZE = 10;
  private static final int PAGE_SIZE = 1024;
  private static final int MEMORY_MAP_SIZE = 100;
  private static final int FREE_SPACE_SIZE = 1000;
  private static final List<Object> PARAMS = new ArrayList<>();
  private static final int TLB_SIZE = 2;
  private static Kernel kernel;
  private static UnprivilegedContextSwitcher contextSwitcher;
  private static Object retVal;
  private static CallType callType;
  private static List<KernelMessage> messages = new ArrayList<>();

  public static int getPageSize() {
    return PAGE_SIZE;
  }

  public static int getMemoryMapSize() {
    return MEMORY_MAP_SIZE;
  }

  public static int getFreeSpaceSize() {
    return FREE_SPACE_SIZE;
  }

  public static int getPhysicalMemorySize() {
    return getPageSize() * getPageSize();
  }

  public static int getTlbSize() {
    return TLB_SIZE;
  }

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
    OutputHelper.debugPrint(OutputHelper.DebugOutputType.SYNC_BEFORE_ENTER, cs.toString());
    OutputHelper.debugPrint("Call type: " + callType);
    synchronized (cs) {
      // Announce our arrival.
      OutputHelper.debugPrint(OutputHelper.DebugOutputType.SYNC_ENTER, cs.toString());

      // Store a reference on OS to the Runnable whose thread is calling this method.
      setContextSwitcher(cs);

      // Set the call type for this context switch.
      setCallType(callType);

      // Clear current params; set new ones.
      setParams(params);

      // Start Kernel; stop contextSwitcher.
      startKernel(cs);

      // Save the value returned from the Kernel to the context switcher.
      getRetVal().ifPresent(cs::setContextSwitchRet);
    }
    OutputHelper.debugPrint(OutputHelper.DebugOutputType.SYNC_LEAVE, cs.toString());

    // The following is the logic that stops the context switcher if needed.
    // We can't have this in the sync block because then the cs would stop while holding the lock.

    OutputHelper.debugPrint(
        """


                    Checking if we should stop the contextSwitcher due to
                    being a UserlandProcess that is not the new currentlyRunning.
                    Checking shouldStopAfterContextSwitch set in Scheduler.switchContext.""");
    if (cs instanceof UserlandProcess) {
      OutputHelper.debugPrint("OS.contextSwitcher is a UserlandProcess");
      ((UserlandProcess) cs).preSetStopRequested(false);
      if (((UserlandProcess) cs).getShouldStopAfterContextSwitch()) {
        OutputHelper.debugPrint(
            """


                        OS.contextSwitcher.shouldStopAfterContextSwitch is true;
                        setting it to false and stopping the contextSwitcher.
                        This is where a UserlandProcess stops due to timeout.""");
        ((UserlandProcess) cs).setShouldStopAfterContextSwitch(false);
        cs.stop();
      } else if (((UserlandProcess) cs).getShouldStopAfterContextSwitch() == null) {
        // TODO: This might never happen
        OutputHelper.debugPrint(
            "OS.contextSwitcher.shouldStopAfterContextSwitch has not been set yet; continuing...");
      } else {
        OutputHelper.debugPrint(
            "OS.contextSwitcher.shouldStopAfterContextSwitch is false; continuing...");
      }
    } else {
      OutputHelper.debugPrint("OS.contextSwitcher is not a UserlandProcess; continuing...");
    }
  }

  public static void switchContext(
      UnprivilegedContextSwitcher cs,
      CallType callType,
      BiConsumer<UnprivilegedContextSwitcher, Object> retSaver,
      Object... params) {
    OutputHelper.debugPrint(OutputHelper.DebugOutputType.SYNC_BEFORE_ENTER, cs.toString());
    OutputHelper.debugPrint("Call type: " + callType);
    synchronized (cs) {
      // Announce our arrival.
      OutputHelper.debugPrint(OutputHelper.DebugOutputType.SYNC_ENTER, cs.toString());

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
    OutputHelper.debugPrint(OutputHelper.DebugOutputType.SYNC_LEAVE, cs.toString());

    // The following is the logic that stops the context switcher if needed.
    // We can't have this in the sync block because then the cs would stop while holding the lock.

    OutputHelper.debugPrint(
        """


                        Checking if we should stop the contextSwitcher due to
                        being a UserlandProcess that is not the new currentlyRunning.
                        Checking shouldStopAfterContextSwitch set in Scheduler.switchContext.""");
    if (cs instanceof UserlandProcess) {
      OutputHelper.debugPrint("OS.contextSwitcher is a UserlandProcess");
      ((UserlandProcess) cs).preSetStopRequested(false);
      if (((UserlandProcess) cs).getShouldStopAfterContextSwitch()) {
        OutputHelper.debugPrint(
            """


                            OS.contextSwitcher.shouldStopAfterContextSwitch is true;
                            setting it to false and stopping the contextSwitcher.
                            This is where a UserlandProcess stops due to timeout.""");
        ((UserlandProcess) cs).setShouldStopAfterContextSwitch(false);
        cs.stop();
      } else if (((UserlandProcess) cs).getShouldStopAfterContextSwitch() == null) {
        // TODO: This might never happen
        OutputHelper.debugPrint(
            "OS.contextSwitcher.shouldStopAfterContextSwitch has not been set yet; continuing...");
      } else {
        OutputHelper.debugPrint(
            "OS.contextSwitcher.shouldStopAfterContextSwitch is false; continuing...");
      }
    } else {
      OutputHelper.debugPrint("OS.contextSwitcher is not a UserlandProcess; continuing...");
    }
  }

  private static void setParams(Object... newParams) {
    // The params list will be empty (size = 0) after this call returns.
    PARAMS.clear();

    // Throw an exception if any of the new params are null and log the exception.
    try {
      if (Arrays.stream(newParams).anyMatch(Objects::isNull)) {
        throw new RuntimeException(
            OutputHelper.getErrorStringThrow("Cannot add any null elements to params."));
      }
    } catch (RuntimeException e) {
      OutputHelper.writeToFile(e.toString());
      throw e;
    }

    // Add new params to params.
    PARAMS.addAll(List.of(newParams));
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
      if (idx < 0 || idx >= PARAMS.size()) {
        throw new RuntimeException(
            OutputHelper.getErrorStringThrow("Param index " + idx + " out of range."));
      }
    } catch (RuntimeException e) {
      OutputHelper.writeToFile(e.toString());
      throw e;
    }

    // Get the param.
    Object param = PARAMS.get(idx);

    // Throw an exception if the param is null and log the exception.
    try {
      Objects.requireNonNull(
          param,
          OutputHelper.getErrorStringThrow(
              "Tried to get param at index " + idx + " but it was null."));
    } catch (NullPointerException e) {
      OutputHelper.writeToFile(e.toString());
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
    OutputHelper.debugPrint("Setting OS.retVal to " + rv);
    retVal = rv;
  }

  public static UnprivilegedContextSwitcher getContextSwitcher() {
    try {
      Objects.requireNonNull(
          contextSwitcher,
          OutputHelper.getErrorStringThrow("Tried to get OS.contextSwitcher but it was null"));
    } catch (NullPointerException e) {
      OutputHelper.writeToFile(e.toString());
      throw e;
    }
    OutputHelper.debugPrint("OS.contextSwitcher is " + contextSwitcher.getThreadName());
    return contextSwitcher;
  }

  public static void setContextSwitcher(UnprivilegedContextSwitcher cs) {
    try {
      Objects.requireNonNull(
          cs, OutputHelper.getErrorStringThrow("Cannot set OS.contextSwitcher to null"));
    } catch (NullPointerException e) {
      OutputHelper.writeToFile(e.toString());
      throw e;
    }
    OutputHelper.debugPrint("Setting OS.contextSwitcher to " + cs.getThreadName());
    contextSwitcher = cs;
  }

  public static CallType getCallType() {
    try {
      Objects.requireNonNull(
          callType, OutputHelper.getErrorStringThrow("Tried to get OS.callType but it was null"));
    } catch (NullPointerException e) {
      OutputHelper.writeToFile(e.toString());
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
      Objects.requireNonNull(
          ct, OutputHelper.getErrorStringThrow("Cannot set OS.callType to null."));
    } catch (NullPointerException e) {
      OutputHelper.writeToFile(e.toString());
      throw e;
    }
    callType = ct;
  }

  public static void switchProcess(UnprivilegedContextSwitcher ucs) {
    switchContext(ucs, CallType.SWITCH_PROCESS);
  }

  public static void allocateMemory(
      UnprivilegedContextSwitcher cs,
      BiConsumer<UnprivilegedContextSwitcher, Object> retSaver,
      int size) {
    switchContext(cs, CallType.ALLOCATE_MEMORY, retSaver, size);
  }

  public static void getMapping(
      UnprivilegedContextSwitcher cs,
      BiConsumer<UnprivilegedContextSwitcher, Object> retSaver,
      int virtualPageNumber) {
    switchContext(cs, CallType.GET_MAPPING, retSaver, virtualPageNumber);
  }

  public static void freeMemory(
      UnprivilegedContextSwitcher cs,
      BiConsumer<UnprivilegedContextSwitcher, Object> retSaver,
      int pointer,
      int size) {
    switchContext(cs, CallType.FREE_MEMORY, retSaver, pointer, size);
  }

  public static List<KernelMessage> getMessages() {
    return messages;
  }

  /**
   * This is only set for a process when that process is chosen to run next, so there shouldn't be a
   * danger of the wrong process getting them.
   *
   * @param messages
   */
  public static void setMessages(List<KernelMessage> messages) {
    OutputHelper.debugPrint("Setting OS messages to " + messages);
    OS.messages = messages;
  }

  public static List<KernelMessage> getMessagesAndClear() {
    var ret = getMessages();
    setMessages(new ArrayList<>());
    return ret;
  }

  public enum CallType {
    STARTUP_CREATE_PROCESS,
    CREATE_PROCESS,
    SWITCH_PROCESS,
    SLEEP,
    OPEN,
    WAIT_FOR_MESSAGE,
    SEND_MESSAGE,
    GET_MAPPING,
    ALLOCATE_MEMORY,
    FREE_MEMORY,
    EXIT
  }
}
