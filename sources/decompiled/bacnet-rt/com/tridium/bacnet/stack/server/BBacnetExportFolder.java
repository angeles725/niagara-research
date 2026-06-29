package com.tridium.bacnet.stack.server;

import com.tridium.bacnet.BacUtil;
import com.tridium.bacnet.asn.AsnOutputStream;
import com.tridium.bacnet.asn.AsnUtil;
import com.tridium.bacnet.asn.NErrorType;
import com.tridium.bacnet.asn.NReadPropertyResult;
import com.tridium.bacnet.datatypes.BSvoSubordinate;
import com.tridium.bacnet.services.confirmed.ReadRangeAck;
import com.tridium.bacnet.services.error.NChangeListError;
import com.tridium.util.PxUtil;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.baja.agent.AgentList;
import javax.baja.agent.BIAgent;
import javax.baja.bacnet.BBacnetNetwork;
import javax.baja.bacnet.BacnetException;
import javax.baja.bacnet.datatypes.BBacnetArray;
import javax.baja.bacnet.datatypes.BBacnetObjectIdentifier;
import javax.baja.bacnet.enums.BBacnetNodeType;
import javax.baja.bacnet.enums.BBacnetObjectType;
import javax.baja.bacnet.export.BIBacnetExportObject;
import javax.baja.bacnet.export.BLocalBacnetDevice;
import javax.baja.bacnet.export.BacnetPropertyList;
import javax.baja.bacnet.export.BacnetPropertyListProvider;
import javax.baja.bacnet.io.AsnException;
import javax.baja.bacnet.io.ChangeListError;
import javax.baja.bacnet.io.ErrorType;
import javax.baja.bacnet.io.PropertyReference;
import javax.baja.bacnet.io.PropertyValue;
import javax.baja.bacnet.io.RangeData;
import javax.baja.bacnet.io.RangeReference;
import javax.baja.bacnet.io.RejectException;
import javax.baja.naming.BOrd;
import javax.baja.nre.annotations.NiagaraProperties;
import javax.baja.nre.annotations.NiagaraProperty;
import javax.baja.nre.annotations.NiagaraTopic;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.security.PermissionException;
import javax.baja.spy.SpyWriter;
import javax.baja.status.BStatus;
import javax.baja.sys.BComplex;
import javax.baja.sys.BComponent;
import javax.baja.sys.BFacets;
import javax.baja.sys.BIcon;
import javax.baja.sys.BInteger;
import javax.baja.sys.BObject;
import javax.baja.sys.BString;
import javax.baja.sys.BValue;
import javax.baja.sys.Context;
import javax.baja.sys.DuplicateSlotException;
import javax.baja.sys.Property;
import javax.baja.sys.Slot;
import javax.baja.sys.SlotCursor;
import javax.baja.sys.Sys;
import javax.baja.sys.Topic;
import javax.baja.sys.Type;
import javax.baja.util.BFolder;
import javax.baja.util.Lexicon;

@NiagaraType
@NiagaraProperties({@NiagaraProperty(
      name = "status",
      type = "BStatus",
      defaultValue = "BStatus.ok",
      flags = 67
   ), @NiagaraProperty(
      name = "faultCause",
      type = "String",
      defaultValue = "",
      flags = 3
   ), @NiagaraProperty(
      name = "objectId",
      type = "BBacnetObjectIdentifier",
      defaultValue = "BBacnetObjectIdentifier.make(BBacnetObjectType.STRUCTURED_VIEW)",
      flags = 64
   ), @NiagaraProperty(
      name = "objectName",
      type = "String",
      defaultValue = "",
      flags = 64
   ), @NiagaraProperty(
      name = "description",
      type = "String",
      defaultValue = ""
   ), @NiagaraProperty(
      name = "nodeType",
      type = "BBacnetNodeType",
      defaultValue = "BBacnetNodeType.unknown"
   ), @NiagaraProperty(
      name = "nodeSubtype",
      type = "String",
      defaultValue = ""
   ), @NiagaraProperty(
      name = "subordinates",
      type = "BBacnetArray",
      defaultValue = "new BBacnetArray(BSvoSubordinate.TYPE)"
   )})
