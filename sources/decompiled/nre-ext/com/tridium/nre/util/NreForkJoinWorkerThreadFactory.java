package com.tridium.nre.util;

import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinWorkerThread;
import java.util.concurrent.ForkJoinPool.ForkJoinWorkerThreadFactory;

public class NreForkJoinWorkerThreadFactory implements ForkJoinWorkerThreadFactory {
   public static final ForkJoinWorkerThreadFactory DEFAULT_INSTANCE = new NreForkJoinWorkerThreadFactory();

   @Override
   public ForkJoinWorkerThread newThread(ForkJoinPool pool) {
      return new NreForkJoinWorkerThread(pool);
   }
}
