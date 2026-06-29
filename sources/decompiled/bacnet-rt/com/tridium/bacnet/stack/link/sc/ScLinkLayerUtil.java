package com.tridium.bacnet.stack.link.sc;

import com.tridium.sys.station.BStationScheme;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.baja.bacnet.BBacnetNetwork;
import javax.baja.naming.BLocalScheme;
import javax.baja.naming.BOrd;
import javax.baja.naming.BServiceScheme;
import javax.baja.naming.BSlotScheme;
import javax.baja.naming.OrdQuery;
import javax.baja.space.BHandleScheme;
import javax.baja.sys.BComplex;
import javax.baja.sys.BComponent;
import javax.baja.sys.BajaRuntimeException;
import javax.baja.sys.IllegalChildException;
import javax.baja.sys.IllegalParentException;
import javax.baja.sys.Property;
import javax.baja.sys.SlotCursor;
import javax.baja.sys.Type;
import javax.baja.util.LexiconModule;

public final class ScLinkLayerUtil {
   public static final Set<String> ALLOWED_ORD_QUERY_SCHEMES = Collections.unmodifiableSet(makeValidQueryOrdSchemeSet());
   public static final String NIAGARA_LOCAL_CONNECTION_TOKEN_HEADER = "Niagara-Local-Connection-Token";
   public static final String HUB_FUNCTION_LICENSE_CONNECTION_LIMIT_KEY = "hubConnections.limit";
   public static final String NODE_SWITCH_LICENSE_CONNECTION_LIMIT_KEY = "nodeSwitchConnections.limit";
   public static final LexiconModule LEXICON = LexiconModule.make("bacnet");

   private ScLinkLayerUtil() {
   }

   static int incrementUnsignedShort(int i) {
      return i >= 65535 ? 0 : i + 1;
   }

   public static BScLinkLayer getScLinkLayer(BComplex base) {
      for (BComplex c = base; c != null; c = c.getParent()) {
         if (c instanceof BScLinkLayer) {
            return (BScLinkLayer)c;
         }
      }

      throw new BajaRuntimeException("Missing ScLinkLayer ancestor");
   }

   public static UUID getLocalDeviceUuid() {
      return BBacnetNetwork.localDevice().getUuid();
   }

   public static void checkForDuplicate(BComponent child, BComponent parent) {
      checkForTooMany(child, parent, 1);
   }

   public static void checkForTooMany(BComponent child, BComponent parent, int allowed) {
      SlotCursor<Property> slots = parent.getProperties();

      while (slots.next(child.getClass())) {
         if (slots.get() != child) {
            if (--allowed == 0) {
               throw new IllegalChildException("bacnet", "DuplicateRestrictedComponent", new Object[]{parent.getType(), child.getType()});
            }
         }
      }
   }

   public static void checkParentType(Type childType, Type parentType, Type... allowedTypes) {
      for (Type allowedType : allowedTypes) {
         if (parentType.is(allowedType)) {
            return;
         }
      }

      throw new IllegalParentException("baja", "IllegalParentException.parentAndChild", new Object[]{parentType, childType});
   }

   public static void logException(Logger logger, StringBuilder message, Exception e) {
      logException(logger, Level.WARNING, message, e);
   }

   public static void logException(Logger logger, Level level, StringBuilder message, Exception e) {
      if (logger.isLoggable(Level.FINE)) {
         logger.log(Level.FINE, message.append("; exception: ").append(e.getLocalizedMessage()).toString(), (Throwable)e);
      } else if (logger.isLoggable(level)) {
         logger.log(level, message.append("; exception: ").append(e.getLocalizedMessage()).toString());
      }
   }

   public static String getLexiconText(String key, Object... args) {
      return LEXICON.getText(key, null, args);
   }

   public static boolean areOrdSchemesValid(BOrd ord) {
      for (OrdQuery ordQuery : ord.parse()) {
         if (!ALLOWED_ORD_QUERY_SCHEMES.contains(ordQuery.getScheme())) {
            return false;
         }
      }

      return true;
   }

   private static Set<String> makeValidQueryOrdSchemeSet() {
      Set<String> validSchemes = new HashSet<>();
      validSchemes.add(BStationScheme.INSTANCE.getId());
      validSchemes.add(BSlotScheme.INSTANCE.getId());
      validSchemes.add(BHandleScheme.INSTANCE.getId());
      validSchemes.add(BServiceScheme.INSTANCE.getId());
      validSchemes.add(BLocalScheme.INSTANCE.getId());
      return validSchemes;
   }
}
