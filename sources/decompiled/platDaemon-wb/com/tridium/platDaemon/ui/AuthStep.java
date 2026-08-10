package com.tridium.platDaemon.ui.commissioningwizard;

import com.tridium.install.part.BOsPart;
import com.tridium.niagarad.util.DaemonAuthUtil;
import com.tridium.nre.auth.GroupAccount;
import com.tridium.nre.auth.NativeAccount;
import com.tridium.nre.auth.UserAccount;
import com.tridium.nre.util.LegacyStorageUtil;
import com.tridium.platDaemon.ui.FipsOptionsHelper;
import com.tridium.platDaemon.ui.acctmgt.BNativeUserTable;
import com.tridium.platDaemon.ui.acctmgt.FileDomainPasswordController;
import com.tridium.platDaemon.ui.acctmgt.QnxUserNameController;
import com.tridium.platform.daemon.message.AccountManagementMessage;
import com.tridium.platform.daemon.message.AddMemberMessage;
import com.tridium.platform.daemon.message.AddUserMessage;
import com.tridium.platform.daemon.message.AuthenticateUserMessage;
import com.tridium.platform.daemon.message.ChangePasswordMessage;
import com.tridium.platform.daemon.message.DeleteMemberMessage;
import com.tridium.platform.daemon.message.DeleteUserMessage;
import com.tridium.platform.daemon.message.XmlResponseMessage;
import com.tridium.ui.util.LabelUtil;
import com.tridium.workbench.fieldeditors.BPasswordFE;
import java.io.IOException;
import java.security.AccessController;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import javax.baja.gx.BInsets;
import javax.baja.nre.util.SecurityUtil;
import javax.baja.security.BPassword;
import javax.baja.security.BUsernameAndPassword;
import javax.baja.sys.BajaRuntimeException;
import javax.baja.ui.BBorder;
import javax.baja.ui.BDialog;
import javax.baja.ui.BLabel;
import javax.baja.ui.BListDropDown;
import javax.baja.ui.BRadioButton;
import javax.baja.ui.BTextField;
import javax.baja.ui.BWidget;
import javax.baja.ui.CommandArtifact;
import javax.baja.ui.ToggleCommand;
import javax.baja.ui.ToggleCommandGroup;
import javax.baja.ui.enums.BHalign;
import javax.baja.ui.enums.BValign;
import javax.baja.ui.pane.BBorderPane;
import javax.baja.ui.pane.BEdgePane;
import javax.baja.ui.pane.BGridPane;
import javax.baja.ui.text.TextModel;
import javax.baja.xml.XElem;
import javax.baja.xml.XParser;

public class AuthStep extends CommissioningWizardStep {
   private AuthStep.NativeNewUserStep nativeNewUserStep = null;
   private AuthStep.NativeUsersStep nativeUsersStep = null;
   private AuthStep.NativeCredentialsStep nativeCredentialsStep = null;
   private AuthStep.FileDomainUserStep fileDomainUserStep = null;
   private BRadioButton scramFileButton = null;
   private BRadioButton digestFileButton = null;
   private BRadioButton basicNativeButton = null;
   private BRadioButton scramNativeButton = null;
   private boolean stepInitialized = false;

   public AuthStep(CommissioningWizardModel wizardModel) {
      super(wizardModel);
   }

