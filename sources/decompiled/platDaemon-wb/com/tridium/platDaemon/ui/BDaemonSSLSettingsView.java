package com.tridium.platDaemon.ui;

import com.tridium.crypto.core.io.CryptoSupport;
import com.tridium.nre.security.SecretChars;
import com.tridium.nre.util.Version;
import com.tridium.platDaemon.BDaemonSSLStatus;
import com.tridium.platform.BPlatformSSLSettings;
import com.tridium.platform.daemon.BDaemonSession;
import com.tridium.platform.daemon.BHostProperties;
import com.tridium.platform.ui.util.DialogCommand;
import com.tridium.platform.ui.util.PortConfigController;
import com.tridium.ui.theme.Theme;
import com.tridium.ui.util.LabelUtil;
import com.tridium.workbench.fieldeditors.BBooleanFE;
import com.tridium.workbench.fieldeditors.BDefaultPasswordFE;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.baja.nre.annotations.NiagaraAction;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.security.AuthenticationException;
import javax.baja.security.BPassword;
import javax.baja.security.crypto.BSslTlsEnum;
import javax.baja.security.crypto.BTlsCipherSuiteGroup;
import javax.baja.sys.Action;
import javax.baja.sys.BBoolean;
import javax.baja.sys.BFacets;
import javax.baja.sys.BIcon;
import javax.baja.sys.BasicContext;
import javax.baja.sys.Context;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;
import javax.baja.ui.BDropDown;
import javax.baja.ui.BListDropDown;
import javax.baja.ui.BTextDropDown;
import javax.baja.ui.BTextField;
import javax.baja.ui.enums.BHalign;
import javax.baja.ui.enums.BScrollBarPolicy;
import javax.baja.ui.enums.BValign;
import javax.baja.ui.event.BWidgetEvent;
import javax.baja.ui.list.BList;
import javax.baja.ui.list.DefaultListModel;
import javax.baja.ui.pane.BConstrainedPane;
import javax.baja.ui.pane.BGridPane;
import javax.baja.ui.pane.BScrollPane;
import javax.baja.ui.text.BTextEditor;
import javax.baja.ui.util.UiLexicon;
import javax.baja.util.LexiconText;
import javax.baja.workbench.BWbPlugin;
import javax.baja.xml.XElem;
import javax.baja.xml.XParser;

@NiagaraType
@NiagaraAction(
   name = "settingModified",
   parameterType = "BWidgetEvent",
   defaultValue = "new BWidgetEvent()",
   flags = 4
)
public class BDaemonSSLSettingsView extends BDaemonSessionView {
   public static final Action settingModified = newAction(4, new BWidgetEvent(), null);
   public static final Type TYPE = Sys.loadType(BDaemonSSLSettingsView.class);
   private DialogCommand saveCommand = null;
   private BGridPane grid = null;
   private BPlatformSSLSettings settings = null;
   private BHostProperties hostProperties = null;
   private BListDropDown sslStatusDd = null;
   private BTextField sslPortTextField = null;
   private BTextDropDown sslAliasTextDd = null;
   private BListDropDown sslAlgTypeDd = null;
   private BListDropDown tlsCipherSuiteGroupDd = null;
   private BBooleanFE tlsExtendedMasterSecretFE = null;
   private BDefaultPasswordFE sslKeyPassphraseFE = null;
   public static BIcon ICON = BIcon.std("daemon.png");
   private static final boolean EDITABLE = true;
   private static final int MAX_ALIAS_DISPLAY_WIDTH = 25;
   private static final int MIN_ALIAS_DISPLAY_WIDTH = 8;
   public static final Logger LOG = Logger.getLogger("platDaemon");
   public static final Version KEY_PASSWORD_SUPPORT_VERSION = new Version("4.13");

   public void settingModified(BWidgetEvent parameter) {
      this.invoke(settingModified, parameter, null);
   }

   @Override
   public Type getType() {
      return TYPE;
   }

   public BDaemonSSLSettingsView() {
      this.grid = new BGridPane(2);
      this.grid.setRowGap(5.0);
      this.grid.setHalign(BHalign.center);
      this.grid.setValign(BValign.center);
      BScrollPane scrollPane = new BScrollPane(this.grid);
      scrollPane.setViewportBackground(Theme.scrollPane().getControlBackground());
      scrollPane.setBorderPolicy(BScrollBarPolicy.asNeeded);
      BConstrainedPane bounded = new BConstrainedPane(scrollPane);
      bounded.setMinSize(225.0, 130.0);
      this.setContent(bounded);
      this.saveCommand = new BDaemonSSLSettingsView.SaveCommand();
   }

