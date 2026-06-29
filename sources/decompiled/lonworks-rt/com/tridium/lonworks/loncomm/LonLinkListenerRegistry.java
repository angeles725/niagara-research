package com.tridium.lonworks.loncomm;

import com.tridium.lonworks.util.NmUtil;
import java.util.Vector;
import javax.baja.lonworks.BLonNetwork;

public class LonLinkListenerRegistry {
   private Vector<LonLinkListenerRegistry.LinkListener> linkL = null;
   BLonNetwork lonworks;

   public LonLinkListenerRegistry(BLonNetwork l) {
      this.lonworks = l;
   }

   public void registerLinkListener(LonLinkListenerRegistry.LinkListener l) {
      if (this.linkL == null) {
         this.linkL = new Vector<>();
      }

      synchronized (this.linkL) {
         this.linkL.addElement(l);
      }
   }

   public void unregisterLinkListener(LonLinkListenerRegistry.LinkListener l) {
      if (this.linkL != null) {
         synchronized (this.linkL) {
            this.linkL.removeElement(l);
         }
      }
   }

   public void listenerReceive(NAppBuffer msg) {
      if (this.linkL != null) {
         synchronized (this.linkL) {
            for (int i = 0; i < this.linkL.size(); i++) {
               LonLinkListenerRegistry.LinkListener ll = this.linkL.elementAt(i);

               try {
                  ll.receive(msg);
               } catch (Throwable var7) {
                  var7.printStackTrace();
               }
            }
         }
      }
   }

   public void listenerSend(NAppBuffer msg) {
      if (this.linkL != null) {
         synchronized (this.linkL) {
            for (int i = 0; i < this.linkL.size(); i++) {
               LonLinkListenerRegistry.LinkListener ll = this.linkL.elementAt(i);

               try {
                  ll.send(msg);
               } catch (Throwable var7) {
                  var7.printStackTrace();
               }
            }
         }
      }
   }

   public void writeLinkDebug(String prefix, byte[] a, int len) {
      if ((a[0] & 15) != 6 && (a[3] & 1) != 1) {
         System.out.println();
      }

      StringBuilder sb = new StringBuilder(30 + len * 3);
      sb.append('[').append(this.lonworks.getLogName()).append(']');
      sb.append(prefix).append(NmUtil.timeStamp());
      sb.append("|");
      if (len > a.length) {
         len = a.length;
      }

      if (len > 0) {
         for (int i = 0; i < len; i++) {
            if (i == 2 || i == 5 || i == 16) {
               sb.append(' ');
            }

            if ((a[i] & 255) < 16) {
               sb.append('0');
            }

            sb.append(Integer.toString(a[i] & 255, 16)).append(' ');
         }
      }

      System.out.println(sb.toString());
   }

   public interface LinkListener {
      void receive(NAppBuffer var1);

      void send(NAppBuffer var1);
   }
}
