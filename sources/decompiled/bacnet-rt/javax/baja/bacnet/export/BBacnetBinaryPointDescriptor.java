package javax.baja.bacnet.export;

import com.tridium.bacnet.asn.AsnUtil;
import com.tridium.bacnet.asn.NErrorType;
import com.tridium.bacnet.asn.NReadPropertyResult;
import java.util.Vector;
import java.util.logging.Level;
import javax.baja.bacnet.BacnetException;
import javax.baja.bacnet.datatypes.BBacnetCovSubscription;
import javax.baja.bacnet.datatypes.BBacnetDate;
import javax.baja.bacnet.datatypes.BBacnetDateTime;
import javax.baja.bacnet.datatypes.BBacnetTime;
import javax.baja.bacnet.enums.BBacnetPropertyIdentifier;
import javax.baja.bacnet.enums.BBacnetReliability;
import javax.baja.bacnet.io.AsnException;
import javax.baja.bacnet.io.ErrorType;
import javax.baja.bacnet.io.OutOfRangeException;
import javax.baja.bacnet.io.PropertyValue;
import javax.baja.control.BBooleanPoint;
import javax.baja.control.BControlPoint;
import javax.baja.control.ext.BAbstractProxyExt;
import javax.baja.control.ext.BDiscreteTotalizerExt;
import javax.baja.driver.point.BDefaultProxyConversion;
import javax.baja.driver.point.BProxyExt;
import javax.baja.driver.point.conv.BReversePolarityConversion;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.security.PermissionException;
import javax.baja.status.BStatus;
import javax.baja.status.BStatusBoolean;
import javax.baja.status.BStatusValue;
import javax.baja.sys.BFacets;
import javax.baja.sys.BIBoolean;
import javax.baja.sys.BIcon;
import javax.baja.sys.BRelTime;
import javax.baja.sys.BString;
import javax.baja.sys.Property;
import javax.baja.sys.SlotCursor;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;
import javax.baja.util.BFormat;

@NiagaraType
public abstract class BBacnetBinaryPointDescriptor extends BBacnetPointDescriptor {
   public static final Type TYPE = Sys.loadType(BBacnetBinaryPointDescriptor.class);
   private static final BIcon icon = BIcon.make(BIcon.std("control/booleanPoint.png"), BIcon.std("badges/export.png"));

   @Override
   public Type getType() {
      return TYPE;
   }

   @Override
   protected boolean isPointTypeLegal(BControlPoint pt) {
      return pt instanceof BBooleanPoint;
   }

   @Override
   protected PropertyValue readOptionalProperty(int pId, int ndx) {
      BDiscreteTotalizerExt totExt = this.getTotalizerExt();
      if (totExt != null) {
         switch (pId) {
            case 15:
               return new NReadPropertyResult(pId, ndx, AsnUtil.toAsnUnsigned(totExt.getChangeOfStateCount()));
            case 16:
               return new NReadPropertyResult(pId, ndx, AsnUtil.toBacnetDateTime(totExt.getChangeOfStateTime()));
            case 33:
               return new NReadPropertyResult(pId, ndx, AsnUtil.toAsnUnsigned(totExt.getElapsedActiveTime().getSeconds()));
            case 114:
               return new NReadPropertyResult(pId, ndx, AsnUtil.toBacnetDateTime(totExt.getTimeOfActiveTimeReset()));
            case 115:
               return new NReadPropertyResult(pId, ndx, AsnUtil.toBacnetDateTime(totExt.getTimeOfStateCountReset()));
         }
      }

      BBooleanPoint pt = (BBooleanPoint)this.getPoint();
      switch (pId) {
         case 4:
            BString tt = (BString)pt.getFacets().getFacet("trueText");
            if (tt != null) {
               String trueText = BFormat.format(tt.toString(), null, null);
               return new NReadPropertyResult(pId, ndx, AsnUtil.toAsnCharacterString(trueText));
            }
            break;
         case 46:
            BString ft = (BString)pt.getFacets().getFacet("falseText");
            if (ft != null) {
               String falseText = BFormat.format(ft.toString(), null, null);
               return new NReadPropertyResult(pId, ndx, AsnUtil.toAsnCharacterString(falseText));
            }
      }

      return super.readOptionalProperty(pId, ndx);
   }

   @Override
   protected byte[] makeInterfaceValue(BStatusValue proxyValue) {
      return AsnUtil.toAsnEnumerated(((BStatusBoolean)proxyValue).getValue());
   }

