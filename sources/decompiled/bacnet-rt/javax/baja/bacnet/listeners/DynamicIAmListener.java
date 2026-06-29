package javax.baja.bacnet.listeners;

import com.tridium.bacnet.services.unconfirmed.IAmRequest;
import com.tridium.bacnet.stack.BBacnetStack;
import com.tridium.bacnet.stack.IAmListener;
import com.tridium.bacnet.stack.server.BBacnetServerLayer;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.baja.bacnet.BBacnetDevice;
import javax.baja.bacnet.BBacnetNetwork;
import javax.baja.bacnet.datatypes.BBacnetAddress;
import javax.baja.bacnet.datatypes.BBacnetObjectIdentifier;

public class DynamicIAmListener implements IAmListener {
   private static DynamicIAmListener dynamicIAmListener = new DynamicIAmListener();
   private static final int TRENDING = 1;
   private static final int ALARMING = 2;
   private static final int SCHEDULING = 4;
   private Map<BBacnetObjectIdentifier, Set<DynamicIAmListener.IAmHandler>> handlers;
   private boolean iAmRegistered;
   private static final Logger logger = Logger.getLogger("bacnet.listeners");

   private DynamicIAmListener() {
   }

   public static DynamicIAmListener getDynamicIAmListenerInstance() {
      return dynamicIAmListener;
   }

   @Override
   public void receiveIAm(IAmRequest request, BBacnetAddress sourceAddress) {
      try {
         BBacnetObjectIdentifier remoteOid = request.getObjectId();
         Set<DynamicIAmListener.IAmHandler> handlerList = this.getHandlerList(remoteOid);
         if (handlerList == null) {
            return;
         }

         for (DynamicIAmListener.IAmHandler handler : handlerList) {
            handler.handle(request, sourceAddress);
         }

         this.removeHandler(remoteOid);
      } catch (Exception var7) {
         if (logger.isLoggable(Level.FINE)) {
            logger.log(Level.FINE, "Exception occurred during receiveIAm", (Throwable)var7);
         }
      }
   }

   public void subscribeHandler(DynamicIAmListener.IAmHandler handler, BBacnetObjectIdentifier deviceId) {
      Set<DynamicIAmListener.IAmHandler> handlerList = this.addAndGetHandlerList(deviceId);
      handlerList.add(handler);
   }

   public boolean handlesTrending(BBacnetObjectIdentifier deviceId) {
      return this.handles(deviceId, 1);
   }

   public boolean handlesAlarms(BBacnetObjectIdentifier deviceId) {
      return this.handles(deviceId, 2);
   }

   public boolean handlesScheduling(BBacnetObjectIdentifier deviceId) {
      return this.handles(deviceId, 4);
   }

   private boolean handles(BBacnetObjectIdentifier deviceId, int service) {
      Set<DynamicIAmListener.IAmHandler> handlerList = this.getHandlerList(deviceId);
      if (handlerList == null) {
         return false;
      } else {
         for (DynamicIAmListener.IAmHandler handler : handlerList) {
            if (handler.is(service)) {
               return true;
            }
         }

         return false;
      }
   }

   private void removeHandler(BBacnetObjectIdentifier deviceId) {
      if (this.iAmRegistered && this.handlers.containsKey(deviceId)) {
         this.handlers.remove(deviceId);
         synchronized (this) {
            if (this.handlers.size() == 0) {
               this.unregister();
            }
         }
      }
   }

   private Set<DynamicIAmListener.IAmHandler> getHandlerList(BBacnetObjectIdentifier deviceId) {
      return !this.iAmRegistered && !this.handlers.containsKey(deviceId) ? null : this.handlers.get(deviceId);
   }

   private synchronized Set<DynamicIAmListener.IAmHandler> addAndGetHandlerList(BBacnetObjectIdentifier deviceId) {
      if (!this.iAmRegistered) {
         this.register();
      }

      Set<DynamicIAmListener.IAmHandler> handlerList = this.getHandlerList(deviceId);
      if (handlerList == null) {
         handlerList = new ConcurrentSkipListSet<>();
      }

      this.handlers.put(deviceId, handlerList);
      return handlerList;
   }

   private void register() {
      this.handlers = new ConcurrentHashMap<>();
      this.getServer().registerIAmListener(this);
      this.iAmRegistered = true;
   }

   private void unregister() {
      this.iAmRegistered = false;
      this.handlers = null;
      this.getServer().unregisterIAmListener(this);
   }

   private BBacnetServerLayer getServer() {
      BBacnetNetwork network = BBacnetNetwork.bacnet();
      BBacnetStack comm = (BBacnetStack)network.getBacnetComm();
      return comm.getServer();
   }

   public abstract static class IAmHandler implements Comparable<Object> {
      private int serviceHandled = 0;

      public abstract void handle(IAmRequest var1, BBacnetAddress var2);

      public void handles(int propertyId) {
         switch (propertyId) {
            case 9:
               this.serviceHandled |= 2;
               break;
            case 17:
               this.serviceHandled |= 4;
               break;
            case 20:
               this.serviceHandled |= 1;
         }
      }

      public boolean isTrending() {
         return this.is(1);
      }

      public boolean isAlarming() {
         return this.is(2);
      }

      public boolean isScheduling() {
         return this.is(4);
      }

      private boolean is(int service) {
         switch (service) {
            case 1:
            case 2:
            case 4:
               return (this.serviceHandled & service) != 0;
            case 3:
            default:
               return false;
         }
      }

      protected void bindDevice(BBacnetObjectIdentifier deviceOid) {
         if (BBacnetNetwork.bacnet().lookupDeviceById(deviceOid) == null) {
            BBacnetDevice device = new BBacnetDevice();
            device.setObjectId(deviceOid, null);
            BBacnetNetwork.bacnet().add(device.getName(), device);
         }
      }

      @Override
      public int compareTo(Object o) {
         return o == this ? 0 : 1;
      }
   }
}
