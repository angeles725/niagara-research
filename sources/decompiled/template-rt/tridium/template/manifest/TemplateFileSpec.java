package com.tridium.template.manifest;

import java.util.Objects;
import javax.baja.file.BDataFile;

public class TemplateFileSpec implements Comparable<TemplateFileSpec> {
   private final String name;
   private final String type;
   private final String sourceOrd;
   private final BDataFile file;

   public TemplateFileSpec(String name, String type, String sourceOrd, BDataFile file) {
      this.name = Objects.requireNonNull(name);
      this.type = Objects.requireNonNull(type);
      this.sourceOrd = Objects.requireNonNull(sourceOrd);
      this.file = Objects.requireNonNull(file);
   }

   public String getName() {
      return this.name;
   }

   public String getType() {
      return this.type;
   }

   public String getSourceOrd() {
      return this.sourceOrd;
   }

   public BDataFile getFile() {
      return this.file;
   }

   public int compareTo(TemplateFileSpec o) {
      int result = this.name.compareTo(o.name);
      if (0 == result) {
         result = this.type.compareTo(o.type);
         if (0 == result) {
            result = this.sourceOrd.compareTo(o.sourceOrd);
            if (0 == result) {
               result = this.file.compareTo(o.file);
            }
         }
      }

      return result;
   }
}
