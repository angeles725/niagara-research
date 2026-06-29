package javax.baja.bacnet.datatypes.access;

import javax.baja.bacnet.datatypes.BBacnetDeviceObjectPropertyReference;
import javax.baja.bacnet.datatypes.BBacnetDeviceObjectReference;
import javax.baja.bacnet.datatypes.BIBacnetDataType;
import javax.baja.bacnet.io.AsnException;
import javax.baja.bacnet.io.AsnInput;
import javax.baja.bacnet.io.AsnOutput;
import javax.baja.nre.annotations.NiagaraProperties;
import javax.baja.nre.annotations.NiagaraProperty;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.sys.BStruct;
import javax.baja.sys.Context;
import javax.baja.sys.Property;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType
@NiagaraProperties({@NiagaraProperty(
      name = "timeRangeSpecifier",
      type = "int",
      defaultValue = "1"
   ), @NiagaraProperty(
      name = "timeRange",
      type = "BBacnetDeviceObjectPropertyReference",
      defaultValue = "new BBacnetDeviceObjectPropertyReference()"
   ), @NiagaraProperty(
      name = "locationSpecifier",
      type = "int",
      defaultValue = "1"
   ), @NiagaraProperty(
      name = "location",
      type = "BBacnetDeviceObjectReference",
      defaultValue = "new BBacnetDeviceObjectReference()"
   ), @NiagaraProperty(
      name = "enable",
      type = "boolean",
      defaultValue = "false"
   )})
public final class BBacnetAccessRule extends BStruct implements BIBacnetDataType {
   public static final Property timeRangeSpecifier = newProperty(0, 1, null);
   public static final Property timeRange = newProperty(0, new BBacnetDeviceObjectPropertyReference(), null);
   public static final Property locationSpecifier = newProperty(0, 1, null);
   public static final Property location = newProperty(0, new BBacnetDeviceObjectReference(), null);
   public static final Property enable = newProperty(0, false, null);
   public static final Type TYPE = Sys.loadType(BBacnetAccessRule.class);
   public static final int SPECIFIED = 0;
   public static final int TIME_RANGE_SPECIFIER_TAG = 0;
   public static final int TIME_RANGE_TAG = 1;
   public static final int LOCATION_SPECIFIER_TAG = 2;
   public static final int LOCATION_TAG = 3;
   public static final int ENABLE_TAG = 4;
   public static final int MAX_ENCODED_SIZE = 16;

   public int getTimeRangeSpecifier() {
      return this.getInt(timeRangeSpecifier);
   }

   public void setTimeRangeSpecifier(int v) {
      this.setInt(timeRangeSpecifier, v, null);
   }

   public BBacnetDeviceObjectPropertyReference getTimeRange() {
      return (BBacnetDeviceObjectPropertyReference)this.get(timeRange);
   }

   public void setTimeRange(BBacnetDeviceObjectPropertyReference v) {
      this.set(timeRange, v, null);
   }

   public int getLocationSpecifier() {
      return this.getInt(locationSpecifier);
   }

   public void setLocationSpecifier(int v) {
      this.setInt(locationSpecifier, v, null);
   }

   public BBacnetDeviceObjectReference getLocation() {
      return (BBacnetDeviceObjectReference)this.get(location);
   }

   public void setLocation(BBacnetDeviceObjectReference v) {
      this.set(location, v, null);
   }

   public boolean getEnable() {
      return this.getBoolean(enable);
   }

   public void setEnable(boolean v) {
      this.setBoolean(enable, v, null);
   }

   public Type getType() {
      return TYPE;
   }

   public BBacnetAccessRule() {
   }

   public BBacnetAccessRule(
      int timeRangeSpecifier, BBacnetDeviceObjectPropertyReference timeRange, int locationSpecifier, BBacnetDeviceObjectReference location, boolean enable
   ) {
      this.setTimeRangeSpecifier(timeRangeSpecifier);
      this.setTimeRange(timeRange);
      this.setLocationSpecifier(locationSpecifier);
      this.setLocation(location);
      this.setEnable(enable);
   }

   public String toString(Context context) {
      StringBuilder sb = new StringBuilder("BBacnetAccessRule:");
      sb.append(this.getTimeRangeSpecifier())
         .append(":")
         .append(this.getTimeRange())
         .append(":")
         .append(this.getLocationSpecifier())
         .append(":")
         .append(this.getLocation())
         .append(":")
         .append(this.getEnable());
      return sb.toString();
   }

   @Override
   public void writeAsn(AsnOutput out) {
      int trs = this.getTimeRangeSpecifier();
      out.writeEnumerated(0, trs);
      if (trs == 0) {
         out.writeOpeningTag(1);
         this.getTimeRange().writeAsn(out);
         out.writeClosingTag(1);
      }

      int ls = this.getLocationSpecifier();
      out.writeEnumerated(2, ls);
      if (ls == 0) {
         out.writeOpeningTag(3);
         this.getLocation().writeAsn(out);
         out.writeClosingTag(3);
      }

      out.writeBoolean(4, this.getEnable());
   }

   @Override
   public void readAsn(AsnInput in) throws AsnException {
      int timeRangeSpecifier = in.readEnumerated(0);
      BBacnetDeviceObjectPropertyReference timeRange = new BBacnetDeviceObjectPropertyReference();
      if (in.peekTag() == 1) {
         in.skipOpeningTag(1);
         timeRange.readAsn(in);
         in.skipClosingTag(1);
      }

      int locationSpecifier = in.readEnumerated(2);
      BBacnetDeviceObjectReference location = new BBacnetDeviceObjectReference();
      if (in.peekTag() == 3) {
         in.skipOpeningTag(3);
         location.readAsn(in);
         in.skipClosingTag(3);
      }

      boolean enable = in.readBoolean(4);
      this.setInt(BBacnetAccessRule.timeRangeSpecifier, timeRangeSpecifier, noWrite);
      this.set(BBacnetAccessRule.timeRange, timeRange, noWrite);
      this.setInt(BBacnetAccessRule.locationSpecifier, locationSpecifier, noWrite);
      this.set(BBacnetAccessRule.location, location, noWrite);
      this.setBoolean(BBacnetAccessRule.enable, enable, noWrite);
   }
}
