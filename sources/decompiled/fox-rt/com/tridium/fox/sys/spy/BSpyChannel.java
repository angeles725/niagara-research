package com.tridium.fox.sys.spy;

import com.tridium.fox.session.FoxRequest;
import com.tridium.fox.session.FoxResponse;
import com.tridium.fox.session.InvalidCommandException;
import com.tridium.fox.sys.BFoxChannel;
import java.io.ByteArrayOutputStream;
import javax.baja.file.FilePath;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.security.PermissionException;
import javax.baja.spy.BSpy;
import javax.baja.spy.BSpySpace;
import javax.baja.spy.Spy;
import javax.baja.spy.SpyWriter;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType
public class BSpyChannel extends BFoxChannel {
   public static final Type TYPE = Sys.loadType(BSpyChannel.class);

   @Override
   public Type getType() {
      return TYPE;
   }

   public BSpyChannel() {
      super("spy");
   }

   @Override
   public FoxResponse process(FoxRequest request) throws Exception {
      String command = request.command;
      if (command == "get") {
         return this.get(request);
      } else {
         throw new InvalidCommandException(command);
      }
   }

   public BSpy get(FilePath path) throws Exception {
      FoxRequest req = this.makeRequest("get");
      req.add("path", path.getBody());
      if (this.isTraceOn()) {
         this.trace("c:get \"" + path.getBody() + "\"");
      }

      FoxResponse resp = this.sendSync(req);
      String title = resp.getString("title");
      byte[] content = resp.getBlob("content");
      return BSpy.make(path, new FoxSpy(title, new String(content)));
   }

   public FoxResponse get(FoxRequest req) throws Exception {
      FilePath path = new FilePath(req.getString("path"));
      if (this.isTraceOn()) {
         this.trace("s:get \"" + path.getBody() + "\"");
      }

      BSpy bspy = BSpySpace.INSTANCE.resolveSpy(path);
      if (!this.getSessionContext().getUser().getPermissions().isSuperUser()) {
         throw new PermissionException("You need to have Super User Permissions to view a SPY Page.");
      } else if (!this.getPermissionsFor(bspy).hasOperatorRead()) {
         throw new PermissionException();
      } else {
         Spy spy = bspy.get();
         ByteArrayOutputStream bout = new ByteArrayOutputStream();
         SpyWriter out = new SpyWriter(bout, path, this.getSessionContext());
         spy.write(out);
         out.flush();
         byte[] content = bout.toByteArray();
         FoxResponse resp = new FoxResponse(req);
         resp.add("title", spy.getTitle());
         resp.add("content", content);
         return resp;
      }
   }
}
