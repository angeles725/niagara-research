package com.tridium.workbench.user;

import com.tridium.fox.util.FoxRpcUtil;
import com.tridium.sys.schema.ComponentSlotMap;
import com.tridium.ui.theme.Theme;
import com.tridium.ui.theme.TreeTableTheme;
import com.tridium.util.HistoryCategoryUtil;
import com.tridium.workbench.category.Category;
import com.tridium.workbench.fieldeditors.BFrozenEnumFE;
import com.tridium.workbench.nav.NavMonitor;
import com.tridium.workbench.shell.BNiagaraWbShell;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import javax.baja.category.BCategoryMask;
import javax.baja.category.BCategoryService;
import javax.baja.category.BOrdToCategoryMap;
import javax.baja.collection.BITable;
import javax.baja.collection.Column;
import javax.baja.collection.TableCursor;
import javax.baja.gx.BBrush;
import javax.baja.gx.BColor;
import javax.baja.gx.BFont;
import javax.baja.gx.BImage;
import javax.baja.gx.Graphics;
import javax.baja.naming.BOrd;
import javax.baja.naming.BatchResolve;
import javax.baja.naming.SlotPath;
import javax.baja.naming.UnresolvedException;
import javax.baja.nav.BINavNode;
import javax.baja.nav.BNavRoot;
import javax.baja.nav.NavEvent;
import javax.baja.nav.NavListener;
import javax.baja.nre.annotations.AgentOn;
import javax.baja.nre.annotations.NiagaraAction;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.nre.util.Array;
import javax.baja.role.BRole;
import javax.baja.role.BRoleService;
import javax.baja.security.BIProtected;
import javax.baja.security.BPermissionsMap;
import javax.baja.sys.Action;
import javax.baja.sys.BComplex;
import javax.baja.sys.BComponent;
import javax.baja.sys.BIObject;
import javax.baja.sys.BLink;
import javax.baja.sys.BModule;
import javax.baja.sys.BObject;
import javax.baja.sys.BString;
import javax.baja.sys.Context;
import javax.baja.sys.Flags;
import javax.baja.sys.Property;
import javax.baja.sys.ServiceNotFoundException;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;
import javax.baja.ui.BDialog;
import javax.baja.ui.BLabel;
import javax.baja.ui.BMenu;
import javax.baja.ui.BSeparator;
import javax.baja.ui.BToolBar;
import javax.baja.ui.Command;
import javax.baja.ui.CommandArtifact;
import javax.baja.ui.ToggleCommand;
import javax.baja.ui.enums.BHalign;
import javax.baja.ui.event.BMouseEvent;
import javax.baja.ui.pane.BBorderPane;
import javax.baja.ui.pane.BEdgePane;
import javax.baja.ui.pane.BGridPane;
import javax.baja.ui.table.TableCellRenderer.Cell;
import javax.baja.ui.treetable.BTreeTable;
import javax.baja.ui.treetable.TreeTableCellRenderer;
import javax.baja.ui.treetable.TreeTableController;
import javax.baja.ui.treetable.TreeTableModel;
import javax.baja.ui.treetable.TreeTableNode;
import javax.baja.ui.util.BTitlePane;
import javax.baja.user.BUser;
import javax.baja.user.BUserService;
import javax.baja.util.LexiconModule;
import javax.baja.util.Version;
import javax.baja.virtual.BVirtualComponent;
import javax.baja.virtual.BVirtualGateway;
import javax.baja.workbench.BWbPlugin;
import javax.baja.workbench.view.BWbComponentView;

@NiagaraType(
   agent = {@AgentOn(
      types = {"baja:RoleService", "baja:UserService"},
      requiredPermissions = "W"
   )}
)
@NiagaraAction(
   name = "filterTypeModified",
   flags = 4
)
public class BPermissionsBrowser extends BWbComponentView {
   public static final Action filterTypeModified = newAction(4, null);
   public static final Type TYPE = Sys.loadType(BPermissionsBrowser.class);
   public static final LexiconModule LEXICON = LexiconModule.make(BPermissionsBrowser.class);
   private static final BModule module = Sys.getModuleForClass(BPermissionsBrowser.class);
   private static final Version VER_4_8 = new Version("4.8");
   private static final BBrush HIGHLIGHTED_BRUSH = BColor.make(13630415).toBrush();
   private static final BBrush CONFIGURED_BRUSH = BColor.make(16777147).toBrush();
   private static final BBrush EXPANDER_BRUSH = BColor.black.toBrush();
   private static final BBrush CONNECTING_BRUSH = BColor.make(192, 192, 192).toBrush();
   private static final BBrush DISABLED_BRUSH = BColor.make("#666").toBrush();
   private final BTreeTable table;
   private final BPermissionsBrowser.Model model;
   private final BGridPane queryPane;
   private final BFrozenEnumFE filterFE;
   private final ToggleCommand highlightAccessible;
   private Array<BPermissionsBrowser.Node> roots;
   private BComponent service;
   private BRoleService roleService;
   private BUserService userService;
   private BCategoryService catService;
   BRole[] roles;
   BPermissionsMap[] rolePermissions;
   private boolean[] dirty;
   private String adminRoleName;
   private static final BUser[] USER_TYPE_ARRAY = new BUser[0];
   private BUser[] users;
   private BPermissionsMap[] userPermissions;
   private BPermissionsBrowserFilterType currentFilterType;
   private boolean[] visible;
   private int visibleSize;
   private int[] viewIdx;

