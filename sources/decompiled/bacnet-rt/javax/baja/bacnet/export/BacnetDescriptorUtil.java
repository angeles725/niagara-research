package javax.baja.bacnet.export;

import com.tridium.bacnet.ObjectTypeList;
import com.tridium.bacnet.asn.NBacnetPropertyReference;
import com.tridium.bacnet.asn.NBacnetPropertyValue;
import com.tridium.bacnet.asn.NErrorType;
import com.tridium.bacnet.history.BBacnetBitStringTrendLogExt;
import com.tridium.bacnet.history.BBacnetBitStringTrendLogRemoteExt;
import com.tridium.bacnet.history.BBacnetBooleanTrendLogExt;
import com.tridium.bacnet.history.BBacnetBooleanTrendLogRemoteExt;
import com.tridium.bacnet.history.BBacnetEnumTrendLogExt;
import com.tridium.bacnet.history.BBacnetEnumTrendLogRemoteExt;
import com.tridium.bacnet.history.BBacnetNumericTrendLogExt;
import com.tridium.bacnet.history.BBacnetNumericTrendLogRemoteExt;
import com.tridium.bacnet.history.BBacnetStringTrendLogExt;
import com.tridium.bacnet.history.BBacnetStringTrendLogRemoteExt;
import com.tridium.bacnet.history.BBacnetTrendLogAlarmSourceExt;
import com.tridium.bacnet.history.BBacnetTrendLogRemoteExt;
import com.tridium.bacnet.history.BIBacnetTrendLogExt;
import com.tridium.bacnet.services.error.NChangeListError;
import com.tridium.bacnet.stack.server.BBacnetExportTable;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import javax.baja.bacnet.BBacnetDevice;
import javax.baja.bacnet.BBacnetNetwork;
import javax.baja.bacnet.BacnetException;
import javax.baja.bacnet.datatypes.BBacnetDeviceObjectPropertyReference;
import javax.baja.bacnet.datatypes.BBacnetObjectIdentifier;
import javax.baja.bacnet.datatypes.BBacnetObjectPropertyReference;
import javax.baja.bacnet.enums.BBacnetErrorClass;
import javax.baja.bacnet.enums.BBacnetErrorCode;
import javax.baja.bacnet.enums.BBacnetObjectType;
import javax.baja.bacnet.enums.BBacnetPropertyIdentifier;
import javax.baja.bacnet.export.extensions.BBacnetRemoteUnsignedPropertyExt;
import javax.baja.bacnet.export.extensions.BBacnetUnsignedPropertyExt;
import javax.baja.bacnet.io.ChangeListError;
import javax.baja.bacnet.io.PropertyValue;
import javax.baja.bacnet.io.RejectException;
import javax.baja.bacnet.point.BBacnetBooleanProxyExt;
import javax.baja.bacnet.point.BBacnetEnumProxyExt;
import javax.baja.bacnet.point.BBacnetNumericProxyExt;
import javax.baja.bacnet.point.BBacnetProxyExt;
import javax.baja.bacnet.point.BBacnetStringProxyExt;
import javax.baja.bacnet.util.PropertyInfo;
import javax.baja.control.BBooleanPoint;
import javax.baja.control.BBooleanWritable;
import javax.baja.control.BControlPoint;
import javax.baja.control.BEnumPoint;
import javax.baja.control.BEnumWritable;
import javax.baja.control.BNumericPoint;
import javax.baja.control.BNumericWritable;
import javax.baja.control.BPointExtension;
import javax.baja.control.BStringPoint;
import javax.baja.control.BStringWritable;
import javax.baja.control.ext.BDiscreteTotalizerExt;
import javax.baja.history.BHistoryConfig;
import javax.baja.history.BHistoryId;
import javax.baja.history.BHistoryService;
import javax.baja.history.HistorySpaceConnection;
import javax.baja.history.ext.BHistoryExt;
import javax.baja.naming.BOrd;
import javax.baja.naming.SlotPath;
import javax.baja.status.BStatusNumeric;
import javax.baja.sys.BComponent;
import javax.baja.sys.BDynamicEnum;
import javax.baja.sys.BFacets;
import javax.baja.sys.BLink;
import javax.baja.sys.BRelTime;
import javax.baja.sys.BValue;
import javax.baja.sys.Knob;
import javax.baja.sys.Sys;
import javax.baja.util.BFolder;

