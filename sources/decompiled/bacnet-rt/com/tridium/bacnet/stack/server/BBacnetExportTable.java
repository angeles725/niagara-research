package com.tridium.bacnet.stack.server;

import com.tridium.bacnet.BacnetQuery;
import com.tridium.bacnet.datatypes.BNextInstArgs;
import com.tridium.bacnet.stack.BBacnetStack;
import com.tridium.box.BOrdChannel;
import com.tridium.collection.BFilteredTable;
import com.tridium.json.JSONArray;
import com.tridium.json.JSONObject;
import com.tridium.json.quick.QuickJSONWriter;
import com.tridium.util.ComponentTreeCursor;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.baja.agent.AgentFilter;
import javax.baja.agent.AgentList;
import javax.baja.bacnet.BBacnetNetwork;
import javax.baja.bacnet.datatypes.BBacnetObjectIdentifier;
import javax.baja.bacnet.export.BBacnetPointDescriptor;
import javax.baja.bacnet.export.BIBacnetExportObject;
import javax.baja.bacnet.export.BLocalBacnetDevice;
import javax.baja.bacnet.export.BOutOfServiceExt;
import javax.baja.bacnet.io.AsnOutput;
import javax.baja.collection.BITable;
import javax.baja.control.BControlPoint;
import javax.baja.naming.BOrd;
import javax.baja.naming.OrdQuery;
import javax.baja.nre.annotations.NiagaraAction;
import javax.baja.nre.annotations.NiagaraActions;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.nre.util.Array;
import javax.baja.nre.util.IFilter;
import javax.baja.registry.TypeInfo;
import javax.baja.rpc.NiagaraRpc;
import javax.baja.rpc.Transport;
import javax.baja.rpc.TransportType;
import javax.baja.space.BComponentSpace;
import javax.baja.spy.SpyWriter;
import javax.baja.sys.Action;
import javax.baja.sys.BComponent;
import javax.baja.sys.BIObject;
import javax.baja.sys.BInteger;
import javax.baja.sys.BValue;
import javax.baja.sys.Context;
import javax.baja.sys.Property;
import javax.baja.sys.Slot;
import javax.baja.sys.SlotCursor;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;
import javax.baja.util.LexiconModule;

@NiagaraType
@NiagaraActions({@NiagaraAction(
      name = "getObjectOrdById",
      parameterType = "BBacnetObjectIdentifier",
      defaultValue = "BBacnetObjectIdentifier.DEFAULT",
      returnType = "BOrd",
      flags = 4
   ), @NiagaraAction(
      name = "getNextInst",
      parameterType = "BNextInstArgs",
      defaultValue = "new BNextInstArgs()",
      returnType = "BInteger",
      flags = 4
   ), @NiagaraAction(
      name = "reorderOutOfServiceExt",
      flags = 4
   )})
public class BBacnetExportTable extends BComponent implements BIBacnetExportFolder {
   public static final Action getObjectOrdById = newAction(4, BBacnetObjectIdentifier.DEFAULT, null);
   public static final Action getNextInst = newAction(4, new BNextInstArgs(), null);
   public static final Action reorderOutOfServiceExt = newAction(4, null);
   public static final Type TYPE = Sys.loadType(BBacnetExportTable.class);
   private static final Logger logger = Logger.getLogger("bacnet.server");
   BBacnetExportTable.ServerObjectComparator comparator = new BBacnetExportTable.ServerObjectComparator();
   BBacnetExportTable.ExportObjectsFilter canExportFilter = new BBacnetExportTable.ExportObjectsFilter();
   private static final LexiconModule lex = LexiconModule.make("bacnet");
   private final ConcurrentHashMap<BBacnetObjectIdentifier, BIBacnetExportObject> exportsByObjectId = new ConcurrentHashMap<>();
   private final ConcurrentHashMap<String, BIBacnetExportObject> exportsByObjectName = new ConcurrentHashMap<>();
   private final ArrayList<BBacnetObjectIdentifier> objectList = new ArrayList<>();
   private final Object OBJECTS_BY_ORD_MONITOR = new Object();
   private final HashSet<BBacnetObjectIdentifier> objectsIdExportedByOrd = new HashSet<>();
   private final HashMap<BOrd, BBacnetObjectIdentifier> objectIdsByOrd = new HashMap<>();

   public BOrd getObjectOrdById(BBacnetObjectIdentifier parameter) {
      return (BOrd)this.invoke(getObjectOrdById, parameter, null);
   }

