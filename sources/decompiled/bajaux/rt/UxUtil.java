package com.tridium.ux;

import com.tridium.util.CustomThemeModuleManager;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.baja.naming.BLocalHost;
import javax.baja.naming.BOrd;
import javax.baja.sys.BDynamicEnum;
import javax.baja.sys.Clock;
import javax.baja.sys.Context;
import javax.baja.user.BUser;

public final class UxUtil {
   private static Logger logger = Logger.getLogger("web.theme");
   private static final long minWarningTicks = 900000L;
   private static ConcurrentHashMap<String, Long> requestedMissingThemes = new ConcurrentHashMap<>();

   private UxUtil() {
   }

   public static BOrd checkThemeOrd(String themeName, Context cx) {
      BOrd ord = BOrd.make("module://theme" + themeName + "/ux/theme.css");

      try {
         ord.resolve(BLocalHost.INSTANCE, cx);
      } catch (Throwable var10) {
         BDynamicEnum themeEnum = CustomThemeModuleManager.getModuleEnumForTag(themeName);
         ord = themeEnum.equals(BDynamicEnum.DEFAULT) ? BOrd.NULL : BOrd.make("module://theme" + themeEnum.getTag() + "/ux/theme.css");
         String username = "";

         try {
            BUser user = cx.getUser();
            if (user == null) {
               username = cx.getFacet("username").toString();
            } else {
               username = user.getUsername();
            }
         } catch (Throwable var9) {
         }

         if (ord.equals(BOrd.NULL)) {
            if (logger.isLoggable(Level.FINE)) {
               logger.severe("There are no themes installed in the station and the UI may be unusable.");
            }
         } else {
            String logMessage = String.format(
               "The user selected theme %s was not available and was switched to a fallback. You can select a different theme by going to the user %s's profile configuration.",
               themeName,
               username
            );
            if (logger.isLoggable(Level.FINE)) {
               logger.fine(logMessage);
            }

            long now = Clock.ticks();
            if (!requestedMissingThemes.containsKey(themeName) || now - requestedMissingThemes.get(themeName) >= 900000L) {
               requestedMissingThemes.put(themeName, now);
               logger.warning(logMessage);
            }
         }
      }

      return ord;
   }
}
