package com.tridium.niagarad.servlet;

import com.tridium.niagarad.NiagaraDaemon;
import com.tridium.niagarad.log.ErrorHandler;
import com.tridium.niagarad.log.MessageBundle;
import com.tridium.niagarad.util.CsrfTokenUtil;
import com.tridium.niagarad.util.KeyedList;
import javax.baja.xml.XWriter;
import javax.servlet.http.HttpServletRequest;

public class ClearDaemonOutputServlet extends DaemonServlet {
   public ClearDaemonOutputServlet() {
      super("cleardaemonoutput");
   }

   @Override
   public int doGet(HttpServletRequest request, ErrorHandler handler, KeyedList query, XWriter content) {
      if (!DebugServlet.debugEnabled && !CsrfTokenUtil.verifyCsrfToken(request, query.get("csrfToken", null))) {
         MessageBundle msg = new MessageBundle("invalid CSRF token in request");
         handler.error(msg);
         return 403;
      } else {
         NiagaraDaemon.niagaraDaemonOutputBuffer.clear();
         return 200;
      }
   }
}
