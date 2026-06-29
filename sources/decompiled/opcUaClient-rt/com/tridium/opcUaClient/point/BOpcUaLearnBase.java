package com.tridium.opcUaClient.point;

import com.tridium.ndriver.discover.BINDiscoveryIcon;
import com.tridium.ndriver.discover.BINDiscoveryLeaf;
import com.tridium.ndriver.discover.BINDiscoveryObject;
import com.tridium.ndriver.util.SfUtil;
import com.tridium.opcUaClient.history.BImportHistoryExt;
import com.tridium.opcUaClient.history.BNumericImportHistoryExt;
import com.tridium.opcUaClient.util.MatrixUtil;
import com.tridium.opcUaCore.BUaAccessLevel;
import javax.baja.control.BBooleanPoint;
import javax.baja.control.BBooleanWritable;
import javax.baja.control.BControlPoint;
import javax.baja.control.BEnumPoint;
import javax.baja.control.BEnumWritable;
import javax.baja.control.BNumericPoint;
import javax.baja.control.BNumericWritable;
import javax.baja.control.BStringPoint;
import javax.baja.control.BStringWritable;
import javax.baja.control.ext.BAbstractProxyExt;
import javax.baja.control.trigger.BIntervalTriggerMode;
import javax.baja.history.BCapacity;
import javax.baja.naming.SlotPath;
import javax.baja.nre.annotations.Facet;
import javax.baja.nre.annotations.NiagaraProperties;
import javax.baja.nre.annotations.NiagaraProperty;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.registry.TypeInfo;
import javax.baja.sys.BBoolean;
import javax.baja.sys.BComplex;
import javax.baja.sys.BComponent;
import javax.baja.sys.BEnum;
import javax.baja.sys.BFacets;
import javax.baja.sys.BIcon;
import javax.baja.sys.BNumber;
import javax.baja.sys.BRelTime;
import javax.baja.sys.BString;
import javax.baja.sys.Property;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;
import javax.baja.util.BTypeSpec;
import javax.baja.util.Lexicon;

@NiagaraType
@NiagaraProperties({@NiagaraProperty(
      name = "uaNodeName",
      type = "String",
      defaultValue = "",
      flags = 5
   ), @NiagaraProperty(
      name = "uaDisplayName",
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
      name = "uaNodeId",
      type = "String",
      defaultValue = "",
      flags = 5,
      facets = {@Facet("SfUtil.incl()")}
   ), @NiagaraProperty(
      name = "nameSpaceUri",
      type = "String",
      defaultValue = "",
      flags = 5,
      facets = {@Facet("SfUtil.incl()")}
   ), @NiagaraProperty(
      name = "accessLevel",
      type = "BUaAccessLevel",
      defaultValue = "BUaAccessLevel.DEFAULT",
      flags = 5,
      facets = {@Facet("SfUtil.incl()")}
   ), @NiagaraProperty(
      name = "arrayDimension",
      type = "String",
      defaultValue = "",
      flags = 5,
      facets = {@Facet("SfUtil.incl()")}
   ), @NiagaraProperty(
      name = "arrayIndex",
      type = "String",
      defaultValue = "",
      flags = 5,
      facets = {@Facet("SfUtil.incl()")}
   ), @NiagaraProperty(
      name = "uaDataType",
      type = "String",
      defaultValue = "",
      flags = 5,
      facets = {@Facet("SfUtil.incl()")}
   ), @NiagaraProperty(
      name = "uaDataTypeIdentifier",
      type = "long",
      defaultValue = "-1",
      flags = 5,
      facets = {@Facet("SfUtil.incl()")}
   ), @NiagaraProperty(
      name = "historizing",
      type = "boolean",
      defaultValue = "false",
      flags = 5,
      facets = {@Facet("SfUtil.incl()")}
   ), @NiagaraProperty(
      name = "facets",
      type = "BFacets",
      defaultValue = "BFacets.NULL",
      flags = 5,
      facets = {@Facet("SfUtil.incl()")}
   ), @NiagaraProperty(
      name = "nodeClass",
      type = "String",
      defaultValue = "",
      flags = 5,
      facets = {@Facet("SfUtil.incl()")}
   ), @NiagaraProperty(
      name = "typeSpec",
      type = "String",
      defaultValue = "",
      flags = 5,
      facets = {@Facet("SfUtil.incl()")}
   )})
