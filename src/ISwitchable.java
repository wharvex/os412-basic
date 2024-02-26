public interface ISwitchable extends IStoppable {

  AbsContextSwitchRet getContextSwitchRet(int idx);

  void setContextSwitchRet(AbsContextSwitchRet ret);
}
