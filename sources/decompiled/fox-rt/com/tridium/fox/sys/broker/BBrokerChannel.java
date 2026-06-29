package com.tridium.fox.sys.broker;

import com.tridium.asm.Buffer;
import com.tridium.fox.encoding.BogCodec;
import com.tridium.fox.encoding.DecoderFactory;
import com.tridium.fox.message.FoxMessage;
import com.tridium.fox.message.FoxString;
import com.tridium.fox.message.FoxTuple;
import com.tridium.fox.session.Fox;
import com.tridium.fox.session.FoxCircuit;
import com.tridium.fox.session.FoxRequest;
import com.tridium.fox.session.FoxResponse;
import com.tridium.fox.session.FoxSession;
import com.tridium.fox.session.InvalidCommandException;
import com.tridium.fox.sys.BFoxChannel;
import com.tridium.fox.sys.ModuleNotFoundLocalException;
import com.tridium.fox.util.FoxRpcUtil;
import com.tridium.nre.security.NiagaraBasicPermission;
import com.tridium.space.BIGatewaySpace;
import com.tridium.sys.Nre;
import com.tridium.sys.module.Dependency;
import com.tridium.sys.module.NModule;
import com.tridium.sys.module.SyntheticModuleClassLoader;
import com.tridium.sys.schema.ComplexType;
import com.tridium.sys.schema.ComponentSlotMap;
import com.tridium.sys.schema.EnumType;
import com.tridium.sys.schema.NSlot;
import com.tridium.sys.schema.SimpleType;
import com.tridium.sys.schema.SyntheticCompiler;
import com.tridium.sys.schema.SyntheticComplexType;
import com.tridium.sys.schema.SyntheticEnumType;
import com.tridium.sys.schema.SyntheticSimpleType;
import com.tridium.sys.transfer.DeleteOp;
import com.tridium.sys.transfer.TransferResult;
import com.tridium.sys.transfer.TransferStrategy;
import com.tridium.util.PasswordUtil;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javax.baja.io.ValueDocDecoder;
import javax.baja.io.ValueDocEncoder;
import javax.baja.naming.BOrd;
import javax.baja.naming.SlotPath;
import javax.baja.naming.UnresolvedException;
import javax.baja.nav.NavEvent;
import javax.baja.net.NotConnectedException;
import javax.baja.nre.annotations.NiagaraAction;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.nre.platform.RuntimeProfile;
import javax.baja.nre.util.SecurityUtil;
import javax.baja.registry.DependencyInfo;
import javax.baja.registry.ModuleInfo;
import javax.baja.registry.TypeInfo;
import javax.baja.security.BPermissions;
import javax.baja.space.BComponentSpace;
import javax.baja.spy.SpyWriter;
import javax.baja.sync.ProxyBroker;
import javax.baja.sync.SyncBuffer;
import javax.baja.sync.SyncDecoder;
import javax.baja.sync.SyncEncoder;
import javax.baja.sys.Action;
import javax.baja.sys.ActionInvokeException;
import javax.baja.sys.BAbstractService;
import javax.baja.sys.BComplex;
import javax.baja.sys.BComponent;
import javax.baja.sys.BComponentEvent;
import javax.baja.sys.BFacets;
import javax.baja.sys.BFrozenEnum;
import javax.baja.sys.BModule;
import javax.baja.sys.BSimple;
import javax.baja.sys.BValue;
import javax.baja.sys.BajaRuntimeException;
import javax.baja.sys.Clock;
import javax.baja.sys.Context;
import javax.baja.sys.CopyHints;
import javax.baja.sys.ModuleException;
import javax.baja.sys.Property;
import javax.baja.sys.ServiceNotFoundException;
import javax.baja.sys.Slot;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;
import javax.baja.sys.TypeException;
import javax.baja.user.BUser;
import javax.baja.util.BTypeSpec;
import javax.baja.util.Version;
import javax.baja.virtual.BVirtualComponent;
import javax.baja.virtual.BVirtualComponentSpace;
import javax.baja.xml.XException;

@NiagaraType
@NiagaraAction(
   name = "subscriberGc",
   flags = 16
)
public class BBrokerChannel extends BFoxChannel {
   public static final Action subscriberGc = newAction(16, null);
   public static final Type TYPE = Sys.loadType(BBrokerChannel.class);
   static final Version ver3_2_5 = new Version("3.2.5");
   private static boolean DUMP_BUF = false;
   private static int POLL_RATE = 500;
   private final BBrokerChannel.BrokerTypeResolver typeResolver = new BBrokerChannel.BrokerTypeResolver();
   public static final Logger syncLog = Logger.getLogger("fox.broker.sync");
   private boolean isClient;
   private boolean isServer;
   private BComponentSpace space;
   private BBrokerChannel.ClientSyncThread clientThread;
   private ProxyBroker broker;
   private final Object syncFromMasterLock = new Object();
   private long lastSyncFromMaster;
   private Map<String, Map<RuntimeProfile, NModule>> syntheticModuleMap = null;
   private final List<SyncBuffer> syncBuffers = Collections.synchronizedList(new ArrayList<>());
   private final ThreadLocal<ArrayList<Object>> eventCache = new ThreadLocal<>();
   private static final Version GET_REMOTE_MODULE_VERSION_START = new Version("4.6.91.1");
   public static final String HANDLE_TO_PATH_COMMAND = "handleToPath";

   public void subscriberGc() {
      this.invoke(subscriberGc, null, null);
   }

   @Override
   public Type getType() {
      return TYPE;
   }

   public BBrokerChannel() {
      super("broker");
   }

   public ProxyBroker getProxyBroker() {
      return this.broker;
   }

   @Override
   public void checkProcess(FoxRequest req) throws Throwable {
      if (!req.command.equals("loadRoot")) {
         super.checkProcess(req);
      }
   }

   @Override
   public void checkProcessCircuit(FoxCircuit circuit) throws Throwable {
      if (!circuit.command.equals("syncFromMaster")) {
         super.checkProcessCircuit(circuit);
      }
   }

   @Override
   public FoxResponse process(FoxRequest request) throws Throwable {
      String command = request.command;
      String var3 = request.command;
      switch (var3) {
         case "loadRoot":
            return this.loadRoot(request);
         case "load":
            return this.load(request);
         case "loadSlot":
            return this.loadSlot(request);
         case "sub":
            return this.subscribe(request);
         case "unsub":
            return this.unsubscribe(request);
         case "transfer":
            return this.transferReqRes(request);
         case "invoke":
            return this.invoke(request);
         case "handleToPath":
            return handleToPath(this, this.space, request);
         case "serviceToPath":
            return this.serviceToPath(request);
         case "generateHandles":
            return this.generateHandles(request);
         case "rpc":
            return this.rpc(request);
         case "touch":
            return this.touch(request);
         case "loadType":
            return this.loadType(request);
         case "loadModule":
            return this.loadModule(request);
         case "httpConn":
            return this.getHttpConnectionDetails(request);
         case "checkTypes":
            return this.checkTypes(request);
         case "getRemoteModuleVersion":
            return this.getRemoteModuleVersion(request);
         case "unloadVirtualBroker":
            return this.unloadVirtualBroker(request);
         default:
            throw new InvalidCommandException(command);
      }
   }

   @Override
   public void circuitOpened(FoxCircuit circuit) throws Exception {
      String var2 = circuit.command;
      switch (var2) {
         case "syncToMaster":
            this.syncToMaster(circuit);
            return;
         case "syncFromMaster":
            this.syncFromMaster(circuit);
            return;
         case "transferCircuit":
            this.transferCircuit(circuit);
            return;
         case "copy":
            this.copy(circuit);
            return;
         case "delete":
            this.delete(circuit);
            return;
         default:
            throw new InvalidCommandException(circuit.command);
      }
   }

   public void initClient(BFoxComponentSpace space) throws Exception {
      if (this.isTraceOn()) {
         this.trace("c:initClient " + space.getNavOrd());
      }

      this.isClient = true;
      this.isServer = false;
      this.space = space;
      BBrokerChannel.RootInfo rInfo = this.loadRoot();
      space.setRootComponent(rInfo.root);
      space.spaceReadonly = rInfo.spaceReadonly;
      space.defaultLeaseTime = rInfo.defaultLeaseTime;
      this.clientThread = new BBrokerChannel.ClientSyncThread("Fox:ClientSync:" + SecurityUtil.calculateSessionIdHash(this.getConnection().session().getId()));
      this.clientThread.start();
   }

   public void initServer(BComponentSpace space) throws Exception {
      if (this.isTraceOn()) {
         this.trace("s:initServer " + space.getNavOrd());
      }

      this.isClient = false;
      this.isServer = true;
      this.space = space;
      this.broker = new BBrokerChannel.FoxProxyBroker(space);
      this.broker.start();
   }

   public void cleanupClient() {
      if (this.clientThread != null) {
         this.clientThread.kill();
         this.clientThread = null;

         try {
            if (this.space instanceof BFoxVirtualSpace) {
               ((BFoxVirtualSpace)this.space).cleanup(this.getFoxSession());
            }
         } catch (Exception var2) {
         }
      }
   }

