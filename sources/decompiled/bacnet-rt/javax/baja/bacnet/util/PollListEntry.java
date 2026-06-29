package javax.baja.bacnet.util;

import javax.baja.bacnet.BBacnetDevice;
import javax.baja.bacnet.datatypes.BBacnetObjectIdentifier;
import javax.baja.bacnet.enums.BBacnetPropertyIdentifier;
import javax.baja.bacnet.point.BBacnetProxyExt;
import javax.baja.sys.BFacets;
import javax.baja.sys.BObject;
import javax.baja.sys.BasicContext;
import javax.baja.sys.Context;
import javax.baja.user.BUser;

public class PollListEntry implements Context {
   private BBacnetObjectIdentifier objectId;
   private int propertyId;
   private int propertyArrayIndex;
   private BBacnetDevice device;
   private BIBacnetPollable pollable;
   private int dataSize = 11;
   private Context context;
   private PollList pollList;
   public static final int DEFAULT_DATASIZE = 11;
   public static final Context pointCx = new BasicContext() {
      public boolean equals(Object obj) {
         return this == obj;
      }

      public int hashCode() {
         return 1;
      }

      public String toString() {
         return "Bacnet:pointCx";
      }
   };
   public static final Context forceCx = new BasicContext() {
      public boolean equals(Object obj) {
         return this == obj;
      }

      public int hashCode() {
         return 1;
      }

      public String toString() {
         return "Bacnet:forceCx";
      }
   };
   public static final Context metaCx = new BasicContext() {
      public boolean equals(Object obj) {
         return this == obj;
      }

      public int hashCode() {
         return 1;
      }

      public String toString() {
         return "Bacnet:metaCx";
      }
   };

   public PollListEntry(BBacnetProxyExt pt) {
      this.objectId = pt.getObjectId();
      this.propertyId = pt.getPropertyId().getOrdinal();
      this.propertyArrayIndex = pt.getPropertyArrayIndex();
      this.device = pt.device();
      this.pollable = pt;
      this.context = pointCx;
   }

   public PollListEntry(BBacnetProxyExt pt, int pId) {
      this.objectId = pt.getObjectId();
      this.propertyId = pId;
      this.propertyArrayIndex = pt.getPropertyArrayIndex();
      this.device = pt.device();
      this.pollable = pt;
      this.context = null;
   }

   public PollListEntry(BBacnetObjectIdentifier oid, int pid, BBacnetDevice dev, BIBacnetPollable p) {
      this.objectId = oid;
      this.propertyId = pid;
      this.propertyArrayIndex = -1;
      this.device = dev;
      this.pollable = p;
      this.context = null;
   }

   public PollListEntry(BBacnetObjectIdentifier oid, int pid, int ndx, BBacnetDevice dev, BIBacnetPollable p) {
      this.objectId = oid;
      this.propertyId = pid;
      this.propertyArrayIndex = ndx;
      this.device = dev;
      this.pollable = p;
      this.context = null;
   }

   public PollListEntry(BBacnetObjectIdentifier oid, int pid, int ndx, BBacnetDevice dev, BIBacnetPollable p, Context cx) {
      this.objectId = oid;
      this.propertyId = pid;
      this.propertyArrayIndex = ndx;
      this.device = dev;
      this.pollable = p;
      this.context = cx;
   }

   public final BBacnetObjectIdentifier getObjectId() {
      return this.objectId;
   }

   public final int getPropertyId() {
      return this.propertyId;
   }

   public final int getPropertyArrayIndex() {
      return this.propertyArrayIndex;
   }

   public final BBacnetDevice getDevice() {
      return this.device;
   }

   public final int getAddressHash() {
      return this.device == null ? 0 : this.device.getAddress().hash();
   }

   public final BIBacnetPollable getPollable() {
      return this.pollable;
   }

   public final int getDataSize() {
      return this.dataSize;
   }

   public final void setDataSize(int dataSize) {
      this.dataSize = dataSize;
   }

   public final void doubleDataSize(int max) {
      this.dataSize = Math.min(max, this.dataSize * 2);
   }

   public final Context getContext() {
      return (Context)(this.context == null ? this : this.context);
   }

   public final void setContext(Context context) {
      this.context = context;
   }

   public final PollList getPollList() {
      return this.pollList;
   }

   public final void setPollList(PollList pollList) {
      this.pollList = pollList;
   }

   @Override
   public boolean equals(Object o) {
      if (o == null) {
         return false;
      } else if (!(o instanceof PollListEntry)) {
         return false;
      } else {
         PollListEntry ple = (PollListEntry)o;
         if (this.propertyId != ple.propertyId) {
            return false;
         } else if (this.propertyArrayIndex != ple.propertyArrayIndex) {
            return false;
         } else {
            if (this.context == null) {
               if (ple.context != null) {
                  return false;
               }
            } else if (!this.context.equals(ple.context)) {
               return false;
            }

            if (this.objectId == null) {
               if (ple.objectId != null) {
                  return false;
               }
            } else {
               if (ple.objectId == null) {
                  return false;
               }

               if (this.objectId.hashCode() != ple.objectId.hashCode()) {
                  return false;
               }
            }

            if (this.pollable == null) {
               if (ple.pollable != null) {
                  return false;
               }
            } else if (!this.pollable.equals(ple.pollable)) {
               return false;
            }

            return true;
         }
      }
   }

   @Override
   public int hashCode() {
      return 1;
   }

   @Override
   public String toString() {
      return this.string(false);
   }

   public String debugString() {
      return this.string(true);
   }

   private String string(boolean debug) {
      StringBuilder sb = new StringBuilder();
      sb.append(this.objectId.toString()).append(' ').append(BBacnetPropertyIdentifier.tag(this.propertyId));
      if (this.propertyArrayIndex >= 0) {
         sb.append(" [").append(this.propertyArrayIndex).append(']');
      }

      if (debug) {
         sb.append(" in " + this.device.getName())
            .append(" [")
            .append(this.getAddressHash())
            .append(']')
            .append(" -> " + this.pollable)
            .append(" {")
            .append(this.dataSize)
            .append("}; ")
            .append(this.context);
      }

      return sb.toString();
   }

   public Context getBase() {
      return this.context;
   }

   public BUser getUser() {
      return this.context == null ? null : this.context.getUser();
   }

   public BFacets getFacets() {
      return this.context == null ? null : this.context.getFacets();
   }

   public BObject getFacet(String name) {
      return this.context == null ? null : this.context.getFacet(name);
   }

   public String getLanguage() {
      return this.context == null ? null : this.context.getLanguage();
   }
}
