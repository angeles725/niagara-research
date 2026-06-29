package javax.baja.bacnet.export;

import com.tridium.bacnet.asn.AsnInputStream;
import com.tridium.bacnet.asn.AsnOutputStream;
import com.tridium.bacnet.asn.AsnUtil;
import com.tridium.bacnet.asn.NErrorType;
import com.tridium.bacnet.asn.NReadPropertyResult;
import java.util.Vector;
import javax.baja.alarm.BIAlarmSource;
import javax.baja.alarm.ext.BAlarmSourceExt;
import javax.baja.alarm.ext.fault.BOutOfRangeFaultAlgorithm;
import javax.baja.alarm.ext.offnormal.BOutOfRangeAlgorithm;
import javax.baja.bacnet.BacnetException;
import javax.baja.bacnet.datatypes.BBacnetBitString;
import javax.baja.bacnet.datatypes.BBacnetCovSubscription;
import javax.baja.bacnet.enums.BBacnetEngineeringUnits;
import javax.baja.bacnet.enums.BBacnetEventType;
import javax.baja.bacnet.enums.BBacnetPropertyIdentifier;
import javax.baja.bacnet.enums.BBacnetReliability;
import javax.baja.bacnet.io.AsnException;
import javax.baja.bacnet.io.ErrorType;
import javax.baja.bacnet.io.PropertyValue;
import javax.baja.bacnet.util.BacnetBitStringUtil;
import javax.baja.control.BControlPoint;
import javax.baja.control.BNumericPoint;
import javax.baja.nre.annotations.NiagaraProperty;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.security.PermissionException;
import javax.baja.status.BStatus;
import javax.baja.status.BStatusNumeric;
import javax.baja.status.BStatusValue;
import javax.baja.sys.BDouble;
import javax.baja.sys.BEnum;
import javax.baja.sys.BINumeric;
import javax.baja.sys.BIcon;
import javax.baja.sys.BNumber;
import javax.baja.sys.Property;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;
import javax.baja.units.BUnit;

@NiagaraType
@NiagaraProperty(
   name = "covIncrement",
   type = "double",
   defaultValue = "1.0"
)
public abstract class BBacnetAnalogPointDescriptor extends BBacnetPointDescriptor {
   public static final Property covIncrement = newProperty(0, 1.0, null);
   public static final Type TYPE = Sys.loadType(BBacnetAnalogPointDescriptor.class);
   private static final BIcon icon = BIcon.make(BIcon.std("control/numericPoint.png"), BIcon.std("badges/export.png"));

   public double getCovIncrement() {
      return this.getDouble(covIncrement);
   }

   public void setCovIncrement(double v) {
      this.setDouble(covIncrement, v, null);
   }

   @Override
   public Type getType() {
      return TYPE;
   }

   @Override
   protected boolean isPointTypeLegal(BControlPoint pt) {
      return pt instanceof BNumericPoint;
   }

   @Override
   public BEnum getEventType() {
      return BBacnetEventType.outOfRange;
   }

   @Override
   public boolean isValidAlarmExt(BIAlarmSource ext) {
      return ext instanceof BAlarmSourceExt ? ((BAlarmSourceExt)ext).getOffnormalAlgorithm() instanceof BOutOfRangeAlgorithm : false;
   }

   public int asnType() {
      return 4;
   }

   public double getDeadBandValue(byte[] value) throws AsnException {
      return this.convertFromAsn(value);
   }

   public byte[] getDeadBandBytes(double value) {
      return this.convertToAsn(value);
   }

   public byte[] convertToAsn(double value) {
      return AsnUtil.toAsnReal(value);
   }

   public double convertFromAsn(byte[] value) throws AsnException {
      return AsnUtil.fromAsnReal(value);
   }

   public void appendToAsn(AsnOutputStream out, double value) {
      out.writeReal(value);
   }

   public double readFromAsn(AsnInputStream in) throws AsnException {
      return in.readReal();
   }