   public void filterTypeModified() {
      this.invoke(filterTypeModified, null, null);
   }

   public Type getType() {
      return TYPE;
   }

   public BPermissionsBrowser() {
      this.autoRegisterForComponentEvents = false;
      this.highlightAccessible = new BPermissionsBrowser.HighlightAccessible();
      this.model = new BPermissionsBrowser.Model();
      this.table = new BTreeTable();
      this.table.setMultipleSelection(false);
      this.table.setModel(this.model);
      this.table.setCellRenderer(new BPermissionsBrowser.Renderer());
      this.table.setController(new BPermissionsBrowser.Controller());
      this.filterFE = new BFrozenEnumFE();
      this.filterFE.loadValue(BPermissionsBrowserFilterType.DEFAULT);
      this.queryPane = new BGridPane(3);
      this.queryPane.setHalign(BHalign.left);
      this.queryPane.add(null, new BLabel(LEXICON.getText("permission.showPermissionsFor", this.getCurrentContext())));
      this.queryPane.add("filterFE", this.filterFE);
      BEdgePane pane = new BEdgePane();
      pane.setTop(new BBorderPane(this.queryPane));
      pane.setCenter(BTitlePane.makePane(LEXICON.getText("permission.title", this.getCurrentContext()), this.table, "Rows"));
      this.setContent(pane);
   }

   public void started() throws Exception {
      super.started();
      BNavRoot.INSTANCE.addNavListener(this.model);
   }

   public void stopped() throws Exception {
      super.stopped();
      BNavRoot.INSTANCE.removeNavListener(this.model);
   }

   public BMenu[] getViewMenus() {
      BMenu menu = new BMenu(LEXICON.getText("permissionsBrowser.menu.label", this.getCurrentContext()));
      menu.add(null, new BPermissionsBrowser.ShowConfigured());
      menu.add(null, this.highlightAccessible);
      return new BMenu[]{menu};
   }

   public BToolBar getViewToolBar() {
      BToolBar toolBar = new BToolBar();
      toolBar.add(null, new BPermissionsBrowser.ShowConfigured());
      toolBar.add(null, this.highlightAccessible);
      return toolBar;
   }

   public void doLoadValue(BObject obj, Context cx) {
      this.service = (BComponent)obj;
      this.registerForComponentEvents(this.service, 2);
      BComponent otherService = this.getOtherService();
      this.queryPane.setVisible(hasAccessToOtherService(otherService, cx));
      this.getCategoryService();
      this.highlightAccessible.setEnabled(this.isRemoteVersion4_8());
      this.removeFilterFELinks();
      if (this.service instanceof BRoleService) {
         this.currentFilterType = BPermissionsBrowserFilterType.roles;
         this.filterFE.loadValue(this.currentFilterType);
         this.roleService = (BRoleService)this.service;
         this.roles = (BRole[])this.roleService.getChildren(BRole.class);
         this.adminRoleName = this.roleService.getAdmin().getName();
         this.userService = (BUserService)otherService;
         this.users = null;
         this.loadRolePermissions();
         this.loadUserPermissions();
         this.loadVisible(this.roles.length);
      } else if (this.service instanceof BUserService) {
         this.currentFilterType = BPermissionsBrowserFilterType.users;
         this.filterFE.loadValue(this.currentFilterType);
         this.userService = (BUserService)this.service;
         this.loadVisibleUsers(cx);
         this.roleService = (BRoleService)otherService;
         if (this.roleService != null) {
            this.registerForComponentEvents(this.roleService, 2);
            this.roles = (BRole[])this.roleService.getChildren(BRole.class);
            this.adminRoleName = this.roleService.getAdmin().getName();
         } else {
            this.roles = null;
         }

         this.loadRolePermissions();
         this.loadUserPermissions();
         this.loadVisible(this.users.length);
      }

      this.addFilterFELinks();
      this.addRoots();
      if (!this.model.needsInitialization()) {
         this.model.updateTreeTable(true);
      }
   }

   private void loadVisibleUsers(Context cx) {
      List<BUser> usersList = new ArrayList<>();

      for (BUser user : (BUser[])this.userService.getChildren(BUser.class)) {
         if (!user.getDisplayName(cx).equals("guest") || user.getEnabled() || !user.getRoles().isEmpty()) {
            Property prop = user.getPropertyInParent();
            if (prop != null && !Flags.isHidden(this.userService, prop)) {
               usersList.add(user);
            }
         }
      }

      this.users = usersList.toArray(USER_TYPE_ARRAY);
   }

   private void addRoots() {
      if (this.roots == null) {
         this.roots = new Array(BPermissionsBrowser.Node.class);
      } else {
         this.roots.clear();
      }

      this.addRoot(this.service.getComponentSpace().getRootComponent());

      try {
         this.addRoot((BINavNode)BOrd.make("file:").get(this.service, this.getCurrentContext()));
      } catch (Exception var3) {
      }

      try {
         this.addRoot((BINavNode)BOrd.make("history:").get(this.service, this.getCurrentContext()));
      } catch (Exception var2) {
      }
   }

