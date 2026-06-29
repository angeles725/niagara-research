package com.tridium.opcUaClient.point;

import com.prosysopc.ua.ServiceException;
import com.prosysopc.ua.UaNodeId;
import com.prosysopc.ua.client.AddressSpace;
import com.prosysopc.ua.client.UaClient;
import com.prosysopc.ua.nodes.UaNode;
import com.prosysopc.ua.nodes.UaProperty;
import com.prosysopc.ua.nodes.UaReference;
import com.prosysopc.ua.stack.builtintypes.BuiltinsMap;
import com.prosysopc.ua.stack.builtintypes.DataValue;
import com.prosysopc.ua.stack.builtintypes.DateTime;
import com.prosysopc.ua.stack.builtintypes.NodeId;
import com.prosysopc.ua.stack.builtintypes.Structure;
import com.prosysopc.ua.stack.builtintypes.UnsignedByte;
import com.prosysopc.ua.stack.builtintypes.UnsignedInteger;
import com.prosysopc.ua.stack.builtintypes.UnsignedLong;
import com.prosysopc.ua.stack.builtintypes.UnsignedShort;
import com.prosysopc.ua.stack.builtintypes.Variant;
import com.prosysopc.ua.stack.common.NamespaceTable;
import com.prosysopc.ua.stack.core.AccessLevelType;
import com.prosysopc.ua.stack.core.AttributeWriteMask;
import com.prosysopc.ua.stack.core.Attributes;
import com.prosysopc.ua.stack.core.EUInformation;
import com.prosysopc.ua.stack.core.EventNotifierType;
import com.prosysopc.ua.stack.core.NodeAttributes;
import com.prosysopc.ua.stack.core.NodeClass;
import com.prosysopc.ua.stack.core.ReferenceDescription;
import com.prosysopc.ua.stack.core.ReferenceTypeIdentifiers;
import com.prosysopc.ua.stack.core.TimestampsToReturn;
import com.prosysopc.ua.stack.core.EventNotifierType.Options;
import com.prosysopc.ua.typedictionary.EnumerationSpecification;
import com.prosysopc.ua.typedictionary.FieldSpecification;
import com.prosysopc.ua.typedictionary.OptionSpecification;
import com.tridium.ndriver.discover.BINDiscoveryIcon;
import com.tridium.ndriver.discover.BINDiscoveryLeaf;
import com.tridium.ndriver.discover.BINDiscoveryObject;
import com.tridium.ndriver.util.SfUtil;
import com.tridium.opcUaClient.BOpcUaDevice;
import com.tridium.opcUaClient.alarm.BOpcUaClientAlarmDeviceExt;
import com.tridium.opcUaClient.util.OpcUaClientUtil;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Objects;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.baja.control.BBooleanPoint;
import javax.baja.control.BBooleanWritable;
import javax.baja.control.BEnumPoint;
import javax.baja.control.BEnumWritable;
import javax.baja.control.BNumericPoint;
import javax.baja.control.BNumericWritable;
import javax.baja.control.BStringPoint;
import javax.baja.control.BStringWritable;
import javax.baja.naming.SlotPath;
import javax.baja.nre.annotations.Facet;
import javax.baja.nre.annotations.NiagaraAction;
import javax.baja.nre.annotations.NiagaraActions;
import javax.baja.nre.annotations.NiagaraProperties;
import javax.baja.nre.annotations.NiagaraProperty;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.registry.TypeInfo;
import javax.baja.sys.Action;
import javax.baja.sys.BBoolean;
import javax.baja.sys.BComplex;
import javax.baja.sys.BComponent;
import javax.baja.sys.BDouble;
import javax.baja.sys.BEnum;
import javax.baja.sys.BEnumRange;
import javax.baja.sys.BFacets;
import javax.baja.sys.BIcon;
import javax.baja.sys.BNumber;
import javax.baja.sys.BString;
import javax.baja.sys.BValue;
import javax.baja.sys.Context;
import javax.baja.sys.Property;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;
import javax.baja.units.BUnit;
import javax.baja.util.BTypeSpec;

@NiagaraType
@NiagaraProperties({@NiagaraProperty(
      name = "UaNodeName",
      type = "String",
      defaultValue = "",
      flags = 5
   ), @NiagaraProperty(
      name = "UaDisplayName",
      type = "String",
      defaultValue = "",
      flags = 5,
      facets = {@Facet("SfUtil.incl()")}
   ), @NiagaraProperty(
      name = "structurePath",
      type = "String",
      defaultValue = "",
      flags = 5
   ), @NiagaraProperty(
      name = "isStructureComponent",
      type = "boolean",
      defaultValue = "false",
      flags = 5
   ), @NiagaraProperty(
      name = "isStructure",
      type = "boolean",
      defaultValue = "false",
      flags = 5
   ), @NiagaraProperty(
      name = "UaNodeId",
      type = "String",
      defaultValue = "",
      flags = 5,
      facets = {@Facet("SfUtil.incl()")}
   ), @NiagaraProperty(
      name = "NameSpaceUri",
      type = "String",
      defaultValue = "",
      flags = 5,
      facets = {@Facet("SfUtil.incl()")}
   ), @NiagaraProperty(
      name = "ArrayDimension",
      type = "String",
      defaultValue = "",
      flags = 5,
      facets = {@Facet("SfUtil.incl()")}
   ), @NiagaraProperty(
      name = "ArrayIndex",
      type = "String",
      defaultValue = "",
      flags = 5,
      facets = {@Facet("SfUtil.incl()")}
   ), @NiagaraProperty(
      name = "NodeClass",
      type = "String",
      defaultValue = "",
      flags = 5,
      facets = {@Facet("SfUtil.incl()")}
   ), @NiagaraProperty(
      name = "Description",
      type = "String",
      defaultValue = "",
      flags = 5,
      facets = {@Facet("SfUtil.incl()")}
   ), @NiagaraProperty(
      name = "TypeSpec",
      type = "String",
      defaultValue = "",
      flags = 5,
      facets = {@Facet("SfUtil.incl()")}
   ), @NiagaraProperty(
      name = "UaDataType",
      type = "String",
      defaultValue = "",
      flags = 5,
      facets = {@Facet("SfUtil.incl()")}
   ), @NiagaraProperty(
      name = "UaDataTypeIdentifier",
      type = "long",
      defaultValue = "-1",
      flags = 5,
      facets = {@Facet("SfUtil.incl()")}
   ), @NiagaraProperty(
      name = "UaValue",
      type = "String",
      defaultValue = "",
      flags = 5,
      facets = {@Facet("SfUtil.incl()")}
   ), @NiagaraProperty(
      name = "UaInstanceType",
      type = "String",
      defaultValue = "",
      flags = 5,
      facets = {@Facet("SfUtil.incl()")}
   ), @NiagaraProperty(
      name = "WriteMask",
      type = "long",
      defaultValue = "0",
      flags = 5,
      facets = {@Facet("SfUtil.incl()")}
   ), @NiagaraProperty(
      name = "UserWriteMask",
      type = "long",
      defaultValue = "0",
      flags = 5,
      facets = {@Facet("SfUtil.incl()")}
   ), @NiagaraProperty(
      name = "AccessLevel",
      type = "int",
      defaultValue = "0",
      flags = 5,
      facets = {@Facet("SfUtil.incl()")}
   ), @NiagaraProperty(
      name = "userAccessLevel",
      type = "int",
      defaultValue = "0",
      flags = 5,
      facets = {@Facet("SfUtil.incl()")}
   ), @NiagaraProperty(
      name = "EventNotifier",
      type = "int",
      defaultValue = "0",
      flags = 5,
      facets = {@Facet("SfUtil.incl()")}
   ), @NiagaraProperty(
      name = "HasCondition",
      type = "boolean",
      defaultValue = "false",
      flags = 5,
      facets = {@Facet("SfUtil.incl()")}
   ), @NiagaraProperty(
      name = "Historizing",
      type = "boolean",
      defaultValue = "false",
      flags = 5,
      facets = {@Facet("SfUtil.incl()")}
   ), @NiagaraProperty(
      name = "Facets",
      type = "BFacets",
      defaultValue = "BFacets.NULL",
      flags = 5,
      facets = {@Facet("SfUtil.incl()")}
   ), @NiagaraProperty(
      name = "IsProperty",
      type = "boolean",
      defaultValue = "false",
      flags = 5
   ), @NiagaraProperty(
      name = "IsEventType",
      type = "boolean",
      defaultValue = "false",
      flags = 5
   ), @NiagaraProperty(
      name = "HasAddableDescendant",
      type = "boolean",
      defaultValue = "false",
      flags = 5
   )})
