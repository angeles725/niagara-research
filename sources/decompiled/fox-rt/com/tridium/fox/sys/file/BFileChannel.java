package com.tridium.fox.sys.file;

import com.tridium.fox.message.FoxMessage;
import com.tridium.fox.message.FoxTuple;
import com.tridium.fox.session.Fox;
import com.tridium.fox.session.FoxCircuit;
import com.tridium.fox.session.FoxRequest;
import com.tridium.fox.session.FoxResponse;
import com.tridium.fox.session.InvalidCommandException;
import com.tridium.fox.sys.BFoxChannel;
import com.tridium.fox.sys.BFoxClientConnection;
import com.tridium.fox.sys.UnreachableStationException;
import com.tridium.fox.sys.broker.TransferCodec;
import com.tridium.sys.transfer.TransferResult;
import com.tridium.sys.transfer.TransferStrategy;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.function.BiConsumer;
import javax.baja.file.BAbstractFileStore;
import javax.baja.file.BFileSpace;
import javax.baja.file.BFileSystem;
import javax.baja.file.BIDirectory;
import javax.baja.file.BIFile;
import javax.baja.file.BIFileStore;
import javax.baja.file.BScopedFileSpace;
import javax.baja.file.BajaFileUtil;
import javax.baja.file.FilePath;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.nre.util.Array;
import javax.baja.security.BPermissions;
import javax.baja.security.PermissionException;
import javax.baja.sys.BAbsTime;
import javax.baja.sys.Context;
import javax.baja.sys.LocalizableRuntimeException;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType
public class BFileChannel extends BFoxChannel {
   public static final Type TYPE = Sys.loadType(BFileChannel.class);
   private static final BiConsumer<FoxCircuit, FoxCircuit> FILE_CIRCUIT_WRITE_ROUTER = (sourceCircuit, destCircuit) -> {
      try {
         FoxMessage msg = sourceCircuit.readMessage();
         destCircuit.writeMessage(msg);
         destCircuit.flush();

         byte[] chunk;
         do {
            msg = sourceCircuit.readMessage();
            destCircuit.writeMessage(msg);
            destCircuit.flush();
            chunk = msg.getBlob("chunk");
         } while (chunk.length != 0);

         InputStream destInput = destCircuit.getInputStream();
         OutputStream sourceOutput = sourceCircuit.getOutputStream();
         byte[] byteBuffer = new byte[ROUTED_CIRCUIT_BUFFER_SIZE];

         int len;
         while ((len = destInput.read(byteBuffer, 0, ROUTED_CIRCUIT_BUFFER_SIZE)) >= 0) {
            sourceOutput.write(byteBuffer, 0, len);
         }
      } catch (Exception var7) {
         RuntimeException ex;
         if (var7 instanceof LocalizableRuntimeException && "fox.channel.unsupportedRemoteVersion".equals(((LocalizableRuntimeException)var7).getLexiconKey())) {
            ex = new LocalizableRuntimeException(
               "fox", "fox.channel.unsupportedRemoteVersionAlongRoute", ((LocalizableRuntimeException)var7).getLexiconArguments()
            );
         } else {
            ex = new UnreachableStationException(var7);
         }

         throw ex;
      }
   };
   public static int CHUNK_SIZE = 16384;
   private static final String NexusReq = "_nexusNode";
   private final Array<String> unrestrictedFilePaths = new Array(String.class);

   @Override
   public Type getType() {
      return TYPE;
   }

   public BFileChannel() {
      this("file");
   }

   public BFileChannel(String name) {
      super(name);
   }

   @Override
   public void checkProcess(FoxRequest req) throws Throwable {
   }

   @Override
   public void checkProcessCircuit(FoxCircuit circuit) throws Throwable {
   }

   @Override
   public FoxResponse process(FoxRequest request) throws Exception {
      String command = request.command;
      if (command == "head") {
         return this.head(request);
      } else if (command == "list") {
         return this.list(request);
      } else if (command == "delete") {
         return this.delete(request);
      } else if (command == "makeFile") {
         return this.makeFile(request);
      } else if (command == "makeDir") {
         return this.makeDir(request);
      } else if (command == "move") {
         return this.move(request);
      } else if (command == "setLastModified") {
         return this.setLastModified(request);
      } else if (command == "getCrc") {
         return this.getCrc(request);
      } else {
         throw new InvalidCommandException(command);
      }
   }

