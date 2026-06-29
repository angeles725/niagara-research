package com.tridium.bacnet.util.metrics.mstp;

import com.tridium.bacnet.stack.link.BBacnetLinkLayer;
import com.tridium.bacnet.stack.link.mstp.BBacnetMstpLinkLayer;
import com.tridium.bacnet.stack.network.BNetworkPort;
import com.tridium.bacnet.util.point.BPeriodicNumericPoint;
import com.tridium.bacnet.util.point.EventsPerSecond;
import com.tridium.platMstp.EmstpStats;
import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Scanner;
import java.util.concurrent.atomic.AtomicLong;
import javax.baja.nre.annotations.NiagaraProperties;
import javax.baja.nre.annotations.NiagaraProperty;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.status.BStatus;
import javax.baja.status.BStatusNumeric;
import javax.baja.status.BStatusValue;
import javax.baja.sys.BComponent;
import javax.baja.sys.BDouble;
import javax.baja.sys.BFacets;
import javax.baja.sys.BString;
import javax.baja.sys.BValue;
import javax.baja.sys.Context;
import javax.baja.sys.Property;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;
import javax.baja.units.BUnit;

@NiagaraType
@NiagaraProperties({@NiagaraProperty(
      name = "receivedTokens",
      type = "long",
      defaultValue = "0"
   ), @NiagaraProperty(
      name = "generatedTokens",
      type = "long",
      defaultValue = "0"
   ), @NiagaraProperty(
      name = "parseAllMetrics",
      type = "boolean",
      defaultValue = "false"
   ), @NiagaraProperty(
      name = "facets",
      type = "BFacets",
      defaultValue = "BFacets.makeNumeric(BUnit.getUnit(\"per second\"), 1)",
      override = true
   )})
public class BTokens extends BPeriodicNumericPoint implements EventsPerSecond {
   public static final Property receivedTokens = newProperty(0, 0, null);
   public static final Property generatedTokens = newProperty(0, 0, null);
   public static final Property parseAllMetrics = newProperty(0, false, null);
   public static final Property facets = newProperty(0, BFacets.makeNumeric(BUnit.getUnit("per second"), 1), null);
   public static final Type TYPE = Sys.loadType(BTokens.class);
   private final AtomicLong recTokens = new AtomicLong();
   private final AtomicLong genTokens = new AtomicLong();
   private long lastExecute = 0L;
   private InputStream inputStream = null;
   private volatile boolean fileNotFound = false;
   public static final String PREFIX_CHAR = "n";

   public long getReceivedTokens() {
      return this.getLong(receivedTokens);
   }

   public void setReceivedTokens(long v) {
      this.setLong(receivedTokens, v, null);
   }

   public long getGeneratedTokens() {
      return this.getLong(generatedTokens);
   }

   public void setGeneratedTokens(long v) {
      this.setLong(generatedTokens, v, null);
   }

   public boolean getParseAllMetrics() {
      return this.getBoolean(parseAllMetrics);
   }

   public void setParseAllMetrics(boolean v) {
      this.setBoolean(parseAllMetrics, v, null);
   }

   @Override
   public Type getType() {
      return TYPE;
   }

   public boolean isParentLegal(BComponent parent) {
      if (parent instanceof BNetworkPort) {
         BNetworkPort networkPort = (BNetworkPort)parent;
         BBacnetLinkLayer link;
         if ((link = networkPort.getLink()) != null) {
            return link.getClass().getCanonicalName().toLowerCase().contains("mstp");
         }
      }

      return false;
   }

   public void onExecute(BStatusValue o, Context cx) {
      long t0 = this.lastExecute;
      long t1 = System.currentTimeMillis();
      BBacnetMstpLinkLayer linkLayer = this.getMstpLinkLayer();
      if (linkLayer != null) {
         linkLayer.doGetStatistics();
         EmstpStats stats = linkLayer.getEmstpStats();
         if (stats != null) {
            String statsString = stats.toString();
            InputStream inputStream = new ByteArrayInputStream(statsString.getBytes());
            this.setInputStream(inputStream);
         }
      }

      long oldTokens = this.recTokens.get();
      if (this.inputStream == null) {
         this.readMstpDeviceFile();
      } else {
         this.readMstpMetrics(this.inputStream);
      }

      long newTokens = this.recTokens.get();
      BStatusNumeric out = (BStatusNumeric)o;
      if (t0 > 0L) {
         out.setValue(this.calculateEventsPerSecond(newTokens - oldTokens, t0, t1));
      }

      if (this.fileNotFound && (linkLayer == null || !linkLayer.getUseCoprocessor())) {
         out.setStatus(BStatus.make(this.getStatus(), 2));
      }

      this.lastExecute = t1;
   }

