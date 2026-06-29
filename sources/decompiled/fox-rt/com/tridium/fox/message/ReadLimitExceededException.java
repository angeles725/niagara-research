package com.tridium.fox.message;

import java.io.IOException;

public class ReadLimitExceededException extends IOException {
   public ReadLimitExceededException(long readLimit, long numBytes, long bytesRead) {
      super("Message exceeds size limit (limit = " + readLimit + ", read size = " + numBytes + ", already read = " + bytesRead + ")");
   }
}