   private void initStep() {
      try {
         if (!this.stepInitialized) {
            this.stepInitialized = true;
            if (DaemonAuthUtil.isFileScheme(this.wizardModel.authMethod)) {
               BUsernameAndPassword cred = (BUsernameAndPassword)this.wizardModel.daemonSession.getCredentials();
               if (cred != null) {
                  this.wizardModel.fileDomainAdminPassword = cred.getPassword();
               }
            }

            BOsPart os = this.wizardModel.getPlatform().getOsPart();
            ToggleCommandGroup<ToggleCommand> group = new ToggleCommandGroup();
            BGridPane grid = new BGridPane(1);
            grid.setValign(BValign.top);
            grid.setHalign(BHalign.left);
            String osNameForLabel = makeFriendlyOsName(this.wizardModel.getPlatform().getOsPart().getPartName());
            if (this.wizardModel.authTypesSupported.contains("basic/native")) {
               ToggleCommand nextCommand;
               if (this.wizardModel.daemonSession.getHostProperties().supportsServlet("acctmgt")) {
                  if (this.wizardModel.defaultAccountPresent) {
                     nextCommand = new AuthStep.NextStepCommand(
                        this.wizardModel.owner,
                        this,
                        this.nativeNewUserStep = new AuthStep.NativeNewUserStep(this.wizardModel),
                        "AuthWizard.authMethodStep.basicNative.label",
                        new Object[]{osNameForLabel}
                     );
                     this.nativeNewUserStep.priorStep = this;
                     this.nativeNewUserStep.lastStep.nextStep = this.stepAfterSection;
                  } else {
                     nextCommand = new AuthStep.NextStepCommand(
                        this.wizardModel.owner,
                        this,
                        this.nativeUsersStep = new AuthStep.NativeUsersStep(this.wizardModel),
                        "AuthWizard.authMethodStep.basicNative.label",
                        new Object[]{osNameForLabel}
                     );
                     this.nativeUsersStep.priorStep = this;
                     this.nativeUsersStep.lastStep.nextStep = this.stepAfterSection;
                  }
               } else {
                  nextCommand = new AuthStep.NextStepCommand(
                     this.wizardModel.owner,
                     this,
                     this.nativeCredentialsStep = new AuthStep.NativeCredentialsStep(this.wizardModel),
                     "AuthWizard.authMethodStep.basicNative.label",
                     new Object[]{osNameForLabel}
                  );
                  this.nativeCredentialsStep.priorStep = this;
                  this.nativeCredentialsStep.lastStep.nextStep = this.stepAfterSection;
               }

               grid.add(null, this.basicNativeButton = new BRadioButton(nextCommand, true, false));
               group.add(nextCommand);
            }

            if (this.wizardModel.authTypesSupported.contains("scram-sha512/native")
               || this.wizardModel.authTypesSupported.contains("scram-glibc-sha256/native")
               || this.wizardModel.authTypesSupported.contains("scram-glibc-sha512/native")
               || this.wizardModel.authTypesSupported.contains("scram-bcrypt/native")) {
               ToggleCommand nextCommand;
               if (this.wizardModel.daemonSession.getHostProperties().supportsServlet("acctmgt")) {
                  if (this.wizardModel.defaultAccountPresent) {
                     nextCommand = new AuthStep.NextStepCommand(
                        this.wizardModel.owner,
                        this,
                        this.nativeNewUserStep = new AuthStep.NativeNewUserStep(this.wizardModel),
                        "AuthWizard.authMethodStep.scramNative.label",
                        new Object[]{osNameForLabel}
                     );
                     this.nativeNewUserStep.priorStep = this;
                     this.nativeNewUserStep.lastStep.nextStep = this.stepAfterSection;
                  } else {
                     nextCommand = new AuthStep.NextStepCommand(
                        this.wizardModel.owner,
                        this,
                        this.nativeUsersStep = new AuthStep.NativeUsersStep(this.wizardModel),
                        "AuthWizard.authMethodStep.scramNative.label",
                        new Object[]{osNameForLabel}
                     );
                     this.nativeUsersStep.priorStep = this;
                     this.nativeUsersStep.lastStep.nextStep = this.stepAfterSection;
                  }
               } else {
                  nextCommand = new AuthStep.NextStepCommand(
                     this.wizardModel.owner,
                     this,
                     this.nativeCredentialsStep = new AuthStep.NativeCredentialsStep(this.wizardModel),
                     "AuthWizard.authMethodStep.scramNative.label",
                     new Object[]{osNameForLabel}
                  );
                  this.nativeCredentialsStep.priorStep = this;
                  this.nativeCredentialsStep.lastStep.nextStep = this.stepAfterSection;
               }

               grid.add(null, this.scramNativeButton = new BRadioButton(nextCommand, true, false));
               group.add(nextCommand);
            }

            if (this.wizardModel.authTypesSupported.contains("digest/file")) {
               ToggleCommand cmd = new AuthStep.NextStepCommand(
                  this.wizardModel.owner,
                  this,
                  this.fileDomainUserStep = new AuthStep.FileDomainUserStep(this.wizardModel, "digest/file"),
                  "AuthWizard.authMethodStep.digestFile.label",
                  null
               );
               this.fileDomainUserStep.priorStep = this;
               this.fileDomainUserStep.lastStep.nextStep = this.stepAfterSection;
               grid.add(null, this.digestFileButton = new BRadioButton(cmd, true, false));
               group.add(cmd);
            }

            if (this.wizardModel.authTypesSupported.contains("scram-glibc-sha512/file")) {
               ToggleCommand cmd = new AuthStep.NextStepCommand(
                  this.wizardModel.owner,
                  this,
                  this.fileDomainUserStep = new AuthStep.FileDomainUserStep(this.wizardModel, "scram-glibc-sha512/file"),
                  "AuthWizard.authMethodStep.scramFile.label",
                  null
               );
               this.fileDomainUserStep.priorStep = this;
               this.fileDomainUserStep.lastStep.nextStep = this.stepAfterSection;
               grid.add(null, this.scramFileButton = new BRadioButton(cmd, true, false));
               group.add(cmd);
            }

            BLabel headerLabel = LabelUtil.makeLabel(this.getLexiconText("CommissioningWizard.authMethodStep.header"));
            this.setContent(new BEdgePane(new BBorderPane(headerLabel, 4.0, 0.0, 8.0, 0.0), null, null, null, grid));
            if (this.wizardModel.authTypesSupported.size() == 1) {
               if ((this.basicNativeButton == null || !this.wizardModel.authMethod.equals("basic/native"))
                  && (
                     this.scramNativeButton == null
                        || !this.wizardModel.authMethod.equals("scram-sha512/native")
                           && !this.wizardModel.authMethod.equals("scram-glibc-sha256/native")
                           && !this.wizardModel.authMethod.equals("scram-glibc-sha512/native")
                           && !this.wizardModel.authMethod.equals("scram-bcrypt/native")
                  )) {
                  if (this.digestFileButton != null && this.wizardModel.authMethod.equals("digest/file")) {
                     this.digestFileButton.setSelected(true);
                     this.nextStep = this.fileDomainUserStep;
                     this.lastStep = this.fileDomainUserStep.lastStep;
                     this.setStepAfterSection(this.stepAfterSection);
                  } else if (this.scramFileButton != null && this.wizardModel.authMethod.equals("scram-glibc-sha512/file")) {
                     this.scramFileButton.setSelected(true);
                     this.nextStep = this.fileDomainUserStep;
                     this.lastStep = this.fileDomainUserStep.lastStep;
                     this.setStepAfterSection(this.stepAfterSection);
                  }
               } else {
                  if (this.basicNativeButton != null) {
                     this.basicNativeButton.setSelected(this.wizardModel.authMethod.equals("basic/native"));
                  }

                  if (this.scramNativeButton != null) {
                     this.scramNativeButton
                        .setSelected(
                           this.wizardModel.authMethod.equals("scram-sha512/native")
                              || this.wizardModel.authMethod.equals("scram-glibc-sha256/native")
                              || this.wizardModel.authMethod.equals("scram-glibc-sha512/native")
                              || this.wizardModel.authMethod.equals("scram-bcrypt/native")
                        );
                  }

                  if (this.wizardModel.daemonSession.getHostProperties().supportsServlet("acctmgt")) {
                     if (this.wizardModel.defaultAccountPresent) {
                        this.nextStep = this.nativeNewUserStep;
                        this.lastStep = this.nativeNewUserStep.lastStep;
                     } else {
                        this.nextStep = this.nativeUsersStep;
                        this.lastStep = this.nativeUsersStep.lastStep;
                     }
                  } else {
                     this.nextStep = this.nativeCredentialsStep;
                     this.lastStep = this.nativeCredentialsStep.lastStep;
                  }

                  this.setStepAfterSection(this.stepAfterSection);
               }
            }
         }
      } catch (RuntimeException var7) {
         throw var7;
      } catch (Exception var8) {
         throw new BajaRuntimeException(var8);
      }
   }

