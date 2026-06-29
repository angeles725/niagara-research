package com.tridium.lonworks.discover;

import com.tridium.util.CompUtil;
import java.util.ArrayList;
import java.util.logging.Logger;
import javax.baja.lonworks.BLonComponent;
import javax.baja.lonworks.BLonDevice;
import javax.baja.lonworks.BLonObject;
import javax.baja.lonworks.BLonObjectFolder;
import javax.baja.lonworks.londata.BLonData;
import javax.baja.lonworks.londata.BLonPrimitive;
import javax.baja.naming.UnresolvedException;
import javax.baja.security.BIProtected;
import javax.baja.sys.BComponent;
import javax.baja.sys.BFacets;
import javax.baja.sys.BValue;
import javax.baja.sys.Context;
import javax.baja.sys.Property;
import javax.baja.sys.SlotCursor;
import javax.baja.units.BUnit;

public final class LonLearnUtil {
   private static final Logger log = Logger.getLogger("lonworks");

   private LonLearnUtil() {
   }

   public static BLonCreationEntry[] discoverPointEntries(BComponent target, Context cx) {
      BLonDevice dev = (BLonDevice)CompUtil.closestAncestor(target, BLonDevice.class).orElseThrow(NullPointerException::new);
      if (!dev.getPermissions(cx).hasOperatorRead()) {
         throw new UnresolvedException();
      } else {
         dev.getComponentSpace().update(dev, Integer.MAX_VALUE);
         ArrayList<BLonCreationEntry> list = new ArrayList<>();
         doUpdateDiscoveryRows(list, dev, cx);
         return list.toArray(new BLonCreationEntry[0]);
      }
   }

   public static void doUpdateDiscoveryRows(ArrayList<BLonCreationEntry> list, BComponent tgtCntr, Context cx) {
      SlotCursor<Property> c = tgtCntr.getProperties();

      while (c.nextObject()) {
         BValue value = tgtCntr.get(c.property());
         if (!(value instanceof BIProtected) || ((BIProtected)value).getPermissions(cx).hasOperatorRead()) {
            if (c.property().getType().is(BLonComponent.TYPE)) {
               BLonComponent lc = (BLonComponent)c.get();
               BLonData ldat = lc.getData();
               String target = lc.getDisplayName(cx);
               if (lc != ldat) {
                  target = target + "/" + ldat.getDisplayName(cx);
               }

               BLonCreationEntry e = addElementRows(ldat, target, cx);
               if (e == null) {
                  log.severe("creation entry null");
               } else {
                  list.add(e);
               }
            } else if (c.property().getType().is(BLonObject.TYPE)) {
               BLonObject lo = (BLonObject)c.get();
               ArrayList<BLonCreationEntry> al = new ArrayList<>();
               doUpdateDiscoveryRows(al, lo, cx);
               BLonCreationEntry objEntry = new BLonCreationEntry(lo);
               objEntry.setChildren(al.toArray(new BLonCreationEntry[0]));
               list.add(objEntry);
            } else if (c.property().getType().is(BLonObjectFolder.TYPE)) {
               BLonObjectFolder lof = (BLonObjectFolder)c.get();
               ArrayList<BLonCreationEntry> al = new ArrayList<>();
               doUpdateDiscoveryRows(al, lof, cx);
               BLonCreationEntry objEntry = new BLonCreationEntry(lof);
               objEntry.setChildren(al.toArray(new BLonCreationEntry[0]));
               list.add(objEntry);
            }
         }
      }
   }

   private static BLonCreationEntry addElementRows(BLonData ldat, String prefix, Context cx) {
      BLonCreationEntry firstEntry = null;
      ArrayList<BLonCreationEntry> list = null;
      SlotCursor<Property> c = ldat.getProperties();

      while (c.nextObject()) {
         Property p = c.property();
         BValue value = ldat.get(p);
         if (!(value instanceof BIProtected) || ((BIProtected)value).getPermissions(cx).hasOperatorRead()) {
            BLonCreationEntry entry = null;
            if (p.getType().is(BLonPrimitive.TYPE)) {
               BFacets f = p.getFacets();
               BUnit devUnit = (BUnit)f.get("units");
               String unit = devUnit == null ? "" : devUnit.getSymbol();
               entry = new BLonCreationEntry(ldat, p.getName(), unit);
            } else if (p.getType().is(BLonData.TYPE)) {
               String pre = prefix + "/" + p.getDefaultDisplayName(cx);
               entry = addElementRows((BLonData)c.get(), pre, cx);
            }

            if (entry != null) {
               if (firstEntry == null) {
                  firstEntry = entry;
               } else {
                  if (list == null) {
                     list = new ArrayList<>();
                  }

                  list.add(entry);
               }
            }
         }
      }

      if (list != null) {
         BLonCreationEntry[] rows = list.toArray(new BLonCreationEntry[0]);
         firstEntry.setChildren(rows);
      }

      return firstEntry;
   }
}