public class BOpcUaLearnBase extends BComponent implements BINDiscoveryLeaf, BINDiscoveryIcon, BINDiscoveryObject {
   public static final Property uaNodeName = newProperty(5, "", null);
   public static final Property uaDisplayName = newProperty(5, "", SfUtil.incl());
   public static final Property structurePath = newProperty(5, "", null);
   public static final Property isStructureComponent = newProperty(5, false, null);
   public static final Property isStructure = newProperty(5, false, null);
   public static final Property uaNodeId = newProperty(5, "", SfUtil.incl());
   public static final Property nameSpaceUri = newProperty(5, "", SfUtil.incl());
   public static final Property accessLevel = newProperty(5, BUaAccessLevel.DEFAULT, SfUtil.incl());
   public static final Property arrayDimension = newProperty(5, "", SfUtil.incl());
   public static final Property arrayIndex = newProperty(5, "", SfUtil.incl());
   public static final Property uaDataType = newProperty(5, "", SfUtil.incl());
   public static final Property uaDataTypeIdentifier = newProperty(5, -1, SfUtil.incl());
   public static final Property historizing = newProperty(5, false, SfUtil.incl());
   public static final Property facets = newProperty(5, BFacets.NULL, SfUtil.incl());
   public static final Property nodeClass = newProperty(5, "", SfUtil.incl());
   public static final Property typeSpec = newProperty(5, "", SfUtil.incl());
   public static final Type TYPE = Sys.loadType(BOpcUaLearnBase.class);
   public static final Lexicon lex = Lexicon.make(BOpcUaLearnBase.class);
   private static final int MAX_ARRAY_SIZE = 200;
   private static final BIcon NC_OBJECT_ICON = BIcon.std("folder.png");
   private static final BIcon NC_METHOD_ICON = BIcon.std("gears.png");
   private static final BIcon NUMERIC_ICON = BIcon.std("control/numericPoint.png");
   private static final BIcon BOOLEAN_ICON = BIcon.std("control/booleanPoint.png");
   private static final BIcon ENUM_ICON = BIcon.std("control/enumPoint.png");
   private static final BIcon STRING_ICON = BIcon.std("control/stringPoint.png");
   private static final BIcon HISTORY_ICON = BIcon.std("badges/history.png");
   private static final BIcon ARRAY_ICON = BIcon.std("layers.png");
   private static final TypeInfo[] NO_TYPES = new TypeInfo[0];
   private static final TypeInfo[] RD_NUMERIC_TYPE = new TypeInfo[]{BNumericPoint.TYPE.getTypeInfo()};
   private static final TypeInfo[] RD_BOOLEAN_TYPE = new TypeInfo[]{BBooleanPoint.TYPE.getTypeInfo()};
   private static final TypeInfo[] RD_ENUM_TYPE = new TypeInfo[]{BEnumPoint.TYPE.getTypeInfo()};
   private static final TypeInfo[] RD_STRING_TYPE = new TypeInfo[]{BStringPoint.TYPE.getTypeInfo()};
   private static final TypeInfo[] RD_WR_NUMERIC_TYPE = new TypeInfo[]{BNumericWritable.TYPE.getTypeInfo(), BNumericPoint.TYPE.getTypeInfo()};
   private static final TypeInfo[] RD_WR_BOOLEAN_TYPE = new TypeInfo[]{BBooleanWritable.TYPE.getTypeInfo(), BBooleanPoint.TYPE.getTypeInfo()};
   private static final TypeInfo[] RD_WR_ENUM_TYPE = new TypeInfo[]{BEnumWritable.TYPE.getTypeInfo(), BEnumPoint.TYPE.getTypeInfo()};
   private static final TypeInfo[] RD_WR_STRING_TYPE = new TypeInfo[]{BStringWritable.TYPE.getTypeInfo(), BStringPoint.TYPE.getTypeInfo()};
   private static final TypeInfo[] POINT_FOLDER_TYPE = new TypeInfo[]{BOpcUaObject.TYPE.getTypeInfo()};
   private static final TypeInfo[] METHOD_TYPE = new TypeInfo[]{BOpcUaMethod.TYPE.getTypeInfo()};

   public String getUaNodeName() {
      return this.getString(uaNodeName);
   }

   public void setUaNodeName(String v) {
      this.setString(uaNodeName, v, null);
   }

   public String getUaDisplayName() {
      return this.getString(uaDisplayName);
   }

   public void setUaDisplayName(String v) {
      this.setString(uaDisplayName, v, null);
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
      return this.getString(uaNodeId);
   }

   public void setUaNodeId(String v) {
      this.setString(uaNodeId, v, null);
   }

   public String getNameSpaceUri() {
      return this.getString(nameSpaceUri);
   }

   public void setNameSpaceUri(String v) {
      this.setString(nameSpaceUri, v, null);
   }

