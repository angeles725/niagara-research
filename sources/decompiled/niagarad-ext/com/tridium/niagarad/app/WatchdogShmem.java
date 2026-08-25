package com.tridium.niagarad.app;

public class WatchdogShmem {
   public static final int POLICY_UNDEFINED = -1;
   public static final int POLICY_LOG_ONLY = 1;
   public static final int POLICY_TERMINATE = 2;
   public static final int POLICY_REBOOT = 3;
   public int engineCycles = 0;
   public int policy = 0;
   public int timeout = 0;
}
