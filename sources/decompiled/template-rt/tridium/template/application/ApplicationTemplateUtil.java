package com.tridium.template.application;

import com.tridium.install.BVersion;
import com.tridium.template.BConfigBinding;
import com.tridium.template.BTemplateConfig;
import com.tridium.template.file.BINtplFile;
import com.tridium.template.file.BNtplFile;
import com.tridium.template.file.TemplateFileUtil;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import javax.baja.file.BFileSpace;
import javax.baja.file.BIFile;
import javax.baja.file.BajaFileUtil;
import javax.baja.file.FilePath;
import javax.baja.fox.BFoxProxySession;
import javax.baja.naming.BISession;
import javax.baja.naming.BOrd;
import javax.baja.naming.BOrdList;
import javax.baja.naming.OrdQuery;
import javax.baja.naming.SlotPath;
import javax.baja.sys.BComponent;
import javax.baja.sys.BStation;
import javax.baja.sys.Context;
import javax.baja.sys.Property;
import javax.baja.sys.SlotCursor;
import javax.baja.user.BUser;
import javax.baja.util.BUuid;

public final class ApplicationTemplateUtil {
   private static String MY_USER_MACRO_NAME = "myuser";
   private static String MY_USER_MACRO_KEY = NameTree.MACRO_PREFIX + MY_USER_MACRO_NAME;
   private static NameTree defaultStationDesc;
   private static NameTree contextFreeDefaultStationDesc;

   private ApplicationTemplateUtil() {
   }

   public static BComponent[] findApplicationRoots(BStation station) {
      return findApplicationRoots(station, describeDefaultStation(null));
   }

   public static BComponent[] findApplicationRoots(BStation station, NameTree keepers) {
      ArrayList<BComponent> applicationRoots = new ArrayList<>();
      gatherApplicationRootsForBranch(station, keepers, applicationRoots);
      return applicationRoots.toArray(new BComponent[0]);
   }

   public static BComponent[] findComponents(BComponent root, NameTree toBeFound) {
      ArrayList<BComponent> components = new ArrayList<>();
      gatherComponents(root, toBeFound, components);
      return components.toArray(new BComponent[0]);
   }

   public static void deleteComponents(BComponent parent, NameTree toBeDeleted) {
      ArrayList<BComponent> componentsToBeDeleted = new ArrayList<>();
      gatherComponents(parent, toBeDeleted, componentsToBeDeleted);

      for (BComponent component : componentsToBeDeleted) {
         component.getParent().asComponent().remove(component);
      }
   }

   public static NameTree describeDefaultStation(Context context) {
      if (defaultStationDesc == null) {
         defaultStationDesc = new NameTree();
         NameTree services = defaultStationDesc.get("Services");
         services.get("UserService").add(MY_USER_MACRO_KEY);
         services.add("AuthenticationService");
         services.add("DebugService");
         services.add("FoxService");
         services.add("TemplateService");
         services.add("PlatformServices");
         services.add("HistoryService");
         services.add("JobService");
         services.add("WebService");
      }

      if (context != null && context.getUser() != null && context.getUser().getName() != null) {
         String userName = context.getUser().getName();
         NameTree userDefaultStationDesc = new NameTree(defaultStationDesc);
         userDefaultStationDesc.replaceMacro(MY_USER_MACRO_NAME, userName);
         return userDefaultStationDesc;
      } else {
         if (contextFreeDefaultStationDesc == null) {
            contextFreeDefaultStationDesc = new NameTree(defaultStationDesc);
            contextFreeDefaultStationDesc.removeMacro(MY_USER_MACRO_NAME);
         }

         return contextFreeDefaultStationDesc;
      }
   }

   public static NameTree makeNameTree(BOrdList slotPathOrds) {
      NameTree tree = new NameTree();

      for (BOrd ord : slotPathOrds) {
         if (!ord.isNull()) {
            OrdQuery[] queries = ord.parse();
            if (queries.length != 0) {
               OrdQuery last = queries[queries.length - 1];
               if (last instanceof SlotPath) {
                  SlotPath path = (SlotPath)last;
                  String[] names = path.getNames();
                  if (names.length != 0) {
                     NameTree subTree = tree;
                     int i = 0;

                     while (i < names.length - 1) {
                        subTree = subTree.get(names[i++]);
                     }

                     subTree.add(names[i]);
                  }
               }
            }
         }
      }

      return tree;
   }

   public static void purgeBrokenConfigProperties(BTemplateConfig templateConfig, Set<Object> allHandles) {
      if (templateConfig != null) {
         for (BConfigBinding configBinding : templateConfig.getConfigBindings()) {
            BOrd targetOrd = configBinding.getTargetOrd();
            OrdQuery[] queries = targetOrd.parse();
            if (queries.length > 0) {
               OrdQuery query = queries[queries.length - 1];
               if ("h".equals(query.getScheme()) && allHandles.contains(query.getBody())) {
                  continue;
               }
            }

            String sourceSlot = configBinding.getSourceSlot();
            templateConfig.remove(configBinding.getPropertyInParent());
            templateConfig.remove(sourceSlot);
         }
      }
   }

