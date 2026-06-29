package com.tridium.bacnet.job;

import com.tridium.bacnet.asn.AsnUtil;
import com.tridium.bacnet.asn.NBacnetPropertyReference;
import com.tridium.bacnet.asn.NReadAccessResult;
import com.tridium.bacnet.asn.NReadAccessSpec;
import com.tridium.bacnet.asn.NReadPropertyResult;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Vector;
import java.util.logging.Level;
import javax.baja.bacnet.BacnetException;
import javax.baja.bacnet.datatypes.BBacnetObjectIdentifier;
import javax.baja.bacnet.enums.BBacnetPropertyIdentifier;
import javax.baja.bacnet.io.AsnException;
import javax.baja.bacnet.point.BBacnetPointDeviceExt;
import javax.baja.bacnet.util.PropertyInfo;
import javax.baja.data.BIDataValue;
import javax.baja.nre.annotations.NiagaraAction;
import javax.baja.nre.annotations.NiagaraActions;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.nre.util.ByteArrayUtil;
import javax.baja.sys.Action;
import javax.baja.sys.BBoolean;
import javax.baja.sys.BEnum;
import javax.baja.sys.BEnumRange;
import javax.baja.sys.BFacets;
import javax.baja.sys.BString;
import javax.baja.sys.BValue;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;
import javax.baja.util.BTypeSpec;

@NiagaraType
@NiagaraActions({@NiagaraAction(
      name = "discover",
      parameterType = "BDiscoveryPoint",
      defaultValue = "BDiscoveryPoint.NULL",
      returnType = "BDiscoveryPointTable"
   ), @NiagaraAction(
      name = "discoverFacets",
      parameterType = "BBacnetObjectIdentifier",
      defaultValue = "BBacnetObjectIdentifier.DEFAULT",
      returnType = "BFacets"
   ), @NiagaraAction(
      name = "checkForPriorityArray",
      parameterType = "BBacnetObjectIdentifier",
      defaultValue = "BBacnetObjectIdentifier.DEFAULT",
      returnType = "BBoolean"
   )})
public class BBacnetDiscoverPointsJob extends BBacnetDiscoverJob {
   public static final Action discover = newAction(0, BDiscoveryPoint.NULL, null);
   public static final Action discoverFacets = newAction(0, BBacnetObjectIdentifier.DEFAULT, null);
   public static final Action checkForPriorityArray = newAction(0, BBacnetObjectIdentifier.DEFAULT, null);
   public static final Type TYPE = Sys.loadType(BBacnetDiscoverPointsJob.class);
   private long pointsAdded = 0L;

   public BDiscoveryPointTable discover(BDiscoveryPoint parameter) {
      return (BDiscoveryPointTable)this.invoke(discover, parameter, null);
   }

   public BFacets discoverFacets(BBacnetObjectIdentifier parameter) {
      return (BFacets)this.invoke(discoverFacets, parameter, null);
   }

   public BBoolean checkForPriorityArray(BBacnetObjectIdentifier parameter) {
      return (BBoolean)this.invoke(checkForPriorityArray, parameter, null);
   }

   @Override
   public Type getType() {
      return TYPE;
   }

   public BBacnetDiscoverPointsJob() {
   }

   public BBacnetDiscoverPointsJob(BBacnetPointDeviceExt deviceExt) {
      super(deviceExt);
   }

   public BDiscoveryPointTable doDiscover(BDiscoveryPoint point) {
      BDiscoveryPoint[] propList = new BDiscoveryPoint[0];

      try {
         switch (point.getPointType()) {
            case 0:
               HashMap<String, BIDataValue> facetMap = new HashMap<>();
               propList = this.discoverProperties(point.getObjectName(), point.getObjectId(), facetMap);
               BFacets facets = BFacets.make(facetMap);

               for (int i = 0; i < propList.length; i++) {
                  PropertyInfo info = this.device().getPropertyInfo(propList[i].getObjectId().getObjectType(), propList[i].getPropertyId());
                  if (info != null && !info.getFacetControl().equals("no")) {
                     propList[i].setFacets(facets);
                  }
               }
            case 1:
            case 3:
            default:
               break;
            case 2:
               propList = this.discoverElements(point);
         }
      } catch (Exception var7) {
         this.log().failed(" ERROR: Unable to learn Bacnet properties of " + point.getObjectId() + ": " + var7);
         logger.log(Level.SEVERE, "Unable to learn Bacnet properties of " + point.getObjectId() + ": " + var7, (Throwable)var7);
      }

      return this.makeDiscoveryPointTable(propList);
   }

