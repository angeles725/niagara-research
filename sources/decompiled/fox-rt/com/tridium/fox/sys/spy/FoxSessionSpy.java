package com.tridium.fox.sys.spy;

import com.tridium.fox.session.FoxSession;
import javax.baja.spy.ObjectSpy;
import javax.baja.spy.Spy;
import javax.baja.spy.SpyDir;
import javax.baja.spy.SpyWriter;
import javax.baja.sys.BObject;

public class FoxSessionSpy extends SpyDir {
   private FoxSession session;

   public FoxSessionSpy(FoxSession session) {
      this.session = session;
   }

   public void write(SpyWriter out) throws Exception {
      out.a("connection", "Session Connection Info");
      this.session.spy(out);
   }

   public Spy find(String name) {
      return name.equals("connection") && this.session != null && this.session.conn() instanceof BObject ? new ObjectSpy((BObject)this.session.conn()) : null;
   }
}