   @Override
   public String getStepName() {
      return "authMethodStep";
   }

   @Override
   public String getSectionName() {
      return "auth";
   }

   @Override
   public void enter() {
      this.initStep();
      if (DaemonAuthUtil.isNativeScheme(this.wizardModel.authMethod)) {
         if (this.basicNativeButton != null) {
            this.basicNativeButton.setSelected(this.wizardModel.authMethod.equals("basic/native"));
         }

         if (this.scramNativeButton != null) {
            this.scramNativeButton
               .setSelected(
                  this.wizardModel.authMethod.equals("scram-sha512/native")
                     || this.wizardModel.authMethod.equals("scram-glibc-sha256/native")
                     || this.wizardModel.authMethod.equals("scram-glibc-sha512/native")
                     || this.wizardModel.authMethod.equals("scram-bcrypt/native")
               );
         }

         if (this.wizardModel.daemonSession.getHostProperties().supportsServlet("acctmgt")) {
            if (this.wizardModel.defaultAccountPresent) {
               this.nextStep = this.nativeNewUserStep;
               this.lastStep = this.nativeNewUserStep.lastStep;
            } else {
               this.nextStep = this.nativeUsersStep;
               this.lastStep = this.nativeUsersStep.lastStep;
            }
         } else {
            this.nextStep = this.nativeCredentialsStep;
            this.lastStep = this.nativeCredentialsStep.lastStep;
         }
      } else if (this.wizardModel.authMethod.equals("digest/file")) {
         this.digestFileButton.setSelected(true);
         this.nextStep = this.fileDomainUserStep;
         this.lastStep = this.fileDomainUserStep.lastStep;
      } else if (this.wizardModel.authMethod.equals("scram-glibc-sha512/file")) {
         this.scramFileButton.setSelected(true);
         this.nextStep = this.fileDomainUserStep;
         this.lastStep = this.fileDomainUserStep.lastStep;
      }

      this.setStepAfterSection(this.stepAfterSection);
   }

   @Override
   public boolean exit(int direction) {
      if (!this.wizardModel.forceChangeUserList.isEmpty()) {
         return false;
      } else {
         if (this.digestFileButton != null && this.digestFileButton.isSelected() && !this.wizardModel.authMethod.equals("digest/file")) {
            this.wizardModel.authMethod = "digest/file";
            this.wizardModel.authMethodChanged = true;
         }

         if (this.scramFileButton != null && this.scramFileButton.isSelected()) {
            if (!this.wizardModel.authMethod.equals("scram-glibc-sha512/file")) {
               this.wizardModel.authMethod = "scram-glibc-sha512/file";
               this.wizardModel.authMethodChanged = true;
            }
         } else if (this.basicNativeButton != null && this.basicNativeButton.isSelected()) {
            if (!this.wizardModel.authMethod.equals("basic/native")) {
               this.wizardModel.authMethod = "basic/native";
               this.wizardModel.authMethodChanged = true;
            }
         } else if (this.scramNativeButton != null
            && this.scramNativeButton.isSelected()
            && !this.wizardModel.authMethod.equals("scram-sha512/native")
            && !this.wizardModel.authMethod.equals("scram-glibc-sha256/native")
            && !this.wizardModel.authMethod.equals("scram-glibc-sha512/native")
            && !this.wizardModel.authMethod.equals("scram-bcrypt/native")) {
            if (this.wizardModel.authTypesSupported.contains("scram-sha512/native")) {
               this.wizardModel.authMethod = "scram-sha512/native";
            } else if (this.wizardModel.authTypesSupported.contains("scram-glibc-sha256/native")) {
               this.wizardModel.authMethod = "scram-glibc-sha256/native";
            } else if (this.wizardModel.authTypesSupported.contains("scram-glibc-sha512/native")) {
               this.wizardModel.authMethod = "scram-glibc-sha512/native";
            } else if (this.wizardModel.authTypesSupported.contains("scram-bcrypt/native")) {
               this.wizardModel.authMethod = "scram-bcrypt/native";
            }

            this.wizardModel.authMethodChanged = true;
         }

         return true;
      }
   }

   @Override
   public int getMode() {
      return 3;
   }

   @Override
   public boolean isSkipped() {
      this.initStep();
      return this.wizardModel.authTypesSupported.size() == 1 || this.wizardModel.skipAuth;
   }

   private static String makeFriendlyOsName(String osPart) {
      String osNameForLabel = null;
      if (osPart.toLowerCase().contains("win")) {
         osNameForLabel = "Windows";
      } else if (osPart.toLowerCase().contains("qnx")) {
         osNameForLabel = "QNX";
      } else if (osPart.toLowerCase().contains("linux")) {
         osNameForLabel = "Linux";
      } else {
         osNameForLabel = osPart;
      }

      return osNameForLabel;
   }

   private static class FileDomainUserStep extends CommissioningWizardStep {
      private BTextField userNameField;
      private BPasswordFE passwordFE1;
      private BPasswordFE passwordFE2;
      private String authenticationScheme;

      public FileDomainUserStep(CommissioningWizardModel wizardModel, String authenticationScheme) throws Exception {
         super(wizardModel);
         BOsPart os = wizardModel.getPlatform().getOsPart();
         BGridPane grid = new BGridPane(2);
         grid.setValign(BValign.top);
         grid.setHalign(BHalign.left);
         grid.setStretchColumn(1);
         LabelUtil.addLabelWidgetPair(grid, this.getLexiconText("AuthWizard.authFileDomainUserStep.user.label"), this.userNameField = new BTextField("", 20));
         LabelUtil.addLabelWidgetPair(grid, this.getLexiconText("AuthWizard.authFileDomainUserStep.pw1.label"), this.passwordFE1 = new BPasswordFE());
         LabelUtil.addLabelWidgetPair(grid, this.getLexiconText("AuthWizard.authFileDomainUserStep.pw1.label"), this.passwordFE2 = new BPasswordFE());
         if (wizardModel.fileDomainAdminPassword != null) {
            this.passwordFE1.loadValue(wizardModel.fileDomainAdminPassword);
            this.passwordFE2.loadValue(wizardModel.fileDomainAdminPassword);
         }

         ((BTextField)this.passwordFE1.getContent()).setModel(new AuthStep.ModeControlModel(this));
         ((BTextField)this.passwordFE2.getContent()).setModel(new AuthStep.ModeControlModel(this));
         boolean QNX = os.getPartName().toLowerCase().contains("qnx");
         if (QNX) {
            ((BTextField)this.passwordFE1.getContent()).setController(new FileDomainPasswordController());
            ((BTextField)this.passwordFE2.getContent()).setController(new FileDomainPasswordController());
         }

         this.userNameField.setModel(new AuthStep.ModeControlModel(this));
         if (QNX) {
            this.userNameField.setController(new QnxUserNameController());
         }

         BLabel headerLabel = LabelUtil.makeLabel(this.getLexiconText("CommissioningWizard.authFileDomainUserStep.header"));
         this.setContent(new BEdgePane(new BBorderPane(headerLabel, 4.0, 0.0, 8.0, 0.0), null, null, null, grid));
      }

