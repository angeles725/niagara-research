package com.tridium.ux;

import com.tridium.json.JSONObject;
import com.tridium.json.JSONUtil;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;
import javax.baja.sys.BDynamicEnum;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;
import javax.baja.timezone.BTimeZone;
import javax.baja.user.BUser;
import javax.baja.web.WebOp;
import org.owasp.encoder.Encode;

public class NiagaraEnv {
   private Map<String, Object> map;
   private Type profileType;
   private String themeName;
   private String userName;
   private String timeZoneId;
   private final NiagaraEnv.EnvType type;

   public NiagaraEnv(NiagaraEnv.EnvType type) {
      if (type == null) {
         throw new IllegalArgumentException("type required");
      } else {
         this.type = type;
         this.timeZoneId = BTimeZone.getLocal().getId();
      }
   }

   public NiagaraEnv withCustomProperty(String propName, Object value) {
      if (this.map == null) {
         this.map = new HashMap<>();
      }

      this.map.put(propName, value);
      return this;
   }

   public NiagaraEnv withProfile(Type profileType) {
      this.profileType = profileType;
      return this;
   }

   public NiagaraEnv withThemeName(String themeName) {
      this.themeName = themeName;
      return this;
   }

   public NiagaraEnv withUser(String userName) {
      this.userName = userName;
      return this;
   }

   public NiagaraEnv withTimeZoneId(String timeZoneId) {
      this.timeZoneId = timeZoneId;
      return this;
   }

   public NiagaraEnv withWebOp(WebOp op) {
      BDynamicEnum theme = (BDynamicEnum)op.getProfileConfig().get("selectedHxTheme");
      BUser user = op.getUser();
      if (theme != null) {
         this.withThemeName(theme.getTag());
      }

      if (user != null) {
         this.withUser(user.getUsername());
      }

      return this;
   }

   public JSONObject toJson() {
      if (this.type == null) {
         throw new IllegalStateException("Type required");
      } else {
         JSONObject obj = new JSONObject();
         obj.put("profile", this.profileType == null ? null : this.profileType.toString());
         obj.put("regLastBuildTime", Sys.getRegistry().getLastBuildTime().getMillis());
         obj.put("themeName", this.themeName);
         obj.put("type", this.type.getKey());
         obj.put("user", this.userName);
         obj.put("timeZoneId", this.timeZoneId);
         if (this.map != null) {
            this.map.forEach(obj::put);
         }

         return obj;
      }
   }

   public String toJavaScript() {
      StringWriter sw = new StringWriter();
      this.toJavaScript(new PrintWriter(sw));
      return sw.toString();
   }

   public void toJavaScript(PrintWriter out) {
      JSONObject obj = this.toJson();
      setGlobal(out, "window", "niagara", new JSONObject());
      setGlobal(out, "window.niagara", "env", new JSONObject());
      JSONUtil.sortedKeys(obj).forEachRemaining(key -> setGlobal(out, "window.niagara.env", key, obj.get(key)));
   }

   private static void setGlobal(PrintWriter out, String name, String key, Object value) {
      String encodedString = Encode.forJavaScript(String.valueOf(value));
      String valueString = value instanceof String ? "'" + encodedString + "'" : encodedString;
      out.println(String.format("%1$s['%2$s'] = %1$s['%2$s'] || %3$s;", name, Encode.forJavaScript(key), valueString));
   }

   public static enum EnvType {
      HX("hx"),
      HX_PX("hxPx"),
      MOBILE("mobile"),
      WORKBENCH("wb");

      private final String key;

      private EnvType(String key) {
         this.key = key;
      }

      public String getKey() {
         return this.key;
      }
   }
}