   @Override
   public void circuitOpened(FoxCircuit circuit) throws Exception {
      String command = circuit.command;
      if (command == "read") {
         this.read(circuit);
      } else if (command == "write") {
         this.write(circuit);
      } else if (command == "transfer") {
         this.transfer(circuit);
      } else {
         throw new InvalidCommandException(command);
      }
   }

   @Override
   protected boolean allowRoutingRequestToReachableStation(FoxRequest req) {
      return true;
   }

   @Override
   protected boolean allowRoutingCircuitToReachableStation(FoxCircuit circuit) {
      return true;
   }

   @Override
   protected BiConsumer<FoxCircuit, FoxCircuit> getCircuitRouter(String sourceCircuitCommand, BFoxChannel sourceChannel) {
      return "write".equals(sourceCircuitCommand) ? FILE_CIRCUIT_WRITE_ROUTER : super.getCircuitRouter(sourceCircuitCommand, sourceChannel);
   }

   @Override
   public void sessionClosed(Throwable cause) throws Exception {
      synchronized (this.unrestrictedFilePaths) {
         this.unrestrictedFilePaths.clear();
      }
   }

   protected FoxRequest makeRequest(String command, BFoxFileSpace space) {
      return space == null
         ? this.makeRequest(command)
         : (FoxRequest)BFoxChannel.makeFoxMessageWithReachableStationRoute(this, this.makeRequest(command), null, space.getTargetStationRoute());
   }

   protected BFileSpace getFileSpaceFor(FoxMessage request, FilePath path) {
      return this.getFileSpaceForPath(path);
   }

   public BFoxFileStore head(BFoxFileSpace space, FilePath path) throws Exception {
      FoxRequest req = this.makeRequest("head", space);
      req.add("path", path.getBody());
      if (this.isTraceOn()) {
         this.trace("c:head \"" + path.getBody() + "\"");
      }

      FoxResponse resp = this.sendSync(req);
      return resp == null ? null : msgToStore(space, path, resp);
   }

   public FoxResponse head(FoxRequest req) throws Exception {
      FilePath path = new FilePath(req.getString("path"));
      if (this.isTraceOn()) {
         this.trace("s:head \"" + path.getBody() + "\"");
      }

      BIFile file = this.getFileSpaceFor(req, path).findFile(path);
      if (file == null) {
         return null;
      } else {
         BPermissions permissions = this.getPermissionsFor(file);
         if (!permissions.has(BPermissions.operatorRead)) {
            throw new PermissionException();
         } else {
            FoxResponse resp = new FoxResponse(req);
            storeToMsg(file.getStore(), permissions, resp);
            return resp;
         }
      }
   }

   public BFoxFileStore[] list(BFoxFileSpace space, FilePath path) throws Exception {
      if (this.isTraceOn()) {
         this.trace("c:list \"" + path.getBody() + "\"");
      }

      FoxRequest req = this.makeRequest("list", space);
      req.add("path", path.getBody());
      FoxResponse resp = this.sendSync(req);
      FoxTuple[] msgs = resp.list("file");
      BFoxFileStore[] stores = new BFoxFileStore[msgs.length];

      for (int i = 0; i < stores.length; i++) {
         FoxMessage msg = (FoxMessage)msgs[i];
         String name = msg.getString("name");
         stores[i] = msgToStore(space, path.merge(name), msg);
      }

      return stores;
   }

   public FoxResponse list(FoxRequest req) throws Exception {
      FilePath path = new FilePath(req.getString("path"));
      if (this.isTraceOn()) {
         this.trace("s:list \"" + path + "\"");
      }

      FoxResponse resp = new FoxResponse(req);
      BIFile file = this.getFileSpaceFor(req, path).resolveFile(path);
      if (!this.getPermissionsFor(file).has(BPermissions.operatorRead)) {
         throw new PermissionException();
      } else {
         if (file instanceof BIDirectory) {
            BIDirectory dir = (BIDirectory)file;
            BIFile[] files = dir.listFiles();

            for (int i = 0; i < files.length; i++) {
               BPermissions permissions = this.getPermissionsFor(files[i]);
               if (permissions.has(BPermissions.operatorRead)) {
                  FoxMessage msg = new FoxMessage("file");
                  storeToMsg(files[i].getStore(), permissions, msg);
                  resp.add(msg);
               }
            }
         }

         return resp;
      }
   }

