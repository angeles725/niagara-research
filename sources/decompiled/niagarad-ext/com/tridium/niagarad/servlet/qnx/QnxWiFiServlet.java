package com.tridium.niagarad.servlet.qnx;

import com.tridium.niagarad.log.ErrorHandler;
import com.tridium.niagarad.log.MessageBundle;
import com.tridium.niagarad.servlet.DaemonServlet;
import com.tridium.niagarad.servlet.DebugServlet;
import com.tridium.niagarad.util.CsrfTokenUtil;
import com.tridium.niagarad.util.KeyedList;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.baja.nre.util.ByteBuffer;
import javax.baja.xml.XElem;
import javax.baja.xml.XWriter;
import javax.servlet.http.HttpServletRequest;

public class QnxWiFiServlet extends DaemonServlet {
   public static final int WPA_SUPPLICANT_STATE_UNKNOWN = 0;
   public static final int WPA_SUPPLICANT_STATE_DISCONNECTED = 2;
   public static final int WPA_SUPPLICANT_STATE_IDLE = 3;
   public static final int WPA_SUPPLICANT_STATE_SCANNING = 4;
   public static final int WPA_SUPPLICANT_STATE_ASSOCIATING = 5;
   public static final int WPA_SUPPLICANT_STATE_ASSOCIATED = 6;
   public static final int WPA_SUPPLICANT_STATE_FOURWAYHANDSHAKE = 7;
   public static final int WPA_SUPPLICANT_STATE_GROUPHANDSHAKE = 8;
   public static final int WPA_SUPPLICANT_STATE_COMPLETED = 9;
   private static final String staAdapter = "tiw_sta0";
   private static final String sapAdapter = "tiw_sap0";
   private static final String WIFI_COOKIE_PATH = "/opt/niagara/platform/wifi/use-wifi";
   private static final String GATEWAY_OPTIONS_PATH = "/opt/niagara/platform/wifi/dhclient-options";
   private static final String _SAP_TO_PATH = "/opt/niagara/platform/wifi/sap_to";
   private static final String _WIFI_MONITOR_PATH = "/var/run/wilink";
   private static final String _WIFI_STATUS_PATH = "/var/run/wpaclistatus";
   private static final int OFF = 0;
   private static final int STATION = 1;
   private static final int ACCESS_POINT = 2;
   private static final int LOOKING_FOR_MAC = 0;
   private static final int LOOKING_FOR_TIME = 1;
   protected Logger filter;

   public QnxWiFiServlet() {
      super("qnxwifi");
   }

   @Override
   public boolean doStart() {
      this.filter = Logger.getLogger("qnxwifi");
      return true;
   }

   @Override
   public int doGet(HttpServletRequest request, ErrorHandler handler, KeyedList query, XWriter content) {
      int rc = 400;
      boolean update = false;
      if (query.containsKey("wifiEnabled")
         || query.containsKey("gwswitch")
         || query.containsKey("scan")
         || query.containsKey("connectToNetwork")
         || query.containsKey("disconnectFromNetwork")
         || query.containsKey("reconfigureNetworks")
         || query.containsKey("setSapTimeout")
         || query.containsKey("setCountryCode")) {
         update = true;
      }

      if (update && !DebugServlet.debugEnabled && !CsrfTokenUtil.verifyCsrfToken(request, query.get("csrfToken", null))) {
         MessageBundle msg = new MessageBundle("invalid CSRF token in request");
         handler.error(msg);
         this.filter.severe("invalid CSRF token in request");
         return 403;
      }

      if (query.containsKey("sendMonitorData")) {
         rc = this.sendMonitorData(handler, content);
      } else if (query.containsKey("sendSettings")) {
         rc = this.sendSettings(handler, content);
      } else if (query.containsKey("sendStatus")) {
         rc = this.sendWpaCliStatus(handler, query, content);
      } else if (query.containsKey("wifiEnabled")) {
         rc = this.handleWifiEnabledStateChange(handler, query);
      } else if (query.containsKey("gwswitch")) {
         rc = this.handleEnabledGwSwitchChange(handler, query);
      } else if (query.containsKey("scan")) {
         rc = this.handleScan(handler, query);
      } else if (query.containsKey("scanResults")) {
         rc = this.handleScanResults(handler, query, content);
      } else if (query.containsKey("connectToNetwork")) {
         rc = this.handleConnect(handler, query);
      } else if (query.containsKey("disconnectFromNetwork")) {
         rc = this.handleDisconnect(handler, query);
      } else if (query.containsKey("reconfigureNetworks")) {
         rc = this.handleReconfigure(handler);
      } else if (query.containsKey("setSapTimeout")) {
         rc = this.handleSetSapTimeout(handler, query);
      } else if (query.containsKey("sendClientList")) {
         rc = this.sendClientList(handler, content);
      } else if (query.containsKey("sendChannelList")) {
         rc = this.sendChannelList(handler, query, content);
      } else if (query.containsKey("sendCountryList")) {
         rc = this.sendCountryList(handler, content);
      } else if (query.containsKey("sendCountryCode")) {
         rc = this.sendCountryCode(handler, content);
      } else if (query.containsKey("sendWifiSku")) {
         rc = this.sendWifiSku(handler, content);
      } else if (query.containsKey("setCountryCode")) {
         rc = this.handleSetCountryCode(handler, query);
      } else {
         MessageBundle msg = new MessageBundle("Unknown QnxWifiServlet Request");
         handler.error(msg);
      }

      return rc;
   }

