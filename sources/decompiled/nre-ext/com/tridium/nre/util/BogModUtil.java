package com.tridium.nre.util;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;
import javax.baja.xml.XElem;
import javax.baja.xml.XParser;
import javax.baja.xml.XParserEvent;
import javax.baja.xml.XParserEventElementWriter;
import javax.baja.xml.XParserEventGenerator;
import javax.baja.xml.XParserEventListener;
import javax.baja.xml.XPath;
import javax.baja.xml.XPathElem;
import javax.baja.xml.XWriter;

public class BogModUtil {
   private static final Logger logger = Logger.getLogger(BogModUtil.class.getName());

   public static void replaceValues(String bogFileName, Map<BogModUtil.SlotInfo, String> replacements) {
      Path bogFilePath = null;
      Path newBogFilePath = null;

      try {
         bogFilePath = Paths.get(bogFileName);
         newBogFilePath = Files.createTempFile(bogFilePath.getParent(), "config", ".bog");
      } catch (IOException e) {
         throw new UncheckedIOException("Unable to create temporary bog file", e);
      }

      try (
         ZipFile zipFile = new ZipFile(bogFileName);
         ZipOutputStream output = new ZipOutputStream(new FileOutputStream(newBogFilePath.toFile()));
         XWriter writer = new XWriter(output);
      ) {
         ZipEntry inputEntry = zipFile.getEntry("file.xml");
         if (inputEntry == null) {
            throw new IOException("Invalid bog file format: file.xml not found");
         }

         output.putNextEntry(new ZipEntry("file.xml"));
         InputStream input = zipFile.getInputStream(inputEntry);
         XParser parser = null;

         try {
            parser = XParser.make(input, true);
         } catch (Exception e) {
            throw new RuntimeException("Unable to create XParser", e);
         }

         replaceValues(parser, writer, replacements);
         writer.flush();
         input.close();
      } catch (IOException e) {
         throw new UncheckedIOException(e);
      }

      try {
         Files.move(newBogFilePath, bogFilePath, StandardCopyOption.REPLACE_EXISTING);
      } catch (IOException e) {
         throw new UncheckedIOException("Unable to rename bog file from " + newBogFilePath + " to " + bogFilePath, e);
      }
   }

   public static void replaceValues(XParser parser, final XWriter writer, final Map<BogModUtil.SlotInfo, String> replacements) {
      final XParserEventGenerator eventGenerator = new XParserEventGenerator(parser);
      final Set<String> unresolvedModules = new HashSet<>();
      final List<BogModUtil.SlotInfo> unresolvedSlots = new LinkedList<>();
      final Map<String, String> moduleAliases = new HashMap<>();

      for (BogModUtil.SlotInfo slotInfo : replacements.keySet()) {
         unresolvedSlots.add(slotInfo);
         unresolvedModules.addAll(slotInfo.modules);
      }

      XParserEventListener typeAliasListener = new XParserEventListener() {
         @Override
         public void handleEvent(XParserEvent event) {
            String[] values = ((XElem)event.getContent()).get("m").split("=");
            if (values.length == 2) {
               String moduleAlias = values[0];
               String moduleName = values[1];
               if (unresolvedModules.contains(moduleName)) {
                  BogModUtil.logger.fine(() -> String.format("Found mapping of module [%s] to module alias [%s]", moduleName, moduleAlias));
                  unresolvedModules.remove(moduleName);
                  moduleAliases.put(moduleName, moduleAlias);
                  Set<String> resolvedModules = moduleAliases.keySet();
                  Iterator<BogModUtil.SlotInfo> i = unresolvedSlots.iterator();

                  while (i.hasNext()) {
                     BogModUtil.SlotInfo slotInfo = i.next();
                     if (resolvedModules.containsAll(slotInfo.modules)) {
                        BogModUtil.registerValueChangeHandlers(eventGenerator, slotInfo, moduleAliases, replacements.get(slotInfo), writer);
                        i.remove();
                     }
                  }
               }

               if (unresolvedModules.size() == 0) {
                  eventGenerator.removeListener(this);
               }
            }
         }

         @Override
         public String toString() {
            return "typeAliasListener";
         }
      };
      eventGenerator.addListener(1, new XPath(new XPathElem("p", true).withAttr("m")), typeAliasListener);
      eventGenerator.addListener(new XParserEventElementWriter(writer));
      writer.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
      eventGenerator.run();
   }

