package com.tridium.opcUaServer;

import com.prosysopc.ua.StatusException;
import com.prosysopc.ua.nodes.UaNode;
import com.prosysopc.ua.nodes.UaObject;
import com.prosysopc.ua.nodes.UaReference;
import com.prosysopc.ua.server.EventManagerListener;
import com.prosysopc.ua.server.HistoryManagerListener;
import com.prosysopc.ua.server.NodeManagerListener;
import com.prosysopc.ua.server.NodeManagerTable;
import com.prosysopc.ua.server.NodeManagerUaNode;
import com.prosysopc.ua.server.UaServer;
import com.prosysopc.ua.server.instantiation.NodeBuilderConfiguration;
import com.prosysopc.ua.server.io.IoManagerListener;
import com.prosysopc.ua.server.nodes.CacheVariable;
import com.prosysopc.ua.server.nodes.UaVariableNode;
import com.prosysopc.ua.stack.builtintypes.DataValue;
import com.prosysopc.ua.stack.builtintypes.ExpandedNodeId;
import com.prosysopc.ua.stack.builtintypes.LocalizedText;
import com.prosysopc.ua.stack.builtintypes.NodeId;
import com.prosysopc.ua.stack.builtintypes.StatusCode;
import com.prosysopc.ua.stack.builtintypes.Variant;
import com.prosysopc.ua.stack.core.AccessLevelType;
import com.prosysopc.ua.stack.core.DataTypeIdentifiers;
import com.prosysopc.ua.stack.core.EUInformation;
import com.prosysopc.ua.stack.core.Range;
import com.prosysopc.ua.stack.core.ReferenceTypeIdentifiers;
import com.prosysopc.ua.stack.core.VariableIdentifiers;
import com.prosysopc.ua.stack.core.AccessLevelType.Options;
import com.prosysopc.ua.types.opcua.AnalogItemType;
import com.prosysopc.ua.types.opcua.MultiStateDiscreteType;
import com.prosysopc.ua.types.opcua.OffNormalAlarmType;
import com.prosysopc.ua.types.opcua.TwoStateDiscreteType;
import com.prosysopc.ua.types.opcua.server.AlarmConditionTypeNode;
import com.prosysopc.ua.types.opcua.server.DiscreteAlarmTypeNode;
import com.prosysopc.ua.types.opcua.server.FolderTypeNode;
import com.prosysopc.ua.types.opcua.server.LimitAlarmTypeNode;
import com.tridium.ndriver.BNDevice;
import com.tridium.ndriver.util.SfUtil;
import com.tridium.opcUaServer.event.BOpcUaServerAlarmDeviceExt;
import com.tridium.opcUaServer.export.BIOpcExport;
import com.tridium.opcUaServer.history.OpcUaHistorian;
import com.tridium.opcUaServer.node.OpcUaIoManagerListener;
import com.tridium.opcUaServer.node.OpcUaNodeManagerListener;
import com.tridium.opcUaServer.point.BIOpcUaServerPointFolder;
import com.tridium.opcUaServer.point.BOpcUaServerPointDeviceExt;
import com.tridium.opcUaServer.point.BOpcUaServerPointFolder;
import com.tridium.opcUaServer.point.BOpcUaServerProxyExt;
import com.tridium.opcUaServer.util.OpcUaServerUtil;
import com.tridium.util.CompUtil;
import java.security.AccessController;
import java.security.PrivilegedExceptionAction;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.baja.alarm.ext.BAlarmSourceExt;
import javax.baja.control.BBooleanPoint;
import javax.baja.control.BControlPoint;
import javax.baja.control.BEnumPoint;
import javax.baja.control.BNumericPoint;
import javax.baja.control.BStringPoint;
import javax.baja.driver.point.BIPointFolder;
import javax.baja.history.ext.BHistoryExt;
import javax.baja.nre.annotations.Facet;
import javax.baja.nre.annotations.NiagaraProperties;
import javax.baja.nre.annotations.NiagaraProperty;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.status.BStatus;
import javax.baja.sys.BComponent;
import javax.baja.sys.BEnumRange;
import javax.baja.sys.BFacets;
import javax.baja.sys.BFrozenEnum;
import javax.baja.sys.BObject;
import javax.baja.sys.Context;
import javax.baja.sys.Property;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;
import javax.baja.units.BUnit;
import javax.baja.util.Lexicon;

@NiagaraType
@NiagaraProperties({@NiagaraProperty(
      name = "status",
      type = "BStatus",
      defaultValue = "BStatus.ok",
      flags = 75,
      facets = {@Facet("SfUtil.incl(SfUtil.MGR_EDIT_READONLY)")},
      override = true
   ), @NiagaraProperty(
      name = "namespaceUrl",
      type = "String",
      defaultValue = "http://www.tridium.com/OPCUA/",
      flags = 1
   ), @NiagaraProperty(
      name = "namespaceIndex",
      type = "int",
      defaultValue = "-1",
      flags = 67
   ), @NiagaraProperty(
      name = "namespaceFolderName",
      type = "String",
      defaultValue = "",
      flags = 64
   ), @NiagaraProperty(
      name = "pointsFolderName",
      type = "String",
      defaultValue = "",
      flags = 64
   ), @NiagaraProperty(
      name = "points",
      type = "BOpcUaServerPointDeviceExt",
      defaultValue = "new BOpcUaServerPointDeviceExt()"
   ), @NiagaraProperty(
      name = "AlarmExt",
      type = "BOpcUaServerAlarmDeviceExt",
      defaultValue = "new BOpcUaServerAlarmDeviceExt()",
      flags = 1
   )})