   @Override
   protected PropertyValue readProperty(int pId, int ndx) {
      BNumericPoint pt = (BNumericPoint)this.getPoint();
      if (pt == null) {
         return new NReadPropertyResult(pId, ndx, new NErrorType(1, 1000));
      } else if (ndx >= 0 && !this.isArray(pId)) {
         return new NReadPropertyResult(pId, ndx, new NErrorType(2, 50));
      } else {
         switch (pId) {
            case 22:
               return new NReadPropertyResult(pId, ndx, this.convertToAsn(this.getCovIncrement()));
            case 85:
               return new NReadPropertyResult(pId, ndx, this.convertToAsn(pt.getOut().getValue()));
            case 117:
               BUnit u = (BUnit)pt.getFacets().getFacet("units");
               if (u != null) {
                  return new NReadPropertyResult(pId, ndx, AsnUtil.toAsnEnumerated(BBacnetEngineeringUnits.make(u)));
               }

               return new NReadPropertyResult(pId, ndx, AsnUtil.toAsnEnumerated(95));
            default:
               return super.readProperty(pId, ndx);
         }
      }
   }

   @Override
   protected ErrorType writeProperty(int pId, int ndx, byte[] val, int pri) throws BacnetException {
      BNumericPoint pt = (BNumericPoint)this.getPoint();
      if (pt == null) {
         return new NErrorType(1, 1000);
      } else if (ndx >= 0 && !this.isArray(pId)) {
         return new NErrorType(2, 50);
      } else {
         try {
            switch (pId) {
               case 22:
                  BNumber nmin = (BNumber)this.getPoint().getFacets().getFacet("min");
                  BNumber nmax = (BNumber)this.getPoint().getFacets().getFacet("max");
                  double min = nmin != null ? nmin.getDouble() : Double.NEGATIVE_INFINITY;
                  double max = nmax != null ? nmax.getDouble() : Double.POSITIVE_INFINITY;
                  double inc = this.getCovIncrement(val);
                  if (!(inc < 0.0) && !(inc > max - min)) {
                     this.set(covIncrement, BDouble.make(inc), BLocalBacnetDevice.getBacnetContext());
                     this.checkCov();
                     return null;
                  }

                  return new NErrorType(2, 37);
               case 117:
                  return new NErrorType(2, 40);
               default:
                  return super.writeProperty(pId, ndx, val, pri);
            }
         } catch (AsnException var14) {
            log.warning("AsnException writing property " + pId + " in object " + this.getObjectId() + ": " + var14);
            return new NErrorType(2, 9);
         } catch (PermissionException var15) {
            log.warning("PermissionException writing property " + pId + " in object " + this.getObjectId() + ": " + var15);
            return new NErrorType(2, 40);
         }
      }
   }

   protected double getCovIncrement(byte[] value) throws AsnException {
      return this.convertFromAsn(value);
   }

   @Override
   protected void addRequiredProps(Vector v) {
      super.addRequiredProps(v);
      v.add(BBacnetPropertyIdentifier.presentValue);
      v.add(BBacnetPropertyIdentifier.statusFlags);
      v.add(BBacnetPropertyIdentifier.eventState);
      v.add(BBacnetPropertyIdentifier.outOfService);
      v.add(BBacnetPropertyIdentifier.units);
   }

   @Override
   protected void addOptionalProps(Vector v) {
      super.addOptionalProps(v);
      BAlarmSourceExt almExt = this.getAlarmExt();
      if (almExt != null) {
         v.add(BBacnetPropertyIdentifier.highLimit);
         v.add(BBacnetPropertyIdentifier.lowLimit);
         v.add(BBacnetPropertyIdentifier.deadband);
         v.add(BBacnetPropertyIdentifier.limitEnable);
      }

      BControlPoint pt = this.getPoint();
      BNumber min = (BNumber)pt.getFacets().getFacet("min");
      if (min != null) {
         v.add(BBacnetPropertyIdentifier.minPresValue);
      }

      BNumber max = (BNumber)pt.getFacets().getFacet("max");
      if (max != null) {
         v.add(BBacnetPropertyIdentifier.maxPresValue);
      }

      BNumber prec = (BNumber)pt.getFacets().getFacet("precision");
      if (prec != null) {
         v.add(BBacnetPropertyIdentifier.resolution);
      }

      v.add(BBacnetPropertyIdentifier.covIncrement);
   }

