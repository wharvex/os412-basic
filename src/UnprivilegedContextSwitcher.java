import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * MAINLAND
 *
 * <p>An Unprivileged Context Switcher is an entity that can perform a context switch, but is not
 * allowed to execute in "privileged mode" vis-a-vis the CPU. Which is to say, it can perform a
 * context switch, but is not the Kernel.
 *
 * <p>This would be considered part of Userland were it not for the fact that we will consider the
 * Bootloader a UCS, which I did not think of as part of Userland.
 */
public interface UnprivilegedContextSwitcher extends Stoppable {

  List<Object> getCsRets();

  List<KernelMessage> getMessages();

  default Object getFromCsRets(int idx) {
    return getCsRets().get(idx);
  }

  default void addToCsRets(Object ret) {
    getCsRets().add(ret);
    OutputHelper.debugPrint("Saved " + ret + " to rets of " + getThreadName());
  }

  /**
   * This is called at the end of the synchronized block (still in the block) in {@link
   * OS#switchContext(UnprivilegedContextSwitcher, OS.CallType, Object...) OS.switchContext} to
   * ensure the UCS receives and saves its return value before a new context switch occurs.
   *
   * @param ret
   */
  default void setContextSwitchRet(Object ret) {
    addToCsRets(ret);
  }

  default void setContextSwitchRet(
      BiConsumer<UnprivilegedContextSwitcher, Object> retSaver, Object ret) {
    retSaver.accept(this, ret);
  }

  default void setContextSwitchRet(Consumer<Object> retSaver, Object ret) {
    retSaver.accept(ret);
  }

  @Override
  default boolean isStopped() {
    // We can add UCS-specific stuff here.
    return Stoppable.super.isStopped();
  }

  default boolean isDone() {
    return !getThread().isAlive();
  }
}