   private BComponent getOtherService() {
      String otherServiceOrd = this.service instanceof BRoleService ? "service:baja:UserService" : "service:baja:RoleService";

      try {
         return (BComponent)BOrd.make(otherServiceOrd).get(this.service, this.getCurrentContext());
      } catch (Exception var3) {
         return null;
      }
   }

   private static boolean hasAccessToOtherService(BComponent otherService, Context cx) {
      return otherService != null && otherService.getPermissions(cx).hasAdminWrite();
   }

   private void getCategoryService() {
      try {
         this.catService = (BCategoryService)BOrd.make("service:baja:CategoryService").get(this.service, this.getCurrentContext());
         this.registerForComponentEvents(this.catService, 1);
      } catch (ServiceNotFoundException | UnresolvedException var2) {
         throw new UnresolvedException("Permission to Category Service missing.");
      }
   }

   private void removeFilterFELinks() {
      for (BLink link : this.getLinks(filterTypeModified)) {
         this.remove(link);
      }
   }

   private void addFilterFELinks() {
      this.linkTo(this.filterFE, BWbPlugin.setModified, filterTypeModified);
      this.linkTo(this.filterFE, BWbPlugin.actionPerformed, filterTypeModified);
   }

   private void loadRolePermissions() {
      if (this.roles != null) {
         this.rolePermissions = new BPermissionsMap[this.roles.length];

         for (int i = 0; i < this.roles.length; i++) {
            this.rolePermissions[i] = this.roles[i].getPermissions();
         }

         this.dirty = new boolean[this.roles.length];
      } else {
         this.rolePermissions = null;
         this.dirty = null;
      }
   }

   private void loadUserPermissions() {
      if (this.users != null) {
         this.userPermissions = new BPermissionsMap[this.users.length];

         for (int i = 0; i < this.users.length; i++) {
            this.userPermissions[i] = this.users[i].getPermissions();
         }
      } else {
         this.userPermissions = null;
      }
   }

   private void addRoot(BINavNode nav) {
      try {
         if (nav instanceof BIProtected) {
            this.roots.add(new BPermissionsBrowser.Node(this.model, nav, this.getDeepOrMasks(nav).get(0)));
         }
      } catch (Exception var3) {
         System.out.println("Cannot add root: " + nav.getNavOrd());
         System.out.println("  " + var3);
      }
   }

   public BObject doSaveValue(BObject obj, Context cx) throws Exception {
      if (this.currentFilterType.equals(BPermissionsBrowserFilterType.roles)) {
         this.saveRolePermissions();
         return this.roleService;
      } else if (this.currentFilterType.equals(BPermissionsBrowserFilterType.users)) {
         if (this.roles != null) {
            this.saveRolePermissions();
         }

         return this.userService;
      } else {
         return null;
      }
   }

   private void saveRolePermissions() {
      for (int i = 0; i < this.roles.length; i++) {
         if (this.dirty[i]) {
            this.roles[i].setPermissions(this.rolePermissions[i]);
            this.dirty[i] = false;
         }
      }
   }

   public void doFilterTypeModified() throws Exception {
      BNiagaraWbShell shell = (BNiagaraWbShell)this.getWbShell();
      BPermissionsBrowserFilterType currentVal = (BPermissionsBrowserFilterType)this.filterFE.getCurrentValue();
      BPermissionsBrowserFilterType newVal = (BPermissionsBrowserFilterType)this.filterFE.saveValue();
      if (!newVal.equals(currentVal)) {
         if (newVal.equals(BPermissionsBrowserFilterType.roles)) {
            shell.hyperlink(BOrd.make("service:baja:RoleService|view:wbutil:PermissionsBrowser"));
         } else if (newVal.equals(BPermissionsBrowserFilterType.users)) {
            shell.hyperlink(BOrd.make("service:baja:UserService|view:wbutil:PermissionsBrowser"));
         }
      }
   }

   private String getFilteredColumnName(int col) {
      return this.currentFilterType.equals(BPermissionsBrowserFilterType.roles)
         ? this.roles[this.viewIdx[col - 1]].getDisplayName(this.getCurrentContext())
         : this.users[this.viewIdx[col - 1]].getDisplayName(this.getCurrentContext());
   }

   private BPermissionsMap getPermissionMap(int col) {
      return this.currentFilterType.equals(BPermissionsBrowserFilterType.roles)
         ? this.rolePermissions[this.viewIdx[col - 1]]
         : this.userPermissions[this.viewIdx[col - 1]];
   }

   private void updateVisible() {
      this.visibleSize = 0;

      for (int i = 0; i < this.visible.length; i++) {
         if (this.visible[i]) {
            this.viewIdx[this.visibleSize++] = i;
         }
      }
   }

   private void loadVisible(int len) {
      this.visible = new boolean[len];
      this.viewIdx = new int[len];
      this.visibleSize = len;

      for (int i = 0; i < len; this.viewIdx[i] = i++) {
         this.visible[i] = true;
      }
   }

   private boolean isRemoteVersion4_8() {
      try {
         Version remoteVersion = (Version)this.getCurrentValueSession().asObject().fw(404, "baja", null, null, null);
         return remoteVersion.compareTo(VER_4_8) >= 0;
      } catch (Exception var2) {
         return false;
      }
   }

