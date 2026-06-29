package com.tridium.bacnet.job;

import com.tridium.bacnet.asn.AsnConst;
import com.tridium.bacnet.asn.AsnInputStream;
import com.tridium.bacnet.asn.AsnUtil;
import com.tridium.bacnet.stack.BBacnetStack;
import com.tridium.bacnet.stack.client.BBacnetClientLayer;
import com.tridium.bacnet.stack.transport.TransactionException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.baja.bacnet.BBacnetDevice;
import javax.baja.bacnet.BBacnetNetwork;
import javax.baja.bacnet.BacnetException;
import javax.baja.bacnet.datatypes.BBacnetAddress;
import javax.baja.bacnet.datatypes.BBacnetObjectIdentifier;
import javax.baja.bacnet.enums.BBacnetEngineeringUnits;
import javax.baja.bacnet.enums.BBacnetPropertyIdentifier;
import javax.baja.bacnet.io.AsnException;
import javax.baja.bacnet.io.ErrorException;
import javax.baja.data.BIDataValue;
import javax.baja.naming.SlotPath;
import javax.baja.nre.util.IntHashMap;
import javax.baja.sys.BBoolean;
import javax.baja.sys.BDouble;
import javax.baja.sys.BEnumRange;
import javax.baja.sys.BFloat;
import javax.baja.sys.BInteger;
import javax.baja.sys.BNumber;
import javax.baja.sys.BString;
import javax.baja.sys.InvalidEnumException;
import javax.baja.units.BUnit;

public class BacnetDiscoveryUtil implements AsnConst {
   private static final double LN_10 = Math.log(10.0);
   private static Logger logger = Logger.getLogger("bacnet.client");
   private static IntHashMap facetPropsByType = new IntHashMap();

   public static void addFacet(int facetProp, byte[] propertyValue, HashMap<String, BIDataValue> fmap, BBacnetDevice device) {
      try {
         switch (facetProp) {
            case 4:
               fmap.put("trueText", BString.make(AsnUtil.fromAsnCharacterString(propertyValue)));
               break;
            case 46:
               fmap.put("falseText", BString.make(AsnUtil.fromAsnCharacterString(propertyValue)));
               break;
            case 65:
               fmap.put("max", makeNumericFacetKey(AsnUtil.fromAsnReal(propertyValue)));
               break;
            case 69:
               fmap.put("min", makeNumericFacetKey(AsnUtil.fromAsnReal(propertyValue)));
               break;
            case 106:
               float f = AsnUtil.fromAsnReal(propertyValue);
               fmap.put("resolution", BFloat.make(f));
               if (f > 0.0F) {
                  double fPrec = -(Math.log(f) / LN_10);
                  fPrec -= 1.0E-6;
                  BInteger precision = BInteger.make((int)Math.ceil(fPrec));
                  if (precision.getInt() > 7) {
                     precision = BInteger.make(7);
                  }

                  fmap.put("precision", precision);
               }
               break;
            case 110:
               AsnInputStream in = new AsnInputStream(propertyValue);
               ArrayList<String> v = new ArrayList<>();

               for (int tag = in.peekTag(); tag != -1; tag = in.peekTag()) {
                  StringBuilder s = new StringBuilder(SlotPath.escape(in.readCharacterString()));

                  while (v.contains(s.toString())) {
                     s.append("$2E");
                  }

                  v.add(s.toString());
               }

               int[] ords = new int[v.size()];

               for (int j = 0; j < ords.length; j++) {
                  ords[j] = j + 1;
               }

               String[] tags = v.toArray(new String[0]);
               fmap.put("range", BEnumRange.make(ords, tags));
               break;
            case 117:
               int unitEnum = AsnUtil.fromAsnEnumerated(propertyValue);

               try {
                  if (BBacnetEngineeringUnits.isFixed(unitEnum)) {
                     BUnit u = BBacnetEngineeringUnits.make(unitEnum).getNiagaraUnits();
                     fmap.put("units", u);
                  } else if (logger.isLoggable(Level.INFO)) {
                     logger.info("Unit enumeration " + BBacnetEngineeringUnits.tag(unitEnum) + " is unknown!");
                  }
               } catch (InvalidEnumException var11) {
                  logger.warning("Can't make BUnits from BacnetEngineeringUnits:" + BBacnetEngineeringUnits.tag(unitEnum));
               }
         }
      } catch (AsnException var12) {
         if (logger.isLoggable(Level.INFO)) {
            logger.info("AsnException parsing data for facet property " + BBacnetPropertyIdentifier.tag(facetProp));
         }
      } catch (Exception var13) {
         if (logger.isLoggable(Level.INFO)) {
            logger.info("Exception adding facet for property " + BBacnetPropertyIdentifier.tag(facetProp) + ":" + var13);
         }
      }
   }

