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
import javax.baja.sys.Context;
import javax.baja.sys.Property;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType
@NiagaraProperties({@NiagaraProperty(
      name = "primary",
      type = "int",
      defaultValue = "-1",
      flags = 65
   ), @NiagaraProperty(
      name = "state",
      type = "int",
      defaultValue = "0",
      flags = 69
   ), @NiagaraProperty(
      name = "selector",
      type = "int",
      defaultValue = "UNBOUND_NV_BASE_SELECTOR",
      flags = 65,
      facets = {@Facet("BFacets.make(BFacets.RADIX,BInteger.make(16))")},
      override = true
   ), @NiagaraProperty(
      name = "direction",
      type = "BLonNvDirection",
      defaultValue = "BLonNvDirection.input",
      flags = 65,
      override = true
   )})
public class BAliasConfigData extends BNvConfigData {
   public static final Property primary = newProperty(65, -1, null);
   public static final Property state = newProperty(69, 0, null);
   public static final Property selector = newProperty(65, 16383, BFacets.make("radix", BInteger.make(16)));
   public static final Property direction = newProperty(65, BLonNvDirection.input, null);
   public static final Type TYPE = Sys.loadType(BAliasConfigData.class);
   public static final int UNAVAILABLE_BIT = 1;
   public static final int RESERVE_BIT = 2;
   public static final int BOUND_BIT = 4;
   public static final int FREE = 0;
   public static final int RESERVED = 3;
   public static final int BOUND = 5;

   public int getPrimary() {
      return this.getInt(primary);
   }

   public void setPrimary(int v) {
      this.setInt(primary, v, null);
   }

   public int getState() {
      return this.getInt(state);
   }

   public void setState(int v) {
      this.setInt(state, v, null);
   }

   @Override
   public Type getType() {
      return TYPE;
   }

   @Override
   public boolean isAlias() {
      return true;
   }

   public void clearData() {
      this.setPriority(false);
      this.setDirection(BLonNvDirection.input);
      this.setSelector(16383);
      this.setTurnAround(false);
      this.setServiceType(BLonServiceType.unacked);
      this.setAuthenticated(false);
      this.setAddrIndex(-1);
      this.setPrimary(-1);
      this.setState(0);
   }

   @Override
   public void writeNetworkBytes(LonOutputStream outputStream) {
      super.writeNetworkBytes(outputStream);
      int primary = this.getPrimary();
      if (primary < 0) {
         outputStream.writeUnsigned8(-1);
         outputStream.writeUnsigned16(-1);
      } else if (primary >= 255) {
         outputStream.writeUnsigned8(255);
         outputStream.writeUnsigned16(primary);
      } else {
         outputStream.writeUnsigned8(primary);
      }
   }

   @Override
   public void fromInputStream(LonInputStream inputStream) {
      super.fromInputStream(inputStream);
      if (inputStream.available() > 0) {
         int primary = inputStream.readUnsigned8();
         if (primary == 255 && inputStream.available() >= 2) {
            primary = inputStream.readUnsigned16();
         }

         this.setPrimary(primary);
      }
   }

   @Override
   public String toString(Context c) {
      return super.toString(c) + ",pri:" + this.getPrimary() + ",s:" + this.getState();
   }

   public boolean isAvailable() {
      return (this.getState() & 1) == 0;
   }

   public boolean isReserved() {
      return (this.getState() & 2) != 0;
   }

   public void reserve() {
      this.setState(this.getState() | 3);
   }

   public void makeAvailable() {
      this.setState(this.getState() & -4);
   }

   public void setBound() {
      this.setState(5);
   }

   public void setFree() {
      this.setState(0);
   }
}