public final class BacnetDescriptorUtil {
   private static final Pattern FORWARD_SLASH_PATTERN = Pattern.compile("/");
   private static final Logger logger = Logger.getLogger("bacnet.export.object.util");
   private static final BControlPoint[] EMPTY_POINT_ARRAY = new BControlPoint[0];
   private static final String DISCRETE_TOTALIZER_EXT = "DiscreteTotalizerExtension";

   private BacnetDescriptorUtil() {
   }

   static boolean isValid(BBacnetDeviceObjectPropertyReference reference) {
      return reference != null
         && reference.getPropertyId() >= 0
         && reference.getObjectId().getInstanceNumber() >= 0
         && reference.getDeviceId().getInstanceNumber() >= -1;
   }

   static boolean isLocalDevice(int deviceNum) {
      if (deviceNum == -1) {
         return true;
      } else {
         BLocalBacnetDevice localDevice = BBacnetNetwork.localDevice();
         int localDeviceNum = localDevice.getObjectId().getInstanceNumber();
         return localDeviceNum == deviceNum;
      }
   }

   static BControlPoint findOrAddPoint(BBacnetDeviceObjectPropertyReference objectPropRef) throws Exception {
      int deviceNum = objectPropRef.getDeviceId().getInstanceNumber();
      return isLocalDevice(deviceNum) ? findOrAddLocalPoint(objectPropRef) : findOrAddRemotePoint(objectPropRef);
   }

   private static BControlPoint findOrAddLocalPoint(BBacnetDeviceObjectPropertyReference objPropRef) throws Exception {
      return findOrAddLocalPoint(objPropRef.getObjectId(), objPropRef.getPropertyId(), objPropRef.getPropertyArrayIndex());
   }

   static BControlPoint findOrAddLocalPoint(BBacnetObjectPropertyReference objPropRef) throws Exception {
      return findOrAddLocalPoint(objPropRef.getObjectId(), objPropRef.getPropertyId(), objPropRef.getPropertyArrayIndex());
   }

   private static BControlPoint findOrAddLocalPoint(BBacnetObjectIdentifier objectId, int propertyId, int propertyArrayIndex) throws Exception {
      BComponent point = findLocalObject(objectId);
      if (!(point instanceof BControlPoint)) {
         return null;
      } else {
         if (propertyId == 33) {
            point = getPointForElapsedActiveTime(objectId, propertyArrayIndex, (BControlPoint)point);
         }

         return (BControlPoint)point;
      }
   }

   static BComponent findLocalObject(BBacnetObjectIdentifier objectId) throws Exception {
      BIBacnetExportObject exportObject = BBacnetNetwork.localDevice().lookupBacnetObject(objectId);
      if (exportObject == null) {
         throw new Exception("Could not find a local BACnet export object with ID " + objectId);
      } else {
         BOrd exportObjectOrd = exportObject.getObjectOrd();
         if (exportObjectOrd.isNull()) {
            throw new Exception("ObjectOrd is null for local BACnet object with ID " + objectId);
         } else {
            return (BComponent)exportObjectOrd.get(Sys.getStation());
         }
      }
   }

