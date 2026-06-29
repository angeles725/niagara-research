package com.tridium.lonworks.datatypes;

import com.tridium.lonworks.loncomm.NAppBuffer;
import javax.baja.nre.annotations.Facet;
import javax.baja.nre.annotations.NiagaraProperties;
import javax.baja.nre.annotations.NiagaraProperty;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.sys.BAbsTime;
import javax.baja.sys.BBlob;
import javax.baja.sys.BComponent;
import javax.baja.sys.BFacets;
import javax.baja.sys.BIcon;
import javax.baja.sys.Property;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType
@NiagaraProperties({@NiagaraProperty(
      name = "direction",
      type = "boolean",
      defaultValue = "false",
      facets = {@Facet("BFacets.makeBoolean(\"recv\",\"send\")")}
   ), @NiagaraProperty(
      name = "timeStamp",
      type = "BAbsTime",
      defaultValue = "BAbsTime.DEFAULT"
   ), @NiagaraProperty(
      name = "msg",
      type = "BBlob",
      defaultValue = "BBlob.DEFAULT"
   )})
public class BLinkFilterEntry extends BComponent {
   public static final Property direction = newProperty(0, false, BFacets.makeBoolean("recv", "send"));
   public static final Property timeStamp = newProperty(0, BAbsTime.DEFAULT, null);
   public static final Property msg = newProperty(0, BBlob.DEFAULT, null);
   public static final Type TYPE = Sys.loadType(BLinkFilterEntry.class);
   byte[] a = null;
   NAppBuffer buf = null;
   public static BIcon icon = BIcon.std("magnifyingGlass.png");

   public BLinkFilterEntry() {
   }

   public BLinkFilterEntry(boolean dir, BAbsTime ts, BBlob msg) {
      this.setDirection(dir);
      this.setTimeStamp(ts);
      this.setMsg(msg);
   }

   public boolean getDirection() {
      return this.getBoolean(direction);
   }

   public void setDirection(boolean v) {
      this.setBoolean(direction, v, null);
   }

   public BAbsTime getTimeStamp() {
      return (BAbsTime)this.get(timeStamp);
   }

   public void setTimeStamp(BAbsTime v) {
      this.set(timeStamp, v, null);
   }

   public BBlob getMsg() {
      return (BBlob)this.get(msg);
   }

   public void setMsg(BBlob v) {
      this.set(msg, v, null);
   }

   public Type getType() {
      return TYPE;
   }

   public byte[] getNetBytes() {
      if (this.a == null) {
         this.a = this.getMsg().copyBytes();
      }

      return this.a;
   }

   public NAppBuffer getAppBuffer() {
      if (this.buf == null) {
         this.buf = NAppBuffer.makeAppBuffer(this.getNetBytes());
      }

      return this.buf;
   }

   public BIcon getIcon() {
      return icon;
   }
}