   public void cleanupServer() {
      if (this.broker != null) {
         this.broker.stop();
         this.broker = null;
      }
   }

   @Override
   public void sessionClosed(Throwable cause) {
      if (this.isClient) {
         this.cleanupClient();
      } else {
         this.cleanupServer();
      }
   }

   @Override
   protected final boolean useSharedKeyEncryption() {
      return true;
   }

   Context makeEncodeContext(Context baseContext, boolean outgoing) {
      return this.makeEncryptionContext(baseContext, outgoing);
   }

   @Override
   public String marshal(BValue value) throws Exception {
      NiagaraBasicPermission p = new NiagaraBasicPermission("CHANNEL_MARSHAL");
      SecurityManager s = System.getSecurityManager();
      if (s != null) {
         s.checkPermission(p);
      }

      return this.marshal(value, null);
   }

   @Override
   public BValue unmarshal(String xml) throws Exception {
      NiagaraBasicPermission p = new NiagaraBasicPermission("CHANNEL_MARSHAL");
      SecurityManager s = System.getSecurityManager();
      if (s != null) {
         s.checkPermission(p);
      }

      return this.unmarshal(xml, null);
   }

   NModule[] loadModule(final ValueDocDecoder decoder, final String key, final String moduleNameArg, RuntimeProfile runtimeProfile) {
      Version remoteVersion = this.getClientConnection().getRemoteVersion();
      if (remoteVersion.get(0) >= 4 && remoteVersion.get(1) >= 0) {
         Map<RuntimeProfile, NModule> modules;
         if (this.syntheticModuleMap != null && this.syntheticModuleMap.containsKey(moduleNameArg)) {
            modules = this.syntheticModuleMap.get(moduleNameArg);
         } else {
            modules = new HashMap<>();
            if (this.syntheticModuleMap == null) {
               this.syntheticModuleMap = new HashMap<>();
            }

            if (this.isTraceOn()) {
               this.trace("Synthesizing module " + moduleNameArg);
            }

            try {
               FoxRequest req = this.makeRequest("loadModule");
               req.add("module", moduleNameArg);
               FoxResponse resp = this.sendSync(req);

               for (FoxTuple tuple : resp.list("modulePart")) {
                  final FoxMessage partMessage = (FoxMessage)tuple;
                  String runtimeProfileName = partMessage.getString("runtimeProfile");
                  RuntimeProfile currentRuntimeProfile = RuntimeProfile.valueOf(runtimeProfileName);
                  if (runtimeProfile == null || currentRuntimeProfile.equals(runtimeProfile)) {
                     NModule module = new NModule() {
                        Map<BTypeSpec, Type> syntheticTypeMap = new HashMap<>();

                        {
                           this.classLoader = new SyntheticModuleClassLoader(this) {
                              public Class<?> nfind(String name, boolean resolve) {
                                 BTypeSpec typeSpec = BTypeSpec.make(this.module.getModuleName(), name.substring(name.lastIndexOf(46) + 2));
                                 if (syntheticTypeMap.containsKey(typeSpec)) {
                                    return syntheticTypeMap.get(typeSpec).getTypeClass();
                                 } else if (this.module.getTypeClassName(name.substring(name.lastIndexOf(46) + 2)) != null) {
                                    try {
                                       Type type = BBrokerChannel.this.loadType(decoder, this.module, typeSpec);
                                       syntheticTypeMap.put(typeSpec, type);
                                       return type.getTypeClass();
                                    } catch (Exception var5x) {
                                       throw new BajaRuntimeException(var5x);
                                    }
                                 } else {
                                    return super.nfind(name, resolve);
                                 }
                              }
                           };
                           this.moduleName = moduleNameArg;
                           this.modulePartName = partMessage.getString("modulePartName");
                           this.runtimeProfile = RuntimeProfile.valueOf(partMessage.getString("runtimeProfile"));
                           this.preferredSymbol = key;
                           this.bajaVersion = new Version(partMessage.getString("bajaVersion"));
                           this.vendor = partMessage.getString("vendor");
                           this.description = partMessage.getString("description");
                           this.vendorVersion = new Version(partMessage.getString("vendorVersion"));

                           for (String typeItem : partMessage.listStrings("types")) {
                              this.types.put(typeItem.substring(typeItem.lastIndexOf(46) + 2), typeItem);
                           }

                           String[] dependencyPartName = partMessage.listStrings("dependency_modulePartName");
                           String[] dependency_moduleName = partMessage.listStrings("dependency_moduleName");
                           String[] dependenciesBajaVersion = partMessage.listStrings("dependency_bajaVersion");
                           String[] dependenciesVendor = partMessage.listStrings("dependency_vendor");
                           String[] dependenciesVendorVersion = partMessage.listStrings("dependency_vendorVersion");
                           String[] dependenciesRuntimeProfile = partMessage.listStrings("dependency_runtimeProfile");
                           this.depends = new Dependency[dependencyPartName.length];

                           for (int i = 0; i < dependencyPartName.length; i++) {
                              String modulePartName = dependencyPartName[i];
                              RuntimeProfile dependencyRuntimeProfile = RuntimeProfile.valueOf(dependenciesRuntimeProfile[i]);
                              BModule depModule = BBrokerChannel.this.typeResolver
                                 .loadModule(decoder, null, null, dependency_moduleName[i] + "=" + dependency_moduleName[i], null, dependencyRuntimeProfile);
                              NModule targetPart = null;

                              for (NModule mp : (NModule[])depModule.fw(405)) {
                                 if (mp.getRuntimeProfile().name().equals(dependenciesRuntimeProfile[i])) {
                                    targetPart = mp;
                                    break;
                                 }
                              }

                              this.depends[i] = new Dependency(
                                 modulePartName,
                                 new Version(dependenciesBajaVersion[i]),
                                 dependenciesVendor[i],
                                 new Version(dependenciesVendorVersion[i]),
                                 targetPart
                              );
                           }
                        }

                        public BModule bmodule() {
                           if (this.bmodule == null) {
                              this.bmodule = new BModule();
                              ((BModule)this.bmodule).addModulePart(this);
                           }

                           return (BModule)this.bmodule;
                        }

                        public Type getType(String typeName) throws TypeException {
                           BTypeSpec typeSpec = BTypeSpec.make(this.moduleName, typeName);

                           try {
                              if (this.syntheticTypeMap.containsKey(typeSpec)) {
                                 return this.syntheticTypeMap.get(typeSpec);
                              } else {
                                 Type type = BBrokerChannel.this.loadType(decoder, this, typeSpec);
                                 this.syntheticTypeMap.put(typeSpec, type);
                                 return type;
                              }
                           } catch (Exception var4) {
                              throw new BajaRuntimeException("Unable to synthesize " + typeSpec, var4);
                           }
                        }
                     };
                     modules.put(module.getRuntimeProfile(), module);
                  }
               }

               this.syntheticModuleMap.put(moduleNameArg, modules);
            } catch (Exception var17) {
               throw new BajaRuntimeException(var17);
            }
         }

         return modules.values().toArray(new NModule[0]);
      } else {
         throw new ModuleNotFoundLocalException(moduleNameArg, null);
      }
   }

   private FoxResponse loadModule(FoxRequest req) throws Exception {
      String moduleName = req.getString("module");
      if (this.isTraceOn()) {
         this.trace("s:loadModule " + moduleName);
      }

      FoxResponse resp = new FoxResponse(req);
      NModule[] moduleParts = Nre.getModuleManager().loadModuleParts(moduleName);

      for (NModule module : moduleParts) {
         FoxMessage modulePartMsg = new FoxMessage("modulePart");
         modulePartMsg.add("vendorVersion", module.getVendorVersion().toString());
         modulePartMsg.add("vendor", module.getVendor());
         modulePartMsg.add("bajaVersion", module.getBajaVersion().toString());
         modulePartMsg.add("description", module.getDescription());
         modulePartMsg.add("modulePartName", module.getModulePartName());
         modulePartMsg.add("runtimeProfile", module.getRuntimeProfile().name());

         for (String type : module.getTypeList()) {
            modulePartMsg.add("types", module.getTypeClassName(type));
         }

         ModuleInfo moduleInfo = Sys.getRegistry().getModule(module.getModuleName(), module.getRuntimeProfile());

         for (DependencyInfo d : moduleInfo.getDependencies()) {
            ModuleInfo registryInfo = Sys.getRegistry().moduleForDependency(d.getModulePartName());
            modulePartMsg.add("dependency_modulePartName", d.getModulePartName());
            modulePartMsg.add("dependency_moduleName", registryInfo.getModuleName());
            modulePartMsg.add("dependency_runtimeProfile", registryInfo.getRuntimeProfile().name());
            if (registryInfo.getBajaVersion() != null) {
               modulePartMsg.add("dependency_bajaVersion", registryInfo.getBajaVersion().toString());
            } else {
               modulePartMsg.add("dependency_bajaVersion", "0.0");
            }

            if (registryInfo.getVendorVersion() != null) {
               modulePartMsg.add("dependency_vendorVersion", registryInfo.getVendorVersion().toString());
            } else {
               modulePartMsg.add("dependency_vendorVersion", "0.0");
            }

            if (registryInfo.getVendor() != null) {
               modulePartMsg.add("dependency_vendor", registryInfo.getVendor());
            } else {
               modulePartMsg.add("dependency_vendor", "");
            }
         }

         resp.add("modulePart", modulePartMsg);
      }

      return resp;
   }

