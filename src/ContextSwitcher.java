public interface ContextSwitcher extends Stoppable {

  Object csRetsGet(int idx);

  /**
   * This is called in {@link OS#switchContext(ContextSwitcher, OS.CallType, Object...)
   * OS.switchContext} which is synchronized.
   *
   * @param ret
   */
  default void setContextSwitchRet(Object ret) {
    if (ret != null) {
      csRetsAdd(ret);
    }
  }

  void csRetsAdd(Object ret);
}