   private static BControlPoint getPointForElapsedActiveTime(BBacnetObjectIdentifier objectId, int propertyIndex, BControlPoint point) {
      BDiscreteTotalizerExt[] extensions = (BDiscreteTotalizerExt[])point.getChildren(BDiscreteTotalizerExt.class);
      BControlPoint linkedPoint = null;
      BDiscreteTotalizerExt extension;
      if (extensions.length > 0) {
         extension = extensions[0];
         extension.setEaTimeUpdateInterval(BRelTime.make(1000L));
         linkedPoint = getNumericPointLinkedToDiscreteTotExt(objectId, extension);
      } else {
         extension = addDiscreteTotalizerExtToPoint(point);
      }

      if (linkedPoint == null) {
         linkedPoint = addPropertyPoint(null, objectId, 33, propertyIndex);
         linkToNumericPoint(extension, linkedPoint);
      }

      return linkedPoint;
   }

   private static void linkToNumericPoint(BDiscreteTotalizerExt extension, BControlPoint linkedPoint) {
      try {
         BValue divideCheckVar = linkedPoint.get("divide");
         BControlPoint divide;
         if (divideCheckVar == null) {
            Class<?> divideClass = Sys.loadClass("kitControl", "com.tridium.kitControl.math.BDivide");
            divide = (BControlPoint)divideClass.getDeclaredConstructor().newInstance();
            divide.set("inB", new BStatusNumeric(1000.0));
            linkedPoint.add("divide", divide);
         } else {
            divide = (BControlPoint)divideCheckVar;
         }

         BLink linkToDivide = new BLink(extension.getHandleOrd(), BDiscreteTotalizerExt.elapsedActiveTimeNumeric.getName(), "inA", true);
         divide.add(null, linkToDivide, BLocalBacnetDevice.getBacnetContext());
         BLink link = new BLink(divide.getHandleOrd(), "out", BNumericWritable.in16.getName(), true);
         linkedPoint.add(null, link, BLocalBacnetDevice.getBacnetContext());
      } catch (ClassNotFoundException var6) {
         removePoint(linkedPoint);
         logger.severe("Class BDivide is not found or kitControl module is not found: " + var6);
      } catch (IllegalAccessException | InvocationTargetException | InstantiationException | NoSuchMethodException var7) {
         removePoint(linkedPoint);
         logger.severe("Exception while creating instance of divide: " + var7);
      }
   }

   private static BDiscreteTotalizerExt addDiscreteTotalizerExtToPoint(BComponent point) {
      BDiscreteTotalizerExt discreteTotalizerExt = new BDiscreteTotalizerExt();
      discreteTotalizerExt.setEaTimeUpdateInterval(BRelTime.make(1000L));
      point.add("DiscreteTotalizerExtension?", discreteTotalizerExt);
      return discreteTotalizerExt;
   }

   private static BControlPoint getNumericPointLinkedToDiscreteTotExt(BBacnetObjectIdentifier objectId, BDiscreteTotalizerExt extension) {
      Knob[] knobs = extension.getKnobs(BDiscreteTotalizerExt.elapsedActiveTimeNumeric);
      List<BControlPoint> points = new ArrayList<>();

      for (Knob knob : knobs) {
         BComponent targetParent = knob.getTargetComponent().getParent().getParentComponent();
         if (targetParent instanceof BControlPoint) {
            points.add((BControlPoint)targetParent);
         }
      }

      return !points.isEmpty() ? findElapsedActiveTimePoint(points.toArray(EMPTY_POINT_ARRAY), objectId) : null;
   }

   static BControlPoint findOrAddRemotePoint(BBacnetDeviceObjectPropertyReference objPropRef) {
      BBacnetDevice device = findOrAddRemoteDevice(objPropRef.getDeviceId());
      if (device == null) {
         return null;
      } else {
         BBacnetObjectIdentifier objectId = objPropRef.getObjectId();
         int propertyId = objPropRef.getPropertyId();
         int propertyIndex = objPropRef.getPropertyArrayIndex();
         BControlPoint point = findRemotePoint(device, objectId, propertyId, propertyIndex);
         if (point == null) {
            point = addPropertyPoint(device, objectId, propertyId, propertyIndex);
         }

         return point;
      }
   }

