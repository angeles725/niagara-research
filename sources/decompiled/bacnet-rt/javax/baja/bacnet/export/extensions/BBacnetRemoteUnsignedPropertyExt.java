package javax.baja.bacnet.export.extensions;

import com.tridium.bacnet.asn.AsnUtil;
import java.util.logging.Logger;
import javax.baja.bacnet.BBacnetDevice;
import javax.baja.bacnet.BBacnetNetwork;
import javax.baja.bacnet.datatypes.BBacnetObjectIdentifier;
import javax.baja.bacnet.io.AsnException;
import javax.baja.bacnet.point.BBacnetTuningPolicy;
import javax.baja.bacnet.util.BIBacnetPollable;
import javax.baja.bacnet.util.PollListEntry;
import javax.baja.control.BControlPoint;
import javax.baja.control.BNumericWritable;
import javax.baja.driver.util.BAbstractPollService;
import javax.baja.driver.util.BPollFrequency;
import javax.baja.nre.annotations.NiagaraProperty;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.status.BStatus;
import javax.baja.sys.BDouble;
import javax.baja.sys.Context;
import javax.baja.sys.Property;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType
@NiagaraProperty(
   name = "deviceId",
   type = "int",
   defaultValue = "-1",
   flags = 1
)
public class BBacnetRemoteUnsignedPropertyExt extends BBacnetUnsignedPropertyExt implements BIBacnetPollable {
   public static final Property deviceId = newProperty(1, -1, null);
   public static final Type TYPE = Sys.loadType(BBacnetRemoteUnsignedPropertyExt.class);
   private static final Logger logger = Logger.getLogger("bacnet.export.object.remote.unsigned.property");
   private long lastValue;
   private BBacnetDevice device;

   public int getDeviceId() {
      return this.getInt(deviceId);
   }

   public void setDeviceId(int v) {
      this.setInt(deviceId, v, null);
   }

   @Override
   public Type getType() {
      return TYPE;
   }

   public BBacnetRemoteUnsignedPropertyExt() {
   }

   public BBacnetRemoteUnsignedPropertyExt(BBacnetObjectIdentifier oid, int propertyId) {
      super(oid, propertyId);
   }

   public void started() throws Exception {
      super.started();
      BAbstractPollService pollService = BBacnetNetwork.bacnet().getPollService(this);
      pollService.subscribe(this);
   }

   public void stopped() throws Exception {
      BAbstractPollService pollService = BBacnetNetwork.bacnet().getPollService(this);
      pollService.unsubscribe(this);
      super.stopped();
   }

   @Override
   public BBacnetDevice device() {
      if (this.device == null) {
         this.device = (BBacnetDevice)this.getParent().getParent().getParent();
      }

      return this.device;
   }

   @Override
   public int getPollableType() {
      return 5;
   }

   @Deprecated
   @Override
   public boolean poll() {
      return false;
   }

   @Override
   public void readFail(String failureMsg) {
      this.setStatus(BStatus.makeFault(this.getStatus(), true));
      this.setFaultCause(failureMsg);
   }

   @Override
   public void fromEncodedValue(byte[] encodedValue, BStatus status, Context cx) {
      try {
         long value = AsnUtil.fromAsnUnsignedInt(encodedValue);
         if (this.lastValue != value) {
            this.lastValue = value;
            BNumericWritable point = this.getNumericParentPoint();
            point.set(BDouble.make(value));
         }
      } catch (AsnException var7) {
         logger.severe("Could not decode the encoded the unsigned value of the remote property: " + var7);
      }
   }

   @Override
   public PollListEntry[] getPollListEntries() {
      return new PollListEntry[]{new PollListEntry(this.getObjectId(), this.getPropertyId(), this.device(), this)};
   }

   public BPollFrequency getPollFrequency() {
      return ((BBacnetTuningPolicy)BBacnetNetwork.bacnet().getTuningPolicies().getDefaultPolicy()).getPollFrequency();
   }

   private BNumericWritable getNumericParentPoint() {
      BControlPoint point = this.getParentPoint();
      if (!(point instanceof BNumericWritable)) {
         throw new RuntimeException("Parent of the extension should be of Numeric type.");
      } else {
         return (BNumericWritable)point;
      }
   }
}