   public static BBoolean checkForPriorityArray(BBacnetObjectIdentifier objectId, BBacnetDevice device) {
      if (!objectId.isValid()) {
         return BBoolean.FALSE;
      } else {
         try {
            client().readProperty(device.getAddress(), objectId, 87, 0);
            return BBoolean.TRUE;
         } catch (Exception var3) {
            return BBoolean.FALSE;
         }
      }
   }

   public static HashMap<String, BIDataValue> discoverFacets(BBacnetObjectIdentifier objectId, BBacnetDevice device) {
      return discoverFacets(objectId, device != null ? device.getAddress() : null);
   }

   public static HashMap<String, BIDataValue> discoverFacets(BBacnetObjectIdentifier objectId, BBacnetAddress address) {
      HashMap<String, BIDataValue> map = new HashMap<>();
      if (!objectId.isValid()) {
         return map;
      } else {
         int[] facetProps = getFacetProps(objectId.getObjectType());
         if (facetProps == null) {
            return map;
         } else {
            BBacnetClientLayer client = client();
            if (client != null && address != null) {
               for (int i = 0; i < facetProps.length; i++) {
                  try {
                     byte[] propertyValue = client.readProperty(address, objectId, facetProps[i]);
                     addFacet(facetProps[i], propertyValue, map, null);
                  } catch (TransactionException var7) {
                     if (logger.isLoggable(Level.INFO)) {
                        logger.info("TransactionException reading property " + BBacnetPropertyIdentifier.tag(facetProps[i]) + " in " + objectId + ": " + var7);
                     }

                     return map;
                  } catch (ErrorException var8) {
                  } catch (BacnetException var9) {
                     if (logger.isLoggable(Level.FINE)) {
                        logger.fine("BacnetException reading property " + BBacnetPropertyIdentifier.tag(facetProps[i]) + " in " + objectId + ": " + var9);
                     }
                  }
               }
            }

            return map;
         }
      }
   }

   public static int[] getFacetProps(int objectType) {
      return (int[])facetPropsByType.get(objectType);
   }

   public static final BNumber makeNumericFacetKey(float f) {
      if (f <= -Float.MAX_VALUE) {
         return BDouble.NEGATIVE_INFINITY;
      } else {
         return f >= Float.MAX_VALUE ? BDouble.POSITIVE_INFINITY : BDouble.make(f);
      }
   }

   private static BBacnetClientLayer client() {
      return ((BBacnetStack)BBacnetNetwork.bacnet().getBacnetComm()).getClient();
   }

   static {
      facetPropsByType.put(0, new int[]{106, 69, 65, 117});
      facetPropsByType.put(1, new int[]{106, 69, 65, 117});
      facetPropsByType.put(2, new int[]{106, 69, 65, 117});
      facetPropsByType.put(46, new int[]{106, 69, 65, 117});
      facetPropsByType.put(45, new int[]{106, 69, 65, 117});
      facetPropsByType.put(48, new int[]{106, 69, 65, 117});
      facetPropsByType.put(3, new int[]{4, 46});
      facetPropsByType.put(4, new int[]{4, 46});
      facetPropsByType.put(5, new int[]{4, 46});
      facetPropsByType.put(13, new int[]{110});
      facetPropsByType.put(14, new int[]{110});
      facetPropsByType.put(19, new int[]{110});
   }
}
