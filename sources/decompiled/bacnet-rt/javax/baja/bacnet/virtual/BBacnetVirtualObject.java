package javax.baja.bacnet.virtual;

import com.tridium.bacnet.job.BacnetDiscoveryUtil;
import java.io.IOException;
import java.util.HashMap;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.baja.bacnet.BBacnetDevice;
import javax.baja.bacnet.BBacnetNetwork;
import javax.baja.bacnet.BacnetConst;
import javax.baja.bacnet.datatypes.BBacnetObjectIdentifier;
import javax.baja.bacnet.point.BBacnetTuningPolicy;
import javax.baja.bacnet.point.BBacnetTuningPolicyMap;
import javax.baja.data.BIDataValue;
import javax.baja.nre.annotations.Facet;
import javax.baja.nre.annotations.NiagaraProperties;
import javax.baja.nre.annotations.NiagaraProperty;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.spy.SpyWriter;
import javax.baja.sys.BBoolean;
import javax.baja.sys.BComponent;
import javax.baja.sys.BFacets;
import javax.baja.sys.BValue;
import javax.baja.sys.Context;
import javax.baja.sys.Property;
import javax.baja.sys.SlotCursor;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;
import javax.baja.virtual.BVirtualComponent;

@NiagaraType
@NiagaraProperties({@NiagaraProperty(
      name = "objectId",
      type = "BBacnetObjectIdentifier",
      defaultValue = "BBacnetObjectIdentifier.DEFAULT",
      flags = 5
   ), @NiagaraProperty(
      name = "facets",
      type = "BFacets",
      defaultValue = "UNINITIALIZED_FACETS"
   ), @NiagaraProperty(
      name = "tuningPolicyName",
      type = "String",
      defaultValue = "defaultPolicy",
      facets = {@Facet(
         name = "BFacets.FIELD_EDITOR",
         value = "\"bacnet:VirtualTuningPolicyNameFE\""
      )}
   ), @NiagaraProperty(
      name = "writePriority",
      type = "int",
      defaultValue = "-1"
   ), @NiagaraProperty(
      name = "prioritizedPoint",
      type = "boolean",
      defaultValue = "false"
   )})
public class BBacnetVirtualObject extends BVirtualComponent implements BacnetConst {
   private static final BFacets UNINITIALIZED_FACETS = BFacets.make("initialized", BBoolean.FALSE);
   public static final Property objectId = newProperty(5, BBacnetObjectIdentifier.DEFAULT, null);
   public static final Property facets = newProperty(0, UNINITIALIZED_FACETS, null);
   public static final Property tuningPolicyName = newProperty(0, "defaultPolicy", BFacets.make("fieldEditor", "bacnet:VirtualTuningPolicyNameFE"));
   public static final Property writePriority = newProperty(0, -1, null);
   public static final Property prioritizedPoint = newProperty(0, false, null);
   public static final Type TYPE = Sys.loadType(BBacnetVirtualObject.class);
   private static Logger log = Logger.getLogger("bacnet.virtual");
   static final String POLICY_DEF = "policy=";
   static final int POLICY_DEF_LEN = 7;
   static final String PRIORITY_DEF = "priority=";
   static final int PRIORITY_DEF_LEN = 9;
   private BBacnetTuningPolicy cachedPolicy = null;

   public BBacnetObjectIdentifier getObjectId() {
      return (BBacnetObjectIdentifier)this.get(objectId);
   }

   public void setObjectId(BBacnetObjectIdentifier v) {
      this.set(objectId, v, null);
   }

   public BFacets getFacets() {
      return (BFacets)this.get(facets);
   }

   public void setFacets(BFacets v) {
      this.set(facets, v, null);
   }

   public String getTuningPolicyName() {
      return this.getString(tuningPolicyName);
   }

   public void setTuningPolicyName(String v) {
      this.setString(tuningPolicyName, v, null);
   }

   public int getWritePriority() {
      return this.getInt(writePriority);
   }

   public void setWritePriority(int v) {
      this.setInt(writePriority, v, null);
   }

   public boolean getPrioritizedPoint() {
      return this.getBoolean(prioritizedPoint);
   }

