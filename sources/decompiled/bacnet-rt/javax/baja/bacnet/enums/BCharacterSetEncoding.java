package javax.baja.bacnet.enums;

import java.util.HashMap;
import javax.baja.nre.annotations.NiagaraEnum;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.nre.annotations.Range;
import javax.baja.sys.BFrozenEnum;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType
@NiagaraEnum(
   range = {@Range(
      value = "iso10646_UTF8",
      ordinal = 0
   ), @Range(
      value = "ibmMicrosoftDBCS",
      ordinal = 1
   ), @Range(
      value = "jisX0208",
      ordinal = 2
   ), @Range(
      value = "iso10646_UCS4",
      ordinal = 3
   ), @Range(
      value = "iso10646_UCS2",
      ordinal = 4
   ), @Range(
      value = "iso8859_1",
      ordinal = 5
   ), @Range(
      value = "unknown",
      ordinal = 255
   )}
)
public final class BCharacterSetEncoding extends BFrozenEnum {
   public static final int ISO_10646_UTF8 = 0;
   public static final int IBM_MICROSOFT_DBCS = 1;
   public static final int JIS_X0208 = 2;
   public static final int ISO_10646_UCS4 = 3;
   public static final int ISO_10646_UCS2 = 4;
   public static final int ISO_8859_1 = 5;
   public static final int UNKNOWN = 255;
   public static final BCharacterSetEncoding iso10646_UTF8 = new BCharacterSetEncoding(0);
   public static final BCharacterSetEncoding ibmMicrosoftDBCS = new BCharacterSetEncoding(1);
   public static final BCharacterSetEncoding jisX0208 = new BCharacterSetEncoding(2);
   public static final BCharacterSetEncoding iso10646_UCS4 = new BCharacterSetEncoding(3);
   public static final BCharacterSetEncoding iso10646_UCS2 = new BCharacterSetEncoding(4);
   public static final BCharacterSetEncoding iso8859_1 = new BCharacterSetEncoding(5);
   public static final BCharacterSetEncoding unknown = new BCharacterSetEncoding(255);
   public static final BCharacterSetEncoding DEFAULT = iso10646_UTF8;
   public static final Type TYPE = Sys.loadType(BCharacterSetEncoding.class);
   private static final String[] javaNames = new String[]{"UTF-8", "DBCS", "EUC-JP", "UTF-32BE", "UTF-16BE", "ISO-8859-1", null};
   private static final String[] bacnetNames = new String[]{
      "ISO 10646 (UTF-8)", "IBM/Microsoft DBCS", "JIS X 0208", "ISO 10646 (UCS-4)", "ISO 10646 (UCS-2)", "ISO 8859-1", "Unknown"
   };
   private static final HashMap<String, String> map = new HashMap<>();

   public static BCharacterSetEncoding make(int ordinal) {
      return (BCharacterSetEncoding)iso10646_UTF8.getRange().get(ordinal, false);
   }

   public static BCharacterSetEncoding make(String tag) {
      return (BCharacterSetEncoding)iso10646_UTF8.getRange().get(tag);
   }

   private BCharacterSetEncoding(int ordinal) {
      super(ordinal);
   }

   public Type getType() {
      return TYPE;
   }

   public String getEncodingName() {
      int ord = this.getOrdinal();
      return ord >= 0 && ord < javaNames.length - 1 ? javaNames[ord] : javaNames[javaNames.length - 1];
   }

   public String getCharSetName() {
      int ord = this.getOrdinal();
      return ord >= 0 && ord < bacnetNames.length - 1 ? bacnetNames[ord] : bacnetNames[bacnetNames.length - 1];
   }

   public static BCharacterSetEncoding fromCharSetName(String charsetName) {
      return make(map.get(charsetName));
   }

   static {
      for (int i = 0; i < javaNames.length; i++) {
         String javaName = javaNames[i];
         if (javaName != null) {
            map.put(bacnetNames[i], javaName);
         }
      }
   }
}