   private List<BCategoryMask> getDeepOrMasks(BINavNode... navNodes) {
      if (!this.isRemoteVersion4_8()) {
         return navNodes.length < 1 ? Collections.emptyList() : Collections.nCopies(navNodes.length, BCategoryMask.NULL);
      } else {
         List<String> navOrds = new ArrayList<>();

         for (BINavNode navNode : navNodes) {
            navOrds.add(navNode.getNavOrd().relativizeToSession().toString());
         }

         List<BCategoryMask> deepOrMasks = Collections.emptyList();
         if (!navOrds.isEmpty()) {
            List<String> list = FoxRpcUtil.doSilentRpc(this.catService, "retrieveDeepOrMasks", new Object[]{navOrds})
               .orElse(Collections.nCopies(navNodes.length, BCategoryMask.NULL));
            deepOrMasks = list.stream().map(maskString -> (BCategoryMask)BCategoryMask.DEFAULT.decodeFromString(maskString)).collect(Collectors.toList());
         }

         return deepOrMasks;
      }
   }

   private class ConfigureColumns extends Command {
      public ConfigureColumns() {
         super(
            BPermissionsBrowser.this,
            BPermissionsBrowser.this.currentFilterType.equals(BPermissionsBrowserFilterType.roles)
               ? BPermissionsBrowser.LEXICON.getText("permission.showRoles", BPermissionsBrowser.this.getCurrentContext())
               : BPermissionsBrowser.LEXICON.getText("permission.showUsers", BPermissionsBrowser.this.getCurrentContext())
         );
      }

      public CommandArtifact doInvoke() {
         if (BPermissionsBrowser.this.currentFilterType.equals(BPermissionsBrowserFilterType.roles)) {
            Map<BComplex, Boolean> selectedRolesMap = new LinkedHashMap<>();

            for (int i = 0; i < BPermissionsBrowser.this.roles.length; i++) {
               selectedRolesMap.put(BPermissionsBrowser.this.roles[i], BPermissionsBrowser.this.visible[i]);
            }

            BPermissionsBrowserFilteredCheckListPane pane = new BPermissionsBrowserFilteredCheckListPane(
               BPermissionsBrowser.LEXICON.getText("permission.searchRoles", BPermissionsBrowser.this.getCurrentContext()),
               selectedRolesMap,
               BPermissionsBrowser.this.getCurrentContext()
            );
            if (1
               == BDialog.open(
                  BPermissionsBrowser.this, BPermissionsBrowser.LEXICON.getText("permission.roles", BPermissionsBrowser.this.getCurrentContext()), pane, 3
               )) {
               for (int i = 0; i < BPermissionsBrowser.this.roles.length; i++) {
                  BPermissionsBrowser.this.visible[i] = selectedRolesMap.get(BPermissionsBrowser.this.roles[i]);
               }
            }
         } else {
            Map<BComplex, Boolean> selectedUsersMap = new LinkedHashMap<>();

            for (int i = 0; i < BPermissionsBrowser.this.users.length; i++) {
               selectedUsersMap.put(BPermissionsBrowser.this.users[i], BPermissionsBrowser.this.visible[i]);
            }

            BPermissionsBrowserFilteredCheckListPane pane = new BPermissionsBrowserFilteredCheckListPane(
               BPermissionsBrowser.LEXICON.getText("permission.searchUsers", BPermissionsBrowser.this.getCurrentContext()),
               selectedUsersMap,
               BPermissionsBrowser.this.getCurrentContext()
            );
            if (1
               == BDialog.open(
                  BPermissionsBrowser.this, BPermissionsBrowser.LEXICON.getText("permission.users", BPermissionsBrowser.this.getCurrentContext()), pane, 3
               )) {
               for (int i = 0; i < BPermissionsBrowser.this.users.length; i++) {
                  BPermissionsBrowser.this.visible[i] = selectedUsersMap.get(BPermissionsBrowser.this.users[i]);
               }
            }
         }

         BPermissionsBrowser.this.updateVisible();
         BPermissionsBrowser.this.table.relayout();
         return null;
      }
   }

   private class Controller extends TreeTableController {
      private Controller() {
      }

      protected void cellDoubleClicked(BMouseEvent event, int row, int col) {
         if (col == 0) {
            super.cellDoubleClicked(event, row, 0);
         } else {
            if (BPermissionsBrowser.this.getPermissionMap(col).isSuperUser()) {
               BDialog.warning(
                  BPermissionsBrowser.this,
                  this.getEditPermissionsTitle(col),
                  BPermissionsBrowser.LEXICON.getText("permission.cannotModifySuperUser", BPermissionsBrowser.this.getCurrentContext())
               );
               return;
            }

            if (BPermissionsBrowser.this.currentFilterType.equals(BPermissionsBrowserFilterType.roles)) {
               this.editRolePermissions(col);
            } else {
               this.editUserRoles(col);
            }
         }
      }

