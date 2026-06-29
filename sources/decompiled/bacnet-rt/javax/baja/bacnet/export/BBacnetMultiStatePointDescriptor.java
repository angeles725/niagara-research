package javax.baja.bacnet.export;

import com.tridium.bacnet.asn.AsnOutputStream;
import com.tridium.bacnet.asn.AsnUtil;
import com.tridium.bacnet.asn.NErrorType;
import com.tridium.bacnet.asn.NReadPropertyResult;
import com.tridium.bacnet.services.BacnetConfirmedRequest;
import com.tridium.bacnet.services.confirmed.ReadRangeAck;
import com.tridium.bacnet.services.error.NChangeListError;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Vector;
import javax.baja.alarm.ext.BAlarmSourceExt;
import javax.baja.alarm.ext.fault.BEnumFaultAlgorithm;
import javax.baja.alarm.ext.offnormal.BEnumChangeOfStateAlgorithm;
import javax.baja.bacnet.BBacnetNetwork;
import javax.baja.bacnet.BacnetException;
import javax.baja.bacnet.datatypes.BBacnetBitString;
import javax.baja.bacnet.datatypes.BBacnetCovSubscription;
import javax.baja.bacnet.datatypes.BBacnetUnsigned;
import javax.baja.bacnet.enums.BBacnetErrorClass;
import javax.baja.bacnet.enums.BBacnetErrorCode;
import javax.baja.bacnet.enums.BBacnetPropertyIdentifier;
import javax.baja.bacnet.enums.BBacnetReliability;
import javax.baja.bacnet.io.AsnException;
import javax.baja.bacnet.io.ChangeListError;
import javax.baja.bacnet.io.ErrorType;
import javax.baja.bacnet.io.PropertyValue;
import javax.baja.bacnet.io.RangeData;
import javax.baja.bacnet.io.RangeReference;
import javax.baja.bacnet.io.RejectException;
import javax.baja.bacnet.util.EnumRangeWrapper;
import javax.baja.control.BControlPoint;
import javax.baja.control.BEnumPoint;
import javax.baja.naming.SlotPath;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.nre.util.Array;
import javax.baja.security.PermissionException;
import javax.baja.status.BStatus;
import javax.baja.status.BStatusEnum;
import javax.baja.status.BStatusValue;
import javax.baja.sys.BEnumRange;
import javax.baja.sys.BFacets;
import javax.baja.sys.BIEnum;
import javax.baja.sys.BIcon;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType
public abstract class BBacnetMultiStatePointDescriptor extends BBacnetPointDescriptor {
   public static final Type TYPE = Sys.loadType(BBacnetMultiStatePointDescriptor.class);
   private static final BIcon icon = BIcon.make(BIcon.std("control/enumPoint.png"), BIcon.std("badges/export.png"));

   @Override
   public Type getType() {
      return TYPE;
   }

   @Override
   protected boolean isPointTypeLegal(BControlPoint pt) {
      return pt instanceof BEnumPoint;
   }

   @Override
   protected PropertyValue readProperty(int pId, int ndx) {
      BEnumPoint pt = (BEnumPoint)this.getPoint();
      if (pt == null) {
         return new NReadPropertyResult(pId, ndx, new NErrorType(1, 1000));
      } else if (ndx >= 0 && !this.isArray(pId)) {
         return new NReadPropertyResult(pId, ndx, new NErrorType(2, 50));
      } else {
         switch (pId) {
            case 74:
               BEnumRange r = (BEnumRange)pt.getFacets().getFacet("range");
               if (r != null) {
                  return new NReadPropertyResult(pId, ndx, AsnUtil.toAsnUnsigned(r.getOrdinals().length));
               }

               return new NReadPropertyResult(pId, ndx, AsnUtil.toAsnUnsigned(2147483647L));
            case 85:
               return new NReadPropertyResult(pId, ndx, AsnUtil.toAsnUnsigned(pt.getOut().getValue().getOrdinal()));
            case 110:
               return this.readStateText(ndx);
            default:
               return super.readProperty(pId, ndx);
         }
      }
   }

   @Override
   protected byte[] makeInterfaceValue(BStatusValue proxyValue) {
      return AsnUtil.toAsnUnsigned(((BStatusEnum)proxyValue).getValue());
   }

