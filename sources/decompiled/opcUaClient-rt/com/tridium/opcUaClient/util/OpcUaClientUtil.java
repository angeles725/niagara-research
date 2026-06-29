package com.tridium.opcUaClient.util;

import com.prosysopc.ua.MonitoredItemBase;
import com.prosysopc.ua.ServiceException;
import com.prosysopc.ua.UaNodeId;
import com.prosysopc.ua.client.AddressSpace;
import com.prosysopc.ua.client.AddressSpaceException;
import com.prosysopc.ua.client.MonitoredItem;
import com.prosysopc.ua.client.Subscription;
import com.prosysopc.ua.client.UaClient;
import com.prosysopc.ua.nodes.UaDataType;
import com.prosysopc.ua.nodes.UaInstance;
import com.prosysopc.ua.nodes.UaNode;
import com.prosysopc.ua.nodes.UaObject;
import com.prosysopc.ua.nodes.UaProperty;
import com.prosysopc.ua.nodes.UaType;
import com.prosysopc.ua.nodes.UaVariable;
import com.prosysopc.ua.stack.builtintypes.BuiltinsMap;
import com.prosysopc.ua.stack.builtintypes.ByteString;
import com.prosysopc.ua.stack.builtintypes.DataValue;
import com.prosysopc.ua.stack.builtintypes.DateTime;
import com.prosysopc.ua.stack.builtintypes.Enumeration;
import com.prosysopc.ua.stack.builtintypes.ExpandedNodeId;
import com.prosysopc.ua.stack.builtintypes.LocalizedText;
import com.prosysopc.ua.stack.builtintypes.NodeId;
import com.prosysopc.ua.stack.builtintypes.QualifiedName;
import com.prosysopc.ua.stack.builtintypes.Structure;
import com.prosysopc.ua.stack.builtintypes.UnsignedByte;
import com.prosysopc.ua.stack.builtintypes.UnsignedInteger;
import com.prosysopc.ua.stack.builtintypes.UnsignedLong;
import com.prosysopc.ua.stack.builtintypes.UnsignedShort;
import com.prosysopc.ua.stack.builtintypes.Variant;
import com.prosysopc.ua.stack.core.Argument;
import com.prosysopc.ua.stack.core.EUInformation;
import com.prosysopc.ua.stack.core.EnumValueType;
import com.prosysopc.ua.stack.core.NodeClass;
import com.prosysopc.ua.stack.core.ObjectIdentifiers;
import com.prosysopc.ua.stack.core.Range;
import com.prosysopc.ua.stack.core.ReadResponse;
import com.prosysopc.ua.stack.core.ReadValueId;
import com.prosysopc.ua.stack.core.ReferenceDescription;
import com.prosysopc.ua.stack.core.ReferenceTypeIdentifiers;
import com.prosysopc.ua.stack.core.TimestampsToReturn;
import com.prosysopc.ua.stack.core.AccessLevelType.Options;
import com.prosysopc.ua.stack.utils.AttributesUtil;
import com.prosysopc.ua.stack.utils.NumericRange;
import com.prosysopc.ua.typedictionary.EnumerationSpecification;
import com.prosysopc.ua.typedictionary.OptionSpecification;
import com.prosysopc.ua.typedictionary.StructureSpecification;
import com.prosysopc.ua.types.opcua.AnalogItemType;
import com.tridium.ndriver.discover.BNDiscoveryJob;
import com.tridium.opcUaClient.BOpcUaDevice;
import com.tridium.opcUaClient.BOpcUaNetwork;
import com.tridium.opcUaClient.point.BOpcUaNodeLearnEntry;
import com.tridium.opcUaCore.BUaArgument;
import com.tridium.opcUaCore.enums.BServerState;
import com.tridium.opcUaCore.util.OpcUaCoreUtil;
import java.nio.charset.StandardCharsets;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.SortedSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.baja.history.BBooleanTrendRecord;
import javax.baja.history.BEnumTrendRecord;
import javax.baja.history.BNumericTrendRecord;
import javax.baja.history.BStringTrendRecord;
import javax.baja.history.BTrendRecord;
import javax.baja.naming.SlotPath;
import javax.baja.status.BStatus;
import javax.baja.status.BStatusBoolean;
import javax.baja.status.BStatusEnum;
import javax.baja.status.BStatusNumeric;
import javax.baja.status.BStatusString;
import javax.baja.status.BStatusValue;
import javax.baja.sys.BAbsTime;
import javax.baja.sys.BBoolean;
import javax.baja.sys.BDouble;
import javax.baja.sys.BDynamicEnum;
import javax.baja.sys.BEnum;
import javax.baja.sys.BEnumRange;
import javax.baja.sys.BFacets;
import javax.baja.sys.BFloat;
import javax.baja.sys.BInteger;
import javax.baja.sys.BLong;
import javax.baja.sys.BString;
import javax.baja.util.BTypeSpec;