   Type loadType(ValueDocDecoder decoder, NModule module, BTypeSpec typeSpec) throws Exception {
      if (this.isTraceOn()) {
         this.trace("Synthesizing type " + typeSpec);
      }

      FoxRequest req = this.makeRequest("loadType");
      req.add("type", typeSpec.toString());
      FoxResponse resp = this.sendSync(req);
      String classname = resp.getString("classname");
      boolean isAbstract = resp.getBoolean("abstract", false);
      BTypeSpec superTypeSpec = BTypeSpec.make(((FoxMessage)resp.get("superType")).getString("typespec"));
      BModule superModule = this.typeResolver
         .loadModule(decoder, null, null, superTypeSpec.getModuleName() + "=" + superTypeSpec.getModuleName(), superTypeSpec.toString());
      TypeInfo superTypeInfo = superModule.getType(superTypeSpec.getTypeName()).getTypeInfo();
      Buffer buffer = SyntheticCompiler.compile(
         classname, superTypeInfo, new TypeInfo[0], isAbstract, superTypeInfo.is(BSimple.TYPE) && !isAbstract, true, resp.getBlob("default", null)
      );
      SyntheticModuleClassLoader classLoader = (SyntheticModuleClassLoader)module.getClassLoader();
      Class<?> typeClass = classLoader.ndefineClass(classname, buffer.bytes, 0, buffer.count);
      module.register(classname.substring(classname.lastIndexOf(46) + 2), classname);
      Type type = (Type)typeClass.getField("TYPE").get(null);
      if (type instanceof SyntheticEnumType) {
         SyntheticEnumType syntheticType = (SyntheticEnumType)type;
         syntheticType.setTypeInfo(new SyntheticTypeInfo(type));
         String[] enumOrdinal = resp.listStrings("enum_ordinal");
         String[] enumTag = resp.listStrings("enum_tag");

         for (int i = 0; i < enumOrdinal.length; i++) {
            syntheticType.addEntry(Integer.parseInt(enumOrdinal[i]), enumTag[i]);
         }
      } else if (type instanceof SyntheticComplexType) {
         SyntheticComplexType syntheticType = (SyntheticComplexType)type;
         syntheticType.setTypeInfo(new SyntheticTypeInfo(type));
         String[] slot = resp.listStrings("slot");
         String[] slotName = resp.listStrings("slot_name");
         String[] slotType = resp.listStrings("slot_type");
         String[] slotFlag = resp.listStrings("slot_flag");
         String[] slotFacets = resp.listStrings("slot_facets");
         String[] slotValue = resp.listStrings("slot_value");

         for (int i = 0; i < slot.length; i++) {
            BFacets facets = (BFacets)BFacets.DEFAULT.decodeFromString(slotFacets[i]);
            int flags = Integer.parseInt(slotFlag[i]);
            BTypeSpec valueType = slotType[i].length() == 0 ? null : BTypeSpec.make(slotType[i]);
            String var24 = slot[i];
            switch (var24) {
               case "p":
                  ValueDocDecoder defaultValueDecoder = this.makeDefaultDecoder(new ByteArrayInputStream(slotValue[i].getBytes()), null);
                  defaultValueDecoder.setTypeResolver(this.typeResolver);

                  BValue defaultValue;
                  try {
                     defaultValueDecoder.next();
                     defaultValue = defaultValueDecoder.decode();
                  } catch (XException var30) {
                     throw new BajaRuntimeException("Unable to decode default value for '" + slotName[i] + "' from " + slotValue[i], var30);
                  }

                  if (valueType != null) {
                     BModule propertyTypeModule = this.typeResolver
                        .loadModule(decoder, null, null, valueType.getModuleName() + "=" + valueType.getModuleName(), valueType.toString());
                     Type propertyType = propertyTypeModule.getType(valueType.getTypeName());
                     syntheticType.addProperty(slotName[i], propertyType, flags, defaultValue, facets);
                  }
                  break;
               case "a":
                  BValue defaultValue = null;
                  if (valueType != null) {
                     ValueDocDecoder defaultValueDecoder = this.makeDefaultDecoder(new ByteArrayInputStream(slotValue[i].getBytes()), null);
                     defaultValueDecoder.setTypeResolver(this.typeResolver);
                     defaultValueDecoder.next();
                     defaultValue = defaultValueDecoder.decode();
                  }

                  syntheticType.addAction(slotName[i], flags, defaultValue, facets);
                  break;
               case "t":
                  if (valueType != null) {
                     BModule valueModule = this.typeResolver
                        .loadModule(decoder, null, null, valueType.getModuleName() + "=" + valueType.getModuleName(), valueType.toString());
                     syntheticType.addTopic(slotName[i], flags, valueModule.getType(valueType.getTypeName()), facets);
                  }
            }
         }
      } else {
         if (!(type instanceof SimpleType)) {
            throw new IllegalStateException("Invalid type " + type.getClass());
         }

         SyntheticSimpleType syntheticType = (SyntheticSimpleType)type;
         syntheticType.setTypeInfo(new SyntheticTypeInfo(type));
      }

      return type;
   }

   private FoxResponse loadType(FoxRequest req) throws Exception {
      BTypeSpec typeSpec = BTypeSpec.make(req.getString("type"));
      if (this.isTraceOn()) {
         this.trace("s:loadType " + typeSpec.toString());
      }

      FoxResponse resp = new FoxResponse(req);
      FoxMessage classDetails = resp;

      do {
         classDetails.add("classname", typeSpec.getResolvedType().getTypeClass().getName());
         classDetails.add("typespec", typeSpec.toString());
         classDetails.add("abstract", Modifier.isAbstract(typeSpec.getResolvedType().getTypeClass().getModifiers()));
         if (typeSpec.getResolvedType().is(BSimple.TYPE) && !typeSpec.getResolvedType().isAbstract()) {
            BSimple simple = (BSimple)typeSpec.getInstance();
            this.encodeSimple(classDetails, "default", simple, this.getSessionContext());
         }

         Type type = typeSpec.getResolvedType();
         if (type instanceof EnumType) {
            if (type != BFrozenEnum.TYPE) {
               int[] ordinals = ((EnumType)type).getOrdinals();
               if (ordinals != null) {
                  for (int ord : ordinals) {
                     classDetails.add("enum_ordinal", Integer.toString(ord));
                     classDetails.add("enum_tag", ((EnumType)type).getTag(ord));
                  }
               }
            }
         } else if (type instanceof ComplexType) {
            for (NSlot slot : ((ComplexType)type).getFrozenSlots()) {
               if (slot.getDeclaringType().is(typeSpec.getResolvedType())) {
                  classDetails.add("slot_name", slot.getName());
                  classDetails.add("slot_flag", Integer.toString(slot.getDefaultFlags()));
                  classDetails.add("slot_facets", slot.getFacets().encodeToString());
                  if (slot.isAction()) {
                     classDetails.add("slot", "a");
                     Type paramType = slot.asAction().getParameterType();
                     if (paramType == null) {
                        classDetails.add("slot_type", "");
                        classDetails.add("slot_value", "");
                     } else {
                        classDetails.add("slot_type", paramType.toString());
                        ByteArrayOutputStream stream = new ByteArrayOutputStream();
                        ValueDocEncoder defaultValueEncoder = this.makeDefaultEncoder(stream, this.getSessionContext());
                        defaultValueEncoder.encode(slot.asAction().getParameterDefault());
                        defaultValueEncoder.flush();
                        classDetails.add("slot_value", stream.toString());
                     }
                  } else if (slot.isProperty()) {
                     classDetails.add("slot", "p");
                     classDetails.add("slot_type", slot.asProperty().getType().toString());
                     ByteArrayOutputStream stream = new ByteArrayOutputStream();
                     ValueDocEncoder defaultValueEncoder = this.makeDefaultEncoder(stream, this.getSessionContext());
                     BValue defaultValue = slot.asProperty().getDefaultValue();
                     defaultValueEncoder.encode(defaultValue);
                     defaultValueEncoder.flush();
                     classDetails.add("slot_value", stream.toString());
                  } else if (slot.isTopic()) {
                     classDetails.add("slot", "t");
                     Type eventType = slot.asTopic().getEventType();
                     if (eventType == null) {
                        classDetails.add("slot_type", "");
                     } else {
                        classDetails.add("slot_type", eventType.toString());
                     }

                     classDetails.add("slot_value", "");
                  }
               }
            }
         }

         if (typeSpec.getTypeInfo().getSuperType() != null && !typeSpec.getModuleName().equals(Sys.getBajaModule().getModuleName())) {
            FoxMessage superType = new FoxMessage("superType");
            classDetails.add(superType);
            classDetails = superType;
            typeSpec = typeSpec.getTypeInfo().getSuperType().getTypeSpec();
         } else {
            typeSpec = null;
         }
      } while (typeSpec != null);

      return resp;
   }