   @Override
   public RangeData readRange(RangeReference rangeReference) throws RejectException {
      int propertyId = rangeReference.getPropertyId();
      if (!this.hasProperty(propertyId)) {
         return new ReadRangeAck(BBacnetErrorClass.property, BBacnetErrorCode.unknownProperty);
      } else if (propertyId != 7 && propertyId != 39) {
         return new ReadRangeAck(BBacnetErrorClass.services, BBacnetErrorCode.propertyIsNotA_List);
      } else if (rangeReference.getPropertyArrayIndex() != -1) {
         return new ReadRangeAck(BBacnetErrorClass.property, BBacnetErrorCode.propertyIsNotAnArray);
      } else {
         int rangeType = rangeReference.getRangeType();
         switch (rangeType) {
            case -1:
            case 3:
               BAlarmSourceExt almExt = this.getAlarmExt();
               switch (propertyId) {
                  case 7:
                     if (almExt != null) {
                        BEnumChangeOfStateAlgorithm alg = (BEnumChangeOfStateAlgorithm)almExt.getOffnormalAlgorithm();
                        int[] ordinals = alg.getAlarmValues().getOrdinals();
                        Integer[] avals = new Integer[ordinals.length];

                        for (int i = 0; i < avals.length; i++) {
                           avals[i] = ordinals[i];
                        }

                        return this.readRange(rangeReference, avals, 5);
                     }
                     break;
                  case 39:
                     if (!BBacnetNetwork.bacnet().setAndGetShouldSupportFaults()) {
                        return new ReadRangeAck(2, 32);
                     }

                     if (almExt != null) {
                        BEnumFaultAlgorithm alg = (BEnumFaultAlgorithm)almExt.getFaultAlgorithm();
                        int[] validVals = alg.getValidValues().getOrdinals();
                        BEnumRange r = (BEnumRange)this.getPoint().getFacets().getFacet("range");
                        int[] rangeVals = r.getOrdinals();
                        Array<Integer> a = new Array(Integer.class);

                        for (int i = 0; i < rangeVals.length; i++) {
                           boolean valid = false;

                           for (int j = 0; j < validVals.length; j++) {
                              if (rangeVals[i] == validVals[j]) {
                                 valid = true;
                                 break;
                              }
                           }

                           if (!valid) {
                              a.add(rangeVals[i]);
                           }
                        }

                        Integer[] fvals = (Integer[])a.trim();
                        return this.readRange(rangeReference, fvals, 5);
                     }
               }

               return new ReadRangeAck(2, 32);
            case 0:
            case 1:
            case 2:
            case 4:
            case 5:
            default:
               return new ReadRangeAck(BBacnetErrorClass.services, BBacnetErrorCode.parameterOutOfRange);
            case 6:
               return new ReadRangeAck(BBacnetErrorClass.property, BBacnetErrorCode.listItemNotNumbered);
            case 7:
               return new ReadRangeAck(BBacnetErrorClass.property, BBacnetErrorCode.listItemNotTimestamped);
         }
      }
   }

   @Override
   public ChangeListError addListElements(PropertyValue propertyValue) throws BacnetException {
      int propertyId = propertyValue.getPropertyId();
      if (!this.hasProperty(propertyId)) {
         return BacnetDescriptorUtil.makeAddListElementError(BBacnetErrorClass.property, BBacnetErrorCode.unknownProperty);
      } else if (propertyId != 7 && propertyId != 39) {
         return BacnetDescriptorUtil.makeAddListElementError(BBacnetErrorClass.services, BBacnetErrorCode.propertyIsNotA_List);
      } else if (propertyValue.getPropertyArrayIndex() != -1) {
         return BacnetDescriptorUtil.makeAddListElementError(BBacnetErrorClass.property, BBacnetErrorCode.propertyIsNotAnArray);
      } else {
         BAlarmSourceExt almExt = this.getAlarmExt();
         if (almExt == null) {
            return new NChangeListError(8, new NErrorType(1, 1000), 0L);
         } else {
            BEnumChangeOfStateAlgorithm enumChangeOfStateAlgorithm = (BEnumChangeOfStateAlgorithm)almExt.getOffnormalAlgorithm();
            synchronized (asnIn) {
               switch (propertyId) {
                  case 7:
                     return this.addAlarmValues(propertyValue, enumChangeOfStateAlgorithm);
                  case 39:
                     return BacnetDescriptorUtil.makeAddListElementError(BBacnetErrorClass.property, BBacnetErrorCode.writeAccessDenied);
                  default:
                     return BacnetDescriptorUtil.makeAddListElementError(BBacnetErrorClass.property, BBacnetErrorCode.unknownProperty);
               }
            }
         }
      }
   }