public final class OpcUaClientUtil {
   private static final Class<?>[] NUMERIC_CLASSES = new Class[]{
      Byte.class,
      UnsignedByte.class,
      Short.class,
      UnsignedShort.class,
      Integer.class,
      UnsignedInteger.class,
      Long.class,
      UnsignedLong.class,
      Float.class,
      Double.class
   };
   private static final Class<?>[] ENUM_CLASSES = new Class[]{
      Byte.class, UnsignedByte.class, Short.class, UnsignedShort.class, Integer.class, UnsignedInteger.class, Long.class, UnsignedLong.class
   };
   public static final String NC_OBJECT = "Object";
   public static final String NC_VARIABLE = "Variable";
   public static final String NC_METHOD = "Method";
   public static final String BUILT_IN_TYPES_BASE_URL = "http://opcfoundation.org/UA/";
   public static final String OPC_FOUNDATION_SECURITY_POLICY_URI = "http://opcfoundation.org/UA/SecurityPolicy#";
   public static final String OPC_FOUNDATION_SECURITY_POLICY_URI_REGEX = "http://opcfoundation\\.org/UA/SecurityPolicy#";
   public static final String OPC_FOUNDATION_TRANSPORT_URI = "http://opcfoundation.org/UA-Profile/Transport/";
   public static final String OPC_FOUNDATION_TRANSPORT_URI_REGEX = "http://opcfoundation\\.org/UA-Profile/Transport/";
   private static int discoveryCount;
   private static int progress;
   public static final Logger logger = Logger.getLogger("opcUaClient.util");
   public static final int NODE_CLASS = 0;
   public static final int BROWSE_NAME = 1;
   public static final int DISPLAY_NAME = 2;
   public static final int VALUE = 3;
   public static final int DATATYPE = 4;
   public static final int DESCRIPTION = 5;
   public static final int WRITE_MASK = 6;
   public static final int USER_WRITE_MASK = 7;
   public static final int ACCESS_LEVEL = 8;
   public static final int USER_ACCESS_LEVEL = 9;
   public static final int EVENT_NOTIFIER = 10;
   public static final int HISTORIZING = 11;

   private OpcUaClientUtil() {
   }

   public static void connect(BOpcUaDevice device) throws Exception {
      synchronized (device) {
         try {
            AccessController.doPrivileged((PrivilegedExceptionAction<Void>)(() -> {
               if (device.uaClient != null && device.uaClient.isConnected()) {
                  device.uaClient.disconnect();
                  device.uaClient = null;
               }

               device.uaClient = new UaClient(device.getServerEndpointUrl());
               device.initialize();
               device.configOk();
               device.ping();
               return null;
            }));
         } catch (PrivilegedActionException var4) {
            device.configFail(OpcUaCoreUtil.getLocalizedMessage(var4.getException()));
            throw var4.getException();
         }
      }
   }

   public static void disconnect(BOpcUaDevice device) throws Exception {
      synchronized (device) {
         try {
            AccessController.doPrivileged((PrivilegedExceptionAction<Void>)(() -> {
               if (device.uaClient != null && device.uaClient.isConnected()) {
                  device.uaClient.disconnect();
                  device.uaClient = null;
               }

               if (device.isCommReset()) {
                  device.setServerState(BServerState.ResettingConnection);
               } else {
                  device.setServerState(BServerState.Unknown);
               }

               OpcUaMonitoredDataItemUtil.clearBulkListeners();
               return null;
            }));
         } catch (PrivilegedActionException var4) {
            throw var4.getException();
         }
      }
   }

   public static NodeId getUaObjectRoot(UaClient uaClient) {
      try {
         return AccessController.doPrivileged((PrivilegedExceptionAction<NodeId>)(() -> {
            AddressSpace addressSpace = uaClient.getAddressSpace();
            addressSpace.setMaxReferencesPerNode(1000);
            addressSpace.setReferenceTypeId(ReferenceTypeIdentifiers.HierarchicalReferences);

            for (ReferenceDescription reference : addressSpace.browse(ObjectIdentifiers.RootFolder)) {
               String displayName = reference.getDisplayName().getText();
               if ("Objects".equals(displayName.trim())) {
                  return addressSpace.getNamespaceTable().toNodeId(reference.getNodeId());
               }
            }

            return null;
         }));
      } catch (PrivilegedActionException var2) {
         if (logger.isLoggable(Level.FINE)) {
            logger.log(Level.SEVERE, "Exception occurred while getting UaObjectRoot", (Throwable)var2);
         } else {
            logger.log(Level.SEVERE, "Exception occurred while getting UaObjectRoot: " + var2);
         }

         return null;
      }
   }

   public static boolean isNodeAlreadyAdded(BOpcUaNodeLearnEntry root, BOpcUaNodeLearnEntry newEntry) {
      if (newEntry == null) {
         return false;
      } else {
         String addName = SlotPath.escape(newEntry.getUaDisplayName());
         BOpcUaNodeLearnEntry existingNode = (BOpcUaNodeLearnEntry)root.get(addName);
         return existingNode != null && existingNode.getUaNodeId().equals(newEntry.getUaNodeId());
      }
   }

