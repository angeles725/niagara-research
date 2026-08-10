package com.tridium.program.ui.module;

public abstract class BuildStep {
   protected BuildHelper helper;
   protected IBuildListener interest;

   public final void runStep(BuildHelper helper, IBuildListener interest) throws BuildException {
      this.helper = helper;
      this.interest = interest;

      try {
         this.doStep();
      } catch (BuildException var4) {
         throw var4;
      } catch (Exception var5) {
         throw new BuildException("Build failed. " + var5.getMessage(), var5);
      }
   }

   protected abstract void doStep() throws Exception;
}
