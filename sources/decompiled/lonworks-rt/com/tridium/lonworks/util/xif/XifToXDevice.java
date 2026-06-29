package com.tridium.lonworks.util.xif;

import com.tridium.lonworks.resource.CrossReference;
import com.tridium.lonworks.util.selfdoc.NodeSelfDoc;
import com.tridium.lonworks.xml.XConfigProperty;
import com.tridium.lonworks.xml.XLonData;
import com.tridium.lonworks.xml.XLonDataUtil;
import com.tridium.lonworks.xml.XLonDevice;
import com.tridium.lonworks.xml.XNetworkConfig;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Hashtable;
import java.util.StringTokenizer;
import java.util.Vector;
import javax.baja.file.BIFile;

public class XifToXDevice {
   Hashtable<String, String> nameHash = new Hashtable<>();
   private XifLineReader reader;
   private PrintStream out;
   private boolean readElems;
   private CrossReference cr;
   private byte[] valFile1 = null;
   private byte[] valFile2 = null;

   public static XLonDevice convert(String fileName, CrossReference cr) {
      try (XifLineReader reader = new XifLineReader(fileName)) {
         return convert(reader, System.out, cr);
      } catch (Exception var16) {
         throw new RuntimeException("Unable to access file " + fileName);
      }
   }

   public static XLonDevice convert(BIFile file, CrossReference cr) {
      return convert(file, System.out, cr);
   }

   public static XLonDevice convert(BIFile file, PrintStream out, CrossReference cr) {
      try (XifLineReader reader = new XifLineReader(file.getInputStream())) {
         return convert(reader, out, cr);
      } catch (Exception var17) {
         throw new RuntimeException("Unable to access file.");
      }
   }

   private static XLonDevice convert(XifLineReader reader, PrintStream out, CrossReference cr) {
      XifToXDevice xto = new XifToXDevice(reader, out, cr);
      return xto.parse();
   }

   private XifToXDevice(XifLineReader reader, PrintStream out, CrossReference cr) {
      this.reader = reader;
      this.out = out;
      this.readElems = true;
      this.cr = cr;
   }

   private XLonDevice parse() {
      XLonDevice dev = new XLonDevice();
      if (this.cr == null) {
         this.cr = new CrossReference(this.out);
      }

      DeviceDataParser deviceDataParser = new DeviceDataParser(this.reader, this.out);
      dev.deviceData = deviceDataParser.parse();
      String sd = dev.deviceData.nodeSelfID;
      NodeSelfDoc nodeSelfDoc = new NodeSelfDoc(sd);
      NvParser nvParser = new NvParser(this.reader, this.out, this.readElems, this.cr, nodeSelfDoc);
      boolean endOfFile = false;

      while (!endOfFile) {
         try {
            String line = this.reader.readXifLine();
            if (line.startsWith("VAR")) {
               this.setXdata(nvParser.parseNetwork(line), dev);
            }

            if (line.startsWith("TAG")) {
               this.setXdata(nvParser.parseTag(line), dev);
            }

            if (line.startsWith("FILE")) {
               this.parseFile(dev, line, sd);
            }

            if (line.startsWith("NVVAL")) {
               this.parseNciVal(dev);
            }
         } catch (IOException var8) {
            if (var8 instanceof EOFException) {
               endOfFile = true;
            } else {
               var8.printStackTrace();
            }
            break;
         }
      }

      if (this.valFile1 != null) {
         this.setInitValues(dev);
      }

      return dev;
   }

   private void setXdata(XLonData xdata, XLonDevice dev) {
      if (xdata != null) {
         xdata.setName(this.getUniqueName(xdata.getName()));
         dev.addAttribute(xdata.getName(), xdata);
      }
   }

   private void parseNciVal(XLonDevice dev) throws EOFException, IOException {
      XNetworkConfig[] ncis = dev.getNetworkConfigs();
      ByteArrayOutputStream os = new ByteArrayOutputStream(64);
      String line = this.reader.readLine();

      for (int i = 0; i < ncis.length; i++) {
         if (line == null || line.length() == 0) {
            System.out.println("Not enough entries in NVVAL");
            return;
         }

         os.reset();

         do {
            this.parseBytes(os, line);
            line = this.reader.readLine();
         } while (line != null && line.startsWith("\t"));

         byte[] a = os.toByteArray();
         if (!this.isZeros(a)) {
            ncis[i].init = a;
         }
      }
   }