      @Override
      public String getStepName() {
         return "authFileDomainUserStep";
      }

      @Override
      public String getSectionName() {
         return "auth";
      }

      @Override
      public void enter() {
         if (this.nextStep != null) {
            this.nextStep.priorStep = this;
         }

         this.userNameField.setText(this.wizardModel.fileDomainAdminUser);
         if (this.wizardModel.fileDomainAdminPassword != null) {
            this.passwordFE1.loadValue(this.wizardModel.fileDomainAdminPassword);
            this.passwordFE2.loadValue(this.wizardModel.fileDomainAdminPassword);
         }
      }

      @Override
      public boolean exit(int direction) {
         boolean back = direction == 1;
         if (back) {
            return true;
         } else {
            try {
               BPassword pw1 = (BPassword)this.passwordFE1.saveValue();
               BPassword pw2 = (BPassword)this.passwordFE2.saveValue();
               if (pw1 != null
                  && AccessController.<String>doPrivileged(pw1::getValue).trim().length() != 0
                  && pw2 != null
                  && AccessController.<String>doPrivileged(pw2::getValue).trim().length() != 0) {
                  if (!SecurityUtil.equals(AccessController.doPrivileged(pw1::getValue), AccessController.doPrivileged(pw2::getValue))) {
                     BDialog.error(this.wizardModel.getWizard(), LEX.getText("AuthWizard.error.passwordMismatch"));
                     return false;
                  } else if (!BNativeUserTable.validatePassword(
                     this.wizardModel.getWizard(),
                     pw1,
                     FipsOptionsHelper.getInstance().getPasswordStrengthRequirements(this.wizardModel.isFips, this.wizardModel.daemonSession)
                  )) {
                     return false;
                  } else if (!NativeAccount.isReservedName(this.userNameField.getText()) && !this.userNameField.getText().contains("suNkiNg")) {
                     boolean userChanged = false;
                     boolean passwordChanged = false;
                     if (!this.wizardModel.fileDomainAdminUser.equals(this.userNameField.getText())) {
                        this.wizardModel.fileDomainAdminUser = this.userNameField.getText();
                        userChanged = true;
                     }

                     if (this.wizardModel.fileDomainAdminPassword == null
                        || !SecurityUtil.equals(
                           AccessController.doPrivileged(this.wizardModel.fileDomainAdminPassword::getValue), AccessController.doPrivileged(pw1::getValue)
                        )) {
                        this.wizardModel.fileDomainAdminPassword = pw1;
                        passwordChanged = true;
                     }

                     if (!this.wizardModel.defaultAccountPresent || userChanged && passwordChanged) {
                        if (userChanged || passwordChanged) {
                           this.wizardModel.newCredentials = new BUsernameAndPassword(
                              this.wizardModel.fileDomainAdminUser, this.wizardModel.fileDomainAdminPassword
                           );
                           this.wizardModel.authMethodChanged = true;
                        }

                        return true;
                     } else {
                        BDialog.error(this.wizardModel.getWizard(), LEX.getText("AuthWizard.error.defaultCredentials"));
                        return false;
                     }
                  } else {
                     BDialog.error(this.wizardModel.getWizard(), LEX.getText("AuthWizard.error.userReserved", new Object[]{this.userNameField.getText()}));
                     return false;
                  }
               } else {
                  BDialog.error(this.wizardModel.getWizard(), LEX.getText("UserManager.error.passwordRequired"));
                  return false;
               }
            } catch (RuntimeException var7) {
               throw var7;
            } catch (Exception var8) {
               throw new BajaRuntimeException(var8);
            }
         }
      }

      @Override
      public int getMode() {
         int mode = 1;
         return this.userNameField.getText().trim().length() != 0
               && ((BTextField)this.passwordFE1.getContent()).getText().trim().length() != 0
               && ((BTextField)this.passwordFE2.getContent()).getText().trim().length() != 0
            ? mode | 2
            : mode;
      }

      @Override
      public boolean isSkipped() {
         return this.wizardModel.skipAuth;
      }
   }

   private static class ModeControlModel extends TextModel {
      private CommissioningWizardStep step;

      public ModeControlModel(CommissioningWizardStep pStep) {
         this.step = pStep;
      }

      protected void textModified() {
         this.step.wizardModel.update(this.step.getMode());
      }
   }

   private static class NativeCredentialsStep extends CommissioningWizardStep {
      private BPasswordFE passwordFE;
      private BTextField userNameField;

      public NativeCredentialsStep(CommissioningWizardModel wizardModel) throws Exception {
         super(wizardModel);
         BGridPane grid = new BGridPane(2);
         grid.setValign(BValign.top);
         grid.setHalign(BHalign.left);
         grid.setStretchColumn(1);
         LabelUtil.addLabelWidgetPair(grid, this.getLexiconText("AuthWizard.authNativeCredentialsStep.user.label"), this.userNameField = new BTextField("", 20));
         LabelUtil.addLabelWidgetPair(grid, this.getLexiconText("AuthWizard.authNativeCredentialsStep.pw.label"), this.passwordFE = new BPasswordFE());
         this.userNameField.setModel(new AuthStep.ModeControlModel(this));
         ((BTextField)this.passwordFE.getContent()).setModel(new AuthStep.ModeControlModel(this));
         this.nextStep = new AuthStep.NativeGroupStep(wizardModel);
         this.lastStep = this.nextStep;
         this.nextStep.priorStep = this;
         BLabel headerLabel = LabelUtil.makeLabel(this.getLexiconText("CommissioningWizard.authNativeCredentialsStep.header"));
         this.setContent(new BEdgePane(new BBorderPane(headerLabel, 4.0, 0.0, 8.0, 0.0), null, null, null, grid));
      }