   @Override
   public ChangeListError removeListElements(PropertyValue propertyValue) throws BacnetException {
      int propertyId = propertyValue.getPropertyId();
      if (!this.hasProperty(propertyId)) {
         return BacnetDescriptorUtil.makeRemoveListElementError(BBacnetErrorClass.property, BBacnetErrorCode.unknownProperty);
      } else if (propertyId != 7 && propertyId != 39) {
         return BacnetDescriptorUtil.makeRemoveListElementError(BBacnetErrorClass.services, BBacnetErrorCode.propertyIsNotA_List);
      } else if (propertyValue.getPropertyArrayIndex() != -1) {
         return BacnetDescriptorUtil.makeRemoveListElementError(BBacnetErrorClass.property, BBacnetErrorCode.propertyIsNotAnArray);
      } else {
         BAlarmSourceExt almExt = this.getAlarmExt();
         if (almExt == null) {
            return new NChangeListError(9, new NErrorType(1, 1000), 0L);
         } else {
            BEnumChangeOfStateAlgorithm enumChangeOfStateAlgorithm = (BEnumChangeOfStateAlgorithm)almExt.getOffnormalAlgorithm();
            synchronized (asnIn) {
               switch (propertyId) {
                  case 7:
                     return this.removeAlarmValues(propertyValue, enumChangeOfStateAlgorithm);
                  case 39:
                     return BacnetDescriptorUtil.makeRemoveListElementError(BBacnetErrorClass.property, BBacnetErrorCode.writeAccessDenied);
                  default:
                     return BacnetDescriptorUtil.makeRemoveListElementError(BBacnetErrorClass.property, BBacnetErrorCode.unknownProperty);
               }
            }
         }
      }
   }

   private ChangeListError addAlarmValues(PropertyValue propertyValue, BEnumChangeOfStateAlgorithm alg) {
      ArrayList<BBacnetUnsigned> v = new ArrayList<>();
      int ffen = 1;

      try {
         synchronized (asnIn) {
            asnIn.setBuffer(propertyValue.getPropertyValue());

            for (int tag = asnIn.peekTag(); tag != -1; tag = asnIn.peekTag()) {
               v.add(asnIn.readUnsigned());
               ffen++;
            }
         }
      } catch (AsnException var16) {
         return new NChangeListError(8, new NErrorType(2, 9), ffen);
      }

      BEnumRange r = (BEnumRange)this.getPoint().getFacets().getFacet("range");
      BEnumRange almVals = alg.getAlarmValues();
      Array<BBacnetUnsigned> a = new Array(BBacnetUnsigned.class);
      int[] ordinals = almVals.getOrdinals();

      for (int i = 0; i < ordinals.length; i++) {
         a.add(BBacnetUnsigned.make(ordinals[i]));
      }

      try {
         for (int i = 0; i < v.size(); i++) {
            BBacnetUnsigned u = v.get(i);
            int newOrdinal = u.getInt();
            boolean found = false;

            for (int j = 0; j < ordinals.length; j++) {
               if (ordinals[j] == newOrdinal) {
                  found = true;
                  break;
               }
            }

            if (!found) {
               a.add(u);
            }
         }

         BBacnetUnsigned[] newUVals = (BBacnetUnsigned[])a.trim();
         int[] newOrdinals = new int[newUVals.length];
         String[] newTags = new String[newUVals.length];

         for (int i = 0; i < newOrdinals.length; i++) {
            newOrdinals[i] = newUVals[i].getInt();
            newTags[i] = r.getTag(newOrdinals[i]);
         }

         alg.set(BEnumChangeOfStateAlgorithm.alarmValues, BEnumRange.make(newOrdinals, newTags), BLocalBacnetDevice.getBacnetContext());
         return null;
      } catch (PermissionException var14) {
         log.warning("PermissionException adding elements to alarmValues in object " + this.getObjectId() + ": " + var14);
         return new NChangeListError(8, new NErrorType(2, 40), 0L);
      }
   }