   private static BControlPoint findRemotePoint(BBacnetDevice device, BBacnetObjectIdentifier objectId, int propertyId, int propertyArrayIndex) {
      if (propertyId == 33) {
         BControlPoint[] remotePoints = device.getPoints().getPoints();
         BControlPoint point = findElapsedActiveTimePoint(remotePoints, objectId);
         if (point != null) {
            return point;
         }
      }

      return (BControlPoint)device.lookupBacnetObject(objectId, propertyId, propertyArrayIndex, "point");
   }

   private static BControlPoint findElapsedActiveTimePoint(BControlPoint[] points, BBacnetObjectIdentifier objectId) {
      if (points == null) {
         return null;
      } else {
         for (BControlPoint point : points) {
            BBacnetUnsignedPropertyExt[] extensions = (BBacnetUnsignedPropertyExt[])point.getChildren(BBacnetUnsignedPropertyExt.class);

            for (BBacnetUnsignedPropertyExt ext : extensions) {
               if (ext.getPropertyId() == 33 && ext.getObjectId().equivalent(objectId)) {
                  return point;
               }
            }
         }

         return null;
      }
   }

   private static BControlPoint addPropertyPoint(BBacnetDevice device, BBacnetObjectIdentifier objectId, int propertyId, int propertyArrayIndex) {
      try {
         BControlPoint point = makePropertyPoint(device, objectId, propertyId, propertyArrayIndex);
         String name = makePointName(objectId, propertyId, propertyArrayIndex);
         if (device == null) {
            BComponent dynamicPointsFolder = (BComponent)BBacnetNetwork.bacnet().get("dynamicPoints");
            if (dynamicPointsFolder == null) {
               dynamicPointsFolder = new BFolder();
               BBacnetNetwork.bacnet().add("dynamicPoints", dynamicPointsFolder, 5);
            }

            dynamicPointsFolder.add(SlotPath.escape(name), point);
         } else {
            device.getPoints().add(SlotPath.escape(name), point);
         }

         return point;
      } catch (Exception var7) {
         logger.severe("Could not add point for property " + propertyId + " on device " + device + "; " + var7.getMessage());
         return null;
      }
   }

   private static String makePointName(BBacnetObjectIdentifier objectId, int propertyId, int propertyArrayIndex) {
      StringBuilder pointName = new StringBuilder(objectId.toString());
      if (propertyId != 85) {
         pointName.append('-').append(BBacnetPropertyIdentifier.tag(propertyId));
      }

      if (propertyArrayIndex > 0) {
         pointName.append('_').append(propertyArrayIndex);
      }

      return FORWARD_SLASH_PATTERN.matcher(pointName.toString()).replaceAll(".");
   }

   private static void removePoint(BControlPoint point) {
      if (null == BBacnetNetwork.bacnet().get("dynamicPoints")) {
         BBacnetNetwork.bacnet().get("dynamicPoints").asComponent().remove(point.getName());
      }
   }

   private static BBacnetDevice findOrAddRemoteDevice(BBacnetObjectIdentifier deviceId) {
      BBacnetNetwork network = BBacnetNetwork.bacnet();
      BBacnetDevice device = network.lookupDeviceById(deviceId);
      if (device == null) {
         device = addRemoteDevice(deviceId.getInstanceNumber());
      }

      return device;
   }

   private static BBacnetDevice addRemoteDevice(int instanceNum) {
      BBacnetObjectIdentifier id = BBacnetObjectIdentifier.make(8, instanceNum);
      BBacnetDevice device = new BBacnetDevice();
      device.setObjectId(id, null);
      BBacnetNetwork.bacnet().add(null, device);
      return device;
   }