   private static void registerValueChangeHandlers(
      final XParserEventGenerator eventGenerator,
      final BogModUtil.SlotInfo slotInfo,
      final Map<String, String> moduleAliases,
      final String newValue,
      final XWriter writer
   ) {
      logger.fine(() -> String.format("Registering change value handlers for %s", slotInfo));
      XPathElem componentXPathElem = new XPathElem("p", true).withAttr("t", slotInfo.getQualifiedComponentType(moduleAliases));
      XPathElem slotXPathElem = new XPathElem("p").withAttr("n", slotInfo.slotName);
      if (slotInfo.slotHasType()) {
         slotXPathElem.withAttr("t", slotInfo.getQualifiedSlotType(moduleAliases));
      }

      final AtomicBoolean existingValueFound = new AtomicBoolean(false);
      final XParserEventListener updateElementHandler = new XParserEventListener() {
         @Override
         public void handleEvent(XParserEvent event) {
            BogModUtil.logger.fine(() -> String.format("updateElementHandler called for %s", slotInfo));
            ((XElem)event.getContent()).setAttr("v", newValue);
            existingValueFound.set(true);
            eventGenerator.removeListener(this);
         }

         @Override
         public String toString() {
            return "updateElementHandler[" + slotInfo + "]";
         }
      };
      eventGenerator.addListener(1, new XPath(componentXPathElem, slotXPathElem), updateElementHandler);
      XParserEventListener createElementHandler = new XParserEventListener() {
         @Override
         public void handleEvent(XParserEvent event) {
            BogModUtil.logger.fine(() -> String.format("createElementHandler called for %s", slotInfo));
            if (!existingValueFound.get()) {
               BogModUtil.logger.fine(() -> String.format("createElementHandler - creating new element for %s", slotInfo));
               XElem newElem = new XElem("p").addAttr("n", slotInfo.slotName).addAttr("v", newValue);
               if (slotInfo.slotHasType()) {
                  newElem.addAttr("t", slotInfo.getQualifiedSlotType(moduleAliases));
               }

               newElem.write(writer, 0, true);
               writer.write("\n");
            } else {
               BogModUtil.logger.fine(() -> String.format("createElementHandler - element already updated for %s", slotInfo));
            }

            eventGenerator.removeListener(this);
            eventGenerator.removeListener(updateElementHandler);
         }

         @Override
         public String toString() {
            return "createElementHandler[" + slotInfo + "]";
         }
      };
      eventGenerator.addListener(2, new XPath(componentXPathElem), createElementHandler);
   }

   public static class SlotInfo {
      private String componentModule;
      private String componentType;
      private String slotModule;
      private String slotType;
      private String slotName;
      private Set<String> modules = new HashSet<>();

      public SlotInfo(String componentModule, String componentType, String slotModule, String slotType, String slotName) {
         this.componentModule = componentModule;
         this.componentType = componentType;
         this.slotModule = slotModule;
         this.slotType = slotType;
         this.slotName = slotName;
         this.modules.add(componentModule);
         if (slotType != null) {
            this.modules.add(slotModule);
         }
      }

      public boolean slotHasType() {
         return this.slotModule != null && this.slotType != null;
      }

      public String getQualifiedSlotType(Map<String, String> moduleAliases) {
         return moduleAliases.get(this.slotModule) + ":" + this.slotType;
      }

      public String getQualifiedComponentType(Map<String, String> moduleAliases) {
         return moduleAliases.get(this.componentModule) + ":" + this.componentType;
      }

      @Override
      public boolean equals(Object o) {
         if (this == o) {
            return true;
         }

         if (o != null && this.getClass() == o.getClass()) {
            BogModUtil.SlotInfo slotInfo = (BogModUtil.SlotInfo)o;
            if (this.componentModule != null ? this.componentModule.equals(slotInfo.componentModule) : slotInfo.componentModule == null) {
               if (this.componentType != null ? this.componentType.equals(slotInfo.componentType) : slotInfo.componentType == null) {
                  if (this.slotModule != null ? this.slotModule.equals(slotInfo.slotModule) : slotInfo.slotModule == null) {
                     if (this.slotType != null ? this.slotType.equals(slotInfo.slotType) : slotInfo.slotType == null) {
                        return this.slotName != null ? this.slotName.equals(slotInfo.slotName) : slotInfo.slotName == null;
                     } else {
                        return false;
                     }
                  } else {
                     return false;
                  }
               } else {
                  return false;
               }
            } else {
               return false;
            }
         } else {
            return false;
         }
      }

      @Override
      public int hashCode() {
         int result = this.componentModule != null ? this.componentModule.hashCode() : 0;
         result = 31 * result + (this.componentType != null ? this.componentType.hashCode() : 0);
         result = 31 * result + (this.slotModule != null ? this.slotModule.hashCode() : 0);
         result = 31 * result + (this.slotType != null ? this.slotType.hashCode() : 0);
         return 31 * result + (this.slotName != null ? this.slotName.hashCode() : 0);
      }

      @Override
      public String toString() {
         StringBuilder sb = new StringBuilder("SlotInfo{");
         sb.append("componentModule='").append(this.componentModule).append('\'');
         sb.append(", componentType='").append(this.componentType).append('\'');
         sb.append(", slotModule='").append(this.slotModule).append('\'');
         sb.append(", slotType='").append(this.slotType).append('\'');
         sb.append(", slotName='").append(this.slotName).append('\'');
         sb.append(", modules=").append(this.modules);
         sb.append('}');
         return sb.toString();
      }
   }
}