   BBrokerChannel.RootInfo loadRoot() throws Exception {
      FoxRequest req = this.makeRequest("loadRoot");
      FoxResponse resp = this.sendSync(req);
      String handle = resp.getString("handle");
      BTypeSpec typeSpec = BTypeSpec.make(resp.getString("type"));
      boolean spaceReadonly = resp.getBoolean("spaceReadonly", false);
      long defaultLeaseTime = resp.getTime("defaultLeaseTime", 60000L);
      if (this.isTraceOn()) {
         this.trace("c:loadRoot " + handle + " " + typeSpec);
      }

      BComponent root = (BComponent)typeSpec.getInstance();
      ((ComponentSlotMap)root.fw(1)).setHandle(handle);
      return new BBrokerChannel.RootInfo(root, spaceReadonly, defaultLeaseTime);
   }

   private FoxResponse loadRoot(FoxRequest req) throws Exception {
      if (this.isTraceOn()) {
         this.trace("s:loadRoot");
      }

      BComponent root = this.space.getRootComponent();
      FoxResponse resp = new FoxResponse(req);
      resp.add("handle", String.valueOf(root.getHandle()));
      resp.add("type", root.getType().toString());
      resp.add("spaceReadonly", this.space.isSpaceReadonly());
      resp.add("defaultLeaseTime", this.space.getDefaultLeaseTime());
      return resp;
   }

   public void ensureLoaded(String[] ords, int depth) throws Exception {
      FoxRequest req = this.space instanceof BFoxVirtualSpace ? this.makeRequest("loadSlot") : this.makeRequest("load");
      req.add("depth", depth);

      for (String ord : ords) {
         req.add("ord", ord);
      }

      if (this.isTraceOn()) {
         this.trace("c:ensureLoaded[" + ords.length + "] depth=" + depth);

         for (String ord : ords) {
            this.trace("  " + ord);
         }
      }

      this.sendSync(req);
      this.syncFromMaster();
   }

   public void load(BComponent c, int depth) throws Exception {
      this.load(c, depth, false);
   }

   public void load(BComponent c, int depth, boolean forceReload) throws Exception {
      String ord = this.toOrd(c);
      FoxRequest req = this.makeRequest("load");
      req.add("ord", ord);
      req.add("depth", depth);
      if (forceReload) {
         req.add("reload", forceReload);
      }

      if (this.isTraceOn()) {
         if (forceReload) {
            this.trace("c:load \"" + c.toPathString() + "\" depth=" + depth + "\" reload=" + forceReload);
         } else {
            this.trace("c:load \"" + c.toPathString() + "\" depth=" + depth);
         }
      }

      this.sendSync(req);
      this.syncFromMaster();
      if (c instanceof BVirtualComponent) {
         ((ComponentSlotMap)c.fw(1)).setBrokerPropsLoaded(true);
      }
   }

   private FoxResponse load(FoxRequest req) throws Exception {
      int depth = req.getInt("depth");
      boolean forceReload = req.getBoolean("reload", false);
      FoxTuple[] ords = req.list("ord");

      for (int i = 0; i < ords.length; i++) {
         String ord = ((FoxString)ords[i]).value;

         try {
            BComponent comp = this.fromOrd(ord);
            if (this.isTraceOn()) {
               if (ords.length == 1) {
                  this.trace("s:load \"" + comp.toPathString() + "\" depth=" + depth);
               } else {
                  if (i == 0) {
                     this.trace("s:load[" + ords.length + "] depth=" + depth);
                  }

                  this.trace("  \"" + comp.toPathString() + "\"");
               }
            }

            BUser user = this.getSessionContext().getUser();
            user.check(comp, BPermissions.operatorRead);
            if (this.space instanceof BVirtualComponentSpace) {
               if (forceReload) {
                  ((ComponentSlotMap)comp.fw(1)).setBrokerPropsLoaded(false);
               }

               comp.loadSlots();
               this.broker.loadOp(comp, depth, comp instanceof BVirtualComponent);
            } else {
               if (this.space instanceof BIGatewaySpace) {
                  comp.loadSlots();
               }

               this.broker.loadOp(comp, depth);
            }
         } catch (Exception var9) {
         }
      }

      return null;
   }

   public Slot loadSlot(BComponent c, String slotName, int depth) throws Exception {
      String ord = this.toOrd(c);
      FoxRequest req = this.makeRequest("loadSlot");
      req.add("ord", ord);
      req.add("slotName", slotName);
      req.add("depth", depth);
      if (this.isTraceOn()) {
         this.trace("c:loadSlot \"" + c.toPathString() + "\" slotName= \"" + slotName + "\" depth=" + depth);
      }

      this.sendSync(req);
      this.syncFromMaster();
      return c.getSlot(slotName);
   }

   // $VF: Could not verify finally blocks. A semaphore variable has been added to preserve control flow.
   // Please report this to the Vineflower issue tracker, at https://github.com/Vineflower/vineflower/issues with a copy of the class file (if you have the rights to distribute it!)
   private FoxResponse loadSlot(FoxRequest req) throws Exception {
      String slotName = req.getString("slotName", null);
      int depth = req.getInt("depth");
      FoxTuple[] ords = req.list("ord");
      ArrayList<Object> events = null;
      boolean var20 = false /* VF: Semaphore variable */;

      try {
         var20 = true;
         if (slotName == null && this.space != null && ords.length > 0) {
            BOrd[] vOrds = new BOrd[ords.length];

            for (int i = 0; i < ords.length; i++) {
               vOrds[i] = BOrd.make(((FoxString)ords[i]).value);
            }

            events = new ArrayList<>();
            this.eventCache.set(events);
            this.space.fw(104, vOrds, null, null, null);
         }

         Set<BComponent> loadedComps = new HashSet<>();

         for (int i = 0; i < ords.length; i++) {
            String ord = ((FoxString)ords[i]).value;

            try {
               BComponent comp = this.fromOrd(ord);
               if (this.isTraceOn()) {
                  if (ords.length == 1) {
                     this.trace("s:load \"" + comp.toPathString() + "\" depth=" + depth);
                  } else {
                     if (i == 0) {
                        this.trace("s:load[" + ords.length + "] depth=" + depth);
                     }

                     this.trace("  \"" + comp.toPathString() + "\"");
                  }
               }

               BUser user = this.getSessionContext().getUser();
               user.check(comp, BPermissions.operatorRead);
               if (slotName != null) {
                  this.space.getLoadCallbacks().loadSlot(comp, slotName);
                  this.broker.loadOp(comp, depth, true);
               } else {
                  BComponent parent = (BComponent)comp.getParent();
                  if (!loadedComps.contains(parent)) {
                     this.broker.loadOp(parent, depth, true);
                     loadedComps.add(parent);
                  }
               }
            } catch (Exception var23) {
            }
         }

         var20 = false;
      } finally {
         if (var20) {
            if (events != null) {
               this.eventCache.set(null);
               if (this.broker != null) {
                  int len = events.size();

                  for (int i = 0; i < len; i++) {
                     try {
                        Object event = events.get(i);
                        if (event instanceof NavEvent) {
                           this.broker.navEvent((NavEvent)event);
                        } else if (event instanceof BComponentEvent) {
                           this.broker.event((BComponentEvent)event);
                        }
                     } catch (Throwable var21) {
                        System.out.println("Error during EnsureLoaded event replay.");
                        var21.printStackTrace();
                     }
                  }
               }
            }
         }
      }

      if (events != null) {
         this.eventCache.set(null);
         if (this.broker != null) {
            int len = events.size();

            for (int i = 0; i < len; i++) {
               try {
                  Object event = events.get(i);
                  if (event instanceof NavEvent) {
                     this.broker.navEvent((NavEvent)event);
                  } else if (event instanceof BComponentEvent) {
                     this.broker.event((BComponentEvent)event);
                  }
               } catch (Throwable var22) {
                  System.out.println("Error during EnsureLoaded event replay.");
                  var22.printStackTrace();
               }
            }
         }
      }

      return null;
   }

   public BValue[] copy(BValue[] values, CopyHints hints) throws Exception {
      BValue[] copy = new BValue[values.length];
      if (this.isTraceOn()) {
         this.trace("c:copy[" + values.length + "]");

         for (BValue value : values) {
            if (value instanceof BComponent) {
               this.trace("  \"" + ((BComponent)value).toPathString() + "\"");
            }
         }
      }

      FoxRequest req = this.makeRequest("copy");
      req.add("defaultOnClone", hints.defaultOnClone);
      req.add("swizzleHandles", hints.swizzleHandles);

      for (int i = 0; i < values.length; i++) {
         BValue v = values[i];
         if (v.isComponent()) {
            req.add("ord", this.toOrd((BComponent)v));
         } else {
            copy[i] = v.newCopy(hints);
         }
      }

      FoxCircuit circuit = this.openCircuit("copy");
      circuit.writeMessage(req);
      FoxMessage resp = circuit.readMessage();
      if (resp.getString("exception", null) != null) {
         throw Fox.exceptionTranslator.messageToException(resp);
      } else {
         ValueDocDecoder decoder = this.makeDefaultDecoder(circuit.getInputStream(), null);

         for (int ix = 0; ix < copy.length; ix++) {
            if (copy[ix] == null) {
               decoder.next();
               copy[ix] = decoder.decode();
            }
         }

         decoder.close();
         return copy;
      }
   }

