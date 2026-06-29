package javax.baja.bacnet.virtual;

import com.tridium.bacnet.asn.AsnUtil;
import com.tridium.bacnet.asn.NBacnetPropertyValue;
import java.util.logging.Level;
import javax.baja.bacnet.BBacnetDevice;
import javax.baja.bacnet.BacnetException;
import javax.baja.bacnet.datatypes.BBacnetObjectIdentifier;
import javax.baja.bacnet.datatypes.BBacnetPropertyReference;
import javax.baja.bacnet.enums.BBacnetPropertyIdentifier;
import javax.baja.bacnet.export.BLocalBacnetDevice;
import javax.baja.bacnet.io.PropertyReference;
import javax.baja.bacnet.io.PropertyValue;
import javax.baja.bacnet.util.PropertyInfo;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.status.BStatus;
import javax.baja.sys.BComponent;
import javax.baja.sys.BValue;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType
public class BLocalBacnetVirtualProperty extends BBacnetVirtualProperty {
   public static final Type TYPE = Sys.loadType(BLocalBacnetVirtualProperty.class);
   private PropertyReference propertyReference = null;

   @Override
   public Type getType() {
      return TYPE;
   }

   public BLocalBacnetVirtualProperty() {
   }

   public BLocalBacnetVirtualProperty(int propertyId, BValue v, String readFault, boolean useFacets) {
      super(propertyId, v, readFault, useFacets);
      this.propertyReference = new BBacnetPropertyReference(propertyId);
   }

   @Override
   public BBacnetDevice device() {
      return null;
   }

   public BLocalBacnetDevice localDevice() {
      return ((BLocalBacnetVirtualGateway)this.getVirtualGateway()).localDevice();
   }

   @Override
   protected BStatus getDeviceStatus() {
      return ((BLocalBacnetVirtualGateway)this.getVirtualGateway()).localDevice().getStatus();
   }

   @Override
   protected void pollSubscribe() {
      ((BLocalBacnetVirtualGateway)this.getVirtualGateway()).getLocalPoll().subscribe(this);
   }

   @Override
   protected void pollUnsubscribe() {
      ((BLocalBacnetVirtualGateway)this.getVirtualGateway()).getLocalPoll().unsubscribe(this);
   }

   @Override
   public boolean isParentLegal(BComponent parent) {
      return parent instanceof BLocalBacnetVirtualObject;
   }

   @Override
   void write(int propertyArrayIndex, byte[] encodedValue, int priority) {
      this.network().postWrite(new BLocalBacnetVirtualProperty.LocalWrite(this.getPropertyId(), propertyArrayIndex, encodedValue, priority));
   }

   @Override
   byte[] encodeValue(BValue v) {
      byte[] ev = null;
      PropertyInfo pi = this.localDevice().getPropertyInfo(this.object().getObjectId().getObjectType(), this.getPropertyId());
      if (pi != null) {
         ev = AsnUtil.toAsn(pi.getAsnType(), v);
      } else {
         ev = AsnUtil.toAsn(v);
      }

      return ev;
   }

   PropertyReference getPropertyReference() {
      return this.propertyReference;
   }

   BLocalBacnetVirtualObject localObject() {
      return (BLocalBacnetVirtualObject)this.getParent();
   }

   class LocalWrite implements Runnable {
      BBacnetObjectIdentifier objectId;
      PropertyValue pv = null;

      LocalWrite(int propId, int index, byte[] ev, int pri) {
         if (propId == 87) {
            this.pv = new NBacnetPropertyValue(85, -1, ev, index);
         } else {
            this.pv = new NBacnetPropertyValue(propId, index, ev, pri);
         }
      }

      @Override
      public void run() {
         try {
            BLocalBacnetVirtualProperty.this.localObject().getExport().writeProperty(this.pv);
            BLocalBacnetVirtualProperty.this.writeOk();
         } catch (BacnetException var2) {
            BBacnetVirtualProperty.log
               .severe("BacnetException writing " + BBacnetPropertyIdentifier.tag(BLocalBacnetVirtualProperty.this.getPropertyId()) + " in " + this);
            BLocalBacnetVirtualProperty.this.writeFail(var2.toString());
            if (BBacnetVirtualProperty.log.isLoggable(Level.FINE)) {
               BBacnetVirtualProperty.log.log(Level.FINE, "Stack Trace: ", (Throwable)var2);
            }
         }
      }
   }
}
