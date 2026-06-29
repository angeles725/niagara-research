package com.tridium.fox.sys.spy;

import javax.baja.spy.Spy;
import javax.baja.spy.SpyWriter;

public class FoxSpy extends Spy {
   String title;
   String content;

   public FoxSpy(String t, String c) {
      this.title = t;
      this.content = c;
   }

   public String getTitle() {
      return this.title;
   }

   public void write(SpyWriter out) {
      out.write(this.content);
   }
}
