package javax.baja.bacnet.datatypes;

import javax.baja.bacnet.io.AsnException;
import javax.baja.bacnet.io.AsnInput;
import javax.baja.bacnet.io.AsnOutput;
import javax.baja.nre.annotations.NiagaraProperties;
import javax.baja.nre.annotations.NiagaraProperty;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.sys.BComponent;
import javax.baja.sys.BInteger;
import javax.baja.sys.Context;
import javax.baja.sys.Property;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType
@NiagaraProperties({@NiagaraProperty(
      name = "channel",
      type = "int",
      defaultValue = "-1"
   ), @NiagaraProperty(
      name = "value",
      type = "BBacnetChannelValue",
      defaultValue = "new BBacnetChannelValue()"
   )})
public class BBacnetGroupChannelValue extends BComponent implements BIBacnetDataType {
   public static final Property channel = newProperty(0, -1, null);
   public static final Property value = newProperty(0, new BBacnetChannelValue(), null);
   public static final Type TYPE = Sys.loadType(BBacnetGroupChannelValue.class);
   public static final int CHANNEL_TAG = 0;
   public static final int OVERRIDING_PRIORITY_TAG = 1;

   public int getChannel() {
      return this.getInt(channel);
   }

   public void setChannel(int v) {
      this.setInt(channel, v, null);
   }

   public BBacnetChannelValue getValue() {
      return (BBacnetChannelValue)this.get(value);
   }

   public void setValue(BBacnetChannelValue v) {
      this.set(value, v, null);
   }

   public Type getType() {
      return TYPE;
   }

   public BBacnetGroupChannelValue() {
   }

   public BBacnetGroupChannelValue(int channel, Integer overridingPriority, BBacnetChannelValue value) {
      this.setChannel(channel);
      if (overridingPriority != null) {
         this.setOverridingPriority(overridingPriority);
      }

      this.setValue(value);
   }

   @Override
   public void writeAsn(AsnOutput out) {
      out.writeUnsignedInteger(0, this.getChannel());
      Integer oPri = this.getOverridingPriority();
      if (oPri != null) {
         out.writeUnsignedInteger(1, oPri.intValue());
      }

      this.getValue().writeAsn(out);
   }

   @Override
   public void readAsn(AsnInput in) throws AsnException {
      int channel = in.readUnsignedInt(0);
      Integer overridingPriority = null;
      if (in.peekTag() == 1) {
         overridingPriority = in.readUnsignedInt(1);
      }

      BBacnetChannelValue value = new BBacnetChannelValue();
      value.readAsn(in);
      this.setInt(BBacnetGroupChannelValue.channel, channel, noWrite);
      this.setOverridingPriority(overridingPriority);
      this.set(BBacnetGroupChannelValue.value, value, noWrite);
   }

   public Integer getOverridingPriority() {
      BInteger priority = (BInteger)this.get("overridingPriority");
      return priority != null ? priority.getInt() : null;
   }

   public void setOverridingPriority(Integer overridingPriority) {
      Property property = this.getProperty("overridingPriority");
      if (property != null) {
         if (overridingPriority == null) {
            this.remove(property);
         } else {
            this.setInt(property, overridingPriority);
         }
      } else if (overridingPriority != null) {
         this.add("overridingPriority", BInteger.make(overridingPriority));
      }
   }

   public String toString(Context context) {
      StringBuilder sb = new StringBuilder("Channel: ").append(this.getChannel());
      sb.append("\n\tOverridingPriority: ").append(this.getOverridingPriority());
      return sb.toString();
   }
}
