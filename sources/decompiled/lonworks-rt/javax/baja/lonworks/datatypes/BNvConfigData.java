package javax.baja.lonworks.datatypes;

import javax.baja.lonworks.enums.BLonNvDirection;
import javax.baja.lonworks.enums.BLonServiceType;
import javax.baja.lonworks.io.LonInputStream;
import javax.baja.lonworks.io.LonOutputStream;
import javax.baja.nre.annotations.Facet;
import javax.baja.nre.annotations.NiagaraProperties;
import javax.baja.nre.annotations.NiagaraProperty;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.sys.BFacets;
import javax.baja.sys.BInteger;
import javax.baja.sys.BStruct;
import javax.baja.sys.Context;
import javax.baja.sys.Property;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType
@NiagaraProperties({@NiagaraProperty(
      name = "priority",
      type = "boolean",
      defaultValue = "false",
      flags = 65
   ), @NiagaraProperty(
      name = "direction",
      type = "BLonNvDirection",
      defaultValue = "BLonNvDirection.input",
      flags = 1
   ), @NiagaraProperty(
      name = "selector",
      type = "int",
      defaultValue = "-1",
      flags = 65,
      facets = {@Facet("BFacets.make(BFacets.RADIX,BInteger.make(16))")}
   ), @NiagaraProperty(
      name = "turnAround",
      type = "boolean",
      defaultValue = "false",
      flags = 65
   ), @NiagaraProperty(
      name = "serviceType",
      type = "BLonServiceType",
      defaultValue = "BLonServiceType.unacked",
      flags = 1
   ), @NiagaraProperty(
      name = "authenticated",
      type = "boolean",
      defaultValue = "false",
      flags = 65
   ), @NiagaraProperty(
      name = "addrIndex",
      type = "int",
      defaultValue = "DEFAULT_ADDR_INDEX",
      flags = 65
   )})
public class BNvConfigData extends BStruct {
   public static final int DEFAULT_ADDR_INDEX = -1;
   public static final Property priority = newProperty(65, false, null);
   public static final Property direction = newProperty(1, BLonNvDirection.input, null);
   public static final Property selector = newProperty(65, -1, BFacets.make("radix", BInteger.make(16)));
   public static final Property turnAround = newProperty(65, false, null);
   public static final Property serviceType = newProperty(1, BLonServiceType.unacked, null);
   public static final Property authenticated = newProperty(65, false, null);
   public static final Property addrIndex = newProperty(65, -1, null);
   public static final Type TYPE = Sys.loadType(BNvConfigData.class);
   public static final int UNBOUND_NV_BASE_SELECTOR = 16383;
   public static final int MAX_BOUND_SELECTOR = 12287;
   public static final int UNUSED_ADDR_INDEX = 15;
   public static final int UNUSED_EXT_ADDR_INDEX = 65535;

   public boolean getPriority() {
      return this.getBoolean(priority);
   }

   public void setPriority(boolean v) {
      this.setBoolean(priority, v, null);
   }

   public BLonNvDirection getDirection() {
      return (BLonNvDirection)this.get(direction);
   }

   public void setDirection(BLonNvDirection v) {
      this.set(direction, v, null);
   }

   public int getSelector() {
      return this.getInt(selector);
   }

   public void setSelector(int v) {
      this.setInt(selector, v, null);
   }

   public boolean getTurnAround() {
      return this.getBoolean(turnAround);
   }

   public void setTurnAround(boolean v) {
      this.setBoolean(turnAround, v, null);
   }

   public BLonServiceType getServiceType() {
      return (BLonServiceType)this.get(serviceType);
   }

   public void setServiceType(BLonServiceType v) {
      this.set(serviceType, v, null);
   }

   public boolean getAuthenticated() {
      return this.getBoolean(authenticated);
   }

   public void setAuthenticated(boolean v) {
      this.setBoolean(authenticated, v, null);
   }

   public int getAddrIndex() {
      return this.getInt(addrIndex);
   }

   public void setAddrIndex(int v) {
      this.setInt(addrIndex, v, null);
   }

   public Type getType() {
      return TYPE;
   }

   public boolean isAlias() {
      return false;
   }

   public void writeNetworkBytes(LonOutputStream out) {
      int mrk = out.setBitFieldMark();
      out.writeBooleanBit(this.getPriority(), 0, 7, 1);
      out.writeBooleanBit(this.isOutput(), 0, 6, 1);
      int sel = this.getSelector();
      out.writeBit(sel >> 8, 0, 0, 6);
      out.writeUnsigned8(sel);
      out.writeBooleanBit(this.getTurnAround(), 2, 7, 1);
      out.writeBit(this.getServiceType().getOrdinal(), 2, 5, 2);
      out.writeBooleanBit(this.getAuthenticated(), 2, 4, 1);
      int ndx = this.getAddrIndex() == -1 ? 15 : this.getAddrIndex();
      out.writeBit(ndx, 2, 0, 4);
      out.resetBitFieldMark(mrk);
   }

   public void fromInputStream(LonInputStream in) {
      int mrk = in.setBitFieldMark();
      this.setPriority(in.readBooleanBit(0, 7, 1));
      BLonNvDirection dir = in.readBooleanBit(0, 6, 1) ? BLonNvDirection.output : BLonNvDirection.input;
      this.setDirection(dir);
      int sel = in.readBit(0, 0, 6) << 8 | in.readUnsigned8();
      this.setSelector(sel);
      this.setTurnAround(in.readBooleanBit(2, 7, 1));
      this.setServiceType(BLonServiceType.make(in.readBit(2, 5, 2)));
      this.setAuthenticated(in.readBooleanBit(2, 4, 1));
      int ndx = in.readBit(2, 0, 4);
      this.setAddrIndex(ndx == 15 ? -1 : ndx);
      in.resetBitFieldMark(mrk);
   }

   public boolean isInput() {
      return this.getDirection() == BLonNvDirection.input;
   }

   public boolean isOutput() {
      return this.getDirection() == BLonNvDirection.output;
   }

   public boolean isBoundNv() {
      return this.getSelector() >= 0 && this.getSelector() < 12287;
   }

   public void setUnbound(int nvIndex) {
      this.setSelector(16383 - nvIndex);
      this.setTurnAround(false);
      this.setAddrIndex(-1);
   }

   public String toString(Context c) {
      StringBuilder sb = new StringBuilder();
      sb.append("sel:0x").append(Integer.toString(this.getSelector(), 16));
      sb.append(",").append(this.getServiceType());
      sb.append(",adr:").append(this.getAddrIndex());
      if (this.getDirection() == BLonNvDirection.input) {
         sb.append(",in");
      } else {
         sb.append(",out");
      }

      if (this.getTurnAround()) {
         sb.append(",turn");
      }

      if (this.getPriority()) {
         sb.append(",pri");
      }

      if (this.getAuthenticated()) {
         sb.append(",auth");
      }

      return sb.toString();
   }

   public BLonServiceType getWriteServiceType() {
      return this.getServiceType().getWriteServiceType();
   }
}
