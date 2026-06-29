package com.tridium.lonworks.util.selfdoc;

import com.tridium.lonworks.util.ByteArrayTokenizer;
import java.io.PrintWriter;
import java.util.Vector;
import javax.baja.lonworks.LonException;

public class ConfigTemplateFile {
   private String version;
   private ConfigTemplateRecord[] records;

   public ConfigTemplateFile(byte[] data) throws LonException {
      Vector<ConfigTemplateRecord> v = new Vector<>();
      ByteArrayTokenizer bt = new ByteArrayTokenizer(data, ';');
      this.version = new String(bt.nextToken());

      while (bt.hasMoreTokens()) {
         byte[] a = bt.nextToken();
         if (a[0] > 0) {
            ConfigTemplateRecord ctr;
            try {
               ctr = new ConfigTemplateRecord(a);
            } catch (Throwable var7) {
               var7.printStackTrace();
               break;
            }

            v.addElement(ctr);
         }
      }

      this.records = new ConfigTemplateRecord[v.size()];
      v.copyInto(this.records);
   }

   public ConfigTemplateRecord[] getRecords() {
      return this.records;
   }

   public void toString(StringBuffer sb) {
      sb.append(this.version + ";\n");

      for (int i = 0; i < this.records.length; i++) {
         sb.append(this.records[i].toFileString() + ";\n");
      }
   }

   public void toString(PrintWriter out) {
      out.print(this.version + ";\n");

      for (int i = 0; i < this.records.length; i++) {
         out.print(this.records[i].toFileString() + ";\n");
      }
   }
}