   private static BControlPoint addPointForElapsedActiveTime(BBacnetObjectIdentifier bOid, boolean isLocal) {
      BControlPoint point = new BNumericWritable();
      BPointExtension extension = null;
      if (isLocal) {
         extension = new BBacnetUnsignedPropertyExt(bOid, 33);
      } else {
         extension = new BBacnetRemoteUnsignedPropertyExt(bOid, 33);
      }

      point.add("ElapsedActiveTimeExtension?", extension);
      return point;
   }

   private static BControlPoint makePropertyPoint(BBacnetDevice device, BBacnetObjectIdentifier objectId, int propertyId, int propertyArrayIndex) throws BacnetException {
      if (propertyId == 33) {
         return addPointForElapsedActiveTime(objectId, device == null);
      } else {
         int objectType = objectId.getObjectType();
         PropertyInfo propInfo = device.getPropertyInfo(objectType, propertyId);
         if (propInfo == null) {
            throw new BacnetException(
               "BACnet property information not found when making a property point; object type: "
                  + BBacnetObjectType.tag(objectType)
                  + ", property ID: "
                  + BBacnetPropertyIdentifier.tag(propertyId)
            );
         } else {
            BControlPoint point = makePointForPropertyInfo(objectType, propInfo);
            BBacnetProxyExt ext = (BBacnetProxyExt)point.getProxyExt();
            ext.setDeviceFacets((BFacets)point.getFacets().newCopy());
            ext.setDataType(propInfo.getDataType());
            ext.setObjectId(objectId);
            ext.setPropertyId(BDynamicEnum.make(BBacnetPropertyIdentifier.make(propertyId)));
            ext.setPropertyArrayIndex(propertyArrayIndex);
            ext.setEnabled(true);
            return point;
         }
      }
   }

   private static BControlPoint makePointForPropertyInfo(int objectType, PropertyInfo propInfo) throws BacnetException {
      switch (propInfo.getAsnType()) {
         case -6:
         case -5:
         case -4:
         case -3:
         case -2:
         case -1:
         case 10:
         case 11:
         case 12:
            return makeBacnetStringWritable();
         case 0:
            return makeBacnetStringWritable();
         case 1:
            return makeBacnetBooleanWritable();
         case 2:
            return (BControlPoint)(isMultiStatePresentValue(propInfo.getId(), objectType) ? makeBacnetEnumWritable() : makeBacnetNumericWritable());
         case 3:
         case 4:
         case 5:
            return makeBacnetNumericWritable();
         case 6:
         case 7:
         case 8:
            return makeBacnetStringWritable();
         case 9:
            return (BControlPoint)(propInfo.getType().equals("bacnet:BacnetBinaryPv") ? makeBacnetBooleanWritable() : makeBacnetEnumWritable());
         default:
            throw new BacnetException("BACnet property type " + BBacnetPropertyIdentifier.tag(objectType) + " is not supported when making a property point");
      }
   }

   private static BBooleanWritable makeBacnetBooleanWritable() {
      BBooleanWritable booleanWritable = new BBooleanWritable();
      booleanWritable.setProxyExt(new BBacnetBooleanProxyExt());
      return booleanWritable;
   }

   private static BNumericWritable makeBacnetNumericWritable() {
      BNumericWritable numericWritable = new BNumericWritable();
      numericWritable.setProxyExt(new BBacnetNumericProxyExt());
      return numericWritable;
   }

   private static BEnumWritable makeBacnetEnumWritable() {
      BEnumWritable enumWritable = new BEnumWritable();
      enumWritable.setProxyExt(new BBacnetEnumProxyExt());
      return enumWritable;
   }

   private static BStringWritable makeBacnetStringWritable() {
      BStringWritable stringWritable = new BStringWritable();
      stringWritable.setProxyExt(new BBacnetStringProxyExt());
      return stringWritable;
   }

   private static boolean isMultiStatePresentValue(int propertyId, int objectType) {
      return propertyId == 85 && (objectType == 13 || objectType == 14 || objectType == 19);
   }