   private int handleReconfigure(ErrorHandler handler) {
      if (wpa_cli(this.filter, new String[]{"reconfigure"}) == null) {
         MessageBundle msg = new MessageBundle("Error reconfiguring WiFi networks");
         handler.error(msg);
         this.filter.severe("error occurred reconfiguring WiFi networks");
         return 500;
      } else {
         return 200;
      }
   }

   private int handleWifiEnabledStateChange(ErrorHandler handler, KeyedList query) {
      if (query.containsKey("wifiEnabled") && !query.get("wifiEnabled", "").equalsIgnoreCase("")) {
         File wifiCookie = new File("/opt/niagara/platform/wifi/use-wifi");
         boolean currentWifiEnabled = wifiCookie.exists();
         boolean requestedWifiEnabled = Boolean.valueOf(query.get("wifiEnabled", "false"));
         if (currentWifiEnabled != requestedWifiEnabled) {
            if (currentWifiEnabled) {
               if (!wifiCookie.delete()) {
                  MessageBundle msg = new MessageBundle("failed to delete /opt/niagara/platform/wifi/use-wifi");
                  handler.error(msg);
                  this.filter.severe("failed to delete /opt/niagara/platform/wifi/use-wifi");
                  return 500;
               }
            } else {
               try {
                  if (!wifiCookie.createNewFile()) {
                     throw new IOException("Failed to create new file");
                  }
               } catch (IOException e) {
                  MessageBundle msg = new MessageBundle("failed to create /opt/niagara/platform/wifi/use-wifi (" + e + ")");
                  handler.error(msg);
                  this.filter.log(Level.SEVERE, "failed to create /opt/niagara/platform/wifi/use-wifi (" + e + ")", e);
                  return 500;
               }
            }
         }

         return 200;
      } else {
         MessageBundle msg = new MessageBundle("platform", "Servlet.missingParam", "wifiEnabled", "Missing wifiEnabled argument");
         handler.error(msg);
         return 400;
      }
   }

   private int handleEnabledGwSwitchChange(ErrorHandler handler, KeyedList query) {
      if (query.containsKey("gwswitch") && !query.get("gwswitch", "").equalsIgnoreCase("")) {
         boolean gwSwitchEnabled = false;
         File gwOptionsFile = new File("/opt/niagara/platform/wifi/dhclient-options");
         String gwOptions = "";
         if (gwOptionsFile.exists()) {
            gwOptions = cat(this.filter, "/opt/niagara/platform/wifi/dhclient-options");
            if (gwOptions != null) {
               gwOptions = gwOptions.trim();
               gwSwitchEnabled = !gwOptions.contains("--no-def-route");
            } else {
               gwOptions = "";
               gwSwitchEnabled = false;
            }
         } else {
            gwSwitchEnabled = false;
         }

         boolean requestedGwSwitchEnabled = Boolean.valueOf(query.get("gwswitch", "false"));
         if (gwSwitchEnabled != requestedGwSwitchEnabled) {
            if (!requestedGwSwitchEnabled) {
               if (gwOptions.length() == 0) {
                  gwOptions = "--no-def-route tiw_sta0";
               } else {
                  gwOptions = "--no-def-route " + gwOptions;
               }
            } else {
               String[] tokens = gwOptions.split("\\s+");
               StringBuilder sb = new StringBuilder();

               for (String token : tokens) {
                  if (!token.equals("--no-def-route")) {
                     sb.append(token).append(" ");
                  }
               }

               gwOptions = sb.toString().trim();
               if (gwOptions.length() == 0) {
                  gwOptions = "tiw_sta0";
               }
            }

            try (FileWriter writer = new FileWriter("/opt/niagara/platform/wifi/dhclient-options")) {
               if (this.filter.isLoggable(Level.FINE)) {
                  this.filter.fine("writing new options string '" + gwOptions + "'");
               }

               writer.write(gwOptions);
               writer.write("\n");
               writer.flush();
            } catch (IOException e) {
               MessageBundle msg = new MessageBundle("Error setting gwswitching: " + requestedGwSwitchEnabled + " (" + e + ")");
               handler.error(msg);
               this.filter.log(Level.SEVERE, "error setting gwswitching: " + requestedGwSwitchEnabled + " (" + e + ")", e);
               return 500;
            }
         }

         return 200;
      } else {
         MessageBundle msg = new MessageBundle("platform", "Servlet.missingParam", "gwswitch", "Missing gwswitch argument");
         handler.error(msg);
         return 400;
      }
   }