public class BOpcUaNamespace extends BNDevice {
   public static final Property status = newProperty(75, BStatus.ok, SfUtil.incl("ed.ro"));
   public static final Property namespaceUrl = newProperty(1, "http://www.tridium.com/OPCUA/", null);
   public static final Property namespaceIndex = newProperty(67, -1, null);
   public static final Property namespaceFolderName = newProperty(64, "", null);
   public static final Property pointsFolderName = newProperty(64, "", null);
   public static final Property points = newProperty(0, new BOpcUaServerPointDeviceExt(), null);
   public static final Property AlarmExt = newProperty(1, new BOpcUaServerAlarmDeviceExt(), null);
   public static final Type TYPE = Sys.loadType(BOpcUaNamespace.class);
   private final Map<String, BControlPoint> controlPointsMap = new HashMap<>();
   private static final Lexicon lex = Lexicon.make(BOpcUaServer.class);
   private static final AccessLevelType AL_EXPORT = AccessLevelType.of(new Options[]{Options.CurrentRead});
   private static final AccessLevelType AL_EXPORT_HISTORY = AccessLevelType.of(new Options[]{Options.CurrentRead, Options.HistoryRead});
   private static final AccessLevelType AL_IMPORT = AccessLevelType.of(new Options[]{Options.CurrentRead, Options.CurrentWrite});
   private static final AccessLevelType AL_IMPORT_HISTORY = AccessLevelType.of(new Options[]{Options.CurrentRead, Options.CurrentWrite, Options.HistoryRead});
   private static final String NAMESPACE_URL = "http://www.tridium.com/OPCUA/";
   private final NodeManagerListener myNodeManagerListener = new OpcUaNodeManagerListener();
   private final OpcUaHistorian myHistorian = new OpcUaHistorian();
   private UaServer server;
   private NodeManagerUaNode uaNodeManager;
   private OpcUaIoManagerListener ioManagerListener;
   private FolderTypeNode nsRootFolder;
   private FolderTypeNode myPointsFolder;
   private final Logger logger = Logger.getLogger("opcUaServer.namespace");

   public String getNamespaceUrl() {
      return this.getString(namespaceUrl);
   }

   public void setNamespaceUrl(String v) {
      this.setString(namespaceUrl, v, null);
   }

   public int getNamespaceIndex() {
      return this.getInt(namespaceIndex);
   }

   public void setNamespaceIndex(int v) {
      this.setInt(namespaceIndex, v, null);
   }

   public String getNamespaceFolderName() {
      return this.getString(namespaceFolderName);
   }

   public void setNamespaceFolderName(String v) {
      this.setString(namespaceFolderName, v, null);
   }

   public String getPointsFolderName() {
      return this.getString(pointsFolderName);
   }

   public void setPointsFolderName(String v) {
      this.setString(pointsFolderName, v, null);
   }

   public BOpcUaServerPointDeviceExt getPoints() {
      return (BOpcUaServerPointDeviceExt)this.get(points);
   }

   public void setPoints(BOpcUaServerPointDeviceExt v) {
      this.set(points, v, null);
   }

   public BOpcUaServerAlarmDeviceExt getAlarmExt() {
      return (BOpcUaServerAlarmDeviceExt)this.get(AlarmExt);
   }

   public void setAlarmExt(BOpcUaServerAlarmDeviceExt v) {
      this.set(AlarmExt, v, null);
   }

   public Type getType() {
      return TYPE;
   }

   public Type getNetworkType() {
      return BOpcUaServer.TYPE;
   }

   public void changed(Property p, Context cx) {
      if (this.isRunning() && !Context.decoding.equals(cx)) {
         if (p.equals(status)) {
            if (!this.getStatus().isDisabled()) {
               try {
                  this.startNodeSpace(true);
               } catch (Exception var4) {
                  this.logger.log(Level.SEVERE, "Exception occurred while starting the Namespace", (Throwable)var4);
               }
            } else {
               this.stopNodeSpace();
            }
         }

         if (p.equals(namespaceUrl)) {
            this.getOpcUaServer().doRestart();
         }
      } else {
         super.changed(p, cx);
      }
   }

   public OpcUaIoManagerListener getIoManagerListener() {
      return this.ioManagerListener;
   }

   public void started() throws Exception {
      super.started();
      this.startNodeSpace(false);
      this.getAlarmSourceInfo().setAlarmClass(this.getOpcUaServer().getAlarmSourceInfo().getAlarmClass());
   }

   public void stopped() throws Exception {
      this.stopNodeSpace();
      super.stopped();
   }

   public void stopNodeSpace() {
      this.server = this.getOpcUaServer().server;
      if (this.server != null) {
         if (this.uaNodeManager != null) {
            int index = this.uaNodeManager.getNamespaceIndex();
            NodeManagerTable nodeManager = this.server.getAddressSpace();
            nodeManager.removeNodeManager(index);
         }
      }
   }