   private void copy(FoxCircuit circuit) throws Exception {
      FoxMessage req = circuit.readMessage();
      CopyHints hints = new CopyHints();
      hints.defaultOnClone = req.getBoolean("defaultOnClone", hints.defaultOnClone);
      hints.swizzleHandles = req.getBoolean("swizzleHandles", hints.swizzleHandles);
      hints.cx = this.getSessionContext();
      FoxTuple[] ords = req.list("ord");
      BValue[] values = new BValue[ords.length];

      for (int i = 0; i < ords.length; i++) {
         String ord = ((FoxString)ords[i]).value;
         BComponent comp = this.fromOrd(ord);
         values[i] = comp;
         if (this.isTraceOn()) {
            if (ords.length == 1) {
               this.trace("s:copy \"" + comp.toPathString() + "\"");
            } else {
               if (i == 0) {
                  this.trace("s:copy[" + ords.length + "]");
               }

               this.trace("  \"" + comp.toPathString() + "\"");
            }
         }
      }

      try {
         values = BValue.newCopy(values, hints);
      } catch (Exception var11) {
         var11.printStackTrace();
         circuit.writeMessage(Fox.exceptionTranslator.exceptionToMessage(var11));
         circuit.close();
         return;
      }

      circuit.writeMessage(new FoxMessage());
      ValueDocEncoder encoder = this.makeDefaultEncoder(circuit.getOutputStream(), this.getSessionContext());
      encoder.setEncodeTransients(true);
      encoder.setEncodeComments(false);

      for (BValue value : values) {
         encoder.encode(value);
         encoder.flush();
      }

      encoder.close();
   }

   public DeleteOp delete(DeleteOp op, boolean undo) throws Exception {
      if (this.isTraceOn()) {
         this.trace("c:delete undo=" + undo);
      }

      FoxRequest req = this.makeRequest("delete");
      req.add("undo", undo);
      FoxCircuit circuit = this.openCircuit("delete");
      circuit.writeMessage(req);
      OutputStream out = circuit.getOutputStream();
      op.write(out);
      out.flush();
      FoxMessage resp = circuit.readMessage();
      if (resp.getString("exception", null) != null) {
         throw Fox.exceptionTranslator.messageToException(resp);
      } else {
         InputStream in = circuit.getInputStream();
         op = DeleteOp.make(this.space, null, in);
         in.close();
         return op;
      }
   }

   private void delete(FoxCircuit circuit) throws Exception {
      FoxMessage req = circuit.readMessage();
      boolean undo = req.getBoolean("undo");
      if (this.isTraceOn()) {
         this.trace("s:delete undo=" + undo);
      }

      InputStream in = circuit.getInputStream();
      DeleteOp op = DeleteOp.make(this.space, this.getSessionContext(), in);

      try {
         if (undo) {
            op = op.undelete();
         } else {
            op = op.delete();
         }
      } catch (Exception var7) {
         var7.printStackTrace();
         circuit.writeMessage(Fox.exceptionTranslator.exceptionToMessage(var7));
         circuit.close();
         return;
      }

      circuit.writeMessage(new FoxMessage());
      OutputStream out = circuit.getOutputStream();
      op.write(out);
      out.close();
   }

   public void subscribe(BComponent[] c, int depth) throws Exception {
      if (this.isTraceOn()) {
         if (c.length == 1) {
            this.trace("c:sub \"" + c[0].toPathString() + "\" depth=" + depth);
         } else {
            this.trace("c:sub[" + c.length + "] depth=" + depth);

            for (BComponent sub : c) {
               this.trace("  " + sub.toPathString());
            }
         }
      }

      FoxRequest req = this.makeRequest("sub");

      for (BComponent sub : c) {
         req.add("ord", this.toOrd(sub));
      }

      req.add("depth", depth);
      this.sendSync(req);
      this.syncFromMaster();
   }

   private FoxResponse subscribe(FoxRequest req) throws Exception {
      int depth = req.getInt("depth");
      FoxTuple[] ords = req.list("ord");

      for (int i = 0; i < ords.length; i++) {
         String ord = ((FoxString)ords[i]).value;
         BComponent comp = this.fromOrd(ord);
         if (this.isTraceOn()) {
            if (ords.length == 1) {
               this.trace("s:sub \"" + comp.toPathString() + "\" n = " + comp.getName() + " depth=" + depth + " ord = " + ord);
            } else {
               if (i == 0) {
                  this.trace("s:sub[" + ords.length + "] depth=" + depth);
               }

               this.trace("  " + comp.toPathString());
            }
         }

         BUser user = this.getSessionContext().getUser();
         user.check(comp, BPermissions.operatorRead);
         if (this.space instanceof BVirtualComponentSpace) {
            this.broker.subscribeOp(comp, depth, true);
         } else {
            this.broker.subscribeOp(comp, depth);
         }
      }

      return null;
   }

   public void unsubscribe(BComponent[] c) throws Exception {
      FoxRequest req = this.makeRequest("unsub");

      for (BComponent component : c) {
         try {
            req.add("ord", this.toOrd(component));
         } catch (Exception var9) {
         }
      }

      if (this.isTraceOn()) {
         if (c.length == 1) {
            this.trace("c:unsub \"" + c[0].toPathString() + "\"");
         } else {
            this.trace("c:unsub[" + c.length + "]");

            for (BComponent component : c) {
               this.trace("  " + component.toPathString());
            }
         }
      }

      try {
         this.sendAsync(req);
      } catch (NotConnectedException var8) {
      }
   }

   private FoxResponse unsubscribe(FoxRequest req) throws Exception {
      for (String ord : req.listStrings("ord")) {
         try {
            BComponent c = this.fromOrd(ord);
            if (this.isTraceOn()) {
               this.trace("s:unsub \"" + c.toPathString() + "\"");
            }

            this.broker.unsubscribe(this.fromOrd(ord));
         } catch (UnresolvedException var7) {
         }
      }

      return null;
   }

   public Object[] generateHandles(int count) throws Exception {
      FoxRequest req = this.makeRequest("generateHandles");
      req.add("count", count);
      if (this.isTraceOn()) {
         this.trace("c:generateHandles " + count);
      }

      FoxResponse resp = this.sendSync(req);
      Object[] handles = new Object[count];
      StringTokenizer st = new StringTokenizer(resp.getString("handles"), "|");

      for (int i = 0; st.hasMoreTokens(); i++) {
         handles[i] = st.nextToken();
      }

      return handles;
   }

   private FoxResponse generateHandles(FoxRequest req) throws Exception {
      int count = req.getInt("count");
      if (this.isTraceOn()) {
         this.trace("s:generateHandles " + count);
      }

      Object[] handles = (Object[])this.space.fw(103, count, null, null, null);
      StringBuilder buf = new StringBuilder(handles.length * 5);

      for (Object handle : handles) {
         buf.append(handle).append('|');
      }

      FoxResponse resp = new FoxResponse(req);
      resp.add("handles", buf.toString());
      return resp;
   }

   public SlotPath[] handleToPath(Object[] handles) throws Exception {
      return handleToPath(this, handles);
   }

   public static SlotPath[] handleToPath(BFoxChannel channel, Object[] handles) throws Exception {
      FoxRequest req = channel.makeRequest("handleToPath");

      for (Object handle : handles) {
         req.add("handle", handle.toString());
      }

      if (channel.isTraceOn()) {
         if (handles.length == 1) {
            channel.trace("c:handleToPath \"" + handles[0] + "\"");
         } else {
            channel.trace("c:handleToPath[" + handles.length + "]");

            for (Object handle : handles) {
               channel.trace("  \"" + handle + "\"");
            }
         }
      }

      FoxResponse resp = channel.sendSync(req);
      FoxTuple[] paths = resp.list("path");
      if (paths.length != handles.length) {
         throw new IllegalStateException();
      } else {
         SlotPath[] result = new SlotPath[paths.length];

         for (int i = 0; i < paths.length; i++) {
            String path = ((FoxString)paths[i]).value;
            if (!path.equals("\u0000")) {
               result[i] = new SlotPath(path);
            }
         }

         return result;
      }
   }

   public static FoxResponse handleToPath(BFoxChannel channel, BComponentSpace space, FoxRequest req) throws Exception {
      FoxResponse resp = new FoxResponse(req);
      FoxTuple[] handles = req.list("handle");

      for (int i = 0; i < handles.length; i++) {
         String handle = ((FoxString)handles[i]).value;
         if (channel.isTraceOn()) {
            if (handles.length == 1) {
               channel.trace("s:handleToPath \"" + handle + "\"");
            } else {
               if (i == 0) {
                  channel.trace("s:handleToPath[" + handles.length + "]");
               }

               channel.trace("  \"" + handle + "\"");
            }
         }

         SlotPath path = space.handleToSlotPath(handle);
         String pathStr = path == null ? "\u0000" : path.getBody();
         resp.add("path", pathStr);
      }

      return resp;
   }

