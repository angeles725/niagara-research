package javax.baja.bacnet.virtual;

import com.tridium.bacnet.job.BacnetDiscoveryUtil;
import java.util.HashMap;
import java.util.logging.Logger;
import javax.baja.bacnet.BBacnetDevice;
import javax.baja.bacnet.BacnetException;
import javax.baja.bacnet.datatypes.BBacnetPropertyReference;
import javax.baja.bacnet.enums.BBacnetPropertyIdentifier;
import javax.baja.bacnet.export.BIBacnetExportObject;
import javax.baja.bacnet.io.PropertyValue;
import javax.baja.data.BIDataValue;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.sys.BComponent;
import javax.baja.sys.BFacets;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType
public class BLocalBacnetVirtualObject extends BBacnetVirtualObject {
   public static final Type TYPE = Sys.loadType(BLocalBacnetVirtualObject.class);
   private static final BBacnetPropertyReference PRIORITY_ARRAY_SIZE = new BBacnetPropertyReference(87, 0);
   private BIBacnetExportObject export;
   private static final Logger loggerBacnetVirtual = Logger.getLogger("bacnet.virtual");
   private static final Logger loggerBacnetClient = Logger.getLogger("bacnet.client");

   @Override
   public Type getType() {
      return TYPE;
   }

   public BLocalBacnetVirtualObject() {
   }

   public BLocalBacnetVirtualObject(BLocalBacnetVirtualGateway lgw, String virtualPathName) {
      super(virtualPathName);
      this.export = lgw.localDevice().lookupBacnetObject(this.getObjectId());
   }

   public BLocalBacnetVirtualObject(BIBacnetExportObject o) {
      this.export = o;
      if (o != null) {
         this.setObjectId(o.getObjectId());
      }
   }

   @Override
   public boolean isChildLegal(BComponent child) {
      return child instanceof BLocalBacnetVirtualProperty;
   }

   @Override
   public BBacnetDevice device() {
      throw new UnsupportedOperationException("Not supported for LocalBacnetVirtualObject");
   }

   @Override
   protected void discoverFacets() {
      HashMap<String, BIDataValue> map = new HashMap<>();
      if (this.getObjectId().isValid()) {
         int[] facetProps = BacnetDiscoveryUtil.getFacetProps(this.getObjectId().getObjectType());
         if (facetProps != null) {
            for (int i = 0; i < facetProps.length; i++) {
               try {
                  PropertyValue pv = this.export.readProperty(new BBacnetPropertyReference(facetProps[i]));
                  if (!pv.isError()) {
                     BacnetDiscoveryUtil.addFacet(facetProps[i], pv.getPropertyValue(), map, null);
                  }
               } catch (BacnetException var7) {
                  loggerBacnetClient.info("BacnetException reading property " + BBacnetPropertyIdentifier.tag(facetProps[i]) + " in " + objectId + ": " + var7);
               } catch (Exception var8) {
                  loggerBacnetClient.info("Exception reading property " + BBacnetPropertyIdentifier.tag(facetProps[i]) + " in " + objectId + ": " + var8);
               }
            }
         }
      }

      this.setFacets(BFacets.make(map));
      int objectType = this.getObjectId().getObjectType();
      if (objectType == 1 || objectType == 4 || objectType == 14) {
         this.setPrioritizedPoint(true);
      } else if (objectType == 2 || objectType == 5 || objectType == 19) {
         try {
            PropertyValue pv = this.export.readProperty(PRIORITY_ARRAY_SIZE);
            if (!pv.isError()) {
               this.setPrioritizedPoint(true);
            }
         } catch (BacnetException var5) {
            loggerBacnetVirtual.info("BacnetException reading priorityArray size in " + this.getObjectId() + ": " + var5);
         } catch (Exception var6) {
            loggerBacnetVirtual.info("Exception reading priorityArray size in " + this.getObjectId() + ": " + var6);
         }
      }
   }

   BIBacnetExportObject getExport() {
      return this.export;
   }
}
