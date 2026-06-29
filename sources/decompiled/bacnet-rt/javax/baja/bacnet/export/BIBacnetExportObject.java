package javax.baja.bacnet.export;

import com.tridium.bacnet.stack.client.AsyncEventNotificationRequest;
import com.tridium.bacnet.stack.transport.BBacnetTransportLayer;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.baja.agent.BIAgent;
import javax.baja.alarm.BAlarmClass;
import javax.baja.alarm.BAlarmRecord;
import javax.baja.bacnet.BacnetConst;
import javax.baja.bacnet.BacnetException;
import javax.baja.bacnet.datatypes.BBacnetObjectIdentifier;
import javax.baja.bacnet.io.ChangeListError;
import javax.baja.bacnet.io.ErrorType;
import javax.baja.bacnet.io.PropertyReference;
import javax.baja.bacnet.io.PropertyValue;
import javax.baja.bacnet.io.RangeData;
import javax.baja.bacnet.io.RangeReference;
import javax.baja.bacnet.io.RejectException;
import javax.baja.control.BControlPoint;
import javax.baja.naming.BOrd;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.status.BStatus;
import javax.baja.sys.BComplex;
import javax.baja.sys.BComponent;
import javax.baja.sys.BComponentEvent;
import javax.baja.sys.BComponentEventMask;
import javax.baja.sys.BInterface;
import javax.baja.sys.BObject;
import javax.baja.sys.Context;
import javax.baja.sys.Slot;
import javax.baja.sys.Subscriber;
import javax.baja.sys.Sys;
import javax.baja.sys.Topic;
import javax.baja.sys.Type;

@NiagaraType
public interface BIBacnetExportObject extends BInterface, BIAgent, BacnetConst {
   Type TYPE = Sys.loadType(BIBacnetExportObject.class);

   BComplex getParent();

   BObject getObject();

   BOrd getObjectOrd();

   void setObjectOrd(BOrd var1, Context var2);

   BStatus getStatus();

   boolean isFatalFault();

   void checkConfiguration();

   BBacnetObjectIdentifier getObjectId();

   void setObjectId(BBacnetObjectIdentifier var1);

   String getObjectName();

   void setObjectName(String var1);

   int[] getPropertyList();

   PropertyValue readProperty(PropertyReference var1) throws RejectException;

   PropertyValue[] readPropertyMultiple(PropertyReference[] var1) throws RejectException;

   RangeData readRange(RangeReference var1) throws RejectException;

   ErrorType writeProperty(PropertyValue var1) throws BacnetException;

   ChangeListError addListElements(PropertyValue var1) throws BacnetException;

   ChangeListError removeListElements(PropertyValue var1) throws BacnetException;

   default void setTransportLayer(BBacnetTransportLayer transportLayer) {
   }

   default boolean isDynamicallyCreated() {
      return false;
   }

   public static class ObjectSubscriber extends Subscriber {
      private static final BComponentEventMask EVENT_MASK = BComponentEventMask.make(new int[]{0, 5, 9, 8, 13, 20});
      private HashMap<BComponent, BIBacnetExportObject> sublist = new HashMap<>();
      private static final Logger logger = Logger.getLogger("bacnet.server");

      public ObjectSubscriber() {
         this.setMask(EVENT_MASK);
      }

      public void subscribe(BIBacnetExportObject export, BComponent src) {
         this.sublist.put(src, export);
         super.subscribe(src);
      }

      public void unsubscribe(BIBacnetExportObject export, BComponent src) {
         if (src != null) {
            this.sublist.remove(src);
            super.unsubscribe(src);
         }
      }

      public void event(BComponentEvent event) {
         BComponent src = event.getSourceComponent();
         BIBacnetExportObject export = this.sublist.get(src);
         if (export == null) {
            logger.info("ObjectSubscriber: event from unknown source:" + src);
            this.sublist.remove(src);
         } else {
            try {
               switch (event.getId()) {
                  case 0:
                     if (export instanceof BBacnetScheduleDescriptor && src == ((BBacnetScheduleDescriptor)export).getSchedule()) {
                        if (event.getSlotName().equals("out")) {
                           ((BBacnetScheduleDescriptor)export).writePresentValue();
                        } else if (event.getSlotName().equals("lastModified")) {
                           export.checkConfiguration();
                        }
                     }

                     if (export instanceof BBacnetEventSource) {
                        Slot s = event.getSlot();
                        String sn = event.getSlotName();
                        if (s.equals(BControlPoint.facets)) {
                           export.checkConfiguration();
                        } else if (sn.equals("out")) {
                           BBacnetEventSource eventSource = (BBacnetEventSource)export;
                           eventSource.checkValid();
                           eventSource.statusChanged();
                        }
                     }
                     break;
                  case 5:
                     if (export instanceof BBacnetNotificationClassDescriptor) {
                        Topic topic = (Topic)event.getSlot();
                        if (topic.equals(BAlarmClass.alarm)) {
                           AsyncEventNotificationRequest.removeAckTimestamp((BAlarmRecord)event.getValue());
                        }
                     }
                     break;
                  case 8:
                  case 9:
                     if (export instanceof BBacnetNotificationClassDescriptor) {
                        ((BBacnetNotificationClassDescriptor)export).recipientListChanged();
                     }
                     break;
                  case 13:
                     String oldOrd = src.getSlotPath().getParent().getBody().substring(1);
                     oldOrd = oldOrd + "/" + event.getValue().toString();
                     if (export.getObjectName().equals(oldOrd)) {
                        export.setObjectName(((BComponent)export.getObject()).getSlotPath().getBody().substring(1));
                     }

                     if (export.getObjectOrd().toString().endsWith(oldOrd)) {
                        export.setObjectOrd(BOrd.make("station:|" + event.getSourceComponent().getSlotPath().toString()), null);
                     }
                     break;
                  case 20:
                     BObject object = export.getObject();
                     if (object != export) {
                        ((BComponent)export.getParent()).remove((BComponent)export);
                     }
               }
            } catch (Exception var9) {
               if (logger.isLoggable(Level.FINE)) {
                  logger.log(Level.FINE, "Error in BACnet ObjectSubscriber:export=" + export + "; src=" + src, (Throwable)var9);
               }
            }
         }
      }
   }
}
