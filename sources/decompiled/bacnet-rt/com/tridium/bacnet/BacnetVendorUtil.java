package com.tridium.bacnet;

import java.util.logging.Level;
import java.util.logging.Logger;
import javax.baja.file.BIFile;
import javax.baja.naming.BOrd;
import javax.baja.nre.util.IntHashMap;
import javax.baja.sys.BajaRuntimeException;
import javax.baja.xml.XElem;
import javax.baja.xml.XParser;

public class BacnetVendorUtil {
   private static final Logger logger = Logger.getLogger("bacnet");
   private static final BacnetVendorUtil list = new BacnetVendorUtil(BOrd.make("module://bacnet/com/tridium/bacnet/vendors.xml"));
   private final IntHashMap namesById = new IntHashMap(180);

   private BacnetVendorUtil(BOrd ord) {
      this.load(ord);
   }

   private void load(BOrd ord) {
      try {
         if (ord != null && !ord.equals(BOrd.NULL)) {
            if (logger.isLoggable(Level.FINE)) {
               logger.fine("Loading vendor id/name info from " + ord);
            }

            BIFile file = (BIFile)ord.resolve().get();
            XElem root = XParser.make(file.getInputStream()).parse();
            XElem[] vendors = root.elems("vendor");

            for (int i = 0; i < vendors.length; i++) {
               int id = vendors[i].geti("id");
               String name = vendors[i].get("n");
               this.namesById.put(id, name);
            }
         }
      } catch (Exception var8) {
         logger.log(Level.SEVERE, "Error loading vendor file!", (Throwable)var8);
         throw new BajaRuntimeException("Error loading vendor file!", var8);
      }
   }

   public static String getVendorName(int vendorId) {
      return list.nameById(vendorId);
   }

   private String nameById(int vendorId) {
      String s = (String)this.namesById.get(vendorId);
      return s != null ? s : "";
   }

   private void dump() {
      for (int i = 0; i < this.namesById.size(); i++) {
         System.out.println(i + ":" + this.nameById(i));
      }
   }

   public static void main(String[] args) {
      System.out.println("BACnet Vendor List:");
      list.dump();
   }
}