   private static void gatherApplicationRootsForBranch(BComponent subject, NameTree keepersBranch, ArrayList<BComponent> roots) {
      SlotCursor<Property> cursor = subject.getProperties();

      while (cursor.nextComponent()) {
         BComponent component = cursor.get().asComponent();
         String checkName = component.getName();
         if (keepersBranch.hasBranch(checkName)) {
            gatherApplicationRootsForBranch(component, keepersBranch.fetch(checkName), roots);
         } else if (component.getPropertyInParent().isDynamic() && !keepersBranch.hasLeaf(checkName)) {
            roots.add(component);
         }
      }
   }

   private static void gatherComponents(BComponent subject, NameTree branchToBeFound, ArrayList<BComponent> components) {
      SlotCursor<Property> cursor = subject.getProperties();

      while (cursor.nextComponent()) {
         BComponent component = cursor.get().asComponent();
         String checkName = component.getName();
         if (branchToBeFound.hasBranch(checkName)) {
            gatherComponents(component, branchToBeFound.fetch(checkName), components);
         } else if (component.getPropertyInParent().isDynamic() && branchToBeFound.hasLeaf(checkName)) {
            components.add(component);
         }
      }
   }

   public static boolean isSuperUser(Context cx) {
      if (cx != null) {
         BUser user = cx.getUser();
         if (user != null) {
            return user.getPermissions().isSuperUser();
         }
      }

      return true;
   }

   public static BIFile copyApplicationTemplateToStation(BNtplFile sourceTemplateFile, BStation station) throws Exception {
      String sourceFileName = sourceTemplateFile.getFileName();
      List<BNtplFile.FileTransferSpec> fileTransferSpecs = new ArrayList<>();
      FilePath stationFilePath = sourceTemplateFile.listFilesToBeTransferred(station, fileTransferSpecs);
      BFileSpace stationSharedFileSpace = ((BIFile)BOrd.make("file:^").get(station)).getFileSpace();

      for (BNtplFile.FileTransferSpec fileTransferSpec : fileTransferSpecs) {
         FilePath path = fileTransferSpec.getTargetPath().merge(fileTransferSpec.getNewFileName());

         try {
            BIFile file = stationSharedFileSpace.makeFile(path);
            BajaFileUtil.pipe(fileTransferSpec.getFileToTransfer(), file);
         } catch (IOException var12) {
            try {
               stationSharedFileSpace.delete(path);
            } catch (IOException var11) {
            }

            throw var12;
         }
      }

      return stationSharedFileSpace.makeFile(stationFilePath);
   }

   public static BNtplFile getApplicationUpgrade(BStation station) {
      BUuid applicationId = getStationApplicationId(station);
      if (!applicationId.isNull()) {
         BVersion applicationVersion = getStationApplicationVersion(station);
         return getApplicationUpgrade(applicationId, applicationVersion);
      } else {
         return null;
      }
   }

   public static BNtplFile getApplicationUpgrade(BUuid applicationId, BVersion applicationVersion) {
      for (BINtplFile file : TemplateFileUtil.getApplicationTemplatesByUuid(applicationId)) {
         BVersion templateVersion = new BVersion(file.getVersion());
         if (applicationVersion.compareTo(templateVersion) < 0) {
            return (BNtplFile)file;
         }
      }

      return null;
   }

   public static BUuid getStationApplicationId(BStation station) {
      BUuid applicationId = BUuid.DEFAULT;
      BISession session = station.getSession();
      if (session instanceof BFoxProxySession) {
         Optional<String> result = Optional.empty();

         try {
            result = ((BFoxProxySession)session).rpc(BOrd.make("type:template:ApplicationService"), "getApplicationId", new Object[0]);
            if (result.isPresent()) {
               applicationId = BUuid.make(result.get());
            }
         } catch (Exception var5) {
         }
      }

      return applicationId;
   }

   public static BVersion getStationApplicationVersion(BStation station) {
      BVersion applicationVersion = BVersion.makeZero();
      BISession session = station.getSession();
      if (session instanceof BFoxProxySession) {
         Optional<String> result = Optional.empty();

         try {
            result = ((BFoxProxySession)session).rpc(BOrd.make("type:template:ApplicationService"), "getApplicationVersion", new Object[0]);
            if (result.isPresent()) {
               applicationVersion = new BVersion(result.get());
            }
         } catch (Exception var5) {
         }
      }

      return applicationVersion;
   }
}