      private void editRolePermissions(int col) {
         BPermissionsMap newMap = BPermissionGrid.open(
            BPermissionsBrowser.this.getWbShell(), BPermissionsBrowser.this.getPermissionMap(col), this.getMaxPermissions(), this.getEditPermissionsTitle(col)
         );
         if (newMap != null) {
            BPermissionsBrowser.this.rolePermissions[BPermissionsBrowser.this.viewIdx[col - 1]] = newMap;
            BPermissionsBrowser.this.dirty[BPermissionsBrowser.this.viewIdx[col - 1]] = true;
            BPermissionsBrowser.this.setModified();
            BPermissionsBrowser.this.repaint();
         }
      }

      private void editUserRoles(int col) {
         if (BPermissionsBrowser.this.roles != null && BPermissionsBrowser.this.rolePermissions != null) {
            BPermissionsBrowserUserRolesEditor editor = new BPermissionsBrowserUserRolesEditor.Builder()
               .forUser(BPermissionsBrowser.this.users[BPermissionsBrowser.this.viewIdx[col - 1]])
               .withCategories(this.getCategories(BPermissionsBrowser.this.userPermissions[BPermissionsBrowser.this.viewIdx[col - 1]]))
               .withViewUserPermissions(this.getMaxPermissions())
               .forRoles(BPermissionsBrowser.this.roles)
               .forRolePermissions(BPermissionsBrowser.this.rolePermissions)
               .makeEditor(BPermissionsBrowser.this.getCurrentContext());
            int dialogResponse = BDialog.open(BPermissionsBrowser.this, this.getEditPermissionsTitle(col), editor, 3);
            if (dialogResponse == 1) {
               boolean roleUpdated = this.updateRolePermissions(editor.getRolePermissionsMaps());
               if (roleUpdated) {
                  this.updateUserPermissions();
                  BPermissionsBrowser.this.repaint();
               }
            }
         } else {
            BDialog.warning(
               BPermissionsBrowser.this,
               this.getEditPermissionsTitle(col),
               BPermissionsBrowser.LEXICON.getText("permission.missingRoleService", BPermissionsBrowser.this.getCurrentContext())
            );
         }
      }

      private Category[] getCategories(BPermissionsMap permissionsMap) {
         return Category.load(BPermissionsBrowser.this.getWbShell(), permissionsMap.size()).categories;
      }

      private String getEditPermissionsTitle(int col) {
         return BPermissionsBrowser.LEXICON
            .getText("permission.generic", BPermissionsBrowser.this.getCurrentContext(), new Object[]{BPermissionsBrowser.this.getFilteredColumnName(col)});
      }

      private BPermissionsMap getMaxPermissions() {
         BPermissionsMap max = BPermissionsMap.SUPER_USER;
         Context cx = BPermissionsBrowser.this.getCurrentContext();
         BString username = (BString)cx.getFacet("username");
         if (username != null) {
            BUser user = this.getUserService().getUser(username.toString());
            if (user != null) {
               max = user.getPermissions();
            }
         }

         return max;
      }

      private boolean updateRolePermissions(Map<BRole, BPermissionsMap> newRolePermissions) {
         boolean roleUpdated = false;

         for (int i = 0; i < BPermissionsBrowser.this.roles.length; i++) {
            BPermissionsMap newPermissions = newRolePermissions.get(BPermissionsBrowser.this.roles[i]);
            if (newPermissions != null && !BPermissionsBrowser.this.rolePermissions[i].equals(newPermissions)) {
               BPermissionsBrowser.this.rolePermissions[i] = newPermissions;
               BPermissionsBrowser.this.dirty[i] = true;
               roleUpdated = true;
               BPermissionsBrowser.this.setModified();
            }
         }

         return roleUpdated;
      }

      private void updateUserPermissions() {
         for (int i = 0; i < BPermissionsBrowser.this.users.length; i++) {
            Set<String> userRoleNames = BPermissionsBrowser.this.users[i].getRoleIds();
            if (!userRoleNames.contains(SlotPath.unescape(BPermissionsBrowser.this.adminRoleName))) {
               BPermissionsMap newUserPermissions = BPermissionsMap.DEFAULT;

               for (int j = 0; j < BPermissionsBrowser.this.roles.length; j++) {
                  if (userRoleNames.contains(SlotPath.unescape(BPermissionsBrowser.this.roles[j].getName())) && BPermissionsBrowser.this.roles[j].getEnabled()) {
                     if (BPermissionsBrowser.this.rolePermissions[j].isSuperUser()) {
                        newUserPermissions = BPermissionsMap.SUPER_USER;
                        break;
                     }

                     newUserPermissions = newUserPermissions.or(BPermissionsBrowser.this.rolePermissions[j]);
                  }
               }

               BPermissionsBrowser.this.userPermissions[i] = newUserPermissions;
            }
         }
      }

      protected BMenu makeOptionsMenu() {
         BMenu menu = super.makeOptionsMenu();
         menu.add(null, new BSeparator());
         menu.add(null, BPermissionsBrowser.this.new ShowAllColumns(true));
         menu.add(null, BPermissionsBrowser.this.new ShowAllColumns(false));
         menu.add(null, BPermissionsBrowser.this.new ConfigureColumns());
         return menu;
      }

      private BUserService getUserService() {
         return (BUserService)BOrd.make("service:baja:UserService")
            .get(BPermissionsBrowser.this.getCurrentValue(), BPermissionsBrowser.this.getCurrentContext());
      }
   }