   @Override
   protected PropertyValue readOptionalProperty(int pId, int ndx) {
      BAlarmSourceExt almExt = this.getAlarmExt();
      if (almExt != null) {
         BOutOfRangeAlgorithm alg = (BOutOfRangeAlgorithm)almExt.getOffnormalAlgorithm();
         switch (pId) {
            case 25:
               return new NReadPropertyResult(pId, ndx, this.getDeadBandBytes(alg.getDeadband()));
            case 45:
               return new NReadPropertyResult(pId, ndx, this.convertToAsn(alg.getHighLimit()));
            case 52:
               return new NReadPropertyResult(pId, ndx, AsnUtil.toAsnBitString(BacnetBitStringUtil.getBacnetLimitEnable(alg.getLimitEnable())));
            case 59:
               return new NReadPropertyResult(pId, ndx, this.convertToAsn(alg.getLowLimit()));
         }
      }

      BNumericPoint pt = (BNumericPoint)this.getPoint();
      if (pt == null) {
         return new NReadPropertyResult(pId, ndx, new NErrorType(1, 1000));
      } else {
         switch (pId) {
            case 65:
               BNumber max = (BNumber)pt.getFacets().getFacet("max");
               if (max != null) {
                  return new NReadPropertyResult(pId, ndx, this.convertToAsn(max.getDouble()));
               }
               break;
            case 69:
               BNumber min = (BNumber)pt.getFacets().getFacet("min");
               if (min != null) {
                  return new NReadPropertyResult(pId, ndx, this.convertToAsn(min.getDouble()));
               }
               break;
            case 106:
               BNumber prec = (BNumber)pt.getFacets().getFacet("precision");
               if (prec != null) {
                  return new NReadPropertyResult(pId, ndx, this.convertToAsn(Math.pow(10.0, -prec.getFloat())));
               }
         }

         return super.readOptionalProperty(pId, ndx);
      }
   }

   @Override
   protected byte[] makeInterfaceValue(BStatusValue proxyValue) {
      return AsnUtil.toAsnReal(((BStatusNumeric)proxyValue).getValue());
   }

   @Override
   protected ErrorType writeOptionalProperty(int pId, int ndx, byte[] val, int pri) throws BacnetException {
      BAlarmSourceExt almExt = this.getAlarmExt();

      try {
         if (almExt != null) {
            BOutOfRangeAlgorithm alg = (BOutOfRangeAlgorithm)almExt.getOffnormalAlgorithm();
            BNumber nmin = (BNumber)this.getPoint().getFacets().getFacet("min");
            BNumber nmax = (BNumber)this.getPoint().getFacets().getFacet("max");
            double min = nmin != null ? nmin.getDouble() : Double.NEGATIVE_INFINITY;
            double max = nmax != null ? nmax.getDouble() : Double.POSITIVE_INFINITY;
            switch (pId) {
               case 25:
                  double db = this.getDeadBandValue(val);
                  double hi = alg.getHighLimit();
                  double lo = alg.getLowLimit();
                  if (!(db < 0.0) && !(db > hi - lo)) {
                     alg.setDouble(BOutOfRangeAlgorithm.deadband, db, BLocalBacnetDevice.getBacnetContext());
                     return null;
                  }

                  return new NErrorType(2, 37);
               case 45:
                  double hl = this.convertFromAsn(val);
                  if (!(hl < min) && !(hl > max)) {
                     alg.setDouble(BOutOfRangeAlgorithm.highLimit, this.convertFromAsn(val), BLocalBacnetDevice.getBacnetContext());
                     return null;
                  }

                  return new NErrorType(2, 37);
               case 52:
                  BBacnetBitString le = AsnUtil.fromAsnBitString(val);
                  alg.set(BOutOfRangeAlgorithm.limitEnable, BacnetBitStringUtil.getBLimitEnable(le), BLocalBacnetDevice.getBacnetContext());
                  return null;
               case 59:
                  double ll = this.convertFromAsn(val);
                  if (!(ll < min) && !(ll > max)) {
                     alg.setDouble(BOutOfRangeAlgorithm.lowLimit, this.convertFromAsn(val), BLocalBacnetDevice.getBacnetContext());
                     return null;
                  }

                  return new NErrorType(2, 37);
            }
         }

         switch (pId) {
            case 65:
               BNumber max = (BNumber)this.getPoint().getFacets().getFacet("max");
               if (max != null) {
                  return new NErrorType(2, 40);
               }
               break;
            case 69:
               BNumber min = (BNumber)this.getPoint().getFacets().getFacet("min");
               if (min != null) {
                  return new NErrorType(2, 40);
               }
               break;
            case 106:
               BNumber prec = (BNumber)this.getPoint().getFacets().getFacet("precision");
               if (prec != null) {
                  return new NErrorType(2, 40);
               }
         }

         return super.writeOptionalProperty(pId, ndx, val, pri);
      } catch (AsnException var24) {
         log.warning("AsnException writing property " + pId + " in object " + this.getObjectId() + ": " + var24);
         return new NErrorType(2, 9);
      } catch (PermissionException var25) {
         log.warning("PermissionException writing property " + pId + " in object " + this.getObjectId() + ": " + var25);
         return new NErrorType(2, 40);
      }
   }