   public void startNodeSpace(boolean isRestart) throws Exception {
      if (this.isRunning()) {
         this.server = this.getOpcUaServer().server;
         if (!this.getStatus().isDisabled() && this.server != null) {
            AccessController.doPrivileged((PrivilegedExceptionAction<Void>)(() -> {
               String namespaceUrl;
               if (!isRestart && "http://www.tridium.com/OPCUA/".equals(this.getNamespaceUrl())) {
                  namespaceUrl = this.getNamespaceUrl() + this.getName();
               } else {
                  namespaceUrl = this.getNamespaceUrl();
               }

               this.setNamespaceUrl(namespaceUrl);
               this.uaNodeManager = new NodeManagerUaNode(this.server, namespaceUrl);
               int ns = this.uaNodeManager.getNamespaceIndex();
               if (this.logger.isLoggable(Level.FINE)) {
                  this.logger.fine("NodeManager created for namespace " + namespaceUrl + " at index " + ns);
               }

               this.setNamespaceIndex(ns);
               this.uaNodeManager.addListener(this.myNodeManagerListener);
               this.ioManagerListener = new OpcUaIoManagerListener();
               this.ioManagerListener.setOpcUaNamespace(this);
               this.initializeControlPointsMap();
               this.uaNodeManager.getIoManager().addListeners(new IoManagerListener[]{this.ioManagerListener});
               this.myHistorian.init();
               this.uaNodeManager.getHistoryManager().setListener(this.myHistorian);
               this.getAlarmExt().init();
               this.uaNodeManager.getEventManager().setListener(this.getAlarmExt());
               UaObject objectsFolder = this.server.getNodeManagerRoot().getObjectsFolder();
               String namespaceFolderName = this.getNamespaceFolderName();
               String namespaceName = namespaceFolderName.isEmpty() ? this.getName() : namespaceFolderName;
               NodeId myObjectsFolderId = new NodeId(ns, namespaceName);
               this.nsRootFolder = (FolderTypeNode)this.uaNodeManager.createInstance(FolderTypeNode.class, namespaceName, myObjectsFolderId);
               this.uaNodeManager.addNodeAndReference(objectsFolder, this.nsRootFolder, ReferenceTypeIdentifiers.Organizes);
               namespaceName = this.getPointsFolderName().isEmpty() ? this.getPoints().getName() : this.getPointsFolderName();
               NodeId myPointsFolderId = new NodeId(ns, namespaceName);
               this.myPointsFolder = (FolderTypeNode)this.uaNodeManager.createInstance(FolderTypeNode.class, namespaceName, myPointsFolderId);
               this.uaNodeManager.addNodeAndReference(this.nsRootFolder, this.myPointsFolder, ReferenceTypeIdentifiers.Organizes);
               this.getPoints().setUaNodeId(myPointsFolderId.toString());
               if (Sys.atSteadyState()) {
                  BOpcUaServerPointFolder[] pointFolders = (BOpcUaServerPointFolder[])CompUtil.getDescendants(this.getPoints(), BOpcUaServerPointFolder.class);

                  for (BOpcUaServerPointFolder pointFolder : pointFolders) {
                     this.addPointsFolder(pointFolder);
                  }

                  BOpcUaServerProxyExt[] proxyExts = (BOpcUaServerProxyExt[])CompUtil.getDescendants(this.getPoints(), BOpcUaServerProxyExt.class);

                  for (BOpcUaServerProxyExt proxyExt : proxyExts) {
                     this.addControlPoint(proxyExt);
                  }
               }

               return null;
            }));
         }
      }
   }

   public NodeManagerUaNode getUaNodeManager() {
      return this.uaNodeManager;
   }

   public void doPing() {
      this.pingOk();
   }

   public void doPoll() {
   }

   public final BOpcUaServer getOpcUaServer() {
      return (BOpcUaServer)this.getNetwork();
   }

   public String addPointsFolder(BOpcUaServerPointFolder pointFolder) {
      try {
         int ns = this.uaNodeManager.getNamespaceIndex();
         String addName = getRelPathString(this, pointFolder);
         NodeId myPointsFolderId = new NodeId(ns, addName);
         FolderTypeNode pointsFolder = (FolderTypeNode)this.uaNodeManager.createInstance(FolderTypeNode.class, addName, myPointsFolderId);
         UaNode uaNode = this.uaNodeManager.addNodeAndReference(this.myPointsFolder, pointsFolder, ReferenceTypeIdentifiers.Organizes);
         uaNode.setDisplayName(new LocalizedText(pointFolder.getName()));
         return uaNode.getNodeId().toString();
      } catch (StatusException var7) {
         if (this.logger.isLoggable(Level.FINE)) {
            this.logger.log(Level.SEVERE, "Exception occurred while adding PointFolder", (Throwable)var7);
         } else {
            this.logger.log(Level.SEVERE, "Exception occurred while adding PointFolder: " + var7.getLocalizedMessage());
         }

         return null;
      }
   }