   public BUaAccessLevel getAccessLevel() {
      return (BUaAccessLevel)this.get(accessLevel);
   }

   public void setAccessLevel(BUaAccessLevel v) {
      this.set(accessLevel, v, null);
   }

   public String getArrayDimension() {
      return this.getString(arrayDimension);
   }

   public void setArrayDimension(String v) {
      this.setString(arrayDimension, v, null);
   }

   public String getArrayIndex() {
      return this.getString(arrayIndex);
   }

   public void setArrayIndex(String v) {
      this.setString(arrayIndex, v, null);
   }

   public String getUaDataType() {
      return this.getString(uaDataType);
   }

   public void setUaDataType(String v) {
      this.setString(uaDataType, v, null);
   }

   public long getUaDataTypeIdentifier() {
      return this.getLong(uaDataTypeIdentifier);
   }

   public void setUaDataTypeIdentifier(long v) {
      this.setLong(uaDataTypeIdentifier, v, null);
   }

   public boolean getHistorizing() {
      return this.getBoolean(historizing);
   }

   public void setHistorizing(boolean v) {
      this.setBoolean(historizing, v, null);
   }

   public BFacets getFacets() {
      return (BFacets)this.get(facets);
   }

   public void setFacets(BFacets v) {
      this.set(facets, v, null);
   }

   public String getNodeClass() {
      return this.getString(nodeClass);
   }

   public void setNodeClass(String v) {
      this.setString(nodeClass, v, null);
   }

   public String getTypeSpec() {
      return this.getString(typeSpec);
   }

   public void setTypeSpec(String v) {
      this.setString(typeSpec, v, null);
   }

   public Type getType() {
      return TYPE;
   }

   public BOpcUaLearnBase() {
   }

   public BOpcUaLearnBase(String uaName, String nodeId, String arrayDimensions, String index) {
      this.setUaNodeName(uaName);
      this.setUaNodeId(nodeId);
      this.setArrayDimension(arrayDimensions);
      this.setArrayIndex(index);
   }

   public static BOpcUaLearnBase make(BOpcUaNodeLearnEntry entry, long[] indexes) {
      BOpcUaLearnBase learnBase = new BOpcUaLearnBase();
      learnBase.setUaDisplayName(entry.getUaDisplayName());
      learnBase.setUaNodeName(entry.getUaNodeName());
      learnBase.setStructurePath(entry.getStructurePath());
      learnBase.setIsStructure(entry.getIsStructure());
      learnBase.setIsStructureComponent(entry.getIsStructureComponent());
      learnBase.setUaNodeId(entry.getUaNodeId());
      learnBase.setNameSpaceUri(entry.getNameSpaceUri());
      learnBase.setArrayDimension(entry.getArrayDimension());
      learnBase.setArrayIndex(entry.getArrayIndex());
      learnBase.setHistorizing(entry.getHistorizing());
      learnBase.setUaDataType(entry.getUaDataType());
      learnBase.setUaDataTypeIdentifier(entry.getUaDataTypeIdentifier());
      learnBase.setTypeSpec(entry.getTypeSpec());
      learnBase.setNodeClass(entry.getNodeClass());
      learnBase.setFacets(entry.getFacets());
      learnBase.setAccessLevel(BUaAccessLevel.make(entry.getAccessLevel()));
      if (indexes.length > 0) {
         learnBase.setArrayIndex(MatrixUtil.indexesToString(indexes));
         learnBase.setUaNodeName(MatrixUtil.makeAddName(entry.getUaNodeName(), indexes));
         learnBase.setUaDisplayName(learnBase.getUaNodeName());
      }

      return learnBase;
   }

   public static BOpcUaLearnBase make(BOpcUaNodeLearnEntry entry) {
      if (!entry.isArray()) {
         return make(entry, new long[0]);
      } else {
         BOpcUaLearnBase learnBase = new BOpcUaLearnBase();
         learnBase.setUaDisplayName(entry.getUaDisplayName());
         learnBase.setUaNodeName(entry.getUaNodeName());
         learnBase.setStructurePath(entry.getStructurePath());
         learnBase.setIsStructure(entry.getIsStructure());
         learnBase.setIsStructureComponent(entry.getIsStructureComponent());
         learnBase.setUaNodeId(entry.getUaNodeId());
         learnBase.setNameSpaceUri(entry.getNameSpaceUri());
         learnBase.setArrayDimension(entry.getArrayDimension());
         learnBase.setNodeClass(entry.getNodeClass());
         long[] arrayDims = entry.getArrayDimensions();
         long arraySize = 1L;

         for (int i = 0; i < arrayDims.length; i++) {
            arraySize *= arrayDims[i];
         }

         long[] indexes = new long[arrayDims.length];
         boolean done = false;
         int maxArraySize = 200;

         for (int size = 0; !done && size < maxArraySize; done = MatrixUtil.indexToNext(indexes, arrayDims)) {
            String addName = MatrixUtil.makeAddName(entry.getUaNodeName(), indexes);
            learnBase.add(SlotPath.escape(addName), make(entry, indexes));
            if (++size == 200) {
               BOpcUaNodeLearnEntry.logger.info(lex.getText("clientPointLearn.maxArrayLearn", new Object[]{addName, 200}));
            }
         }

         return learnBase;
      }
   }