@NiagaraTopic(
   name = "subordinateAnnotationChanged"
)
public class BBacnetExportFolder extends BFolder implements BIAgent, BIBacnetExportObject, BIBacnetExportFolder, BacnetPropertyListProvider {
   public static final Property status = newProperty(67, BStatus.ok, null);
   public static final Property faultCause = newProperty(3, "", null);
   public static final Property objectId = newProperty(64, BBacnetObjectIdentifier.make(29), null);
   public static final Property objectName = newProperty(64, "", null);
   public static final Property description = newProperty(0, "", null);
   public static final Property nodeType = newProperty(0, BBacnetNodeType.unknown, null);
   public static final Property nodeSubtype = newProperty(0, "", null);
   public static final Property subordinates = newProperty(0, new BBacnetArray(BSvoSubordinate.TYPE), null);
   public static final Topic subordinateAnnotationChanged = newTopic(0, null);
   public static final Type TYPE = Sys.loadType(BBacnetExportFolder.class);
   private boolean fatalFault = false;
   private static final BIcon icon = BIcon.make(BIcon.std("folder.png"), BIcon.std("badges/export.png"));
   private BBacnetObjectIdentifier oldId = null;
   private String oldName = null;
   private boolean duplicate = false;
   private static final AsnOutputStream asnOut = new AsnOutputStream();
   private static final Logger logger = Logger.getLogger("bacnet.server");
   private static final Lexicon lex = Lexicon.make("bacnet");
   private static final int[] REQUIRED_PROPS = new int[]{75, 77, 79, 208, 211};
   private static final int[] OPTIONAL_PROPS = new int[]{28, 207, 210};

   @Override
   public BStatus getStatus() {
      return (BStatus)this.get(status);
   }

   public void setStatus(BStatus v) {
      this.set(status, v, null);
   }

   public String getFaultCause() {
      return this.getString(faultCause);
   }

   public void setFaultCause(String v) {
      this.setString(faultCause, v, null);
   }

   @Override
   public BBacnetObjectIdentifier getObjectId() {
      return (BBacnetObjectIdentifier)this.get(objectId);
   }

   @Override
   public void setObjectId(BBacnetObjectIdentifier v) {
      this.set(objectId, v, null);
   }

   @Override
   public String getObjectName() {
      return this.getString(objectName);
   }

   @Override
   public void setObjectName(String v) {
      this.setString(objectName, v, null);
   }

   public String getDescription() {
      return this.getString(description);
   }

   public void setDescription(String v) {
      this.setString(description, v, null);
   }

   public BBacnetNodeType getNodeType() {
      return (BBacnetNodeType)this.get(nodeType);
   }

   public void setNodeType(BBacnetNodeType v) {
      this.set(nodeType, v, null);
   }

   public String getNodeSubtype() {
      return this.getString(nodeSubtype);
   }

   public void setNodeSubtype(String v) {
      this.setString(nodeSubtype, v, null);
   }

   public BBacnetArray getSubordinates() {
      return (BBacnetArray)this.get(subordinates);
   }

   public void setSubordinates(BBacnetArray v) {
      this.set(subordinates, v, null);
   }

   public void fireSubordinateAnnotationChanged(BValue event) {
      this.fire(subordinateAnnotationChanged, event, null);
   }

   public Type getType() {
      return TYPE;
   }

   @Override
   public BBacnetExportTable getExports() {
      for (BComplex p = this.getParent(); p != null; p = p.getParent()) {
         if (p instanceof BBacnetExportTable) {
            return (BBacnetExportTable)p;
         }
      }

      throw new IllegalStateException();
   }

   public final void started() throws Exception {
      super.started();
      this.checkFatalFault();
      this.oldId = this.getObjectId();
      this.oldName = this.getObjectName();
      this.checkConfiguration();
      if (Sys.isStationStarted()) {
         BBacnetNetwork.localDevice().incrementDatabaseRevision();
      }
   }

