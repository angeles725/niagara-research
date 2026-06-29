package javax.baja.bacnet.export;

import com.tridium.bacnet.asn.AsnUtil;
import com.tridium.bacnet.asn.NErrorType;
import com.tridium.bacnet.asn.NReadPropertyResult;
import java.util.StringTokenizer;
import java.util.Vector;
import javax.baja.alarm.ext.BAlarmSourceExt;
import javax.baja.bacnet.BacnetException;
import javax.baja.bacnet.enums.BBacnetPropertyIdentifier;
import javax.baja.bacnet.io.AsnException;
import javax.baja.bacnet.io.ErrorType;
import javax.baja.bacnet.io.PropertyValue;
import javax.baja.bacnet.util.BacnetBitStringUtil;
import javax.baja.control.BControlPoint;
import javax.baja.control.BNumericWritable;
import javax.baja.control.enums.BPriorityLevel;
import javax.baja.nre.annotations.NiagaraProperty;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.nre.util.TextUtil;
import javax.baja.security.PermissionException;
import javax.baja.status.BStatusNumeric;
import javax.baja.sys.BDouble;
import javax.baja.sys.BLink;
import javax.baja.sys.BNumber;
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
   defaultValue = "BBacnetAnalogPointDescriptor.lexNotWritable",
   flags = 5
)
public abstract class BBacnetAnalogWritableDescriptor extends BBacnetAnalogPointDescriptor implements BacnetWritableDescriptor {
   public static final Property bacnetWritable = newProperty(5, BBacnetAnalogPointDescriptor.lexNotWritable, null);
   public static final Type TYPE = Sys.loadType(BBacnetAnalogWritableDescriptor.class);

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
      return pt instanceof BNumericWritable;
   }

   @Override
   protected boolean isCommandable() {
      return true;
   }

   protected boolean commandabilityRequired() {
      return true;
   }

   @Override
   protected PropertyValue readProperty(int pId, int ndx) {
      BNumericWritable pt = (BNumericWritable)this.getPoint();
      if (pt == null) {
         return new NReadPropertyResult(pId, ndx, new NErrorType(1, 1000));
      } else if (ndx >= 0 && !this.isArray(pId)) {
         return new NReadPropertyResult(pId, ndx, new NErrorType(2, 50));
      } else {
         switch (pId) {
            case 87:
               return this.readPriorityArray(ndx);
            case 104:
               byte[] asn = this.convertToAsn(pt.getFallback().getValue());
               return new NReadPropertyResult(pId, ndx, asn);
            default:
               return super.readProperty(pId, ndx);
         }
      }
   }

   @Override
   protected ErrorType writeProperty(int pId, int ndx, byte[] val, int pri) throws BacnetException {
      BNumericWritable pt = (BNumericWritable)this.getPoint();
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
                  BNumber nmin = (BNumber)this.getPoint().getFacets().getFacet("min");
                  BNumber nmax = (BNumber)this.getPoint().getFacets().getFacet("max");
                  double min = nmin != null ? nmin.getDouble() : Double.NEGATIVE_INFINITY;
                  double max = nmax != null ? nmax.getDouble() : Double.POSITIVE_INFINITY;
                  double real = Double.NaN;
                  real = this.convertFromAsn(val);
                  if (!(real < min) && !(real > max)) {
                     BStatusNumeric fb = pt.getFallback();
                     fb.setDouble(BStatusNumeric.value, real, BLocalBacnetDevice.getBacnetContext());
                     fb.setStatusNull(false);
                     return null;
                  }

                  return new NErrorType(2, 37);
            }
         } catch (AsnException var15) {
            log.warning("AsnException writing property " + pId + " in object " + this.getObjectId() + ": " + var15);
            return new NErrorType(2, 9);
         } catch (PermissionException var16) {
            log.warning("PermissionException writing property " + pId + " in object " + this.getObjectId() + ": " + var16);
            return new NErrorType(2, 40);
         }

         return super.writeProperty(pId, ndx, val, pri);
      }
   }

   @Override
   protected void addRequiredProps(Vector v) {
      super.addRequiredProps(v);
      if (this.commandabilityRequired()) {
         v.add(BBacnetPropertyIdentifier.priorityArray);
         v.add(BBacnetPropertyIdentifier.relinquishDefault);
      }
   }

   @Override
   protected void addOptionalProps(Vector v) {
      super.addOptionalProps(v);
      if (!this.commandabilityRequired()) {
         v.add(BBacnetPropertyIdentifier.priorityArray);
         v.add(BBacnetPropertyIdentifier.relinquishDefault);
      }
   }

   protected PropertyValue readPriorityArray(int ndx) {
      BNumericWritable pt = (BNumericWritable)this.getPoint();
      if (pt == null) {
         return new NReadPropertyResult(87, ndx, new NErrorType(1, 1000));
      } else if (ndx == -1) {
         synchronized (asnOut) {
            asnOut.reset();

            for (int i = 1; i <= 16; i++) {
               BStatusNumeric e = pt.getLevel(BPriorityLevel.make(i));
               if (e.getStatus().isNull()) {
                  asnOut.writeNull();
               } else {
                  this.appendToAsn(asnOut, e.getValue());
               }
            }

            return new NReadPropertyResult(87, ndx, asnOut.toByteArray());
         }
      } else if (ndx == 0) {
         return new NReadPropertyResult(87, ndx, AsnUtil.toAsnUnsigned(16L));
      } else {
         try {
            BStatusNumeric e = pt.getLevel(BPriorityLevel.make(ndx));
            return e.getStatus().isNull()
               ? new NReadPropertyResult(87, ndx, AsnUtil.toAsnNull())
               : new NReadPropertyResult(87, ndx, this.convertToAsn(e.getValue()));
         } catch (Exception var8) {
            return new NReadPropertyResult(87, ndx, new NErrorType(2, 42));
         }
      }
   }

   private ErrorType writePriorityArray(int pri, byte[] val) throws BacnetException {
      BNumericWritable pt = (BNumericWritable)this.getPoint();
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
                  BStatusNumeric bacval = (BStatusNumeric)this.get(inSlot).newCopy();
                  BNumber nmin = (BNumber)pt.getFacets().getFacet("min");
                  BNumber nmax = (BNumber)pt.getFacets().getFacet("max");
                  double min = nmin != null ? nmin.getDouble() : Double.NEGATIVE_INFINITY;
                  double max = nmax != null ? nmax.getDouble() : Double.POSITIVE_INFINITY;
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
                        if (tag != this.asnType()) {
                           throw new AsnException("Invalid tag: " + tag);
                        }

                        double real = this.readFromAsn(asnIn);
                        if (real < min || real > max) {
                           return new NErrorType(2, 37);
                        }

                        BOutOfServiceExt outOfServiceExt = this.getOosExt();
                        if (outOfServiceExt.getOutOfService()) {
                           outOfServiceExt.set(BOutOfServiceExt.presentValue, BDouble.make(real), BLocalBacnetDevice.getBacnetContext());
                        }

                        bacval.setStatusNull(false);
                        bacval.setValue(real);
                     }
                  }

                  this.set(inSlot, bacval, BLocalBacnetDevice.getBacnetContext());
                  return null;
               }
            } else {
               return new NErrorType(5, 80);
            }
         } catch (IllegalArgumentException var20) {
            log.warning("IllegalArgumentException writing priorityArray in object " + this.getObjectId() + ": " + var20);
            return new NErrorType(2, 37);
         } catch (PermissionException var21) {
            log.warning("PermissionException writing priorityArray in object " + this.getObjectId() + ": " + var21);
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
         BNumericWritable pt = (BNumericWritable)this.getPoint();
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

            BStatusNumeric[] bacnetValues = (BStatusNumeric[])this.getChildren(BStatusNumeric.class);

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
                  BValue value = pt.get(tgtSlotName).newCopy();
                  String srcSlotName = "bacnetValue" + TextUtil.capitalize(tgtSlotName);
                  BStatusNumeric sf = new BStatusNumeric();
                  sf.setStatusNull(true);
                  this.add(srcSlotName, sf, 257);
                  pt.setFlags(tgtSlot, pt.getFlags(tgtSlot) | 1024);
                  BLink link = new BLink(this.getHandleOrd(), srcSlotName, tgtSlotName, true);
                  pt.add("bacnet" + tgtSlotName, link, 1);
                  pt.set(tgtSlotName, value);
               }
            }
         }
      }
   }

   @Override
   protected ErrorType writeOptionalProperty(int pId, int ndx, byte[] val, int pri) throws BacnetException {
      switch (pId) {
         case 35:
            BAlarmSourceExt almExt = this.getAlarmExt();
            if (almExt != null) {
               almExt.set(
                  BAlarmSourceExt.alarmEnable,
                  BacnetBitStringUtil.getBAlarmTransitionBits(AsnUtil.fromAsnBitString(val)),
                  BLocalBacnetDevice.getBacnetContext()
               );
               return null;
            }
         default:
            return super.writeOptionalProperty(pId, ndx, val, pri);
      }
   }
}