   public void move(BFoxFileSpace space, FilePath from, FilePath to) throws Exception {
      FoxRequest req = this.makeRequest("move", space);
      req.add("from", from.getBody());
      req.add("to", to.getBody());
      if (this.isTraceOn()) {
         this.trace("c:move \"" + from + "\" -> \"" + to + "\"");
      }

      this.sendSync(req);
   }

   public FoxResponse move(FoxRequest req) throws Exception {
      FilePath from = new FilePath(req.getString("from"));
      FilePath to = new FilePath(req.getString("to"));
      if (this.isTraceOn()) {
         this.trace("c:move \"" + from + "\" -> \"" + to + "\"");
      }

      this.getFileSpaceFor(req, from).move(from, to, this.getSessionContext());
      return null;
   }

   public void delete(BFoxFileSpace space, FilePath path) throws Exception {
      FoxRequest req = this.makeRequest("delete", space);
      req.add("path", path.getBody());
      if (this.isTraceOn()) {
         this.trace("c:delete \"" + path.getBody() + "\"");
      }

      this.sendSync(req);
   }

   public FoxResponse delete(FoxRequest req) throws Exception {
      FilePath path = new FilePath(req.getString("path"));
      if (this.isTraceOn()) {
         this.trace("s:delete \"" + path.getBody() + "\"");
      }

      this.getFileSpaceFor(req, path).delete(path, this.getSessionContext());
      return null;
   }

   public BFoxFileStore makeFile(BFoxFileSpace space, FilePath path) throws Exception {
      FoxRequest req = this.makeRequest("makeFile", space);
      req.add("path", path.getBody());
      if (this.isTraceOn()) {
         this.trace("c:makeFile \"" + path.getBody() + "\"");
      }

      FoxResponse resp = this.sendSync(req);
      return msgToStore(space, path, resp);
   }

   public FoxResponse makeFile(FoxRequest req) throws Exception {
      FilePath path = new FilePath(req.getString("path"));
      if (this.isTraceOn()) {
         this.trace("s:makeFile \"" + path.getBody() + "\"");
      }

      Context cx;
      try {
         cx = this.getSessionContext();
      } catch (Exception var7) {
         cx = null;
      }

      BIFile file = this.getFileSpaceFor(req, path).makeFile(path, cx);
      BPermissions permissions;
      if (cx != null) {
         permissions = this.getPermissionsFor(file);
      } else {
         permissions = BPermissions.all;
      }

      FoxResponse resp = new FoxResponse(req);
      storeToMsg(file.getStore(), permissions, resp);
      return resp;
   }

   public BFoxFileStore makeDir(BFoxFileSpace space, FilePath path) throws Exception {
      FoxRequest req = this.makeRequest("makeDir", space);
      req.add("path", path.getBody());
      if (this.isTraceOn()) {
         this.trace("c:makeDir \"" + path.getBody() + "\"");
      }

      FoxResponse resp = this.sendSync(req);
      return msgToStore(space, path, resp);
   }

   public FoxResponse makeDir(FoxRequest req) throws Exception {
      FilePath path = new FilePath(req.getString("path"));
      if (this.isTraceOn()) {
         this.trace("s:makeDir \"" + path.getBody() + "\"");
      }

      BIFile file = this.getFileSpaceFor(req, path).makeDir(path, this.getSessionContext());
      BPermissions permissions = this.getPermissionsFor(file);
      FoxResponse resp = new FoxResponse(req);
      storeToMsg(file.getStore(), permissions, resp);
      return resp;
   }

   public TransferResult transfer(TransferStrategy t) throws Exception {
      return this.transfer(t, null);
   }

   public TransferResult transfer(TransferStrategy t, BFoxFileSpace space) throws Exception {
      if (this.isTraceOn()) {
         this.trace("c:transfer");
         t.dump();
      }

      FoxRequest req = this.makeRequest("transfer");
      TransferCodec.transferToMessage(req, t);
      FoxMessage metadata = space != null ? makeFoxMessageWithReachableStationRoute(this, null, null, space.getTargetStationRoute()) : null;
      FoxCircuit circuit = this.openCircuit("transfer", metadata);
      circuit.writeMessage(req);

      while (true) {
         FoxMessage resp = null;
         resp = circuit.readMessage();
         String status = resp.getString("s", null);
         if (status != null) {
            t.updateStatus(status);
         } else {
            if (resp.getString("exception", null) != null) {
               throw Fox.exceptionTranslator.messageToException(resp);
            }

            if (resp.getBoolean("done", false)) {
               return null;
            }
         }
      }
   }