   public final void stopped() throws Exception {
      super.stopped();
      BLocalBacnetDevice local = BBacnetNetwork.localDevice();
      local.unexport(this.oldId, this.oldName, this);
      this.oldId = null;
      this.oldName = null;
      if (local.isRunning()) {
         local.incrementDatabaseRevision();
      }
   }

   public final void changed(Property p, Context cx) {
      super.changed(p, cx);
      if (this.isRunning()) {
         if (p.equals(objectId)) {
            BBacnetNetwork.localDevice().unexport(this.oldId, this.oldName, this);
            this.checkConfiguration();
            this.oldId = this.getObjectId();

            try {
               ((BComponent)this.getParent()).rename(this.getPropertyInParent(), this.getObjectId().toString(nameContext));
            } catch (DuplicateSlotException var4) {
               logger.log(Level.SEVERE, "DuplicateSlotException in changed", (Throwable)var4);
            }

            if (this.getStatus().isOk()) {
               BBacnetNetwork.localDevice().incrementDatabaseRevision();
            }
         } else if (p.equals(objectName)) {
            BBacnetNetwork.localDevice().unexport(this.oldId, this.oldName, this);
            this.checkConfiguration();
            this.oldName = this.getObjectName();
            if (this.getStatus().isOk()) {
               BBacnetNetwork.localDevice().incrementDatabaseRevision();
            }
         }
      }
   }

   public final BFacets getSlotFacets(Slot s) {
      return s == objectId ? BBacnetObjectType.getObjectIdFacets(29) : super.getSlotFacets(s);
   }

   @Override
   public final BObject getObject() {
      return this;
   }

   @Override
   public final BOrd getObjectOrd() {
      return this.getOrdInSession();
   }

   @Override
   public final void setObjectOrd(BOrd objectOrd, Context cx) {
      throw new UnsupportedOperationException(lex.getText("UnsupportedOperationException.structuredView.setObjectOrd"));
   }

   @Override
   public void checkConfiguration() {
      BLocalBacnetDevice local = BBacnetNetwork.localDevice();
      if (this.isFatalFault()) {
         this.setStatus(BStatus.makeFault(this.getStatus(), true));
      } else {
         boolean configOk = true;
         if (!this.getObjectId().isValid()) {
            this.setFaultCause("Invalid Object ID");
            configOk = false;
         }

         if (configOk) {
            String err = local.export(this);
            if (err != null) {
               this.duplicate = true;
               this.setFaultCause(err);
               configOk = false;
            } else {
               this.duplicate = false;
            }
         }

         if (configOk) {
            this.setFaultCause("");
         }

         this.setStatus(BStatus.makeFault(this.getStatus(), !configOk));
      }
   }

   @Override
   public final PropertyValue readProperty(PropertyReference ref) throws RejectException {
      return this.readProperty(ref.getPropertyId(), ref.getPropertyArrayIndex());
   }

   @Override
   public final PropertyValue[] readPropertyMultiple(PropertyReference[] refs) throws RejectException {
      ArrayList<PropertyValue> results = new ArrayList<>(refs.length);

      for (int i = 0; i < refs.length; i++) {
         switch (refs[i].getPropertyId()) {
            case 8:
               int[] props = REQUIRED_PROPS;

               for (int j = 0; j < props.length; j++) {
                  results.add(this.readProperty(props[j], -1));
               }

               props = OPTIONAL_PROPS;

               for (int j = 0; j < props.length; j++) {
                  results.add(this.readProperty(props[j], -1));
               }
               break;
            case 80:
               int[] props = OPTIONAL_PROPS;

               for (int j = 0; j < props.length; j++) {
                  results.add(this.readProperty(props[j], -1));
               }
               break;
            case 105:
               int[] props = REQUIRED_PROPS;

               for (int j = 0; j < props.length; j++) {
                  results.add(this.readProperty(props[j], -1));
               }
               break;
            default:
               results.add(this.readProperty(refs[i].getPropertyId(), refs[i].getPropertyArrayIndex()));
         }
      }

      return results.toArray(new PropertyValue[0]);
   }