   public BOpcUaLearnBase[] getArray() {
      return this.isArray() ? (BOpcUaLearnBase[])this.getChildren(BOpcUaLearnBase.class) : new BOpcUaLearnBase[0];
   }

   public BControlPoint makePoint() {
      if (this.isAddable()) {
         TypeInfo[] validDatabaseTypes = this.getValidDatabaseTypes();
         if (validDatabaseTypes.length > 0) {
            BComplex instance = validDatabaseTypes[0].getInstance().asComplex();
            BOpcUaClientProxyExt proxyExt = new BOpcUaClientProxyExt();
            if (instance instanceof BOpcUaMethod) {
               proxyExt = new BOpcUaMethodProxyExt();
               BComplex parent = this.getParent();
               if (parent instanceof BOpcUaLearnBase) {
                  String parentNodeId = ((BOpcUaLearnBase)parent).getUaNodeId();
                  ((BOpcUaMethodProxyExt)proxyExt).setObjectId(parentNodeId);
               }
            }

            if (instance instanceof BControlPoint) {
               BControlPoint point = (BControlPoint)instance;
               point.setProxyExt(proxyExt);
               this.updateTarget(point);
               return point;
            }
         }
      }

      return null;
   }

   public BOpcUaClientPointFolder makePointFolder() {
      return !"Object".equals(this.getNodeClass()) && !this.getIsStructure() ? null : new BOpcUaClientPointFolder();
   }

   public boolean isAddable() {
      return true;
   }

   public boolean getHasAddableDescendant() {
      return false;
   }

   public boolean isArray() {
      return !this.getArrayDimension().isEmpty() && this.getArrayIndex().isEmpty();
   }

   public String getDiscoveryName() {
      return this.getUaDisplayName();
   }