      @Override
      public String getStepName() {
         return "authNativeCredentialsStep";
      }

      @Override
      public String getSectionName() {
         return "auth";
      }

      @Override
      public void enter() {
         if (this.nextStep != null) {
            this.nextStep.priorStep = this;
         }

         if (this.wizardModel.newCredentials != null) {
            this.userNameField.setText(this.wizardModel.newCredentials.getUsername());
            this.passwordFE.loadValue(this.wizardModel.newCredentials.getPassword());
         }
      }

      @Override
      public boolean exit(int direction) {
         try {
            boolean back = direction == 1;
            if (back) {
               return true;
            } else {
               String username = this.userNameField.getText();
               BPassword password = (BPassword)this.passwordFE.saveValue();
               if (username != null
                  && username.trim().length() != 0
                  && password != null
                  && AccessController.<String>doPrivileged(password::getValue).trim().length() != 0) {
                  AuthenticateUserMessage authUserMessage = new AuthenticateUserMessage(
                     username, password, this.wizardModel.daemonSession.generateSharedSecretKey("commissioningWizard_authn")
                  );
                  XElem userAuthInfo = XParser.make(
                        this.wizardModel.daemonSession.getInputStream(authUserMessage, AuthenticateUserMessage.AUTH_USER_MESSAGE_TIMEOUT, "text/xml")
                     )
                     .parse();
                  if (userAuthInfo.elems("domain").length == 0) {
                     BDialog.error(this.wizardModel.getWizard(), LEX.getText("AuthWizard.error.badCredentials"));
                     return false;
                  } else {
                     if (username.length() > 0 && ((BTextField)this.passwordFE.getContent()).getText().length() > 0) {
                        this.wizardModel.newCredentials = new BUsernameAndPassword(username, password);
                     }

                     this.wizardModel.authInfo = userAuthInfo;
                     return true;
                  }
               } else {
                  BDialog.error(this.wizardModel.getWizard(), LEX.getText("AuthWizard.error.fieldRequired"));
                  return false;
               }
            }
         } catch (RuntimeException var7) {
            throw var7;
         } catch (Exception var8) {
            throw new BajaRuntimeException(var8);
         }
      }

      @Override
      public boolean isSkipped() {
         return this.wizardModel.skipAuth;
      }

      @Override
      public int getMode() {
         int result = 1;
         if (this.userNameField.getText().length() > 0 && ((BTextField)this.passwordFE.getContent()).getText().length() > 0) {
            result |= 2;
         }

         return result;
      }
   }

   private static class NativeGroupStep extends CommissioningWizardStep {
      private BListDropDown adminList;

      public NativeGroupStep(CommissioningWizardModel wizardModel) throws Exception {
         super(wizardModel);
         BGridPane grid = new BGridPane(2);
         grid.setValign(BValign.top);
         grid.setHalign(BHalign.left);
         grid.setStretchColumn(1);
         LabelUtil.addLabelWidgetPair(grid, this.getLexiconText("AuthWizard.authNativeGroupStep.adminList.label"), this.adminList = new BListDropDown());
         String osNameForLabel = AuthStep.makeFriendlyOsName(wizardModel.getPlatform().getOsPart().getPartName());
         BLabel headerLabel = LabelUtil.makeLabel(this.getLexiconText("CommissioningWizard.authNativeGroupStep.header"), new Object[]{osNameForLabel});
         this.setContent(new BEdgePane(new BBorderPane(headerLabel, 4.0, 0.0, 8.0, 0.0), null, null, null, grid));
      }

      @Override
      public String getStepName() {
         return "authNativeGroupStep";
      }

      @Override
      public String getSectionName() {
         return "auth";
      }

      @Override
      public void enter() {
         this.adminList.getList().removeAllItems();

         for (XElem domain : this.wizardModel.authInfo.elems("domain")) {
            for (XElem group : domain.elems("group")) {
               this.adminList.getList().addItem(new GroupAccount(group.get("name"), group.get("id")));
            }
         }

         if (this.adminList.getList().indexOfItem(this.wizardModel.nativeAdminGroup) >= 0) {
            this.adminList.getList().setSelectedItem(this.wizardModel.nativeAdminGroup);
         } else {
            this.adminList.getList().getSelection().deselectAll();
         }
      }

      @Override
      public boolean exit(int direction) {
         boolean back = direction == 1;
         if (back) {
            return true;
         } else if (this.adminList.getList().getSelectedIndex() < 0) {
            BDialog.error(this.wizardModel.getWizard(), LEX.getText("AuthWizard.error.nullAdminGroup"));
            return false;
         } else {
            if (this.wizardModel.nativeAdminGroup == null || !this.wizardModel.nativeAdminGroup.equals(this.adminList.getList().getSelectedItem())) {
               this.wizardModel.nativeAdminGroup = (GroupAccount)this.adminList.getList().getSelectedItem();
               this.wizardModel.authMethodChanged = true;
            }

            return true;
         }
      }

      @Override
      public int getMode() {
         return 3;
      }

      @Override
      public boolean isSkipped() {
         return this.wizardModel.skipAuth;
      }
   }

   private static class NativeNewUserStep extends CommissioningWizardStep {
      BTextField userNameField = new BTextField("", 20);
      BTextField commentField = new BTextField("", 20);
      BPasswordFE passwordFE1 = new BPasswordFE();
      BPasswordFE passwordFE2 = new BPasswordFE();
      private XElem[] existingAdminUsers = null;

