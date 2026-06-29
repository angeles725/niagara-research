package com.tridium.lonworks.util;

public class RunnableCommand implements Runnable {
   public Object arg1 = null;
   public Object arg2 = null;

   public RunnableCommand(Object arg1) {
      this.arg1 = arg1;
   }

   public RunnableCommand(Object arg1, Object arg2) {
      this.arg1 = arg1;
      this.arg2 = arg2;
   }

   @Override
   public boolean equals(Object o) {
      if (!(o instanceof RunnableCommand)) {
         return false;
      } else {
         RunnableCommand comp = (RunnableCommand)o;
         return this.arg1.equals(comp.arg1) && this.arg2.equals(comp.arg2);
      }
   }

   @Override
   public int hashCode() {
      return this.arg1.hashCode() ^ this.arg2.hashCode();
   }

   @Override
   public void run() {
   }
}