   public SlotPath serviceToPath(String typeSpec) throws Exception {
      FoxRequest req = this.makeRequest("serviceToPath");
      req.add("typeSpec", typeSpec);
      if (this.isTraceOn()) {
         this.trace("c:serviceToPath " + typeSpec);
      }

      FoxResponse resp = this.sendSync(req);
      String path = resp.getString("path");
      return path == null ? null : new SlotPath(path);
   }

   private FoxResponse serviceToPath(FoxRequest req) throws Throwable {
      String typeSpec = req.getString("typeSpec");
      if (this.isTraceOn()) {
         this.trace("s:serviceToPath " + typeSpec);
      }

      FoxResponse resp = new FoxResponse(req);

      try {
         Type type = Sys.getType(typeSpec);
         BComponent service = Sys.getService(type);
         SlotPath path = service.getSlotPath();
         resp.add("path", path.getBody());
      } catch (ServiceNotFoundException var7) {
         if (this.isTraceOn()) {
            this.trace("s:serviceToPath " + typeSpec + " - ERROR: not found");
         }
      }

      return resp;
   }

   public BValue invoke(BComponent c, Action action, BValue arg) throws Exception {
      String ord = this.toOrd(c);
      FoxRequest req = this.makeRequest("invoke");
      req.add("ord", ord);
      req.add("action", action.getName());
      this.encodeValue(req, "arg", arg, null);
      if (this.isTraceOn()) {
         this.trace("c:invoke " + ord + "." + action.getName());
      }

      FoxResponse resp = this.sendSync(req);
      return this.decodeValue(resp, "return", null);
   }

   private FoxResponse invoke(FoxRequest req) throws Throwable {
      String ord = req.getString("ord");
      String actionName = req.getString("action");
      BValue arg = this.decodeValue(req, "arg", null);
      if (this.isTraceOn()) {
         this.trace("s:invoke " + ord + "." + actionName);
      }

      BComponent comp = this.fromOrd(ord);
      Action action = comp.getAction(actionName);
      Context cx = this.getSessionContext();

      BValue ret;
      try {
         ret = comp.invoke(action, arg, cx);
      } catch (ActionInvokeException var10) {
         this.log.error("Cannot invoke action: " + ord + " " + actionName, var10.getCause());
         throw var10.getCause();
      } catch (Exception var11) {
         this.log.error("Cannot invoke action: " + ord + " " + actionName, var11);
         throw var11;
      }

      FoxResponse resp = new FoxResponse(req);
      this.encodeValue(resp, "return", ret, null);
      return resp;
   }

   public BValue rpc(BComponent c, String id, BValue arg) throws Exception {
      if (this.getConnection().getRemoteVersion().compareTo(FoxSession.VERSION_4_2) >= 0) {
         throw new UnsupportedOperationException("Fw.RPC to Niagara 4.2 or later hosts is not allowed");
      } else {
         String ord = this.toOrd(c);
         FoxRequest req = this.makeRequest("rpc");
         req.add("ord", ord);
         req.add("id", id);
         this.encodeValue(req, "arg", arg, null);
         if (this.isTraceOn()) {
            this.trace("c:rpc " + ord + "." + id);
         }

         FoxResponse resp = this.sendSync(req);
         return this.decodeValue(resp, "return", null);
      }
   }

   private FoxResponse rpc(FoxRequest req) throws Throwable {
      if (this.getConnection().getRemoteVersion().compareTo(FoxSession.VERSION_4_2) >= 0) {
         throw new UnsupportedOperationException("Fw.RPC to Niagara 4.2 or later hosts is not allowed");
      } else {
         String ord = req.getString("ord");
         String id = req.getString("id");
         BValue arg = this.decodeValue(req, "arg", null);
         if (this.isTraceOn()) {
            this.trace("s:rpc " + ord + "." + id);
         }

         BComponent comp = this.fromOrd(ord);

         BValue ret;
         try {
            String[] idValues = id.split(":");
            String methodName = idValues.length == 3 ? idValues[2] : idValues[1];
            if (idValues.length == 3 && "reflectProperty".equals(idValues[0])) {
               ret = FoxRpcUtil.<BValue>doRpcProperty(comp, comp.getProperty(idValues[1]), methodName, arg).orElse(null);
            } else {
               ret = FoxRpcUtil.<BValue>doRpc(comp, methodName, arg).orElse(null);
            }
         } catch (Exception var9) {
            this.log.error("Cannot invoke rpc: " + ord + " " + id, var9);
            throw var9;
         }

         FoxResponse resp = new FoxResponse(req);
         this.encodeValue(resp, "return", ret, null);
         return resp;
      }
   }

   public void touch(String[] ords) throws Exception {
      FoxRequest req = this.makeRequest("touch");

      for (String ord : ords) {
         req.add("ord", ord);
      }

      if (this.isTraceOn()) {
         this.trace("c:touch[" + ords.length + "]");

         for (String ord : ords) {
            this.trace("  " + ord);
         }
      }

      try {
         this.sendAsync(req);
      } catch (NotConnectedException var7) {
      }
   }

   private FoxResponse touch(FoxRequest req) throws Throwable {
      FoxTuple[] ords = req.list("ord");
      if (ords == null) {
         return null;
      } else {
         if (this.isTraceOn()) {
            this.trace("s:touch[" + ords.length + "]");
         }

         BOrd[] ordsInSpace = new BOrd[ords.length];

         for (int i = 0; i < ords.length; i++) {
            ordsInSpace[i] = (BOrd)BOrd.DEFAULT.decodeFromString(((FoxString)ords[i]).value);
            if (this.isTraceOn()) {
               this.trace("  " + ordsInSpace[i]);
            }
         }

         this.space.fw(110, ordsInSpace, null, null, null);
         return null;
      }
   }

   public BComponent getHttpConnectionDetails() {
      try {
         FoxRequest req = this.makeRequest("httpConn");
         if (this.isTraceOn()) {
            this.trace("c:httpConn");
         }

         FoxResponse resp = this.sendSync(req);
         return (BComponent)DecoderFactory.decode(resp, "return", null);
      } catch (Exception var3) {
         throw new BajaRuntimeException(var3);
      }
   }

   private FoxResponse getHttpConnectionDetails(FoxRequest req) throws Exception {
      BComponent webSrv = Sys.getService(Sys.getType("web:WebService"));
      Method method = webSrv.getClass().getMethod("getHttpConnectionDetails", Context.class);
      BComponent details = (BComponent)method.invoke(webSrv, this.getSessionContext());
      FoxResponse resp = new FoxResponse(req);
      BogCodec.add(resp, "return", details, null);
      return resp;
   }

   public Map<String, Version> checkTypes(Collection<TypeInfo> types) throws Exception {
      if (types.size() == 0) {
         return Collections.emptyMap();
      } else {
         if (this.isTraceOn()) {
            this.trace("c:checkTypes");
         }

         FoxRequest req = this.makeRequest("checkTypes");

         for (TypeInfo info : types) {
            RuntimeProfile rp = info.getRuntimeProfile();
            String moduleName = info.getModuleName();
            ModuleInfo moduleInfo = Sys.getRegistry().getModule(moduleName, rp);
            FoxMessage ti = new FoxMessage();
            ti.add("ts", info.toString());
            ti.add("rp", rp.toString());
            ti.add("mn", moduleName);
            ti.add("mv", moduleInfo.getVendorVersion().toString());
            req.add("ti", ti);
         }

         return Arrays.stream(this.sendSync(req).list("ti")).collect(Collectors.toMap(tuple -> {
            try {
               return ((FoxMessage)tuple).getString("ts");
            } catch (IOException var2x) {
               throw new BajaRuntimeException(var2x);
            }
         }, tuple -> {
            try {
               return new Version(((FoxMessage)tuple).getString("mv"));
            } catch (IOException var2x) {
               throw new BajaRuntimeException(var2x);
            }
         }));
      }
   }

   private FoxResponse checkTypes(FoxRequest req) throws Exception {
      if (this.isTraceOn()) {
         this.trace("s:checkTypes");
      }

      TypeInfo webWidgetTypeInfo = null;
      BAbstractService webService = null;
      BAbstractService boxService = null;

      try {
         webWidgetTypeInfo = Sys.getRegistry().getType("web:IFormFactorMax");
         webService = (BAbstractService)Sys.getService(Sys.getType("web:WebService"));
         boxService = (BAbstractService)Sys.getService(Sys.getType("box:BoxService"));
      } catch (Throwable var15) {
      }

      FoxResponse resp = new FoxResponse(req);

      for (FoxTuple tuple : req.list("ti")) {
         try {
            FoxMessage message = (FoxMessage)tuple;
            TypeInfo info = Sys.getRegistry().getType(message.getString("ts"));
            RuntimeProfile rp = RuntimeProfile.valueOf(message.getString("rp"));
            ModuleInfo moduleInfo = Sys.getRegistry().getModule(message.getString("mn"), rp);
            if (info.getRuntimeProfile().toString().equals(message.getString("rp"))
               && info.getModuleName().equals(message.getString("mn"))
               && (
                  webWidgetTypeInfo == null
                     || !info.is(webWidgetTypeInfo)
                     || webService != null && webService.isOperational() && boxService != null && boxService.isOperational()
               )) {
               FoxMessage ti = new FoxMessage();
               ti.add("ts", info.toString());
               ti.add("mv", moduleInfo.getVendorVersion().toString());
               resp.add("ti", ti);
            }
         } catch (Throwable var16) {
         }
      }

      return resp;
   }

