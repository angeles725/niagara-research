package com.tridium.lonworks.util;

import com.tridium.lonworks.resource.CrossReference;
import com.tridium.lonworks.resource.ResourceFile;
import com.tridium.lonworks.resource.ResourceFileUtil;
import com.tridium.lonworks.resource.ResourceToXLon;
import com.tridium.lonworks.resource.TypeFile;
import com.tridium.lonworks.util.xif.XifToXDevice;
import com.tridium.lonworks.xml.DevToXLon;
import com.tridium.lonworks.xml.XLonDevice;
import com.tridium.lonworks.xml.XLonInterfaceFile;
import java.io.IOException;
import java.io.PrintStream;
import javax.baja.file.BIFile;
import javax.baja.lonworks.BLonDevice;

public class XLonUtil {
   public static XLonDevice xifToXDevice(BIFile file) throws IOException {
      return XifToXDevice.convert(file, null);
   }

   public static XLonDevice xifToXDevice(String fileName, CrossReference cr) throws IOException {
      return XifToXDevice.convert(fileName, cr);
   }

   public static XLonInterfaceFile resourceToXLonFile(ResourceFile rf, CrossReference cr, PrintStream out) throws IOException {
      ResourceToXLon r2x = new ResourceToXLon(cr, true, out);
      return r2x.convert((TypeFile)rf, cr.getConversion(rf.fileName));
   }

   public static XLonInterfaceFile resourceToXLonFile(String fileName, CrossReference cr) throws IOException {
      return resourceToXLonFile(fileName, cr, System.out);
   }

   private static XLonInterfaceFile resourceToXLonFile(String fileName, CrossReference cr, PrintStream out) throws IOException {
      ResourceFile rf = ResourceFileUtil.getResourceFile(fileName);
      ResourceToXLon r2x = new ResourceToXLon(cr, true, out);
      return r2x.convert((TypeFile)rf, cr.getConversion(fileName));
   }

   public static XLonInterfaceFile deviceToXLonFile(BLonDevice dev) throws Exception {
      return deviceToXLonFile(dev, dev.getDisplayName(null));
   }

   public static XLonInterfaceFile deviceToXLonFile(BLonDevice dev, String name) throws Exception {
      XLonInterfaceFile xfile = DevToXLon.devToXLon(dev);
      xfile.getLonDevice().setName(name);
      xfile.setName(name);
      return xfile;
   }
}