@NiagaraActions({@NiagaraAction(
      name = "test",
      flags = 16
   ), @NiagaraAction(
      name = "showDetail",
      flags = 16
   ), @NiagaraAction(
      name = "hideDetail",
      flags = 16
   ), @NiagaraAction(
      name = "dumpHistory",
      flags = 16
   ), @NiagaraAction(
      name = "subscribeForEvents",
      flags = 16
   )})
public class BOpcUaNodeLearnEntry extends BComponent implements BINDiscoveryLeaf, BINDiscoveryIcon, BINDiscoveryObject {
   public static final Property UaNodeName = newProperty(5, "", null);
   public static final Property UaDisplayName = newProperty(5, "", SfUtil.incl());
   public static final Property structurePath = newProperty(5, "", null);
   public static final Property isStructureComponent = newProperty(5, false, null);
   public static final Property isStructure = newProperty(5, false, null);
   public static final Property UaNodeId = newProperty(5, "", SfUtil.incl());
   public static final Property NameSpaceUri = newProperty(5, "", SfUtil.incl());
   public static final Property ArrayDimension = newProperty(5, "", SfUtil.incl());
   public static final Property ArrayIndex = newProperty(5, "", SfUtil.incl());
   public static final Property NodeClass = newProperty(5, "", SfUtil.incl());
   public static final Property Description = newProperty(5, "", SfUtil.incl());
   public static final Property TypeSpec = newProperty(5, "", SfUtil.incl());
   public static final Property UaDataType = newProperty(5, "", SfUtil.incl());
   public static final Property UaDataTypeIdentifier = newProperty(5, -1, SfUtil.incl());
   public static final Property UaValue = newProperty(5, "", SfUtil.incl());
   public static final Property UaInstanceType = newProperty(5, "", SfUtil.incl());
   public static final Property WriteMask = newProperty(5, 0, SfUtil.incl());
   public static final Property UserWriteMask = newProperty(5, 0, SfUtil.incl());
   public static final Property AccessLevel = newProperty(5, 0, SfUtil.incl());
   public static final Property userAccessLevel = newProperty(5, 0, SfUtil.incl());
   public static final Property EventNotifier = newProperty(5, 0, SfUtil.incl());
   public static final Property HasCondition = newProperty(5, false, SfUtil.incl());
   public static final Property Historizing = newProperty(5, false, SfUtil.incl());
   public static final Property Facets = newProperty(5, BFacets.NULL, SfUtil.incl());
   public static final Property IsProperty = newProperty(5, false, null);
   public static final Property IsEventType = newProperty(5, false, null);
   public static final Property HasAddableDescendant = newProperty(5, false, null);
   public static final Action test = newAction(16, null);
   public static final Action showDetail = newAction(16, null);
   public static final Action hideDetail = newAction(16, null);
   public static final Action dumpHistory = newAction(16, null);
   public static final Action subscribeForEvents = newAction(16, null);
   public static final Type TYPE = Sys.loadType(BOpcUaNodeLearnEntry.class);
   private static int level = 0;
   public static final Logger logger = Logger.getLogger("opcUaClient.point");
   private static final BIcon NC_OBJECT_ICON = BIcon.std("folder.png");
   private static final BIcon NC_METHOD_ICON = BIcon.std("gears.png");
   private static final BIcon NUMERIC_ICON = BIcon.std("control/numericPoint.png");
   private static final BIcon BOOLEAN_ICON = BIcon.std("control/booleanPoint.png");
   private static final BIcon ENUM_ICON = BIcon.std("control/enumPoint.png");
   private static final BIcon STRING_ICON = BIcon.std("control/stringPoint.png");
   private static final BIcon HISTORY_ICON = BIcon.std("badges/history.png");
   private static final String lBracket = "\\[";
   private static final TypeInfo[] NO_TYPES = new TypeInfo[0];
   private static final TypeInfo[] RD_NUMERIC_TYPE = new TypeInfo[]{BNumericPoint.TYPE.getTypeInfo()};
   private static final TypeInfo[] RD_BOOLEAN_TYPE = new TypeInfo[]{BBooleanPoint.TYPE.getTypeInfo()};
   private static final TypeInfo[] RD_ENUM_TYPE = new TypeInfo[]{BEnumPoint.TYPE.getTypeInfo()};
   private static final TypeInfo[] RD_STRING_TYPE = new TypeInfo[]{BStringPoint.TYPE.getTypeInfo()};
   private static final TypeInfo[] RD_WR_NUMERIC_TYPE = new TypeInfo[]{BNumericWritable.TYPE.getTypeInfo(), BNumericPoint.TYPE.getTypeInfo()};
   private static final TypeInfo[] RD_WR_BOOLEAN_TYPE = new TypeInfo[]{BBooleanWritable.TYPE.getTypeInfo(), BBooleanPoint.TYPE.getTypeInfo()};
   private static final TypeInfo[] RD_WR_ENUM_TYPE = new TypeInfo[]{BEnumWritable.TYPE.getTypeInfo(), BEnumPoint.TYPE.getTypeInfo()};
   private static final TypeInfo[] RD_WR_STRING_TYPE = new TypeInfo[]{BStringWritable.TYPE.getTypeInfo(), BStringPoint.TYPE.getTypeInfo()};
   private int structureDepth = 0;
   private static final int MAX_STRUCTURE_DEPTH_SUPPORTED = 100;

