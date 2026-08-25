package com.tridium.nre.protocol.program;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;

public class ProgramConnection extends URLConnection {
   public ProgramConnection(URL u) {
      super(u);
   }

   @Override
   public void connect() throws IOException {
      throw new UnsupportedOperationException("connect() not supported");
   }

   @Override
   public Object getContent() throws IOException {
      throw new UnsupportedOperationException("getContent() not supported");
   }

   @Override
   public InputStream getInputStream() throws IOException {
      throw new UnsupportedOperationException("getInputStream() not supported");
   }
}
