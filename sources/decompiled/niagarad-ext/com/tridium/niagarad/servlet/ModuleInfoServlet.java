package com.tridium.niagarad.servlet;

import com.tridium.crypto.core.cert.JarSignatureRegistry;
import com.tridium.niagarad.NiagaraDaemon;
import com.tridium.niagarad.log.ErrorHandler;
import com.tridium.niagarad.util.KeyedList;
import com.tridium.niagarad.util.XModuleInfoParser;
import com.tridium.nre.platform.IPlatformProvider;
import com.tridium.nre.util.NiagaraFiles;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectOutputStream;
import java.security.CodeSigner;
import java.security.Timestamp;
import java.security.cert.CertPath;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import javax.baja.xml.XElem;
import javax.baja.xml.XParser;
import javax.baja.xml.XWriter;
import javax.servlet.http.HttpServletRequest;

public class ModuleInfoServlet extends DaemonServlet {
   private final JarSignatureRegistry jarSignatureRegistry;
   private String modulesDirectory = null;

   public ModuleInfoServlet(JarSignatureRegistry jarSignatureRegistry, IPlatformProvider platformProvider) {
      super("moduleInfo");
      this.jarSignatureRegistry = jarSignatureRegistry;
      this.modulesDirectory = platformProvider.isNiagaraHomeReadonly() ? "/niagara/modules" : "/niagara_user/modules";
   }

   public ModuleInfoServlet(IPlatformProvider platformProvider) {
      this(NiagaraDaemon.getInstance().getSignatureRegistry(), platformProvider);
   }

   @Override
   public int doGet(HttpServletRequest request, ErrorHandler handler, KeyedList query, XWriter content) {
      boolean depsOnly = false;
      String signers = null;
      String moduleName = null;
      boolean sendCrc;
      boolean fullInfo;
      if (query == null) {
         sendCrc = false;
         fullInfo = false;
      } else {
         sendCrc = Boolean.parseBoolean(query.get("sendCrc", "false"));
         fullInfo = Boolean.parseBoolean(query.get("full", "false"));
         depsOnly = Boolean.parseBoolean(query.get("depsOnly", "false"));
         signers = query.get("signers", null);
         moduleName = query.get("moduleName", null);
      }

      if (this.jarSignatureRegistry == null && "required".equals(signers)) {
         handler.error("Cannot request module info with signers=required when signature registry is absent.");
         this.getServer().getFilter().warning(this.getName() + ": cannot request module info with signers=required when signature registry is absent.");
         return 400;
      }

      if (NiagaraDaemon.getInstance() != null
         && NiagaraDaemon.getInstance().getStationRegistry() != null
         && NiagaraDaemon.getInstance().getStationRegistry().appRunning()
         && "required".equals(signers)) {
         handler.error("Cannot request module info with signers=required when station running.");
         this.getServer().getFilter().warning(this.getName() + ": cannot request module info with signers=required when station running.");
         return 400;
      }

      File moduleDirectory = NiagaraFiles.getModulesPath();
      boolean responseWritten = false;
      boolean byName = moduleName != null && !moduleName.isEmpty();
      List<CertPath> certPathList = new ArrayList<>();
      if (moduleDirectory.exists()) {
         File[] list = moduleDirectory.listFiles();
         if (list != null) {
            Arrays.sort(list);
            boolean newContent = false;

            for (File current : list) {
               File item = current;
               newContent = false;
               if ((endsWith(item.getName(), ".jar") || endsWith(item.getName(), ".sjar"))
                  && (!byName || item.getName().equalsIgnoreCase(moduleName + ".jar") || item.getName().equalsIgnoreCase(moduleName + ".sjar"))) {
                  if (!responseWritten) {
                     content.w("<moduleList>\n");
                  }

                  this.sendModuleInfo(content, this.modulesDirectory, item, sendCrc, fullInfo, depsOnly, signers, certPathList, this.jarSignatureRegistry);
                  newContent = true;
                  if (byName) {
                     responseWritten = true;
                     break;
                  }
               }

               if (newContent) {
                  this.sendResponse(request, this.response, handler, null, content, true);
                  responseWritten = true;
                  if (handler.getLastError() != null) {
                     if (this.getServer().getState() == 1 && this.getServer().getFilter().isLoggable(Level.FINE)) {
                        this.getServer()
                           .getFilter()
                           .fine(
                              this.getName()
                                 + ": error occurred sending module information \""
                                 + handler.getLastError().getNonLocalizedMessage()
                                 + "\", skipping remaining response"
                           );
                     }

                     return 500;
                  }
               }
            }
         }
      }

      try {
         if (!certPathList.isEmpty()) {
            content.w("<certPaths>\n");

            for (CertPath certPath : certPathList) {
               ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
               ObjectOutputStream oos = new ObjectOutputStream(byteArrayOutputStream);
               oos.writeObject(certPath);
               byte[] certPathBytes = byteArrayOutputStream.toByteArray();
               String certPathBase64 = Base64.getEncoder().encodeToString(certPathBytes);
               content.w("<certPath>").w(certPathBase64).w("</certPath>\n");
            }

            content.w("</certPaths>\n");
         }
      } catch (IOException e) {
         handler.error("error writing certPaths (" + e + ")");
         Logger logger = this.getServer().getFilter();
         logger.severe(this.getName() + ": error writing certPaths (" + e + ")");
         if (logger.isLoggable(Level.FINE)) {
            logger.log(Level.FINE, "Stack trace: ", e);
         }

         return 500;
      }

      if (!responseWritten) {
         if (byName) {
            return 404;
         }

         content.w("<moduleList>\n");
      }

      content.w("</moduleList>\n");
      return 200;
   }