   private boolean isZeros(byte[] a) {
      for (int i = 0; i < a.length; i++) {
         if (a[i] != 0) {
            return false;
         }
      }

      return true;
   }

   private void parseBytes(ByteArrayOutputStream os, String line) {
      boolean hex = line.indexOf(120) >= 0;
      int b = 0;
      int nibbles = 0;
      boolean removeHex = hex;

      for (int i = 0; i < line.length(); i++) {
         char c = line.charAt(i);
         if (c != '\\' && !Character.isWhitespace(c) && c != ',') {
            if (c != 'x' && c != 'X') {
               if (Character.digit(c, 16) < 0) {
                  break;
               }

               if (!removeHex) {
                  b = (b << 4) + Character.digit(c, 16);
                  if (++nibbles == 2) {
                     os.write(b);
                     nibbles = 0;
                  }
               }
            } else {
               removeHex = false;
            }
         } else {
            if (nibbles > 0) {
               os.write(b);
            }

            b = 0;
            nibbles = 0;
            if (hex) {
               removeHex = true;
            }
         }
      }

      if (nibbles != 0) {
         os.write(b);
      }
   }

   private void parseFile(XLonDevice dev, String line, String sd) throws EOFException, IOException {
      StringTokenizer st = new StringTokenizer(line);
      st.nextToken();
      st.nextToken();
      int index = Integer.decode(st.nextToken());
      int type = Integer.decode(st.nextToken());
      if (index == 0) {
         this.reader.readXifLine();
         ConfigFileParser configParser = new ConfigFileParser(this.reader, this.out, this.cr, sd);
         Vector<XConfigProperty> config = configParser.parse();

         for (int i = 0; i < config.size(); i++) {
            XConfigProperty cp = config.elementAt(i);
            cp.setName(this.getUniqueName(cp.getName()));
            dev.addAttribute(cp.getName(), cp);
         }
      } else if (type == 1) {
         if (index == 1) {
            this.valFile1 = this.parseValueFile();
         }

         if (index == 2) {
            this.valFile2 = this.parseValueFile();
         }
      }
   }

   private byte[] parseValueFile() throws EOFException, IOException {
      ByteArrayOutputStream os = new ByteArrayOutputStream(64);

      for (String line = this.reader.readLine(); line != null && line.length() > 0; line = this.reader.readLine()) {
         this.parseBytes(os, line);
      }

      return os.toByteArray();
   }

   private void setInitValues(XLonDevice dev) {
      boolean hasRO = this.valFile2 != null;
      if (hasRO) {
         dev.hasReadOnlyFile = true;
      }

      ByteArrayInputStream is1 = new ByteArrayInputStream(this.valFile1);
      ByteArrayInputStream is2 = hasRO ? new ByteArrayInputStream(this.valFile2) : null;
      XConfigProperty[] xcps = dev.getConfigProperties();

      for (int i = 0; i < xcps.length; i++) {
         int len = xcps[i].length * xcps[i].dimension;
         byte[] a;
         if (XLonDataUtil.isReadOnly(xcps[i].modifyFlag) && hasRO) {
            a = this.readBytes(is2, len);
         } else {
            a = this.readBytes(is1, len);
         }

         if (!this.isZeros(a)) {
            xcps[i].init = a;
         }
      }
   }

   private byte[] readBytes(ByteArrayInputStream is, int len) {
      byte[] a = new byte[len];

      for (int i = 0; i < len; i++) {
         a[i] = (byte)is.read();
      }

      return a;
   }

   private String getUniqueName(String name) {
      String n = name;
      int cnt = 1;

      while (this.nameHash.get(n) != null) {
         n = name + "_" + cnt++;
      }

      this.nameHash.put(n, n);
      if (cnt > 1) {
         this.out.println("Duplicate name " + name + " changed to " + n);
      }

      return n;
   }

   public static void main(String[] args) {
      String filename = args[0];
      if (filename == null) {
         System.out.println("must specify file to parse");
      }

      convert(filename, null);
   }
}