   @Override
   protected ErrorType writeOptionalProperty(int pId, int ndx, byte[] val, int pri) throws BacnetException {
      BDiscreteTotalizerExt totExt = this.getTotalizerExt();

      try {
         if (totExt != null) {
            switch (pId) {
               case 15:
                  long changeOfStateCount = AsnUtil.fromAsnUnsignedInteger(val);
                  if (changeOfStateCount == 0L) {
                     totExt.invoke(BDiscreteTotalizerExt.resetChangeOfStateCount, null, BLocalBacnetDevice.getBacnetContext());
                  } else {
                     if (changeOfStateCount > 2147483647L) {
                        return new NErrorType(2, 37);
                     }

                     totExt.setInt(BDiscreteTotalizerExt.changeOfStateCount, (int)changeOfStateCount, BLocalBacnetDevice.getBacnetContext());
                  }

                  return null;
               case 16:
                  return new NErrorType(2, 40);
               case 33:
                  long elapsedActiveTime = AsnUtil.fromAsnUnsignedInteger(val);
                  if (elapsedActiveTime == 0L) {
                     totExt.invoke(BDiscreteTotalizerExt.resetElapsedActiveTime, null, BLocalBacnetDevice.getBacnetContext());
                  } else {
                     if (elapsedActiveTime > 2147483647L) {
                        return new NErrorType(2, 37);
                     }

                     totExt.set(BDiscreteTotalizerExt.elapsedActiveTime, BRelTime.makeSeconds((int)elapsedActiveTime), BLocalBacnetDevice.getBacnetContext());
                  }

                  return null;
               case 114:
                  BBacnetDateTime timeOfActiveTimeReset = new BBacnetDateTime();
                  AsnUtil.fromAsn(-4, val, timeOfActiveTimeReset);
                  checkDateTime(timeOfActiveTimeReset);
                  totExt.set(BDiscreteTotalizerExt.timeOfActiveTimeReset, timeOfActiveTimeReset.toBAbsTime(), BLocalBacnetDevice.getBacnetContext());
                  return null;
               case 115:
                  BBacnetDateTime timeOfStateCountReset = new BBacnetDateTime();
                  AsnUtil.fromAsn(-4, val, timeOfStateCountReset);
                  checkDateTime(timeOfStateCountReset);
                  totExt.set(BDiscreteTotalizerExt.timeOfStateCountReset, timeOfStateCountReset.toBAbsTime(), BLocalBacnetDevice.getBacnetContext());
                  return null;
            }
         }

         BBooleanPoint pt = (BBooleanPoint)this.getPoint();
         switch (pId) {
            case 4:
               BString tt = (BString)pt.getFacets().getFacet("trueText");
               if (tt != null) {
                  pt.set(
                     BControlPoint.facets,
                     BFacets.make(pt.getFacets(), "trueText", BString.make(AsnUtil.fromAsnCharacterString(val))),
                     BLocalBacnetDevice.getBacnetContext()
                  );
                  return null;
               }
               break;
            case 46:
               BString ft = (BString)pt.getFacets().getFacet("falseText");
               if (ft != null) {
                  pt.set(
                     BControlPoint.facets,
                     BFacets.make(pt.getFacets(), "falseText", BString.make(AsnUtil.fromAsnCharacterString(val))),
                     BLocalBacnetDevice.getBacnetContext()
                  );
                  return null;
               }
         }

         return super.writeOptionalProperty(pId, ndx, val, pri);
      } catch (OutOfRangeException var12) {
         log.warning("OutOfRangeException writing property " + pId + " in object " + this.getObjectId() + ": " + var12);
         return new NErrorType(2, 37);
      } catch (AsnException var13) {
         log.warning("AsnException writing property " + pId + " in object " + this.getObjectId() + ": " + var13);
         return new NErrorType(2, 9);
      } catch (PermissionException var14) {
         log.warning("PermissionException writing property " + pId + " in object " + this.getObjectId() + ": " + var14);
         return new NErrorType(2, 40);
      }
   }

   private static void checkDateTime(BBacnetDateTime dateTime) throws OutOfRangeException {
      BBacnetDate date = dateTime.getDate();
      if (!date.isYearUnspecified() && !date.isMonthUnspecified() && !date.isMonthSpecial() && !date.isDayOfMonthUnspecified() && !date.isDayOfMonthSpecial()) {
         BBacnetTime time = dateTime.getTime();
         if (time.isHourUnspecified() || time.isMinuteUnspecified()) {
            throw new OutOfRangeException("Time contains unspecified values for the hour or minute: " + dateTime);
         }
      } else {
         throw new OutOfRangeException("Date contains unspecified or special values for the year, month, or day-of-month: " + dateTime);
      }
   }