   private ChangeListError removeAlarmValues(PropertyValue propertyValue, BEnumChangeOfStateAlgorithm alg) {
      ArrayList<BBacnetUnsigned> v = new ArrayList<>();
      int ffen = 1;

      try {
         synchronized (asnIn) {
            asnIn.setBuffer(propertyValue.getPropertyValue());

            for (int tag = asnIn.peekTag(); tag != -1; tag = asnIn.peekTag()) {
               v.add(asnIn.readUnsigned());
               ffen++;
            }
         }
      } catch (AsnException var15) {
         return new NChangeListError(9, new NErrorType(2, 9), ffen);
      }

      BEnumRange r = (BEnumRange)this.getPoint().getFacets().getFacet("range");
      BEnumRange almVals = alg.getAlarmValues();
      Array<BBacnetUnsigned> a = new Array(BBacnetUnsigned.class);
      int[] ordinals = almVals.getOrdinals();

      for (int i = 0; i < ordinals.length; i++) {
         a.add(BBacnetUnsigned.make(ordinals[i]));
      }

      try {
         for (int var16 = 1; var16 <= v.size(); var16++) {
            BBacnetUnsigned u = v.get(var16 - 1);
            if (!a.contains(u)) {
               return new NChangeListError(9, new NErrorType(5, 81), var16);
            }

            a.remove(u);
         }

         BBacnetUnsigned[] newUVals = (BBacnetUnsigned[])a.trim();
         int[] newOrdinals = new int[newUVals.length];
         String[] newTags = new String[newUVals.length];

         for (int i = 0; i < newOrdinals.length; i++) {
            newOrdinals[i] = newUVals[i].getInt();
            newTags[i] = r.getTag(newOrdinals[i]);
         }

         alg.set(BEnumChangeOfStateAlgorithm.alarmValues, BEnumRange.make(newOrdinals, newTags), BLocalBacnetDevice.getBacnetContext());
         return null;
      } catch (PermissionException var13) {
         log.warning("PermissionException removing elements from alarmValues in object " + this.getObjectId() + ": " + var13);
         return new NChangeListError(9, new NErrorType(2, 40), 0L);
      }
   }

   @Override
   protected ErrorType writeProperty(int pId, int ndx, byte[] val, int pri) throws BacnetException {
      BEnumPoint pt = (BEnumPoint)this.getPoint();
      if (pt == null) {
         return new NErrorType(1, 1000);
      } else if (ndx >= 0 && !this.isArray(pId)) {
         return new NErrorType(2, 50);
      } else {
         try {
            switch (pId) {
               case 74:
                  return new NErrorType(2, 40);
               case 110:
                  return this.writeStateText(ndx, val, pt);
               default:
                  return super.writeProperty(pId, ndx, val, pri);
            }
         } catch (AsnException var7) {
            log.warning("AsnException writing property " + pId + " in object " + this.getObjectId() + ": " + var7);
            return new NErrorType(2, 9);
         }
      }
   }

