package com.tridium.niagarad.servlet;

import com.tridium.niagarad.http.Http;
import java.io.IOException;
import java.util.List;
import java.util.logging.Level;
import javax.baja.nre.util.ByteBuffer;
import javax.baja.xml.XWriter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class DefaultServlet extends Servlet {
   public DefaultServlet() {
      super("");
   }

   @Override
   public void doGet(HttpServletRequest req, HttpServletResponse resp) {
      String uri = this.getUriWithoutName(req.getRequestURI());
      if (!uri.equalsIgnoreCase("") && !uri.equalsIgnoreCase("/")) {
         String servletName = Http.getServletName(req.getRequestURI());
         if (!servletName.equalsIgnoreCase("servlets")) {
            Http.sendError(req, resp, 404);
         } else {
            ByteBuffer buffer = new ByteBuffer();
            XWriter content = new XWriter();
            content.setOutputStream(buffer.getOutputStream());
            List<Servlet> servlets = this.getServer().getServlets();
            content.prolog();
            content.w("<servlets>\n");
            if (servlets != null) {
               for (Servlet current : servlets) {
                  if (current.useDefaultAuthentication()
                     || !current.requiresAuthentication()
                     || current.requiresAuthentication() && current.authenticate(req, resp)) {
                     content.w("  <servlet ").attr("name", current.getName());
                     switch (current.getState()) {
                        case 0:
                           content.w(' ').attr("state", "idle");
                           break;
                        case 1:
                           content.w(' ').attr("state", "starting");
                           break;
                        case 2:
                           content.w(' ').attr("state", "running");
                           break;
                        case 3:
                           content.w(' ').attr("state", "stopping");
                           break;
                        default:
                           content.w(' ').attr("state", String.valueOf(current.getState()));
                     }

                     content.w("/>\n");
                  }
               }
            }

            content.w("</servlets>\n");
            content.flush();
            content.close();
            byte[] bytes = buffer.toByteArray();
            resp.setStatus(200);
            resp.setHeader("Content-Type", "text/xml");
            resp.setIntHeader("Content-Length", bytes.length);

            try {
               resp.getOutputStream().write(bytes);
            } catch (IOException ioe) {
               if (this.getServer() != null && this.getServer().getState() == 1) {
                  this.getServer().getFilter().log(Level.SEVERE, "default: failed to write default servlet response (" + ioe + ")", ioe);
                  Http.sendError(req, resp, 500);
               }
            }
         }
      } else {
         resp.setStatus(200);
         ByteBuffer buffer = new ByteBuffer();
         XWriter xml = new XWriter();
         xml.setOutputStream(buffer.getOutputStream());
         xml.w("<html>\n");
         xml.w("<body>\n");
         xml.w("<h1>");
         xml.w("OK");
         xml.w("</h1>\n");
         xml.w("</body>\n");
         xml.w("</html>");
         xml.flush();
         xml.close();
         byte[] bytes = buffer.toByteArray();
         resp.setHeader("Content-Type", "text/html");
         resp.setIntHeader("Content-Length", bytes.length);

         try {
            resp.getOutputStream().write(bytes);
         } catch (IOException ioe) {
            if (this.getServer() != null && this.getServer().getState() == 1) {
               this.getServer().getFilter().log(Level.SEVERE, "default: failed to write default servlet response (" + ioe + ")", ioe);
               Http.sendError(req, resp, 500);
            }
         }
      }
   }
}