   public void deletePointsFolder(BOpcUaServerPointFolder pointFolder) {
      try {
         BOpcUaServer opcUaServer = this.getOpcUaServer();
         if (opcUaServer.server == null) {
            throw new Exception(lex.getText("namespace.error.serverNotRunning"));
         }

         NodeManagerTable addressSpace = opcUaServer.server.getAddressSpace();
         NodeId nodeId = NodeId.parseNodeId(pointFolder.getUaNodeId());
         UaNode ptNode = addressSpace.getNode(nodeId);
         this.uaNodeManager.deleteNode(ptNode, true, true);
      } catch (Exception var6) {
         if (this.logger.isLoggable(Level.FINE)) {
            this.logger.log(Level.SEVERE, "Exception occurred while deleting PointFolder", (Throwable)var6);
         } else {
            this.logger.log(Level.SEVERE, "Exception occurred while deleting PointFolder: " + var6.getLocalizedMessage());
         }
      }
   }

   public boolean addUaNodeHistory(String nodeId, BControlPoint point) {
      try {
         BOpcUaServer opcUaServer = this.getOpcUaServer();
         if (opcUaServer.server == null) {
            throw new Exception(lex.getText("namespace.error.serverNotRunning"));
         } else {
            NodeManagerTable addressSpace = opcUaServer.server.getAddressSpace();
            UaNode node = addressSpace.getNode(NodeId.parseNodeId(nodeId));
            boolean historizing = this.checkAddHistory((UaVariableNode)node, point);
            checkAddAccessLevel((UaVariableNode)node, point, historizing);
            return true;
         }
      } catch (Exception var7) {
         if (this.logger.isLoggable(Level.FINE)) {
            this.logger.log(Level.SEVERE, "Exception occurred while adding HistoryExt to UaNode", (Throwable)var7);
         } else {
            this.logger.log(Level.SEVERE, "Exception occurred while adding HistoryExt to UaNode: " + var7.getLocalizedMessage());
         }

         return false;
      }
   }

   public boolean removeUaNodeHistory(String nodeId, BControlPoint point) {
      try {
         BOpcUaServer opcUaServer = this.getOpcUaServer();
         if (opcUaServer.server == null) {
            throw new Exception(lex.getText("namespace.error.serverNotRunning"));
         } else {
            NodeManagerTable addressSpace = opcUaServer.server.getAddressSpace();
            UaNode node = addressSpace.getNode(NodeId.parseNodeId(nodeId));
            boolean historizing = this.checkRemoveHistory((UaVariableNode)node, point);
            checkAddAccessLevel((UaVariableNode)node, point, historizing);
            return historizing;
         }
      } catch (Exception var7) {
         if (this.logger.isLoggable(Level.FINE)) {
            this.logger.log(Level.SEVERE, "Exception occurred while removing HistoryExt from UaNode", (Throwable)var7);
         } else {
            this.logger.log(Level.SEVERE, "Exception occurred while removing HistoryExt from UaNode: " + var7.getLocalizedMessage());
         }

         return false;
      }
   }

   public boolean addUaNodeCondition(String nodeId, BControlPoint point) {
      try {
         BOpcUaServer opcUaServer = this.getOpcUaServer();
         if (opcUaServer.server == null) {
            throw new Exception(lex.getText("namespace.error.serverNotRunning"));
         } else {
            NodeManagerTable addressSpace = opcUaServer.server.getAddressSpace();
            UaNode node = addressSpace.getNode(NodeId.parseNodeId(nodeId));
            this.checkAddCondition((UaVariableNode)node, point);
            return true;
         }
      } catch (Exception var6) {
         if (this.logger.isLoggable(Level.FINE)) {
            this.logger.log(Level.SEVERE, "Exception occurred while adding NodeCondition", (Throwable)var6);
         } else {
            this.logger.log(Level.SEVERE, "Exception occurred while adding NodeCondition: " + var6.getLocalizedMessage());
         }

         return false;
      }
   }

   public boolean removeUaNodeCondition(String nodeId, BControlPoint point) {
      try {
         BOpcUaServer opcUaServer = this.getOpcUaServer();
         if (opcUaServer.server == null) {
            throw new Exception(lex.getText("namespace.error.serverNotRunning"));
         } else {
            NodeManagerTable addressSpace = opcUaServer.server.getAddressSpace();
            UaNode node = addressSpace.getNode(NodeId.parseNodeId(nodeId));
            return this.checkRemoveCondition((UaVariableNode)node);
         }
      } catch (Exception var7) {
         if (this.logger.isLoggable(Level.FINE)) {
            this.logger.log(Level.SEVERE, "Exception occurred while removing NodeCondition", (Throwable)var7);
         } else {
            this.logger.log(Level.SEVERE, "Exception occurred while removing NodeCondition: " + var7.getLocalizedMessage());
         }

         return false;
      }
   }