   public Optional<Map<String, Version>> getRemoteModuleVersion(Collection<RuntimeProfile> profiles, Collection<ModuleInfo> moduleInfos) throws Exception {
      if (this.isTraceOn()) {
         this.trace("c:getRemoteModuleVersion");
      }

      if (this.getClientConnection().getRemoteVersion().compareTo(GET_REMOTE_MODULE_VERSION_START) < 0) {
         return Optional.empty();
      } else {
         FoxRequest req = this.makeRequest("getRemoteModuleVersion");

         for (RuntimeProfile profile : profiles) {
            req.add("rp", String.valueOf(profile));
         }

         for (ModuleInfo info : moduleInfos) {
            req.add("mi", info.getModuleName() + " " + info.getRuntimeProfile());
         }

         return Optional.of(Arrays.stream(this.sendSync(req).list("ti")).collect(Collectors.toMap(tuple -> {
            try {
               return ((FoxMessage)tuple).getString("mn");
            } catch (IOException var2x) {
               throw new BajaRuntimeException(var2x);
            }
         }, tuple -> {
            try {
               return new Version(((FoxMessage)tuple).getString("mv"));
            } catch (IOException var2x) {
               throw new BajaRuntimeException(var2x);
            }
         })));
      }
   }

   private FoxResponse getRemoteModuleVersion(FoxRequest req) throws IOException {
      if (this.isTraceOn()) {
         this.trace("s:getRemoteModuleVersion");
      }

      FoxResponse resp = new FoxResponse(req);
      List<String> moduleInfos = Arrays.asList(req.listStrings("mi"));
      List<String> runtimeProfiles = Arrays.asList(req.listStrings("rp"));

      for (ModuleInfo moduleInfo : Sys.getRegistry().getModules()) {
         String moduleName = moduleInfo.getModuleName();
         String modulePartName = moduleInfo.getModulePartName();
         String runtimeProfile = String.valueOf(moduleInfo.getRuntimeProfile());
         String moduleKey = moduleName + " " + runtimeProfile;
         if (moduleInfos.isEmpty() && runtimeProfiles.isEmpty()
            || (moduleInfos.contains(moduleKey) || runtimeProfiles.contains(runtimeProfile)) && !modulePartName.endsWith("Test")) {
            FoxMessage ti = new FoxMessage();
            ti.add("mn", moduleName + " " + runtimeProfile);
            ti.add("mv", moduleInfo.getVendorVersion().toString());
            resp.add("ti", ti);
         }
      }

      return resp;
   }

   public void unloadVirtualBroker() {
      if (this.space instanceof BFoxVirtualSpace) {
         if (this.isTraceOn()) {
            this.trace("c:unloadVirtualBroker");
         }

         Version remoteVersion = this.getConnection().getRemoteVersion();
         if (remoteVersion.get(0) >= 4 && remoteVersion.get(1) >= 1) {
            FoxRequest req = this.makeRequest("unloadVirtualBroker");

            try {
               this.sendSync(req);
            } catch (Throwable var4) {
            }
         }

         this.cleanupClient();
      }
   }

   private FoxResponse unloadVirtualBroker(FoxRequest req) throws Exception {
      if (this.isTraceOn()) {
         this.trace("s:unloadVirtualBroker");
      }

      FoxResponse resp = new FoxResponse(req);
      this.cleanupServer();
      this.getConnection().getChannels().remove(this.getPropertyInParent());
      return resp;
   }

   public void syncToMaster(SyncBuffer buf) throws Exception {
      if (this.isTraceOn()) {
         this.trace("c:syncToMaster");
      }

      dump(buf, null);
      FoxCircuit circuit = this.openCircuit("syncToMaster");
      FoxMessage req = new FoxMessage();
      circuit.writeMessage(req);
      SyncEncoder out = this.makeDefaultSyncEncoder(circuit.getOutputStream(), null);
      buf.encode(out);
      out.flush();
      FoxMessage resp = circuit.readMessage();
      FoxMessage error = (FoxMessage)resp.getOptional("error");
      if (error != null) {
         throw Fox.exceptionTranslator.messageToException(error);
      } else {
         this.syncFromMaster();
      }
   }

   public void syncToMaster(FoxCircuit circuit) throws Exception {
      if (this.isTraceOn()) {
         this.trace("s: --> syncToMaster");
      }

      circuit.readMessage();
      FoxMessage resp = new FoxMessage();

      try {
         SyncDecoder in = this.makeDefaultSyncDecoder(circuit.getInputStream(), this.getSessionContext());

         try {
            in.setTypeResolver(this.typeResolver);
         } catch (Throwable var6) {
         }

         SyncBuffer buf = new SyncBuffer(this.space, false);
         buf.decode(in);
         dump(buf, null);
         Context cx = this.getSessionContext();
         buf.commit(cx);
      } catch (Throwable var7) {
         var7.printStackTrace();
         resp.add("error", Fox.exceptionTranslator.exceptionToMessage(var7));
      }

      circuit.writeMessage(resp);
      if (this.isTraceOn()) {
         this.trace("s: <--- syncToMaster");
      }
   }

   public void syncFromMaster() throws Exception {
      long ticks = Clock.ticks();
      if (syncLog.isLoggable(Level.FINE)) {
         this.trace("c:doSyncFromMaster " + (ticks - this.lastSyncFromMaster) + "ms");
      }

      this.lastSyncFromMaster = ticks;
      synchronized (this.syncFromMasterLock) {
         FoxCircuit circuit = this.openCircuit("syncFromMaster");
         FoxMessage req = new FoxMessage();
         circuit.writeMessage(req);
         SyncBuffer buf = new SyncBuffer(this.space, false);

         try {
            PasswordUtil.setAllowSubstitutePasswordValuesOnThread(true);
            SyncDecoder in = this.makeDefaultSyncDecoder(circuit.getInputStream(), null);
            Throwable var8 = null;

            try {
               try {
                  in.setTypeResolver(this.typeResolver);
               } catch (Throwable var29) {
               }

               buf.decode(in);
            } catch (Throwable var30) {
               var8 = var30;
               throw var30;
            } finally {
               if (in != null) {
                  if (var8 != null) {
                     try {
                        in.close();
                     } catch (Throwable var28) {
                        var8.addSuppressed(var28);
                     }
                  } else {
                     in.close();
                  }
               }
            }

            this.syncBuffers.add(this.syncBuffers.size(), buf);
            SyncBuffer[] bufs = this.syncBuffers.toArray(new SyncBuffer[0]);

            for (SyncBuffer b : bufs) {
               b.commit(Context.commit);
            }

            this.syncBuffers.remove(buf);
         } finally {
            PasswordUtil.setAllowSubstitutePasswordValuesOnThread(false);
         }
      }
   }

   public void syncFromMaster(FoxCircuit circuit) throws Exception {
      if (syncLog.isLoggable(Level.FINE)) {
         this.trace("s:syncFromMaster");
      }

      circuit.readMessage();
      SyncBuffer buf = this.broker.detachBuffer();
      if (this.getConnection().session().isLegacyConnection()) {
         buf = new SyncBuffer(this.space, false);
      }

      SyncEncoder out = this.makeDefaultSyncEncoder(circuit.getOutputStream(), this.getSessionContext());
      dump(buf, out.getContext());
      buf.encode(out);
      out.close();
   }

   public TransferResult transfer(TransferStrategy t) throws Exception {
      if (this.isTraceOn()) {
         this.trace("c:transfer");
         t.dump();
      }

      Version ver = this.getConnection().getRemoteVersion();
      return ver != null && ver.compareTo(ver3_2_5) >= 0 ? this.transferCircuit(t) : this.transferReqRes(t);
   }

   private TransferResult transferReqRes(TransferStrategy t) throws Exception {
      FoxRequest req = this.makeRequest("transfer");
      TransferCodec.transferToMessage(req, t);
      FoxResponse resp = this.sendSync(req);
      if (this.isTraceOn()) {
         if (resp == null) {
            this.trace(" resp:  null");
         } else {
            resp.dump();
         }
      }

      TransferResult result = TransferCodec.messageToResult(this.space, resp);
      this.syncFromMaster();
      return result;
   }

   FoxResponse transferReqRes(FoxRequest req) throws Exception {
      Context cx = this.getSessionContext();
      TransferStrategy t = TransferCodec.messageToTransfer(req, cx);
      if (this.isTraceOn()) {
         this.trace("s:transfer");
         t.dump();
      }

      FoxResponse resp = new FoxResponse(req);
      TransferResult result = t.transfer();
      return result == null ? null : TransferCodec.resultToMessage(resp, result);
   }

