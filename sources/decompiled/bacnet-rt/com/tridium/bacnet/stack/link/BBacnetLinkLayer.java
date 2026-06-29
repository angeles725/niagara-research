package com.tridium.bacnet.stack.link;

import com.tridium.bacnet.stack.BacnetInputStream;
import com.tridium.bacnet.stack.network.BNetworkPort;
import com.tridium.bacnet.stack.network.DataAttributes;
import com.tridium.bacnet.stack.network.NetworkPdu;
import java.util.HashSet;
import java.util.Set;
import javax.baja.bacnet.BBacnetNetwork;
import javax.baja.bacnet.export.BLocalBacnetDevice;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.sys.BComponent;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;
import javax.baja.util.Lexicon;

@NiagaraType
public abstract class BBacnetLinkLayer extends BComponent {
   public static final Type TYPE = Sys.loadType(BBacnetLinkLayer.class);
   protected Set<LinkListener> linkListeners = new HashSet<>();
   private boolean linkInitialized = false;
   private boolean linkStarted = false;
   protected static Lexicon lex = Lexicon.make("bacnet");

   public Type getType() {
      return TYPE;
   }

   public final void linkInit() throws Exception {
      if (!this.linkInitialized) {
         BLocalBacnetDevice local = BBacnetNetwork.localDevice();
         local.setMaxAPDULengthAccepted(Math.min(this.getMaxAPDULengthAccepted(), local.getMaxAPDULengthAccepted()));
         this.linkCommInit();
         this.linkInitialized = true;
      }
   }

   public final void linkStart() throws Exception {
      if (!this.linkStarted) {
         if (!this.linkInitialized) {
            this.linkInit();
         }

         this.linkCommStart();
         this.linkStarted = true;
      }
   }

   public final void linkStop() throws Exception {
      this.linkCommStop();
      this.linkStarted = false;
   }

   public final void linkCleanup() throws Exception {
      this.linkCommCleanup();
      this.linkInitialized = false;
   }

   public void linkCommInit() throws Exception {
   }

   public void linkCommStart() throws Exception {
   }

   public void linkCommStop() throws Exception {
   }

   public void linkCommCleanup() throws Exception {
   }

   public final boolean isParentLegal(BComponent parent) {
      return parent instanceof BNetworkPort;
   }

   public void checkFatalFault() {
   }

   protected BNetworkPort getNetworkPort() {
      return (BNetworkPort)this.getParent();
   }

   public void addLinkListener(LinkListener linkListener) {
      this.linkListeners.add(linkListener);
   }

   public void removeLinkListener(LinkListener linkListener) {
      if (linkListener != null) {
         this.linkListeners.remove(linkListener);
      }
   }

   public abstract byte[] getMacAddress();

   public abstract int getMaxAPDULengthAccepted();

   public abstract void sendRequest(byte[] var1, NetworkPdu var2);

   public void rcvIndication(byte[] srcMacAddress, byte[] destMacAddress, BacnetInputStream is) {
      this.rcvIndication(srcMacAddress, destMacAddress, is, false);
   }

   public void rcvIndication(byte[] srcMacAddress, byte[] destMacAddress, BacnetInputStream is, boolean isBroadcast) {
      this.rcvIndication(srcMacAddress, destMacAddress, is, isBroadcast, null);
   }

   public void rcvIndication(byte[] srcMacAddress, byte[] destMacAddress, BacnetInputStream is, boolean isBroadcast, DataAttributes dataAttributes) {
      for (LinkListener listener : this.linkListeners) {
         listener.rcvIndication(srcMacAddress, destMacAddress, is, isBroadcast, dataAttributes);
      }
   }

   public void doDump() {
      System.out.println(this + ": dump");
      System.out.println("listeners:" + this.linkListeners.size());
   }
}
