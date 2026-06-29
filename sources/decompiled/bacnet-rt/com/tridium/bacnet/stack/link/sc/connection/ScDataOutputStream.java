package com.tridium.bacnet.stack.link.sc.connection;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;

public final class ScDataOutputStream extends DataOutputStream {
   public ScDataOutputStream(ByteArrayOutputStream out) {
      super(out);
   }

   public ByteArrayOutputStream getUnderlyingStream() {
      return (ByteArrayOutputStream)this.out;
   }

   public void reset() {
      ((ByteArrayOutputStream)this.out).reset();
      this.written = 0;
   }
}
