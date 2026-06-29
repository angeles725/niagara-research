package javax.baja.bacnet.export;

import com.tridium.bacnet.asn.AsnUtil;
import com.tridium.bacnet.asn.NErrorType;
import com.tridium.bacnet.asn.NReadPropertyResult;
import java.util.StringTokenizer;
import javax.baja.bacnet.BacnetException;
import javax.baja.bacnet.io.AsnException;
import javax.baja.bacnet.io.ErrorType;
import javax.baja.bacnet.io.OutOfRangeException;
import javax.baja.bacnet.io.PropertyValue;
import javax.baja.control.BControlPoint;
import javax.baja.control.BEnumWritable;
import javax.baja.control.enums.BPriorityLevel;
import javax.baja.nre.annotations.NiagaraProperty;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.nre.util.TextUtil;
import javax.baja.security.PermissionException;
import javax.baja.status.BStatusEnum;
import javax.baja.sys.BDynamicEnum;
import javax.baja.sys.BEnumRange;
import javax.baja.sys.BLink;
import javax.baja.sys.BObject;
import javax.baja.sys.BString;
import javax.baja.sys.BValue;
import javax.baja.sys.Context;
import javax.baja.sys.Knob;
import javax.baja.sys.Property;
import javax.baja.sys.Slot;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType
@NiagaraProperty(
   name = "bacnetWritable",
   type = "String",
   defaultValue = "BBacnetPointDescriptor.lexNotWritable",
   flags = 5
)
public abstract class BBacnetMultiStateWritableDescriptor extends BBacnetMultiStatePointDescriptor implements BacnetWritableDescriptor {
   public static final Property bacnetWritable = newProperty(5, BBacnetPointDescriptor.lexNotWritable, null);
   public static final Type TYPE = Sys.loadType(BBacnetMultiStateWritableDescriptor.class);

   @Override
   public String getBacnetWritable() {
      return this.getString(bacnetWritable);
   }

   public void setBacnetWritable(String v) {
      this.setString(bacnetWritable, v, null);
   }

   @Override
   public Type getType() {
      return TYPE;
   }

   @Override
   protected final boolean isPointTypeLegal(BControlPoint pt) {
      return pt instanceof BEnumWritable;
   }

   @Override
   protected boolean isCommandable() {
      return true;
   }

   @Override
   protected PropertyValue readProperty(int pId, int ndx) {
      BEnumWritable pt = (BEnumWritable)this.getPoint();
      if (pt == null) {
         return new NReadPropertyResult(pId, ndx, new NErrorType(1, 1000));
      } else if (ndx >= 0 && !this.isArray(pId)) {
         return new NReadPropertyResult(pId, ndx, new NErrorType(2, 50));
      } else {
         switch (pId) {
            case 87:
               return this.readPriorityArray(ndx);
            case 104:
               return new NReadPropertyResult(pId, ndx, AsnUtil.toAsnUnsigned(pt.getFallback().getValue().getOrdinal()));
            default:
               return super.readProperty(pId, ndx);
         }
      }
   }

   @Override
   protected ErrorType writeProperty(int pId, int ndx, byte[] val, int pri) throws BacnetException {
      BEnumWritable pt = (BEnumWritable)this.getPoint();
      if (pt == null) {
         return new NErrorType(1, 1000);
      } else if (ndx >= 0 && !this.isArray(pId)) {
         return new NErrorType(2, 50);
      } else {
         try {
            switch (pId) {
               case 85:
                  return this.writePriorityArray(pri, val);
               case 87:
                  return new NErrorType(2, 40);
               case 104:
                  int writeVal = AsnUtil.fromAsnUnsignedInt(val);
                  BDynamicEnum ms = pt.getFallback().getValue();
                  BEnumRange r = (BEnumRange)pt.getFacets().getFacet("range");
                  if (r != null && !r.isOrdinal(writeVal)) {
                     return new NErrorType(2, 37);
                  }

                  BStatusEnum fb = pt.getFallback();
                  fb.set(BStatusEnum.value, BDynamicEnum.make(writeVal, ms.getRange()), BLocalBacnetDevice.getBacnetContext());
                  fb.setStatusNull(false);
                  return null;
               default:
                  return super.writeProperty(pId, ndx, val, pri);
            }
         } catch (IllegalArgumentException | OutOfRangeException var10) {
            return new NErrorType(2, 37);
         } catch (AsnException var11) {
            log.warning("AsnException writing property " + pId + " in object " + this.getObjectId() + ": " + var11);
            return new NErrorType(2, 9);
         } catch (PermissionException var12) {
            log.warning("PermissionException writing property " + pId + " in object " + this.getObjectId() + ": " + var12);
            return new NErrorType(2, 40);
         }
      }
   }

   private PropertyValue readPriorityArray(int ndx) {
      BEnumWritable pt = (BEnumWritable)this.getPoint();
      if (pt == null) {
         return new NReadPropertyResult(87, ndx, new NErrorType(1, 1000));
      } else if (ndx == -1) {
         synchronized (asnOut) {
            asnOut.reset();

            for (int i = 1; i <= 16; i++) {
               BStatusEnum e = pt.getLevel(BPriorityLevel.make(i));
               if (e.getStatus().isNull()) {
                  asnOut.writeNull();
               } else {
                  asnOut.writeUnsignedInteger(e.getValue().getOrdinal());
               }
            }

            return new NReadPropertyResult(87, ndx, asnOut.toByteArray());
         }
      } else if (ndx == 0) {
         return new NReadPropertyResult(87, ndx, AsnUtil.toAsnUnsigned(16L));
      } else {
         try {
            BStatusEnum e = pt.getLevel(BPriorityLevel.make(ndx));
            return e.getStatus().isNull()
               ? new NReadPropertyResult(87, ndx, AsnUtil.toAsnNull())
               : new NReadPropertyResult(87, ndx, AsnUtil.toAsnUnsigned(e.getValue().getOrdinal()));
         } catch (Exception var8) {
            return new NReadPropertyResult(87, ndx, new NErrorType(2, 42));
         }
      }
   }