   @Override
   public final RangeData readRange(RangeReference rangeReference) throws RejectException {
      return new ReadRangeAck(2, 45);
   }

   @Override
   public final ErrorType writeProperty(PropertyValue val) throws BacnetException {
      return this.writeProperty(val.getPropertyId(), val.getPropertyArrayIndex(), val.getPropertyValue(), val.getPriority());
   }

   @Override
   public final ChangeListError addListElements(PropertyValue propertyValue) throws BacnetException {
      int propertyId = propertyValue.getPropertyId();

      for (int i = 0; i < REQUIRED_PROPS.length; i++) {
         if (propertyId == REQUIRED_PROPS[i]) {
            return new NChangeListError(8, new NErrorType(5, 22), 0L);
         }
      }

      for (int ix = 0; ix < OPTIONAL_PROPS.length; ix++) {
         if (propertyId == OPTIONAL_PROPS[ix]) {
            return new NChangeListError(8, new NErrorType(5, 22), 0L);
         }
      }

      return new NChangeListError(8, new NErrorType(2, 32), 0L);
   }

   @Override
   public final ChangeListError removeListElements(PropertyValue propertyValue) throws BacnetException {
      int propertyId = propertyValue.getPropertyId();

      for (int i = 0; i < REQUIRED_PROPS.length; i++) {
         if (propertyId == REQUIRED_PROPS[i]) {
            return new NChangeListError(9, new NErrorType(5, 22), 0L);
         }
      }

      for (int ix = 0; ix < OPTIONAL_PROPS.length; ix++) {
         if (propertyId == OPTIONAL_PROPS[ix]) {
            return new NChangeListError(9, new NErrorType(5, 22), 0L);
         }
      }

      return new NChangeListError(9, new NErrorType(2, 32), 0L);
   }

   protected PropertyValue readProperty(int pId, int ndx) {
      if (!this.isArray(pId) && ndx >= 0) {
         return new NReadPropertyResult(pId, ndx, new NErrorType(2, 50));
      } else {
         switch (pId) {
            case 28:
               return new NReadPropertyResult(pId, ndx, AsnUtil.toAsnCharacterString(this.getDescription()));
            case 75:
               return new NReadPropertyResult(pId, ndx, AsnUtil.toAsnObjectId(this.getObjectId()));
            case 77:
               return new NReadPropertyResult(pId, ndx, AsnUtil.toAsnCharacterString(this.getObjectName()));
            case 79:
               return new NReadPropertyResult(pId, ndx, AsnUtil.toAsnEnumerated(this.getObjectId().getObjectType()));
            case 207:
               return new NReadPropertyResult(pId, ndx, AsnUtil.toAsnCharacterString(this.getNodeSubtype()));
            case 208:
               return new NReadPropertyResult(pId, ndx, AsnUtil.toAsnEnumerated(this.getNodeType()));
            case 210:
               return this.readSubordinateAnnotations(ndx);
            case 211:
               return this.readSubordinateList(ndx);
            case 371:
               return this.readPropertyList(ndx);
            default:
               return new NReadPropertyResult(pId, ndx, new NErrorType(2, 32));
         }
      }
   }

   protected ErrorType writeProperty(int pId, int ndx, byte[] val, int pri) throws BacnetException {
      if (ndx >= 0 && !this.isArray(pId)) {
         return new NErrorType(2, 50);
      } else {
         try {
            switch (pId) {
               case 28:
                  this.setString(description, AsnUtil.fromAsnCharacterString(val), BLocalBacnetDevice.getBacnetContext());
                  return null;
               case 75:
                  return new NErrorType(2, 40);
               case 77:
                  return BacUtil.setObjectName(this, objectName, val);
               case 79:
                  return new NErrorType(2, 40);
               case 207:
                  return new NErrorType(2, 40);
               case 208:
                  return new NErrorType(2, 40);
               case 210:
                  return new NErrorType(2, 40);
               case 211:
                  return new NErrorType(2, 40);
               case 371:
                  return new NErrorType(2, 40);
               default:
                  return new NErrorType(2, 32);
            }
         } catch (AsnException var6) {
            logger.warning("AsnException writing property " + pId + " in object " + this.getObjectId() + ": " + var6);
            return new NErrorType(2, 9);
         } catch (PermissionException var7) {
            logger.warning("PermissionException writing property " + pId + " in object " + this.getObjectId() + ": " + var7);
            return new NErrorType(2, 40);
         }
      }
   }