   public static int learn(
      BOpcUaDevice uaDevice,
      BOpcUaNodeLearnEntry root,
      NodeId nodeId,
      BNDiscoveryJob job,
      int level,
      int expectedLearnCount,
      boolean excludeServer,
      boolean excludeTypesFolder
   ) {
      boolean isRoot = level == 0;
      if (isRoot) {
         discoveryCount = 0;
         if (job != null) {
            job.setProgress(5);
         }
      }

      if (job != null && job.isCanceled()) {
         return -1;
      } else {
         AddressSpace addressSpace = uaDevice.uaClient.getAddressSpace();

         try {
            if (level < 2 && job != null) {
               job.log().message(BOpcUaNetwork.lex.getText("clientPointLearn.processing", new Object[]{root.getUaNodeName()}));
            }

            if (root.getName() == null) {
               String var10000 = "nullName";
            } else {
               root.getName();
            }

            List<ReferenceDescription> references = browseAddressSpace(addressSpace, nodeId);
            boolean isLeafNode = true;

            for (ReferenceDescription reference : references) {
               if (job != null && job.isCanceled()) {
                  break;
               }

               NodeId nextId = addressSpace.getNamespaceTable().toNodeId(reference.getNodeId());
               if (!nextId.toString().equals(nodeId.toString() + ".Alarm")) {
                  if (excludeServer && nextId.equals(ObjectIdentifiers.Server)) {
                     if (job != null) {
                        job.log().message("Excluding Server branch.");
                     }
                  } else if (!excludeTypesFolder || !nextId.equals(ObjectIdentifiers.TypesFolder)) {
                     BOpcUaNodeLearnEntry learnEntry = BOpcUaNodeLearnEntry.make(addressSpace, reference, nextId, uaDevice.uaClient);
                     if (!isNodeAlreadyAdded(root, learnEntry)) {
                        if (++progress > expectedLearnCount) {
                           progress = 0;
                        }

                        if (job != null) {
                           job.setProgress(progress * 100 / expectedLearnCount);
                        }

                        discoveryCount++;
                        level++;
                        isLeafNode = false;
                        if (learnEntry != null) {
                           String addName = learnEntry.getUaDisplayName();
                           root.add(SlotPath.escape(addName) + '?', learnEntry, 2);
                           learn(uaDevice, learnEntry, nextId, job, level, expectedLearnCount, excludeServer, excludeTypesFolder);
                           uaDevice.setLastLearnedInfo(toServerRootPath(learnEntry));
                        }

                        level--;
                     }
                  } else if (job != null) {
                     job.log().message("Excluding TypesFolder branch.");
                  }
               }
            }

            if (isLeafNode) {
               root.initHasAddableDescendant();
            }
         } catch (Exception var18) {
            if (logger.isLoggable(Level.FINE)) {
               logger.log(
                  Level.SEVERE, "Exception occurred during OpcUaClient learn: level = " + level + ", root name = " + toServerRootPath(root), (Throwable)var18
               );
            } else {
               logger.log(
                  Level.SEVERE, "Exception occurred during OpcUaClient learn: level = " + level + ", root name = " + toServerRootPath(root), var18.getMessage()
               );
            }

            if (job != null) {
               job.log().message("Problem with: " + toServerRootPath(root));
            }
         }

         if (job != null) {
            switch (level) {
               case 0:
                  job.log().message(BOpcUaNetwork.lex.getText("clientPointLearn.discoverCount", new Object[]{discoveryCount}));
                  break;
               case 1:
                  if (!root.getHasAddableDescendant()) {
                     job.log().message(BOpcUaNetwork.lex.getText("clientPointLearn.pruned", new Object[]{root.getUaNodeName()}));
                  }
            }
         }

         return discoveryCount;
      }
   }

   public static UaNode getAddressSpaceNode(AddressSpace addressSpace, NodeId nodeId) {
      try {
         return AccessController.doPrivileged((PrivilegedExceptionAction<UaNode>)(() -> addressSpace.getNode(nodeId)));
      } catch (PrivilegedActionException var3) {
         if (logger.isLoggable(Level.FINE)) {
            logger.log(Level.SEVERE, "Exception occurred while getting node data " + nodeId, (Throwable)var3);
         } else {
            logger.log(Level.SEVERE, "Exception occurred while getting node data " + nodeId + ": " + var3);
         }

         return null;
      }
   }

   public static String getDisplayName(ReferenceDescription reference, DataValue[] arrayOfNodes, String fallbackNodeName) {
      String displayName = arrayOfNodes[2].getStatusCode().isNotBad() && arrayOfNodes[2].getValue() != null && !getNodeLocalizedText(arrayOfNodes[2]).isEmpty()
         ? getNodeLocalizedText(arrayOfNodes[2])
         : reference.getDisplayName().getText();
      if (displayName.isEmpty() && !fallbackNodeName.isEmpty()) {
         displayName = fallbackNodeName;
      }

      return displayName;
   }

   public static String getNodeName(ReferenceDescription reference, DataValue[] arrayOfNodes) {
      return arrayOfNodes[1].getStatusCode().isNotBad() && arrayOfNodes[1].getValue() != null && !getNodeLocalizedText(arrayOfNodes[1]).isEmpty()
         ? getNodeLocalizedText(arrayOfNodes[1])
         : reference.getBrowseName().getName();
   }

   public static DataValue readNodeAttribute(UaNode node, UnsignedInteger attribute) {
      try {
         return AccessController.doPrivileged((PrivilegedExceptionAction<DataValue>)(() -> node.readAttribute(attribute)));
      } catch (PrivilegedActionException var3) {
         if (logger.isLoggable(Level.FINE)) {
            logger.log(Level.SEVERE, "Exception occurred while reading node attributes " + attribute, (Throwable)var3);
         } else {
            logger.log(Level.SEVERE, "Exception occurred while reading node attributes " + attribute + ": " + var3);
         }

         return null;
      }
   }

   public static List<ReferenceDescription> browseAddressSpace(AddressSpace addressSpace, NodeId nodeId) {
      try {
         return AccessController.doPrivileged((PrivilegedExceptionAction<List<ReferenceDescription>>)(() -> addressSpace.browse(nodeId)));
      } catch (PrivilegedActionException var3) {
         if (logger.isLoggable(Level.FINE)) {
            logger.log(Level.SEVERE, "Exception occurred while browsing address space " + nodeId, (Throwable)var3);
         } else {
            logger.log(Level.SEVERE, "Exception occurred while browsing address space " + nodeId + ": " + var3);
         }

         return null;
      }
   }

