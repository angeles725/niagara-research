package com.tridium.lonworks.xml;

import com.tridium.lonworks.device.DynaDev;
import javax.baja.file.BIComponentFile;
import javax.baja.file.BIFileStore;
import javax.baja.file.types.text.BXmlFile;
import javax.baja.lonworks.BDynamicDevice;
import javax.baja.naming.BOrd;
import javax.baja.nre.annotations.FileExt;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.space.Mark;
import javax.baja.sys.BIcon;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType(
   ext = {@FileExt(
      name = "lnml"
   )}
)
public class BLnmlFile extends BXmlFile implements BILnmlFile, BIComponentFile {
   public static final Type TYPE = Sys.loadType(BLnmlFile.class);
   private static final BIcon icon = BIcon.make("module://lonworks/com/tridium/lonworks/ui/icons/lnml.png");

   public Type getType() {
      return TYPE;
   }

   public BLnmlFile(BIFileStore store) {
      super(store);
   }

   public BLnmlFile() {
   }

   public Mark readComponents() throws Exception {
      BOrd ord = this.getOrdInHost();
      BDynamicDevice ddev = new BDynamicDevice();
      ddev.setXmlFile(ord);
      XLonInterfaceFile root = LonXMLReader.decode(ord);
      DynaDev.importXLon(ddev, root, null);
      String s = ord.encodeToString();
      String name = s.substring(s.lastIndexOf("/") + 1, s.lastIndexOf("."));
      return new Mark(ddev, name);
   }

   public String getMimeType() {
      return "text/xml/lnml";
   }

   public BIcon getIcon() {
      return icon;
   }
}