   private TransferResult transferCircuit(TransferStrategy t) throws Exception {
      FoxRequest req = this.makeRequest("transferCircuit");
      TransferCodec.transferToMessage(req, t);
      FoxCircuit circuit = this.openCircuit("transferCircuit");
      circuit.writeMessage(req);

      while (true) {
         FoxMessage resp = circuit.readMessage();
         String status = resp.getString("s", null);
         if (status == null) {
            if (resp.getString("exception", null) != null) {
               throw Fox.exceptionTranslator.messageToException(resp);
            } else if (resp.getBoolean("done", false)) {
               TransferResult result = TransferCodec.messageToResult(this.space, resp);
               this.syncFromMaster();
               return result;
            } else {
               throw new IllegalStateException();
            }
         }

         t.updateStatus(status);
      }
   }

   void transferCircuit(FoxCircuit circuit) throws Exception {
      try {
         Context cx = this.getSessionContext();
         FoxMessage req = circuit.readMessage();
         TransferStrategy t = TransferCodec.messageToTransfer(req, cx);
         if (this.isTraceOn()) {
            this.trace("s:transfer");
            t.dump();
         }

         t.setListener(new BFoxChannel.TransferStatusPipe(circuit));
         TransferResult result = t.transfer();
         FoxMessage resp = TransferCodec.resultToMessage(new FoxResponse(), result);
         resp.add("done", true);
         circuit.writeMessage(resp);
      } catch (Exception var7) {
         var7.printStackTrace();
         circuit.writeMessage(Fox.exceptionTranslator.exceptionToMessage(var7));
      }

      circuit.close();
   }

   public void spy(SpyWriter out) throws Exception {
      if (this.broker != null) {
         this.broker.spy(out);
      }

      ThreadPoolExecutor executor = BBrokerChannel.SubscriberGcHolder.instance;
      out.startProps("Subscriber GC Executor");
      out.prop("isRunning", executor != null && !executor.isShutdown());
      if (executor != null) {
         out.prop("Keep Alive (seconds)", executor.getKeepAliveTime(TimeUnit.SECONDS))
            .prop("Pool Size", executor.getPoolSize())
            .prop("Core Pool Size", executor.getCorePoolSize())
            .prop("Active Threads Working", executor.getActiveCount())
            .prop("Task Count", executor.getTaskCount())
            .prop("Completed Task Count", executor.getCompletedTaskCount());
      }

      out.endProps();
      super.spy(out);
   }

   public final String toOrd(BComponent c) {
      String handle = (String)c.getHandle();
      if (handle == null) {
         throw new IllegalStateException(c.toDebugString());
      } else {
         return "h:" + handle;
      }
   }

   public final BComponent fromOrd(String ord) {
      return (BComponent)BOrd.make(ord).get(this.space);
   }

   public static void dump(SyncBuffer buf, Context cx) throws Exception {
      if (DUMP_BUF) {
         try {
            SyncEncoder out = new SyncEncoder(System.out, cx);
            buf.encode(out);
            out.flush();
         } catch (Exception var3) {
            var3.printStackTrace();
         }
      }
   }

   public void doSubscriberGc() {
      BBrokerChannel.SubscriberGcHolder.instance.execute(() -> {
         ProxyBroker broker = this.broker;
         if (broker != null) {
            broker.gc();
         }
      });
   }

   class BrokerTypeResolver extends BFoxChannel.LegacyTypeResolver {
      public BrokerTypeResolver() {
         super(BBrokerChannel.this, false);
      }

      public BModule loadModule(ValueDocDecoder decoder, BComplex parent, String propName, String moduleStr, String typeStr) {
         return this.loadModule(decoder, parent, propName, moduleStr, typeStr, null);
      }

      public BModule loadModule(ValueDocDecoder decoder, BComplex parent, String propName, String moduleStr, String typeStr, RuntimeProfile runtimeProfile) {
         try {
            return super.loadModule(decoder, parent, propName, moduleStr, typeStr);
         } catch (XException var14) {
            int equals = moduleStr.indexOf(61);
            String key = moduleStr.substring(0, equals).trim();
            String moduleName = moduleStr.substring(equals + 1).trim();
            Throwable t = var14.getCause();
            if (t instanceof ModuleException && Fox.appName.equals("Station")) {
               try {
                  this.getModuleMap(decoder).put(key, null);
               } catch (Exception var13) {
                  throw var14;
               }

               if (BBrokerChannel.syncLog.isLoggable(Level.FINE)) {
                  StringBuilder sb = new StringBuilder();
                  sb.append("Skipping property '").append(propName);
                  sb.append("', type='").append(typeStr);
                  sb.append("' on parent '");
                  if (parent == null) {
                     sb.append("null");
                  } else {
                     sb.append(parent.getType().getDisplayName(null));
                  }

                  sb.append("': ").append(var14.getMessage());
                  sb.append(" [").append(decoder.line()).append(":").append(decoder.column()).append("]");
                  BBrokerChannel.syncLog.fine(sb.toString());
               }

               return null;
            } else {
               return AccessController.doPrivileged((PrivilegedAction<BModule>)(() -> {
                  NModule[] modules = BBrokerChannel.this.loadModule(decoder, key, moduleName, runtimeProfile);
                  this.updateModuleMap(decoder, modules);

                  for (NModule module : modules) {
                     if (runtimeProfile != null && module.getRuntimeProfile() == runtimeProfile) {
                        return module.bmodule();
                     }

                     if (typeStr != null) {
                        List<String> types = Arrays.asList(module.getTypeList());
                        if (types.contains(typeStr.substring(typeStr.indexOf(58) + 1))) {
                           return module.bmodule();
                        }
                     }
                  }

                  return modules[0].bmodule();
               }));
            }
         }
      }

      @Override
      public BValue newInstance(ValueDocDecoder decoder, BComplex parent, String propName, Property prop, String typeStr) {
         try {
            return super.newInstance(decoder, parent, propName, prop, typeStr);
         } catch (RuntimeException var11) {
            Throwable c = var11.getCause();
            if (c instanceof ModuleException && Fox.appName.equals("Station")) {
               if (BBrokerChannel.syncLog.isLoggable(Level.FINE)) {
                  StringBuilder sb = new StringBuilder();
                  sb.append("Skipping property '").append(propName);
                  sb.append("', type='").append(typeStr);
                  sb.append("' on parent '");
                  if (parent == null) {
                     sb.append("null");
                  } else {
                     sb.append(parent.getType().getDisplayName(null));
                  }

                  sb.append("': ").append(var11.getMessage());
                  sb.append(" [").append(decoder.line()).append(":").append(decoder.column()).append("]");
                  BBrokerChannel.syncLog.fine(sb.toString());
               }

               try {
                  decoder.skip();
                  return null;
               } catch (XException var9) {
                  throw var9;
               } catch (Exception var10) {
                  throw new XException(var10);
               }
            } else {
               throw var11;
            }
         }
      }
   }

   class ClientSyncThread extends Thread {
      boolean isAlive = true;
      int count;

      ClientSyncThread(String name) {
         super(Fox.threadGroup, name);
      }

      public void kill() {
         this.isAlive = false;
         this.interrupt();
      }

      @Override
      public synchronized void run() {
         while (this.isAlive) {
            try {
               sleep(BBrokerChannel.POLL_RATE);
               if (Clock.ticks() - BBrokerChannel.this.lastSyncFromMaster >= BBrokerChannel.POLL_RATE) {
                  BBrokerChannel.this.syncFromMaster();
               }
            } catch (InterruptedException var2) {
            } catch (Exception var3) {
               if (this.isAlive) {
                  var3.printStackTrace();
               }
            }
         }
      }
   }

   class FoxProxyBroker extends ProxyBroker {
      FoxProxyBroker(BComponentSpace master) {
         super(master);
      }

      public void event(BComponentEvent event) {
         ArrayList<Object> events = BBrokerChannel.this.eventCache.get();
         if (events != null) {
            events.add(event);
         } else {
            super.event(event);
         }
      }

      public void navEvent(NavEvent event) {
         ArrayList<Object> events = BBrokerChannel.this.eventCache.get();
         if (events != null) {
            events.add(event);
         } else {
            super.navEvent(event);
            if (event.getParent() instanceof BComponent) {
               switch (event.getId()) {
                  case 2:
                  case 5:
                     BBrokerChannel.this.subscriberGc();
               }
            }
         }
      }

      public String toString() {
         return "ProxyBroker for FoxServerConnection [" + BBrokerChannel.this.getConnection().session() + "]";
      }
   }

   static class RootInfo {
      BComponent root;
      boolean spaceReadonly;
      long defaultLeaseTime;

      RootInfo(BComponent root, boolean spaceReadonly, long defaultLeaseTime) {
         this.root = root;
         this.spaceReadonly = spaceReadonly;
         this.defaultLeaseTime = defaultLeaseTime;
      }
   }

   private static final class SubscriberGcHolder {
      private static final ThreadPoolExecutor instance = new ThreadPoolExecutor(
         0, 1, 60L, TimeUnit.SECONDS, new LinkedBlockingQueue<>(), r -> new Thread(r, "brokerChannelSubscriberGc")
      );
   }
}
