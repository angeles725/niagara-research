package com.tridium.bacnet.stack.link.sc.connection;

import com.tridium.bacnet.stack.link.sc.BAbstractConnectionManager;
import com.tridium.bacnet.stack.link.sc.ScLinkLayerUtil;
import java.lang.ref.WeakReference;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.baja.naming.BOrd;
import javax.baja.nre.annotations.Facet;
import javax.baja.nre.annotations.NiagaraProperties;
import javax.baja.nre.annotations.NiagaraProperty;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.sys.BComponent;
import javax.baja.sys.BFacets;
import javax.baja.sys.Context;
import javax.baja.sys.IllegalParentException;
import javax.baja.sys.Property;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;
import javax.baja.util.BIRestrictedComponent;
import javax.baja.web.BINiagaraWebServlet;
import javax.baja.web.BWebServer;
import javax.baja.web.BWebService;

@NiagaraType
@NiagaraProperties({@NiagaraProperty(
      name = "servletName",
      type = "String",
      defaultValue = ""
   ), @NiagaraProperty(
      name = "webServiceOrd",
      type = "BOrd",
      defaultValue = "BOrd.NULL",
      facets = {@Facet(
         name = "BFacets.TARGET_TYPE",
         value = "\"web:WebService\""
      )}
   )})
public abstract class BAbstractScWebSocketAcceptor extends BComponent implements BINiagaraWebServlet, BIRestrictedComponent {
   public static final Property servletName = newProperty(0, "", null);
   public static final Property webServiceOrd = newProperty(0, BOrd.NULL, BFacets.make("targetType", "web:WebService"));
   public static final Type TYPE = Sys.loadType(BAbstractScWebSocketAcceptor.class);
   private WeakReference<BWebService> webServiceRef = new WeakReference<>(null);
   private BAbstractConnectionManager parent;
   private String faultCause;

   public String getServletName() {
      return this.getString(servletName);
   }

   public void setServletName(String v) {
      this.setString(servletName, v, null);
   }

   public BOrd getWebServiceOrd() {
      return (BOrd)this.get(webServiceOrd);
   }

   public void setWebServiceOrd(BOrd v) {
      this.set(webServiceOrd, v, null);
   }

   public Type getType() {
      return TYPE;
   }

   public abstract void updateWebSocketSettings();

   public void started() throws Exception {
      super.started();
      this.parent = (BAbstractConnectionManager)this.getParent();
   }

   public void stopped() throws Exception {
      this.unregister();
      super.stopped();
   }

   public void linkCommStart() {
      this.register();
   }

   public void linkCommStop() {
      this.unregister();
   }

   public void changed(Property property, Context context) {
      super.changed(property, context);
      if (this.isRunning() && (property.equals(servletName) || property.equals(webServiceOrd))) {
         this.unregister();
         this.register();
      }
   }

   private void register() {
      if (this.parent != null && !this.parent.isFatalFault() && !this.parent.getStatus().isDisabled() && this.parent.acceptEnabled()) {
         BWebService webService = this.getWebService();
         if (webService != null) {
            BWebServer webServer = webService.getWebServer();
            if (webServer == null) {
               this.getLogger().log(Level.FINE, "WebService does not contain a WebServer on which to register WebSocketAcceptor");
               this.configFail("webSocketAcceptor.webServiceMissingWebServer");
            } else {
               try {
                  webServer.register(this);
                  this.webServiceRef = new WeakReference<>(webService);
               } catch (Exception var4) {
                  this.getLogger().log(Level.FINE, "Failed to register WebSocketAcceptor", (Throwable)var4);
                  this.configFail("webSocketAcceptor.couldNotRegister");
               }
            }
         }
      }
   }

   private void unregister() {
      BWebService webService = this.webServiceRef.get();
      this.webServiceRef.clear();
      if (webService == null) {
         this.getLogger().fine("Did not unregister WebSocketAcceptor: not previously registered");
      } else {
         BWebServer webServer = webService.getWebServer();
         if (webServer == null) {
            this.getLogger().fine("Failed to unregister WebSocketAcceptor: WebService does not contain a WebServer on which to unregister WebSocketAcceptor");
         } else {
            try {
               webServer.unregister(this);
            } catch (Exception var4) {
               this.getLogger().log(Level.FINE, "Failed to unregister WebSocketAcceptor", (Throwable)var4);
            }
         }
      }
   }

   public void setValidServletName(boolean valid) {
      if (valid) {
         this.configOk();
      } else {
         this.configFail("webSocketAcceptor.couldNotRegister");
      }
   }

   public final BWebService getWebService() {
      BOrd ord = this.getWebServiceOrd();
      if (!ord.isNull()) {
         if (!ScLinkLayerUtil.areOrdSchemesValid(ord)) {
            String allowedOrdQuerySchemes = String.join(", ", ScLinkLayerUtil.ALLOWED_ORD_QUERY_SCHEMES);
            if (this.getLogger().isLoggable(Level.FINE)) {
               this.getLogger().fine("WebService ord " + ord + " contains an ord query scheme not in the allowed list: " + allowedOrdQuerySchemes);
            }

            this.configFail("webSocketAcceptor.disallowedOrdScheme", allowedOrdQuerySchemes);
            return null;
         }

         try {
            return (BWebService)ord.get(this);
         } catch (Exception var3) {
            this.getLogger().log(Level.FINE, "Failed to find WebService at " + ord, (Throwable)var3);
            this.configFail("webSocketAcceptor.couldNotResolveWebService");
         }
      } else {
         try {
            return (BWebService)Sys.getService(BWebService.TYPE);
         } catch (Exception var4) {
            this.getLogger().log(Level.FINE, "Failed to find default WebService", (Throwable)var4);
            this.configFail("webSocketAcceptor.couldNotResolveWebService");
         }
      }

      return null;
   }

   private Logger getLogger() {
      return this.parent != null ? this.parent.getLogger() : Logger.getLogger("bacnet.sc.webSocketAcceptor");
   }

   public String getFaultCause() {
      return this.faultCause;
   }

   private void configFail(String key, Object... args) {
      this.faultCause = ScLinkLayerUtil.LEXICON.getText(key, null, args);
      this.parent.updateStatus();
   }

   private void configOk() {
      this.faultCause = null;
      this.parent.updateStatus();
   }

   public void checkParentForRestrictedComponent(BComponent parent, Context cx) {
      if (!(parent instanceof BAbstractConnectionManager)) {
         throw new IllegalParentException("baja", "IllegalParentException.parentAndChild", new Object[]{parent.getType(), this.getType()});
      } else {
         ScLinkLayerUtil.checkForDuplicate(this, parent);
      }
   }
}