      public NativeNewUserStep(CommissioningWizardModel wizardModel) throws Exception {
         super(wizardModel);
         BGridPane grid = new BGridPane(2);
         grid.setValign(BValign.top);
         grid.setHalign(BHalign.left);
         grid.setStretchColumn(1);
         this.userNameField = new BTextField("", 20);
         this.commentField = new BTextField("", 20);
         this.passwordFE1 = new BPasswordFE();
         this.passwordFE2 = new BPasswordFE();
         LabelUtil.addLabelWidgetPair(grid, this.getLexiconText("UserManager.AddUserCommand.label.name"), this.userNameField);
         LabelUtil.addLabelWidgetPair(grid, this.getLexiconText("UserManager.AddUserCommand.label.password"), this.passwordFE1);
         LabelUtil.addLabelWidgetPair(grid, this.getLexiconText("UserManager.AddUserCommand.label.password2"), this.passwordFE2);
         LabelUtil.addLabelWidgetPair(grid, this.getLexiconText("UserManager.AddUserCommand.label.comment"), this.commentField);
         ((BTextField)this.passwordFE1.getContent()).setModel(new AuthStep.ModeControlModel(this));
         ((BTextField)this.passwordFE2.getContent()).setModel(new AuthStep.ModeControlModel(this));
         this.userNameField.setModel(new AuthStep.ModeControlModel(this));
         if (wizardModel.daemonSession.getHostProperties().getOsName().toLowerCase().contains("qnx")) {
            this.userNameField.setController(new QnxUserNameController());
         }

         this.nextStep = new AuthStep.NativeUsersStep(wizardModel);
         this.lastStep = this.nextStep;
         this.nextStep.priorStep = this;
         BLabel headerLabel = LabelUtil.makeLabel(this.getLexiconText("CommissioningWizard.authNewNativeUserStep.header"));
         this.setContent(new BEdgePane(new BBorderPane(headerLabel, 4.0, 0.0, 8.0, 0.0), null, null, null, grid));
      }

      @Override
      public String getStepName() {
         return "authNewNativeUserStep";
      }

      @Override
      public String getSectionName() {
         return "auth";
      }

      @Override
      public void enter() {
         if (this.wizardModel.newNativeUserAccountToAdd != null) {
            this.userNameField.setText(this.wizardModel.newNativeUserAccountToAdd.getAccountName());
            this.commentField.setText(this.wizardModel.newNativeUserAccountToAdd.getComment());

            try {
               this.passwordFE1.loadValue(BPassword.make(LegacyStorageUtil.decode(this.wizardModel.newNativeUserAccountToAdd.getPassword())));
               this.passwordFE2.loadValue(BPassword.make(LegacyStorageUtil.decode(this.wizardModel.newNativeUserAccountToAdd.getPassword())));
            } catch (Exception var2) {
            }
         }
      }

      @Override
      public boolean exit(int direction) {
         boolean back = direction == 1;
         if (back) {
            return true;
         } else {
            try {
               String accountName = this.userNameField.getText().trim();
               if (accountName.length() == 0) {
                  BDialog.error(this.wizardModel.getWizard(), LEX.getText("UserManager.error.nameRequired"));
                  return false;
               } else {
                  String fullyQualifiedName;
                  if (!NativeAccount.isAccountNameFullyQualified(accountName)) {
                     fullyQualifiedName = this.wizardModel.nativeAdminGroup.getDomain() + "\\" + accountName;
                  } else {
                     fullyQualifiedName = accountName;
                     accountName = NativeAccount.fullyQualifiedToUsername(accountName);
                  }

                  String comment = this.commentField.getText().trim();
                  if (BNativeUserTable.isReserved(accountName)) {
                     BDialog.error(this.wizardModel.getWizard(), LEX.getText("UserManager.error.userReserved", new Object[]{accountName}));
                     return false;
                  } else {
                     boolean alreadyExists = false;
                     if (this.nextStep instanceof AuthStep.NativeUsersStep) {
                        if (((AuthStep.NativeUsersStep)this.nextStep).userTable.getUsers().containsKey(fullyQualifiedName)) {
                           alreadyExists = true;
                        }
                     } else {
                        if (this.existingAdminUsers == null) {
                           XElem accountInfo = XParser.make(this.wizardModel.daemonSession.getInputStream(new AccountManagementMessage())).parse();
                           XElem[] remoteGroups = accountInfo.elems("group");

                           for (XElem currentGroup : remoteGroups) {
                              if (currentGroup.get("name").equals(this.wizardModel.nativeAdminGroup.getFullyQualifiedName())) {
                                 this.existingAdminUsers = currentGroup.elems("user");
                              }
                           }
                        }

                        if (this.existingAdminUsers != null) {
                           for (XElem adminUser : this.existingAdminUsers) {
                              if (UserAccount.fullyQualifiedToUsername(adminUser.get("name")).equals(accountName)) {
                                 alreadyExists = true;
                              }
                           }
                        }
                     }

                     if (alreadyExists) {
                        BDialog.error(this.wizardModel.getWizard(), LEX.getText("UserManager.error.userAlreadyExists", new Object[]{accountName}));
                        return false;
                     } else {
                        for (int i = 0; i < comment.length(); i++) {
                           char c = comment.charAt(i);
                           if (!"-=+()@._ ".contains(String.valueOf(c)) && !Character.isLetterOrDigit(c)) {
                              BDialog.error(
                                 this.wizardModel.getWizard(), LEX.getText("UserManager.error.invalidCommentCharacter", new Object[]{String.valueOf(c)})
                              );
                              return false;
                           }

                           if (i >= 64) {
                              BDialog.error(this.wizardModel.getWizard(), LEX.getText("UserManager.error.commentTooLong"));
                              return false;
                           }
                        }

                        BPassword pw1 = (BPassword)this.passwordFE1.saveValue();
                        BPassword pw2 = (BPassword)this.passwordFE2.saveValue();
                        if (pw1 == null
                           || AccessController.<String>doPrivileged(pw1::getValue).trim().length() == 0
                           || pw2 == null
                           || AccessController.<String>doPrivileged(pw2::getValue).trim().length() == 0) {
                           BDialog.error(this.wizardModel.getWizard(), LEX.getText("UserManager.error.passwordRequired"));
                           return false;
                        } else if (!BNativeUserTable.validatePassword(
                           this.wizardModel.getWizard(),
                           pw1,
                           FipsOptionsHelper.getInstance().getPasswordStrengthRequirements(this.wizardModel.isFips, this.wizardModel.daemonSession)
                        )) {
                           return false;
                        } else if (!SecurityUtil.equals(AccessController.doPrivileged(pw1::getValue), AccessController.doPrivileged(pw2::getValue))) {
                           BDialog.error(this.wizardModel.getWizard(), LEX.getText("UserManager.error.passwordMismatch"));
                           return false;
                        } else {
                           this.wizardModel.newNativeUserAccountToAdd = new UserAccount(
                              fullyQualifiedName, null, comment, LegacyStorageUtil.encode(AccessController.doPrivileged(pw1::getValue), 0)
                           );
                           this.wizardModel.defaultNativeUserAccountToRemove = new UserAccount(
                              this.wizardModel.nativeAdminGroup.getDomain() + "\\" + this.wizardModel.defaultAccountUserName, null, null, null
                           );
                           this.wizardModel.authUsersChanged = true;
                           return true;
                        }
                     }
                  }
               }
            } catch (RuntimeException var13) {
               throw var13;
            } catch (Exception var14) {
               throw new BajaRuntimeException(var14);
            }
         }
      }

