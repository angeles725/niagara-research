package com.tridium.fox.sys;

import com.tridium.fox.session.FoxSession;
import com.tridium.fox.session.InvalidChannelException;
import com.tridium.fox.sys.data.BDataChannel;
import com.tridium.fox.sys.file.BFileChannel;
import com.tridium.fox.sys.spy.BSpyChannel;
import com.tridium.fox.sys.user.BUserChannel;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.sys.BComponent;
import javax.baja.sys.BIcon;
import javax.baja.sys.Property;
import javax.baja.sys.SlotCursor;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType
public class BFoxChannelRegistry extends BComponent {
   public static final Type TYPE = Sys.loadType(BFoxChannelRegistry.class);
   private static final BFoxChannelRegistry prototype = new BFoxChannelRegistry();
   private static final BIcon icon;
   private static final Logger LOG;

   public Type getType() {
      return TYPE;
   }

   public final BSysChannel getSysChannel() {
      return (BSysChannel)this.get("sys");
   }

   public final BFileChannel getFileChannel() {
      return (BFileChannel)this.get("file");
   }

   public final BUserChannel getUserChannel() {
      return (BUserChannel)this.get("user");
   }

   public final BFoxChannel get(String name, Type channelType) {
      BFoxChannel channel = (BFoxChannel)this.get(name);
      if (channel != null) {
         if (channel.getType() != channelType) {
            throw new ClassCastException("Type mismatch: " + channel.getType() + " != " + channelType);
         }
      } else {
         channel = (BFoxChannel)channelType.getInstance();
         this.add(name, channel, 2);
         if (((BFoxConnection)this.getParent()).isConnected()) {
            this.sessionOpened(channel);
         }
      }

      return channel;
   }

   public static BFoxChannelRegistry getPrototype() {
      return prototype;
   }

   public void sessionOpened() {
      SlotCursor<Property> c = this.getProperties();

      while (c.nextComponent()) {
         this.sessionOpened((BFoxChannel)c.get());
      }
   }

   public void sessionClosed(Throwable cause) {
      SlotCursor<Property> c = this.getProperties();

      while (c.nextComponent()) {
         sessionClosed((BFoxChannel)c.get(), cause);
      }
   }

   private void sessionOpened(BFoxChannel channel) {
      FoxSession session = null;

      try {
         session = ((BFoxConnection)this.getParent()).session();
         channel.log.setSession(session);
         channel.fwSessionOpened();
         channel.sessionOpened();
      } catch (Throwable var7) {
         Level level = var7 instanceof InvalidChannelException ? Level.FINE : Level.SEVERE;
         if (LOG.isLoggable(level)) {
            StringBuilder sb = new StringBuilder();
            if (channel != null && channel.getName() != null) {
               sb.append(" of channel '").append(channel.getName()).append('\'');
            }

            if (session != null && session.getRemoteHello() != null) {
               String remoteStationName = session.getRemoteHello().getString("station.name", null);
               if (remoteStationName != null) {
                  sb.append(" for target station \"").append(remoteStationName).append('"');
               }
            }

            LOG.log(level, "Exception occurred in sessionOpen" + sb, var7);
         }
      }
   }

   private static void sessionClosed(BFoxChannel channel, Throwable cause) {
      try {
         channel.sessionClosed(cause);
      } catch (Throwable var3) {
         LOG.log(Level.SEVERE, "Exception occurred in sessionClosed", var3);
      }

      channel.fwSessionClosed(cause);
      channel.log.setSession(null);
   }

   public BIcon getIcon() {
      return icon;
   }

   static {
      prototype.add("data", new BDataChannel());
      prototype.add("file", new BFileChannel());
      prototype.add("spy", new BSpyChannel());
      prototype.add("sys", new BSysChannel());
      prototype.add("user", new BUserChannel());

      try {
         prototype.add("alarmui", (BFoxChannel)Sys.newInstance("alarm", "com.tridium.alarm.BAlarmConsoleChannel"));
      } catch (Throwable var1) {
      }

      icon = BIcon.std("book.png");
      LOG = Logger.getLogger("fox.channel");
   }
}