   public String getUaNodeName() {
      return this.getString(UaNodeName);
   }

   public void setUaNodeName(String v) {
      this.setString(UaNodeName, v, null);
   }

   public String getUaDisplayName() {
      return this.getString(UaDisplayName);
   }

   public void setUaDisplayName(String v) {
      this.setString(UaDisplayName, v, null);
   }

   public String getStructurePath() {
      return this.getString(structurePath);
   }

   public void setStructurePath(String v) {
      this.setString(structurePath, v, null);
   }

   public boolean getIsStructureComponent() {
      return this.getBoolean(isStructureComponent);
   }

   public void setIsStructureComponent(boolean v) {
      this.setBoolean(isStructureComponent, v, null);
   }

   public boolean getIsStructure() {
      return this.getBoolean(isStructure);
   }

   public void setIsStructure(boolean v) {
      this.setBoolean(isStructure, v, null);
   }

   public String getUaNodeId() {
      return this.getString(UaNodeId);
   }

   public void setUaNodeId(String v) {
      this.setString(UaNodeId, v, null);
   }

   public String getNameSpaceUri() {
      return this.getString(NameSpaceUri);
   }

   public void setNameSpaceUri(String v) {
      this.setString(NameSpaceUri, v, null);
   }

   public String getArrayDimension() {
      return this.getString(ArrayDimension);
   }

   public void setArrayDimension(String v) {
      this.setString(ArrayDimension, v, null);
   }

   public String getArrayIndex() {
      return this.getString(ArrayIndex);
   }

   public void setArrayIndex(String v) {
      this.setString(ArrayIndex, v, null);
   }

   public String getNodeClass() {
      return this.getString(NodeClass);
   }

   public void setNodeClass(String v) {
      this.setString(NodeClass, v, null);
   }

   public String getDescription() {
      return this.getString(Description);
   }

   public void setDescription(String v) {
      this.setString(Description, v, null);
   }

   public String getTypeSpec() {
      return this.getString(TypeSpec);
   }

   public void setTypeSpec(String v) {
      this.setString(TypeSpec, v, null);
   }

   public String getUaDataType() {
      return this.getString(UaDataType);
   }

   public void setUaDataType(String v) {
      this.setString(UaDataType, v, null);
   }

   public long getUaDataTypeIdentifier() {
      return this.getLong(UaDataTypeIdentifier);
   }

   public void setUaDataTypeIdentifier(long v) {
      this.setLong(UaDataTypeIdentifier, v, null);
   }

   public String getUaValue() {
      return this.getString(UaValue);
   }

   public void setUaValue(String v) {
      this.setString(UaValue, v, null);
   }

   public String getUaInstanceType() {
      return this.getString(UaInstanceType);
   }

   public void setUaInstanceType(String v) {
      this.setString(UaInstanceType, v, null);
   }

   public long getWriteMask() {
      return this.getLong(WriteMask);
   }

   public void setWriteMask(long v) {
      this.setLong(WriteMask, v, null);
   }

   public long getUserWriteMask() {
      return this.getLong(UserWriteMask);
   }

   public void setUserWriteMask(long v) {
      this.setLong(UserWriteMask, v, null);
   }

   public int getAccessLevel() {
      return this.getInt(AccessLevel);
   }

   public void setAccessLevel(int v) {
      this.setInt(AccessLevel, v, null);
   }

   public int getUserAccessLevel() {
      return this.getInt(userAccessLevel);
   }

   public void setUserAccessLevel(int v) {
      this.setInt(userAccessLevel, v, null);
   }

   public int getEventNotifier() {
      return this.getInt(EventNotifier);
   }

   public void setEventNotifier(int v) {
      this.setInt(EventNotifier, v, null);
   }

   public boolean getHasCondition() {
      return this.getBoolean(HasCondition);
   }

   public void setHasCondition(boolean v) {
      this.setBoolean(HasCondition, v, null);
   }

   public boolean getHistorizing() {
      return this.getBoolean(Historizing);
   }

   public void setHistorizing(boolean v) {
      this.setBoolean(Historizing, v, null);
   }

   public BFacets getFacets() {
      return (BFacets)this.get(Facets);
   }

   public void setFacets(BFacets v) {
      this.set(Facets, v, null);
   }

   public boolean getIsProperty() {
      return this.getBoolean(IsProperty);
   }

   public void setIsProperty(boolean v) {
      this.setBoolean(IsProperty, v, null);
   }

   public boolean getIsEventType() {
      return this.getBoolean(IsEventType);
   }

   public void setIsEventType(boolean v) {
      this.setBoolean(IsEventType, v, null);
   }

   public boolean getHasAddableDescendant() {
      return this.getBoolean(HasAddableDescendant);
   }

   public void setHasAddableDescendant(boolean v) {
      this.setBoolean(HasAddableDescendant, v, null);
   }

   public void test() {
      this.invoke(test, null, null);
   }

   public void showDetail() {
      this.invoke(showDetail, null, null);
   }

   public void hideDetail() {
      this.invoke(hideDetail, null, null);
   }

   public void dumpHistory() {
      this.invoke(dumpHistory, null, null);
   }

   public void subscribeForEvents() {
      this.invoke(subscribeForEvents, null, null);
   }

   public Type getType() {
      return TYPE;
   }

   public BOpcUaNodeLearnEntry() {
   }

   public BOpcUaNodeLearnEntry(String nodeName, String nodeId, String nodeClass, String uaDataType, BTypeSpec typeSpec) {
      this.setUaNodeName(nodeName);
      this.setUaDisplayName(nodeName);
      this.setUaNodeId(nodeId);
      this.setNodeClass(nodeClass);
      this.setUaDataType(uaDataType);
      if (!typeSpec.isNull()) {
         this.setTypeSpec(typeSpec.toString());
      }
   }

