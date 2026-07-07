package com.tridium.template.ui;

import com.tridium.template.BTemplateService;
import com.tridium.template.application.ApplicationTemplateUtil;
import com.tridium.template.file.BNtplFile;
import com.tridium.template.manifest.TemplateManifest;
import com.tridium.template.ui.file.BWbDeployableNtplFile;
import com.tridium.template.ui.file.ExportApplicationCommand;
import com.tridium.template.ui.file.TmplUtil;
import com.tridium.template.ui.installapp.InstallApplicationCommand;
import com.tridium.template.ui.upgradeapp.UpgradeApplicationCommand;
import com.tridium.workbench.nav.BComponentMenuAgent;
import com.tridium.workbench.nav.BIComponentMenuDecorator;
import com.tridium.workbench.nav.BComponentMenuAgent.EditTagsCommand;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.baja.license.Feature;
import javax.baja.license.FeatureNotLicensedException;
import javax.baja.naming.BOrd;
import javax.baja.nre.annotations.AgentOn;
import javax.baja.nre.annotations.NiagaraSingleton;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.space.BComponentSpace;
import javax.baja.sys.BComponent;
import javax.baja.sys.BSingleton;
import javax.baja.sys.BStation;
import javax.baja.sys.BValue;
import javax.baja.sys.Context;
import javax.baja.sys.Property;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;
import javax.baja.ui.BDialog;
import javax.baja.ui.BMenu;
import javax.baja.ui.BMenuItem;
import javax.baja.ui.BSeparator;
import javax.baja.ui.BSubMenuItem;
import javax.baja.ui.BWidget;
import javax.baja.ui.Command;
import javax.baja.ui.CommandArtifact;
import javax.baja.ui.util.UiLexicon;
import javax.baja.util.BTypeSpec;
import javax.baja.util.Lexicon;
import javax.baja.workbench.BWbShell;
import javax.baja.workbench.nav.tree.BNavTree;

@NiagaraType(
   agent = {@AgentOn(
      types = {"workbench:ComponentMenuAgent"}
   )}
)
@NiagaraSingleton
public final class BTemplateComponentMenuDecorator extends BSingleton implements BIComponentMenuDecorator {
   public static final BTemplateComponentMenuDecorator INSTANCE = new BTemplateComponentMenuDecorator();
   public static final Type TYPE = Sys.loadType(BTemplateComponentMenuDecorator.class);
   private static final Lexicon LEX = Lexicon.make(BTemplateComponentMenuDecorator.class);

   private BTemplateComponentMenuDecorator() {
   }

   public Type getType() {
      return TYPE;
   }

   public void decorateMenu(BComponentMenuAgent menuAgent, BMenu menu, BWidget owner, BComponent target, Context cx) {
      if (isLicensed()) {
         if (isChildTemplateConfigAllowed(target)) {
            List<Property> properties = new ArrayList<>(Arrays.asList(menu.getDynamicPropertiesArray()));
            int startIndex = findStartIndexForTemplateCommands(properties, menu) + 1;
            int addIndex = startIndex;
            if (!isTemplateEditorNav(owner)) {
               if (target instanceof BStation) {
                  BMenu templatesMenu = new BMenu(UiLexicon.bajaui().get("commands.templates.label"));
                  BStation station = (BStation)target;
                  templatesMenu.add(null, BTemplateComponentMenuDecorator.MakeTemplateCommand.makeStationTemplateCommand(owner, station));
                  templatesMenu.add(null, BTemplateComponentMenuDecorator.MakeTemplateCommand.makeApplicationTemplateCommand(owner, station));
                  if (TemplateUiUtil.isSuperUser(station)) {
                     templatesMenu.add(null, new InstallApplicationCommand(owner, station));
                     BNtplFile upgradeFile = ApplicationTemplateUtil.getApplicationUpgrade(station);
                     if (upgradeFile instanceof BWbDeployableNtplFile) {
                        templatesMenu.add(null, new UpgradeApplicationCommand(owner, station, upgradeFile.getAbsoluteOrd()));
                     }
                  }

                  if (!ApplicationTemplateUtil.getStationApplicationId(station).isNull()) {
                     templatesMenu.add(null, new ExportApplicationCommand(owner, station));
                  }

                  addIndex = startIndex + 1;
                  addPropertyToList(properties, addIndex, menu.add(null, new BSubMenuItem(templatesMenu)));
               } else {
                  Property property = menu.add(null, BTemplateComponentMenuDecorator.MakeTemplateCommand.makeTemplateCommand(owner, target))
                     .getPropertyInParent();
                  addIndex = startIndex + 1;
                  addPropertyToList(properties, addIndex, property);
               }
            } else if (isApplicationTemplateEditorNav(owner) && canBeAnOptionalComponent(target, owner)) {
               Property property = menu.add(null, new MarkAsOptionalCommand(owner, getManifest(owner))).getPropertyInParent();
               addIndex = startIndex + 1;
               addPropertyToList(properties, addIndex, property);
            }

            if (addIndex > startIndex) {
               addPropertyToList(properties, ++addIndex, menu.add(null, new BSeparator()));
            }

            menu.reorder(properties.toArray(new Property[0]));
         }
      }
   }

   private static TemplateManifest getManifest(BWidget owner) {
      for (BWidget container = owner.getParentWidget(); container != null; container = container.getParentWidget()) {
         if (container.getType().is(BTemplateBogEditor.TYPE)) {
            BTemplateBogEditor bogEditor = (BTemplateBogEditor)container;
            return bogEditor.getManifest();
         }
      }

      return null;
   }