   void transfer(FoxCircuit circuit) throws Exception {
      try {
         Context cx = this.getSessionContext();
         FoxMessage req = circuit.readMessage();
         TransferStrategy t = TransferCodec.messageToTransfer(req, cx);
         if (this.isTraceOn()) {
            this.trace("s:transfer");
            t.dump();
         }

         t.setListener(new BFoxChannel.TransferStatusPipe(circuit));
         t.transfer();
         FoxMessage done = new FoxMessage();
         done.add("done", true);
         circuit.writeMessage(done);
      } catch (Exception var6) {
         var6.printStackTrace();
         circuit.writeMessage(Fox.exceptionTranslator.exceptionToMessage(var6));
      }

      circuit.close();
   }

   public InputStream read(BFoxFileStore store) throws Exception {
      BFoxFileSpace space = (BFoxFileSpace)store.getFileSpace();
      FilePath path = store.getFilePath();
      if (this.isTraceOn()) {
         this.trace("c:read \"" + path.getBody() + "\"");
      }

      FoxCircuit circuit = this.openCircuit("read", this.makeRequest("meta", space));
      FoxMessage msgout = new FoxMessage();
      msgout.add("path", store.getFilePath().getBody());
      circuit.writeMessage(msgout);
      circuit.flush();
      FoxMessage msgin = circuit.readMessage();
      long size = Long.parseLong(msgin.getString("size", "-1"));
      if (size > -1L && size != store.size) {
         store.size = size;
      }

      return circuit.getInputStream();
   }

   public void read(FoxCircuit circuit) throws Exception {
      FoxMessage inmsg = circuit.readMessage();
      FilePath path = new FilePath(inmsg.getString("path"));
      if (this.isTraceOn()) {
         this.trace("s:read \"" + path.getBody() + "\"");
      }

      BIFile file = this.getFileSpaceFor(circuit.metadata, path).resolveFile(path);
      if (!this.getPermissionsFor(file).has(BPermissions.operatorRead)) {
         throw new PermissionException();
      } else {
         long size = file.getSize();
         FoxMessage outmsg = new FoxMessage();
         outmsg.add("size", String.valueOf(size));
         circuit.writeMessage(outmsg);
         InputStream in = file.getInputStream();
         OutputStream out = circuit.getOutputStream();

         try {
            BajaFileUtil.pipe(in, size, out);
         } finally {
            in.close();
            circuit.close();
         }
      }
   }

   public OutputStream write(BFoxFileStore store) throws Exception {
      BFoxFileSpace space = (BFoxFileSpace)store.getFileSpace();
      FilePath path = store.getFilePath();
      if (this.isTraceOn()) {
         this.trace("c:write \"" + path.getBody() + "\"");
      }

      FoxCircuit circuit = this.openCircuit("write", this.makeRequest("meta", space));
      FoxMessage msgout = new FoxMessage();
      msgout.add("path", store.getFilePath().getBody());
      circuit.writeMessage(msgout);
      circuit.flush();
      return new BFileChannel.ChunkedOutputStream(this, store, circuit);
   }

   public void write(FoxCircuit circuit) throws Exception {
      FoxMessage inmsg = circuit.readMessage();
      FilePath path = new FilePath(inmsg.getString("path"));
      if (this.isTraceOn()) {
         this.trace("s:write \"" + path.getBody() + "\"");
      }

      BIFile file = this.getFileSpaceFor(circuit.metadata, path).resolveFile(path);
      FoxMessage outmsg = new FoxMessage();
      if (!this.getPermissionsFor(file, false).has(BPermissions.operatorWrite)) {
         outmsg.add("error", "No Permission");
         circuit.writeMessage(outmsg);
      } else {
         OutputStream out = file.getOutputStream();
         boolean streamOpen = true;

         try {
            while (true) {
               FoxMessage msg = circuit.readMessage();
               byte[] chunk = msg.getBlob("chunk");
               if (chunk.length == 0) {
                  streamOpen = false;
                  out.close();
                  outmsg.add("size", String.valueOf(file.getSize()));
                  outmsg.add("modified", file.getLastModified().getMillis());
                  circuit.writeMessage(outmsg);
                  return;
               }

               out.write(chunk);
            }
         } finally {
            if (streamOpen) {
               out.close();
            }

            circuit.close();
         }
      }
   }

