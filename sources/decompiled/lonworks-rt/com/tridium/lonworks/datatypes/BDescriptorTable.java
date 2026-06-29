package com.tridium.lonworks.datatypes;

import javax.baja.lonworks.enums.BLonReceiveTimer;
import javax.baja.lonworks.enums.BLonRepeatTimer;
import javax.baja.lonworks.enums.BLonServiceType;
import javax.baja.nre.annotations.NiagaraProperties;
import javax.baja.nre.annotations.NiagaraProperty;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.sys.BStruct;
import javax.baja.sys.Property;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType
@NiagaraProperties({@NiagaraProperty(
      name = "standard",
      type = "BLinkDescriptor",
      defaultValue = "new BLinkDescriptor(BLonServiceType.unacked, BLonRepeatTimer.milliSec16,3,BLonReceiveTimer.milliSec128,BLonRepeatTimer.milliSec128)"
   ), @NiagaraProperty(
      name = "reliable",
      type = "BLinkDescriptor",
      defaultValue = "new BLinkDescriptor(BLonServiceType.unackedRpt, BLonRepeatTimer.milliSec16,3,BLonReceiveTimer.milliSec128,BLonRepeatTimer.milliSec128)"
   ), @NiagaraProperty(
      name = "critical",
      type = "BLinkDescriptor",
      defaultValue = "new BLinkDescriptor(BLonServiceType.acked, BLonRepeatTimer.milliSec16,3,BLonReceiveTimer.milliSec128,BLonRepeatTimer.milliSec128)"
   ), @NiagaraProperty(
      name = "authenticated",
      type = "BLinkDescriptor",
      defaultValue = "new BLinkDescriptor(BLonServiceType.acked, BLonRepeatTimer.milliSec16,3,BLonReceiveTimer.milliSec128,BLonRepeatTimer.milliSec128)"
   )})
public class BDescriptorTable extends BStruct {
   public static final Property standard = newProperty(
      0, new BLinkDescriptor(BLonServiceType.unacked, BLonRepeatTimer.milliSec16, 3, BLonReceiveTimer.milliSec128, BLonRepeatTimer.milliSec128), null
   );
   public static final Property reliable = newProperty(
      0, new BLinkDescriptor(BLonServiceType.unackedRpt, BLonRepeatTimer.milliSec16, 3, BLonReceiveTimer.milliSec128, BLonRepeatTimer.milliSec128), null
   );
   public static final Property critical = newProperty(
      0, new BLinkDescriptor(BLonServiceType.acked, BLonRepeatTimer.milliSec16, 3, BLonReceiveTimer.milliSec128, BLonRepeatTimer.milliSec128), null
   );
   public static final Property authenticated = newProperty(
      0, new BLinkDescriptor(BLonServiceType.acked, BLonRepeatTimer.milliSec16, 3, BLonReceiveTimer.milliSec128, BLonRepeatTimer.milliSec128), null
   );
   public static final Type TYPE = Sys.loadType(BDescriptorTable.class);

   public BLinkDescriptor getStandard() {
      return (BLinkDescriptor)this.get(standard);
   }

   public void setStandard(BLinkDescriptor v) {
      this.set(standard, v, null);
   }

   public BLinkDescriptor getReliable() {
      return (BLinkDescriptor)this.get(reliable);
   }

   public void setReliable(BLinkDescriptor v) {
      this.set(reliable, v, null);
   }

   public BLinkDescriptor getCritical() {
      return (BLinkDescriptor)this.get(critical);
   }

   public void setCritical(BLinkDescriptor v) {
      this.set(critical, v, null);
   }

   public BLinkDescriptor getAuthenticated() {
      return (BLinkDescriptor)this.get(authenticated);
   }

   public void setAuthenticated(BLinkDescriptor v) {
      this.set(authenticated, v, null);
   }

   public Type getType() {
      return TYPE;
   }

   public BLinkDescriptor getDescriptor(int ndx) {
      switch (ndx) {
         case 1:
            return this.getStandard();
         case 2:
            return this.getReliable();
         case 3:
            return this.getCritical();
         case 4:
            return this.getAuthenticated();
         default:
            return this.getStandard();
      }
   }
}
