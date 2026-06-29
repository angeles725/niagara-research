package javax.baja.bacnet.export;

import com.tridium.bacnet.asn.AsnUtil;
import com.tridium.bacnet.asn.CovNotificationParameters;
import com.tridium.bacnet.asn.NBacnetPropertyReference;
import com.tridium.bacnet.asn.NBacnetPropertyValue;
import com.tridium.bacnet.asn.NReadPropertyResult;
import com.tridium.bacnet.stack.BBacnetStack;
import com.tridium.bacnet.stack.DeviceRegistry;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.baja.bacnet.BBacnetNetwork;
import javax.baja.bacnet.BacnetException;
import javax.baja.bacnet.datatypes.BBacnetAddress;
import javax.baja.bacnet.datatypes.BBacnetCovSubscription;
import javax.baja.bacnet.datatypes.BBacnetObjectIdentifier;
import javax.baja.bacnet.datatypes.BBacnetRecipient;
import javax.baja.bacnet.io.PropertyValue;
import javax.baja.bacnet.io.RejectException;
import javax.baja.control.BControlPoint;
import javax.baja.status.BStatus;
import javax.baja.util.ICoalesceable;

public class Cov implements Runnable, ICoalesceable {
   private BBacnetCovSubscription sub;
   private PropertyValue[] propertyValues;
   private BIBacnetCovSource covSrc;
   private BControlPoint pt;
   private BBacnetAddress address;
   private static final Logger logger = Logger.getLogger("bacnet.server");

   public Cov(BBacnetCovSubscription sub, BIBacnetCovSource object, BControlPoint pt) {
      this.sub = sub;
      this.covSrc = object;
      this.pt = pt;
   }

   @Override
   public String toString() {
      return "Cov:" + this.sub + " on " + this.covSrc + " to " + this.pt.getName() + ":" + this.pt;
   }

   public Object getCoalesceKey() {
      return this;
   }

   @Override
   public boolean equals(Object o) {
      if (o instanceof Cov) {
         Cov cov = (Cov)o;
         BIBacnetCovSource covObj = cov.covSrc;
         if (this.sub.equivalent(cov.sub) && this.covSrc.getExport().getObjectId().equals(covObj.getExport().getObjectId()) && this.pt == cov.pt) {
            return true;
         }
      }

      return false;
   }

   @Override
   public int hashCode() {
      return 1;
   }

   public ICoalesceable coalesce(ICoalesceable c) {
      this.sub = ((Cov)c).sub;
      this.covSrc = ((Cov)c).covSrc;
      this.pt = ((Cov)c).pt;
      return this;
   }

   @Override
   public void run() {
      try {
         int timeRemaining = this.sub.getTimeRemaining();
         if (timeRemaining < 0) {
            return;
         }

         BBacnetNetwork bacnet = BBacnetNetwork.bacnet();
         BBacnetRecipient recipient = this.sub.getRecipient().getRecipient();
         if (recipient.getChoice() == 0) {
            this.address = DeviceRegistry.getDeviceAddress(recipient.getDevice());
         } else {
            this.address = recipient.getAddress();
         }

         if (log().isLoggable(Level.FINE)) {
            log().fine("Sending Cov notification to address: " + this.address);
         }

         BBacnetObjectIdentifier initiatingDeviceId = BBacnetNetwork.localDevice().getObjectId();
         BIBacnetExportObject export = this.covSrc.getExport();
         this.buildPropertyValues(export);
         CovNotificationParameters cnp = new CovNotificationParameters(
            this.sub.getRecipient().getProcessIdentifier().getUnsigned(),
            initiatingDeviceId,
            this.sub.getMonitoredPropertyReference().getObjectId(),
            timeRemaining,
            this.propertyValues
         );
         if (this.sub.getIssueConfirmedNotifications()) {
            ((BBacnetStack)bacnet.getBacnetComm()).getClient().confirmedCovNotification(this.address, cnp);
         } else {
            ((BBacnetStack)bacnet.getBacnetComm()).getClient().unconfirmedCovNotification(this.address, cnp);
         }
      } catch (BacnetException var7) {
         log().log(Level.SEVERE, "BacnetException sending COV Notification", (Throwable)var7);
      } catch (Exception var8) {
         log().log(Level.SEVERE, "Unable to send COV notification to Bacnet", (Throwable)var8);
      }
   }

   private void buildPropertyValues(BIBacnetExportObject export) throws RejectException {
      if (export.getObjectId().getObjectType() == 12) {
         PropertyValue cov = new NBacnetPropertyValue(
            export.readProperty(
               new NBacnetPropertyReference(
                  this.sub.getMonitoredPropertyReference().getPropertyId(), this.sub.getMonitoredPropertyReference().getPropertyArrayIndex()
               )
            )
         );
         PropertyValue status = new NBacnetPropertyValue(export.readProperty(new NBacnetPropertyReference(111, -1)));
         PropertyValue setpt = new NBacnetPropertyValue(export.readProperty(new NBacnetPropertyReference(108, -1)));
         PropertyValue cvv = new NBacnetPropertyValue(export.readProperty(new NBacnetPropertyReference(21, -1)));
         this.propertyValues = new PropertyValue[]{cov, status, setpt, cvv};
      } else {
         PropertyValue status = null;
         PropertyValue cov;
         if (!this.sub.isCovProperty()) {
            cov = this.readPropertyValue(export);
            status = readStatus(export);
         } else {
            PropertyValue last = this.sub.getLastPropertyValue();
            if (last != null) {
               cov = new NBacnetPropertyValue(last);
            } else {
               cov = this.readPropertyValue(export);
               this.sub.setLastPropertyValue(cov);
            }

            BStatus lastStatus = this.sub.getLastStatusFlags();
            if (lastStatus != null) {
               status = new NBacnetPropertyValue(new NReadPropertyResult(111, -1, AsnUtil.statusToAsnStatusFlags(lastStatus)));
            } else {
               status = readStatus(export);
               this.sub.setLastStatusFlags(AsnUtil.asnStatusFlagsToBStatus(status.getPropertyValue()));
            }
         }

         this.propertyValues = new PropertyValue[]{cov, status};
      }
   }

   private PropertyValue readPropertyValue(BIBacnetExportObject export) throws RejectException {
      return new NBacnetPropertyValue(
         export.readProperty(
            new NBacnetPropertyReference(
               this.sub.getMonitoredPropertyReference().getPropertyId(), this.sub.getMonitoredPropertyReference().getPropertyArrayIndex()
            )
         )
      );
   }

   private static PropertyValue readStatus(BIBacnetExportObject export) throws RejectException {
      return new NBacnetPropertyValue(export.readProperty(new NBacnetPropertyReference(111, -1)));
   }

   public BBacnetCovSubscription getSub() {
      return this.sub;
   }

   private static Logger log() {
      return logger;
   }
}