   private int handleScan(ErrorHandler handler, KeyedList query) {
      if (query.containsKey("adapter") && !query.get("adapter", "").equalsIgnoreCase("")) {
         String adapter = query.get("adapter", null);
         String scanOutput = wpa_cli(this.filter, new String[]{"-i", adapter, "scan"});
         if (scanOutput != null && scanOutput.trim().equals("OK")) {
            return 200;
         }

         MessageBundle msg = new MessageBundle("Error initiating wifi scan: \"" + scanOutput + "\"");
         handler.error(msg);
         this.filter.severe("error initiating wifi scan: \"" + scanOutput + "\"");
         return 500;
      } else {
         MessageBundle msg = new MessageBundle("platform", "Servlet.missingParam", "adapter", "Missing adapter argument");
         handler.error(msg);
         return 400;
      }
   }

   private int handleScanResults(ErrorHandler handler, KeyedList query, XWriter content) {
      if (query.containsKey("adapter") && !query.get("adapter", "").equalsIgnoreCase("")) {
         String adapter = query.get("adapter", null);
         String scanResultsOutput = wpa_cli(this.filter, new String[]{"-i", adapter, "scan_results"});

         for (int count = 10; (scanResultsOutput == null || scanResultsOutput.length() < 2) && count > 0; count--) {
            try {
               Thread.sleep(1000L);
            } catch (Exception var12) {
            }

            scanResultsOutput = wpa_cli(this.filter, new String[]{"-i", adapter, "scan_results"});
         }

         String[] discoveredNetworks = scanResultsOutput != null ? scanResultsOutput.split("\n") : new String[0];
         XElem wifiScanResultsElem = new XElem("wifiDiscovery");
         wifiScanResultsElem.addAttr("wifiNetworks", String.valueOf(discoveredNetworks.length - 1));

         for (int i = 1; i < discoveredNetworks.length; i++) {
            XElem wifiScanResultElem = new XElem("wifiNetwork");
            wifiScanResultElem.addAttr("description", discoveredNetworks[i]);
            wifiScanResultsElem.addContent(wifiScanResultElem);
         }

         ByteBuffer buffer = new ByteBuffer();
         XWriter out = new XWriter();
         out.setOutputStream(buffer.getOutputStream());
         out.prolog();
         wifiScanResultsElem.write(out);
         out.flush();
         out.close();
         String scanResultsXml = new String(buffer.getBytes(), 0, buffer.getLength());
         content.write(scanResultsXml);
         return 200;
      } else {
         MessageBundle msg = new MessageBundle("platform", "Servlet.missingParam", "adapter", "Missing adapter argument");
         handler.error(msg);
         return 400;
      }
   }

