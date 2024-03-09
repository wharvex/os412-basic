import java.util.*;

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
  }

  public PCB getCurrentlyRunningSafe() {
    try {
      return preGetCurrentlyRunning()
          .orElseThrow(
              () ->
                  new RuntimeException(
                      Output.getErrorString("Expected Scheduler.currentlyRunning to not be null")));
    } catch (RuntimeException e) {
      Output.writeToFile(e.toString());
      throw e;
    }
  }

  public void switchProcess() {
    PCB oldCurRun = getCurrentlyRunningSafe();
    PCB chosenProcess = wqGetRand();
    Output.debugPrint(
        """
                    If the chosen process doesn't equal the old curRun,
                    the current switch-process operation should result in
                    the old curRun stopping and being added to the wq""");
    if (chosenProcess != oldCurRun) {
      Output.debugPrint(
          """
                      The chosen process doesn't equal the old curRun;
                      setting shouldStopFromSwitch to true on old curRun
                      and adding it to the wq...""");
      oldCurRun.getUserlandProcess().setShouldStopFromSwitch(true);
      wqAdd(oldCurRun);
    } else {
      Output.debugPrint(
          "The chosen process does equal the old curRun; not adding or stopping;\n"
              + "setting old CR's UP's shouldStopFromSwitch to false...");
      oldCurRun.getUserlandProcess().setShouldStopFromSwitch(false);
    }
    preSetCurrentlyRunning(chosenProcess);
  }

  public synchronized Optional<PCB> getCurrentlyRunning() {
    Output.debugPrint(
        "Scheduler.currentlyRunning is "
            + (currentlyRunning != null ? currentlyRunning.getThreadName() : "null"));
    return Optional.ofNullable(currentlyRunning);
  }

  public synchronized void setCurrentlyRunning(PCB currentlyRunning) {
    Output.debugPrint(
        "Setting Scheduler.currentlyRunning to "
            + (currentlyRunning != null ? currentlyRunning.getThreadName() : "null"));
    this.currentlyRunning = currentlyRunning;
  }

  public Optional<PCB> preGetCurrentlyRunning() {
    Output.debugPrint("About to enter Scheduler.getCurrentlyRunning");
    return getCurrentlyRunning();
  }

  public void preSetCurrentlyRunning(PCB currentlyRunning) {
    Output.debugPrint("About to enter Scheduler.setCurrentlyRunning");
    setCurrentlyRunning(currentlyRunning);
  }

  public void wqAdd(PCB pcb) {
    Output.debugPrint("Adding " + pcb.getThreadName() + " to wq");
    wqGet().add(pcb);
  }

  private void wqRemove(int idx) {
    PCB removed = wqGet().remove(idx);
    Output.debugPrint("Removed " + removed.getThreadName() + " from wq");
  }

  private List<PCB> wqGet() {
    return wqBackground;
  }

  private PCB wqGetFrom(int idx) {
    return wqGet().get(idx);
  }

  public PCB wqGetRand() {
    Random r = new Random();
    int chosenIdx = r.nextInt(wqGet().size());
    PCB chosenProcess = wqGetFrom(chosenIdx);
    Output.debugPrint("The chosen process to switch to is " + chosenProcess.getThreadName());
    wqRemove(chosenIdx);
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
      Output.writeToFile(e.toString());
      throw e;
    }
  }

  public void startTimer() {
    Output.debugPrint("Scheduling Timer...");
    timer.schedule(
        new TimerTask() {
          @Override
          public void run() {
            Output.debugPrint("Starting");
            preGetCurrentlyRunning()
                .ifPresentOrElse(
                    PCB::stop,
                    () -> {
                      Output.debugPrint(
                          "Bootloader is " + ThreadHelper.getThreadStateString("bootloaderThread"));
                      Output.debugPrint(
                          "Main is " + ThreadHelper.getThreadStateString("mainThread"));
                      Output.debugPrint(
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
  }

  public enum PriorityType {
    REALTIME,
    INTERACTIVE,
    BACKGROUND
  }
}
