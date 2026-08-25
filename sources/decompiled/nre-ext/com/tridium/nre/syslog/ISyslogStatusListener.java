package com.tridium.nre.syslog;

public interface ISyslogStatusListener {
   void onSyslogServerConnectionStatusChanged(boolean var1, String var2);

   void onSyslogMessageQueueChanged(int var1);
}