   @Override
   protected void doLoadSession(BDaemonSession session, Context cx) throws AuthenticationException {
      if (session.getHostProperties().getSslSupported()) {
         this.hostProperties = session.getHostProperties();
         BPlatformSSLSettings settings = this.hostProperties.getSslSettings();
         this.settings = new BPlatformSSLSettings(settings);
         this.createSSLEnabledDropDown(settings);
         this.createSSLPortTextField(settings);
         loadAliases(session, settings);
         this.createKeyAlias(settings);
         if (new Version(this.hostProperties.getDaemonVersion()).compareTo(KEY_PASSWORD_SUPPORT_VERSION) >= 0) {
            this.createKeyPassword(settings);
         }

         this.createAlgTypeDropDown(settings);

         try {
            if (new Version(this.hostProperties.getDaemonVersion()).compareTo(CryptoSupport.MIN_MASTER_SECRET_NIAGARA_VERSION) >= 0) {
               this.createExtendedMasterSecretDropDown(settings);
            }
         } catch (Exception var6) {
         }

         try {
            if (new Version(this.hostProperties.getDaemonVersion()).compareTo(CryptoSupport.MIN_CIPHER_SUITE_GROUP_NIAGARA_VERSION) >= 0) {
               this.createCipherSuitesGroupDropDown(settings);
            }
         } catch (Exception var5) {
         }
      }
   }

   private void createSSLEnabledDropDown(BPlatformSSLSettings sslSettings) {
      LabelUtil.addLabelWidgetPair(this.grid, LexiconText.make(TYPE, "PlatformAdministration.sslStatus"), this.sslStatusDd = new BListDropDown());
      DefaultListModel model = (DefaultListModel)this.sslStatusDd.getList().getModel();
      int[] ordinals = BDaemonSSLStatus.make(0).getRange().getOrdinals();

      for (int i = 0; i < ordinals.length; i++) {
         if (i != 3) {
            model.addItem(null, BDaemonSSLStatus.make(ordinals[i]));
         }
      }

      int index = 0;
      if (sslSettings.getSslEnabled()) {
         index = sslSettings.getSslOnly() ? 2 : 1;
      }

      this.sslStatusDd.getList().setSelectedIndex(index);
      this.linkTo(this.sslStatusDd, BDropDown.valueModified, settingModified);
      this.sslStatusDd.setEnabled(!sslSettings.getSslEnabledStateReadonly());
   }

   private void createSSLPortTextField(BPlatformSSLSettings sslSettings) {
      String sslPort = sslSettings.getSslPort() < 0 ? "" : String.valueOf(sslSettings.getSslPort());
      LabelUtil.addLabelWidgetPair(
         this.grid, LexiconText.make(TYPE, "PlatformAdministration.command.sslSettings.sslPort"), this.sslPortTextField = new BTextField(sslPort, 5)
      );
      this.sslPortTextField.setController(new PortConfigController());
      this.linkTo(this.sslPortTextField, BTextEditor.textModified, settingModified);
   }

   private void createKeyAlias(BPlatformSSLSettings sslSettings) {
      String currentAlias = sslSettings.getKeyAlias();
      List<String> aliases = sslSettings.getAliases();
      int maxWidth = 8;
      boolean currentAliasFound = false;
      if (aliases != null) {
         for (String alias : aliases) {
            if (alias.equalsIgnoreCase(currentAlias)) {
               currentAliasFound = true;
            }

            if (alias.length() > maxWidth) {
               if (alias.length() >= 25) {
                  maxWidth = 25;
                  break;
               }

               maxWidth = alias.length();
            }
         }
      } else {
         aliases = new ArrayList<>();
      }

      if (!currentAliasFound) {
         aliases.add(currentAlias);
      }

      this.sslAliasTextDd = new BTextDropDown(aliases, maxWidth, true);
      BList list = this.sslAliasTextDd.getList();

      for (int i = 0; i < list.getItemCount(); i++) {
         String listAlias = (String)list.getItem(i);
         if (listAlias.equalsIgnoreCase(currentAlias)) {
            list.setSelectedIndex(i);
            this.sslAliasTextDd.setText((String)list.getSelectedItem());
            break;
         }
      }

      LabelUtil.addLabelWidgetPair(this.grid, LexiconText.make(TYPE, "PlatformAdministration.command.sslSettings.sslKeyAlias"), this.sslAliasTextDd);
      this.linkTo(this.sslAliasTextDd, BDropDown.valueModified, settingModified);
   }