   public String toString(Context c) {
      return this.getObjectName() + " [" + this.getObjectId() + "]";
   }

   boolean isArray(int propertyId) {
      if (propertyId == 210) {
         return true;
      } else {
         return propertyId == 211 ? true : propertyId == 371;
      }
   }

   public boolean addSubordinate(BSvoSubordinate sub) {
      try {
         this.getSubordinates().addElement(sub);
         return true;
      } catch (Exception var3) {
         logger.log(Level.SEVERE, "Exception in addSubordinate", (Throwable)var3);
         return false;
      }
   }

   public boolean modifySubordinate(int index, BSvoSubordinate sub) {
      try {
         this.getSubordinates().setElement(index, sub);
         return true;
      } catch (Exception var4) {
         logger.log(Level.SEVERE, "Exception in modifySubordinate", (Throwable)var4);
         return false;
      }
   }

   public boolean removeSubordinate(int index) {
      try {
         this.getSubordinates().removeElement(BInteger.make(index));
         return true;
      } catch (Exception var3) {
         logger.log(Level.SEVERE, "Exception in removeSubordinate", (Throwable)var3);
         return false;
      }
   }

   public int countLocalSubordinates() {
      SlotCursor<Property> c = this.getProperties();
      int count = 0;

      while (c.next(BIBacnetExportObject.class)) {
         count++;
      }

      return count;
   }

   public BIBacnetExportObject[] getLocalSubordinates() {
      return (BIBacnetExportObject[])this.getChildren(BIBacnetExportObject.class);
   }

   private NReadPropertyResult readSubordinateList(int ndx) {
      if (ndx == 0) {
         int exportCount = this.countLocalSubordinates();
         return new NReadPropertyResult(211, ndx, AsnUtil.toAsnUnsigned(exportCount + this.getSubordinates().getSize()));
      } else if (ndx == -1) {
         synchronized (asnOut) {
            asnOut.reset();
            BIBacnetExportObject[] localSubs = this.getLocalSubordinates();

            for (int i = 0; i < localSubs.length; i++) {
               asnOut.writeObjectIdentifier(1, localSubs[i].getObjectId());
            }

            BSvoSubordinate[] subs = (BSvoSubordinate[])this.getSubordinates().getChildren(BSvoSubordinate.class);

            for (int i = 0; i < subs.length; i++) {
               subs[i].getReference().writeAsn(asnOut);
            }

            return new NReadPropertyResult(211, ndx, asnOut.toByteArray());
         }
      } else {
         int exportCount = this.countLocalSubordinates();
         if (ndx < 0 || ndx > exportCount + this.getSubordinates().getSize()) {
            return new NReadPropertyResult(211, ndx, new NErrorType(2, 42));
         } else if (ndx <= exportCount) {
            BIBacnetExportObject[] localSubs = this.getLocalSubordinates();
            synchronized (asnOut) {
               asnOut.reset();
               asnOut.writeObjectIdentifier(1, localSubs[ndx - 1].getObjectId());
               return new NReadPropertyResult(211, ndx, asnOut.toByteArray());
            }
         } else {
            return new NReadPropertyResult(211, ndx, AsnUtil.toAsn(((BSvoSubordinate)this.getSubordinates().getElement(ndx - exportCount)).getReference()));
         }
      }
   }