   protected RangeData readRange(RangeReference ref, Integer[] list, int maxEncodedSize) {
      int rangeType = ref.getRangeType();
      int len = list.length;
      boolean[] rflags = new boolean[]{false, false, false};
      int maxDataLength = -1;
      if (ref instanceof BacnetConfirmedRequest) {
         maxDataLength = ((BacnetConfirmedRequest)ref).getMaxDataLength() - 23 + 3 + 5;
      }

      if (rangeType == 3) {
         int refNdx = (int)ref.getReferenceIndex();
         int count = ref.getCount();
         if (refNdx <= len && refNdx >= 1) {
            Array<Integer> a = new Array(Integer.class);
            int itemsFound = 0;
            if (count > 0) {
               for (int i = refNdx - 1; i < len && itemsFound < count; i++) {
                  a.add(list[i]);
                  itemsFound++;
               }

               if (refNdx == 1) {
                  rflags[0] = true;
               }

               if (refNdx + count - 1 >= len) {
                  rflags[1] = true;
               }
            } else {
               if (count >= 0) {
                  return new ReadRangeAck(5, 7);
               }

               count = -count;

               for (int i = refNdx - 1; i >= 0 && itemsFound < count; i--) {
                  a.add(list[i]);
                  itemsFound++;
               }

               a = a.reverse();
               if (refNdx - count <= 0) {
                  rflags[0] = true;
               }

               if (refNdx == len) {
                  rflags[1] = true;
               }
            }

            Iterator<Integer> it = a.iterator();
            int itemCount = 0;
            synchronized (asnOut) {
               asnOut.reset();
               if (maxDataLength > 0) {
                  while (it.hasNext()) {
                     if (maxDataLength - asnOut.size() < maxEncodedSize) {
                        rflags[1] = false;
                        break;
                     }

                     asnOut.writeUnsignedInteger(it.next().intValue());
                     itemCount++;
                  }
               } else {
                  itemCount = itemsFound;

                  while (it.hasNext()) {
                     asnOut.writeUnsignedInteger(it.next().intValue());
                  }
               }

               if (itemCount < itemsFound) {
                  rflags[2] = true;
               }

               return new ReadRangeAck(this.getObjectId(), ref.getPropertyId(), -1, BBacnetBitString.make(rflags), itemCount, asnOut.toByteArray());
            }
         } else {
            return new ReadRangeAck(this.getObjectId(), ref.getPropertyId(), -1, BBacnetBitString.emptyBitString(3), 0L, new byte[0]);
         }
      } else if (rangeType == -1) {
         rflags[0] = false;
         int itemCount = 0;
         synchronized (asnOut) {
            asnOut.reset();
            if (maxDataLength > 0) {
               for (int i = 0; i < len; i++) {
                  asnOut.writeUnsignedInteger(list[i].intValue());
                  itemCount++;
                  if (maxDataLength - asnOut.size() < maxEncodedSize) {
                     break;
                  }
               }

               if (itemCount > 0) {
                  rflags[0] = true;
               }

               if (itemCount > 0 && itemCount == len) {
                  rflags[1] = true;
               }
            } else {
               itemCount = len;

               for (int ix = 0; ix < len; ix++) {
                  asnOut.writeUnsignedInteger(list[ix].intValue());
               }

               if (len > 0) {
                  rflags[0] = true;
               }

               if (len > 0 && len == len) {
                  rflags[1] = true;
               }
            }

            if (itemCount < len) {
               rflags[2] = true;
            }

            return new ReadRangeAck(this.getObjectId(), ref.getPropertyId(), -1, BBacnetBitString.make(rflags), itemCount, asnOut.toByteArray());
         }
      } else {
         return new ReadRangeAck(5, 7);
      }
   }

   @Override
   protected void addRequiredProps(Vector v) {
      super.addRequiredProps(v);
      v.add(BBacnetPropertyIdentifier.presentValue);
      v.add(BBacnetPropertyIdentifier.statusFlags);
      v.add(BBacnetPropertyIdentifier.eventState);
      v.add(BBacnetPropertyIdentifier.outOfService);
      v.add(BBacnetPropertyIdentifier.numberOfStates);
   }

   @Override
   protected void addOptionalProps(Vector v) {
      super.addOptionalProps(v);
      BEnumRange r = (BEnumRange)this.getPoint().getFacets().getFacet("range");
      if (r != null) {
         v.add(BBacnetPropertyIdentifier.stateText);
      }
   }

   @Override
   protected final boolean checkPointConfiguration() {
      BEnumRange r = (BEnumRange)this.getPoint().getFacets().getFacet("range");
      if (r != null) {
         int[] ords = r.getOrdinals();
         if (ords.length > 0) {
            if (ords[0] != 1) {
               this.setFaultCause("Range must be 1-N for export to BACnet.");
               return false;
            }

            for (int i = 0; i < ords.length; i++) {
               if (ords[i] != i + 1) {
                  this.setFaultCause("State Range supports only contiguous ordinals.");
                  return false;
               }
            }
         }
      }

      return true;
   }