   public BFacets doDiscoverFacets(BBacnetObjectIdentifier objectId) {
      HashMap<String, BIDataValue> map = BacnetDiscoveryUtil.discoverFacets(objectId, this.device());
      switch (objectId.getObjectType()) {
         case 2:
         case 5:
         case 19:
            map.put("priPV", BacnetDiscoveryUtil.checkForPriorityArray(objectId, this.device()));
         default:
            return BFacets.make(map);
      }
   }

   public BBoolean doCheckForPriorityArray(BBacnetObjectIdentifier objectId) {
      return BacnetDiscoveryUtil.checkForPriorityArray(objectId, this.device());
   }

   @Override
   void addDiscoveryChild(BBacnetDiscoverJob.IdVals iv) {
      BBacnetDiscoverJob.PropVal primary = iv.primary();
      if (primary == null) {
         if (logger.isLoggable(Level.FINE)) {
            logger.fine("No primary value for discovered object " + iv);
         }
      } else {
         BDiscoveryPoint dp = new BDiscoveryPoint(iv.name, iv.id, BBacnetPropertyIdentifier.tag(primary.propId), primary.val, null, true);
         BBacnetDiscoverJob.PropVal desc = iv.get(28);
         if (desc != null) {
            dp.setDescription(desc.toString());
         }

         this.add("dc" + this.pointsAdded++, dp);
      }
   }

   protected BDiscoveryPointTable makeDiscoveryPointTable(BDiscoveryPoint[] pointList) {
      BDiscoveryPointTable table = new BDiscoveryPointTable();
      if (pointList != null) {
         for (int i = 0; i < pointList.length; i++) {
            table.add(null, pointList[i].newCopy());
         }
      }

      return table;
   }

   BDiscoveryPoint[] discoverProperties(String objectName, BBacnetObjectIdentifier objectId, HashMap<String, BIDataValue> facetMap) {
      if (!this.device().isServiceSupported("readPropertyMultiple")) {
         return this.buildPropertyChildren(objectName, objectId, this.device().getRequiredProperties(objectId), facetMap);
      } else {
         Vector specs = new Vector();
         specs.add(new NReadAccessSpec(objectId, 8));

         Vector vals;
         try {
            vals = client().readPropertyMultiple(this.device().getAddress(), specs);
         } catch (BacnetException var12) {
            logger.info("BacnetException reading properties for " + objectId + ": " + var12);
            return this.buildPropertyChildren(objectName, objectId, this.device().getRequiredProperties(objectId), facetMap);
         }

         if (vals == null) {
            return this.buildPropertyChildren(objectName, objectId, this.device().getRequiredProperties(objectId), facetMap);
         } else {
            Iterator it = ((NReadAccessResult)vals.elementAt(0)).getResults();
            int objectType = objectId.getObjectType();
            PropertyInfo propInfo = null;
            ArrayList<BDiscoveryPoint> results = new ArrayList<>();

            while (it.hasNext()) {
               NReadPropertyResult rpr = (NReadPropertyResult)it.next();
               if (rpr.getPropertyId() == 8) {
                  return this.buildPropertyChildren(objectName, objectId, this.device().getRequiredProperties(objectId), facetMap);
               }

               propInfo = this.device().getPropertyInfo(objectType, rpr.getPropertyId());
               if (rpr.isError()) {
                  BDiscoveryPoint prop = new BDiscoveryPoint(
                     objectName,
                     objectId,
                     BBacnetPropertyIdentifier.tag(rpr.getPropertyId()),
                     BString.make(lex.get("pointManager.error") + ":" + rpr.getPropertyAccessError().toString()),
                     null,
                     false
                  );
                  results.add(prop);
               } else if (propInfo != null) {
                  BDiscoveryPoint prop = this.buildPropertyResult(objectName, objectId, propInfo, rpr.getPropertyValue(), facetMap);
                  if (prop != null) {
                     results.add(prop);
                  }
               } else {
                  BDiscoveryPoint prop = this.buildPropertyResult(objectName, objectId, rpr.getPropertyId(), rpr.getPropertyValue());
                  if (prop != null) {
                     results.add(prop);
                  }
               }
            }

            return results.toArray(new BDiscoveryPoint[0]);
         }
      }
   }