   private int sendCountryList(ErrorHandler handler, XWriter content) {
      XElem countriesElem = new XElem("countries");
      StringBuilder output = new StringBuilder();
      List<String> command = new ArrayList<>();
      command.add("/proc/boot/wifi-skuread");
      if (execute(this.filter, command.toArray(new String[0]), output) != 0) {
         MessageBundle msg = new MessageBundle("Error retrieving wifi sku code");
         handler.error(msg);
         this.filter.severe("error retrieving wifi sku code");
         return 500;
      }

      String skuline = output.toString();
      String[] skuwords = skuline.split(" ");
      String firmwarepath = "/lib/firmware/ti18xx/2015a";
      if (skuwords.length > 1) {
         firmwarepath = skuwords[1];
      }

      List<QnxWiFiServlet.Ccinfo> countries = new ArrayList<>();

      try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(firmwarepath + "/ww-country-codes.txt")))) {
         for (String line = reader.readLine(); line != null; line = reader.readLine()) {
            if (!line.startsWith("#")) {
               StringTokenizer tokenizer = new StringTokenizer(line.trim());

               while (tokenizer.countTokens() >= 2) {
                  String cc = tokenizer.nextToken();
                  String name = tokenizer.nextToken();
                  if (cc.length() == 2) {
                     QnxWiFiServlet.Ccinfo info = new QnxWiFiServlet.Ccinfo(cc, name);
                     countries.add(info);
                  }
               }
            }
         }
      } catch (IOException e) {
         MessageBundle msg = new MessageBundle("Error retrieving country list (" + e + ")");
         handler.error(msg);
         this.filter.log(Level.SEVERE, "error retrieving country list (" + e + ")", e);
         return 500;
      }

      Collections.sort(countries);

      for (QnxWiFiServlet.Ccinfo country : countries) {
         XElem ccElem = new XElem("country");
         ccElem.addAttr("code", country.getCode());
         ccElem.addAttr("name", country.getName().replace('_', ' '));
         countriesElem.addContent(ccElem);
      }

      ByteBuffer buffer = new ByteBuffer();
      XWriter out = new XWriter();
      out.setOutputStream(buffer.getOutputStream());
      out.prolog();
      countriesElem.write(out);
      out.flush();
      out.close();
      String xmlStr = new String(buffer.getBytes(), 0, buffer.getLength());
      content.write(xmlStr);
      return 200;
   }

   private int sendCountryCode(ErrorHandler handler, XWriter content) {
      String cc = getCountryCode(this.filter);
      if (cc == null) {
         MessageBundle msg = new MessageBundle("Error retrieving country code");
         handler.error(msg);
         this.filter.severe("error retrieving country code");
         return 500;
      } else {
         XElem countryElem = new XElem("country");
         countryElem.addAttr("cc", cc);
         ByteBuffer buffer = new ByteBuffer();
         XWriter out = new XWriter();
         out.setOutputStream(buffer.getOutputStream());
         out.prolog();
         countryElem.write(out);
         out.flush();
         out.close();
         String countryConfigXml = new String(buffer.getBytes(), 0, buffer.getLength());
         content.write(countryConfigXml);
         return 200;
      }
   }

   private int sendWifiSku(ErrorHandler handler, XWriter content) {
      String sku = getWifiSkuCode(this.filter);
      if (sku == null) {
         MessageBundle msg = new MessageBundle("Error retrieving wifi sku code");
         handler.error(msg);
         this.filter.severe("error retrieving wifi sku code");
         return 500;
      } else {
         XElem skuElem = new XElem("code");
         skuElem.addAttr("sku", sku);
         ByteBuffer buffer = new ByteBuffer();
         XWriter out = new XWriter();
         out.setOutputStream(buffer.getOutputStream());
         out.prolog();
         skuElem.write(out);
         out.flush();
         out.close();
         String skuConfigXml = new String(buffer.getBytes(), 0, buffer.getLength());
         content.write(skuConfigXml);
         return 200;
      }
   }

   private int sendChannelList(ErrorHandler handler, KeyedList query, XWriter content) {
      String cc = getCountryCode(this.filter);
      XElem countryElem = new XElem("channelConfig");
      countryElem.addAttr("countryCode", cc);
      if (!cc.equals("WW")) {
         StringBuilder sb = new StringBuilder("CountryConfig = ");
         int char0 = cc.charAt(0);
         int char1 = cc.charAt(1);
         sb.append(char0).append(" ").append(char1).append(" ");
         String lineStart = sb.toString();
         StringBuilder output = new StringBuilder();
         List<String> command = new ArrayList<>();
         command.add("/proc/boot/wifi-skuread");
         if (execute(this.filter, command.toArray(new String[0]), output) != 0) {
            MessageBundle msg = new MessageBundle("Error retrieving wifi sku code");
            handler.error(msg);
            this.filter.severe("error retrieving wifi sku code");
            return 500;
         }

         String skuline = output.toString();
         String firmwarepath = "/lib/firmware/ti18xx/2015a";
         String[] skuwords = skuline.split(" ");
         if (skuwords.length > 1) {
            firmwarepath = skuwords[1];
         }

         try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(firmwarepath + "/tiwlanRegDomain.ini")))) {
            for (String line = reader.readLine(); line != null; line = reader.readLine()) {
               if (line.startsWith(lineStart)) {
                  line = line.substring("CountryConfig = ".length() + 6).trim();
                  XElem channelBandElem = new XElem("entry");
                  channelBandElem.addAttr("bandConfig", line);
                  countryElem.addContent(channelBandElem);
               }
            }
         } catch (IOException e) {
            MessageBundle msg = new MessageBundle("Error retrieving channel list (" + e + ")");
            handler.error(msg);
            this.filter.log(Level.SEVERE, "error retrieving channel list (" + e + ")", e);
            return 500;
         }
      }

      ByteBuffer buffer = new ByteBuffer();
      XWriter out = new XWriter();
      out.setOutputStream(buffer.getOutputStream());
      out.prolog();
      countryElem.write(out);
      out.flush();
      out.close();
      String countryConfigXml = new String(buffer.getBytes(), 0, buffer.getLength());
      content.write(countryConfigXml);
      return 200;
   }

   private int handleDisconnect(ErrorHandler handler, KeyedList query) {
      if (query.containsKey("adapter") && !query.get("adapter", "").equalsIgnoreCase("")) {
         String adapter = query.get("adapter", null);
         if (wpa_cli(this.filter, new String[]{"-i", adapter, "disconnect"}) == null) {
            MessageBundle msg = new MessageBundle("Error disconnecting from WiFi network");
            handler.error(msg);
            this.filter.severe("error occurred disconnecting from WiFi network");
            return 500;
         } else {
            return 200;
         }
      } else {
         MessageBundle msg = new MessageBundle("platform", "Servlet.missingParam", "adapter", "Missing adapter argument");
         handler.error(msg);
         return 400;
      }
   }

   private int handleConnect(ErrorHandler handler, KeyedList query) {
      if (!query.containsKey("adapter") || query.get("adapter", "").equalsIgnoreCase("")) {
         MessageBundle msg = new MessageBundle("platform", "Servlet.missingParam", "adapter", "Missing adapter argument");
         handler.error(msg);
         return 400;
      }

      if (query.containsKey("network_id") && !query.get("network_id", "").equalsIgnoreCase("")) {
         String networkId = query.get("network_id", null);
         String adapter = query.get("adapter", null);
         if (wpa_cli(this.filter, new String[]{"-i", adapter, "select_network", networkId}) == null) {
            MessageBundle msg = new MessageBundle("Error connecting to WiFi network \"" + networkId + "\"");
            handler.error(msg);
            this.filter.severe("error occurred connecting to WiFi network \"" + networkId + "\"");
            return 500;
         } else {
            return 200;
         }
      } else {
         MessageBundle msg = new MessageBundle("platform", "Servlet.missingParam", "network_id", "Missing network_id argument");
         handler.error(msg);
         return 400;
      }
   }

   private int sendWpaCliStatus(ErrorHandler handler, KeyedList query, XWriter content) {
      if (query.containsKey("adapter") && !query.get("adapter", "").equalsIgnoreCase("")) {
         String statusOutput = cat(this.filter, "/var/run/wpaclistatus");
         XElem wifiStatusElem = new XElem("wifiStatus");
         String[] statusLines = statusOutput != null ? statusOutput.split("\n") : new String[0];

         for (String statusLine : statusLines) {
            XElem statusPropertyElem = new XElem("statusProperty");
            String[] tuple = statusLine.split("=");
            statusPropertyElem.addAttr("key", tuple[0]);
            statusPropertyElem.addAttr("value", tuple[1]);
            wifiStatusElem.addContent(statusPropertyElem);
         }

         ByteBuffer buffer = new ByteBuffer();
         XWriter out = new XWriter();
         out.setOutputStream(buffer.getOutputStream());
         out.prolog();
         wifiStatusElem.write(out);
         out.flush();
         out.close();
         String wifiStatusString = new String(buffer.getBytes(), 0, buffer.getLength());
         content.write(wifiStatusString);
         return 200;
      } else {
         MessageBundle msg = new MessageBundle("platform", "Servlet.missingParam", "adapter", "Missing adapter argument");
         handler.error(msg);
         return 400;
      }
   }

   private int sendClientList(ErrorHandler handler, XWriter content) {
      XElem clientListElem = new XElem("clientList");
      String hapdCliOutput = hostapd_cli(this.filter, new String[]{"all_sta"});
      int state = 0;
      String mac = "00:00:00:00:00:00";
      String[] statusLines = hapdCliOutput != null ? hapdCliOutput.split("\n") : new String[0];

      for (String statusLine : statusLines) {
         statusLine = statusLine.trim();
         switch (state) {
            case 0:
               if (statusLine.startsWith("dot11RSNAStatsSTAAddress=")) {
                  mac = statusLine.split("=")[1];
                  state = 1;
               }

               if (statusLine.matches("^([0-9a-fA-F][0-9a-fA-F]:){5}[0-9a-fA-F][0-9a-fA-F]$")) {
                  mac = statusLine;
                  state = 1;
               }
               break;
            case 1:
               if (statusLine.startsWith("connected_time=")) {
                  String connected_time = statusLine.split("=")[1];
                  state = 0;
                  XElem clientElem = new XElem("client");
                  clientElem.addAttr("mac", mac);
                  clientElem.addAttr("time", connected_time);
                  clientListElem.addContent(clientElem);
               }
         }
      }

      ByteBuffer buffer = new ByteBuffer();
      XWriter out = new XWriter();
      out.setOutputStream(buffer.getOutputStream());
      out.prolog();
      clientListElem.write(out);
      out.flush();
      out.close();
      String clientListString = new String(buffer.getBytes(), 0, buffer.getLength());
      content.write(clientListString);
      return 200;
   }

   private int sendMonitorData(ErrorHandler handler, XWriter content) {
      XElem wifiSettingsElem = new XElem("wifiMonitor");
      int wifiSwitch = this.getSwitchMode();
      wifiSettingsElem.addAttr("wifiSwitch", String.valueOf(wifiSwitch));
      String monitorString = cat(this.filter, "/var/run/wilink");
      String wifistate = monitorString != null ? monitorString.trim() : null;
      if (wifistate != null) {
         wifiSettingsElem.addAttr("wifimonState", wifistate);
      } else {
         wifiSettingsElem.addAttr("wifimonState", "unknown");
      }

      boolean wifiEnabled = new File("/opt/niagara/platform/wifi/use-wifi").exists();
      wifiSettingsElem.addAttr("wifiEnabled", String.valueOf(wifiEnabled));
      ByteBuffer buffer = new ByteBuffer();
      XWriter out = new XWriter();
      out.setOutputStream(buffer.getOutputStream());
      out.prolog();
      wifiSettingsElem.write(out);
      out.flush();
      out.close();
      String wifiMonitorStr = new String(buffer.getBytes(), 0, buffer.getLength());
      content.write(wifiMonitorStr);
      return 200;
   }

   private int sendSettings(ErrorHandler handler, XWriter content) {
      XElem wifiSettingsElem = new XElem("wifiSettings");
      int wifiSwitch = this.getSwitchMode();
      wifiSettingsElem.addAttr("wifiSwitch", String.valueOf(wifiSwitch));
      String monitorString = cat(this.filter, "/var/run/wilink");
      String wifistate = monitorString != null ? monitorString.trim() : null;
      if (wifistate != null) {
         wifiSettingsElem.addAttr("wifimonState", wifistate);
      } else {
         wifiSettingsElem.addAttr("wifimonState", "unknown");
      }

      boolean wifiEnabled = new File("/opt/niagara/platform/wifi/use-wifi").exists();
      wifiSettingsElem.addAttr("wifiEnabled", String.valueOf(wifiEnabled));
      XElem stationSettings = new XElem("stationSettings");
      String wpa_state = "unknown";
      String ssid = "unknown";
      String address = "unknown";
      String ip_address = "unknown";
      int statusOrdinal = 0;
      boolean isSupplicantUp = wifistate != null
         && (
            wifistate.equals("sta_supplicant_running")
               || wifistate.equals("sta_disconnected")
               || wifistate.equals("sta_associating")
               || wifistate.equals("sta_associated")
               || wifistate.equals("sta_4way_handshake")
               || wifistate.equals("sta_scanning")
               || wifistate.equals("sta_running")
         );
      if (isSupplicantUp) {
         String statusOutput = wpa_cli(this.filter, new String[]{"-i", "tiw_sta0", "status"});
         String[] statusLines = statusOutput != null ? statusOutput.split("\n") : new String[0];

         for (String statusLine : statusLines) {
            if (statusLine.startsWith("wpa_state=")) {
               wpa_state = statusLine.split("=")[1];
            } else if (statusLine.startsWith("ssid=")) {
               ssid = statusLine.split("=")[1];
            } else if (statusLine.startsWith("address")) {
               address = statusLine.split("=")[1];
            } else if (statusLine.startsWith("ip_address")) {
               ip_address = statusLine.split("=")[1];
            }
         }

         switch (wpa_state) {
            case "COMPLETED":
               statusOrdinal = 9;
               break;
            case "GROUPHANDSHAKE":
               statusOrdinal = 8;
               break;
            case "4WAY_HANDSHAKE":
               statusOrdinal = 7;
               break;
            case "ASSOCIATED":
               statusOrdinal = 6;
               break;
            case "ASSOCIATING":
               statusOrdinal = 5;
               break;
            case "SCANNING":
               statusOrdinal = 4;
               break;
            case "IDLE":
               statusOrdinal = 3;
               break;
            case "DISCONNECTED":
               statusOrdinal = 2;
               break;
            default:
               statusOrdinal = 0;
         }

         if (ssid.equals("unknown")) {
            String listNetworks = wpa_cli(this.filter, new String[]{"-i", "tiw_sta0", "list_networks"});
            String[] dotConfNetworks = listNetworks != null ? listNetworks.split("\n") : new String[0];

            for (int i = 1; i < dotConfNetworks.length; i++) {
               if (dotConfNetworks[i].contains("[CURRENT]")) {
                  ssid = dotConfNetworks[i].split("\t")[1];
                  break;
               }
            }
         }
      }

      stationSettings.addAttr("staAdapter", "tiw_sta0");
      stationSettings.addAttr("wpa_state", String.valueOf(statusOrdinal));
      stationSettings.addAttr("ssid", ssid);
      stationSettings.addAttr("address", address);
      stationSettings.addAttr("ip_address", ip_address);
      File gwoptionsfile = new File("/opt/niagara/platform/wifi/dhclient-options");
      if (gwoptionsfile.exists()) {
         String gwoptions = cat(this.filter, "/opt/niagara/platform/wifi/dhclient-options");
         if (gwoptions != null && gwoptions.contains("--no-def-route")) {
            stationSettings.addAttr("gwswitch", "false");
         } else {
            stationSettings.addAttr("gwswitch", "true");
         }
      } else {
         stationSettings.addAttr("gwswitch", "true");
      }

      wifiSettingsElem.addContent(stationSettings);
      XElem accessPointSettings = new XElem("accessPointSettings");
      accessPointSettings.addAttr("sapAdapter", "tiw_sap0");
      String sapTimeoutStr = cat(this.filter, "/opt/niagara/platform/wifi/sap_to");
      accessPointSettings.addAttr("sap_to", sapTimeoutStr != null ? sapTimeoutStr.trim() : "");
      wifiSettingsElem.addContent(accessPointSettings);
      ByteBuffer buffer = new ByteBuffer();
      XWriter out = new XWriter();
      out.setOutputStream(buffer.getOutputStream());
      out.prolog();
      wifiSettingsElem.write(out);
      out.flush();
      out.close();
      String wifiSettingsString = new String(buffer.getBytes(), 0, buffer.getLength());
      content.write(wifiSettingsString);
      return 200;
   }

   private int handleSetSapTimeout(ErrorHandler handler, KeyedList query) {
      String timeoutStr = query.get("setSapTimeout", null);

      int timeout;
      try {
         timeout = Integer.parseInt(timeoutStr);
      } catch (NumberFormatException nfe) {
         MessageBundle msg = new MessageBundle("Error setting sap timeout: " + timeoutStr);
         handler.error(msg);
         this.filter.severe("error occurred setting sap timeout: " + timeoutStr);
         return 400;
      }

      try (FileWriter writer = new FileWriter("/opt/niagara/platform/wifi/sap_to")) {
         writer.write(String.valueOf(timeout));
         writer.flush();
         return 200;
      } catch (IOException e) {
         MessageBundle msg = new MessageBundle("Error setting sap timeout: " + timeoutStr + " (" + e + ")");
         handler.error(msg);
         this.filter.log(Level.SEVERE, "error occurred setting sap timeout: " + timeoutStr + " (" + e + ")", e);
         return 500;
      }
   }

   private int handleSetCountryCode(ErrorHandler handler, KeyedList query) {
      String cc = query.get("setCountryCode", null);
      String wifiSku = getWifiSkuCode(this.filter);
      if (wifiSku == null) {
         MessageBundle msg = new MessageBundle("Error, invalid wifi SKU code");
         handler.error(msg);
         this.filter.severe("invalid wifi SKU code");
         return 500;
      } else if (wifiSku.equals("US")) {
         MessageBundle msg = new MessageBundle("Error, not allowed to set cc for US");
         handler.error(msg);
         this.filter.severe("not allowed to set cc for US");
         return 400;
      } else {
         List<String> command = new ArrayList<>();
         command.add("/proc/boot/wifi-ccutil");
         command.add("-w");
         command.add(cc);
         StringBuilder output = new StringBuilder();
         int rc = execute(this.filter, command.toArray(new String[0]), output);
         if (rc != 0) {
            MessageBundle msg = new MessageBundle("Error setting country code: " + cc);
            handler.error(msg);
            this.filter.severe("error occurred setting country code: " + cc);
            return 500;
         } else {
            return 200;
         }
      }
   }

   public int getSwitchMode() {
      String staValue = cat(this.filter, "/opt/gpio/switches/wifi/sta");
      if (staValue != null && staValue.trim().equalsIgnoreCase("0")) {
         return 1;
      }

      String sapValue = cat(this.filter, "/opt/gpio/switches/wifi/sap");
      return sapValue != null && sapValue.trim().equalsIgnoreCase("0") ? 2 : 0;
   }

   private static String wpa_cli(Logger log, String[] arguments) {
      StringBuilder output = new StringBuilder();
      List<String> command = new ArrayList<>();
      command.add("/proc/boot/wpa_cli");
      Collections.addAll(command, arguments);
      return execute(log, command.toArray(new String[0]), output) != 0 ? null : output.toString();
   }

   private static String hostapd_cli(Logger log, String[] arguments) {
      StringBuilder output = new StringBuilder();
      List<String> command = new ArrayList<>();
      command.add("/proc/boot/hostapd_cli");
      Collections.addAll(command, arguments);
      return execute(log, command.toArray(new String[0]), output) != 0 ? null : output.toString();
   }

   private static String cat(Logger log, String filePath) {
      StringBuilder output = new StringBuilder();
      List<String> command = new ArrayList<>();
      command.add("/proc/boot/cat");
      command.add(filePath);
      return execute(log, command.toArray(new String[0]), output) != 0 ? null : output.toString();
   }

   private static String getCountryCode(Logger log) {
      StringBuilder output = new StringBuilder();
      List<String> command = new ArrayList<>();
      command.add("/proc/boot/wifi-ccutil");
      int rc = execute(log, command.toArray(new String[0]), output);
      if (rc != 0) {
         return null;
      }

      String cc = output.toString().trim();
      return cc.length() != 2 ? null : cc;
   }

   private static String getWifiSkuCode(Logger log) {
      StringBuilder output = new StringBuilder();
      List<String> command = new ArrayList<>();
      command.add("/proc/boot/wifi-skuread");
      int rc = execute(log, command.toArray(new String[0]), output);
      if (rc != 0) {
         return null;
      } else {
         String sku = output.toString().trim();
         if (sku.length() > 2) {
            String[] skuwords = sku.split(" ");
            return skuwords[0].length() == 2 ? skuwords[0] : null;
         } else {
            return sku.length() == 2 ? sku : null;
         }
      }
   }

   private static int execute(Logger log, String[] arguments, StringBuilder standardOutputBuffer) {
      int rc;
      try {
         rc = AccessController.doPrivileged(() -> {
            Process process = Runtime.getRuntime().exec(arguments);
            if (standardOutputBuffer != null) {
               char[] buffer = new char[512];
               int charactersRead = 0;

               try (BufferedReader stdoutReader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                  while ((charactersRead = stdoutReader.read(buffer)) != -1) {
                     standardOutputBuffer.append(new String(buffer, 0, charactersRead));
                  }
               }
            }

            while (process.isAlive()) {
               try {
                  process.waitFor();
               } catch (InterruptedException var16) {
               }
            }

            return process.exitValue();
         });
      } catch (PrivilegedActionException pae) {
         log.log(Level.SEVERE, "failed to execute requested command '" + arguments[0] + " (" + pae.getException() + ")", pae.getException());
         rc = -1;
      }

      return rc;
   }

   class Ccinfo implements Comparable<QnxWiFiServlet.Ccinfo> {
      private final String code;
      private final String name;

      public Ccinfo(String code, String name) {
         this.name = name;
         this.code = code;
      }

      public String getCode() {
         return this.code;
      }

      public String getName() {
         return this.name;
      }

      @Override
      public boolean equals(Object o) {
         if (!(o instanceof QnxWiFiServlet.Ccinfo)) {
            return false;
         }

         QnxWiFiServlet.Ccinfo info = (QnxWiFiServlet.Ccinfo)o;
         return info.name.equals(this.name) && info.code.equals(this.code);
      }

      @Override
      public int hashCode() {
         return 255 * this.name.hashCode() + this.code.hashCode();
      }

      @Override
      public String toString() {
         StringBuilder sb = new StringBuilder();
         sb.append(this.code);
         String country = this.code.replace('_', ' ');
         sb.append(country);
         return sb.toString();
      }

      public int compareTo(QnxWiFiServlet.Ccinfo info) {
         return this.name.compareTo(info.name);
      }
   }
}