   private void createKeyPassword(BPlatformSSLSettings sslSettings) {
      this.sslKeyPassphraseFE = new BDefaultPasswordFE(1);
      BFacets facets = BFacets.make("placeholderText", "%lexicon(workbench:default.password.placeholder.label)%");
      BasicContext cx = new BasicContext(facets);
      this.sslKeyPassphraseFE.loadValue(BPassword.DEFAULT, cx);
      LabelUtil.addLabelWidgetPair(this.grid, LexiconText.make(TYPE, "PlatformAdministration.command.sslSettings.sslKeyPassword"), this.sslKeyPassphraseFE);
      this.linkTo(this.sslKeyPassphraseFE, BDefaultPasswordFE.modified, settingModified);
   }

   private static void loadAliases(BDaemonSession session, BPlatformSSLSettings settings) {
      BHostProperties props = session.getHostProperties();
      if (props.getSslSupported() && props.supportsServlet("crypto")) {
         try {
            InputStream in = session.getInputStream("crypto?action=sendServerAliases", BDaemonSession.DEFAULT_TIMEOUT, null, null);
            XElem aliasesElem = XParser.make(in).parse();
            settings.loadAliases(aliasesElem);
         } catch (Exception var5) {
            if (log.isLoggable(Level.FINE)) {
               log.log(Level.FINE, "Exception loading aliases", (Throwable)var5);
            }
         }
      }
   }

   private void createAlgTypeDropDown(BPlatformSSLSettings sslSettings) {
      LabelUtil.addLabelWidgetPair(
         this.grid, LexiconText.make(TYPE, "PlatformAdministration.command.sslSettings.sslAlgType"), this.sslAlgTypeDd = new BListDropDown()
      );
      boolean remoteSupportsTLS1dot3 = false;

      try {
         if (new Version(this.hostProperties.getDaemonVersion()).compareTo(CryptoSupport.MIN_TLS_1_3_NIAGARA_VERSION) >= 0) {
            remoteSupportsTLS1dot3 = true;
         }
      } catch (IllegalArgumentException var12) {
         log.warning("Failed to determine remote daemon version (" + var12 + "), using legacy TLS level support");
      }

      DefaultListModel algTypeModel = (DefaultListModel)this.sslAlgTypeDd.getList().getModel();
      int[] algTypeOrdinals = BSslTlsEnum.make(1).getRange().getOrdinals();
      int numItemsAdded = 0;
      boolean isFipsMode = sslSettings.getFipsMode();

      for (int algTypeOrdinal : algTypeOrdinals) {
         BSslTlsEnum type = BSslTlsEnum.make(algTypeOrdinal);
         if ((!isFipsMode || this.isFipsApprovedTlsLevel(type)) && (type != BSslTlsEnum.tlsv1_3 || type == BSslTlsEnum.tlsv1_3 && remoteSupportsTLS1dot3)) {
            algTypeModel.addItem(null, type);
            if (type == sslSettings.getSslAlgType()) {
               this.sslAlgTypeDd.getList().setSelectedIndex(numItemsAdded);
            }

            numItemsAdded++;
         }
      }

      this.linkTo(this.sslAlgTypeDd, BDropDown.valueModified, settingModified);
      boolean canEdit = this.hostProperties.isNiagara4();
      this.sslAlgTypeDd.setEnabled(canEdit);
   }

   private boolean isFipsApprovedTlsLevel(BSslTlsEnum type) {
      return type == BSslTlsEnum.tlsv1_2 || type == BSslTlsEnum.tlsv1_3;
   }

   private void createCipherSuitesGroupDropDown(BPlatformSSLSettings settings) {
      LabelUtil.addLabelWidgetPair(
         this.grid, LexiconText.make(TYPE, "PlatformAdministration.command.sslSettings.tlsCipherSuiteGroup"), this.tlsCipherSuiteGroupDd = new BListDropDown()
      );
      DefaultListModel tlsCipherSuiteGroupsModel = (DefaultListModel)this.tlsCipherSuiteGroupDd.getList().getModel();
      int[] cipherSuitesGroupOrdinal = BTlsCipherSuiteGroup.make(0).getRange().getOrdinals();

      for (int i = 0; i < cipherSuitesGroupOrdinal.length; i++) {
         BTlsCipherSuiteGroup type = BTlsCipherSuiteGroup.make(cipherSuitesGroupOrdinal[i]);
         tlsCipherSuiteGroupsModel.addItem(null, type);
         if (type == settings.getTlsCipherSuiteGroup()) {
            this.tlsCipherSuiteGroupDd.getList().setSelectedIndex(i);
         }
      }

      this.linkTo(this.tlsCipherSuiteGroupDd, BDropDown.valueModified, settingModified);
      boolean canEdit = !settings.getFipsMode();
      this.tlsCipherSuiteGroupDd.setEnabled(canEdit);
   }