   public boolean setLastModified(BFoxFileStore store, BAbsTime absTime) throws Exception {
      BFoxFileSpace space = (BFoxFileSpace)store.getFileSpace();
      FilePath path = store.getFilePath();
      FoxRequest req = this.makeRequest("setLastModified", space);
      req.add("path", path.getBody());
      req.add("lastModified", absTime.getMillis());
      if (this.isTraceOn()) {
         StringBuilder buff = new StringBuilder();
         buff.append("c:setLastModified \"");
         buff.append(path.getBody());
         buff.append("\" (");
         buff.append(absTime);
         buff.append(")");
         this.trace(buff.toString());
      }

      FoxResponse resp = this.sendSync(req);
      return resp.getBoolean("done");
   }

   public FoxResponse setLastModified(FoxRequest req) throws IOException {
      FilePath path = new FilePath(req.getString("path"));
      BAbsTime absTime = BAbsTime.make(req.getTime("lastModified"));
      if (this.isTraceOn()) {
         StringBuilder buff = new StringBuilder();
         buff.append("s:setLastModified \"");
         buff.append(path.getBody());
         buff.append("\" (");
         buff.append(absTime);
         buff.append(")");
         this.trace(buff.toString());
      }

      BIFile file = this.getFileSpaceFor(req, path).resolveFile(path);
      if (file.getStore().isReadonly()) {
         throw new IOException("File Store is readonly");
      } else if (!this.getPermissionsFor(file).hasOperatorWrite()) {
         throw new PermissionException();
      } else {
         boolean done = false;
         if (file.getStore() instanceof BAbstractFileStore) {
            BAbstractFileStore store = (BAbstractFileStore)file.getStore();
            done = store.setLastModified(absTime);
            FoxResponse resp = new FoxResponse(req);
            resp.add("done", done);
            return resp;
         } else {
            throw new IOException("Unsupported File Store");
         }
      }
   }

   public long getCrc(BFoxFileSpace space, FilePath path) throws Exception {
      FoxRequest req = this.makeRequest("getCrc", space);
      req.add("path", path.getBody());
      if (this.isTraceOn()) {
         StringBuilder buff = new StringBuilder();
         buff.append("c:getCrc \"");
         buff.append(path.getBody());
         buff.append("\"");
         this.trace(buff.toString());
      }

      FoxResponse resp = this.sendSync(req);
      return resp.getTime("crc");
   }

   public FoxResponse getCrc(FoxRequest req) throws IOException {
      FilePath path = new FilePath(req.getString("path"));
      if (this.isTraceOn()) {
         StringBuilder buff = new StringBuilder();
         buff.append("s:getCrc \"");
         buff.append(path.getBody());
         buff.append("\"");
         this.trace(buff.toString());
      }

      BIFile file = this.getFileSpaceFor(req, path).resolveFile(path);
      if (!this.getPermissionsFor(file).hasOperatorRead()) {
         throw new PermissionException();
      } else {
         long crc = -1L;
         if (file.getStore() instanceof BAbstractFileStore) {
            BAbstractFileStore store = (BAbstractFileStore)file.getStore();
            crc = store.getCrc();
            FoxResponse resp = new FoxResponse(req);
            resp.add("crc", crc);
            return resp;
         } else {
            throw new IOException("Unsupported File Store");
         }
      }
   }

   public static BFoxFileStore msgToStore(BFoxFileSpace space, FilePath path, FoxMessage msg) throws IOException {
      BFoxFileStore store = new BFoxFileStore(space, path);
      store.isDirectory = msg.getBoolean("dir");
      store.isReadonly = msg.getBoolean("readonly");
      store.modified = BAbsTime.make(msg.getTime("modified"));
      store.size = Long.parseLong(msg.getString("size"));
      store.permissions = BPermissions.make(msg.getString("permissions", "rw"));
      return store;
   }

   public static void storeToMsg(BIFileStore store, BPermissions permissions, FoxMessage msg) {
      msg.add("name", store.getFileName());
      msg.add("dir", store.isDirectory());
      msg.add("readonly", store.isReadonly() || !permissions.has(BPermissions.operatorWrite));
      msg.add("modified", store.getLastModified().getMillis());
      msg.add("size", String.valueOf(store.getSize()));
      msg.add("permissions", permissions.encodeToString());
   }