   public TypeInfo[] getValidDatabaseTypes() {
      boolean canWrite = (this.getAccessLevel().getBits() & 2) != 0;
      if ("Object".equals(this.getNodeClass()) || this.getIsStructure()) {
         return (TypeInfo[])POINT_FOLDER_TYPE.clone();
      } else if ("Method".equals(this.getNodeClass())) {
         return (TypeInfo[])METHOD_TYPE.clone();
      } else {
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
   }

   public void updateTarget(BComponent target) {
      if (target instanceof BOpcUaObject) {
         ((BOpcUaObject)target).setUaNodeId(this.getUaNodeId());
      } else if (target instanceof BControlPoint) {
         BAbstractProxyExt proxyExt = ((BControlPoint)target).getProxyExt();
         if (proxyExt instanceof BOpcUaClientProxyExt) {
            BOpcUaClientProxyExt opcProxyExt = (BOpcUaClientProxyExt)proxyExt;
            ((BControlPoint)target).setFacets(this.getFacets());
            opcProxyExt.setUaDisplayName(this.getUaDisplayName());
            opcProxyExt.setStructurePath(this.getStructurePath());
            opcProxyExt.setIsStructureComponent(this.getIsStructureComponent());
            opcProxyExt.setDeviceFacets(this.getFacets());
            opcProxyExt.setUaNodeId(this.getUaNodeId());
            opcProxyExt.setNameSpaceUri(this.getNameSpaceUri());
            opcProxyExt.setArrayDimensions(this.getArrayDimension());
            opcProxyExt.setArrayIndex(this.getArrayIndex());
            opcProxyExt.setUaDataType(this.getUaDataType());
            opcProxyExt.setUaDataTypeIdentifier(this.getUaDataTypeIdentifier());
         }

         if (proxyExt instanceof BOpcUaMethodProxyExt) {
            BComplex parent = this.getParent();
            if (parent instanceof BOpcUaLearnBase) {
               String uaNodeId = ((BOpcUaLearnBase)parent).getUaNodeId();
               ((BOpcUaMethodProxyExt)proxyExt).setObjectId(uaNodeId);
            }
         }

         if (this.getHistorizing()) {
            BImportHistoryExt[] exts = (BImportHistoryExt[])target.getChildren(BImportHistoryExt.class);
            if (exts == null || exts.length == 0) {
               if (this.isNumeric()) {
                  BNumericImportHistoryExt historyExt = new BNumericImportHistoryExt();
                  this.initHistories(historyExt);
                  target.add("hist", historyExt);
               } else if (this.isBoolean() || this.isEnum() || this.isString()) {
                  BImportHistoryExt historyExt = new BImportHistoryExt();
                  this.initHistories(historyExt);
                  target.add("hist", historyExt);
               }
            }
         }
      }
   }

   private void initHistories(BImportHistoryExt histExt) {
      histExt.getHistoryConfig().setCapacity(BCapacity.makeByRecordCount(500));
      histExt.getExecutionTime().setTriggerMode(BIntervalTriggerMode.make(BRelTime.makeMinutes(10)));
      histExt.setEnabled(true);
   }

   public boolean isNumeric() {
      String typeSpec = this.getTypeSpec();
      if (typeSpec.equals("")) {
         return false;
      } else {
         BTypeSpec ts = BTypeSpec.make(typeSpec);
         if (ts.isNull()) {
            return false;
         } else {
            TypeInfo typeInfo = ts.getTypeInfo();
            return typeInfo.is(BNumber.TYPE);
         }
      }
   }

   public boolean isBoolean() {
      String typeSpec = this.getTypeSpec();
      if (typeSpec.equals("")) {
         return false;
      } else {
         BTypeSpec ts = BTypeSpec.make(typeSpec);
         if (ts.isNull()) {
            return false;
         } else {
            TypeInfo typeInfo = ts.getTypeInfo();
            return typeInfo.is(BBoolean.TYPE);
         }
      }
   }

   public boolean isEnum() {
      String typeSpec = this.getTypeSpec();
      if (typeSpec.equals("")) {
         return false;
      } else {
         BTypeSpec ts = BTypeSpec.make(typeSpec);
         if (ts.isNull()) {
            return false;
         } else {
            TypeInfo typeInfo = ts.getTypeInfo();
            return typeInfo.is(BEnum.TYPE);
         }
      }
   }

   public boolean isString() {
      String typeSpec = this.getTypeSpec();
      if (typeSpec.equals("")) {
         return false;
      } else {
         BTypeSpec ts = BTypeSpec.make(typeSpec);
         if (ts.isNull()) {
            return false;
         } else {
            TypeInfo typeInfo = ts.getTypeInfo();
            return typeInfo.is(BString.TYPE);
         }
      }
   }

   public boolean isExisting(BComponent component) {
      if (component instanceof BOpcUaClientPointFolder) {
         String s = component.getName();
         return s != null && s.equals(this.getUaDisplayName());
      } else if (!(component instanceof BControlPoint)) {
         return false;
      } else {
         BAbstractProxyExt proxyExt = ((BControlPoint)component).getProxyExt();
         if (!(proxyExt instanceof BOpcUaClientProxyExt)) {
            return false;
         } else {
            BOpcUaClientProxyExt opcProxyExt = (BOpcUaClientProxyExt)proxyExt;
            return opcProxyExt.getUaNodeId().equals(this.getUaNodeId()) && opcProxyExt.getArrayDimensions().equals(this.getArrayDimension())
               ? this.getArrayDimension().isEmpty() || opcProxyExt.getArrayIndex().equals(this.getArrayIndex())
               : false;
         }
      }
   }

   public void defaultTargetUpdate(BComponent target) {
      BComplex parent = this.getParent();
      if (parent instanceof BOpcUaNodeLearnEntry) {
         ((BOpcUaNodeLearnEntry)parent).defaultTargetUpdate(target);
      }
   }

   public BIcon getDiscoveryIcon() {
      String typeSpec = this.getTypeSpec();
      if (typeSpec.equals("")) {
         if (this.getNodeClass().equals("Method")) {
            return NC_METHOD_ICON;
         } else if (this.getNodeClass().equals("Object")) {
            return NC_OBJECT_ICON;
         } else {
            return this.isArray() ? ARRAY_ICON : null;
         }
      } else {
         BTypeSpec ts = BTypeSpec.make(typeSpec);
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

   public BIcon getIcon() {
      if (this.getNodeClass().equals("Object")) {
         return NC_OBJECT_ICON;
      } else if (this.getNodeClass().equals("Variable")) {
         return this.getDiscoveryIcon();
      } else {
         return this.getNodeClass().equals("Method") ? NC_METHOD_ICON : super.getIcon();
      }
   }
}
