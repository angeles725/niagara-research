package com.tridium.fox.sys.broker;

import com.tridium.fox.encoding.BogCodec;
import com.tridium.fox.encoding.DecoderFactory;
import com.tridium.fox.message.FoxMessage;
import com.tridium.fox.message.FoxTuple;
import com.tridium.fox.session.FoxRequest;
import com.tridium.fox.session.FoxResponse;
import com.tridium.sys.transfer.CompTransferResult;
import com.tridium.sys.transfer.TransferResult;
import com.tridium.sys.transfer.TransferStrategy;
import javax.baja.naming.BISession;
import javax.baja.naming.BLocalHost;
import javax.baja.naming.BOrd;
import javax.baja.space.BISpaceNode;
import javax.baja.space.Mark;
import javax.baja.sys.BComponent;
import javax.baja.sys.BObject;
import javax.baja.sys.Context;
import javax.baja.util.BNameMap;

public class TransferCodec {
   private static String getOrdStr(BISpaceNode n) {
      BISession session = n.getSession();
      BOrd ord = n.getOrdInSession();
      return ord.toString();
   }

   public static FoxRequest transferToMessage(FoxRequest req, TransferStrategy t) throws Exception {
      req.add("target", getOrdStr((BISpaceNode)t.getTarget()));
      Mark mark = t.getMark();
      BObject[] values = mark.getValues();
      String[] names = mark.getNames();

      for (int i = 0; i < values.length; i++) {
         FoxMessage s = new FoxMessage("mark");
         s.add("ord", getOrdStr((BISpaceNode)values[i]));
         s.add("name", names[i]);
         req.add(s);
      }

      req.add("action", t.getAction());
      BogCodec.add(req, "params", t.getParameters(), null);
      return req;
   }

   public static TransferStrategy messageToTransfer(FoxMessage msg, Context cx) throws Exception {
      BObject target = BOrd.make(msg.getString("target")).get(BLocalHost.INSTANCE, cx);
      FoxTuple[] s = msg.list("mark");
      BObject[] values = new BObject[s.length];
      String[] names = new String[s.length];

      for (int i = 0; i < s.length; i++) {
         FoxMessage src = (FoxMessage)s[i];
         values[i] = BOrd.make(src.getString("ord")).get(BLocalHost.INSTANCE, cx);
         names[i] = src.getString("name");
      }

      Mark mark = new Mark(values, names);
      int action = msg.getInt("action");
      BComponent params = (BComponent)DecoderFactory.decode(msg, "params", null);
      return TransferStrategy.make(action, mark, target, params, cx);
   }

   public static FoxResponse resultToMessage(FoxResponse resp, TransferResult result) throws Exception {
      if (!(result instanceof CompTransferResult)) {
         return null;
      } else {
         CompTransferResult r = (CompTransferResult)result;
         resp.add("class", r.getClass().getName());
         resp.add("action", r.action);
         resp.add("origParent", r.origParent.getOrdInSession().toString());

         for (int i = 0; i < r.origNames.length; i++) {
            resp.add("origName", r.origNames[i]);
         }

         resp.add("target", r.target.getOrdInSession().toString());

         for (int i = 0; i < r.insertNames.length; i++) {
            resp.add("insertName", r.insertNames[i]);
         }

         if (r.origDisplayNames != null) {
            resp.add("origDisplayNames", r.origDisplayNames.encodeToString());
         }

         return resp;
      }
   }

   public static TransferResult messageToResult(BObject baseSession, FoxMessage msg) throws Exception {
      if (msg == null) {
         return null;
      } else {
         int action = msg.getInt("action");
         BOrd origParentOrd = BOrd.make(msg.getString("origParent"));
         String[] origNames = msg.listStrings("origName");
         BOrd targetOrd = BOrd.make(msg.getString("target"));
         String[] insertNames = msg.listStrings("insertName");
         String origDisplayNamesStr = msg.getString("origDisplayNames", null);
         BNameMap origDisplayNames = origDisplayNamesStr != null ? (BNameMap)BNameMap.DEFAULT.decodeFromString(origDisplayNamesStr) : null;
         BComponent origParent = (BComponent)origParentOrd.get(baseSession);
         BComponent target = (BComponent)targetOrd.get(baseSession);
         return new CompTransferResult(action, origParent, origNames, target, insertNames, origDisplayNames, null);
      }
   }
}