   public String addControlPoint(BOpcUaServerProxyExt proxyExt) {
      try {
         BIPointFolder iPointFolder = proxyExt.getParentPointFolder();
         if (iPointFolder instanceof BIOpcUaServerPointFolder) {
            String uaNodeId = ((BIOpcUaServerPointFolder)iPointFolder).getUaNodeId();
            BOpcUaServer opcUaServer = this.getOpcUaServer();
            if (opcUaServer.server == null) {
               return lex.getText("namespace.error.serverNotRunning");
            } else {
               NodeManagerTable addressSpace = opcUaServer.server.getAddressSpace();
               NodeId nodeId = NodeId.parseNodeId(uaNodeId);
               FolderTypeNode uaFolder = (FolderTypeNode)addressSpace.getNode(nodeId);
               if (proxyExt.isNumeric()) {
                  return this.addNumericPoint(uaFolder, proxyExt);
               } else if (proxyExt.isBoolean()) {
                  return this.addBooleanPoint(uaFolder, proxyExt);
               } else if (proxyExt.isEnum()) {
                  return this.addEnumPoint(uaFolder, proxyExt);
               } else {
                  return proxyExt.isString() ? this.addStringPoint(uaFolder, proxyExt) : lex.getText("namespace.error.invalidPointType");
               }
            }
         } else {
            return lex.getText("namespace.error.invalidParentPointFolder");
         }
      } catch (Exception var8) {
         return lex.getText("namespace.error.exception", new Object[]{var8.getLocalizedMessage()});
      }
   }

   public void deleteControlPoint(BOpcUaServerProxyExt proxyExt) {
      try {
         BOpcUaServer opcUaServer = this.getOpcUaServer();
         if (opcUaServer.server == null) {
            throw new Exception(lex.getText("namespace.error.serverNotRunning"));
         }

         NodeManagerTable addressSpace = opcUaServer.server.getAddressSpace();
         NodeId nodeId = NodeId.parseNodeId(proxyExt.getUaNodeId());
         UaNode ptNode = addressSpace.getNode(nodeId);
         this.uaNodeManager.deleteNode(ptNode, true, true);
      } catch (Exception var6) {
         if (this.logger.isLoggable(Level.FINE)) {
            this.logger.log(Level.SEVERE, "Exception occurred while deleting point", (Throwable)var6);
         } else {
            this.logger.log(Level.SEVERE, "Exception occurred while deleting point: " + var6.getLocalizedMessage());
         }
      }
   }

   private String addNumericPoint(FolderTypeNode uaFolder, BOpcUaServerProxyExt proxyExt) {
      try {
         BControlPoint parentPoint = proxyExt.getParentPoint();
         String addName = getRelPathString(this, parentPoint);
         BFacets pointFacets = parentPoint.getFacets();
         NodeBuilderConfiguration conf = new NodeBuilderConfiguration();
         conf.addOptional(VariableIdentifiers.BaseAnalogType_EngineeringUnits);
         conf.addOptional(new ExpandedNodeId("http://opcfoundation.org/UA/", VariableIdentifiers.BaseAnalogType_InstrumentRange.getValue()));
         conf.addOptional("Definition");
         AnalogItemType node = (AnalogItemType)this.uaNodeManager.createNodeBuilder(AnalogItemType.class, conf).setName(addName).build();
         node.setDefinition("NumericControlPoint");
         boolean historizing = this.checkAddHistory((UaVariableNode)node, parentPoint);
         checkAddAccessLevel((UaVariableNode)node, parentPoint, historizing);
         this.checkAddCondition((UaVariableNode)node, parentPoint);
         BUnit units = (BUnit)pointFacets.get("units");
         if (units != null) {
            String unitName = units.getUnitName();
            node.setEngineeringUnits(
               new EUInformation(this.getNamespaceUrl(), 3, new LocalizedText(unitName, LocalizedText.NO_LOCALE), new LocalizedText(unitName, Locale.ENGLISH))
            );
         }

         double max = pointFacets.getd("max", Double.MAX_VALUE);
         double min = pointFacets.getd("min", Double.MIN_VALUE);
         node.setEURange(new Range(min, max));
         node.setValue(new DataValue(new Variant(0.0), StatusCode.GOOD));
         node.setDataTypeId(DataTypeIdentifiers.Double);
         UaNode uaNode = this.uaNodeManager.addNodeAndReference(uaFolder, node, ReferenceTypeIdentifiers.Organizes);
         return this.getUpdatedNodeIdOfProxyExt(uaNode, proxyExt);
      } catch (Exception var15) {
         if (this.logger.isLoggable(Level.FINE)) {
            this.logger.log(Level.SEVERE, "Exception occurred while adding numeric point", (Throwable)var15);
         } else {
            this.logger.log(Level.SEVERE, "Exception occurred while adding numeric point: " + var15.getLocalizedMessage());
         }

         return null;
      }
   }

