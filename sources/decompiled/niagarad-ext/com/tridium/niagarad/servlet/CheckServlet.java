package com.tridium.niagarad.servlet;

import com.tridium.niagarad.log.ErrorHandler;
import com.tridium.niagarad.util.KeyedList;
import javax.baja.xml.XWriter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class CheckServlet extends DaemonServlet {
   public CheckServlet() {
      super("check");
   }

   @Override
   public int doGet(HttpServletRequest request, ErrorHandler handler, KeyedList query, XWriter content) {
      return 200;
   }

   @Override
   public boolean useDefaultAuthentication() {
      return false;
   }

   @Override
   public boolean requiresAuthentication() {
      return false;
   }

   @Override
   public boolean authenticate(HttpServletRequest req, HttpServletResponse resp) {
      return true;
   }
}
