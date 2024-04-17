import java.util.*;
import java.util.function.Supplier;

/**
 * KERNELLAND
 *
 * <p>Models an operating system process scheduler.
 */
public class Scheduler {
  // The priority-appropriate waiting queues.
  private final ArrayList<PCB> wqRealtime;
  private final ArrayList<PCB> wqInteractive;
  private final ArrayList<PCB> wqBackground;
  private final ArrayList<KernelMessage> waitingMessages;
  private final ArrayList<PCB> waitingRecipients;

  // The sleeping queue.
  private final ArrayList<PCB> sleepingQueue;

  // The hashmap for looking up a PCB by its PID that contains all living PCBs.
  private final HashMap<Integer, PCB> pcbByPidComplete;

  // The hashmap for looking up a PCB by its PID that only contains PCBs waiting for a message.
  private final HashMap<Integer, PCB> pcbByPidMessageWaiters;

  // The timer, which simulates the hardware-based timer that interrupts the CPU and makes switching
  // between processes possible.
  private final Timer timer;

  // The currently running process.
  private PCB currentlyRunning;

  public Scheduler() {
    timer = new Timer("timerThread");
    wqRealtime = new ArrayList<>();
    wqInteractive = new ArrayList<>();
    wqBackground = new ArrayList<>();
    sleepingQueue = new ArrayList<>();
    pcbByPidComplete = new HashMap<>();
    pcbByPidMessageWaiters = new HashMap<>();
    waitingMessages = new ArrayList<>();
    waitingRecipients = new ArrayList<>();
  }

  public void populateTlbRand() {
    // Get random integers for zeroth and first virtual-to-physical mappings in the TLB.
    int vz = RandomHelper.getVirtPageNum();
    int pz = RandomHelper.getPhysPageNum();
    int vf = RandomHelper.getVirtPageNum();
    while (vf == vz) {
      vf = RandomHelper.getVirtPageNum();
    }
    int pf = RandomHelper.getPhysPageNum();
    while (pf == pz) {
      pf = RandomHelper.getPhysPageNum();
    }

    populateTlb(vz, pz, vf, pf);
  }

  public void populateTlb(int vz, int pz, int vf, int pf) {
    // Set zeroth mapping.
    UserlandProcess.preSetOnTlb(0, 0, vz);
    UserlandProcess.preSetOnTlb(1, 0, pz);

    // Set first mapping.
    UserlandProcess.preSetOnTlb(0, 1, vf);
    UserlandProcess.preSetOnTlb(1, 1, pf);
  }

  public PCB getCurrentlyRunningSafe() {
    try {
      return preGetCurrentlyRunning()
          .orElseThrow(
              () ->
                  new RuntimeException(
                      OutputHelper.getErrorStringThrow(
                          "Expected Scheduler.currentlyRunning to not be null")));
    } catch (RuntimeException e) {
      OutputHelper.writeToFile(e.toString());
      throw e;
    }
  }

  private List<KernelMessage> getWaitingMessagesForPCB(PCB pcb) {
    return getWaitingMessages().stream().filter(km -> km.getTargetPid() == pcb.getPid()).toList();
  }

  private void handleMessages() {
    // Get the message-waiters who have messages now.
    var doneWaiters =
        getWaitingRecipients().stream()
            .filter(pcb -> !getWaitingMessagesForPCB(pcb).isEmpty())
            .toList();
    OutputHelper.debugPrint("Initial contents of doneWaiters: " + doneWaiters);

    // Remove from waitingRecipients the message-waiters who have messages now.
    getWaitingRecipients().removeAll(doneWaiters);
    OutputHelper.debugPrint(
        "Contents of waitingRecipients after removing doneWaiters: " + getWaitingRecipients());

    // Add each doneWaiter's messages to its PCB.
    doneWaiters =
        doneWaiters.stream()
            .map(pcb -> pcb.addAllToMessagesAndReturnThis(getWaitingMessagesForPCB(pcb)))
            .toList();
    OutputHelper.debugPrint("Contents of donewaiters after adding messages to them:");
    doneWaiters.forEach(dw -> OutputHelper.debugPrint(dw + " -- " + dw.getMessages()));

    // Add all the doneWaiters to the waiting (readyToRun) queue.
    getWQ().addAll(doneWaiters);

    // Remove from waitingMessages all the messages that were waiting for the now doneWaiters.
    doneWaiters.forEach(dw -> getWaitingMessages().removeAll(getWaitingMessagesForPCB(dw)));
    OutputHelper.debugPrint(
        "Contents of waitingMessages after removing"
            + " the messages that were waiting for the doneWaiters: "
            + getWaitingMessages());
  }

