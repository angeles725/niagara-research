package com.tridium.workbench.user;

import com.tridium.authn.BAuthenticationService;
import com.tridium.fox.sys.BFoxSession;
import com.tridium.workbench.auth.PasswordUtils;
import com.tridium.workbench.util.WbUtil;
import javax.baja.authn.BAuthenticationScheme;
import javax.baja.authn.BPasswordAuthenticationScheme;
import javax.baja.naming.BISession;
import javax.baja.nre.annotations.AgentOn;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.registry.TypeInfo;
import javax.baja.security.BAbstractAuthenticator;
import javax.baja.security.BPassword;
import javax.baja.security.BPasswordAuthenticator;
import javax.baja.security.BPasswordCache;
import javax.baja.security.PermissionException;
import javax.baja.sys.BAbsTime;
import javax.baja.sys.BComplex;
import javax.baja.sys.BComponent;
import javax.baja.sys.BObject;
import javax.baja.sys.BString;
import javax.baja.sys.BValue;
import javax.baja.sys.Context;
import javax.baja.sys.Flags;
import javax.baja.sys.Property;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;
import javax.baja.ui.CommandArtifact;
import javax.baja.user.BUser;
import javax.baja.user.BUserService;
import javax.baja.util.Lexicon;
import javax.baja.workbench.BWbEditor;
import javax.baja.workbench.mgr.BAbstractManager;
import javax.baja.workbench.mgr.BMgrTable;
import javax.baja.workbench.mgr.MgrColumn;
import javax.baja.workbench.mgr.MgrController;
import javax.baja.workbench.mgr.MgrEdit;
import javax.baja.workbench.mgr.MgrEditRow;
import javax.baja.workbench.mgr.MgrModel;
import javax.baja.workbench.mgr.MgrTagDictionary;
import javax.baja.workbench.mgr.MgrTypeInfo;
import javax.baja.workbench.mgr.MgrColumn.MixIn;
import javax.baja.workbench.mgr.MgrColumn.Name;
import javax.baja.workbench.mgr.MgrColumn.Prop;

@NiagaraType(
   agent = {@AgentOn(
      types = {"baja:UserService"},
      requiredPermissions = "W"
   )}
)
public class BUserManager extends BAbstractManager {
   public static final Type TYPE = Sys.loadType(BUserManager.class);
   static final Lexicon lex = Lexicon.make(BUserManager.class);
   static final String lexNever = lex.getText("never");
   static final String lexExpired = lex.getText("expired");
   MgrColumn colName = new BUserManager.UserNameColumn();
   MgrColumn colFullName = new BUserManager.UserProp(BUser.fullName, 1);
   MgrColumn colEnabled = new BUserManager.UserProp(BUser.enabled, 1);
   MgrColumn colExpiration = new BUserManager.ExpirationColumn();
   MgrColumn colLockOut = new BUserManager.UserProp(BUser.lockOut, 2);
   MgrColumn colRoles = new BUserManager.UserProp(BUser.roles, 3);
   MgrColumn colAllowConcurrentSession = new BUserManager.UserProp(BUser.allowConcurrentSessions, 1);
   MgrColumn colAutoLogoffSettings = new BUserManager.UserProp(BUser.autoLogoffSettings, 1);
   MgrColumn colLanguage = new BUserManager.UserProp(BUser.language, 1);
   MgrColumn colNetworkUser = new BUserManager.UserProp(BUser.networkUser, 1);
   MgrColumn colPrototype = new BUserManager.UserProp(BUser.prototypeName, 1);
   MgrColumn colAuthScheme = new BUserManager.UserProp(BUser.authenticationSchemeName, 1);
   MgrColumn colAuth = new BUserManager.AuthenticatorColumn();
   MgrColumn colEmail = new BUserManager.UserProp(BUser.email, 1);
   MgrColumn colFacets = new BUserManager.UserProp(BUser.facets, 3);
   MgrColumn colNavFile = new BUserManager.UserProp(BUser.navFile, 3);
   MgrColumn colCellPhoneNumber = new BUserManager.UserProp(BUser.cellPhoneNumber, 3);
   protected MgrColumn[] cols = new MgrColumn[]{
      this.colName,
      this.colFullName,
      this.colEnabled,
      this.colExpiration,
      this.colLockOut,
      this.colRoles,
      this.colAllowConcurrentSession,
      this.colAutoLogoffSettings,
      this.colNetworkUser,
      this.colPrototype,
      this.colLanguage,
      this.colAuthScheme,
      this.colAuth,
      this.colEmail,
      this.colCellPhoneNumber,
      this.colFacets,
      this.colNavFile
   };
   BUserService service;