   public static Variant[] call(UaClient uaClient, NodeId objectNodeId, NodeId methodNodeId, Variant[] methodArg) throws PrivilegedActionException {
      return AccessController.doPrivileged((PrivilegedExceptionAction<Variant[]>)(() -> uaClient.call(objectNodeId, methodNodeId, methodArg)));
   }

   public static String getNodeName(UaNode node, String defaultName) {
      String returnName = defaultName;
      QualifiedName browseName = node.getBrowseName();
      if (browseName != null) {
         returnName = browseName.getName();
      }

      return returnName;
   }

   public static Optional<DataValue> getNodeDataTypeNode(DataValue dataTypeDV) {
      Optional<DataValue> rtnValue = Optional.empty();
      if (dataTypeDV != null) {
         try {
            rtnValue = Optional.of(dataTypeDV);
         } catch (Exception var3) {
         }
      }

      return rtnValue;
   }

   public static void addItemToSubscription(Subscription subscription, MonitoredItem item, boolean throwException) throws Exception {
      try {
         AccessController.doPrivileged((PrivilegedExceptionAction<Void>)(() -> {
            subscription.addItem(item);
            return null;
         }));
      } catch (PrivilegedActionException var4) {
         if (logger.isLoggable(Level.FINE)) {
            logger.log(Level.SEVERE, "Exception occurred while adding subscription item " + item.getNodeId().toString(), (Throwable)var4);
         } else {
            logger.log(Level.SEVERE, "Exception occurred while adding subscription item " + item.getNodeId().toString() + ": " + var4);
         }

         if (throwException) {
            throw var4.getException();
         }
      }
   }

   public static void removeItemFromSubscription(Subscription subscription, MonitoredItemBase item, boolean throwException) throws Exception {
      try {
         AccessController.doPrivileged((PrivilegedExceptionAction<Void>)(() -> {
            subscription.removeItem(item);
            return null;
         }));
      } catch (PrivilegedActionException var4) {
         if (logger.isLoggable(Level.FINE)) {
            logger.log(Level.SEVERE, "Exception occurred while removing subscription item " + item.getNodeId().toString(), (Throwable)var4);
         } else {
            logger.log(Level.SEVERE, "Exception occurred while removing subscription item " + item.getNodeId().toString() + ": " + var4);
         }

         if (throwException) {
            throw var4.getException();
         }
      }
   }

   public static void addSubscription(UaClient uaClient, Subscription subscription) {
      try {
         AccessController.doPrivileged((PrivilegedExceptionAction<Void>)(() -> {
            uaClient.addSubscription(subscription);
            return null;
         }));
      } catch (PrivilegedActionException var3) {
         if (logger.isLoggable(Level.FINE)) {
            logger.log(Level.SEVERE, "Exception occurred while adding subscription " + subscription, (Throwable)var3);
         } else {
            logger.log(Level.SEVERE, "Exception occurred while adding subscription " + subscription + ": " + var3);
         }
      }
   }

   public static ReadResponse readNodes(UaClient uaClient, Double maxAge, TimestampsToReturn timestampsToReturn, ReadValueId... valueIds) {
      try {
         return AccessController.doPrivileged((PrivilegedExceptionAction<ReadResponse>)(() -> uaClient.read(maxAge, timestampsToReturn, valueIds)));
      } catch (PrivilegedActionException var5) {
         if (logger.isLoggable(Level.FINE)) {
            logger.log(Level.SEVERE, "Exception occurred while reading nodes", (Throwable)var5);
         } else {
            logger.log(Level.SEVERE, "Exception occurred while reading nodes: " + var5);
         }

         return null;
      }
   }

   public static DataValue[] historyReadRaw(
      UaClient uaClient,
      NodeId nodeId,
      DateTime startTime,
      DateTime endTime,
      UnsignedInteger numValuesPerNode,
      boolean returnBounds,
      NumericRange indexRange,
      TimestampsToReturn timestampsToReturn
   ) {
      try {
         return AccessController.doPrivileged((PrivilegedExceptionAction<DataValue[]>)(() -> {
            UaNode node = uaClient.getAddressSpace().getNode(nodeId);
            UaVariable variable = (UaVariable)node;
            if (!variable.getAccessLevel().contains(new OptionSpecification[]{Options.HistoryRead})) {
               logger.log(Level.INFO, "The variable does not have history");
               return null;
            } else if (!variable.getUserAccessLevel().contains(new OptionSpecification[]{Options.HistoryRead})) {
               logger.log(Level.INFO, "The variable has history, but it is not readable with this user account");
               return null;
            } else {
               return uaClient.historyReadRaw(nodeId, startTime, endTime, numValuesPerNode, returnBounds, indexRange, timestampsToReturn);
            }
         }));
      } catch (PrivilegedActionException var9) {
         if (logger.isLoggable(Level.FINE)) {
            logger.log(Level.SEVERE, "Exception occurred while reading raw history data", (Throwable)var9);
         } else {
            logger.log(Level.SEVERE, "Exception occurred while reading raw history data: " + var9);
         }

         return null;
      }
   }

   public static long getValueDataTypeIdentifier(Object nodeIdValue) {
      try {
         if (nodeIdValue instanceof UnsignedInteger) {
            return ((UnsignedInteger)nodeIdValue).getValue();
         }

         if (logger.isLoggable(Level.FINE)) {
            logger.fine("getValueDataTypeIdentifier(): Unknown dataType: " + nodeIdValue);
         }
      } catch (Exception var2) {
      }

      return -1L;
   }