   @Override
   protected void validate() {
      BStatusEnum se = ((BEnumPoint)this.getPoint()).getOut();
      BStatus s = se.getStatus();
      if (s.isNull()) {
         this.setReliability(BBacnetReliability.unreliableOther);
         this.setFaultCause("Invalid value for BACnet Object:" + se);
         this.setStatus(BStatus.makeFault(this.getStatus(), true));
      } else if (s.isFault()) {
         this.setReliability(BBacnetReliability.multiStateFault);
      } else if (s.isDown()) {
         this.setReliability(BBacnetReliability.communicationFailure);
      } else {
         int pv = se.getValue().getOrdinal();
         BEnumRange r = (BEnumRange)this.getPoint().getFacets().getFacet("range");
         if (r == null) {
            this.setReliability(BBacnetReliability.unreliableOther);
            this.setStatus(BStatus.makeFault(this.getStatus(), true));
            this.setFaultCause(lex.getText("export.configurationFault"));
            return;
         }

         if (pv == 0 || !r.isOrdinal(pv)) {
            this.setReliability(BBacnetReliability.unreliableOther);
            this.setFaultCause("Value out of range:" + pv);
            this.setStatus(BStatus.makeFault(this.getStatus(), true));
            return;
         }

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

   protected static EnumRangeWrapper getWritableEnumRange(byte[] val, BEnumRange tagRange, boolean skipOrdinals) throws AsnException {
      List<BBacnetUnsigned> alarmOrdinalList;
      synchronized (asnIn) {
         asnIn.setBuffer(val);
         int tag = asnIn.peekTag();
         if (!skipOrdinals) {
            for (alarmOrdinalList = Collections.synchronizedList(new ArrayList<>()); tag != -1; tag = asnIn.peekTag()) {
               alarmOrdinalList.add(asnIn.readUnsigned());
            }
         } else {
            alarmOrdinalList = Collections.synchronizedList(new LinkedList<>());

            for (int ordinal : tagRange.getOrdinals()) {
               alarmOrdinalList.add(BBacnetUnsigned.make(ordinal));
            }

            while (tag != -1) {
               BBacnetUnsigned excludeUnsigned = asnIn.readUnsigned();
               int excludeOrdinal = excludeUnsigned.getInt();
               if (!tagRange.isOrdinal(excludeOrdinal)) {
                  log.warning("Invalid ordinal value : " + excludeOrdinal);
                  return EnumRangeWrapper.make(BEnumRange.DEFAULT, new NErrorType(2, 37));
               }

               alarmOrdinalList.remove(excludeUnsigned);
               tag = asnIn.peekTag();
            }
         }
      }

      return makeEnumRange(tagRange, alarmOrdinalList);
   }

   private static EnumRangeWrapper makeEnumRange(BEnumRange tagRange, List<BBacnetUnsigned> ordinalList) {
      Iterator<BBacnetUnsigned> it = ordinalList.iterator();
      int size = ordinalList.size();
      int[] ordinals = new int[size];
      String[] tags = new String[size];
      int counter = 0;

      while (it.hasNext()) {
         BBacnetUnsigned bacnetUnsigned = it.next();
         int ordinal = bacnetUnsigned.getInt();
         if (!tagRange.isOrdinal(ordinal)) {
            log.warning("Invalid ordinal value: " + ordinal);
            return EnumRangeWrapper.make(BEnumRange.DEFAULT, new NErrorType(2, 37));
         }

         ordinals[counter] = ordinal;
         tags[counter++] = tagRange.getTag(ordinal);
      }

      BEnumRange enumRange = BEnumRange.make(ordinals, tags);
      return EnumRangeWrapper.make(enumRange, null);
   }

   @Override
   BStatusValue getCurrentStatusValue() {
      BStatusValue sv = new BStatusEnum(((BEnumPoint)this.getPoint()).getOut().getValue());
      sv.setStatus(this.getStatusFlags());
      return sv;
   }

   @Override
   boolean checkCov(BStatusValue currentValue, BStatusValue covValue) {
      return currentValue.getStatus().getBits() != covValue.getStatus().getBits()
         ? true
         : ((BStatusEnum)currentValue).getEnum().getOrdinal() != ((BStatusEnum)covValue).getEnum().getOrdinal();
   }

   @Deprecated
   boolean checkCov(BControlPoint pt, BBacnetCovSubscription covSub) {
      if (pt.getStatus().getBits() != covSub.getLastValue().getStatus().getBits()) {
         return true;
      } else {
         int currentValue = ((BEnumPoint)pt).getEnum().getOrdinal();
         int covValue = ((BIEnum)covSub.getLastValue()).getEnum().getOrdinal();
         return currentValue != covValue;
      }
   }

   @Override
   BStatus getStatusFlags() {
      int status = super.getStatusFlags().getBits();
      BEnumPoint pt = (BEnumPoint)this.getPoint();
      if (pt.getOut().getValue().getOrdinal() <= 0) {
         status |= 2;
      }

      return BStatus.make(status);
   }

   private static String[] getTags(BEnumRange r) {
      int[] ordinals = r.getOrdinals();
      String[] tags = new String[ordinals.length];

      for (int i = 0; i < tags.length; i++) {
         tags[i] = r.getTag(ordinals[i]);
      }

      return tags;
   }

   private static int findIndex(int ndx, int[] ordinals) {
      for (int i = 0; i < ordinals.length; i++) {
         if (ordinals[i] == ndx) {
            return i;
         }
      }

      return -1;
   }

   private PropertyValue readStateText(int ndx) {
      BEnumRange range = (BEnumRange)this.getPoint().getFacets().getFacet("range");
      if (range == null) {
         return new NReadPropertyResult(110, ndx, new NErrorType(2, 32));
      } else {
         int length = range.getOrdinals().length;
         if (ndx == -1) {
            AsnOutputStream asnOut = AsnOutputStream.make();

            NReadPropertyResult var11;
            try {
               for (int i = 1; i <= length; i++) {
                  asnOut.writeCharacterString(SlotPath.unescape(range.getTag(i)));
               }

               var11 = new NReadPropertyResult(110, -1, asnOut.toByteArray());
            } finally {
               asnOut.release();
            }

            return var11;
         } else if (ndx == 0) {
            return new NReadPropertyResult(110, 0, AsnUtil.toAsnUnsigned(length));
         } else if (ndx >= 1 && ndx <= length) {
            try {
               return new NReadPropertyResult(110, ndx, AsnUtil.toAsnCharacterString(SlotPath.unescape(range.getTag(ndx))));
            } catch (Exception var9) {
               return new NReadPropertyResult(110, ndx, new NErrorType(2, 42));
            }
         } else {
            return new NReadPropertyResult(110, ndx, new NErrorType(2, 42));
         }
      }
   }

   private NErrorType writeStateText(int ndx, byte[] val, BEnumPoint pt) throws BacnetException {
      BFacets f = pt.getFacets();
      BEnumRange r = (BEnumRange)f.getFacet("range");
      if (r == null) {
         return new NErrorType(2, 32);
      } else {
         try {
            switch (ndx) {
               case -1:
                  ArrayList<String> v = new ArrayList<>();
                  synchronized (asnIn) {
                     asnIn.setBuffer(val);

                     for (int tag = asnIn.peekTag(); tag != -1; tag = asnIn.peekTag()) {
                        v.add(asnIn.readCharacterString());
                     }
                  }

                  if (v.size() != r.getOrdinals().length) {
                     return new NErrorType(2, 37);
                  } else {
                     int[] newOrdinals = new int[v.size()];
                     String[] newTags = new String[v.size()];

                     for (int i = 0; i < newOrdinals.length; i++) {
                        newOrdinals[i] = i + 1;
                        newTags[i] = SlotPath.escape(v.get(i));
                        if (newTags[i].length() == 0) {
                           return new NErrorType(2, 37);
                        }
                     }

                     BEnumRange range;
                     try {
                        range = BEnumRange.make(newOrdinals, newTags);
                     } catch (IllegalArgumentException var14) {
                        return new NErrorType(2, 37);
                     }

                     pt.set(BControlPoint.facets, BFacets.make(f, "range", range), BLocalBacnetDevice.getBacnetContext());
                     return null;
                  }
               case 0:
                  return new NErrorType(2, 42);
               default:
                  int[] ordinals = r.getOrdinals();
                  String[] tags = getTags(r);
                  if (ndx >= 1 && ndx <= ordinals.length) {
                     int i = findIndex(ndx, ordinals);
                     if (i < 0) {
                        log.severe("MultiStatePointDescriptor.writeStateText: Index not found in ordinal list: " + ndx);
                        return new NErrorType(2, 42);
                     } else {
                        tags[i] = SlotPath.escape(AsnUtil.fromAsnCharacterString(val));
                        if (tags[i].length() == 0) {
                           return new NErrorType(2, 37);
                        } else {
                           BEnumRange range;
                           try {
                              range = BEnumRange.make(ordinals, tags);
                           } catch (IllegalArgumentException var15) {
                              return new NErrorType(2, 37);
                           }

                           pt.set(BControlPoint.facets, BFacets.make(f, "range", range), BLocalBacnetDevice.getBacnetContext());
                           return null;
                        }
                     }
                  } else {
                     return new NErrorType(2, 42);
                  }
            }
         } catch (PermissionException var17) {
            log.warning("PermissionException writing stateText in object " + this.getObjectId() + ": " + var17);
            return new NErrorType(2, 40);
         }
      }
   }

   public BIcon getIcon() {
      return icon;
   }
}