   public static BOpcUaNodeLearnEntry make(AddressSpace addressSpace, ReferenceDescription reference, NodeId thisNodeId, UaClient client) {
      try {
         return AccessController.doPrivileged(
            (PrivilegedAction<BOpcUaNodeLearnEntry>)(() -> {
               BOpcUaNodeLearnEntry retEntry = new BOpcUaNodeLearnEntry();
               boolean isProperty = reference.getReferenceTypeId().equals(ReferenceTypeIdentifiers.HasProperty);
               UnsignedInteger[] nodeAttriBs = new UnsignedInteger[]{
                  Attributes.NodeClass,
                  Attributes.BrowseName,
                  Attributes.DisplayName,
                  Attributes.Value,
                  Attributes.DataType,
                  Attributes.Description,
                  Attributes.WriteMask,
                  Attributes.UserWriteMask,
                  Attributes.AccessLevel,
                  Attributes.UserAccessLevel,
                  Attributes.EventNotifier,
                  Attributes.Historizing
               };
               DataValue[] arrayOfNodes = null;

               try {
                  arrayOfNodes = client.readAttributes(thisNodeId, nodeAttriBs);
               } catch (ServiceException var28) {
                  logger.info(var28.getMessage());
                  return retEntry;
               }

               try {
                  String nodeName = OpcUaClientUtil.getNodeName(reference, arrayOfNodes);
                  String displayName = OpcUaClientUtil.getDisplayName(reference, arrayOfNodes, nodeName);
                  DataValue dataValue = Objects.requireNonNull(arrayOfNodes[3], "DataValue should not be null. Skipping node entry for nodeId: " + thisNodeId);
                  String dataType = "";
                  long dtIdentifier = -1L;

                  try {
                     Optional<DataValue> nodeOptional = OpcUaClientUtil.getNodeDataTypeNode(arrayOfNodes[4]);
                     if (nodeOptional.isPresent()) {
                        DataValue dtUaNode = nodeOptional.get();
                        if (dtUaNode != null && dtUaNode.getValue() != null && dtUaNode.getValue().getValue() != null) {
                           NodeId nodeId = (NodeId)dtUaNode.getValue().getValue();
                           dataType = addressSpace.getDataType(nodeId).getBrowseName().getName();
                           if (nodeId.getValue() != null) {
                              dtIdentifier = Long.parseLong(nodeId.getValue().toString());
                           }
                        }
                     }
                  } catch (Exception var29) {
                     if (logger.isLoggable(Level.FINE)) {
                        logger.log(Level.FINE, "Exception while fetching dataType and Identifier details for " + displayName, (Throwable)var29);
                     }
                  }

                  Object dataValueValue = OpcUaClientUtil.getDataValueValue(dataValue);
                  String description = OpcUaClientUtil.getNodeLocalizedText(arrayOfNodes[5]);
                  BTypeSpec typeSpec = OpcUaClientUtil.getTypeSpec(dataValue.getValue(), dataType);
                  if (!dataValue.getValue().isEmpty()) {
                     retEntry.setIsStructure(OpcUaClientUtil.isStructure(dataValue.getValue().getValue()));
                     retEntry.setIsStructureComponent(retEntry.getIsStructure());
                  }

                  DataValue wmdv = arrayOfNodes[6];
                  DataValue uwmdv = arrayOfNodes[7];
                  DataValue al = arrayOfNodes[8];
                  DataValue ual = arrayOfNodes[9];
                  DataValue eventNotifier = arrayOfNodes[10];
                  DataValue historizing = arrayOfNodes[11];
                  retEntry.setUaNodeName(nodeName);
                  retEntry.setUaDisplayName(displayName);
                  retEntry.setUaNodeId(thisNodeId.toString());
                  retEntry.setNameSpaceUri(addressSpace.getNamespaceTable().toExpandedNodeId(thisNodeId).getNamespaceUri());
                  retEntry.setNodeClass(
                     arrayOfNodes[0].getStatusCode().isNotBad()
                        ? com.prosysopc.ua.stack.core.NodeClass.valueOf(arrayOfNodes[0].getValue().toNumber().intValue()).toString()
                        : com.prosysopc.ua.stack.core.NodeClass.valueOf(reference.getNodeClass().getValue()).toString()
                  );
                  retEntry.setUaDataType(dataType);
                  retEntry.setUaDataTypeIdentifier(dtIdentifier);
                  retEntry.setTypeSpec(typeSpec.isNull() ? "" : typeSpec.toString());
                  retEntry.setDescription(description);
                  retEntry.setIsProperty(isProperty);
                  retEntry.setIsEventType(false);
                  if (!wmdv.isNull()) {
                     Object oValue = wmdv.getValue().getValue();
                     retEntry.setWriteMask(((UnsignedInteger)oValue).getValue());
                  }

                  if (!uwmdv.isNull()) {
                     Object oValue = uwmdv.getValue().getValue();
                     retEntry.setUserWriteMask(((UnsignedInteger)oValue).getValue());
                  }

                  Object oValue = al.isNull() ? UnsignedByte.valueOf(0) : al.getValue().getValue();
                  int accessLvl = ((UnsignedByte)oValue).getValue();
                  retEntry.setAccessLevel(accessLvl);
                  oValue = ual.isNull() ? UnsignedByte.valueOf(0) : ual.getValue().getValue();
                  retEntry.setUserAccessLevel(((UnsignedByte)oValue).getValue());
                  oValue = !historizing.isNull() && historizing.getValue().booleanValue();
                  String userBit = Integer.toBinaryString(accessLvl);
                  boolean hasHistory = userBit.length() > 2 && userBit.charAt(2) == '1' || userBit.length() > 3 && userBit.charAt(3) == '1' || (Boolean)oValue;
                  retEntry.setHistorizing(hasHistory);
                  oValue = eventNotifier.isNull() ? -1 : eventNotifier.getValue().intValue();
                  retEntry.setEventNotifier((Integer)oValue);
                  if (dataValueValue != null) {
                     retEntry.setUaValue(dataValueValue.toString());
                     retEntry.setUaInstanceType(dataValue.getValue().getCompositeClass().getSimpleName());
                  }

                  if (!dataValue.isNull() && dataValue.getValue().isArray()) {
                     retEntry.initArrayInfo(dataValue);
                  }

                  if (dataValue != null && !dataValue.getValue().isEmpty() && retEntry.getIsStructure()) {
                     retEntry.initStructure(dataValue.getValue().getValue(), addressSpace);
                  }
               } catch (Exception var30) {
                  logger.log(Level.INFO, "Exception while instantiating BOpcUaNodeLearnEntry: ", (Throwable)var30);
               }

               return retEntry;
            })
         );
      } catch (Exception var5) {
         if (logger.isLoggable(Level.FINE)) {
            logger.fine("Exception while instantiating BOpcUaNodeLearnEntry : " + var5.getMessage());
         }

         return null;
      }
   }

   public boolean hasCondition() {
      UaReference hasCondition = null;

      try {
         UaNode node = OpcUaClientUtil.getAddressSpaceNode(this.getDevice().uaClient.getAddressSpace(), NodeId.parseNodeId(this.getUaNodeId()));
         if (node != null) {
            hasCondition = node.getReference(ReferenceTypeIdentifiers.HasCondition, false);
         }
      } catch (Exception var3) {
         if (logger.isLoggable(Level.FINE)) {
            logger.log(Level.WARNING, "Unable to determine hasCondition", (Throwable)var3);
         } else {
            logger.log(Level.WARNING, "Unable to determine hasCondition: " + var3.getMessage());
         }
      }

      this.setHasCondition(hasCondition != null);
      return hasCondition != null;
   }

   public BOpcUaLearnBase toDiscoveryTree(boolean showAddableOnly) {
      BOpcUaLearnBase root = BOpcUaLearnBase.make(this);
      buildDiscoveryTree(this, root, showAddableOnly);
      return root;
   }