   static boolean isEqual(BBacnetDeviceObjectPropertyReference dopr1, BBacnetDeviceObjectPropertyReference dopr2) {
      if (dopr1 == null && dopr2 == null) {
         return true;
      } else if (dopr1 == null || dopr2 == null) {
         return false;
      } else {
         return dopr1.isNull() && dopr2.isNull()
            ? true
            : dopr1.getDeviceId().getInstanceNumber() == dopr2.getDeviceId().getInstanceNumber()
               && dopr1.getObjectId().getObjectType() == dopr2.getObjectId().getObjectType()
               && dopr1.getObjectId().getInstanceNumber() == dopr2.getObjectId().getInstanceNumber()
               && dopr1.getPropertyId() == dopr2.getPropertyId()
               && dopr1.getPropertyArrayIndex() == dopr2.getPropertyArrayIndex();
      }
   }

   static boolean areTrendLogAndPointCompatible(BControlPoint point, BIBacnetTrendLogExt trendLogExt, BBacnetDeviceObjectPropertyReference objPropRef) {
      PropertyInfo propInfo = ObjectTypeList.getInstance().getPropertyInfo(objPropRef.getObjectId().getObjectType(), objPropRef.getPropertyId());
      if (propInfo != null && propInfo.isBitString()) {
         return trendLogExt instanceof BBacnetBitStringTrendLogExt || trendLogExt instanceof BBacnetBitStringTrendLogRemoteExt;
      } else if (point instanceof BNumericPoint) {
         return trendLogExt instanceof BBacnetNumericTrendLogExt || trendLogExt instanceof BBacnetNumericTrendLogRemoteExt;
      } else if (point instanceof BStringPoint) {
         return trendLogExt instanceof BBacnetStringTrendLogExt || trendLogExt instanceof BBacnetStringTrendLogRemoteExt;
      } else if (point instanceof BBooleanPoint) {
         return trendLogExt instanceof BBacnetBooleanTrendLogExt || trendLogExt instanceof BBacnetBooleanTrendLogRemoteExt;
      } else {
         return !(point instanceof BEnumPoint) ? false : trendLogExt instanceof BBacnetEnumTrendLogExt || trendLogExt instanceof BBacnetEnumTrendLogRemoteExt;
      }
   }

   static BIBacnetTrendLogExt copy(BIBacnetExportObject descriptor, BBacnetDeviceObjectPropertyReference dopr, PropertyValue[] initialPVs) throws BacnetException {
      try {
         int objectType = descriptor.getObjectId().getObjectType();
         switch (objectType) {
            case 20:
               return copyTrendLogProperties(descriptor, dopr, initialPVs);
         }
      } catch (BacnetException var4) {
         throw var4;
      } catch (Exception var5) {
         logger.severe(
            descriptor + ": Could not copy the trend log extension when writing BACnet objectPropertyReference property: " + dopr + "; error: " + var5
         );
      }

      return null;
   }

   private static BIBacnetTrendLogExt copyTrendLogProperties(
      BIBacnetExportObject descriptor, BBacnetDeviceObjectPropertyReference dopr, PropertyValue[] initialPVs
   ) throws Exception {
      BIBacnetTrendLogExt trendLogExt = makeTrendLogExt(dopr);
      BControlPoint point = null;
      if (isValid(dopr)) {
         point = findOrAddPoint(dopr);
      }

      if (point != null && trendLogExt != null) {
         deleteOrd(descriptor);
         point.add("TrendLog" + descriptor.getObjectId().getInstanceNumber(), (BComponent)trendLogExt);
         BOrd newOrd = ((BComponent)trendLogExt).getHandleOrd();
         descriptor.setObjectOrd(newOrd, null);
         ((BBacnetTrendLogDescriptor)descriptor).getLog(true);

         for (PropertyValue pv : initialPVs) {
            if (!(pv instanceof NErrorType)) {
               byte[] value = pv.getPropertyValue();
               int propertyId = pv.getPropertyId();
               PropertyValue bpv = new NBacnetPropertyValue(propertyId, value);
               descriptor.writeProperty(bpv);
            }
         }
      }

      return trendLogExt;
   }

