package jboost.util;

import java.util.concurrent.CountDownLatch;

/**
 * BaseCountWorker is a base class for workers that decrement
 * <code>CountDown</code> counters.
 * 
 * @author Peter Kharchenko
 */
public abstract class BaseCountWorker implements Runnable {

  CountDownLatch count;

  public BaseCountWorker(CountDownLatch count) {
    this.count = count;
  }

  protected abstract void doWork();

  public void run() {
    try {
      doWork();
    }
    finally {
      count.countDown();
    }

  }
}
