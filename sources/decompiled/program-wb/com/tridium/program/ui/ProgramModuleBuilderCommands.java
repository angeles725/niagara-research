package com.tridium.program.ui;

import com.tridium.security.BPermissionGroupInfo;
import com.tridium.workbench.security.BPermissionGroupFE;
import javax.baja.ui.BDialog;
import javax.baja.ui.Command;
import javax.baja.ui.CommandArtifact;
import javax.baja.ui.util.UiLexicon;

public class ProgramModuleBuilderCommands {
   private final BProgramModuleBuilder builder;
   ProgramModuleBuilderCommands.AddPermissionRequestCommand addPermissionCommand;
   ProgramModuleBuilderCommands.EditPermissionRequestCommand editPermissionCommand;
   ProgramModuleBuilderCommands.RemovePermissionRequestCommand removePermissionCommand;

   public ProgramModuleBuilderCommands(BProgramModuleBuilder builder) {
      this.builder = builder;
      this.addPermissionCommand = new ProgramModuleBuilderCommands.AddPermissionRequestCommand();
      this.editPermissionCommand = new ProgramModuleBuilderCommands.EditPermissionRequestCommand();
      this.removePermissionCommand = new ProgramModuleBuilderCommands.RemovePermissionRequestCommand();
   }

   class AddPermissionRequestCommand extends ProgramModuleBuilderCommands.ProgramModuleBuilderCommand {
      public AddPermissionRequestCommand() {
         super("programModuleBuilder.addPermissionRequest");
      }

      public CommandArtifact doInvoke() {
         BPermissionGroupFE plugin = new BPermissionGroupFE();
         plugin.loadValue(new BPermissionGroupInfo());
         plugin.doUpdateParameters();
         int r = BDialog.open(ProgramModuleBuilderCommands.this.builder, this.getLabel(), plugin, 3);
         if (r == 2) {
            return null;
         } else {
            BPermissionGroupInfo permissionGroupInfo = plugin.getPermissionGroupInfo();
            ProgramModuleBuilderCommands.this.builder.permissions.add(permissionGroupInfo);
            return null;
         }
      }
   }

   class EditPermissionRequestCommand extends ProgramModuleBuilderCommands.ProgramModuleBuilderCommand {
      public EditPermissionRequestCommand() {
         super("programModuleBuilder.editPermissionRequest");
      }

      public CommandArtifact doInvoke() {
         BPermissionGroupFE plugin = new BPermissionGroupFE();
         BPermissionGroupInfo oldInfo = ProgramModuleBuilderCommands.this.builder.permissions.getSelectedPermission();
         if (oldInfo != null) {
            plugin.loadValue(oldInfo);
            int r = BDialog.open(ProgramModuleBuilderCommands.this.builder, this.getLabel(), plugin, 3);
            if (r == 2) {
               return null;
            }

            BPermissionGroupInfo newInfo = plugin.getPermissionGroupInfo();
            ProgramModuleBuilderCommands.this.builder.pmod.getPermissions().set(oldInfo.getName(), newInfo);
         }

         return null;
      }
   }

   class ProgramModuleBuilderCommand extends Command {
      public ProgramModuleBuilderCommand(String keyBase) {
         super(ProgramModuleBuilderCommands.this.builder, UiLexicon.bajaui().module, keyBase);
      }
   }

   class RemovePermissionRequestCommand extends ProgramModuleBuilderCommands.ProgramModuleBuilderCommand {
      public RemovePermissionRequestCommand() {
         super("programModuleBuilder.removePermissionRequest");
      }

      public CommandArtifact doInvoke() {
         BPermissionGroupInfo[] permissionGroupInfos = (BPermissionGroupInfo[])ProgramModuleBuilderCommands.this.builder
            .pmod
            .getPermissions()
            .getChildren(BPermissionGroupInfo.class);

         for (int row : ProgramModuleBuilderCommands.this.builder.permissions.getSelection().getRows()) {
            ProgramModuleBuilderCommands.this.builder.pmod.getPermissions().remove(permissionGroupInfos[row].getName());
         }

         return null;
      }
   }
}
