package com.tridium.bacnet.timers;

import javax.baja.sys.BAbsTime;

public class StopWatch {
   private BAbsTime[] t;
   private String[] s;
   private int ndx = 0;
   private static StopWatch sw = new StopWatch(30);

   private StopWatch(int size) {
      this.t = new BAbsTime[size];
      this.s = new String[size];
   }

   public static void time() {
      sw.t[sw.ndx] = BAbsTime.make();
      sw.s[sw.ndx] = "?";
      sw.ndx++;
   }

   public static void time(String desc) {
      sw.t[sw.ndx] = BAbsTime.make();
      sw.s[sw.ndx] = desc;
      sw.ndx++;
   }

   public static void clear() {
      for (int i = 0; i < sw.ndx; i++) {
         sw.t[i] = null;
      }

      sw.ndx = 0;
   }

   public static void show() {
      for (int i = 0; i < sw.ndx; i++) {
         System.out.println("t[" + i + "]:" + sw.t[i] + " ==> " + sw.s[i]);
      }
   }
}
