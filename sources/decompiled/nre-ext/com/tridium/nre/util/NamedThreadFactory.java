package com.tridium.nre.util;

import java.util.Objects;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

public class NamedThreadFactory implements ThreadFactory {
   protected final AtomicLong index = new AtomicLong(0L);
   protected final String namePrefix;
   protected final Consumer<Thread> threadInitializer;

   public NamedThreadFactory(String namePrefix) {
      this(namePrefix, null);
   }

   public NamedThreadFactory(String namePrefix, boolean isDaemon) {
      this(namePrefix, thread -> thread.setDaemon(isDaemon));
   }

   public NamedThreadFactory(String namePrefix, Consumer<Thread> threadInitializer) {
      Objects.requireNonNull(namePrefix);
      this.namePrefix = namePrefix;
      this.threadInitializer = threadInitializer == null ? thread -> {} : threadInitializer;
   }

   @Override
   public Thread newThread(Runnable r) {
      Objects.requireNonNull(r);
      Thread result = new Thread(r, this.namePrefix + "-" + this.index.getAndIncrement());
      this.threadInitializer.accept(result);
      return result;
   }
}