   private class HighlightAccessible extends ToggleCommand {
      public HighlightAccessible() {
         super(BPermissionsBrowser.this, BPermissionsBrowser.module, "permissions.highlightAccessible");
      }

      public CommandArtifact doInvoke() throws Exception {
         BPermissionsBrowser.this.enterBusy();

         try {
            BPermissionsBrowser.this.model.updateTable();
         } finally {
            BPermissionsBrowser.this.exitBusy();
         }

         return null;
      }
   }

   private class Model extends TreeTableModel implements NavListener {
      private Model() {
      }

      public int getRootCount() {
         return BPermissionsBrowser.this.roots.size();
      }

      public TreeTableNode getRoot(int index) {
         return (TreeTableNode)BPermissionsBrowser.this.roots.get(index);
      }

      public int getColumnCount() {
         return BPermissionsBrowser.this.visibleSize + 1;
      }

      public String getColumnName(int col) {
         if (col == 0) {
            return "";
         } else {
            try {
               return BPermissionsBrowser.this.getFilteredColumnName(col);
            } catch (Exception var3) {
               return null;
            }
         }
      }

      public void navEvent(NavEvent event) {
         if (event.getId() == 2) {
            BPermissionsBrowser.Node parent = this.eventToNode(event);
            if (parent != null && parent.nav instanceof BComponent) {
               BComponent component = (BComponent)parent.nav;
               if (component instanceof BVirtualGateway) {
                  try {
                     component = ((BVirtualGateway)component).getVirtualSpace().getRootComponent();
                  } catch (Exception var5) {
                  }
               }

               if (!((ComponentSlotMap)component.fw(1)).isBrokerPropsLoaded()) {
                  parent.performFullLoad = true;
               }
            }
         }
      }

      private BPermissionsBrowser.Node eventToNode(NavEvent event) {
         BOrd ord = event.getParentOrd();
         if (ord == null) {
            return null;
         } else {
            String relOrd = ord.relativizeToSession().toString();
            BPermissionsBrowser.Node node = null;

            for (int j = 0; j < BPermissionsBrowser.this.roots.size(); j++) {
               node = this.findNodeByNavOrd(relOrd, (BPermissionsBrowser.Node)BPermissionsBrowser.this.roots.get(j));
               if (node != null) {
                  break;
               }
            }

            return node;
         }
      }

      private BPermissionsBrowser.Node findNodeByNavOrd(String ord, BPermissionsBrowser.Node node) {
         if (ord.equals(node.navOrdRelStr)) {
            return node;
         } else {
            if (ord.startsWith(node.navOrdRelStr)) {
               BPermissionsBrowser.Node[] children = node.kids;
               if (children != null) {
                  for (BPermissionsBrowser.Node child : children) {
                     BPermissionsBrowser.Node result = this.findNodeByNavOrd(ord, child);
                     if (result != null) {
                        return result;
                     }
                  }
               }
            }

            return null;
         }
      }
   }

   private class Node extends TreeTableNode {
      private final BINavNode nav;
      private BOrd categorizableOrd;
      private String categorizableOrdRelStr;
      private String navOrdRelStr;
      private BPermissionsBrowser.Node[] kids;
      private BImage icon;
      private BCategoryMask mask;
      private final BCategoryMask deepOrMask;
      private final boolean isRoot;
      private boolean performFullLoad;
      private boolean buildChildren;

      public Node(BPermissionsBrowser.Model model, BINavNode nav, BCategoryMask deepOrMask) {
         super(model);
         this.nav = nav;
         this.isRoot = true;
         this.deepOrMask = deepOrMask;
         this.init();
         if (this.mask == null) {
            this.mask = BCategoryMask.make("1");
         }
      }

      public Node(BPermissionsBrowser.Node parent, BINavNode nav, BCategoryMask deepOrMask) {
         super(parent);
         this.nav = nav;
         this.isRoot = false;
         this.deepOrMask = deepOrMask;
         this.init();
      }

      private void init() {
         if (!(this.nav instanceof BIProtected)) {
            throw new IllegalStateException(this.nav.getClass().getName() + " is not BIProtected");
         } else {
            boolean virtual = this.nav instanceof BVirtualComponent;
            BOrd navOrd = this.nav.getNavOrd();
            BOrd navOrdRel = navOrd.relativizeToSession();
            this.categorizableOrd = virtual ? ((BVirtualComponent)this.nav).getCategorizableOrd().relativizeToSession() : navOrdRel;
            this.categorizableOrdRelStr = this.categorizableOrd.toString();
            this.navOrdRelStr = navOrdRel.toString();
            this.icon = BImage.make(this.nav.getNavIcon());
            if (this.nav instanceof BComponent && !virtual) {
               this.mask = ((BIProtected)this.nav).getCategoryMask();
            } else {
               this.mask = HistoryCategoryUtil.getOrdMapCategoryMask(this.categorizableOrd, BPermissionsBrowser.this.catService);
            }
         }
      }

      public Object getSubject() {
         return this.nav;
      }

      public Object getValueAt(int col) {
         return col == 0
            ? this.nav.getNavDisplayName(BPermissionsBrowser.this.getCurrentContext())
            : BPermissionsBrowser.this.getPermissionMap(col).getCategoryPermissions(this.applied());
      }