   private void createExtendedMasterSecretDropDown(BPlatformSSLSettings settings) {
      LabelUtil.addLabelWidgetPair(
         this.grid,
         LexiconText.make(TYPE, "PlatformAdministration.command.sslSettings.extendedMasterSecret"),
         this.tlsExtendedMasterSecretFE = new BBooleanFE()
      );
      if (!settings.getUseExtendedMasterSecret().equals("true") && !settings.getUseExtendedMasterSecret().equals("false")) {
         this.tlsExtendedMasterSecretFE.loadValue(BBoolean.FALSE);
         this.tlsExtendedMasterSecretFE.setEnabled(false);
         this.tlsExtendedMasterSecretFE.setReadonly(true);
      } else {
         this.tlsExtendedMasterSecretFE.loadValue(BBoolean.make(settings.getUseExtendedMasterSecret()));
      }

      this.linkTo(this.tlsExtendedMasterSecretFE, BWbPlugin.setModified, settingModified);
   }

   public DialogCommand getSaveCommand() {
      return this.saveCommand;
   }

   public BPlatformSSLSettings getSettings() {
      this.settings.setKeyAlias(this.sslAliasTextDd.getText());
      if (this.sslKeyPassphraseFE != null) {
         try {
            BPassword keyPassword = (BPassword)this.sslKeyPassphraseFE.saveValue();
            SecretChars secretChars = keyPassword.getSecretChars();
            Throwable var3 = null;

            try {
               if (secretChars.size() > 0) {
                  this.settings.setKeyPassphrase(keyPassword);
               } else {
                  this.settings.setKeyPassphrase(BPassword.DEFAULT);
               }
            } catch (Throwable var15) {
               var3 = var15;
               throw var15;
            } finally {
               if (secretChars != null) {
                  if (var3 != null) {
                     try {
                        secretChars.close();
                     } catch (Throwable var14) {
                        var3.addSuppressed(var14);
                     }
                  } else {
                     secretChars.close();
                  }
               }
            }
         } catch (Exception var17) {
            LOG.log(Level.SEVERE, "error processing key password, setting to DEFAULT", (Throwable)var17);
            this.settings.setKeyPassphrase(BPassword.DEFAULT);
         }
      }

      this.settings.setSslAlgType((BSslTlsEnum)this.sslAlgTypeDd.getSelectedItem());
      this.settings.setSslPort(Integer.valueOf(this.sslPortTextField.getText()));
      this.settings.setSslEnabled(this.sslStatusDd.getSelectedIndex() == 1 || this.sslStatusDd.getSelectedIndex() == 2);
      this.settings.setSslOnly(this.sslStatusDd.getSelectedIndex() == 2);
      if (this.tlsCipherSuiteGroupDd != null) {
         this.settings.setTlsCipherSuiteGroup((BTlsCipherSuiteGroup)this.tlsCipherSuiteGroupDd.getSelectedItem());
      }

      if (this.tlsExtendedMasterSecretFE != null) {
         String useExtendedMasterSecret;
         try {
            useExtendedMasterSecret = ((BBoolean)this.tlsExtendedMasterSecretFE.saveValue()).getTag();
         } catch (Exception var13) {
            useExtendedMasterSecret = "true";
         }

         if (this.tlsExtendedMasterSecretFE != null) {
            this.settings.setUseExtendedMasterSecret(useExtendedMasterSecret);
         }
      }

      return this.settings;
   }

   public void doSettingModified(BWidgetEvent event) {
      this.saveCommand.setEnabled(this.sslPortTextField.getText().length() != 0);
   }

   public BIcon getIcon() {
      return ICON;
   }

   private class SaveCommand extends DialogCommand {
      public SaveCommand() {
         super(BDaemonSSLSettingsView.this, 1, UiLexicon.bajaui().getText("commands.save.label"), null, null, null);
         this.setEnabled(false);
      }
   }
}
