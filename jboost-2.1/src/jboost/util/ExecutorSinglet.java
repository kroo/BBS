package jboost.util;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * ExecutorSinglet is an accessor class for the Executor singlet
 * 
 * @author Peter Kharchenko
 */
public class ExecutorSinglet {

  static Executor e = Executors.newFixedThreadPool(1);

  public static Executor getExecutor() {
    return e;
  }

  public synchronized static void setExecutor(Executor ex) {
    e = ex;
  }

}