   @Override
   protected void validate() {
      BStatusNumeric sn = ((BNumericPoint)this.getPoint()).getOut();
      BStatus s = sn.getStatus();
      if (s.isNull()) {
         this.setReliability(BBacnetReliability.unreliableOther);
         this.setFaultCause("Invalid value for BACnet Object:" + sn);
         this.setStatus(BStatus.makeFault(this.getStatus(), true));
      } else if (s.isFault()) {
         BAlarmSourceExt alarmExt = this.getAlarmExt();
         if (alarmExt != null && alarmExt.getFaultAlgorithm() instanceof BOutOfRangeFaultAlgorithm) {
            BOutOfRangeFaultAlgorithm outOfRange = (BOutOfRangeFaultAlgorithm)alarmExt.getFaultAlgorithm();
            double pointValue = ((BNumericPoint)this.getPoint()).getNumeric();
            if (pointValue < outOfRange.getLowLimit()) {
               this.setReliability(BBacnetReliability.underRange);
            } else if (pointValue > outOfRange.getHighLimit()) {
               this.setReliability(BBacnetReliability.overRange);
            } else {
               this.setReliability(BBacnetReliability.unreliableOther);
            }
         } else {
            this.setReliability(BBacnetReliability.unreliableOther);
         }
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
      BStatusValue sv = new BStatusNumeric(((BNumericPoint)this.getPoint()).getOut().getValue());
      sv.setStatus(this.getStatusFlags());
      return sv;
   }

   @Override
   boolean checkCov(BStatusValue currentValue, BStatusValue covValue) {
      if (currentValue.getStatus().getBits() != covValue.getStatus().getBits()) {
         return true;
      } else {
         double cur = ((BStatusNumeric)currentValue).getNumeric();
         double lst = ((BStatusNumeric)covValue).getNumeric();
         if (Double.isNaN(cur)) {
            return !Double.isNaN(lst);
         } else {
            return Double.isNaN(lst)
               ? true
               : Math.abs(((BStatusNumeric)currentValue).getNumeric() - ((BStatusNumeric)covValue).getNumeric()) >= this.getCovIncrement();
         }
      }
   }

   @Deprecated
   boolean checkCov(BControlPoint pt, BBacnetCovSubscription covSub) {
      if (pt.getStatus().getBits() != covSub.getLastValue().getStatus().getBits()) {
         return true;
      } else {
         double currentValue = ((BNumericPoint)pt).getNumeric();
         double covValue = ((BINumeric)covSub.getLastValue()).getNumeric();
         return Math.abs(currentValue - covValue) >= this.getCovIncrement();
      }
   }

   public BIcon getIcon() {
      return icon;
   }
}
