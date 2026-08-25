package com.tridium.niagarad.log;

public interface ErrorHandler {
   void error(String var1);

   void error(MessageBundle var1);

   MessageBundle getLastError();
}