   public final Object fw(int x, Object a, Object b, Object c, Object d) {
      if (x == 901) {
         String unrestrictedFilePath = null;
         FilePath fp = null;
         if (a instanceof String) {
            unrestrictedFilePath = (String)a;
            new FilePath(unrestrictedFilePath);
         } else if (a instanceof FilePath) {
            fp = (FilePath)a;
            unrestrictedFilePath = fp.getBody();
         }

         if (unrestrictedFilePath != null) {
            synchronized (this.unrestrictedFilePaths) {
               if (!this.unrestrictedFilePaths.contains(unrestrictedFilePath) && this.getConnection().isConnected()) {
                  this.unrestrictedFilePaths.add(unrestrictedFilePath);
               }
            }
         }
      } else if (x == 902) {
         String unrestrictedFilePathx = null;
         if (a instanceof String) {
            unrestrictedFilePathx = (String)a;
         } else if (a instanceof FilePath) {
            unrestrictedFilePathx = ((FilePath)a).getBody();
         }

         if (unrestrictedFilePathx != null) {
            synchronized (this.unrestrictedFilePaths) {
               this.unrestrictedFilePaths.remove(unrestrictedFilePathx);
            }
         }
      }

      return super.fw(x, a, b, c, d);
   }

   protected BFileSpace getFileSpaceForPath(FilePath path) {
      synchronized (this.unrestrictedFilePaths) {
         if (this.unrestrictedFilePaths.contains(path.getBody())) {
            return BFileSystem.INSTANCE;
         } else if (this.getConnection() instanceof BFoxClientConnection) {
            throw new PermissionException("Illegal file operation (server cannot make file request to client)");
         } else {
            return BScopedFileSpace.STATION_HOME;
         }
      }
   }

   static class ChunkedOutputStream extends OutputStream {
      BFileChannel channel;
      BFoxFileStore store;
      FoxCircuit circuit;
      byte[] buf = new byte[Fox.circuitChunkSize - 256];
      int n;

      ChunkedOutputStream(BFileChannel channel, BFoxFileStore store, FoxCircuit circuit) {
         this.channel = channel;
         this.store = store;
         this.circuit = circuit;
      }

      @Override
      public void write(int b) throws IOException {
         this.buf[this.n++] = (byte)(0xFF & b);
         if (this.n >= this.buf.length) {
            this.flush();
         }
      }

      @Override
      public void flush() throws IOException {
         if (this.n > 0) {
            FoxMessage msg = new FoxMessage();
            msg.add("chunk", this.buf, this.n);
            this.writeMessage(msg);
            this.n = 0;
         }
      }

      @Override
      public void close() throws IOException {
         if (this.circuit.isOpen()) {
            this.flush();
            FoxMessage msg = new FoxMessage();
            msg.add("chunk", this.buf, 0);
            this.writeMessage(msg);
            FoxMessage reply = this.readMessage();
            this.circuit.close();
            String err = reply.getString("error", null);
            if (err != null) {
               throw new IOException(err);
            } else {
               String size = reply.getString("size", null);
               if (size != null) {
                  this.store.size = Long.parseLong(size);
                  this.store.modified = BAbsTime.make(reply.getTime("modified"));
               } else {
                  try {
                     FoxRequest req = this.channel.makeRequest("head", (BFoxFileSpace)this.store.getFileSpace());
                     req.add("path", this.store.getFilePath().getBody());
                     if (this.channel.isTraceOn()) {
                        this.channel.trace("c:head \"" + this.store.getFilePath().getBody() + "\"");
                     }

                     FoxResponse resp = this.channel.sendSync(req);
                     if (resp != null) {
                        size = resp.getString("size", null);
                        if (size != null) {
                           this.store.size = Long.parseLong(size);
                           this.store.modified = BAbsTime.make(resp.getTime("modified"));
                        }
                     }
                  } catch (Exception var7) {
                     if (this.channel.isTraceOn()) {
                        this.channel.trace("Error retrieving file info for \"" + this.store.getFilePath().getBody() + "\" " + var7.getMessage());
                     }
                  }
               }
            }
         }
      }

      private FoxMessage readMessage() throws IOException {
         try {
            return this.circuit.readMessage();
         } catch (IOException var2) {
            throw var2;
         } catch (Exception var3) {
            throw new IOException(var3.toString());
         }
      }

      private void writeMessage(FoxMessage msg) throws IOException {
         try {
            this.circuit.writeMessage(msg);
         } catch (IOException var3) {
            throw var3;
         } catch (Exception var4) {
            throw new IOException(var4.toString());
         }
      }
   }
}
