package javax.baja.bacnet.export;

import com.tridium.bacnet.asn.AsnOutputStream;
import com.tridium.bacnet.asn.AsnUtil;
import com.tridium.bacnet.asn.NErrorType;
import com.tridium.bacnet.asn.NReadPropertyResult;
import java.util.StringTokenizer;
import java.util.Vector;
import javax.baja.alarm.BIAlarmSource;
import javax.baja.alarm.ext.BAlarmSourceExt;
import javax.baja.alarm.ext.offnormal.BStringChangeOfStateAlgorithm;
import javax.baja.bacnet.BacnetException;
import javax.baja.bacnet.datatypes.BBacnetObjectIdentifier;
import javax.baja.bacnet.datatypes.BBacnetOptionalCharacterString;
import javax.baja.bacnet.enums.BBacnetEventType;
import javax.baja.bacnet.enums.BBacnetPropertyIdentifier;
import javax.baja.bacnet.enums.BBacnetReliability;
import javax.baja.bacnet.io.AsnException;
import javax.baja.bacnet.io.ErrorType;
import javax.baja.bacnet.io.PropertyValue;
import javax.baja.control.BControlPoint;
import javax.baja.control.BStringPoint;
import javax.baja.control.BStringWritable;
import javax.baja.control.enums.BPriorityLevel;
import javax.baja.nre.annotations.AgentOn;
import javax.baja.nre.annotations.NiagaraProperties;
import javax.baja.nre.annotations.NiagaraProperty;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.nre.util.TextUtil;
import javax.baja.security.PermissionException;
import javax.baja.status.BStatus;
import javax.baja.status.BStatusString;
import javax.baja.status.BStatusValue;
import javax.baja.sys.BEnum;
import javax.baja.sys.BIcon;
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

@NiagaraType(
   agent = {@AgentOn(
      types = {"control:StringWritable"}
   )}
)
@NiagaraProperties({@NiagaraProperty(
      name = "objectId",
      type = "BBacnetObjectIdentifier",
      defaultValue = "BBacnetObjectIdentifier.make(BBacnetObjectType.CHARACTER_STRING_VALUE)",
      flags = 64,
      override = true
   ), @NiagaraProperty(
      name = "bacnetWritable",
      type = "String",
      defaultValue = "BBacnetPointDescriptor.lexNotWritable",
      flags = 5
   )})
public class BBacnetCharacterStringDescriptor extends BBacnetPointDescriptor implements BacnetWritableDescriptor {
   public static final Property objectId = newProperty(64, BBacnetObjectIdentifier.make(40), null);
   public static final Property bacnetWritable = newProperty(5, BBacnetPointDescriptor.lexNotWritable, null);
   public static final Type TYPE = Sys.loadType(BBacnetCharacterStringDescriptor.class);
   private static final BIcon icon = BIcon.make(BIcon.std("control/stringPoint.png"), BIcon.std("badges/export.png"));

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
   protected boolean isPointTypeLegal(BControlPoint pt) {
      return pt instanceof BStringPoint;
   }

   @Override
   public BEnum getEventType() {
      return BBacnetEventType.changeOfCharacterstring;
   }

   @Override
   public boolean isValidAlarmExt(BIAlarmSource ext) {
      return ext instanceof BAlarmSourceExt ? ((BAlarmSourceExt)ext).getOffnormalAlgorithm() instanceof BStringChangeOfStateAlgorithm : false;
   }

   @Override
   protected boolean isCommandable() {
      return true;
   }

   @Override
   protected PropertyValue readProperty(int pId, int ndx) {
      BStringWritable pt = (BStringWritable)this.getPoint();
      if (pt == null) {
         return new NReadPropertyResult(pId, ndx, new NErrorType(1, 1000));
      } else if (ndx >= 0 && !this.isArray(pId)) {
         return new NReadPropertyResult(pId, ndx, new NErrorType(2, 50));
      } else {
         switch (pId) {
            case 85:
               return new NReadPropertyResult(pId, ndx, AsnUtil.toAsnCharacterString(pt.getOut().getValue()));
            case 87:
               return this.readPriorityArray(ndx);
            case 104:
               return new NReadPropertyResult(pId, ndx, AsnUtil.toAsnCharacterString(pt.getFallback().getValue()));
            default:
               return super.readProperty(pId, ndx);
         }
      }
   }

