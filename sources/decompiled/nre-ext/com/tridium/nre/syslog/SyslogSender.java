package com.tridium.nre.syslog;

import java.util.concurrent.BlockingQueue;

public abstract class SyslogSender extends Thread {
   protected final SyslogManager syslogManager;
   protected final BlockingQueue<Message> blockingQueue;
   public static final int FAILURE_TIMEOUT = 5000;

   protected SyslogSender(SyslogManager syslogManager, BlockingQueue<Message> blockingQueue, String threadName) {
      super(threadName);
      this.syslogManager = syslogManager;
      this.blockingQueue = blockingQueue;
   }

   public abstract void shutdown();
}