   public static String readValue(UaClient client, NodeId nodeId, String defaultValue) {
      try {
         return AccessController.doPrivileged((PrivilegedExceptionAction<String>)(() -> client.readValue(nodeId).getValue().toString(false)));
      } catch (PrivilegedActionException var4) {
         if (logger.isLoggable(Level.FINE)) {
            logger.log(Level.SEVERE, "Exception occurred while reading value " + nodeId, (Throwable)var4);
         } else {
            logger.log(Level.SEVERE, "Exception occurred while reading value " + nodeId + ": " + var4);
         }

         return defaultValue;
      }
   }

   public static BAbsTime dataValueToAbsTime(DataValue dv) {
      Object objValue = dv.getValue().getValue();
      return objValue instanceof DateTime ? dateTimeToAbsTime((DateTime)objValue) : BAbsTime.NULL;
   }

   public static BAbsTime dateTimeToAbsTime(DateTime dt) {
      long timeInMillis = dt.getTimeInMillis();
      return BAbsTime.make(timeInMillis);
   }

   public static Object getDataValueValue(DataValue dataValue) {
      Object dataValueValue = "";
      if (dataValue != null) {
         Variant variant = dataValue.getValue();
         boolean isArray = variant.isArray();
         dataValueValue = variant.getValue();
         if (dataValueValue != null) {
            if (dataValueValue instanceof LocalizedText) {
               dataValueValue = ((LocalizedText)dataValueValue).getText();
            } else if (dataValueValue instanceof EUInformation) {
               dataValueValue = ((EUInformation)dataValueValue).getDisplayName();
            } else if (dataValueValue instanceof Range) {
               Double high = ((Range)dataValueValue).getHigh();
               Double low = ((Range)dataValueValue).getLow();
               dataValueValue = low + "," + high;
            } else if (dataValueValue instanceof LocalizedText[] || dataValueValue instanceof String[]) {
               Object[] text = (Object[])dataValueValue;
               StringBuilder sb = new StringBuilder();

               for (int i = 0; i < text.length; i++) {
                  if (i > 0) {
                     sb.append(',');
                  }

                  if (text[i] instanceof LocalizedText) {
                     sb.append(((LocalizedText)text[i]).getText());
                  } else {
                     sb.append(text[i]);
                  }
               }

               dataValueValue = sb.toString();
            } else if (dataValueValue instanceof byte[]) {
               byte[] bytes = (byte[])dataValueValue;
               dataValueValue = new String(bytes);
            } else if (dataValueValue instanceof EnumValueType[]) {
               EnumValueType[] valueTypes = (EnumValueType[])dataValueValue;
               HashMap<Integer, String> enumKeyValuesMap = new HashMap<>();

               for (EnumValueType valueType : valueTypes) {
                  enumKeyValuesMap.put(Math.toIntExact(valueType.getValue()), valueType.getDisplayName().toString());
               }

               return enumKeyValuesMap.toString();
            }
         }
      }

      return dataValueValue;
   }

   public static String dataValueValueToString(Object dataValueValue) {
      try {
         return AccessController.doPrivileged(dataValueValue::toString);
      } catch (PrivilegedActionException var2) {
         if (logger.isLoggable(Level.FINE)) {
            logger.log(Level.SEVERE, "Exception occurred while converting value to String " + dataValueValue, (Throwable)var2);
         } else {
            logger.log(Level.SEVERE, "Exception occurred while converting value to String " + dataValueValue + ": " + var2);
         }

         return null;
      }
   }

   public static String variantToString(Variant variant, boolean includeCompositeClass) {
      try {
         return AccessController.doPrivileged((PrivilegedExceptionAction<String>)(() -> variant.toString(includeCompositeClass)));
      } catch (PrivilegedActionException var3) {
         if (logger.isLoggable(Level.FINE)) {
            logger.log(Level.SEVERE, "Exception occurred while converting variant to String " + variant, (Throwable)var3);
         } else {
            logger.log(Level.SEVERE, "Exception occurred while converting variant to String " + variant + ": " + var3);
         }

         return "";
      }
   }

   public static String getNodeLocalizedText(DataValue dv) {
      String description = "";
      Object valueValue = dv.getValue().getValue();
      if (valueValue != null && !valueValue.equals("null")) {
         if (valueValue instanceof LocalizedText) {
            description = ((LocalizedText)valueValue).getText();
         } else if (valueValue instanceof QualifiedName) {
            description = ((QualifiedName)valueValue).getName();
         } else {
            description = dv.getValue().toString(false);
         }
      }

      if (description == null) {
         description = "";
      }

      return description;
   }

   public static BTypeSpec getTypeSpec(Variant dataValue) {
      return getTypeSpec(dataValue, "");
   }

   public static BTypeSpec getTypeSpec(Variant dataValue, String dataType) {
      Objects.requireNonNull(dataValue);
      Objects.requireNonNull(dataType);
      Object value = dataValue.getValue();
      return getTypeSpec(value, dataType);
   }

   public static BTypeSpec getTypeSpec(Object value, String dataType) {
      if (value == null) {
         return classToTypeSpec(dataType);
      } else if (isBoolean(value)) {
         return BTypeSpec.make(BBoolean.TYPE);
      } else if (isEnum(value)) {
         return BTypeSpec.make(BEnum.TYPE);
      } else {
         return isNumeric(value) ? BTypeSpec.make(BDouble.TYPE) : BTypeSpec.make(BString.TYPE);
      }
   }

   public static Class<?> getValueClass(Variant dataValue) {
      return dataValue.getValue().getClass();
   }