      public BImage getIcon() {
         return this.icon;
      }

      public boolean hasChildren() {
         return this.kids == null || this.kids.length > 0;
      }

      public int getChildCount() {
         this.build();
         return this.kids.length;
      }

      public TreeTableNode getChild(int index) {
         this.build();
         return this.kids[index];
      }

      private boolean inherited() {
         return this.mask == null || this.mask.isNull();
      }

      private BCategoryMask applied() {
         return this.inherited() ? ((BPermissionsBrowser.Node)this.getParent()).applied() : this.mask;
      }

      private void build() {
         if (this.buildChildren || this.kids == null) {
            this.buildChildren = false;
            BPermissionsBrowser.this.enterBusy();

            try {
               Array<BPermissionsBrowser.Node> a = new Array(BPermissionsBrowser.Node.class);
               BINavNode[] navChildren = this.nav.getNavChildren();
               List<BCategoryMask> deepOrMasks = BPermissionsBrowser.this.getDeepOrMasks(navChildren);

               for (int i = 0; i < navChildren.length; i++) {
                  if (navChildren[i] instanceof BIProtected) {
                     a.add(BPermissionsBrowser.this.new Node(this, navChildren[i], deepOrMasks.get(i)));
                  }
               }

               this.kids = (BPermissionsBrowser.Node[])a.trim();
            } finally {
               BPermissionsBrowser.this.exitBusy();
            }
         }
      }

      public void expanded() {
         if (this.performFullLoad) {
            this.performFullLoad = false;
            this.buildChildren = true;
         }

         NavMonitor.touch();
      }
   }

   private class Renderer extends TreeTableCellRenderer {
      private Renderer() {
      }

      public BBrush getForeground(Cell cell) {
         if (cell.column == 0) {
            return super.getForeground(cell);
         } else {
            return BPermissionsBrowser.this.getPermissionMap(cell.column).isSuperUser() ? Theme.widget().getControlShadow() : super.getForeground(cell);
         }
      }

      public BBrush getBackground(Cell cell) {
         BPermissionsBrowser.Node node = (BPermissionsBrowser.Node)BPermissionsBrowser.this.model.rowToNode(cell.row);
         if (BPermissionsBrowser.this.highlightAccessible.isSelected()) {
            if (this.isAccessibleByVisibleColumns(node)) {
               return BPermissionsBrowser.HIGHLIGHTED_BRUSH;
            }
         } else if (!node.inherited()) {
            return BPermissionsBrowser.CONFIGURED_BRUSH;
         }

         return super.getBackground(cell);
      }

      public BBrush getSelectionForeground(Cell cell) {
         return this.getForeground(cell);
      }

      public BBrush getSelectionBackground(Cell cell) {
         return this.getBackground(cell);
      }

      public void paintCell(Graphics g, Cell cell) {
         this.paintCellBackground(g, cell);
         BPermissionsBrowser.Node node = (BPermissionsBrowser.Node)BPermissionsBrowser.this.model.rowToNode(cell.row);
         if (cell.column == 0) {
            double x = this.paintExpander(g, cell, node);
            x = this.paintIcon(g, x, cell, node.inherited());
            this.paintCellText(g, x, this.getCellText(cell), node.inherited());
         } else {
            this.paintCellText(g, 2.0, this.getCellText(cell), node.inherited());
         }

         this.paintVerticalLine(g, cell);
      }

      private double paintIcon(Graphics g, double x, Cell cell, boolean isInherited) {
         BImage icon = BPermissionsBrowser.this.model.getRowIcon(cell.row);
         if (icon != null) {
            double y = (cell.height - 16.0) / 2.0;
            if (isInherited) {
               g.drawImage(icon.getDisabledImage(), x, y);
            } else {
               g.drawImage(icon, x, y);
            }

            x += 18.0;
         }

         return x;
      }

      private void paintCellText(Graphics g, double x, String text, boolean isInherited) {
         if (text != null && !text.isEmpty()) {
            BBrush origBrush = g.getBrush();
            if (isInherited) {
               g.setBrush(BPermissionsBrowser.DISABLED_BRUSH);
            }

            BFont font = Theme.table().getCellFont();
            g.setFont(font);
            g.drawString(text, x, font.getAscent() + 2.0);
            g.setBrush(origBrush);
         }
      }

      private double paintExpander(Graphics g, Cell cell, BPermissionsBrowser.Node node) {
         TreeTableTheme theme = Theme.treeTable();
         int depth = node.getDepth();
         double x = theme.getIndent(depth);
         if (BPermissionsBrowser.this.model.isDepthExpandable(depth)) {
            if (node.hasChildren()) {
               int state = node.isExpanded() ? 2 : 1;
               double y = (cell.height - theme.getExpanderHeight()) / 2.0;
               this.paintExpander(g, x, y, theme, state);
            }

            x += theme.getExpanderWidth() + 5.0;
         }

         return x;
      }