   public BInteger getNextInst(BNextInstArgs parameter) {
      return (BInteger)this.invoke(getNextInst, parameter, null);
   }

   public void reorderOutOfServiceExt() {
      this.invoke(reorderOutOfServiceExt, null, null);
   }

   public Type getType() {
      return TYPE;
   }

   public BOrd doGetObjectOrdById(BBacnetObjectIdentifier objectId) {
      BIBacnetExportObject e = this.byObjectId(objectId);
      return e == null ? BOrd.DEFAULT : ((BComponent)e).getOrdInSpace();
   }

   public void doReorderOutOfServiceExt() {
      BComponent parent = this.getParent().asComponent();
      if (parent != null && parent instanceof BLocalBacnetDevice) {
         BComponent networkComp = parent.getParent().asComponent();
         if (networkComp != null && networkComp instanceof BBacnetNetwork) {
            BBacnetNetwork network = (BBacnetNetwork)networkComp;
            network.getWorker().post(new Runnable() {
               @Override
               public void run() {
                  SlotCursor<Slot> sc = BBacnetExportTable.this.getSlots();

                  while (sc.next(BBacnetPointDescriptor.class)) {
                     BValue value = sc.get();
                     if (value instanceof BBacnetPointDescriptor) {
                        BBacnetPointDescriptor pd = (BBacnetPointDescriptor)value;
                        BBacnetExportTable.this.reorderOutOfServiceExt(pd);
                     }
                  }
               }
            });
         }
      }
   }

   private void reorderOutOfServiceExt(BBacnetPointDescriptor export) {
      BControlPoint point = export.getPoint();
      if (point != null) {
         Property[] props = point.getDynamicPropertiesArray();
         if (props != null) {
            for (int i = 0; i < props.length; i++) {
               if (props[i].getType().equals(BOutOfServiceExt.TYPE)) {
                  point.reorderToTop(props[i]);
                  break;
               }
            }
         }
      }
   }

   public String toString(Context cx) {
      return "ExportTable:" + this.getSize();
   }

   @Override
   public BBacnetExportTable getExports() {
      return this;
   }

   public String export(BIBacnetExportObject object) {
      BBacnetObjectIdentifier objectId = object.getObjectId();
      String objectName = object.getObjectName();
      if (logger.isLoggable(Level.FINE)) {
         logger.fine("export:" + object + " id=" + objectId + " name=" + objectName + " ord=" + object.getObjectOrd());
      }

      BIBacnetExportObject dup = this.exportsByObjectId.get(objectId);
      if (dup != null && dup != object) {
         return "Duplicate Object ID";
      } else {
         dup = this.exportsByObjectName.get(objectName);
         if (dup != null && dup != object) {
            return "Duplicate Object Name";
         } else {
            if (dup == null) {
               this.exportsByObjectId.put(objectId, object);
               this.exportsByObjectName.put(objectName, object);
               this.objectList.add(objectId);
               synchronized (this.OBJECTS_BY_ORD_MONITOR) {
                  if (this.objectsIdExportedByOrd.contains(objectId)) {
                     this.objectIdsByOrd.values().remove(objectId);
                  }

                  this.objectsIdExportedByOrd.add(objectId);
                  this.objectIdsByOrd.put(object.getObjectOrd(), objectId);
               }
            }

            return null;
         }
      }
   }

   public String exportByOrd(BIBacnetExportObject object) {
      BBacnetObjectIdentifier objectId = object.getObjectId();
      if (objectId != null) {
         synchronized (this.OBJECTS_BY_ORD_MONITOR) {
            if (this.objectsIdExportedByOrd.contains(objectId)) {
               this.objectIdsByOrd.values().remove(objectId);
            }

            this.objectsIdExportedByOrd.add(objectId);
            this.objectIdsByOrd.put(object.getObjectOrd(), objectId);
         }
      }

      return null;
   }

