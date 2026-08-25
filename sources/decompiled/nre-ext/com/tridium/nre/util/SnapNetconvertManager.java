package com.tridium.nre.util;

import com.tridium.nre.platform.PlatformUtil;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.util.ArrayList;
import java.util.List;

public final class SnapNetconvertManager {
   private static final int MAXIMUM_ARGUMENT_LENGTH = 8192;
   private static final String WRITE_ARG = "-w";
   private static final String FILE_ARG = "-f";

   public static String getHostFileName() {
      return "/etc/hosts";
   }

   public static String getNetworkSettingsXML() {
      StringBuilder output = new StringBuilder();
      List<String> command = new ArrayList<>();
      command.add(SnapNetconvertManager.NetconvertExecutablePathHolder.EXECUTABLE_NAME);
      return execute(command.toArray(new String[0]), output) != 0 ? null : output.toString();
   }

   public static int setNetworkSettingsXML(String networkSettingsXML) {
      if (networkSettingsXML == null) {
         return -1;
      }

      if (networkSettingsXML.length() > 8192) {
         return -1;
      }

      try (BufferedWriter tempXmlWriter = new BufferedWriter(
            new OutputStreamWriter(new FileOutputStream(SnapNetconvertManager.NetconvertExecutablePathHolder.TEMPORARY_FILE_PATH), StandardCharsets.UTF_8)
         )) {
         tempXmlWriter.write(networkSettingsXML);
      } catch (IOException ioe) {
         ioe.printStackTrace();
         return -1;
      }

      return execute(
         new String[]{
            SnapNetconvertManager.NetconvertExecutablePathHolder.EXECUTABLE_NAME,
            "-w",
            "-f",
            SnapNetconvertManager.NetconvertExecutablePathHolder.TEMPORARY_FILE_PATH
         },
         null
      );
   }

   private static int execute(String[] arguments, StringBuilder standardOutputBuffer) {
      int rc;
      try {
         rc = AccessController.doPrivileged(() -> {
            Process process = Runtime.getRuntime().exec(arguments);
            if (standardOutputBuffer != null) {
               String line;
               try (BufferedReader stdoutReader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                  while ((line = stdoutReader.readLine()) != null) {
                     standardOutputBuffer.append(line);
                  }
               }
            }

            return process.waitFor();
         });
      } catch (PrivilegedActionException pae) {
         pae.printStackTrace();
         rc = -1;
      }

      return rc;
   }

   private static class NetconvertExecutablePathHolder {
      private static final String EXECUTABLE_NAME = AccessController.doPrivileged(
         () -> System.getenv("SNAP") + File.separator + "bin" + File.separator + "netconvert"
      );
      private static final String TEMPORARY_FILE_PATH = AccessController.doPrivileged(
         () -> PlatformUtil.getPlatformProvider().getTempDirPath() + File.separator + "tcpIpSettings.xml"
      );
   }
}