   private BDiscoveryPoint buildPropertyResult(String objectName, BBacnetObjectIdentifier objectId, int propertyId, byte[] propertyValue) {
      BDiscoveryPoint plr = null;
      String property = BBacnetPropertyIdentifier.tag(propertyId);

      try {
         BValue[] propVals = AsnUtil.fromAsn(propertyValue);
         if (propVals.length == 1) {
            plr = new BDiscoveryPoint(objectName, objectId, property, propVals[0], null, false);
         } else {
            Type t = propVals[0].getType();
            boolean array = true;

            for (int i = 1; i < propVals.length; i++) {
               if (propVals[i].getType() != t) {
                  array = false;
                  break;
               }
            }

            if (array) {
               BDiscoveryPoint[] kids = new BDiscoveryPoint[propVals.length];

               for (int ix = 1; ix <= propVals.length; ix++) {
                  BDiscoveryPoint kid = new BDiscoveryPoint(objectName, objectId, property, ix, BString.make(""), null);
                  kids[ix - 1] = kid;
               }

               plr = new BDiscoveryPoint(objectName, objectId, property, kids, null);
            } else {
               plr = new BDiscoveryPoint(objectName, objectId, property, BString.make(""), null, false);
            }
         }
      } catch (Exception var13) {
         logger.log(
            Level.INFO,
            "Exception in buildPropertyResult for "
               + objectName
               + " ["
               + objectId
               + "] pId:"
               + propertyId
               + " val="
               + ByteArrayUtil.toHexString(propertyValue)
               + ":"
               + var13,
            (Throwable)var13
         );
         plr = new BDiscoveryPoint(objectName, objectId, property, BString.make("???"), null, false);
      }

      return plr;
   }

   private BDiscoveryPoint buildPropertyResult(
      String objectName, BBacnetObjectIdentifier objectId, PropertyInfo info, byte[] propertyValue, HashMap<String, BIDataValue> facetMap
   ) {
      try {
         BDiscoveryPoint plr = null;
         if (info.isArray()) {
            int size = info.getSize();
            if (size < 0) {
               try {
                  byte[] encodedSize = client().readProperty(this.device().getAddress(), objectId, info.getId(), 0);
                  int arraySize = AsnUtil.fromAsnInteger(encodedSize);
                  BDiscoveryPoint[] kids = new BDiscoveryPoint[arraySize];

                  for (int i = 1; i <= arraySize; i++) {
                     BDiscoveryPoint kid = new BDiscoveryPoint(objectName, objectId, info.getName(), i, BString.make(""), null);
                     kids[i - 1] = kid;
                  }

                  plr = new BDiscoveryPoint(objectName, objectId, info.getName(), kids, null);
               } catch (BacnetException var15) {
                  logger.info("Unable to read array size for " + objectName + " [" + objectId + "] " + info.getName() + ": " + var15);
               }
            } else {
               BDiscoveryPoint[] kids = new BDiscoveryPoint[size];

               for (int i = 1; i <= size; i++) {
                  BDiscoveryPoint kid = new BDiscoveryPoint(objectName, objectId, info.getName(), i, BString.make(""), null);
                  kids[i - 1] = kid;
               }

               plr = new BDiscoveryPoint(objectName, objectId, info.getName(), kids, null);
            }
         } else {
            try {
               BValue v = AsnUtil.asnToValue(info, propertyValue);
               BFacets facets = null;
               if (info.isEnum()) {
                  try {
                     BTypeSpec tspec = BTypeSpec.make(info.getType());
                     BEnum en = (BEnum)tspec.getInstance();
                     if (info.isExtensible()) {
                        facets = BFacets.makeEnum(BEnumRange.make(en.getType()));
                     } else {
                        facets = BFacets.makeEnum(en.getRange());
                     }
                  } catch (Exception var13) {
                     logger.log(
                        Level.SEVERE,
                        "Exception in buildPropertyResult for "
                           + objectName
                           + " ["
                           + objectId
                           + "] pInfo:"
                           + info
                           + " val="
                           + ByteArrayUtil.toHexString(propertyValue)
                           + " f="
                           + facetMap
                           + ":",
                        (Throwable)var13
                     );
                  }
               }

               plr = new BDiscoveryPoint(objectName, objectId, info.getName(), v, facets, false);
            } catch (AsnException var14) {
               logger.log(
                  Level.SEVERE,
                  "AsnException in buildPropertyResult for "
                     + objectName
                     + " ["
                     + objectId
                     + "] pInfo:"
                     + info
                     + " val="
                     + ByteArrayUtil.toHexString(propertyValue)
                     + " f="
                     + facetMap
                     + ":",
                  (Throwable)var14
               );
               return new BDiscoveryPoint(objectName, objectId, info.getName(), BString.make("???"), null, false);
            }
         }

         if (info.isFacet()) {
            BacnetDiscoveryUtil.addFacet(info.getId(), propertyValue, facetMap, this.device());
         }

         return plr;
      } catch (RuntimeException var16) {
         logger.log(
            Level.SEVERE,
            "Exception in buildPropertyResult for "
               + objectName
               + " ["
               + objectId
               + "] pInfo:"
               + info
               + " val="
               + ByteArrayUtil.toHexString(propertyValue)
               + " f="
               + facetMap
               + ":",
            (Throwable)var16
         );
         return new BDiscoveryPoint(objectName, objectId, info.getName(), BString.make("???"), null, false);
      }
   }