   public static boolean isStructure(Object value) {
      return value == null ? false : value instanceof Structure;
   }

   public static boolean isBoolean(Object value) {
      return value instanceof Boolean || value instanceof Boolean[] || value.getClass().getTypeName().startsWith(Boolean.class.getTypeName());
   }

   public static boolean isNumeric(Object value) {
      return isObjectNumeric(value);
   }

   public static boolean isString(Object value) {
      return value instanceof String || value instanceof String[] || value.getClass().getTypeName().startsWith(String.class.getTypeName());
   }

   public static boolean isEnum(Object value) {
      return value instanceof Enumeration || value instanceof Enumeration[] || value.getClass().getTypeName().startsWith(Enumeration.class.getTypeName());
   }

   public static void printCurrentNode(UaClient uaClient, NodeId nodeId, String indent) {
      if (uaClient != null && uaClient.isConnected()) {
         try {
            UaNode node = uaClient.getAddressSpace().getNode(nodeId);
            if (node == null) {
               return;
            }

            if (node instanceof UaObject) {
               UaObject object = (UaObject)node;
               UnsignedInteger[] supportedAttributes = object.getSupportedAttributes();

               for (UnsignedInteger supportedAttribute : supportedAttributes) {
                  String s = AttributesUtil.toString(supportedAttribute);
                  DataValue dataValue = object.readAttribute(supportedAttribute);
                  println("[" + s + " = " + dataValue.getValue() + "]", indent + "  ");
               }

               UaProperty[] properties = object.getProperties();
               if (properties != null) {
                  for (UaProperty property : properties) {
                     String propName = property.getDisplayName().getText();
                     Variant value = property.getValue().getValue();
                     println("(" + propName + "=" + value + ")", indent + "  ");
                  }
               }
            }
         } catch (AddressSpaceException | ServiceException var13) {
            logger.severe("Exception occurred printing current node: " + var13);
         }
      }
   }

   public static String getCurrentNodeAsString(UaNode node, String indent) {
      String analogInfoStr = "";
      String nodeStr = node.getDisplayName().getText();
      UaType type = null;
      if (node instanceof UaInstance) {
         type = ((UaInstance)node).getTypeDefinition();
      }

      String typeStr = type == null ? nodeClassToStr(node.getNodeClass()) : type.getDisplayName().getText();
      if (node instanceof AnalogItemType) {
         try {
            AnalogItemType analogNode = (AnalogItemType)node;
            EUInformation units = analogNode.getEngineeringUnits();
            analogInfoStr = units == null ? "" : " Units=" + units.getDisplayName().getText();
            Range range = analogNode.getEURange();
            analogInfoStr = analogInfoStr + (range == null ? "" : String.format(" Range=(%f; %f)", range.getLow(), range.getHigh()));
         } catch (Exception var12) {
            logger.severe("Exception occurred getting current node as String: " + var12);
         }
      }

      if (node instanceof UaObject) {
         UaProperty[] properties = node.getProperties();

         for (UaProperty property1 : properties) {
            String var11 = property1.getDisplayName() + " = " + property1.getValue();
         }
      }

      return String.format(indent + "*** Current Node: %s: %s (ID: %s)%s", nodeStr, typeStr, node.getNodeId(), analogInfoStr);
   }

   public static String eventFieldsToString(UaClient client, QualifiedName[] fieldNames, Variant[] fieldValues) {
      StringBuilder sb = new StringBuilder();

      for (int i = 0; i < fieldValues.length; i++) {
         Object fieldValue = fieldValues[i] == null ? null : fieldValues[i].getValue();

         try {
            UaNode node = null;
            if (fieldValue instanceof NodeId) {
               node = client.getAddressSpace().getNode((NodeId)fieldValue);
            } else if (fieldValue instanceof ExpandedNodeId) {
               node = client.getAddressSpace().getNode((ExpandedNodeId)fieldValue);
            }

            if (node != null) {
               fieldValue = String.format("%s {%s}", node.getBrowseName(), fieldValue);
            }
         } catch (Exception var7) {
         }

         if (i < fieldNames.length) {
            QualifiedName fieldName = fieldNames[i];
            sb.append(String.format("%s=%s;%n     ", fieldName.getName(), fieldValue));
         } else {
            sb.append("Node=").append(fieldValue);
         }
      }

      return sb.toString();
   }

   public static String eventToString(UaClient client, NodeId nodeId, QualifiedName[] fieldNames, Variant[] fieldValues) {
      return String.format("Node: %s Fields:%n     %s", nodeId, eventFieldsToString(client, fieldNames, fieldValues));
   }

   public static QualifiedName[] createBrowsePath(QualifiedName qualifiedName) {
      if (!qualifiedName.getName().contains("/")) {
         return new QualifiedName[]{qualifiedName};
      } else {
         int namespaceIndex = qualifiedName.getNamespaceIndex();
         String[] names = qualifiedName.getName().split("/");
         QualifiedName[] result = new QualifiedName[names.length];

         for (int i = 0; i < names.length; i++) {
            result[i] = new QualifiedName(namespaceIndex, names[i]);
         }

         return result;
      }
   }

   private static String toServerRootPath(BOpcUaNodeLearnEntry learnEntry) {
      String s = learnEntry.getSlotPath().toString();
      int i = s.indexOf("/serverRoot/");
      return s.substring(i);
   }

   private static String nodeClassToStr(NodeClass nodeClass) {
      return "[" + nodeClass + "]";
   }

   private static void println(String string, String indent) {
      System.out.println(indent + string);
   }