   private String addBooleanPoint(FolderTypeNode uaFolder, BOpcUaServerProxyExt proxyExt) {
      try {
         BControlPoint parentPoint = proxyExt.getParentPoint();
         String addName = getRelPathString(this, parentPoint);
         BFacets pointFacets = parentPoint.getFacets();
         NodeBuilderConfiguration conf = new NodeBuilderConfiguration();
         conf.addOptional(VariableIdentifiers.TwoStateDiscreteType_FalseState);
         conf.addOptional(VariableIdentifiers.TwoStateDiscreteType_TrueState);
         conf.addOptional("Definition");
         TwoStateDiscreteType node = (TwoStateDiscreteType)this.uaNodeManager.createNodeBuilder(TwoStateDiscreteType.class, conf).setName(addName).build();
         boolean historizing = this.checkAddHistory((UaVariableNode)node, parentPoint);
         checkAddAccessLevel((UaVariableNode)node, parentPoint, historizing);
         this.checkAddCondition((UaVariableNode)node, parentPoint);
         node.setDefinition("BooleanControlPoint");
         String trueText = pointFacets.gets("trueText", "");
         String falseText = pointFacets.gets("falseText", "");
         if (!trueText.isEmpty()) {
            node.setTrueState(new LocalizedText(trueText, Locale.ENGLISH));
         }

         if (!falseText.isEmpty()) {
            node.setFalseState(new LocalizedText(falseText, Locale.ENGLISH));
         }

         node.setValue(new DataValue(new Variant(false), StatusCode.GOOD));
         UaNode uaNode = this.uaNodeManager.addNodeAndReference(uaFolder, node, ReferenceTypeIdentifiers.Organizes);
         return this.getUpdatedNodeIdOfProxyExt(uaNode, proxyExt);
      } catch (Exception var12) {
         return lex.getText("namespace.error", new Object[]{var12.getLocalizedMessage()});
      }
   }

   private String addEnumPoint(FolderTypeNode uaFolder, BOpcUaServerProxyExt proxyExt) {
      try {
         BControlPoint parentPoint = proxyExt.getParentPoint();
         String addName = getRelPathString(this, parentPoint);
         BFacets pointFacets = parentPoint.getFacets();
         NodeBuilderConfiguration conf = new NodeBuilderConfiguration();
         conf.addOptional(VariableIdentifiers.MultiStateDiscreteType_EnumStrings);
         conf.addOptional("Definition");
         MultiStateDiscreteType node = (MultiStateDiscreteType)this.uaNodeManager
            .createNodeBuilder(MultiStateDiscreteType.class, conf)
            .setName(addName)
            .build();
         boolean historizing = this.checkAddHistory((UaVariableNode)node, parentPoint);
         checkAddAccessLevel((UaVariableNode)node, parentPoint, historizing);
         this.checkAddCondition((UaVariableNode)node, parentPoint);
         node.setDefinition("EnumControlPoint");
         BObject bObject = pointFacets.get("range");
         if (bObject instanceof BEnumRange) {
            BEnumRange eRange = (BEnumRange)bObject;
            Type frozenType = eRange.getFrozenType();
            if (frozenType != null) {
               BFrozenEnum instance = (BFrozenEnum)frozenType.getInstance();
               eRange = instance.getRange();
            }

            int[] ordinals = eRange.getOrdinals();
            String[] tags = new String[ordinals.length];
            LocalizedText[] ltArray = new LocalizedText[tags.length];

            for (int i = 0; i < ordinals.length; i++) {
               tags[i] = eRange.getTag(ordinals[i]);
               ltArray[i] = new LocalizedText(tags[i], Locale.ENGLISH);
            }

            if (ltArray.length > 0) {
               node.setEnumStrings(ltArray);
            }
         }

         node.setValue(new DataValue(new Variant(0), StatusCode.GOOD));
         UaNode uaNode = this.uaNodeManager.addNodeAndReference(uaFolder, node, ReferenceTypeIdentifiers.Organizes);
         return this.getUpdatedNodeIdOfProxyExt(uaNode, proxyExt);
      } catch (Exception var16) {
         return lex.getText("namespace.error", new Object[]{var16.getLocalizedMessage()});
      }
   }

   private String addStringPoint(FolderTypeNode uaFolder, BOpcUaServerProxyExt proxyExt) {
      try {
         BControlPoint parentPoint = proxyExt.getParentPoint();
         String addName = getRelPathString(this, parentPoint);
         NodeId stringNodeId = new NodeId(this.getNamespaceIndex(), addName);
         CacheVariable stringVar = new CacheVariable(this.uaNodeManager, stringNodeId, addName, Locale.ENGLISH);
         stringVar.setDataTypeId(DataTypeIdentifiers.String);
         UaNode uaNode = this.uaNodeManager.addNodeAndReference(uaFolder, stringVar, ReferenceTypeIdentifiers.Organizes);
         NodeBuilderConfiguration conf = new NodeBuilderConfiguration();
         conf.addOptional(DataTypeIdentifiers.String);
         boolean historizing = this.checkAddHistory((UaVariableNode)uaNode, parentPoint);
         checkAddAccessLevel((UaVariableNode)uaNode, parentPoint, historizing);
         this.checkAddCondition((UaVariableNode)uaNode, parentPoint);
         stringVar.setValue(new DataValue(new Variant(""), StatusCode.GOOD));
         return this.getUpdatedNodeIdOfProxyExt(uaNode, proxyExt);
      } catch (Exception var10) {
         return lex.getText("namespace.error", new Object[]{var10.getLocalizedMessage()});
      }
   }