   private NReadPropertyResult readSubordinateAnnotations(int ndx) {
      if (ndx == 0) {
         int exportCount = this.countLocalSubordinates();
         return new NReadPropertyResult(210, ndx, AsnUtil.toAsnUnsigned(exportCount + this.getSubordinates().getSize()));
      } else if (ndx == -1) {
         synchronized (asnOut) {
            asnOut.reset();
            BIBacnetExportObject[] localSubs = this.getLocalSubordinates();

            for (int i = 0; i < localSubs.length; i++) {
               asnOut.writeCharacterString((BString)((BComplex)localSubs[i]).get("description"));
            }

            BSvoSubordinate[] subs = (BSvoSubordinate[])this.getSubordinates().getChildren(BSvoSubordinate.class);

            for (int i = 0; i < subs.length; i++) {
               asnOut.writeCharacterString(subs[i].getAnnotation());
            }

            return new NReadPropertyResult(210, ndx, asnOut.toByteArray());
         }
      } else {
         int exportCount = this.countLocalSubordinates();
         if (ndx < 0 || ndx > exportCount + this.getSubordinates().getSize()) {
            return new NReadPropertyResult(210, ndx, new NErrorType(2, 42));
         } else if (ndx <= exportCount) {
            BIBacnetExportObject[] localSubs = this.getLocalSubordinates();
            synchronized (asnOut) {
               asnOut.reset();
               asnOut.writeCharacterString((BString)((BComplex)localSubs[ndx - 1]).get("description"));
               return new NReadPropertyResult(210, ndx, asnOut.toByteArray());
            }
         } else {
            return new NReadPropertyResult(
               210, ndx, AsnUtil.toAsnCharacterString(((BSvoSubordinate)this.getSubordinates().getElement(ndx - exportCount)).getAnnotation())
            );
         }
      }
   }

   @Override
   public final boolean isFatalFault() {
      return this.fatalFault;
   }

   private void checkFatalFault() {
      BBacnetExportTable exports = null;
      BLocalBacnetDevice local = null;
      BBacnetNetwork network = null;
      if (!this.fatalFault) {
         for (BComplex parent = this.getParent(); parent != null; parent = parent.getParent()) {
            if (parent instanceof BBacnetExportTable) {
               exports = (BBacnetExportTable)parent;
            } else if (parent instanceof BLocalBacnetDevice) {
               local = (BLocalBacnetDevice)parent;
               break;
            }
         }

         if (exports == null || local == null) {
            this.fatalFault = true;
            this.setFaultCause("Not under LocalBacnetDevice Export Table");
         } else if (local.isFatalFault()) {
            this.fatalFault = true;
            this.setFaultCause("LocalDevice fault: " + local.getFaultCause());
         } else {
            network = (BBacnetNetwork)local.getParent();
            if (network == null) {
               this.fatalFault = true;
               this.setFaultCause("Not under BacnetNetwork");
            } else if (network.isFatalFault()) {
               this.fatalFault = true;
               this.setFaultCause("Network fault: " + network.getFaultCause());
            } else if (!network.hasServerLicense()) {
               this.fatalFault = true;
               this.setFaultCause("Server capability not licensed");
            } else {
               this.setFaultCause("");
            }
         }
      }
   }

   public void spy(SpyWriter out) throws Exception {
      super.spy(out);
      out.startProps();
      out.trTitle("BacnetStructuredViewDescriptor", 2);
      out.prop("fatalFault", this.fatalFault);
      out.prop("oldId", this.oldId);
      out.prop("oldName", this.oldName);
      out.prop("duplicate", this.duplicate);
      out.endProps();
   }

   public BIcon getIcon() {
      return icon;
   }

   public AgentList getAgents(Context cx) {
      AgentList agents = super.getAgents(cx);
      agents.toTop("bacnet:BacnetExportManager");
      return PxUtil.movePxViewsToTop(agents);
   }

   @Override
   public int[] getPropertyList() {
      return BacnetPropertyList.makePropertyList(REQUIRED_PROPS, OPTIONAL_PROPS);
   }
}
