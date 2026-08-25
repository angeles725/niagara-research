package com.tridium.niagarad.servlet;

import com.tridium.niagarad.http.Http;
import com.tridium.niagarad.log.ErrorHandler;
import com.tridium.niagarad.log.MessageBundle;
import com.tridium.niagarad.util.CsrfTokenUtil;
import com.tridium.niagarad.util.DaemonAuthUtil;
import com.tridium.niagarad.util.KeyedList;
import com.tridium.nre.platform.IPlatformProvider;
import com.tridium.nre.util.SystemPropertiesUtil;
import java.nio.charset.StandardCharsets;
import java.security.AccessController;
import java.util.Collection;
import java.util.HashMap;
import javax.baja.nre.util.ByteBuffer;
import javax.baja.nre.util.TextUtil;
import javax.baja.xml.XElem;
import javax.baja.xml.XParser;
import javax.baja.xml.XWriter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class TcpIpServlet extends DaemonServlet {
   private volatile XElem hostSettingsToSave = null;
   HashMap<String, XElem> adapterSettingsToSave = new HashMap<>();
   boolean niagaraUsesIpv6;
   private final IPlatformProvider platformProvider;

   public TcpIpServlet(IPlatformProvider platformProvider) {
      super("tcpip");
      this.platformProvider = platformProvider;
   }

   @Override
   public synchronized int doGet(HttpServletRequest request, ErrorHandler handler, KeyedList query, XWriter content) {
      if (query == null || !query.containsKey("update")) {
         return this.sendSettings(content);
      } else if (!DebugServlet.debugEnabled && !CsrfTokenUtil.verifyCsrfToken(request, query.get("csrfToken", null))) {
         MessageBundle msg = new MessageBundle("invalid CSRF token in request");
         handler.error(msg);
         return 403;
      } else {
         return this.doUpdate(handler, query);
      }
   }

   @Override
   public boolean useDefaultAuthentication() {
      return false;
   }

   @Override
   public boolean authenticate(HttpServletRequest req, HttpServletResponse resp) {
      boolean requireAdmin = false;
      String queryString = req.getQueryString();
      if (queryString != null) {
         KeyedList query = Http.getGetForm(queryString);
         requireAdmin = query.containsKey("update");
      }

      return requireAdmin
         ? DaemonAuthUtil.authAdmin(this.getServer().getAuthenticator(), req, resp)
         : DaemonAuthUtil.authUser(this.getServer().getAuthenticator(), req, resp);
   }

   private int doUpdate(ErrorHandler handler, KeyedList query) {
      if (query.containsKey("save")) {
         return this.saveNetworkSettings(handler);
      }

      if (!query.containsKey("adapterId")) {
         XElem hostSettings = new XElem();
         hostSettings.setName("tcpIpSettings");
         if (query.containsKey("hostname")) {
            String buffer = query.get("hostname", null);
            hostSettings.addAttr("hostname", buffer);
         } else if (query.containsKey("hostName")) {
            String buffer = query.get("hostName", null);
            hostSettings.addAttr("hostname", buffer);
         }

         if (query.containsKey("niagaraUsesIpv6")) {
            this.niagaraUsesIpv6 = Boolean.parseBoolean(query.get("niagaraUsesIpv6", "false"));
         }

         if (query.containsKey("domain")) {
            String buffer = query.get("domain", null);
            hostSettings.addAttr("domain", buffer);
         }

         if (query.containsKey("defaultGateway")) {
            String buffer = query.get("defaultGateway", null);
            hostSettings.addAttr("defaultGateway", buffer);
         }

         if (query.containsKey("ipv6DefaultGateway")) {
            String buffer = query.get("ipv6DefaultGateway", null);
            hostSettings.addAttr("ipv6DefaultGateway", buffer);
         }

         if (query.containsKey("dnsHosts")) {
            String buffer = query.get("dnsHosts", null);
            String[] hosts = TextUtil.split(buffer, ',');
            XElem dnsHosts = new XElem();
            dnsHosts.setName("dnsHosts");

            for (String host : hosts) {
               XElem dnsHost = new XElem();
               dnsHost.setName("dnsHost");
               dnsHost.setAttr("ipAddress", host);
               dnsHosts.addContent(dnsHost);
            }

            hostSettings.addContent(dnsHosts);
         }

         if (query.containsKey("ipv6DnsHosts")) {
            String buffer = query.get("ipv6DnsHosts", null);
            String[] hosts = TextUtil.split(buffer, ',');
            XElem dnsHosts = new XElem();
            dnsHosts.setName("ipv6DnsHosts");

            for (String host : hosts) {
               XElem dnsHost = new XElem();
               dnsHost.setName("ipv6DnsHost");
               dnsHost.setAttr("ipv6Address", host);
               dnsHosts.addContent(dnsHost);
            }

            hostSettings.addContent(dnsHosts);
         }

         this.hostSettingsToSave = hostSettings;
      } else {
         XElem adapterSettings = new XElem();
         adapterSettings.setName("adapter");
         String adapterId = null;
         if (query.containsKey("adapterId")) {
            adapterId = query.get("adapterId", null);
            adapterSettings.setAttr("id", adapterId);
         }

         String[] adapterNames = this.platformProvider.getAdapterNames();
         boolean found = false;

         for (String adapterName : adapterNames) {
            if (adapterName.equals(adapterId)) {
               found = true;
               break;
            }
         }

         if (!found) {
            MessageBundle msg = new MessageBundle("platform", "TcpIpPlatform.invalidAdapter", "TcpIpServlet: Adapter does not exist");
            handler.error(msg);
            return 400;
         }

         if (query.containsKey("enabled")) {
            String buffer = query.get("enabled", null);
            adapterSettings.setAttr("enabled", buffer);
         }

         if (query.containsKey("dhcpEnabled")) {
            String buffer = query.get("dhcpEnabled", null);
            adapterSettings.setAttr("dhcpEnabled", buffer);
         }

         if (query.containsKey("ipv6Enabled")) {
            String buffer = query.get("ipv6Enabled", null);
            adapterSettings.setAttr("ipv6Enabled", buffer);
         }

         if (query.containsKey("ipv6DhcpEnabled")) {
            String buffer = query.get("ipv6DhcpEnabled", null);
            adapterSettings.setAttr("ipv6DhcpEnabled", buffer);
         }

         if (query.containsKey("ipAddress")) {
            String buffer = query.get("ipAddress", null);
            adapterSettings.setAttr("ipAddress", buffer);
         }

         if (query.containsKey("subnetMask")) {
            String buffer = query.get("subnetMask", null);
            adapterSettings.setAttr("subnetMask", buffer);
         }

         if (query.containsKey("defaultGateway")) {
            String buffer = query.get("defaultGateway", null);
            adapterSettings.setAttr("defaultGateway", buffer);
         }

         if (query.containsKey("domain")) {
            String buffer = query.get("domain", null);
            adapterSettings.setAttr("domain", buffer);
         }

         if (query.containsKey("dnsHosts")) {
            String buffer = query.get("dnsHosts", null);
            String[] hosts = TextUtil.split(buffer, ',');
            XElem dnsHosts = new XElem();
            dnsHosts.setName("dnsHosts");

            for (String host : hosts) {
               XElem dnsHost = new XElem();
               dnsHost.setName("dnsHost");
               dnsHost.setAttr("ipAddress", host);
               dnsHosts.addContent(dnsHost);
            }

            adapterSettings.addContent(dnsHosts);
         }

         if (query.containsKey("ipv6Address")) {
            String buffer = query.get("ipv6Address", null);
            adapterSettings.setAttr("ipv6Address", buffer);
         }

         if (query.containsKey("ipv6SubnetPrefixLength")) {
            String buffer = query.get("ipv6SubnetPrefixLength", null);
            adapterSettings.setAttr("ipv6SubnetPrefixLength", buffer);
         }

         if (query.containsKey("ipv6DefaultGateway")) {
            String buffer = query.get("ipv6DefaultGateway", null);
            adapterSettings.setAttr("ipv6DefaultGateway", buffer);
         }

         if (query.containsKey("ipv6DnsHosts")) {
            String buffer = query.get("ipv6DnsHosts", null);
            String[] hosts = TextUtil.split(buffer, ',');
            XElem dnsHosts = new XElem();
            dnsHosts.setName("ipv6DnsHosts");

            for (String host : hosts) {
               XElem dnsHost = new XElem();
               dnsHost.setName("ipv6DnsHost");
               dnsHost.setAttr("ipv6Address", host);
               dnsHosts.addContent(dnsHost);
            }

            adapterSettings.addContent(dnsHosts);
         }

         this.adapterSettingsToSave.put(adapterId, adapterSettings);
      }

      return 200;
   }

   private int sendSettings(XWriter content) {
      String hostSettings;
      synchronized (this) {
         hostSettings = this.platformProvider.getNetworkSettingsXML();
      }

      try {
         XElem hostSettingsXml = XParser.make(hostSettings).parse(true);
         if (hostSettingsXml.attrIndex("niagaraUsesIpv6") == -1) {
            hostSettingsXml.addAttr("niagaraUsesIpv6", AccessController.doPrivileged(() -> System.getProperty("niagara.ipv6Enabled", "false")));
         }

         ByteBuffer buffer = new ByteBuffer();
         XWriter out = new XWriter(buffer.getOutputStream());
         out.prolog();
         hostSettingsXml.write(out);
         out.flush();
         out.close();
         hostSettings = new String(buffer.getBytes(), 0, buffer.getLength(), StandardCharsets.UTF_8);
      } catch (Exception var6) {
      }

      content.write(hostSettings);
      return 200;
   }

   private int saveNetworkSettings(ErrorHandler handler) {
      if (this.hostSettingsToSave == null) {
         return 200;
      }

      XElem networkSettings = this.hostSettingsToSave;
      XElem adapterSettings = new XElem();
      adapterSettings.setName("adapters");
      Collection<XElem> adapterElems = this.adapterSettingsToSave.values();
      adapterElems.forEach(adapterSettings::addContent);
      networkSettings.addContent(adapterSettings);
      ByteBuffer byteBuffer = new ByteBuffer();
      XWriter content = new XWriter();
      content.setOutputStream(byteBuffer.getOutputStream());
      networkSettings.write(content);
      content.flush();
      content.close();
      boolean result = SystemPropertiesUtil.setSystemProperty("niagara.ipv6Enabled", String.valueOf(this.niagaraUsesIpv6));

      int rc;
      try {
         rc = this.platformProvider.setNetworkSettingsXML(new String(byteBuffer.getBytes(), 0, byteBuffer.getLength(), StandardCharsets.UTF_8));
      } catch (RuntimeException re) {
         MessageBundle msg = new MessageBundle("TcpIpServlet: Failed to save TCP/IP settings (" + re + ")");
         handler.error(msg);
         return 500;
      }

      this.hostSettingsToSave = null;
      this.adapterSettingsToSave.clear();
      if (result && rc == 0) {
         return 200;
      }

      MessageBundle msg = new MessageBundle("TcpIpServlet: Failed to save TCP/IP settings (result = " + result + ", rc = " + rc + ")");
      handler.error(msg);
      return 500;
   }
}
