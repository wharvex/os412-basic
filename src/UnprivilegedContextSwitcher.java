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

  Object csRetsGet(int idx);

  /**
   * This is called in {@link OS#switchContext(UnprivilegedContextSwitcher, OS.CallType, Object...)
   * OS.switchContext} which is synchronized.
   *
   * @param ret
   */
  default void setContextSwitchRet(Object ret) {
    if (ret != null) {
      csRetsAdd(ret);
    }
  }

  @Override
  default boolean isStopped() {
    return Stoppable.super.isStopped();
  }

  void csRetsAdd(Object ret);

  default boolean isDone() {
    return !getThread().isAlive();
  }
}
