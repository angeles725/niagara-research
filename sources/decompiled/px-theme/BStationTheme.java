package com.tridium.ui.theme;

import com.tridium.util.BIStationTheme;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.sys.BDynamicEnum;
import javax.baja.sys.BEnumRange;
import javax.baja.sys.BObject;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType
public class BStationTheme extends BObject implements BIStationTheme {
   public static final Type TYPE = Sys.loadType(BStationTheme.class);
   private static Logger logger = Logger.getLogger("com.tridium.ui.theme");
   private static boolean stationThemeSet = false;
   private static String STATION_THEME = AccessController.doPrivileged((PrivilegedAction<String>)(() -> System.getProperty("niagara.stationTheme")));

   public Type getType() {
      return TYPE;
   }

   public void setStationTheme(BDynamicEnum stationTheme) {
      if (!stationThemeSet) {
         stationThemeSet = true;
         if ("palladium".equalsIgnoreCase(STATION_THEME)) {
            logger.fine("Keeping default palladium theme for station theme.");
         } else {
            if (STATION_THEME != null) {
               stationTheme = BDynamicEnum.make(0, BEnumRange.make(new String[]{STATION_THEME}));
               if (logger.isLoggable(Level.FINE)) {
                  logger.fine("Installing custom theme module from system.property entry " + stationTheme.getTag() + "...");
               }
            } else if (logger.isLoggable(Level.FINE)) {
               logger.fine("Installing custom theme module " + stationTheme.getTag() + "...");
            }

            Theme.installFromEnum(stationTheme);
         }
      }
   }
}