   public Type getType() {
      return TYPE;
   }

   public void doLoadValue(BObject value, Context cx) {
      this.service = (BUserService)value;
      super.doLoadValue(value, cx);
   }

   protected MgrTagDictionary makeTagDictionary() {
      return null;
   }

   protected MgrModel makeModel() {
      return new BUserManager.Model(this);
   }

   protected MgrController makeController() {
      return new BUserManager.Controller(this);
   }

   void validate(MgrEditRow row) throws Exception {
      BUser user = (BUser)row.getTarget();
      BAbstractAuthenticator auth = (BAbstractAuthenticator)row.getCell(this.colAuth);
      auth.lease(1);
      String authName = ((BString)row.getCell(this.colAuthScheme)).getString();
      BAuthenticationService service = (BAuthenticationService)WbUtil.findService(this, BAuthenticationService.TYPE);
      service.lease(1);
      BAuthenticationScheme scheme = service.getAuthenticationScheme(authName);
      if (scheme instanceof BPasswordAuthenticationScheme && auth instanceof BPasswordAuthenticator) {
         BPasswordAuthenticationScheme passScheme = (BPasswordAuthenticationScheme)scheme;
         passScheme.lease(1);
         BPasswordAuthenticator passAuth = (BPasswordAuthenticator)auth;
         BPassword pass = passAuth.getPassword();
         if (pass.getPasswordEncoder().isReversible()) {
            BPasswordAuthenticator.checkPassword(user, passScheme, passAuth.getPasswordConfig(), pass, this.getCurrentContext());
         }
      }
   }

   class AuthenticatorColumn extends BUserManager.UserProp {
      AuthenticatorColumn() {
         super(BUser.authenticator, 3);
      }

      public BValue load(MgrEditRow row) {
         BValue auth = row.getTarget().get(this.prop);
         if (auth instanceof BPasswordCache && !((BPasswordCache)auth).isMounted()) {
            BPassword pass = ((BPasswordCache)auth).getPassword();
            if (pass.isDefault()) {
               ((BPasswordCache)auth).setPassword(BPassword.make(""));
            }
         } else if (auth != null
            && BUserManager.ClientCertAuthTypeHolder.CLIENT_CERT_AUTH_TYPE != null
            && auth.getType().is(BUserManager.ClientCertAuthTypeHolder.CLIENT_CERT_AUTH_TYPE)) {
            auth = auth.newCopy(true);
         }

         return auth;
      }

      public void save(MgrEditRow row, BValue value, Context cx) {
         BComponent target = row.getTarget();
         BValue old = target.get(this.prop);
         if (!old.equivalent(value)) {
            BValue authCopy = value.newCopy();
            if (authCopy instanceof BPasswordCache) {
               ((BPasswordCache)authCopy).setPassword(((BPasswordCache)value).getPassword());
            }

            target.set(this.prop, authCopy, cx);
         }
      }
   }

   private static final class ClientCertAuthTypeHolder {
      public static final TypeInfo CLIENT_CERT_AUTH_TYPE;

      static {
         TypeInfo typeInfo;
         try {
            typeInfo = Sys.getType("clientCertAuth:ClientCertAuthenticator").getTypeInfo();
         } catch (Exception var2) {
            typeInfo = null;
         }

         CLIENT_CERT_AUTH_TYPE = typeInfo;
      }
   }

   class Controller extends MgrController {
      Controller(BUserManager mgr) {
         super(mgr);
         BISession session = this.getManager().getCurrentValueSession();
         if (!PasswordUtils.isPasswordChangeAllowed(session)) {
            this.newCommand.setEnabled(false);
         }
      }

      public MgrEdit makeEdit(String label) {
         return BUserManager.this.new Edit(this.getManager(), label);
      }
   }

   class Edit extends MgrEdit {
      Edit(BAbstractManager mgr, String label) {
         super(mgr, label);
      }

      public CommandArtifact invoke(Context cx) throws Exception {
         BFoxSession foxSession = null;
         BISession session = BUserManager.this.getCurrentValueSession();
         if (session instanceof BFoxSession) {
            foxSession = (BFoxSession)session;
            foxSession.setThreadLocalSessionId();
         }

         CommandArtifact var4;
         try {
            var4 = super.invoke(cx);
         } finally {
            if (foxSession != null) {
               foxSession.clearThreadLocalSessionId();
            }
         }

         return var4;
      }