   private static void deleteOrd(BIBacnetExportObject descriptor) {
      BOrd ord = descriptor.getObjectOrd();
      if (!ord.isNull()) {
         BComponent tle = ord.resolve(Sys.getStation()).get().asComponent();
         BComponent parent = tle.getParent().asComponent();
         parent.remove(tle.getName());
      }
   }

   static void removeHistory(BIBacnetExportObject descriptor, boolean rename) {
      BOrd ord = descriptor.getObjectOrd();
      if (!ord.isNull()) {
         BComponent tle = ord.resolve(Sys.getStation()).get().asComponent();
         BHistoryExt historyExt = (BHistoryExt)tle;
         BHistoryConfig config = historyExt.getHistoryConfig();
         BHistoryService historyService = (BHistoryService)Sys.getService(BHistoryService.TYPE);
         HistorySpaceConnection conn = historyService.getDatabase().getConnection(BLocalBacnetDevice.getBacnetContext());
         Throwable var8 = null;

         try {
            BHistoryId historyId = config.getId();
            if (rename) {
               removeHistory(descriptor, false);
            } else if (conn.exists(historyId)) {
               conn.clearAllRecords(config.getId());
            }
         } catch (Throwable var17) {
            var8 = var17;
            throw var17;
         } finally {
            if (conn != null) {
               if (var8 != null) {
                  try {
                     conn.close();
                  } catch (Throwable var16) {
                     var8.addSuppressed(var16);
                  }
               } else {
                  conn.close();
               }
            }
         }
      }
   }

   static PropertyValue[] getValuesWrittenToTrendExtension(BIBacnetExportObject descriptor) throws RejectException {
      return descriptor.readPropertyMultiple(
         new NBacnetPropertyReference[]{
            new NBacnetPropertyReference(142),
            new NBacnetPropertyReference(143),
            new NBacnetPropertyReference(127),
            new NBacnetPropertyReference(128),
            new NBacnetPropertyReference(134),
            new NBacnetPropertyReference(144),
            new NBacnetPropertyReference(141),
            new NBacnetPropertyReference(72),
            new NBacnetPropertyReference(17),
            new NBacnetPropertyReference(137),
            new NBacnetPropertyReference(35)
         }
      );
   }

   static BIBacnetTrendLogExt makeTrendLogExt(BBacnetDeviceObjectPropertyReference ref) throws BacnetException {
      int propertyId = ref.getPropertyId();
      int objectType = ref.getObjectId().getObjectType();
      BBacnetObjectIdentifier deviceId = ref.getDeviceId();
      BIBacnetTrendLogExt trendLog;
      if (isLocalDevice(deviceId.getInstanceNumber())) {
         if (propertyId != 85) {
            throw new BacnetException(
               "BACnet property type "
                  + BBacnetPropertyIdentifier.tag(propertyId)
                  + " is not supported when making a trend log extension for the local device; only Present Value"
            );
         }

         BLocalBacnetDevice localDevice = BBacnetNetwork.localDevice();
         PropertyInfo propInfo = localDevice.getPropertyInfo(objectType, propertyId);
         trendLog = makeTrendLogExt(objectType, propInfo, false);
      } else {
         BBacnetDevice remoteDevice = findOrAddRemoteDevice(deviceId);
         PropertyInfo propInfo = remoteDevice.getPropertyInfo(objectType, propertyId);
         trendLog = makeTrendLogExt(objectType, propInfo, true);
         BBacnetTrendLogRemoteExt remoteTrendLog = (BBacnetTrendLogRemoteExt)trendLog;
         remoteTrendLog.setDevice(remoteDevice);
         remoteTrendLog.setObjectId(ref.getObjectId());
         remoteTrendLog.setPropertyId(propertyId);
         remoteTrendLog.setArrayIndex(ref.getPropertyArrayIndex());
      }

      BBacnetTrendLogAlarmSourceExt almExt = new BBacnetTrendLogAlarmSourceExt();
      ((BComponent)trendLog).add(almExt.getName(), almExt);
      return trendLog;
   }