   private void sendModuleInfo(
      XWriter content,
      String moduleDir,
      File item,
      boolean sendCrc,
      boolean fullInfo,
      boolean depsOnly,
      String signers,
      List<CertPath> certPathList,
      JarSignatureRegistry signatureRegistry
   ) {
      try (ZipFile zipFile = new ZipFile(item)) {
         ZipEntry entry = zipFile.getEntry("META-INF/module.xml");
         if (entry == null) {
            entry = zipFile.getEntry("meta-inf/module.xml");
         }

         if (entry == null) {
            content.w("<moduleItem")
               .w(' ')
               .attr("path", moduleDir + "/" + item.getName())
               .w(' ')
               .attr("size", String.valueOf(item.length()))
               .w(' ')
               .attr("status", "nomanifest")
               .w("/>\n");
         } else {
            try (InputStream entryIn = zipFile.getInputStream(entry)) {
               XParser parser;
               if (depsOnly) {
                  parser = new XModuleInfoParser(entryIn);
               } else {
                  parser = XParser.make(entryIn);
               }

               XElem moduleElem = parser.parse();
               content.w("<moduleItem")
                  .w(' ')
                  .attr("path", moduleDir + "/" + item.getName())
                  .w(' ')
                  .attr("size", String.valueOf(item.length()))
                  .w(' ')
                  .attr("status", "ok");
               if (sendCrc) {
                  content.w(' ').attr("crc", String.valueOf(entry.getCrc()));
               }

               List<CodeSigner> codeSigners = null;
               if ("optional".equals(signers) && signatureRegistry != null) {
                  codeSigners = signatureRegistry.getCachedCodeSigners(item);
               } else if ("required".equals(signers)) {
                  if (signatureRegistry == null) {
                     throw new IllegalArgumentException("Signature registry is null when required");
                  }

                  codeSigners = signatureRegistry.getCodeSigners(item);
               }

               if (!fullInfo && codeSigners == null) {
                  content.w("/>\n");
               } else {
                  if (codeSigners == null) {
                     content.w(">\n");
                  } else {
                     content.w(' ').attr("signatureFailureCause", signatureRegistry.getSignatureFailureCause(item));
                     content.w(">\n");
                     if (codeSigners.isEmpty()) {
                        content.w("<signers/>\n");
                     } else {
                        content.w("<signers>\n");

                        for (CodeSigner signer : codeSigners) {
                           CertPath signerCertPath = signer.getSignerCertPath();
                           int certPathIndex = certPathList.indexOf(signerCertPath);
                           if (certPathIndex == -1) {
                              certPathList.add(signerCertPath);
                              certPathIndex = certPathList.size() - 1;
                           }

                           content.w("<signer ").attr("certPathIndex", String.valueOf(certPathIndex));
                           Timestamp timestamp = signer.getTimestamp();
                           if (timestamp != null) {
                              CertPath timestampCertPath = timestamp.getSignerCertPath();
                              int timestampCertPathIndex = certPathList.indexOf(timestampCertPath);
                              if (timestampCertPathIndex == -1) {
                                 certPathList.add(timestampCertPath);
                                 timestampCertPathIndex = certPathList.size() - 1;
                              }

                              long timestampTime = timestamp.getTimestamp().getTime();
                              content.w(' ')
                                 .attr("timestampTime", String.valueOf(timestampTime))
                                 .w(' ')
                                 .attr("timestampCertPathIndex", String.valueOf(timestampCertPathIndex));
                           }

                           content.w("/>\n");
                        }

                        content.w("</signers>\n");
                     }
                  }

                  if (fullInfo) {
                     moduleElem.write(content);
                  }

                  content.w("</moduleItem>\n");
               }
            } catch (Exception e) {
               this.getServer().getFilter().log(Level.SEVERE, "failed to parse the module manifest for '" + item.getName() + "' (" + e + ")", e);
               content.w("<moduleItem")
                  .w(' ')
                  .attr("path", moduleDir + "/" + item.getName())
                  .w(' ')
                  .attr("size", String.valueOf(item.length()))
                  .w(' ')
                  .attr("status", "corrupt")
                  .w("/>\n");
            }
         }
      } catch (IOException ioe) {
         content.w("<moduleItem")
            .w(' ')
            .attr("path", moduleDir + "/" + item.getName())
            .w(' ')
            .attr("size", String.valueOf(item.length()))
            .w(' ')
            .attr("status", "corrupt")
            .w("/>\n");
      }
   }

   private static boolean endsWith(String string, String ending) {
      return string.toLowerCase().endsWith(ending.toLowerCase());
   }
}
