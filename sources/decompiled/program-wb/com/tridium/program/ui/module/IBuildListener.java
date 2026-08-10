package com.tridium.program.ui.module;

import javax.baja.ui.BWidget;

public interface IBuildListener {
   void setNumBuildSteps(int var1);

   void nextStep(String var1);

   void updateDesc(String var1);

   BWidget getOwner();
}