   @Override
   protected PropertyValue readOptionalProperty(int pId, int ndx) {
      switch (pId) {
         case 7:
            BAlarmSourceExt alarmExt = this.getAlarmExt();
            if (alarmExt != null && alarmExt.getOffnormalAlgorithm() instanceof BStringChangeOfStateAlgorithm) {
               String alarmExpression = ((BStringChangeOfStateAlgorithm)alarmExt.getOffnormalAlgorithm()).getExpression();
               AsnOutputStream asnOut = new AsnOutputStream();
               new BBacnetOptionalCharacterString(alarmExpression).writeAsn(asnOut);
               byte[] val = asnOut.toByteArray();
               return new NReadPropertyResult(pId, ndx, val);
            }

            return new NReadPropertyResult(pId, ndx, new NErrorType(2, 32));
         default:
            return super.readOptionalProperty(pId, ndx);
      }
   }

   @Override
   protected byte[] makeInterfaceValue(BStatusValue proxyValue) {
      throw new UnsupportedOperationException("BACnet Characterstring object does not have an interface value property");
   }

   @Override
   protected ErrorType writeProperty(int pId, int ndx, byte[] val, int pri) throws BacnetException {
      BStringWritable pt = (BStringWritable)this.getPoint();
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
                  BStatusString fb = pt.getFallback();
                  fb.setString(BStatusString.value, AsnUtil.fromAsnCharacterString(val), BLocalBacnetDevice.getBacnetContext());
                  fb.setStatusNull(false);
                  return null;
               default:
                  return super.writeProperty(pId, ndx, val, pri);
            }
         } catch (AsnException var7) {
            log.warning("AsnException writing property " + pId + " in object " + this.getObjectId() + ": " + var7);
            return new NErrorType(2, 9);
         } catch (PermissionException var8) {
            log.warning("PermissionException writing property " + pId + " in object " + this.getObjectId() + ": " + var8);
            return new NErrorType(2, 40);
         }
      }
   }

   @Override
   protected ErrorType writeOptionalProperty(int pId, int ndx, byte[] val, int pri) throws BacnetException {
      switch (pId) {
         case 7:
            if (this.getAlarmExt() != null) {
               return new NErrorType(2, 40);
            }

            return new NErrorType(2, 32);
         default:
            return super.writeOptionalProperty(pId, ndx, val, pri);
      }
   }

   @Override
   protected void addRequiredProps(Vector v) {
      super.addRequiredProps(v);
      v.add(BBacnetPropertyIdentifier.presentValue);
      v.add(BBacnetPropertyIdentifier.statusFlags);
   }

   @Override
   protected void addOptionalProps(Vector v) {
      super.addOptionalProps(v);
      v.add(BBacnetPropertyIdentifier.eventState);
      v.add(BBacnetPropertyIdentifier.outOfService);
      v.add(BBacnetPropertyIdentifier.priorityArray);
      v.add(BBacnetPropertyIdentifier.relinquishDefault);
      if (this.getAlarmExt() != null) {
         v.add(BBacnetPropertyIdentifier.alarmValues);
      }
   }

   @Override
   protected void validate() {
      BStatusString sn = ((BStringPoint)this.getPoint()).getOut();
      BStatus s = sn.getStatus();
      if (s.isNull()) {
         this.setReliability(BBacnetReliability.unreliableOther);
         this.setFaultCause("Invalid value for BACnet Object:" + sn);
         this.setStatus(BStatus.makeFault(this.getStatus(), true));
      } else if (s.isFault()) {
         this.setReliability(BBacnetReliability.unreliableOther);
      } else if (s.isDown()) {
         this.setReliability(BBacnetReliability.communicationFailure);
      } else {
         this.setReliability(BBacnetReliability.noFaultDetected);
         if (this.configOk()) {
            this.setStatus(BStatus.makeFault(this.getStatus(), false));
            this.setFaultCause("");
         } else {
            this.setStatus(BStatus.makeFault(this.getStatus(), true));
            this.setFaultCause(lex.getText("export.configurationFault"));
         }
      }
   }

   private ErrorType writePriorityArray(int pri, byte[] val) throws BacnetException {
      BStringWritable pt = (BStringWritable)this.getPoint();
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
                  BStatusString bacval = (BStatusString)this.get(inSlot).newCopy();
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
                        if (tag != 7) {
                           throw new AsnException("Invalid tag: " + tag);
                        }

                        String characterString = asnIn.readCharacterString();
                        BOutOfServiceExt outOfServiceExt = this.getOosExt();
                        if (outOfServiceExt.getOutOfService()) {
                           outOfServiceExt.set(BOutOfServiceExt.presentValue, BString.make(characterString), BLocalBacnetDevice.getBacnetContext());
                        }

                        bacval.setStatusNull(false);
                        bacval.setValue(characterString);
                     }
                  }

                  this.set(inSlot, bacval, BLocalBacnetDevice.getBacnetContext());
                  return null;
               }
            } else {
               return new NErrorType(5, 80);
            }
         } catch (IllegalArgumentException var13) {
            log.warning("IllegalArgumentException writing priorityArray in object " + this.getObjectId() + ": " + var13);
            return new NErrorType(2, 37);
         } catch (PermissionException var14) {
            log.warning("PermissionException writing priorityArray in object " + this.getObjectId() + ": " + var14);
            return new NErrorType(2, 40);
         }
      }
   }

   @Override
   BStatusValue getCurrentStatusValue() {
      BStatusValue sv = new BStatusString(((BStringPoint)this.getPoint()).getOut().getValue());
      sv.setStatus(this.getStatusFlags());
      return sv;
   }

   @Override
   public boolean checkCov(BStatusValue currentValue, BStatusValue covValue) {
      return !currentValue.toString().equals(covValue.toString());
   }

   private PropertyValue readPriorityArray(int ndx) {
      BStringWritable pt = (BStringWritable)this.getPoint();
      if (pt == null) {
         return new NReadPropertyResult(87, ndx, new NErrorType(1, 1000));
      } else if (ndx == -1) {
         synchronized (asnOut) {
            asnOut.reset();

            for (int i = 1; i <= 16; i++) {
               BStatusString e = pt.getLevel(BPriorityLevel.make(i));
               if (e.getStatus().isNull()) {
                  asnOut.writeNull();
               } else {
                  asnOut.writeCharacterString(e.getValue());
               }
            }

            return new NReadPropertyResult(87, ndx, asnOut.toByteArray());
         }
      } else if (ndx == 0) {
         return new NReadPropertyResult(87, ndx, AsnUtil.toAsnUnsigned(16L));
      } else {
         try {
            BStatusString e = pt.getLevel(BPriorityLevel.make(ndx));
            return e.getStatus().isNull()
               ? new NReadPropertyResult(87, ndx, AsnUtil.toAsnNull())
               : new NReadPropertyResult(87, ndx, AsnUtil.toAsnCharacterString(e.getValue()));
         } catch (Exception var8) {
            return new NReadPropertyResult(87, ndx, new NErrorType(2, 42));
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
         BControlPoint point = this.getPoint();
         if (point instanceof BStringWritable) {
            BLink[] links = point.getLinks();

            for (int i = 0; i < links.length; i++) {
               if (links[i].isActive()) {
                  if (links[i].getSourceComponent() == this && links[i].getTargetSlot().getName().startsWith("in")) {
                     point.remove(links[i]);
                  }
               } else {
                  point.remove(links[i]);
               }
            }

            BStatusString[] bacnetValues = (BStatusString[])this.getChildren(BStatusString.class);

            for (int ix = 0; ix < bacnetValues.length; ix++) {
               if (bacnetValues[ix].getName().startsWith("bacnetValueIn")) {
                  this.remove(bacnetValues[ix]);
               }
            }

            String s = ((BString)writable).getString();
            if (s.equals(lexNotWritable)) {
               return;
            }

            StringTokenizer st = new StringTokenizer(s, ",");

            while (st.hasMoreTokens()) {
               String tgtSlotName = st.nextToken();
               Slot tgtSlot = point.getSlot(tgtSlotName);
               String srcSlotName = "bacnetValue" + TextUtil.capitalize(tgtSlotName);
               BStatusString sf = new BStatusString();
               sf.setStatusNull(true);
               this.add(srcSlotName, sf, 257);
               point.setFlags(tgtSlot, point.getFlags(tgtSlot) | 1024);
               BLink link = new BLink(this.getHandleOrd(), srcSlotName, tgtSlotName, true);
               point.add("bacnet" + tgtSlotName, link, 1);
            }
         }
      }
   }

   public BIcon getIcon() {
      return icon;
   }
}