      public void validate(MgrEditRow row) throws Exception {
         BUserManager.this.validate(row);
      }

      public boolean isReadonly(MgrEditRow[] selectedRows, MgrColumn col) {
         boolean readonly = super.isReadonly(selectedRows, col);
         if (!readonly) {
            BUser currentUser = null;
            Property colProp = col instanceof BUserManager.UserProp ? ((BUserManager.UserProp)col).getProperty() : null;
            if (colProp != null || col instanceof MixIn) {
               try {
                  Context cx = this.getManager().getCurrentContext();
                  BString username = cx != null ? (BString)cx.getFacet("username") : null;
                  if (username != null) {
                     currentUser = BUserManager.this.service.getUser(username.toString());
                  }
               } catch (Exception var12) {
               }
            }

            for (int i = 0; i < selectedRows.length; i++) {
               BUser user = (BUser)selectedRows[i].getTarget();
               BComponent parentComp = (BComponent)user.getParent();
               Property prop = user.getPropertyInParent();
               if (parentComp != null && prop != null && !prop.isFrozen() && Flags.isReadonly(parentComp, prop)) {
                  readonly = true;
                  break;
               }

               if (currentUser != null && colProp != null) {
                  try {
                     currentUser.checkWrite(user, colProp);
                  } catch (PermissionException var13) {
                     readonly = true;
                     break;
                  }
               } else if (colProp != null) {
                  readonly = Flags.isReadonly(user, colProp);
               } else if (currentUser != null && col instanceof MixIn) {
                  Object mixin = ((MixIn)col).get(user);
                  if (mixin instanceof BComplex) {
                     prop = ((BComplex)mixin).getPropertyInParent();
                     if (prop != null) {
                        try {
                           currentUser.checkWrite(user, prop);
                        } catch (PermissionException var14) {
                           readonly = true;
                           break;
                        } catch (Exception var15) {
                        }
                     }
                  }
               }
            }
         }

         return readonly;
      }
   }

   class ExpirationColumn extends BUserManager.UserProp {
      ExpirationColumn() {
         super(BUser.expiration, 1);
      }

      public BWbEditor toEditor(MgrEditRow[] rows, int colIndex, BWbEditor currentEditor) {
         BWbEditor editor = super.toEditor(rows, colIndex, currentEditor);
         boolean enabled = true;

         for (int i = 0; i < rows.length; i++) {
            enabled &= !"admin".equals(((BUser)rows[i].getTarget()).getUsername());
         }

         if (editor != null) {
            editor.setEnabled(enabled);
         }

         return editor;
      }

      public String toDisplayString(Object row, Object value, Context cx) {
         BAbsTime t = (BAbsTime)value;
         if (t.isNull()) {
            return BUserManager.lexNever;
         } else {
            return BUser.isExpired(t) ? BUserManager.lexExpired : t.toDateString(cx);
         }
      }
   }

   class Model extends MgrModel {
      Model(BUserManager mgr) {
         super(mgr);
      }

      protected BMgrTable makeTable() {
         return new BUserMgrTable(this);
      }

      protected String makeTableTitle() {
         return BUserManager.TYPE.getDisplayName(null);
      }

      protected MgrColumn[] makeColumns() {
         return this.appendMixInColumns(BUserManager.this.cols, BUser.TYPE);
      }

      public int getSubscribeDepth() {
         return 3;
      }

      public Type[] getIncludeTypes() {
         return new Type[]{BUser.TYPE};
      }

      public MgrTypeInfo[] getNewTypes() {
         return new MgrTypeInfo[]{MgrTypeInfo.make(new BUser())};
      }

      public BComponent newInstance(MgrTypeInfo type) {
         return (BUser)BUserManager.this.service.getUserPrototypes().getDefaultPrototype().newCopy();
      }
   }

   class UserNameColumn extends Name {
      public Object get(Object target) {
         String s = (String)super.get(target);
         if (s.equals("Admin")) {
            return BString.make("admin");
         } else {
            return s.equals("Guest") ? BString.make("guest") : s;
         }
      }
   }

   static class UserProp extends Prop {
      public UserProp(Property prop, int flags) {
         super(prop, flags);
      }

      public Property getProperty() {
         return this.prop;
      }
   }
}