   public static BStatusValue makeStatusValue(DataValue dataValue, BTrendRecord histRecord) {
      Variant value = dataValue.getValue();
      Object varValue = value.getValue();
      if (varValue instanceof Object[]) {
         return null;
      } else {
         BStatusValue rtnValue;
         if (histRecord instanceof BBooleanTrendRecord && varValue instanceof Boolean) {
            rtnValue = new BStatusBoolean((Boolean)varValue);
         } else if (histRecord instanceof BNumericTrendRecord && isObjectNumeric(varValue)) {
            double dValue = Double.parseDouble(varValue.toString());
            rtnValue = new BStatusNumeric(dValue);
         } else if (histRecord instanceof BEnumTrendRecord && isObjectEnum(varValue)) {
            int iValue = Integer.parseInt(varValue.toString());
            BEnum v = BDynamicEnum.make(iValue);
            rtnValue = new BStatusEnum(v);
         } else if (histRecord instanceof BStringTrendRecord && varValue instanceof String) {
            rtnValue = new BStatusString((String)varValue);
         } else if (histRecord instanceof BStringTrendRecord && varValue instanceof LocalizedText) {
            rtnValue = new BStatusString(((LocalizedText)varValue).getText());
         } else if (histRecord instanceof BStringTrendRecord && varValue instanceof QualifiedName) {
            rtnValue = new BStatusString(((QualifiedName)varValue).getName());
         } else if (histRecord instanceof BStringTrendRecord && varValue instanceof byte[]) {
            String s = new String((byte[])varValue);
            rtnValue = new BStatusString(s);
         } else if (histRecord instanceof BStringTrendRecord && varValue instanceof ByteString) {
            String s = new String(((ByteString)varValue).getValue());
            rtnValue = new BStatusString(s);
         } else {
            rtnValue = new BStatusString(varValue.toString());
         }

         return rtnValue;
      }
   }

   public static BStatusValue makeStatusValue(DataValue dataValue, BStatusValue statusValue, int[] arrayIndex, String structurePath) {
      BStatusValue rtnValue = statusValue;
      Variant value = dataValue.getValue();
      Object varValue = value.getValue();
      Object setValue = null;
      boolean isUnwritten = dataValue.getSourceTimestamp() == null && dataValue.getServerTimestamp() == null;
      BStatus valueStatus = isUnwritten ? BStatus.stale : BStatus.ok;
      if (isStructure(varValue)) {
         String[] keySequence = structurePath.split(">");
         Structure structure = (Structure)varValue;

         for (String key : keySequence) {
            varValue = structure.get(key);
            if (isStructure(varValue)) {
               structure = (Structure)varValue;
            }
         }
      }

      if (varValue instanceof Object[]) {
         switch (arrayIndex.length) {
            case 1:
               setValue = ((Object[])varValue)[arrayIndex[0]];
               break;
            case 2:
               setValue = ((Object[][])varValue)[arrayIndex[1]][arrayIndex[0]];
               break;
            case 3:
               setValue = ((Object[][][])varValue)[arrayIndex[2]][arrayIndex[1]][arrayIndex[0]];
               break;
            case 4:
               setValue = ((Object[][][][])varValue)[arrayIndex[3]][arrayIndex[2]][arrayIndex[1]][arrayIndex[0]];
               break;
            case 5:
               setValue = ((Object[][][][][])varValue)[arrayIndex[4]][arrayIndex[3]][arrayIndex[2]][arrayIndex[1]][arrayIndex[0]];
               break;
            default:
               logger.severe("Array dimension size not supported: " + arrayIndex.length);
         }
      } else {
         setValue = varValue;
      }

      if (statusValue instanceof BStatusBoolean && setValue instanceof Boolean) {
         rtnValue = new BStatusBoolean((Boolean)setValue, valueStatus);
      } else if (!(statusValue instanceof BStatusNumeric) || !isObjectNumeric(setValue) && !isObjectNumeric(varValue)) {
         if (statusValue instanceof BStatusString) {
            if (setValue instanceof String) {
               rtnValue = new BStatusString((String)setValue, valueStatus);
            } else if (setValue instanceof LocalizedText) {
               rtnValue = new BStatusString(((LocalizedText)setValue).getText(), valueStatus);
            } else if (setValue instanceof QualifiedName) {
               rtnValue = new BStatusString(((QualifiedName)setValue).getName(), valueStatus);
            } else if (setValue instanceof byte[]) {
               String s = new String((byte[])setValue, StandardCharsets.UTF_8);
               rtnValue = new BStatusString(s, valueStatus);
            } else if (setValue == null) {
               String s = "-";
               rtnValue = new BStatusString(s, valueStatus);
            } else {
               rtnValue = new BStatusString(setValue.toString(), valueStatus);
            }
         } else if (statusValue instanceof BStatusEnum && isEnum(setValue)) {
            rtnValue = new BStatusEnum(BDynamicEnum.make(((Enumeration)setValue).getValue()), valueStatus);
         } else if (statusValue instanceof BStatusEnum && isObjectEnum(varValue)) {
            rtnValue = new BStatusEnum(BDynamicEnum.make(Integer.parseInt(setValue.toString())), valueStatus);
         }
      } else {
         rtnValue = new BStatusNumeric(Double.parseDouble(setValue.toString()), valueStatus);
      }

      return rtnValue;
   }

   public static double makeDoubleValue(Object value) throws NumberFormatException {
      return Double.parseDouble(value.toString());
   }