      @Override
      public int getMode() {
         int mode = 1;
         return this.userNameField.getText().trim().length() != 0
               && ((BTextField)this.passwordFE1.getContent()).getText().trim().length() != 0
               && ((BTextField)this.passwordFE2.getContent()).getText().trim().length() != 0
            ? mode | 2
            : mode;
      }

      @Override
      public boolean isSkipped() {
         return this.wizardModel.skipAuth || !this.wizardModel.defaultAccountPresent;
      }
   }

   private static class NativeUsersStep extends CommissioningWizardStep {
      private BNativeUserTable userTable;
      private HashMap<String, UserAccount> beforeUsers = new HashMap<>();
      private HashMap<String, UserAccount> afterUsers = new HashMap<>();

      public NativeUsersStep(CommissioningWizardModel wizardModel) throws Exception {
         super(wizardModel);
         BGridPane headers = new BGridPane(1);
         headers.setHalign(BHalign.left);
         headers.add(null, LabelUtil.makeLabel(this.getLexiconText("CommissioningWizard.authNativeUsersStep.header"), false));
         this.userTable = new BNativeUserTable(false);
         this.userTable.loadSession(wizardModel.daemonSession);
         this.beforeUsers = this.userTable.getUsers();
         BGridPane usersPane = new BGridPane(1);
         usersPane.setHalign(BHalign.left);
         usersPane.add(null, LabelUtil.makeLabel(this.getLexiconText("UserManager.label.users"), true));
         this.setContent(
            new BEdgePane(
               new BEdgePane(
                  null,
                  new BBorderPane(usersPane, 3.0, 0.0, 7.0, 0.0),
                  null,
                  null,
                  new BBorderPane(headers, BBorder.make("bottom(groove)"), BInsets.make(4.0, 0.0, 8.0, 0.0))
               ),
               new BBorderPane(this.userTable.getButtonPane(), 4.0, 0.0, 4.0, 0.0),
               null,
               null,
               new BBorderPane(this.userTable, BBorder.inset, BInsets.DEFAULT)
            )
         );
      }

      @Override
      public String getStepName() {
         return "authNativeUsersStep";
      }

      @Override
      public String getSectionName() {
         return "auth";
      }

      @Override
      public void enter() {
         this.userTable.setForceChangeUsers(this.wizardModel.forceChangeUsers, this.wizardModel.forceChangeUserList);
         if (this.wizardModel.newNativeUserAccountToAdd != null) {
            BNativeUserTable.UserModel model = (BNativeUserTable.UserModel)this.userTable.getModel();

            for (int i = 0; i < model.getRowCount(); i++) {
               if (model.getUser(i).getAccountName().equalsIgnoreCase(this.wizardModel.defaultAccountUserName)) {
                  UserAccount toRemove = (UserAccount)model.remove(i);
                  this.beforeUsers.remove(toRemove.getFullyQualifiedName());
                  this.wizardModel.defaultNativeUserAccountToRemove = toRemove;
                  break;
               }
            }

            boolean newUserFound = false;

            for (int ix = 0; ix < model.getRowCount(); ix++) {
               if (model.getUser(ix).getAccountName().equalsIgnoreCase(this.wizardModel.newNativeUserAccountToAdd.getAccountName())) {
                  newUserFound = true;
                  break;
               }
            }

            if (!newUserFound) {
               model.add(0, this.wizardModel.newNativeUserAccountToAdd);
            }

            BNativeUserTable.replacementAccountUserName = this.wizardModel.newNativeUserAccountToAdd.getAccountName();
         }
      }