   private static void buildDiscoveryTree(BOpcUaNodeLearnEntry entry, BOpcUaLearnBase root, boolean showAddableOnly) {
      for (BOpcUaNodeLearnEntry pointEntry : (BOpcUaNodeLearnEntry[])entry.getChildren(BOpcUaNodeLearnEntry.class)) {
         if (!pointEntry.getIsProperty()
            && !pointEntry.getIsEventType()
            && ("Method".equals(pointEntry.getNodeClass()) || !pointEntry.getTypeSpec().isEmpty() || !showAddableOnly || pointEntry.getHasAddableDescendant())) {
            Property prop = root.add("le?", BOpcUaLearnBase.make(pointEntry));
            BOpcUaLearnBase addChild = (BOpcUaLearnBase)root.get(prop);
            level++;
            buildDiscoveryTree(pointEntry, addChild, showAddableOnly);
            level--;
         }
      }
   }

   public String getDefaultName() {
      return this.getUaNodeId();
   }

   public void doDumpHistory() {
      if (!this.getHistorizing()) {
         logger.info(this.getUaNodeName() + " is not Historizing");
      } else {
         BOpcUaDevice device = this.getDevice();
         UaClient client = device.uaClient;
         NodeId nodeId = NodeId.parseNodeId(this.getUaNodeId());

         try {
            DataValue[] result = client.historyReadRaw(
               nodeId, DateTime.MIN_VALUE, DateTime.currentTime(), UnsignedInteger.MAX_VALUE, true, null, TimestampsToReturn.Source
            );

            for (DataValue dataValue : result) {
               System.out.println(dataValue);
            }
         } catch (Exception var9) {
            if (logger.isLoggable(Level.FINE)) {
               logger.fine("Exception while dumping History: " + var9);
            }
         }
      }
   }

   public void doShowDetail() {
      for (Property property : this.getFrozenPropertiesArray()) {
         this.setFlags(property, this.getFlags(property) & -5);
      }
   }

   public void doHideDetail() {
      for (Property property : this.getFrozenPropertiesArray()) {
         this.setFlags(property, this.getFlags(property) | 4);
      }
   }

   public void added(Property property, Context context) {
      BValue bValue = this.get(property);
      if (bValue instanceof BOpcUaNodeLearnEntry) {
         if ("EnumStrings".equals(property.getName())) {
            this.setTypeSpec(BTypeSpec.make(BEnum.TYPE).toString(null));
         } else if ("EnumValues".equals(property.getName())) {
            this.setTypeSpec(BTypeSpec.make(BEnum.TYPE).toString(null));
         }
      }

      super.added(property, context);
   }

   public void initStructure(Object value, AddressSpace addressSpace) {
      try {
         AccessController.doPrivileged(
            (PrivilegedAction<Void>)(() -> {
               if (this.getStructureDepth() >= 100) {
                  logger.log(Level.WARNING, "Aborting traversal - Structure depth is greater than 100 levels for: " + this.getUaDisplayName());
                  return null;
               } else {
                  String branchOrRoot = this.getStructureDepth() > 0 ? " branch " : " root ";
                  if (value == null) {
                     throw new IllegalArgumentException("Parameter 'value' for initializing Structure" + branchOrRoot + "should not be null");
                  } else if (!OpcUaClientUtil.isStructure(value)) {
                     return null;
                  } else {
                     Structure structure = Objects.requireNonNull(
                        (Structure)value, "Structure" + branchOrRoot + "should not be null after cast from non-null parameter 'value'"
                     );
                     if (logger.isLoggable(Level.FINER)) {
                        logger.log(Level.FINER, "Adding structure: " + structure);
                     }

                     for (FieldSpecification fieldSpecification : structure.specification().getFields()) {
                        Object fieldValue = structure.get(fieldSpecification);
                        String fieldName = fieldSpecification.getName();
                        UaNodeId fieldDataTypeId = fieldSpecification.getDataTypeId();
                        EnumerationSpecification fieldEnumerationSpecification = addressSpace.getEncoderContext()
                           .getEnumerationSpecification(fieldSpecification.getDataTypeId());
                        if (logger.isLoggable(Level.FINER)) {
                           logger.log(Level.FINER, "Field: " + fieldName + " = " + fieldValue);
                        }

                        try {
                           Variant fieldVariant = new Variant(fieldValue);
                           NodeId dataTypeNodeId = this.getDataTypeNodeId(fieldEnumerationSpecification, fieldDataTypeId, addressSpace.getNamespaceTable());
                           if (dataTypeNodeId == null) {
                              logger.log(Level.SEVERE, "Failed to determine data type for structure field: " + fieldName);
                              return null;
                           }

                           String dataType = addressSpace.getDataType(dataTypeNodeId).getBrowseName().getName();
                           Class<?> builtInType = (Class<?>)BuiltinsMap.MAP.get("http://opcfoundation.org/UA/" + dataType);
                           BTypeSpec typeSpec = builtInType != null
                              ? OpcUaClientUtil.getTypeSpec(fieldValue, builtInType.getSimpleName())
                              : OpcUaClientUtil.getTypeSpec(fieldValue, dataType);
                           BOpcUaNodeLearnEntry nodeEntry = new BOpcUaNodeLearnEntry(fieldName, this.getUaNodeId(), this.getNodeClass(), dataType, typeSpec);
                           StringBuilder structurePathBuilder = new StringBuilder(this.getStructurePath());
                           structurePathBuilder.append(fieldName).append('>');
                           if (fieldValue != null) {
                              nodeEntry.setUaValue(fieldValue.toString());
                           }

                           nodeEntry.setStructureDepth(this.getStructureDepth() + 1);
                           nodeEntry.setStructurePath(structurePathBuilder.toString());
                           nodeEntry.setIsStructureComponent(this.getIsStructureComponent());
                           nodeEntry.setUaDisplayName(fieldName);
                           nodeEntry.setNameSpaceUri(this.getNameSpaceUri());
                           nodeEntry.setWriteMask(this.getWriteMask());
                           nodeEntry.setUserWriteMask(this.getUserWriteMask());
                           nodeEntry.setAccessLevel(this.getAccessLevel());
                           nodeEntry.setUserAccessLevel(this.getUserAccessLevel());
                           nodeEntry.setUaInstanceType(
                              fieldValue != null ? fieldVariant.getCompositeClass().getSimpleName() : fieldSpecification.getJavaClass().getSimpleName()
                           );
                           nodeEntry.setUaDataTypeIdentifier(OpcUaClientUtil.getDataTypeIdentifier(dataTypeNodeId));
                           this.add(SlotPath.escape(fieldName) + '?', nodeEntry, 2);
                           if (OpcUaClientUtil.isStructure(fieldValue)) {
                              nodeEntry.setIsStructure(true);
                              nodeEntry.initStructure(fieldValue, addressSpace);
                           }

                           if (fieldVariant.isArray()) {
                              nodeEntry.initArrayInfo(new DataValue(fieldVariant));
                           }

                           if (!typeSpec.isNull() && typeSpec.getTypeInfo().is(BEnum.TYPE) && fieldEnumerationSpecification != null) {
                              nodeEntry.setFacets(OpcUaClientUtil.makeEnumFacets(fieldEnumerationSpecification));
                           }
                        } catch (Exception var18) {
                           if (logger.isLoggable(Level.FINE)) {
                              logger.log(Level.SEVERE, "Failed to add structure field: " + fieldName, (Throwable)var18);
                           } else {
                              logger.log(Level.SEVERE, "Failed to add structure field: " + fieldName + ", error message: " + var18.getMessage());
                           }
                        }
                     }

                     return null;
                  }
               }
            })
         );
      } catch (Exception var4) {
         if (logger.isLoggable(Level.FINE)) {
            logger.log(Level.SEVERE, "Failed to initialize structure for: " + value, (Throwable)var4);
         } else {
            logger.log(Level.SEVERE, "Failed to initialize structure for: " + value + ", error message: " + var4.getMessage());
         }
      }
   }