   public void unexport(BBacnetObjectIdentifier objectId, String objectName, BIBacnetExportObject object) {
      BIBacnetExportObject idDup = this.exportsByObjectId.get(objectId);
      BIBacnetExportObject nameDup = this.exportsByObjectName.get(objectName);
      if (logger.isLoggable(Level.FINE)) {
         logger.fine(
            "unexport:" + object + " id=" + objectId + " name=" + objectName + " ord=" + object.getObjectOrd() + "\n  idDup=" + idDup + "  nameDup=" + nameDup
         );
      }

      if (idDup == object) {
         this.exportsByObjectId.remove(objectId);
         this.objectList.remove(objectId);
      }

      if (nameDup == object) {
         this.exportsByObjectName.remove(objectName);
      }

      synchronized (this.OBJECTS_BY_ORD_MONITOR) {
         if (this.objectsIdExportedByOrd.contains(objectId)) {
            this.objectsIdExportedByOrd.remove(objectId);
            this.objectIdsByOrd.values().remove(objectId);
         }
      }

      ((BBacnetStack)BBacnetNetwork.bacnet().getBacnetComm()).getServer().getEventHandler().removeEventSummary(objectId);
   }

   public BIBacnetExportObject byObjectId(BBacnetObjectIdentifier objectId) {
      BIBacnetExportObject e = this.exportsByObjectId.get(objectId);
      if (e == null) {
         return null;
      } else {
         return e.isFatalFault() ? null : e;
      }
   }

   public BIBacnetExportObject byObjectName(String objectName) {
      BIBacnetExportObject e = this.exportsByObjectName.get(objectName);
      if (e == null) {
         return null;
      } else {
         return e.isFatalFault() ? null : e;
      }
   }

   public BBacnetObjectIdentifier byObjectOrd(BOrd ord) {
      if (ord != null && !ord.equals(BOrd.NULL)) {
         BOrd objectOrd = ord.relativizeToSession();
         OrdQuery[] oqs = objectOrd.parse();
         String scheme = oqs[0].getScheme();
         if (scheme.equals("station")) {
            objectOrd = BOrd.make(oqs, 1, oqs.length);
         } else if (scheme.equals("bac")) {
            try {
               return (BBacnetObjectIdentifier)BBacnetObjectIdentifier.DEFAULT.decodeFromString(((BacnetQuery)oqs[0]).getObject());
            } catch (Exception var10) {
               logger.severe("Error decoding BACnet Ord:" + objectOrd + ": oq=" + oqs[0]);
               if (logger.isLoggable(Level.FINE)) {
                  logger.log(Level.FINE, "Stack Trace: ", (Throwable)var10);
               }

               return null;
            }
         }

         BBacnetObjectIdentifier objectId = null;
         synchronized (this.OBJECTS_BY_ORD_MONITOR) {
            objectId = this.objectIdsByOrd.get(objectOrd);
            if (objectId == null && scheme.equals("slot")) {
               BOrd handleOrd = ord.resolve(Sys.getStation()).get().asComponent().getHandleOrd();
               objectId = this.objectIdsByOrd.get(handleOrd);
            }

            if (objectId == null && scheme.equals("h")) {
               BOrd slotPathOrd = ord.resolve(Sys.getStation()).get().asComponent().getSlotPathOrd();
               objectId = this.objectIdsByOrd.get(slotPathOrd);
            }

            return objectId;
         }
      } else {
         return null;
      }
   }

   public void writeObjectIds(AsnOutput out) {
      Iterator<BBacnetObjectIdentifier> ids = this.objectList.iterator();

      while (ids.hasNext()) {
         out.writeObjectIdentifier(ids.next());
      }
   }

   public BBacnetObjectIdentifier[] getObjectIds() {
      return this.objectList.toArray(new BBacnetObjectIdentifier[0]);
   }

   public BBacnetObjectIdentifier getEntry(int index) {
      return this.objectList.get(index);
   }

   public int getSize() {
      return this.objectList.size();
   }

   public BIBacnetExportObject[] getExportedObjects(Type t) {
      if (!t.is(BIBacnetExportObject.TYPE)) {
         return null;
      } else {
         Class<?> c = t.getTypeClass();
         Array<BIBacnetExportObject> ret = new Array(BIBacnetExportObject.class);
         Enumeration<BIBacnetExportObject> e = this.exportsByObjectId.elements();

         while (e.hasMoreElements()) {
            BIBacnetExportObject o = e.nextElement();
            if (c.isInstance(o) && !o.isFatalFault()) {
               ret.add(o);
            }
         }

         return (BIBacnetExportObject[])ret.trim();
      }
   }

   public BInteger doGetNextInst(BNextInstArgs arg) {
      return BInteger.make(this.getNextInstance(arg.getObjectType(), arg.getSiblings().getOrdinals()));
   }

   public int getNextInstance(int objectType) {
      return this.getNextInstance(objectType, new int[0]);
   }

