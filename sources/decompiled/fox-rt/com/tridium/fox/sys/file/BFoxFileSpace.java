package com.tridium.fox.sys.file;

import com.tridium.fox.sys.BFoxSession;
import com.tridium.fox.sys.BIFoxProxySpace;
import com.tridium.sys.transfer.RemoteTransferSpace;
import com.tridium.sys.transfer.TransferResult;
import com.tridium.sys.transfer.TransferStrategy;
import java.io.IOException;
import java.util.ArrayList;
import javax.baja.file.BDirectory;
import javax.baja.file.BFileSpace;
import javax.baja.file.BIDirectory;
import javax.baja.file.BIFile;
import javax.baja.file.BIFileStore;
import javax.baja.file.FilePath;
import javax.baja.naming.BOrd;
import javax.baja.nav.BINavNode;
import javax.baja.nre.annotations.AgentOn;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.sys.BIcon;
import javax.baja.sys.Context;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;
import javax.baja.util.LexiconText;

@NiagaraType(
   agent = {@AgentOn(
      types = {"fox:FoxSession"}
   )}
)
public class BFoxFileSpace extends BFileSpace implements BIDirectory, BIFoxProxySpace, RemoteTransferSpace {
   public static final Type TYPE = Sys.loadType(BFoxFileSpace.class);
   private final BOrd ordInSession = BOrd.make("file:");
   private BIFile[] roots;
   private BIFile stationHome;
   private BFileChannel channel;
   private String[] targetStationRoute;

   public Type getType() {
      return TYPE;
   }

   public BFoxFileSpace() {
      super("file", LexiconText.make("fox", "nav.fileSystem"));
   }

   protected BFoxFileSpace(String name, BFileChannel channel) {
      super(name);
      this.channel = channel;
   }

   public BFoxFileSpace(BFileChannel channel) {
      this();
      this.channel = channel;
   }

   public BFoxFileSpace(BFileChannel channel, String... targetStationRoute) {
      this();
      this.channel = channel;
      this.targetStationRoute = targetStationRoute;
   }

   @Override
   public void init(BFoxSession foxSession) {
   }

   @Override
   public void cleanup(BFoxSession foxSession) {
   }

   public BDirectory makeDir(FilePath path, Context cx) throws IOException {
      try {
         BFoxFileStore store = this.channel().makeDir(this, path);
         return new BDirectory(store);
      } catch (Exception var4) {
         var4.printStackTrace();
         throw new FoxFileException(path.toString(), var4);
      }
   }

   public BIFile makeFile(FilePath path, Context cx) throws IOException {
      try {
         BFoxFileStore store = this.channel().makeFile(this, path);
         return this.makeFile(store);
      } catch (Exception var4) {
         var4.printStackTrace();
         throw new FoxFileException(path.toString(), var4);
      }
   }

   public void move(FilePath from, FilePath to, Context cx) throws IOException {
      try {
         this.channel().move(this, from, to);
      } catch (Exception var5) {
         var5.printStackTrace();
         throw new FoxFileException("" + from + "->" + to, var5);
      }
   }

   public void delete(FilePath path, Context cx) throws IOException {
      try {
         this.channel().delete(this, path);
      } catch (Exception var4) {
         var4.printStackTrace();
         throw new FoxFileException(path.toString(), var4);
      }
   }

   public BIFile[] listFiles() {
      if (this.roots == null) {
         BFoxFileStore sysStore = new BFoxFileStore(this, new FilePath("!"));
         BDirectory sysHome = new BDirectory(sysStore, LexiconText.make("baja", "nav.sysHome"));
         sysHome.setIcon(BIcon.std("home.png"));
         new BFoxFileStore(this, new FilePath("~"));
         BDirectory userHome = new BDirectory(sysStore, LexiconText.make("baja", "nav.userHome"));
         userHome.setIcon(BIcon.std("home.png"));
         BFoxFileStore stationStore = new BFoxFileStore(this, new FilePath("^"));
         BDirectory stationHome = new BDirectory(stationStore, LexiconText.make("baja", "nav.stationHome"));
         stationHome.setIcon(BIcon.std("database.png"));
         ArrayList<BIFile> v = new ArrayList<>();
         this.addRoot(v, "~", "nav.userHome", "home.png");
         this.addRoot(v, "!", "nav.sysHome", "home.png");
         this.addRoot(v, "^", "nav.stationHome", "database.png");
         return v.toArray(new BIFile[0]);
      } else {
         return this.roots;
      }
   }

   private void addRoot(ArrayList<BIFile> v, String filePathStr, String lexKey, String icon) {
      try {
         FilePath filePath = new FilePath(filePathStr);
         BFoxFileStore store = this.channel().head(this, filePath);
         BDirectory dir = new BDirectory(store, LexiconText.make("baja", lexKey));
         dir.setIcon(BIcon.std(icon));
         v.add(dir);
      } catch (Exception var8) {
         System.out.println("FoxFileSpace \"" + filePathStr + "\" unsupported");
      }
   }

   public BOrd getOrdInSession() {
      return this.ordInSession;
   }

   public BIFileStore findStore(FilePath path) {
      try {
         return this.channel().getConnection().isConnected() ? this.channel().head(this, path) : null;
      } catch (Exception var3) {
         var3.printStackTrace();
         throw new FoxFileException(path.toString(), var3);
      }
   }

   public BIFile getChild(BIFile dir, String name) {
      return this.findFile(dir.getFilePath().merge(name));
   }

   public BIFile[] getChildren(BIFile dir) {
      return this.list(dir.getFilePath());
   }

   protected BIFile[] list(FilePath path) {
      try {
         BFoxFileStore[] stores = this.channel().list(this, path);
         BIFile[] files = new BIFile[stores.length];

         for (int i = 0; i < files.length; i++) {
            files[i] = this.makeFile(stores[i]);
         }

         return files;
      } catch (Exception var5) {
         throw new FoxFileException(path.toString(), var5);
      }
   }

   public TransferResult transfer(TransferStrategy strategy) throws Exception {
      return this.channel().transfer(strategy, this);
   }

   public boolean hasNavChildren() {
      return true;
   }

   public BOrd getNavOrd() {
      BOrd navOrd = super.getNavOrd();
      return navOrd != null ? BOrd.make(navOrd.toString() + "^") : null;
   }

   public BINavNode getNavChild(String navName) {
      return this.getStationHome().getNavChild(navName);
   }

   public BINavNode[] getNavChildren() {
      return this.getStationHome().getNavChildren();
   }

   public BIFile getStationHome() {
      if (this.stationHome == null) {
         this.stationHome = this.findFile(new FilePath("^"));
      }

      return this.stationHome;
   }

   public BFoxSession getFoxSession() {
      if (this.channel != null) {
         return null;
      } else {
         for (BINavNode session = this.getNavParent(); session != null; session = session.getNavParent()) {
            if (session instanceof BFoxSession) {
               return (BFoxSession)session;
            }
         }

         return null;
      }
   }

   public BFileChannel channel() {
      return this.channel != null ? this.channel : (BFileChannel)this.getFoxSession().getConnection().getChannels().get("file");
   }

   String[] getTargetStationRoute() {
      return this.targetStationRoute;
   }
}