   public void setPrioritizedPoint(boolean v) {
      this.setBoolean(prioritizedPoint, v, null);
   }

   public Type getType() {
      return TYPE;
   }

   public BBacnetVirtualObject() {
   }

   public BBacnetVirtualObject(String virtualPathName) {
      try {
         StringTokenizer st = new StringTokenizer(virtualPathName, ";");
         BBacnetObjectIdentifier id = (BBacnetObjectIdentifier)BBacnetObjectIdentifier.DEFAULT.decodeFromString(st.nextToken());
         this.setObjectId(id);

         while (st.hasMoreTokens()) {
            String s = st.nextToken();
            if (s.startsWith("policy=")) {
               this.setTuningPolicyName(s.substring(7));
            } else if (s.startsWith("priority=")) {
               try {
                  int pri = Integer.parseInt(s.substring(9));
                  this.setWritePriority(pri);
               } catch (Exception var6) {
                  log.log(Level.SEVERE, "Invalid priority: " + s + " in virtualPathName for " + virtualPathName, (Throwable)var6);
               }
            }
         }
      } catch (IOException var7) {
         log.log(Level.SEVERE, "IOException occurred in BBacnetVirtualObject", (Throwable)var7);
      }
   }

   public String toString(Context cx) {
      return this.getObjectId().toString(cx);
   }

   public void started() throws Exception {
      this.discoverFacets();
   }

   public boolean isChildLegal(BComponent child) {
      return child instanceof BBacnetVirtualProperty;
   }

   public BBacnetDevice device() {
      return this.getVirtualGateway() == null ? null : ((BBacnetVirtualGateway)this.getVirtualGateway()).device();
   }

   public void updateStatus() {
      SlotCursor<Property> sc = this.getProperties();

      while (sc.next(BBacnetVirtualProperty.class)) {
         ((BBacnetVirtualProperty)sc.get()).updateStatus();
      }
   }

   public BBacnetTuningPolicy getPolicy() {
      if (this.cachedPolicy == null) {
         String tpName = this.getTuningPolicyName();
         BBacnetTuningPolicyMap map = this.getPolicyMap();
         BValue x = map.get(tpName);
         if (x instanceof BBacnetTuningPolicy) {
            this.cachedPolicy = (BBacnetTuningPolicy)x;
         } else {
            log.warning("TuningPolicy not found: " + tpName);
            this.cachedPolicy = (BBacnetTuningPolicy)map.getDefaultPolicy();
         }
      }

      return this.cachedPolicy;
   }

   protected void discoverFacets() {
      this.network()
         .postAsync(
            new Runnable() {
               @Override
               public void run() {
                  HashMap<String, BIDataValue> m = BacnetDiscoveryUtil.discoverFacets(
                     BBacnetVirtualObject.this.getObjectId(), BBacnetVirtualObject.this.device()
                  );
                  BBacnetVirtualObject.this.setFacets(BFacets.make(m));
                  int objectType = BBacnetVirtualObject.this.getObjectId().getObjectType();
                  if (objectType == 1 || objectType == 4 || objectType == 14) {
                     BBacnetVirtualObject.this.setPrioritizedPoint(true);
                  } else if (objectType == 2 || objectType == 5 || objectType == 19) {
                     BBacnetVirtualObject.this.setPrioritizedPoint(
                        BacnetDiscoveryUtil.checkForPriorityArray(BBacnetVirtualObject.this.getObjectId(), BBacnetVirtualObject.this.device()).getBoolean()
                     );
                  }
               }
            }
         );
   }

   private BBacnetNetwork network() {
      return ((BBacnetVirtualGateway)this.getVirtualGateway()).network();
   }

   private BBacnetTuningPolicyMap getPolicyMap() {
      BBacnetTuningPolicyMap map = (BBacnetTuningPolicyMap)this.network().get("tuningPolicies");
      if (map != null) {
         return map;
      } else {
         throw new IllegalStateException("Network missing tuningPolicies property");
      }
   }

   public void spy(SpyWriter out) throws Exception {
      super.spy(out);
      out.startProps();
      out.trTitle("BacnetVirtualObject", 2);
      out.prop("cachedPolicy", this.cachedPolicy);
      out.endProps();
   }
}