   private void readMstpDeviceFile() {
      AccessController.doPrivileged((PrivilegedAction<Void>)(() -> {
         try {
            String filename = this.getMstpOsName();
            if (filename == null) {
               this.fileNotFound = true;
            } else {
               FileInputStream fis = new FileInputStream(filename);
               this.readMstpMetrics(fis);
               this.fileNotFound = false;
            }
         } catch (FileNotFoundException var3) {
            this.fileNotFound = true;
         }

         return null;
      }));
   }

   public void readMstpMetrics(InputStream input) {
      Scanner scanner = null;

      try {
         scanner = new Scanner(input);

         while (scanner.hasNextLine()) {
            String line = scanner.nextLine();
            String[] keyValue = this.parse(line);
            if (keyValue != null) {
               this.addOrUpdateMetric(keyValue[0], keyValue[1]);
            }
         }
      } finally {
         if (scanner != null) {
            scanner.close();
         }
      }
   }

   public boolean isLineValid(String line) {
      return line != null && !line.isEmpty() && !line.startsWith("-----");
   }

   public String[] parse(String line) {
      if (!this.isLineValid(line)) {
         return null;
      } else {
         String[] split = line.split(":");
         if (split.length < 2) {
            return null;
         } else {
            String key = split[0];
            String value = split[1];
            key = key.trim().toLowerCase().replaceAll(" ", "");
            if (key.matches("[0-9].*")) {
               key = "n" + key;
            }

            split[0] = key;
            split[1] = value.trim();
            return split;
         }
      }
   }

   public void addOrUpdateMetric(String key, String value) {
      if ("receivedTokens".equalsIgnoreCase(key)) {
         long receivedTokens = Long.parseLong(value);
         this.recTokens.set(receivedTokens);
         this.setReceivedTokens(receivedTokens);
      } else if ("generatedTokens".equalsIgnoreCase(key)) {
         long generatedTokens = Long.parseLong(value);
         this.genTokens.set(generatedTokens);
         this.setGeneratedTokens(generatedTokens);
      } else if (this.getParseAllMetrics()) {
         BValue bValue = parseMetric(key, value);
         if (bValue != null) {
            Property existingProperty = null;
            if ((existingProperty = this.getProperty(key)) != null) {
               if (!this.get(existingProperty).equals(bValue)) {
                  this.set(existingProperty, bValue, null);
               }
            } else {
               this.add(key, bValue, 4);
            }
         }
      }
   }

   private static BValue parseMetric(String key, String value) {
      BValue bValue = null;
      Double parsed = null;

      try {
         parsed = Double.parseDouble(value);
         bValue = BDouble.make(parsed);
      } catch (NumberFormatException var5) {
         bValue = BString.make(value);
      }

      return bValue;
   }

   public void setInputStream(InputStream inputStream) {
      this.inputStream = inputStream;
   }

   private String getMstpOsName() {
      BNetworkPort networkPort = (BNetworkPort)this.getParent();
      if (networkPort != null) {
         BBacnetLinkLayer link = networkPort.getLink();
         if (link instanceof BBacnetMstpLinkLayer) {
            BBacnetMstpLinkLayer mstpLink = (BBacnetMstpLinkLayer)link;
            return "/dev/mstp" + mstpLink.getMstpTrunk();
         }
      }

      return null;
   }

   private BBacnetMstpLinkLayer getMstpLinkLayer() {
      BNetworkPort networkPort = (BNetworkPort)this.getParent();
      if (networkPort != null) {
         BBacnetLinkLayer link = networkPort.getLink();
         if (link instanceof BBacnetMstpLinkLayer) {
            return (BBacnetMstpLinkLayer)link;
         }
      }

      return null;
   }
}