  public void switchProcess(Supplier<PCB> processChooser) {
    handleMessages();

    // Add CR to WQ if CR is not null.
    // Pro-tip: Set CR to null right before calling switchProcess if you don't want to give it a
    // chance to run again this context switch. Just be aware that if you have unlucky timing, this
    // will cause the Timer thread to skip a quantum if it happens to fire while CR is null.
    preGetCurrentlyRunning().ifPresent(this::addToWQ);

    // Choose the new process to run.
    PCB chosenProcess = processChooser.get();

    // Save the chosen process' messages to OS.
    OS.setMessages(chosenProcess.getMessages());

    // Reset the PCB's messages.
    chosenProcess.setMessages(new ArrayList<>());

    // Get what will become the old currently running.
    PCB oldCurRun =
        preGetCurrentlyRunning()
            .orElse(getFromPcbByPidComplete(getPidByName(OS.getContextSwitcher().getThreadName())));
    OutputHelper.debugPrint("oldCurRun threadName: " + oldCurRun.getThreadName());

    // Mark oldCurRun for stopping based on whether chosenProcess ref-equals oldCurRun.
    OutputHelper.debugPrint(
        """


                        If the chosen process doesn't equal the old curRun,
                        the old curRun should stop after the context switch""");
    oldCurRun.getUserlandProcess().setShouldStopAfterContextSwitch(chosenProcess != oldCurRun);

    // Set currentlyRunning to the chosen process.
    preSetCurrentlyRunning(chosenProcess);
  }

  public synchronized Optional<PCB> getCurrentlyRunning() {
    OutputHelper.debugPrint(OutputHelper.DebugOutputType.SYNC_ENTER, this.toString());
    OutputHelper.debugPrint(
        "Scheduler.currentlyRunning is "
            + (currentlyRunning != null ? currentlyRunning.getThreadName() : "null"));
    return Optional.ofNullable(currentlyRunning);
  }

  public synchronized void setCurrentlyRunning(PCB currentlyRunning) {
    OutputHelper.debugPrint(OutputHelper.DebugOutputType.SYNC_ENTER, this.toString());
    OutputHelper.debugPrint(
        "Setting Scheduler.currentlyRunning to "
            + (currentlyRunning != null ? currentlyRunning.getThreadName() : "null"));
    this.currentlyRunning = currentlyRunning;
  }

  public Optional<PCB> preGetCurrentlyRunning() {
    OutputHelper.debugPrint(OutputHelper.DebugOutputType.SYNC_BEFORE_ENTER, this.toString());
    var ret = getCurrentlyRunning();
    OutputHelper.debugPrint(OutputHelper.DebugOutputType.SYNC_LEAVE, this.toString());
    return ret;
  }

  public void preSetCurrentlyRunning(PCB currentlyRunning) {
    OutputHelper.debugPrint(OutputHelper.DebugOutputType.SYNC_BEFORE_ENTER, this.toString());
    setCurrentlyRunning(currentlyRunning);
    OutputHelper.debugPrint(OutputHelper.DebugOutputType.SYNC_LEAVE, this.toString());
  }

  public void addToWQ(PCB pcb) {
    getWQ().add(pcb);
    OutputHelper.debugPrint("Added " + pcb.getThreadName() + " to wq");
    OutputHelper.debugPrint("Contents of wq:");
    getWQ().forEach(wqElm -> OutputHelper.debugPrint(wqElm.getThreadName()));
    OutputHelper.debugPrint("Size of wq: " + getWQ().size());
  }

