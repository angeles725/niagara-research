package com.tridium.bacnet.datatypes;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.Arrays;
import java.util.StringTokenizer;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.sys.BObject;
import javax.baja.sys.BSimple;
import javax.baja.sys.Context;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType
public final class BDiscoveryNetworks extends BSimple {
   public static final BDiscoveryNetworks DEFAULT = new BDiscoveryNetworks(true, new int[0]);
   public static final Type TYPE = Sys.loadType(BDiscoveryNetworks.class);
   private boolean all;
   private int[] networks;

   private BDiscoveryNetworks(boolean all, int[] nets) {
      this.all = all;
      if (nets == null) {
         this.networks = new int[0];
      } else {
         this.networks = new int[nets.length];
         System.arraycopy(nets, 0, this.networks, 0, nets.length);
         Arrays.sort(this.networks);
      }
   }

   public static BDiscoveryNetworks make(boolean all, int[] networks) {
      return new BDiscoveryNetworks(all, networks);
   }

   public boolean isAllNetworks() {
      return this.all;
   }

   public int[] getNetworks() {
      int[] n = new int[this.networks.length];
      System.arraycopy(this.networks, 0, n, 0, n.length);
      return n;
   }

   public boolean equals(Object o) {
      if (o == null) {
         return false;
      } else if (o instanceof BDiscoveryNetworks) {
         BDiscoveryNetworks n = (BDiscoveryNetworks)o;
         if (n.all != this.all) {
            return false;
         } else {
            int len = this.networks.length;
            if (n.networks.length != len) {
               return false;
            } else {
               for (int i = 0; i < len; i++) {
                  if (n.networks[i] != this.networks[i]) {
                     return false;
                  }
               }

               return true;
            }
         }
      } else {
         return false;
      }
   }

   public int hashCode() {
      return 0;
   }

   public String toString(Context cx) {
      StringBuilder sb = new StringBuilder();
      sb.append(this.all ? "all;" : "-;");
      int len = this.networks.length;

      for (int i = 0; i < len; i++) {
         sb.append(this.networks[i]).append(',');
      }

      return sb.substring(0, sb.length() - 1);
   }

   public void encode(DataOutput out) throws IOException {
      out.writeBoolean(this.all);
      int len = this.networks.length;
      out.writeInt(len);

      for (int i = 0; i < len; i++) {
         out.writeInt(this.networks[i]);
      }
   }

   public BObject decode(DataInput in) throws IOException {
      boolean all = in.readBoolean();
      int len = in.readInt();
      int[] nets = new int[len];

      for (int i = 0; i < len; i++) {
         nets[i] = in.readInt();
      }

      return new BDiscoveryNetworks(all, nets);
   }

   public String encodeToString() throws IOException {
      return this.toString(null);
   }

   public BObject decodeFromString(String s) throws IOException {
      boolean all = s.startsWith("all");
      int ndx = s.indexOf(";");
      int[] nets = null;
      if (ndx >= 0) {
         StringTokenizer st = new StringTokenizer(s.substring(ndx + 1), ",");
         nets = new int[st.countTokens()];
         int count = 0;

         while (st.hasMoreTokens()) {
            nets[count++] = Integer.parseInt(st.nextToken());
         }
      }

      return new BDiscoveryNetworks(all, nets);
   }

   public boolean contains(int networkNumber) {
      for (int i = 0; i < this.networks.length; i++) {
         if (this.networks[i] == networkNumber) {
            return true;
         }
      }

      return false;
   }

   public Type getType() {
      return TYPE;
   }
}