   public int getNextInstance(int objectType, int[] siblings) {
      ComponentTreeCursor c = new ComponentTreeCursor(this, null);
      int count = 0;

      while (c.next(BIBacnetExportObject.class)) {
         if (((BIBacnetExportObject)c.get()).getObjectId().getObjectType() == objectType) {
            count++;
         }
      }

      int[] list = new int[siblings.length + count];
      System.arraycopy(siblings, 0, list, 0, siblings.length);
      c.reset();
      count = siblings.length;

      while (c.next(BIBacnetExportObject.class)) {
         BIBacnetExportObject obj = (BIBacnetExportObject)c.get();
         if (obj.getObjectId().getObjectType() == objectType) {
            list[count++] = obj.getObjectId().getInstanceNumber();
         }
      }

      Arrays.sort(list);
      int next = 0;

      for (int i = 0; i < list.length; i++) {
         if (list[i] > next) {
            return next;
         }

         if (list[i] == next) {
            next++;
         }
      }

      return next;
   }

   @NiagaraRpc(
      transports = {@Transport(
         type = TransportType.box
      )},
      permissions = "R"
   )
   public JSONObject bqlDiscover(String bqlQuery, Context cx) throws Exception {
      BComponentSpace componentSpace = Sys.getStation().getComponentSpace();
      BComponent rootComponent = componentSpace.getRootComponent();
      BOrd bql = BOrd.make(bqlQuery);

      BITable<?> table;
      try {
         table = (BITable<?>)bql.get(rootComponent, cx);
      } catch (Exception var11) {
         String errorMsg = lex.getText("export.rpc.bqlDiscover.error", cx, new Object[]{bqlQuery});
         logger.log(Level.SEVERE, errorMsg, (Throwable)var11);
         throw new Exception(errorMsg);
      }

      BITable<?> filtered = new BFilteredTable(table, this.canExportFilter);
      JSONObject schema = BOrdChannel.encodeTableSchema(filtered, cx);
      JSONArray contents = QuickJSONWriter.toJSONArray(out -> BOrdChannel.encodeTableContents(filtered, out, cx));
      JSONObject result = new JSONObject();
      result.put("schema", schema);
      result.put("contents", contents);
      return result;
   }

   public void spy(SpyWriter out) throws Exception {
      super.spy(out);
      out.startProps();
      out.trTitle("BacnetExportTable", 2);
      out.prop("Exports By Object Id", "");
      Enumeration<BBacnetObjectIdentifier> ebboi = this.exportsByObjectId.keys();

      while (ebboi.hasMoreElements()) {
         BBacnetObjectIdentifier id = ebboi.nextElement();
         out.prop("  " + id, this.exportsByObjectId.get(id));
      }

      out.prop("Exports By Object Name", "");
      Enumeration<String> es = this.exportsByObjectName.keys();

      while (es.hasMoreElements()) {
         String n = es.nextElement();
         out.prop("  " + n, this.exportsByObjectName.get(n));
      }

      out.prop("ObjectList", "[] is external index");
      int len = this.objectList.size();

      for (int i = 0; i < len; i++) {
         out.prop("  " + i + " [" + (i + 2) + "]", this.objectList.get(i));
      }

      out.prop("ObjectIDs By Object Ord", "");
      synchronized (this.OBJECTS_BY_ORD_MONITOR) {
         for (BOrd o : this.objectIdsByOrd.keySet()) {
            out.prop("  " + o, this.objectIdsByOrd.get(o));
         }
      }

      out.endProps();
   }

   static class ExportObjectsFilter implements IFilter {
      public boolean accept(Object obj) {
         TypeInfo typeInfo = ((BIObject)obj).getType().getTypeInfo();
         AgentFilter filter = AgentFilter.is(BIBacnetExportObject.TYPE);
         AgentList list = Sys.getRegistry().getAgents(typeInfo).filter(filter);
         return list.size() > 0;
      }
   }

   static class ServerObjectComparator implements Comparator<Object> {
      @Override
      public int compare(Object o1, Object o2) {
         BIBacnetExportObject obj1 = (BIBacnetExportObject)o1;
         BIBacnetExportObject obj2 = (BIBacnetExportObject)o2;
         long id1 = obj1.getObjectId().hashCode();
         long id2 = obj2.getObjectId().hashCode();
         return (int)(id1 - id2);
      }
   }
}