  private void removeFromWQ(int idx) {
    PCB removed = getWQ().remove(idx);
    OutputHelper.debugPrint("Removed " + removed.getThreadName() + " from wq");
    OutputHelper.debugPrint("Contents of wq:");
    getWQ().forEach(wqElm -> OutputHelper.debugPrint(wqElm.getThreadName()));
  }

  private List<PCB> getWQ() {
    return wqBackground;
  }

  private PCB getFromWQ(int idx) {
    return getWQ().get(idx);
  }

  public PCB getRandomProcess() {
    Random r = new Random();
    int chosenIdx = r.nextInt(getWQ().size());
    PCB chosenProcess = getFromWQ(chosenIdx);
    OutputHelper.debugPrint("The chosen process to switch to is " + chosenProcess.getThreadName());
    removeFromWQ(chosenIdx);
    return chosenProcess;
  }

  public int getPidByName(String name) {
    try {
      return getPcbByPidComplete().entrySet().stream()
          .filter(e -> e.getValue().getThreadName().equals(name))
          .findFirst()
          .orElseThrow(() -> new RuntimeException("No such thread name"))
          .getKey();
    } catch (RuntimeException e) {
      OutputHelper.writeToFile(e.toString());
      throw e;
    }
  }

  public int getPid() {
    return getCurrentlyRunningSafe().getPid();
  }

  public void startTimer() {
    OutputHelper.debugPrint("Scheduling Timer...");
    timer.schedule(
        new TimerTask() {
          @Override
          public void run() {
            OutputHelper.debugPrint("Starting");
            preGetCurrentlyRunning()
                .ifPresentOrElse(
                    // TODO: What happens if the currentlyRunning changes here?
                    // OS will set shouldStopFromTimeout on the contextSwitcher (which is the
                    // currentlyRunning we will have gotten here) to false, which will release the
                    // Timer from its waiting loop.
                    PCB::stop,
                    () -> {
                      OutputHelper.debugPrint("Timer found null CR");
                      OutputHelper.debugPrint(
                          "Bootloader is " + ThreadHelper.getThreadStateString("bootloaderThread"));
                      OutputHelper.debugPrint(
                          "Main is " + ThreadHelper.getThreadStateString("mainThread"));
                      OutputHelper.debugPrint(
                          "Kernel is " + ThreadHelper.getThreadStateString("kernelThread"));
                    });
          }
        },
        1000,
        1000);
  }

  public HashMap<Integer, PCB> getPcbByPidComplete() {
    return pcbByPidComplete;
  }

  public PCB getFromPcbByPidComplete(int pid) {
    return getPcbByPidComplete().get(pid);
  }

  public void addToPcbByPidComplete(PCB pcb, int pid) {
    getPcbByPidComplete().put(pid, pcb);
    OutputHelper.debugPrint("Added " + pcb.getThreadName() + " to pcbByPidComplete");
    getPcbByPidComplete()
        .forEach(
            (key, value) ->
                OutputHelper.debugPrint(
                    "Contents of pcbByPidComplete -- Key "
                        + key
                        + "; Value "
                        + value.getThreadName()));
  }

  public ArrayList<KernelMessage> getWaitingMessages() {
    return waitingMessages;
  }

  public ArrayList<PCB> getWaitingRecipients() {
    return waitingRecipients;
  }

  public void addToWaitingMessages(KernelMessage km) {
    OutputHelper.debugPrint("Adding " + km + " to waitingMessages");
    getWaitingMessages().add(km);
    OutputHelper.debugPrint("waitingMessages contents: " + getWaitingMessages());
  }

  public void addToWaitingRecipients(PCB pcb) {
    OutputHelper.debugPrint("Adding " + pcb.getThreadName() + " to waitingRecipients");
    getWaitingRecipients().add(pcb);
    OutputHelper.debugPrint("Contents of waitingRecipients: " + getWaitingRecipients());
  }

  public PCB getFromWaitingRecipients(int idx) {
    return getWaitingRecipients().get(idx);
  }

  public KernelMessage getFromWaitingMessages(int idx) {
    return getWaitingMessages().get(idx);
  }

  public enum PriorityType {
    REALTIME,
    INTERACTIVE,
    BACKGROUND
  }
}