   private static BIBacnetTrendLogExt makeTrendLogExt(int objectType, PropertyInfo propInfo, boolean isRemote) throws BacnetException {
      switch (propInfo.getAsnType()) {
         case -6:
         case -5:
         case -4:
         case -3:
         case -2:
         case -1:
         case 0:
         case 6:
         case 7:
         case 10:
         case 11:
         case 12:
            return (BIBacnetTrendLogExt)(isRemote ? new BBacnetStringTrendLogRemoteExt() : new BBacnetStringTrendLogExt());
         case 1:
            return (BIBacnetTrendLogExt)(isRemote ? new BBacnetBooleanTrendLogRemoteExt() : new BBacnetBooleanTrendLogExt());
         case 2:
            if (isMultiStatePresentValue(propInfo.getId(), objectType)) {
               return (BIBacnetTrendLogExt)(isRemote ? new BBacnetEnumTrendLogRemoteExt() : new BBacnetEnumTrendLogExt());
            }

            return (BIBacnetTrendLogExt)(isRemote ? new BBacnetNumericTrendLogRemoteExt() : new BBacnetNumericTrendLogExt());
         case 3:
         case 4:
         case 5:
            return (BIBacnetTrendLogExt)(isRemote ? new BBacnetNumericTrendLogRemoteExt() : new BBacnetNumericTrendLogExt());
         case 8:
            return (BIBacnetTrendLogExt)(isRemote ? new BBacnetBitStringTrendLogRemoteExt() : new BBacnetBitStringTrendLogExt());
         case 9:
            if (propInfo.getType().equals("bacnet:BacnetBinaryPv")) {
               return (BIBacnetTrendLogExt)(isRemote ? new BBacnetBooleanTrendLogRemoteExt() : new BBacnetBooleanTrendLogExt());
            }

            return (BIBacnetTrendLogExt)(isRemote ? new BBacnetEnumTrendLogRemoteExt() : new BBacnetEnumTrendLogExt());
         default:
            throw new BacnetException(
               "BACnet property type "
                  + BBacnetPropertyIdentifier.tag(objectType)
                  + " is not supported when making a trend log extension for "
                  + (isRemote ? "remote" : "local")
                  + " device"
            );
      }
   }

   public static boolean isGenericTrendLogExtension(BIBacnetTrendLogExt ext) {
      return ext instanceof BBacnetNumericTrendLogExt
         || ext instanceof BBacnetStringTrendLogExt
         || ext instanceof BBacnetEnumTrendLogExt
         || ext instanceof BBacnetBooleanTrendLogExt
         || ext instanceof BBacnetTrendLogRemoteExt;
   }

   public static BBacnetObjectIdentifier nextObjectIdentifier(int objectType) {
      BBacnetExportTable et = exportTable();

      for (int i = 0; i < 4194302; i++) {
         BBacnetObjectIdentifier oid = BBacnetObjectIdentifier.make(objectType, i);
         if (et.byObjectId(oid) == null) {
            return oid;
         }
      }

      return null;
   }

   public static BBacnetExportTable exportTable() {
      return (BBacnetExportTable)BBacnetNetwork.localDevice().getExportTable();
   }

   public static ChangeListError makeAddListElementError(BBacnetErrorClass errorClass, BBacnetErrorCode errorCode) {
      return new NChangeListError(8, new NErrorType(errorClass, errorCode), 0L);
   }

   public static ChangeListError makeRemoveListElementError(BBacnetErrorClass errorClass, BBacnetErrorCode errorCode) {
      return new NChangeListError(9, new NErrorType(errorClass, errorCode), 0L);
   }
}
