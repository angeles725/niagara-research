package javax.baja.nre.platform;

public enum RuntimeProfile {
   rt("VM: Java 8 compact3, UI: headless"),
   ux("VM: Java 8 compact3, UI: web"),
   wb("VM: Java 8 SE, UI: bajaui widgets with few SE dependencies"),
   se("VM: Java 8 SE, UI: any"),
   doc("VM: no Java classes, Contents: Documentation Only");

   private String desc;

   RuntimeProfile(String desc) {
      this.desc = desc;
   }

   public String getDescription() {
      return this.desc;
   }

   public static RuntimeProfile valueOf(String name, RuntimeProfile defaultValue) {
      if (name == null) {
         return defaultValue;
      }

      try {
         return valueOf(name);
      } catch (IllegalArgumentException iae) {
         return defaultValue;
      }
   }

   public boolean supportsDependency(RuntimeProfile profile) {
      switch (profile) {
         case rt:
            return this == rt;
         case ux:
            return this == rt || this == ux || this == wb;
         case se:
            return this != doc;
         case wb:
            return this == rt || this == ux || this == wb;
         case doc:
         default:
            return false;
      }
   }
}
