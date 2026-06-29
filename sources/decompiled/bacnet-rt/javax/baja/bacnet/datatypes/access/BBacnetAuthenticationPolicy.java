package javax.baja.bacnet.datatypes.access;

import com.tridium.bacnet.BacUtil;
import java.util.ArrayList;
import java.util.List;
import javax.baja.bacnet.datatypes.BIBacnetDataType;
import javax.baja.bacnet.io.AsnException;
import javax.baja.bacnet.io.AsnInput;
import javax.baja.bacnet.io.AsnOutput;
import javax.baja.nre.annotations.NiagaraProperties;
import javax.baja.nre.annotations.NiagaraProperty;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.sys.BComponent;
import javax.baja.sys.BValue;
import javax.baja.sys.Context;
import javax.baja.sys.Property;
import javax.baja.sys.SlotCursor;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType
@NiagaraProperties({@NiagaraProperty(
      name = "orderEnforced",
      type = "boolean",
      defaultValue = "false"
   ), @NiagaraProperty(
      name = "timeout",
      type = "long",
      defaultValue = "3000"
   )})
public final class BBacnetAuthenticationPolicy extends BComponent implements BIBacnetDataType {
   public static final Property orderEnforced = newProperty(0, false, null);
   public static final Property timeout = newProperty(0, 3000, null);
   public static final Type TYPE = Sys.loadType(BBacnetAuthenticationPolicy.class);
   public static final int POLICY_TAG = 0;
   public static final int ORDER_ENFORCED_TAG = 1;
   public static final int TIMEOUT_TAG = 2;
   public static int MAX_ENTRIES = 100;

   public boolean getOrderEnforced() {
      return this.getBoolean(orderEnforced);
   }

   public void setOrderEnforced(boolean v) {
      this.setBoolean(orderEnforced, v, null);
   }

   public long getTimeout() {
      return this.getLong(timeout);
   }

   public void setTimeout(long v) {
      this.setLong(timeout, v, null);
   }

   public Type getType() {
      return TYPE;
   }

   public BBacnetAuthenticationPolicy() {
   }

   public BBacnetAuthenticationPolicy(List<BBacnetAuthenticationPolicyEntry> policyEntries, boolean orderEnforced, int timeout) {
      int i = 0;

      for (BBacnetAuthenticationPolicyEntry entry : policyEntries) {
         BacUtil.setOrAdd(this, "entry" + i++, entry, noWrite);
      }

      this.setOrderEnforced(orderEnforced);
      this.setTimeout(timeout);
   }

   public String toString(Context context) {
      StringBuilder sb = new StringBuilder("BacnetAuthenticationPolicy:");
      sb.append(this.getOrderEnforced()).append(":").append(this.getTimeout());
      return sb.toString();
   }

   @Override
   public void writeAsn(AsnOutput out) {
      out.writeOpeningTag(0);
      SlotCursor<Property> sc = this.getProperties();

      while (sc.next(BBacnetAuthenticationPolicyEntry.class)) {
         BBacnetAuthenticationPolicyEntry entry = (BBacnetAuthenticationPolicyEntry)sc.get();
         entry.writeAsn(out);
      }

      out.writeClosingTag(0);
      out.writeBoolean(1, this.getOrderEnforced());
      out.writeUnsignedInteger(2, this.getTimeout());
   }

   @Override
   public void readAsn(AsnInput in) throws AsnException {
      in.skipOpeningTag(0);
      List<BBacnetAuthenticationPolicyEntry> policyEntries = new ArrayList<>();
      in.peekTag();

      while (!in.isClosingTag(0) && policyEntries.size() < MAX_ENTRIES) {
         BBacnetAuthenticationPolicyEntry entry = new BBacnetAuthenticationPolicyEntry();
         entry.readAsn(in);
         policyEntries.add(entry);
         in.peekTag();
      }

      in.skipClosingTag(0);
      boolean orderEnforced = in.readBoolean(1);
      long timeout = in.readUnsignedInteger(2);
      this.removeAll(noWrite);
      int length = policyEntries.size();

      for (int i = 0; i < length; i++) {
         this.add("entry" + i, (BValue)policyEntries.get(i), noWrite);
      }

      this.setBoolean(BBacnetAuthenticationPolicy.orderEnforced, orderEnforced, noWrite);
      this.setLong(BBacnetAuthenticationPolicy.timeout, timeout, noWrite);
   }
}