   public void initArrayInfo(DataValue dataValue) {
      try {
         AccessController.doPrivileged((PrivilegedAction<Void>)(() -> {
            if (dataValue.isNull()) {
               return null;
            } else if (!dataValue.getValue().isArray()) {
               return null;
            } else {
               int[] arrayDimensions = dataValue.getValue().getArrayDimensions();
               int dimension = dataValue.getValue().getDimension();
               if (arrayDimensions != null) {
                  StringBuilder sb = new StringBuilder();
                  sb.append('[');

                  for (int i = 0; i < arrayDimensions.length; i++) {
                     sb.append(arrayDimensions[i]);
                     if (i + 1 < arrayDimensions.length) {
                        sb.append(',');
                     }
                  }

                  sb.append(']');
                  this.setArrayDimension(sb.toString());
               }

               return null;
            }
         }));
      } catch (Exception var3) {
         if (logger.isLoggable(Level.FINE)) {
            logger.fine("Exception while initiating ArrayInfo: " + var3);
         }
      }
   }

   public boolean isEventNotifier(EventNotifierType value) {
      int enValue = this.getEventNotifier();
      if (enValue <= 0) {
         return false;
      } else {
         byte byteValue = (byte)enValue;
         EventNotifierType type = EventNotifierType.of(UnsignedByte.getFromBits(byteValue));
         if (logger.isLoggable(Level.FINEST)) {
            logger.log(Level.FINEST, "Event Notifier Types: " + type.toSet());
         }

         return type == null ? false : type.containsAll(value);
      }
   }

   public boolean isEventSubscribable() {
      return this.isEventNotifier(EventNotifierType.of(new Options[]{Options.SubscribeToEvents}));
   }

   public boolean isArray() {
      return !this.getArrayDimension().isEmpty();
   }

   public long[] getArrayDimensions() {
      String s = this.getArrayDimension();
      int il = s.indexOf(91);
      int ir = s.indexOf(93);
      if (il >= 0 && ir > il) {
         s = s.substring(il + 1, ir);
      }

      String[] split = s.split(",");
      long[] dimensions = new long[split.length];

      for (int i = 0; i < split.length; i++) {
         try {
            dimensions[i] = Long.parseLong(split[i].trim());
         } catch (Exception var8) {
            throw new NumberFormatException(var8.getMessage());
         }
      }

      return dimensions;
   }

   public void initHasAddableDescendant() {
      boolean addable = this.isAddable();

      for (BComplex parent = this.getParent(); parent instanceof BOpcUaNodeLearnEntry; parent = parent.getParent()) {
         BOpcUaNodeLearnEntry parentEntry = (BOpcUaNodeLearnEntry)parent;
         if (parentEntry.getHasAddableDescendant()) {
            break;
         }

         if (addable) {
            parentEntry.setHasAddableDescendant(true);
         } else {
            addable = parentEntry.isAddable();
         }
      }
   }

   public boolean isAddable() {
      if (logger.isLoggable(Level.FINE)) {
         logger.log(
            Level.FINE,
            "OPC UA Client discovered point: "
               + this.getUaNodeName()
               + " - NodeClass:"
               + this.getNodeClass()
               + ", IsProperty:"
               + this.getIsProperty()
               + ", TypeSpec:"
               + this.getTypeSpec()
               + ", EventType:"
               + this.getIsEventType()
               + "."
         );
      }

      return "Variable".equals(this.getNodeClass()) && !this.getIsProperty() && !this.getTypeSpec().isEmpty() && !this.getIsEventType();
   }

   public void doSubscribeForEvents() {
      try {
         NodeId nodeId = NodeId.parseNodeId(this.getUaNodeId());
         BOpcUaDevice device = this.getDevice();
         if (device == null) {
            return;
         }

         BOpcUaClientAlarmDeviceExt eventListener = device.getAlarmExt();
         eventListener.addMonitorEvent(nodeId);
      } catch (Exception var4) {
         if (logger.isLoggable(Level.FINE)) {
            logger.log(Level.INFO, "Error for the action SubscribeForEvent", (Throwable)var4);
         } else {
            logger.log(Level.INFO, "Error for the action SubscribeForEvent: " + var4);
         }
      }
   }

   public void doTest() {
      try {
         NodeId nodeId = NodeId.parseNodeId(this.getUaNodeId());
         Object value1 = nodeId.getValue();
         BOpcUaDevice device = this.getDevice();
         if (device == null) {
            logger.finest("BOpcUaDevice not found");
         } else {
            logger.finest("nodeId.getValue().class = " + value1.getClass().getName());
            logger.finest("NodeId = " + nodeId);
            logger.finest("NodeId.getValue() = " + value1);
            UaClient uaClient = device.uaClient;
            AddressSpace addressSpace = uaClient.getAddressSpace();
            UaNode node = addressSpace.getNode(nodeId);
            NodeClass nodeClass = node.getNodeClass();
            logger.finest("NodeClass = " + nodeClass);
            logger.finest("node = " + node);
            NodeAttributes attributes = node.getAttributes();
            logger.finest("attributes = " + attributes);

            try {
               DataValue dataValue = node.readAttribute(Attributes.Value);
               if (dataValue != null) {
                  Variant variantValue = dataValue.getValue();
                  Object value = variantValue.getValue();
                  if (value instanceof EUInformation) {
                     EUInformation eui = (EUInformation)value;
                     logger.finest(eui.getDisplayName().toString() + ", UnitId = " + eui.getUnitId());
                  }

                  logger.finest("dataValue = " + variantValue.toString(true));
                  if (variantValue.getCompositeClass() != null) {
                     logger.finest("dataValue.type = " + variantValue.getCompositeClass().getSimpleName());
                  }
               } else {
                  logger.info("No data value");
               }
            } catch (Exception var19) {
               logger.info(var19.getMessage());
            }

            UaProperty[] properties = node.getProperties();
            if (properties != null) {
               for (UaProperty uaProperty : properties) {
                  String propName = uaProperty.getDisplayName().getText();
                  Variant valuex = uaProperty.getValue().getValue();
                  logger.info("(" + propName + "=" + valuex + ")");
               }
            } else {
               logger.info("No properties defined");
            }

            BFacets pointFacet = this.makePointFacets();
            this.setFacets(pointFacet);
            logger.info("pointFacets = " + pointFacet);
            logger.info("isArray = " + this.isArray());
            StringBuilder sb = new StringBuilder();

            for (long l : this.getArrayDimensions()) {
               sb.append('[');
               if (l >= 0L) {
                  sb.append(l);
               }

               sb.append(']');
            }

            logger.info("ArrayDimensions = " + sb.toString());
            int accessLevel = this.getAccessLevel();
            AccessLevelType accessLevelType = null;

            for (com.prosysopc.ua.stack.core.AccessLevelType.Options field : com.prosysopc.ua.stack.core.AccessLevelType.Options.values()) {
               if (field.getBitPosition() == accessLevel) {
                  accessLevelType = AccessLevelType.of(new com.prosysopc.ua.stack.core.AccessLevelType.Options[]{field});
                  break;
               }
            }

            if (accessLevelType != null) {
               logger.info("AccessLevel = " + accessLevelType.toString());
            } else {
               logger.info("AccessLevel = null");
            }

            AttributeWriteMask writeMasks = null;

            for (com.prosysopc.ua.stack.core.AttributeWriteMask.Options fieldx : com.prosysopc.ua.stack.core.AttributeWriteMask.Options.values()) {
               if (fieldx.getBitPosition() == (int)this.getWriteMask()) {
                  writeMasks = AttributeWriteMask.of(new com.prosysopc.ua.stack.core.AttributeWriteMask.Options[]{fieldx});
                  break;
               }
            }

            if (writeMasks != null) {
               logger.info("WriteMask = " + writeMasks);
            } else {
               logger.info("WriteMask = null");
            }

            logger.info("EventNotifier = " + this.getEventNotifier());
            logger.info("isEventNotifierSubscribeToEvents = " + this.isEventNotifier(EventNotifierType.of(new Options[]{Options.SubscribeToEvents})));
            logger.info("isEventNotifierHistoryRead  = " + this.isEventNotifier(EventNotifierType.of(new Options[]{Options.HistoryRead})));
            logger.info("isEventNotifierHistoryWrite = " + this.isEventNotifier(EventNotifierType.of(new Options[]{Options.HistoryWrite})));
         }
      } catch (Exception var20) {
         if (logger.isLoggable(Level.FINE)) {
            logger.fine("Exception while testing: " + var20);
         }
      }
   }

