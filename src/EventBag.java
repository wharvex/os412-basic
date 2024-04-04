public class EventBag {
  private final String threadName;
  private final String className;
  private final String methodName;
  private final String description;
  private EventBag nextEvent;

  public EventBag(String threadName, String className, String methodName, String description) {
    this.threadName = threadName;
    this.className = className;
    this.methodName = methodName;
    this.description = description;
  }

  public EventBag getNextEvent() {
    return nextEvent;
  }

  public void setNextEvent(EventBag nextEvent) {
    this.nextEvent = nextEvent;
  }

  public String getThreadName() {
    return threadName;
  }

  public String getClassName() {
    return className;
  }

  public String getMethodName() {
    return methodName;
  }

  public String getDescription() {
    return description;
  }
}