   public String pointFacetsChanged(BOpcUaServerProxyExt proxyExt) {
      try {
         BControlPoint parentPoint = proxyExt.getParentPoint();
         BFacets pointFacets = parentPoint.getFacets();
         BOpcUaServer opcUaServer = this.getOpcUaServer();
         if (opcUaServer.server == null) {
            return lex.getText("namespace.error.serverNotRunning");
         } else {
            NodeManagerTable addressSpace = opcUaServer.server.getAddressSpace();
            NodeId nodeId = NodeId.parseNodeId(proxyExt.getUaNodeId());
            UaNode ptNode = addressSpace.getNode(nodeId);
            if (proxyExt.isNumeric() && ptNode instanceof AnalogItemType) {
               AnalogItemType node = (AnalogItemType)ptNode;
               BUnit units = (BUnit)pointFacets.get("units");
               if (units != null) {
                  String unitName = units.getUnitName();
                  node.setEngineeringUnits(
                     new EUInformation(
                        this.getNamespaceUrl(), 3, new LocalizedText(unitName, LocalizedText.NO_LOCALE), new LocalizedText(unitName, Locale.ENGLISH)
                     )
                  );
               }

               double max = pointFacets.getd("max", Double.MAX_VALUE);
               double min = pointFacets.getd("min", Double.MIN_VALUE);
               node.setEURange(new Range(min, max));
            } else if (proxyExt.isBoolean() && ptNode instanceof TwoStateDiscreteType) {
               TwoStateDiscreteType node = (TwoStateDiscreteType)ptNode;
               String trueText = pointFacets.gets("trueText", "");
               String falseText = pointFacets.gets("falseText", "");
               if (!trueText.isEmpty()) {
                  node.setTrueState(new LocalizedText(trueText, Locale.ENGLISH));
               }

               if (!falseText.isEmpty()) {
                  node.setFalseState(new LocalizedText(falseText, Locale.ENGLISH));
               }
            } else if (proxyExt.isEnum() && ptNode instanceof MultiStateDiscreteType) {
               MultiStateDiscreteType nodex = (MultiStateDiscreteType)ptNode;
               BObject bObject = pointFacets.get("range");
               if (bObject instanceof BEnumRange) {
                  BEnumRange eRange = (BEnumRange)bObject;
                  Type frozenType = eRange.getFrozenType();
                  if (frozenType != null) {
                     BFrozenEnum instance = (BFrozenEnum)frozenType.getInstance();
                     eRange = instance.getRange();
                  }

                  int[] ordinals = eRange.getOrdinals();
                  String[] tags = new String[ordinals.length];
                  LocalizedText[] ltArray = new LocalizedText[tags.length];

                  for (int i = 0; i < ordinals.length; i++) {
                     tags[i] = eRange.getTag(ordinals[i]);
                     ltArray[i] = new LocalizedText(tags[i], Locale.ENGLISH);
                  }

                  nodex.setEnumStrings(ltArray);
               }
            }

            return "Ok";
         }
      } catch (Exception var16) {
         return lex.getText("namespace.error", new Object[]{var16.getLocalizedMessage()});
      }
   }

   private boolean checkAddHistory(UaVariableNode node, BControlPoint point) {
      BHistoryExt historyExt = OpcUaServerUtil.getPointHistoryExt(point);
      if (historyExt == null) {
         return false;
      } else {
         HistoryManagerListener historian = this.uaNodeManager.getHistoryManager().getListener();
         if (historian instanceof OpcUaHistorian) {
            ((OpcUaHistorian)historian).addVariableHistory(node, historyExt);
         }

         return true;
      }
   }

   private boolean checkRemoveHistory(UaVariableNode node, BControlPoint point) {
      HistoryManagerListener historian = this.uaNodeManager.getHistoryManager().getListener();
      if (historian instanceof OpcUaHistorian) {
         ((OpcUaHistorian)historian).removeVariableHistory(node);
      }

      return false;
   }

   private boolean checkAddCondition(UaVariableNode source, BControlPoint point) {
      BAlarmSourceExt alarmExt = OpcUaServerUtil.getPointAlarmExt(point);
      if (alarmExt == null) {
         return false;
      } else {
         int ns = this.getNamespaceIndex();
         NodeId myAlarmId = new NodeId(ns, source.getNodeId().getValue() + ".Alarm");
         String name = source.getBrowseName().getName() + "Alarm";
         Class<?> alarmTypeClass = null;
         if (point instanceof BNumericPoint) {
            alarmTypeClass = LimitAlarmTypeNode.class;
         } else if (point instanceof BBooleanPoint) {
            alarmTypeClass = OffNormalAlarmType.class;
         } else if (point instanceof BEnumPoint) {
            alarmTypeClass = DiscreteAlarmTypeNode.class;
         } else {
            if (!(point instanceof BStringPoint)) {
               return false;
            }

            alarmTypeClass = OffNormalAlarmType.class;
         }

         AlarmConditionTypeNode myAlarm = OpcUaServerUtil.createAlarmTypeInstance(this.uaNodeManager, alarmTypeClass, name, myAlarmId);
         if (myAlarm == null) {
            return false;
         } else {
            myAlarm.setSource(source);
            myAlarm.setInput(source);
            myAlarm.setSeverity(500);
            myAlarm.setEnabled(true);
            source.addComponent(myAlarm);
            source.addReference(myAlarm, ReferenceTypeIdentifiers.HasCondition, false);
            this.myPointsFolder.addReference(source, ReferenceTypeIdentifiers.HasEventSource, false);
            this.nsRootFolder.addReference(this.myPointsFolder, ReferenceTypeIdentifiers.HasNotifier, false);
            EventManagerListener eventManager = this.uaNodeManager.getEventManager().getListener();
            if (eventManager instanceof BOpcUaServerAlarmDeviceExt) {
               ((BOpcUaServerAlarmDeviceExt)eventManager).addAlarmPoint(myAlarm, point);
            }

            return true;
         }
      }
   }