   private static boolean isApplicationTemplateEditorNav(BWidget owner) {
      if (!owner.getType().is(BNavTree.TYPE)) {
         return false;
      } else {
         BTemplateBogEditor bogEditor = findTemplateBogEditor(owner);
         return bogEditor != null && bogEditor.isApplicationTemplate();
      }
   }

   private static boolean isTemplateEditorNav(BWidget owner) {
      return !owner.getType().is(BNavTree.TYPE) ? false : findTemplateBogEditor(owner.getParentWidget()) != null;
   }

   private static BTemplateBogEditor findTemplateBogEditor(BWidget owner) {
      for (BWidget container = owner; container != null; container = container.getParentWidget()) {
         if (container.getType().is(BTemplateBogEditor.TYPE)) {
            return (BTemplateBogEditor)container;
         }
      }

      return null;
   }

   private static boolean canBeAnOptionalComponent(BComponent component, BWidget owner) {
      TemplateManifest manifest = getManifest(owner);
      return manifest == null ? false : MarkAsOptionalCommand.canBeAnOptionalComponent(component, manifest);
   }

   private static void addPropertyToList(List<Property> properties, int index, Property newProperty) {
      if (index >= properties.size()) {
         properties.add(newProperty);
      } else {
         properties.add(index, newProperty);
      }
   }

   private static boolean isLicensed() {
      try {
         Feature feature = Sys.getLicenseManager().getFeature("tridium", "template");
         feature.check();
         return true;
      } catch (FeatureNotLicensedException var1) {
         return false;
      }
   }

   private static boolean isChildTemplateConfigAllowed(BComponent comp) {
      try {
         BComponent templateConfig = (BComponent)BTypeSpec.make("template", "TemplateConfig").getInstance();
         return comp.isChildLegal(templateConfig);
      } catch (Exception var2) {
         return false;
      }
   }

   private static int findStartIndexForTemplateCommands(List<Property> properties, BMenu menu) {
      int addIndex = properties.size() - 1;

      for (int i = 0; i < properties.size(); i++) {
         BValue value = menu.get(properties.get(i));
         if (value instanceof BMenuItem) {
            BMenuItem menuItem = (BMenuItem)value;
            if (menuItem.getCommand() instanceof EditTagsCommand) {
               addIndex = i;
               break;
            }
         }
      }

      return addIndex;
   }

   private static String getLogMessagePrefix(boolean createApp) {
      return createApp ? "makeApplication." : "makeStationTemplate.";
   }

   private static void hyperlink(BWidget owner, BOrd ord) {
      BWbShell shell = BWbShell.getWbShell(owner);
      if (shell != null) {
         shell.hyperlink(ord);
      }
   }

   private static final class MakeTemplateCommand extends Command {
      private final boolean createApp;
      private final BComponent target;

      private MakeTemplateCommand(BWidget owner, BComponent c, String keyBase, boolean createApp) {
         super(owner, UiLexicon.bajaui(), keyBase);
         this.target = c;
         this.createApp = createApp;
      }

      private static BTemplateComponentMenuDecorator.MakeTemplateCommand makeStationTemplateCommand(BWidget owner, BComponent comp) {
         return new BTemplateComponentMenuDecorator.MakeTemplateCommand(owner, comp, "commands.makeStationTemplate", false);
      }

      private static BTemplateComponentMenuDecorator.MakeTemplateCommand makeTemplateCommand(BWidget owner, BComponent comp) {
         return new BTemplateComponentMenuDecorator.MakeTemplateCommand(owner, comp, "commands.makeTemplate", false);
      }

      private static BTemplateComponentMenuDecorator.MakeTemplateCommand makeApplicationTemplateCommand(BWidget owner, BComponent comp) {
         return new BTemplateComponentMenuDecorator.MakeTemplateCommand(owner, comp, "commands.makeApplicationTemplate", true);
      }

      public CommandArtifact doInvoke() {
         boolean goAhead = true;
         if (this.target instanceof BStation) {
            BStation station = (BStation)this.target;
            if (TmplUtil.stationHasAce(station)) {
               BComponentSpace space = station.getComponentSpace();
               boolean isOnline = space != null && space.isProxyComponentSpace();
               BTemplateService templateService = TemplateUiUtil.resolveTemplateService(station);
               boolean templateServiceIsCapable = templateService != null
                  && templateService.getAction(this.createApp ? "makeApplicationTemplate" : "makeStationTemplate") != null;
               if (isOnline) {
                  if (!TemplateUiUtil.isSuperUser(station)) {
                     goAhead = 4
                        == BDialog.confirm(
                           this.getOwner(),
                           BTemplateComponentMenuDecorator.LEX.get(BTemplateComponentMenuDecorator.getLogMessagePrefix(this.createApp) + "lesserUser")
                        );
                  } else if (!templateServiceIsCapable) {
                     goAhead = 4
                        == BDialog.confirm(
                           this.getOwner(),
                           BTemplateComponentMenuDecorator.LEX.get(BTemplateComponentMenuDecorator.getLogMessagePrefix(this.createApp) + "missingService")
                        );
                  }
               }
            }
         }

         if (goAhead) {
            BTemplateComponentMenuDecorator.hyperlink(
               this.getOwner(), BOrd.make(this.target.getAbsoluteOrd(), this.createApp ? "template:createApp" : "template:create")
            );
         }

         return null;
      }
   }
}