   public static BFacets makeEnumFacets(EnumerationSpecification specification) {
      SortedSet<Enumeration> valuesSet = specification.getAllEnumValues();
      int[] ordinals = new int[valuesSet.size()];
      String[] tags = new String[valuesSet.size()];
      int index = 0;

      for (Enumeration eachEnum : valuesSet) {
         ordinals[index] = eachEnum.getValue();
         tags[index] = (String)specification.getIntToStringMappings().get(eachEnum.getValue());
         index++;
      }

      return BFacets.makeEnum(BEnumRange.make(ordinals, tags));
   }

   public static long getDataTypeIdentifier(NodeId dataTypeNodeId) {
      long dataTypeIdentifier = -1L;

      try {
         dataTypeIdentifier = Long.parseLong(dataTypeNodeId.getValue().toString());
      } catch (Exception var4) {
         if (logger.isLoggable(Level.FINE)) {
            logger.log(Level.FINE, "Failed to determine data type identifier for nodeId: " + dataTypeNodeId, (Throwable)var4);
         }
      }

      return dataTypeIdentifier;
   }

   public static StructureSpecification getStructureSpecification(UaClient uaClient, UaDataType dataType) {
      try {
         return uaClient.getTypeDictionary().getStructureSpecification(UaNodeId.fromStandard(dataType.getNodeId()));
      } catch (Exception var5) {
         if (logger.isLoggable(Level.FINE)) {
            logger.log(Level.FINE, "Failed to determine standard structure specification for dataType - " + dataType.getBrowseName(), (Throwable)var5);
         }

         try {
            return uaClient.getTypeDictionary().getStructureSpecification(UaNodeId.fromLocal(dataType.getNodeId(), uaClient.getNamespaceTable()));
         } catch (Exception var4) {
            if (logger.isLoggable(Level.FINE)) {
               logger.log(Level.WARNING, "Failed to determine structure specification for dataType - " + dataType.getBrowseName(), (Throwable)var4);
            } else {
               logger.log(Level.WARNING, "Failed to determine structure specification for dataType - " + dataType.getBrowseName() + ": " + var4.getMessage());
            }

            return null;
         }
      }
   }

   public static BUaArgument makeUaArgument(Argument arg) {
      try {
         return AccessController.doPrivileged((PrivilegedExceptionAction<BUaArgument>)(() -> {
            NodeId dataType = arg.getDataType();
            Class<?> argType = (Class<?>)BuiltinsMap.ID_CLASS_MAP.getRight(dataType);
            BTypeSpec typeSpec = classToTypeSpec(argType.getSimpleName());
            String dimensions = uIntArrayToString(arg.getArrayDimensions());
            LocalizedText description = arg.getDescription();
            String argPrompt = "no description";
            if (description != null && description.getText() != null) {
               argPrompt = description.getText();
            }

            return new BUaArgument(typeSpec, dataType.toString(), dimensions, argPrompt);
         }));
      } catch (PrivilegedActionException var2) {
         if (logger.isLoggable(Level.FINE)) {
            logger.log(Level.SEVERE, "Exception occurred while making argument " + arg, (Throwable)var2);
         } else {
            logger.log(Level.SEVERE, "Exception occurred while making argument " + arg + ": " + var2);
         }

         return null;
      }
   }

   public static String uIntArrayToString(UnsignedInteger[] uiArray) {
      if (uiArray != null && uiArray.length != 0) {
         StringBuilder sb = new StringBuilder();

         for (UnsignedInteger dimSize : uiArray) {
            sb.append('[').append(dimSize.longValue()).append(']');
         }

         return sb.toString();
      } else {
         return "";
      }
   }

   public static BTypeSpec classToTypeSpec(String className) {
      switch (className) {
         case "Boolean":
            return BTypeSpec.make(BBoolean.TYPE);
         case "Byte":
         case "UnsignedByte":
         case "Short":
         case "UnsignedShort":
         case "Integer":
            return BTypeSpec.make(BInteger.TYPE);
         case "UnsignedInteger":
         case "Long":
         case "UnsignedLong":
            return BTypeSpec.make(BLong.TYPE);
         case "Float":
            return BTypeSpec.make(BFloat.TYPE);
         case "Double":
            return BTypeSpec.make(BDouble.TYPE);
         case "byte[]":
         case "ByteString":
         case "String":
         case "LocalizedText":
            return BTypeSpec.make(BString.TYPE);
         default:
            return BTypeSpec.NULL;
      }
   }

   public static boolean isObjectNumeric(Object object) {
      if (object != null) {
         Class<?> objectClass = object.getClass();

         for (Class<?> numericClass : NUMERIC_CLASSES) {
            if (object.getClass().equals(numericClass)) {
               return true;
            }

            if (object.getClass().getTypeName().startsWith(numericClass.getTypeName())) {
               return true;
            }
         }
      }

      return false;
   }

   public static boolean isObjectEnum(Object object) {
      for (Class<?> enumClass : ENUM_CLASSES) {
         if (object.getClass().equals(enumClass)) {
            return true;
         }

         if (object.getClass().getTypeName().startsWith(enumClass.getTypeName())) {
            return true;
         }
      }

      return false;
   }

   public static BAbsTime getModifiedStartTime(BAbsTime lastTimestamp, BAbsTime initialHistoryArchiveFromDate) {
      if ((BAbsTime.DEFAULT.equals(lastTimestamp) || BAbsTime.DEFAULT.isAfter(lastTimestamp)) && !BAbsTime.DEFAULT.equals(initialHistoryArchiveFromDate)) {
         lastTimestamp = initialHistoryArchiveFromDate;
      }

      return lastTimestamp;
   }
}