   @Override
   protected void addRequiredProps(Vector v) {
      super.addRequiredProps(v);
      v.add(BBacnetPropertyIdentifier.presentValue);
      v.add(BBacnetPropertyIdentifier.statusFlags);
      v.add(BBacnetPropertyIdentifier.eventState);
      v.add(BBacnetPropertyIdentifier.outOfService);
   }

   @Override
   protected void addOptionalProps(Vector v) {
      super.addOptionalProps(v);
      BControlPoint pt = this.getPoint();
      BString tt = (BString)pt.getFacets().getFacet("trueText");
      if (tt != null) {
         v.add(BBacnetPropertyIdentifier.activeText);
      }

      BString ft = (BString)pt.getFacets().getFacet("falseText");
      if (ft != null) {
         v.add(BBacnetPropertyIdentifier.inactiveText);
      }

      BDiscreteTotalizerExt tot = this.getTotalizerExt();
      if (tot != null) {
         v.add(BBacnetPropertyIdentifier.changeOfStateTime);
         v.add(BBacnetPropertyIdentifier.changeOfStateCount);
         v.add(BBacnetPropertyIdentifier.timeOfStateCountReset);
         v.add(BBacnetPropertyIdentifier.elapsedActiveTime);
         v.add(BBacnetPropertyIdentifier.timeOfActiveTimeReset);
      }
   }

   @Override
   protected void validate() {
      BStatusBoolean sb = ((BBooleanPoint)this.getPoint()).getOut();
      BStatus s = sb.getStatus();
      if (s.isNull()) {
         this.setReliability(BBacnetReliability.unreliableOther);
         this.setFaultCause("Invalid value for BACnet Object:" + sb);
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

   @Override
   BStatusValue getCurrentStatusValue() {
      BStatusValue sv = new BStatusBoolean(((BBooleanPoint)this.getPoint()).getOut().getValue());
      sv.setStatus(this.getStatusFlags());
      return sv;
   }

   @Override
   boolean checkCov(BStatusValue currentValue, BStatusValue covValue) {
      return currentValue.getStatus().getBits() != covValue.getStatus().getBits()
         ? true
         : ((BStatusBoolean)currentValue).getBoolean() != ((BStatusBoolean)covValue).getBoolean();
   }

   @Deprecated
   boolean checkCov(BControlPoint pt, BBacnetCovSubscription covSub) {
      if (pt.getStatus().getBits() != covSub.getLastValue().getStatus().getBits()) {
         return true;
      } else {
         boolean currentValue = ((BBooleanPoint)pt).getBoolean();
         boolean covValue = ((BIBoolean)covSub.getLastValue()).getBoolean();
         return currentValue != covValue;
      }
   }

   private BDiscreteTotalizerExt getTotalizerExt() {
      BControlPoint pt = this.getPoint();
      if (pt == null) {
         return null;
      } else {
         SlotCursor<Property> c = pt.getProperties();
         return c.next(BDiscreteTotalizerExt.class) ? (BDiscreteTotalizerExt)c.get() : null;
      }
   }

   PropertyValue readPolarityProperty(BBooleanPoint pt) {
      BAbstractProxyExt proxyExt = pt.getProxyExt();
      return proxyExt instanceof BProxyExt && ((BProxyExt)proxyExt).getConversion() instanceof BReversePolarityConversion
         ? new NReadPropertyResult(84, AsnUtil.toAsnEnumerated(1))
         : new NReadPropertyResult(84, AsnUtil.toAsnEnumerated(0));
   }

   protected ErrorType writePolarityProperty(BBooleanPoint pt, byte[] val) throws BacnetException {
      BAbstractProxyExt proxyExt = pt.getProxyExt();
      if (proxyExt instanceof BProxyExt) {
         if (AsnUtil.fromAsnEnumerated(val) == 1) {
            ((BProxyExt)proxyExt).setConversion(BReversePolarityConversion.DEFAULT);
         } else {
            ((BProxyExt)proxyExt).setConversion(BDefaultProxyConversion.DEFAULT);
         }

         return null;
      } else {
         if (log.isLoggable(Level.FINE)) {
            log.fine(
               "Cannot write the Polarity property when the associated point's proxy ext is not instanceof BProxyExt; object ID: "
                  + this.getObjectId()
                  + ", object name: "
                  + this.getObjectName()
            );
         }

         return new NErrorType(2, 40);
      }
   }

   public BIcon getIcon() {
      return icon;
   }
}
