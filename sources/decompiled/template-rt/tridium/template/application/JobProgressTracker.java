package com.tridium.template.application;

import javax.baja.job.BJob;

public class JobProgressTracker extends ProgressTracker {
   private final BJob job;

   public JobProgressTracker(BJob job) {
      this.job = job;
   }

   @Override
   public void message(String lexKey, String... args) {
      super.message(lexKey, args);
      this.job.log().message("template", lexKey, args);
   }

   @Override
   public void heartbeat() {
      super.heartbeat();
      this.job.heartbeat();
   }

   @Override
   public void endFailed(Throwable exception) {
      this.job.log().endFailed(exception);
   }

   @Override
   protected void progress(int percent) {
      super.progress(percent);
      this.job.progress(this.get());
   }
}
