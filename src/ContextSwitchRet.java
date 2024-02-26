public class ContextSwitchRet extends AbsContextSwitchRet {
  private final Object retVal;

  public ContextSwitchRet(Object retVal) {
    this.retVal = retVal;
  }

  public Object getRetVal() {
    return retVal;
  }
}
