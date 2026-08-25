package com.tridium.niagarad.log;

import com.tridium.niagarad.NiagaraDaemon;
import java.nio.charset.StandardCharsets;
import java.util.logging.Handler;
import java.util.logging.LogRecord;

public class LogBufferHandler extends Handler {
   @Override
   public void publish(LogRecord record) {
      byte[] messageBytes = NiagaraDaemonFormatter.getInstance().format(record).getBytes(StandardCharsets.UTF_8);
      NiagaraDaemon.niagaraDaemonOutputBuffer.writeBuffer(messageBytes, messageBytes.length);
   }

   @Override
   public void flush() {
   }

   @Override
   public void close() throws SecurityException {
   }
}
