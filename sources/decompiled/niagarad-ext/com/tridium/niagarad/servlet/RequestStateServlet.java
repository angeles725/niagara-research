package com.tridium.niagarad.servlet;

import com.tridium.niagarad.log.ErrorHandler;
import com.tridium.niagarad.log.MessageBundle;
import com.tridium.niagarad.util.CsrfTokenUtil;
import com.tridium.niagarad.util.KeyedList;
import com.tridium.niagarad.util.RequestState;
import javax.baja.xml.XWriter;
import javax.servlet.http.HttpServletRequest;

public class RequestStateServlet extends DaemonServlet {
   public RequestStateServlet() {
      super("requeststate");
   }

   @Override
   public int doGet(HttpServletRequest request, ErrorHandler handler, KeyedList query, XWriter content) {
      if (query == null) {
         MessageBundle msg = new MessageBundle("platform", "Servlet.missingParam", "action", "RequestStateServlet: Missing action argument");
         handler.error(msg);
         return 400;
      }

      int result = 400;
      if (query.containsKey("requestId")) {
         String requestId = query.get("requestId", "");
         String state = query.get("state", null);
         if (requestId.length() == 0) {
            MessageBundle msg = new MessageBundle("RequestStateServlet: Invalid requestId argument (length == 0)");
            handler.error(msg);
         } else if (requestId.length() > 255) {
            MessageBundle msg = new MessageBundle("RequestStateServlet: Invalid requestId argument (length > 255)");
            handler.error(msg);
         } else {
            boolean update = Boolean.valueOf(query.get("delete", "false")) || state != null;
            if (update && !DebugServlet.debugEnabled && !CsrfTokenUtil.verifyCsrfToken(request, query.get("csrfToken", null))) {
               MessageBundle msg = new MessageBundle("invalid CSRF token in request");
               handler.error(msg);
               return 403;
            }

            if (Boolean.valueOf(query.get("delete", "false"))) {
               RequestState.getInstance().removeRequest(requestId);
               result = 200;
            } else if (state != null) {
               if (state.length() > 255) {
                  MessageBundle msg = new MessageBundle("RequestStateServlet: Invalid state argument (length > 255)");
                  handler.error(msg);
               } else {
                  RequestState.getInstance().updateRequest(requestId, state);
                  result = 200;
               }
            } else {
               state = RequestState.getInstance().getRequestState(requestId);
               content.w("<request ").attr("id", requestId).w(" ").attr("state", state != null && !state.isEmpty() ? state : "unknown").w("/>\n");
               result = 200;
            }
         }
      } else {
         MessageBundle msg = new MessageBundle("platform", "Servlet.missingParam", "requestId", "RequestStateServlet: Missing requestId argument");
         handler.error(msg);
      }

      return result;
   }
}