      @Override
      public boolean exit(int direction) {
         boolean back = direction == 1;
         if (back) {
            if (this.wizardModel.newNativeUserAccountToAdd != null) {
               BNativeUserTable.UserModel model = (BNativeUserTable.UserModel)this.userTable.getModel();

               for (int i = 0; i < model.getRowCount(); i++) {
                  if (model.getUser(i).getAccountName().equalsIgnoreCase(this.wizardModel.newNativeUserAccountToAdd.getAccountName())) {
                     model.remove(i);
                     break;
                  }
               }
            }

            return true;
         } else if (this.wizardModel.forceChangeUsers && !this.wizardModel.forceChangeUserList.isEmpty()) {
            BDialog.error(
               this.wizardModel.owner,
               LEX.getText("CommissioningWizard.authNativeUsersStep.notFipsReady.title"),
               LEX.getText("CommissioningWizard.authNativeUsersStep.notFipsReady.info")
            );
            return false;
         } else {
            this.afterUsers = this.userTable.getUsers();
            this.wizardModel.nativeUsersToAdd.clear();
            this.wizardModel.nativeUsersToAdd.addAll(this.afterUsers.values());
            this.beforeUsers.values().forEach(this.wizardModel.nativeUsersToAdd::remove);
            this.wizardModel.nativeUsersToRemove.clear();
            this.wizardModel.nativeUsersToRemove.addAll(this.beforeUsers.values());
            this.afterUsers.values().forEach(this.wizardModel.nativeUsersToRemove::remove);
            this.wizardModel.nativePasswordsToChange.clear();
            Set<String> beforeSet = this.beforeUsers.keySet();
            Set<String> afterSet = this.afterUsers.keySet();
            boolean beforeIsLarger = beforeSet.size() > afterSet.size();
            Set<String> intersectionSet = new HashSet<>(beforeIsLarger ? afterSet : beforeSet);
            intersectionSet.retainAll(beforeIsLarger ? beforeSet : afterSet);

            for (String intersection : intersectionSet) {
               UserAccount afterAccount = this.afterUsers.get(intersection);
               if (afterAccount.getPassword() != null && afterAccount.getOldPassword() != null) {
                  this.wizardModel.nativePasswordsToChange.add(afterAccount);
               }
            }

            this.wizardModel.authMessageQueue.clear();
            UserAccount toRemove = null;

            for (UserAccount accountToAdd : this.wizardModel.nativeUsersToAdd) {
               if (this.wizardModel.newNativeUserAccountToAdd != null
                  && this.wizardModel.newNativeUserAccountToAdd.getAccountName().equalsIgnoreCase(accountToAdd.getAccountName())) {
                  toRemove = accountToAdd;
               } else {
                  AddUserMessage addUserMessage = new AddUserMessage(accountToAdd);
                  AddMemberMessage addMemberMessage = new AddMemberMessage(this.userTable.getGroup(), accountToAdd);
                  this.wizardModel.authMessageQueue.add(addUserMessage);
                  this.wizardModel.authMessageQueue.add(addMemberMessage);
               }
            }

            if (toRemove != null) {
               this.wizardModel.nativeUsersToAdd.remove(toRemove);
            }

            toRemove = null;

            for (UserAccount accountToRemove : this.wizardModel.nativeUsersToRemove) {
               if (this.wizardModel.defaultAccountPresent && this.wizardModel.defaultAccountUserName.equalsIgnoreCase(accountToRemove.getAccountName())) {
                  toRemove = accountToRemove;
               } else {
                  DeleteMemberMessage deleteMemberMessage = new DeleteMemberMessage(this.userTable.getGroup(), accountToRemove);
                  DeleteUserMessage deleteUserMessage = new DeleteUserMessage(accountToRemove);
                  this.wizardModel.authMessageQueue.add(deleteMemberMessage);
                  this.wizardModel.authMessageQueue.add(deleteUserMessage);
               }
            }

            if (toRemove != null) {
               this.wizardModel.nativeUsersToRemove.remove(toRemove);
            }

            for (UserAccount accountToChange : this.wizardModel.nativePasswordsToChange) {
               ChangePasswordMessage changeMessage = new ChangePasswordMessage(
                  accountToChange, accountToChange.getOldPassword(), accountToChange.getPassword(), false
               );
               this.wizardModel.authMessageQueue.add(changeMessage);
            }

            String userNameAtFinish;
            if (this.wizardModel.newNativeUserAccountToAdd != null) {
               userNameAtFinish = this.wizardModel.newNativeUserAccountToAdd.getAccountName();
            } else {
               BUsernameAndPassword sessionCred = (BUsernameAndPassword)this.wizardModel.daemonSession.getCredentials();
               userNameAtFinish = NativeAccount.fullyQualifiedToUsername(sessionCred.getUsername());
            }

            this.wizardModel.newCredentials = null;
            ChangePasswordMessage currentUserPasswordMessage = null;

            for (XmlResponseMessage current : this.wizardModel.authMessageQueue) {
               if (current instanceof ChangePasswordMessage) {
                  ChangePasswordMessage message = (ChangePasswordMessage)current;
                  UserAccount account = message.getAccount();
                  if (account.getAccountName().equals(userNameAtFinish)) {
                     currentUserPasswordMessage = message;
                     break;
                  }
               }
            }

            boolean changed = this.wizardModel.authMessageQueue.size() > 0
               || this.wizardModel.newNativeUserAccountToAdd != null
               || this.wizardModel.defaultNativeUserAccountToRemove != null;
            if (changed) {
               if (currentUserPasswordMessage != null) {
                  this.wizardModel.authMessageQueue.remove(currentUserPasswordMessage);
                  this.wizardModel
                     .authMessageQueue
                     .add(
                        new ChangePasswordMessage(
                           currentUserPasswordMessage.getAccount(),
                           currentUserPasswordMessage.getOldPassword(),
                           currentUserPasswordMessage.getNewPassword(),
                           true
                        )
                     );
                  String accountName = currentUserPasswordMessage.getAccount().getAccountName();
                  String encodedPassword = currentUserPasswordMessage.getNewPassword();
                  String decodedPassword = null;

                  try {
                     decodedPassword = LegacyStorageUtil.decode(encodedPassword);
                  } catch (IOException var16) {
                  }

                  this.wizardModel.newCredentials = new BUsernameAndPassword(accountName, BPassword.make(decodedPassword));
               }

               this.wizardModel.authUsersChanged = true;
            }

            return true;
         }
      }

      @Override
      public int getMode() {
         return 3;
      }

      @Override
      public boolean isSkipped() {
         return this.wizardModel.skipAuth || this.wizardModel.skipConfigUsers;
      }
   }

   private class NextStepCommand extends ToggleCommand {
      private CommissioningWizardStep fromStep;
      private CommissioningWizardStep toStep;

      public NextStepCommand(BWidget owner, CommissioningWizardStep pFromStep, CommissioningWizardStep pToStep, String lexTag, Object[] lexiconArguments) {
         super(owner, CommissioningWizardStep.LEX.getText(lexTag, lexiconArguments));
         this.fromStep = pFromStep;
         this.toStep = pToStep;
      }

      public CommandArtifact doInvoke() {
         if (this.isSelected()) {
            this.fromStep.nextStep = this.toStep;
            this.toStep.priorStep = this.fromStep;
         }

         return null;
      }
   }
}
