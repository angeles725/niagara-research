package com.tridium.nre.util;

import java.security.AccessControlContext;
import java.util.Objects;
import java.util.function.Consumer;

public class PrivilegedNamedThreadFactory extends NamedThreadFactory {
   private final ThreadGroup group;
   private final AccessControlContext context;

   public PrivilegedNamedThreadFactory(String prefix, ThreadGroup group, AccessControlContext context) {
      this(prefix, group, context, null);
   }

   public PrivilegedNamedThreadFactory(String prefix, ThreadGroup group, AccessControlContext context, Consumer<Thread> threadInitializer) {
      super(prefix, threadInitializer);
      this.group = group;
      this.context = context;
   }

   @Override
   public Thread newThread(Runnable r) {
      Objects.requireNonNull(r);
      Thread result = new Thread(this.group, new PrivilegedRunnable(r, this.context), this.namePrefix + "-" + this.index.getAndIncrement(), 0L);
      this.threadInitializer.accept(result);
      return result;
   }
}