   BOpcUaDevice getDevice() {
      for (BComplex parent = this.getParent(); parent != null; parent = parent.getParent()) {
         if (parent instanceof BOpcUaDevice) {
            return (BOpcUaDevice)parent;
         }
      }

      return null;
   }

   public TypeInfo[] getValidDatabaseTypes() {
      AccessLevelType levels = null;

      for (com.prosysopc.ua.stack.core.AccessLevelType.Options field : com.prosysopc.ua.stack.core.AccessLevelType.Options.values()) {
         if (field.getBitPosition() == this.getAccessLevel()) {
            levels = AccessLevelType.of(new com.prosysopc.ua.stack.core.AccessLevelType.Options[]{field});
            break;
         }
      }

      boolean canWrite = false;
      if (levels != null) {
         canWrite = levels.contains(new OptionSpecification[]{com.prosysopc.ua.stack.core.AccessLevelType.Options.CurrentWrite});
      }

      String spec = this.getTypeSpec();
      if (spec.isEmpty()) {
         return NO_TYPES;
      } else {
         TypeInfo typeInfo = BTypeSpec.make(spec).getTypeInfo();
         if (typeInfo.is(BNumber.TYPE)) {
            return canWrite ? RD_WR_NUMERIC_TYPE : RD_NUMERIC_TYPE;
         } else if (typeInfo.is(BBoolean.TYPE)) {
            return canWrite ? RD_WR_BOOLEAN_TYPE : RD_BOOLEAN_TYPE;
         } else if (typeInfo.is(BEnum.TYPE)) {
            return canWrite ? RD_WR_ENUM_TYPE : RD_ENUM_TYPE;
         } else if (typeInfo.is(BString.TYPE)) {
            return canWrite ? RD_WR_STRING_TYPE : RD_STRING_TYPE;
         } else {
            return NO_TYPES;
         }
      }
   }

   public void updateTarget(BComponent target) {
   }

   public boolean isExisting(BComponent component) {
      return false;
   }

   public String getDiscoveryName() {
      return SlotPath.escape(this.getUaNodeName());
   }

   public boolean isNumeric() {
      String specType = this.getTypeSpec();
      if ("".equals(specType)) {
         return false;
      } else {
         BTypeSpec ts = BTypeSpec.make(specType);
         if (ts.isNull()) {
            return false;
         } else {
            TypeInfo typeInfo = ts.getTypeInfo();
            return typeInfo.is(BNumber.TYPE);
         }
      }
   }

   public boolean isBoolean() {
      String specType = this.getTypeSpec();
      if ("".equals(specType)) {
         return false;
      } else {
         BTypeSpec ts = BTypeSpec.make(specType);
         if (ts.isNull()) {
            return false;
         } else {
            TypeInfo typeInfo = ts.getTypeInfo();
            return typeInfo.is(BBoolean.TYPE);
         }
      }
   }

   public BIcon getDiscoveryIcon() {
      String specType = this.getTypeSpec();
      if ("".equals(specType)) {
         return "Method".equals(this.getNodeClass()) ? NC_METHOD_ICON : null;
      } else {
         BTypeSpec ts = BTypeSpec.make(specType);
         if (ts.isNull()) {
            return null;
         } else {
            TypeInfo typeInfo = ts.getTypeInfo();
            if (typeInfo.is(BNumber.TYPE)) {
               return this.getHistorizing() ? BIcon.make(NUMERIC_ICON, HISTORY_ICON) : NUMERIC_ICON;
            } else if (typeInfo.is(BBoolean.TYPE)) {
               return this.getHistorizing() ? BIcon.make(BOOLEAN_ICON, HISTORY_ICON) : BOOLEAN_ICON;
            } else if (typeInfo.is(BEnum.TYPE)) {
               return this.getHistorizing() ? BIcon.make(ENUM_ICON, HISTORY_ICON) : ENUM_ICON;
            } else if (typeInfo.is(BString.TYPE)) {
               return this.getHistorizing() ? BIcon.make(STRING_ICON, HISTORY_ICON) : STRING_ICON;
            } else {
               return null;
            }
         }
      }
   }

   public void defaultTargetUpdate(BComponent target) {
   }

   public void updateFacets() {
      this.setFacets(this.makePointFacets());
   }

   public BFacets makePointFacets() {
      BFacets rtnFacets = BFacets.DEFAULT;
      String typeSpec = this.getTypeSpec();
      if (!"".equals(typeSpec)) {
         BTypeSpec ts = BTypeSpec.make(typeSpec);
         if (!ts.isNull()) {
            TypeInfo typeInfo = ts.getTypeInfo();
            if (this.hasEnumStringProperty()) {
               rtnFacets = this.makeEnumStringsFacets();
            } else if (this.hasEnumValuesProperty()) {
               rtnFacets = this.makeEnumValuesFacets();
            } else if (this.hasTrueStateProperty()) {
               rtnFacets = this.makeBooleanFacets();
            } else if (typeInfo.is(BNumber.TYPE)) {
               rtnFacets = this.makeNumericFacets();
            } else if (typeInfo.is(BEnum.TYPE) && !this.getFacets().isNull()) {
               return this.getFacets();
            }
         }
      }

      return rtnFacets;
   }