   private BDiscoveryPoint[] buildPropertyChildren(
      String objectName, BBacnetObjectIdentifier objectId, int[] propertyList, HashMap<String, BIDataValue> facetMap
   ) {
      BDiscoveryPoint[] ret = new BDiscoveryPoint[propertyList.length];

      for (int i = 0; i < propertyList.length; i++) {
         try {
            PropertyInfo info = this.device().getPropertyInfo(objectId.getObjectType(), propertyList[i]);
            byte[] encodedValue = client().readProperty(this.device().getAddress(), objectId, propertyList[i]);
            if (info != null) {
               ret[i] = this.buildPropertyResult(objectName, objectId, info, encodedValue, facetMap);
            } else {
               ret[i] = new BDiscoveryPoint(objectName, objectId, BBacnetPropertyIdentifier.tag(propertyList[i]), BString.make("?n/a?"), null, false);
            }
         } catch (BacnetException var9) {
            logger.info("BacnetException reading property " + BBacnetPropertyIdentifier.tag(propertyList[i]) + ":" + var9);
            ret[i] = new BDiscoveryPoint(objectName, objectId, BBacnetPropertyIdentifier.tag(propertyList[i]), BString.make("???"), null, false);
         }
      }

      return ret;
   }

   BDiscoveryPoint[] discoverElements(BDiscoveryPoint point) {
      BDiscoveryPoint[] elements = point.getPoints();
      int propertyId = point.getPropertyId();
      PropertyInfo propInfo = this.device().getPropertyInfo(point.getObjectId().getObjectType(), propertyId);
      if (this.device().isServiceSupported("readPropertyMultiple")) {
         Vector refs = new Vector();

         for (int i = 0; i < elements.length; i++) {
            refs.add(new NBacnetPropertyReference(propertyId, elements[i].getPropertyArrayIndex()));
         }

         try {
            Vector vals = client().readPropertyMultiple(this.device().getAddress(), point.getObjectId(), refs);
            Iterator it = vals.iterator();
            int elem = 0;

            while (it.hasNext()) {
               try {
                  NReadPropertyResult rpr = (NReadPropertyResult)it.next();
                  if (propInfo != null) {
                     BTypeSpec tspec = BTypeSpec.make(propInfo.getType());
                     BValue v = (BValue)tspec.getInstance();
                     v = AsnUtil.fromAsn(rpr.getPropertyValue(), v);
                     elements[elem].setValue(v);
                  } else {
                     BValue v = AsnUtil.asnToValue(rpr.getPropertyValue());
                     elements[elem].setValue(v);
                  }
               } catch (Exception var20) {
                  logger.info("Exception reading element " + elem + " in " + point.getObjectId() + " [" + point.getPropertyIdentifier() + "]: " + var20);
                  elements[elem].setValue(BString.make("???"));
               } finally {
                  elem++;
               }
            }
         } catch (BacnetException var22) {
            logger.info("BacnetException reading elements in " + point.getObjectId() + " [" + point.getPropertyIdentifier() + "]: " + var22);
         }
      } else {
         for (int i = 0; i < elements.length; i++) {
            try {
               byte[] encodedValue = client()
                  .readProperty(this.device().getAddress(), elements[i].getObjectId(), propertyId, elements[i].getPropertyArrayIndex());
               BTypeSpec tspec = BTypeSpec.make(propInfo.getType());
               BValue v = (BValue)tspec.getInstance();
               v = AsnUtil.fromAsn(encodedValue, v);
               elements[i].setValue(v);
            } catch (BacnetException var18) {
               logger.info("BacnetException reading element " + i + " in " + point.getObjectId() + " [" + point.getPropertyIdentifier() + "]: " + var18);
               elements[i].setValue(BString.make("???"));
            } catch (Exception var19) {
               logger.info("Exception reading element " + i + " in " + point.getObjectId() + " [" + point.getPropertyIdentifier() + "]: " + var19);
               elements[i].setValue(BString.make("???"));
            }
         }
      }

      return elements;
   }
}
