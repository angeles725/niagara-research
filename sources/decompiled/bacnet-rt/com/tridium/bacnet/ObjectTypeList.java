package com.tridium.bacnet;

import com.tridium.bacnet.asn.NBacnetPropertyReference;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Arrays;
import javax.baja.bacnet.datatypes.BBacnetObjectIdentifier;
import javax.baja.bacnet.util.PropertyInfo;
import javax.baja.file.BIFile;
import javax.baja.log.Log;
import javax.baja.naming.BOrd;
import javax.baja.nre.util.IntHashMap;
import javax.baja.nre.util.IntHashMap.Iterator;
import javax.baja.sys.BajaRuntimeException;
import javax.baja.xml.XElem;
import javax.baja.xml.XParser;

public class ObjectTypeList {
   private static ObjectTypeList INSTANCE = null;
   private static ArrayList<NBacnetPropertyReference> BASIC_PROP_LIST = new ArrayList<>();
   private static Log logger = Log.getLog("bacnet");
   private static int[] BASIC_PROPS = new int[]{75, 77, 79};
   private IntHashMap objectMap = new IntHashMap(25);
   private IntHashMap reqPropsMap = new IntHashMap(25);

   private ObjectTypeList(BOrd ord) {
      this.load(ord);
   }

   public static ObjectTypeList make(BOrd ord) {
      return new ObjectTypeList(ord);
   }

   private void load(BOrd ord) {
      try {
         if (ord != null && !ord.equals(BOrd.NULL)) {
            if (logger.isTraceOn()) {
               logger.trace("Loading object type info from " + ord);
            }

            BIFile file = (BIFile)ord.resolve().get();
            XElem root = XParser.make(file.getInputStream()).parse();
            XElem[] types = root.elems("object");

            for (int i = 0; i < types.length; i++) {
               IntHashMap byPropId = new IntHashMap();
               ArrayList<Integer> reqProps = new ArrayList<>();
               int pr = types[i].geti("pr", 0);
               int type = types[i].geti("t");
               XElem[] props = types[i].elems("property");

               for (int j = 0; j < props.length; j++) {
                  int propId = props[j].geti("i");
                  PropertyInfo info = new PropertyInfo(props[j], pr);
                  byPropId.put(propId, info);
                  boolean required = props[j].getb("r", false);
                  if (required) {
                     reqProps.add(propId);
                  }
               }

               int[] reqPropsArr = new int[reqProps.size()];

               for (int jx = 0; jx < reqPropsArr.length; jx++) {
                  reqPropsArr[jx] = reqProps.get(jx);
               }

               this.objectMap.put(type, byPropId);
               this.reqPropsMap.put(type, reqPropsArr);
            }
         }
      } catch (Exception var15) {
         logger.warning("Unable to load Bacnet Object Types from " + ord + ":" + var15);
         throw new BajaRuntimeException("Error loading object types!", var15);
      }
   }

   public PropertyInfo getPropertyInfo(int objectType, int propertyId) {
      IntHashMap map = (IntHashMap)this.objectMap.get(objectType);
      return map == null ? null : (PropertyInfo)map.get(propertyId);
   }

   public boolean isObjectTypeKnown(int objectType) {
      return this.objectMap.get(objectType) != null;
   }

   public int[] getPossibleProperties(BBacnetObjectIdentifier objectId) {
      return this.getPossibleProperties(objectId, -1);
   }

   public int[] getPossibleProperties(BBacnetObjectIdentifier objectId, int revision) {
      if (objectId == null) {
         return BASIC_PROPS;
      } else {
         IntHashMap propsMap = (IntHashMap)this.objectMap.get(objectId.getObjectType());
         if (propsMap == null) {
            return BASIC_PROPS;
         } else {
            int[] props = new int[propsMap.size()];
            Iterator it = propsMap.iterator();
            int ndx = 0;

            while (it.hasNext()) {
               PropertyInfo propInfo = (PropertyInfo)it.next();
               if (revision == -1 || revision >= propInfo.getPr()) {
                  props[ndx++] = it.key();
               }
            }

            return Arrays.copyOf(props, ndx);
         }
      }
   }

   public int[] getRequiredProperties(BBacnetObjectIdentifier objectId) {
      if (objectId == null) {
         return BASIC_PROPS;
      } else {
         int[] props = (int[])this.reqPropsMap.get(objectId.getObjectType());
         return props != null ? props : BASIC_PROPS;
      }
   }

   @Override
   public String toString() {
      return "ObjectTypeList:size=" + this.size();
   }

   public static ObjectTypeList getInstance() {
      return INSTANCE;
   }

   private int size() {
      return this.objectMap.size();
   }

   public IntHashMap getPropertiesForObjectId(BBacnetObjectIdentifier objectId) {
      return (IntHashMap)this.objectMap.get(objectId.getObjectType());
   }

   static {
      AccessController.doPrivileged((PrivilegedAction<Void>)(() -> {
         try {
            INSTANCE = new ObjectTypeList(BOrd.make("file:!defaults/bacnetObjectTypes.xml"));
         } catch (Exception var3) {
            try {
               INSTANCE = new ObjectTypeList(BOrd.make("module://bacnet/com/tridium/bacnet/objectTypes.xml"));
            } catch (Exception var2) {
               System.err.println("Cannot load BACnet object type data from XML file!");
            }
         }

         return null;
      }));
      BASIC_PROP_LIST.add(new NBacnetPropertyReference(75));
      BASIC_PROP_LIST.add(new NBacnetPropertyReference(77));
      BASIC_PROP_LIST.add(new NBacnetPropertyReference(79));
   }
}