   public boolean hasEnumStringProperty() {
      BValue enumStrings = this.get("EnumStrings");
      return enumStrings != null;
   }

   public boolean hasEnumValuesProperty() {
      BValue enumValues = this.get("EnumValues");
      return enumValues != null;
   }

   public boolean hasTrueStateProperty() {
      return this.get("TrueState") != null && this.get("FalseState") != null;
   }

   public BFacets makeEnumStringsFacets() {
      try {
         BOpcUaNodeLearnEntry enumStrings = (BOpcUaNodeLearnEntry)this.get("EnumStrings");
         String values = enumStrings.getUaValue();
         String[] split = values.split(",");
         BEnumRange enumRange = BEnumRange.make(split);
         return BFacets.makeEnum(enumRange);
      } catch (Exception var5) {
         return BFacets.makeEnum();
      }
   }

   public BFacets makeEnumValuesFacets() {
      try {
         BOpcUaNodeLearnEntry enumValues = (BOpcUaNodeLearnEntry)this.get("EnumValues");
         String values = enumValues.getUaValue();
         values = values.substring(values.indexOf(123) + 1, values.indexOf(125));
         String[] keyValues = values.split(",");
         int[] ordinals = new int[keyValues.length];
         String[] tags = new String[keyValues.length];

         for (int i = 0; i < keyValues.length; i++) {
            String[] pair = keyValues[i].trim().split("=");
            ordinals[i] = Integer.parseInt(pair[0].trim());
            tags[i] = pair[1].trim();
         }

         BEnumRange enumRange = BEnumRange.make(ordinals, tags);
         return BFacets.makeEnum(enumRange);
      } catch (Exception var8) {
         return BFacets.makeEnum();
      }
   }

   public BFacets makeBooleanFacets() {
      try {
         BOpcUaNodeLearnEntry trueState = (BOpcUaNodeLearnEntry)this.get("TrueState");
         String trueText = trueState.getUaValue();
         BOpcUaNodeLearnEntry falseState = (BOpcUaNodeLearnEntry)this.get("FalseState");
         String falseText = falseState.getUaValue();
         return BFacets.makeBoolean(trueText, falseText);
      } catch (Exception var5) {
         return BFacets.makeBoolean();
      }
   }

   public BFacets makeNumericFacets() {
      try {
         BFacets rtnFacets = this.makeNumericFacets(this.getUaInstanceType());
         BOpcUaNodeLearnEntry[] children = (BOpcUaNodeLearnEntry[])this.getChildren(BOpcUaNodeLearnEntry.class);
         if (children.length == 0) {
            return rtnFacets;
         } else {
            BDouble maxValue = (BDouble)rtnFacets.get("max", BDouble.POSITIVE_INFINITY);
            BDouble minValue = (BDouble)rtnFacets.get("min", BDouble.NEGATIVE_INFINITY);
            BUnit units = (BUnit)rtnFacets.get("units", BUnit.DEFAULT);
            int precision = rtnFacets.geti("precision", 2);

            for (BOpcUaNodeLearnEntry child : children) {
               if (child.getUaDataType().equals("Range")) {
                  String rangeValue = child.getUaValue();
                  String[] values = rangeValue.split(",");
                  if (values.length == 2) {
                     minValue = BDouble.make(values[0]);
                     maxValue = BDouble.make(values[1]);
                  }
               } else if (child.getUaDataType().equals("EUInformation")) {
                  String uaValue = child.getUaValue();
                  if (uaValue.startsWith("() ")) {
                     uaValue = uaValue.substring(3);

                     try {
                        units = BUnit.getUnit(uaValue);
                     } catch (Exception var13) {
                        logger.info(var13.getLocalizedMessage() + " for: " + this.getUaNodeName());
                     }
                  }
               }
            }

            return BFacets.makeNumeric(units, precision, minValue.getDouble(), maxValue.getDouble());
         }
      } catch (Exception var14) {
         return BFacets.makeNumeric();
      }
   }

   private BFacets makeNumericFacets(String uaInstanceType) {
      switch (uaInstanceType) {
         case "SByte":
         case "Byte":
            return BFacets.makeNumeric(BUnit.DEFAULT, 0, -128.0, 127.0);
         case "UByte":
         case "UnsignedByte":
            return BFacets.makeNumeric(BUnit.DEFAULT, 0, 0.0, 255.0);
         case "Int16":
         case "Short":
            return BFacets.makeNumeric(BUnit.DEFAULT, 0, -32768.0, 32767.0);
         case "UInt16":
         case "UnsignedShort":
            return BFacets.makeNumeric(BUnit.DEFAULT, 0, UnsignedShort.L_MIN_VALUE, UnsignedShort.L_MAX_VALUE);
         case "Int32":
         case "Integer":
            return BFacets.makeNumeric(BUnit.DEFAULT, 0, -2.1474836E9F, 2.147483647E9);
         case "UInt32":
         case "UInteger":
         case "UnsignedInteger":
            return BFacets.makeNumeric(BUnit.DEFAULT, 0, UnsignedInteger.MIN_VALUE.doubleValue(), UnsignedInteger.MAX_VALUE.doubleValue());
         case "Int64":
         case "Long":
            return BFacets.makeNumeric(BUnit.DEFAULT, 0, -9.223372E18F, 9.223372E18F);
         case "UInt64":
         case "ULong":
         case "UnsignedLong":
            return BFacets.makeNumeric(BUnit.DEFAULT, 0, UnsignedLong.MIN_VALUE.doubleValue(), UnsignedLong.MAX_VALUE.doubleValue());
         case "Float":
            return BFacets.makeNumeric(BUnit.DEFAULT, 2, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY);
         case "Double":
         default:
            return BFacets.makeNumeric(BUnit.DEFAULT, 2, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY);
      }
   }

   public NodeId getDataTypeNodeId(EnumerationSpecification specification, UaNodeId uaDataTypeId, NamespaceTable namespaceTable) {
      if (specification != null && specification.getTypeId() != null) {
         return specification.getTypeId().asNodeId(namespaceTable);
      } else {
         return uaDataTypeId != null ? uaDataTypeId.asNodeId(namespaceTable) : null;
      }
   }

   public String toString(Context cx) {
      return this.getUaValue();
   }

   public BIcon getIcon() {
      if (this.getNodeClass().equals("Object")) {
         return NC_OBJECT_ICON;
      } else if (this.getNodeClass().equals("Variable")) {
         return this.getDiscoveryIcon();
      } else {
         return this.getNodeClass().equals("Method") ? NC_METHOD_ICON : super.getIcon();
      }
   }

   private void setStructureDepth(int structureDepth) {
      this.structureDepth = structureDepth;
   }

   public int getStructureDepth() {
      return this.structureDepth;
   }
}