      private void paintExpander(Graphics g, double x, double y, TreeTableTheme theme, int state) {
         BBrush origBrush = g.getBrush();
         g.setBrush(BPermissionsBrowser.CONNECTING_BRUSH);
         double ew = theme.getExpanderWidth();
         double eh = theme.getExpanderHeight();
         g.strokeLine(x, y, x + ew - 1.0, y);
         g.strokeLine(x, y, x, y + ew - 1.0);
         g.strokeLine(x + ew - 1.0, y, x + ew - 1.0, y + eh - 1.0);
         g.strokeLine(x, y + eh - 1.0, x + ew - 1.0, y + eh - 1.0);
         g.setBrush(BPermissionsBrowser.EXPANDER_BRUSH);
         if (state != 0) {
            g.strokeLine(x + 2.0, y + 4.0, x + 6.0, y + 4.0);
         }

         if (state == 1) {
            g.strokeLine(x + 4.0, y + 2.0, x + 4.0, y + 6.0);
         }

         g.setBrush(origBrush);
      }

      private boolean isAccessibleByVisibleColumns(BPermissionsBrowser.Node node) {
         for (int i = 1; i <= BPermissionsBrowser.this.visibleSize; i++) {
            BPermissionsMap pMap = BPermissionsBrowser.this.getPermissionMap(i);
            if (this.isMaskAccessible(pMap, node.deepOrMask) || this.isMaskAccessible(pMap, node.applied())) {
               return true;
            }
         }

         return false;
      }

      private boolean isMaskAccessible(BPermissionsMap pMap, BCategoryMask mask) {
         return mask != null && !mask.isNull() && pMap.getCategoryPermissions(mask).hasOperatorRead();
      }
   }

   private class ShowAllColumns extends Command {
      private final boolean show;

      public ShowAllColumns(boolean show) {
         super(
            BPermissionsBrowser.this,
            show
               ? BPermissionsBrowser.LEXICON.getText("permission.showAll", BPermissionsBrowser.this.getCurrentContext())
               : BPermissionsBrowser.LEXICON.getText("permission.hideAll", BPermissionsBrowser.this.getCurrentContext())
         );
         this.show = show;
      }

      public CommandArtifact doInvoke() {
         for (int i = 0; i < BPermissionsBrowser.this.visible.length; i++) {
            BPermissionsBrowser.this.visible[i] = this.show;
         }

         BPermissionsBrowser.this.updateVisible();
         BPermissionsBrowser.this.table.relayout();
         return null;
      }
   }

   private class ShowConfigured extends ToggleCommand {
      private boolean show;

      public ShowConfigured() {
         super(BPermissionsBrowser.this, BPermissionsBrowser.module, "permissions.showConfigured");
      }

      public void setSelected(boolean sel) {
         super.setSelected(sel);
         this.show = sel;
         if (sel) {
            BPermissionsBrowser.this.addRoots();
         }
      }

      public CommandArtifact doInvoke() throws Exception {
         BPermissionsBrowser.this.enterBusy();

         try {
            BOrd[] ords = this.findConfiguredMasks();
            new BatchResolve(ords).resolve(this.getActiveService());

            for (BOrd ord : ords) {
               String ordString = ord.toString();

               for (int j = 0; j < BPermissionsBrowser.this.roots.size(); j++) {
                  this.checkNode(ordString, (BPermissionsBrowser.Node)BPermissionsBrowser.this.roots.get(j));
               }
            }

            BOrdToCategoryMap ordMap = BPermissionsBrowser.this.catService.getOrdMap();
            int len = ordMap.size();

            for (int i = 0; i < len; i++) {
               String ord = ordMap.getOrd(i).relativizeToSession().toString();

               for (int j = 0; j < BPermissionsBrowser.this.roots.size(); j++) {
                  this.checkNode(ord, (BPermissionsBrowser.Node)BPermissionsBrowser.this.roots.get(j));
               }
            }
         } finally {
            BPermissionsBrowser.this.exitBusy();
         }

         return null;
      }

      private BOrd[] findConfiguredMasks() {
         BOrd query = BOrd.make("station:|slot:/|bql:select navOrd from baja:Component where not categoryMask.isNull");
         BITable<BIObject> table = (BITable<BIObject>)query.get(this.getActiveService());
         Column col = table.getColumns().get(0);
         TableCursor<BIObject> cursor = table.cursor();
         Array<BOrd> arr = new Array(BOrd.class);

         while (cursor.next()) {
            arr.add(BOrd.make(cursor.cell(col).toString(Context.NULL)).relativizeToSession());
         }

         return (BOrd[])arr.trim();
      }

      private BComponent getActiveService() {
         return (BComponent)(BPermissionsBrowser.this.roleService != null ? BPermissionsBrowser.this.roleService : BPermissionsBrowser.this.userService);
      }

      private void checkNode(String ord, BPermissionsBrowser.Node node) {
         if (ord.equals(node.categorizableOrdRelStr)) {
            if (!node.isRoot) {
               this.expandToRoot((BPermissionsBrowser.Node)node.getParent());
            }
         } else if (ord.startsWith(node.categorizableOrdRelStr)) {
            for (int i = 0; i < node.getChildCount(); i++) {
               this.checkNode(ord, (BPermissionsBrowser.Node)node.getChild(i));
            }
         }
      }

      private void expandToRoot(BPermissionsBrowser.Node node) {
         node.setExpanded(this.show);
         if (!node.isRoot) {
            this.expandToRoot((BPermissionsBrowser.Node)node.getParent());
         }
      }
   }
}
