package javax.baja.lonworks.datatypes;

import com.tridium.lonworks.loncomm.NLonLinkLayer;
import javax.baja.lonworks.BLonNetwork;
import javax.baja.lonworks.LonComm;
import javax.baja.lonworks.enums.BLonReceiveTimer;
import javax.baja.lonworks.enums.BLonRepeatTimer;
import javax.baja.lonworks.io.LonLinkLayer;
import javax.baja.nre.annotations.NiagaraProperties;
import javax.baja.nre.annotations.NiagaraProperty;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.sys.BIcon;
import javax.baja.sys.BStruct;
import javax.baja.sys.Property;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType
@NiagaraProperties({@NiagaraProperty(
      name = "deviceName",
      type = "String",
      defaultValue = "LON1"
   ), @NiagaraProperty(
      name = "linkDebug",
      type = "boolean",
      defaultValue = "false"
   ), @NiagaraProperty(
      name = "repeatTimer",
      type = "BLonRepeatTimer",
      defaultValue = "BLonRepeatTimer.milliSec96"
   ), @NiagaraProperty(
      name = "receiveTimer",
      type = "BLonReceiveTimer",
      defaultValue = "BLonReceiveTimer.milliSec384"
   ), @NiagaraProperty(
      name = "transmitTimer",
      type = "BLonRepeatTimer",
      defaultValue = "BLonRepeatTimer.milliSec96"
   ), @NiagaraProperty(
      name = "retryCount",
      type = "int",
      defaultValue = "3"
   )})
public class BLonCommConfig extends BStruct {
   public static final Property deviceName = newProperty(0, "LON1", null);
   public static final Property linkDebug = newProperty(0, false, null);
   public static final Property repeatTimer = newProperty(0, BLonRepeatTimer.milliSec96, null);
   public static final Property receiveTimer = newProperty(0, BLonReceiveTimer.milliSec384, null);
   public static final Property transmitTimer = newProperty(0, BLonRepeatTimer.milliSec96, null);
   public static final Property retryCount = newProperty(0, 3, null);
   public static final Type TYPE = Sys.loadType(BLonCommConfig.class);
   private static final BIcon icon = BIcon.std("commConfig.png");

   public String getDeviceName() {
      return this.getString(deviceName);
   }

   public void setDeviceName(String v) {
      this.setString(deviceName, v, null);
   }

   public boolean getLinkDebug() {
      return this.getBoolean(linkDebug);
   }

   public void setLinkDebug(boolean v) {
      this.setBoolean(linkDebug, v, null);
   }

   public BLonRepeatTimer getRepeatTimer() {
      return (BLonRepeatTimer)this.get(repeatTimer);
   }

   public void setRepeatTimer(BLonRepeatTimer v) {
      this.set(repeatTimer, v, null);
   }

   public BLonReceiveTimer getReceiveTimer() {
      return (BLonReceiveTimer)this.get(receiveTimer);
   }

   public void setReceiveTimer(BLonReceiveTimer v) {
      this.set(receiveTimer, v, null);
   }

   public BLonRepeatTimer getTransmitTimer() {
      return (BLonRepeatTimer)this.get(transmitTimer);
   }

   public void setTransmitTimer(BLonRepeatTimer v) {
      this.set(transmitTimer, v, null);
   }

   public int getRetryCount() {
      return this.getInt(retryCount);
   }

   public void setRetryCount(int v) {
      this.setInt(retryCount, v, null);
   }

   public Type getType() {
      return TYPE;
   }

   public BIcon getIcon() {
      return icon;
   }

   public LonLinkLayer makeLonLinkLayer(LonComm lc, BLonNetwork lon) {
      return new NLonLinkLayer(lc, lon);
   }
}