   private boolean checkRemoveCondition(UaVariableNode node) {
      EventManagerListener eventManager = this.uaNodeManager.getEventManager().getListener();
      if (eventManager instanceof BOpcUaServerAlarmDeviceExt) {
         UaReference reference = node.getReference(ReferenceTypeIdentifiers.HasCondition, false);
         if (reference != null) {
            UaNode conditionNode = reference.getOppositeNode(node);
            if (conditionNode instanceof AlarmConditionTypeNode) {
               ((BOpcUaServerAlarmDeviceExt)eventManager).removeAlarmPoint((AlarmConditionTypeNode)conditionNode);
            }
         }
      }

      return false;
   }

   private static void checkAddAccessLevel(UaVariableNode node, BControlPoint point, boolean historizing) {
      if (point instanceof BIOpcExport) {
         if (historizing) {
            node.setAccessLevel(AL_EXPORT_HISTORY);
            node.setUserAccessLevel(AL_EXPORT_HISTORY);
         } else {
            node.setAccessLevel(AL_EXPORT);
            node.setUserAccessLevel(AL_EXPORT);
         }
      } else if (historizing) {
         node.setAccessLevel(AL_IMPORT_HISTORY);
         node.setUserAccessLevel(AL_IMPORT_HISTORY);
      } else {
         node.setAccessLevel(AL_IMPORT);
         node.setUserAccessLevel(AL_IMPORT);
      }
   }

   private String getUpdatedNodeIdOfProxyExt(UaNode uaNode, BOpcUaServerProxyExt proxyExt) {
      String uaNodeId = uaNode.getNodeId().toString().substring(0, uaNode.getNodeId().toString().indexOf("=") + 1)
         + this.getNamespaceIndex()
         + uaNode.getNodeId().toString().substring(uaNode.getNodeId().toString().indexOf(";"));
      proxyExt.setUaNodeId(uaNodeId);
      return uaNodeId;
   }

   private static String getRelPathString(BComponent root, BComponent leaf) {
      String path = leaf.getSlotPath().toString();
      String rootPath = root.getSlotPath().toString();
      return path.substring(rootPath.length() + 1);
   }

   private void initializeControlPointsMap() {
      BControlPoint[] controlPoints = this.getPoints().getPoints();

      for (BControlPoint controlPoint : controlPoints) {
         if (!this.controlPointsMap.containsKey(((BOpcUaServerProxyExt)controlPoint.getProxyExt()).getUaNodeId())) {
            this.controlPointsMap.put(((BOpcUaServerProxyExt)controlPoint.getProxyExt()).getUaNodeId(), controlPoint);
         }
      }
   }

   public BStatus getControlPointStatus(NodeId nodeId) {
      BControlPoint controlPoint = this.controlPointsMap.get(nodeId.toString());
      if (controlPoint != null) {
         return controlPoint.getStatus();
      } else {
         BControlPoint[] allPointsInServerDatabase = this.getPoints().getPoints();
         if (this.logger.isLoggable(Level.FINE)) {
            this.logger.log(Level.FINE, "Node ID: " + nodeId + " not found in subscribed control points map.");
            this.logger.log(Level.FINE, "Searching for it in the entire server database.");
         }

         for (BControlPoint point : allPointsInServerDatabase) {
            if (nodeId.toString().equals(((BOpcUaServerProxyExt)point.getProxyExt()).getUaNodeId())) {
               return point.getStatus();
            }
         }

         if (this.logger.isLoggable(Level.FINE)) {
            this.logger.log(Level.FINE, "Node ID: " + nodeId + " not found.");
         }

         return null;
      }
   }

   public void addPointToControlPointsMap(BOpcUaServerProxyExt proxyExt) {
      BControlPoint controlPoint = proxyExt.getParentPoint();
      if (!this.controlPointsMap.containsKey(proxyExt.getUaNodeId())) {
         if (this.logger.isLoggable(Level.FINE)) {
            this.logger.log(Level.FINE, "Adding proxy ext, Node ID: " + proxyExt.getUaNodeId() + ", to subscribed points map");
         }

         this.controlPointsMap.put(proxyExt.getUaNodeId(), controlPoint);
      }
   }

   public void removePointFromControlPointsMap(BOpcUaServerProxyExt proxyExt) {
      if (this.logger.isLoggable(Level.FINE)) {
         this.logger.log(Level.FINE, "Removing proxy ext, Node ID: " + proxyExt.getUaNodeId() + ", from subscribed points map");
      }

      this.controlPointsMap.remove(proxyExt.getUaNodeId());
   }
}
