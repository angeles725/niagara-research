package com.tridium.nre.util;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.security.AccessController;
import java.util.Date;
import javax.baja.nre.util.Array;

public class SystemPropertiesUtil {
   public static synchronized boolean setSystemProperty(String key, String value) {
      if (key != null && value != null) {
         String niagaraHome = AccessController.doPrivileged(() -> System.getProperty("niagara.home"));
         if (niagaraHome == null) {
            throw new RuntimeException("'niagara.home' not defined, can not set system.properties value");
         }

         System.setProperty(key, value);
         BufferedReader fin = null;
         BufferedWriter fout = null;
         String sysPropsString = NiagaraFiles.getSystemPropertiesPath().toString();
         File sysPropsFile = new File(sysPropsString);

         try {
            fin = new BufferedReader(new FileReader(sysPropsFile));
            Array<String> lines = new Array<>(String.class);
            boolean foundKey = false;

            String currentLine;
            while ((currentLine = fin.readLine()) != null) {
               if (currentLine.startsWith(key + "=") || currentLine.startsWith("#" + key + "=")) {
                  foundKey = true;
                  currentLine = key + "=" + value;
               }

               lines.add(currentLine);
            }

            if (!foundKey) {
               lines.add(key + "=" + value);
            }

            fin.close();
            fout = new BufferedWriter(new FileWriter(sysPropsFile));

            for (int i = 0; i < lines.size(); i++) {
               fout.write(lines.get(i) + "\n");
            }

            fout.write("\r\n");
            fout.flush();
         } catch (FileNotFoundException fnfe) {
            System.err.println("WARNING [" + new Date() + "][nre] cannot save " + sysPropsFile + " (" + fnfe + ")");
            return false;
         } catch (Throwable e) {
            System.err.println("SEVERE [" + new Date() + "][nre] cannot save " + sysPropsFile + " (" + e + ")");
            return false;
         } finally {
            try {
               if (fin != null) {
                  fin.close();
               }
            } catch (Exception var25) {
            }

            try {
               if (fout != null) {
                  fout.close();
               }
            } catch (Exception var24) {
            }
         }

         return true;
      } else {
         return false;
      }
   }
}