   private ErrorType writePriorityArray(int pri, byte[] val) throws BacnetException {
      BEnumWritable pt = (BEnumWritable)this.getPoint();
      if (pt == null) {
         return new NErrorType(1, 1000);
      } else {
         try {
            if (pri == -1) {
               pri = 16;
            }

            if (pri >= 1 && pri <= 16) {
               String inSlotName = "bacnetValueIn" + pri;
               Property inSlot = this.loadSlots().getProperty(inSlotName);
               if (inSlot == null) {
                  return new NErrorType(2, 40);
               } else {
                  BStatusEnum bacval = (BStatusEnum)this.get(inSlotName).newCopy();
                  synchronized (asnIn) {
                     asnIn.setBuffer(val);
                     int tag = asnIn.peekTag();
                     if (tag == 0) {
                        BOutOfServiceExt outOfServiceExt = this.getOosExt();
                        if (outOfServiceExt.getOutOfService()) {
                           return new NErrorType(2, 37);
                        }

                        bacval.setStatusNull(true);
                     } else {
                        if (tag != 2) {
                           throw new AsnException("Invalid tag: " + tag);
                        }

                        bacval.setStatusNull(false);
                        int writeVal = asnIn.readUnsignedInt();
                        if (writeVal <= 0) {
                           return new NErrorType(2, 37);
                        }

                        BEnumRange range = (BEnumRange)pt.getFacets().getFacet("range");
                        if (range != null && !range.isOrdinal(writeVal)) {
                           return new NErrorType(2, 37);
                        }

                        BEnumRange outRange = pt.getOut().getValue().getRange();
                        BOutOfServiceExt outOfServiceExt = this.getOosExt();
                        if (outOfServiceExt.getOutOfService()) {
                           outOfServiceExt.set(BOutOfServiceExt.presentValue, BDynamicEnum.make(writeVal, outRange), BLocalBacnetDevice.getBacnetContext());
                        }

                        bacval.setValue(outRange.get(writeVal));
                     }
                  }

                  this.set(inSlot, bacval, BLocalBacnetDevice.getBacnetContext());
                  return null;
               }
            } else {
               return new NErrorType(5, 80);
            }
         } catch (IllegalArgumentException var15) {
            log.warning("IllegalArgumentException writing priorityArray in object " + this.getObjectId() + ": " + var15);
            return new NErrorType(2, 37);
         } catch (PermissionException var16) {
            log.warning("PermissionException writing priorityArray in object " + this.getObjectId() + ": " + var16);
            return new NErrorType(2, 40);
         }
      }
   }

   private void resetBacnetWritable() {
      StringBuilder sb = new StringBuilder();
      Knob[] knobs = this.getKnobs();

      for (int i = 0; i < knobs.length; i++) {
         BObject tgt = knobs[i].getTargetOrd().get(this);
         BObject pt = this.getPoint();
         if (knobs[i].getTargetSlotName().startsWith("in") && tgt == pt) {
            sb.append(knobs[i].getTargetSlotName()).append(',');
         }
      }

      this.setBacnetWritable(sb.length() > 0 ? sb.substring(0, sb.length() - 1) : lexNotWritable);
   }

   public void knobAdded(Knob knob, Context cx) {
      this.resetBacnetWritable();
   }

   public void knobRemoved(Knob knob, Context cx) {
      this.resetBacnetWritable();
   }

   @Override
   public final void doMakeWritable(BValue writable) {
      if (this.isRunning()) {
         BEnumWritable pt = (BEnumWritable)this.getPoint();
         if (pt != null) {
            BLink[] links = pt.getLinks();

            for (int i = 0; i < links.length; i++) {
               if (links[i].isActive()) {
                  if (links[i].getSourceComponent() == this && links[i].getTargetSlot().getName().startsWith("in")) {
                     pt.remove(links[i]);
                  }
               } else {
                  pt.remove(links[i]);
               }
            }

            BStatusEnum[] bacnetValues = (BStatusEnum[])this.getChildren(BStatusEnum.class);

            for (int ix = 0; ix < bacnetValues.length; ix++) {
               if (bacnetValues[ix].getName().startsWith("bacnetValueIn")) {
                  this.remove(bacnetValues[ix]);
               }
            }

            String s = ((BString)writable).getString();
            if (!s.equals(lexNotWritable)) {
               StringTokenizer st = new StringTokenizer(s, ",");

               while (st.hasMoreTokens()) {
                  String tgtSlotName = st.nextToken();
                  Slot tgtSlot = pt.getSlot(tgtSlotName);
                  String srcSlotName = "bacnetValue" + TextUtil.capitalize(tgtSlotName);
                  BStatusEnum sf = new BStatusEnum();
                  sf.setStatusNull(true);
                  this.add(srcSlotName, sf, 257);
                  pt.setFlags(tgtSlot, pt.getFlags(tgtSlot) | 1024);
                  BLink link = new BLink(this.getHandleOrd(), srcSlotName, tgtSlotName, true);
                  pt.add("bacnet" + tgtSlotName, link, 1);
               }
            }
         }
      }
   }
}
