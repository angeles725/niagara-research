package com.tridium.bacnet.stack.link.sc;

import com.tridium.bacnet.stack.link.sc.connection.BAbstractConnection;
import com.tridium.bacnet.stack.link.sc.connection.BAcceptingConnection;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.baja.nre.annotations.Facet;
import javax.baja.nre.annotations.NiagaraAction;
import javax.baja.nre.annotations.NiagaraProperties;
import javax.baja.nre.annotations.NiagaraProperty;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.sys.Action;
import javax.baja.sys.BAbsTime;
import javax.baja.sys.BBoolean;
import javax.baja.sys.BComponent;
import javax.baja.sys.BFacets;
import javax.baja.sys.BRelTime;
import javax.baja.sys.Clock;
import javax.baja.sys.Context;
import javax.baja.sys.Property;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;
import javax.baja.sys.Clock.Ticket;

@NiagaraType
@NiagaraProperties({@NiagaraProperty(
      name = "enableIdleCleanup",
      type = "boolean",
      defaultValue = "true"
   ), @NiagaraProperty(
      name = "idleCleanupInterval",
      type = "BRelTime",
      defaultValue = "BRelTime.makeHours(1)",
      facets = {@Facet(
         name = "BFacets.SHOW_SECONDS",
         value = "BBoolean.FALSE"
      ), @Facet(
         name = "BFacets.MIN",
         value = "BRelTime.make(0)"
      )}
   )})
@NiagaraAction(
   name = "cleanupIdle",
   flags = 2068
)
public abstract class BConnectionContainer extends BComponent {
   public static final Property enableIdleCleanup = newProperty(0, true, null);
   public static final Property idleCleanupInterval = newProperty(
      0, BRelTime.makeHours(1), BFacets.make(BFacets.make("showSeconds", BBoolean.FALSE), BFacets.make("min", BRelTime.make(0L)))
   );
   public static final Action cleanupIdle = newAction(2068, null);
   public static final Type TYPE = Sys.loadType(BConnectionContainer.class);
   private Ticket waitTicket;

   public boolean getEnableIdleCleanup() {
      return this.getBoolean(enableIdleCleanup);
   }

   public void setEnableIdleCleanup(boolean v) {
      this.setBoolean(enableIdleCleanup, v, null);
   }

   public BRelTime getIdleCleanupInterval() {
      return (BRelTime)this.get(idleCleanupInterval);
   }

   public void setIdleCleanupInterval(BRelTime v) {
      this.set(idleCleanupInterval, v, null);
   }

   public void cleanupIdle() {
      this.invoke(cleanupIdle, null, null);
   }

   public Type getType() {
      return TYPE;
   }

   private void scheduleIdleCleanup() {
      if (this.isRunning() && this.getEnableIdleCleanup()) {
         if (this.waitTicket == null && this.getIdleCleanupInterval().getMillis() > 0L) {
            this.waitTicket = Clock.schedulePeriodically(this, this.getIdleCleanupInterval(), cleanupIdle, null);
         }
      }
   }

   private void cancelIdleCleanup() {
      if (this.waitTicket != null) {
         this.waitTicket.cancel();
      }

      this.waitTicket = null;
   }

   boolean shouldCleanupImmediately() {
      return this.getEnableIdleCleanup() && this.getIdleCleanupInterval().getMillis() == 0L;
   }

   public abstract Logger getLogger();

   public void doCleanupIdle() {
      if (this.isRunning() && this.getEnableIdleCleanup()) {
         if (this.getLogger().isLoggable(Level.FINE)) {
            this.getLogger().fine("Connection container running periodic idle accepting connection cleanup (interval: " + this.getIdleCleanupInterval() + ')');
         }

         BAbsTime thresholdTime = BAbsTime.make(BAbsTime.now().getMillis() - this.getIdleCleanupInterval().getMillis());

         for (BAcceptingConnection connection : (BAcceptingConnection[])this.getChildren(BAcceptingConnection.class)) {
            if (shouldRemove(connection, thresholdTime)) {
               this.remove(connection);
               if (this.getLogger().isLoggable(Level.FINE)) {
                  this.getLogger().fine(connection.getLogInfo().append(": Removed by idle accepting connection cleanup.").toString());
               }
            }
         }
      }
   }

   public final void doDisconnectAll() {
      if (this.isRunning()) {
         this.getLogger().fine("Connection container calling disconnect on all child connections.");

         for (BAbstractConnection connection : (BAbstractConnection[])this.getChildren(BAbstractConnection.class)) {
            if (connection.isConnected()) {
               connection.disconnect();
            }
         }
      }
   }

   public final void doRemoveAllIdleAccepted() {
      if (this.isRunning()) {
         this.getLogger().fine("Connection container removing all child idle accepted connections.");

         for (BAcceptingConnection connection : (BAcceptingConnection[])this.getChildren(BAcceptingConnection.class)) {
            if (connection.isIdle()) {
               this.remove(connection);
            }
         }
      }
   }

   public final void started() {
      this.scheduleIdleCleanup();
   }

   public final void changed(Property property, Context context) {
      if (property.equals(enableIdleCleanup) || property.equals(idleCleanupInterval)) {
         int currentFlags = this.getFlags(idleCleanupInterval);
         if (this.getEnableIdleCleanup()) {
            this.setFlags(idleCleanupInterval, currentFlags & -2);
            if (this.getIdleCleanupInterval().getMillis() == 0L) {
               this.doCleanupIdle();
            }
         } else {
            this.setFlags(idleCleanupInterval, currentFlags | 1);
         }

         this.cancelIdleCleanup();
         this.scheduleIdleCleanup();
      }
   }

   public final void stopped() {
      this.cancelIdleCleanup();
   }

   private static boolean shouldRemove(BAcceptingConnection connection, BAbsTime thresholdTime) {
      return connection.isIdle() && (isBefore(connection.getLastDisconnect(), thresholdTime) || isBefore(connection.getLastFailureToConnect(), thresholdTime));
   }

   private static boolean isBefore(BAbsTime connectionTime, BAbsTime thresholdTime) {
      return connectionTime != null && !connectionTime.isNull() && connectionTime.isBefore(thresholdTime);
   }
}
