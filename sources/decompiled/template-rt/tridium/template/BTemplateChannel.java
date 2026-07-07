package com.tridium.template;

import com.tridium.fox.message.FoxMessage;
import com.tridium.fox.session.FoxCircuit;
import com.tridium.fox.session.FoxRequest;
import com.tridium.fox.session.FoxResponse;
import com.tridium.fox.session.InvalidCommandException;
import com.tridium.fox.sys.BFoxChannel;
import com.tridium.template.job.BUpgradeTemplateJob;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.baja.job.BJobState;
import javax.baja.naming.BOrd;
import javax.baja.naming.UnresolvedException;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.space.BComponentSpace;
import javax.baja.sys.BComponent;
import javax.baja.sys.BComponentEvent;
import javax.baja.sys.BIService;
import javax.baja.sys.BVector;
import javax.baja.sys.Subscriber;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;
import javax.baja.util.BServiceContainer;
import javax.baja.util.Lexicon;

@NiagaraType
public class BTemplateChannel extends BFoxChannel {
   public static final Type TYPE = Sys.loadType(BTemplateChannel.class);
   public static final String CHANNEL_NAME = "template";
   private static final Lexicon lex = Lexicon.make("template");
   public static final Logger logger = Logger.getLogger("template.channel");

   public Type getType() {
      return TYPE;
   }

   public BTemplateChannel() {
      super("template");
   }

   public BTemplateChannel(String name) {
      super(name);
   }

   public FoxResponse process(FoxRequest request) throws Exception {
      String command = request.command;
      throw new InvalidCommandException(command);
   }

   public void circuitOpened(FoxCircuit circuit) throws Exception {
      String command = circuit.command;
      if (command == "upgradeTemplate") {
         this.upgradeTemplate(circuit);
      } else {
         throw new InvalidCommandException(command);
      }
   }

   public void upgradeTemplate(final FoxCircuit circuit) throws Exception {
      FoxMessage request = circuit.readMessage();
      if (this.isTraceOn()) {
         this.trace("s:upgradeTemplate");
      }

      BOrd deployedOrd = BOrd.make(request.getString("deployedSlotPath"));

      BComponent deployedRootComponent;
      try {
         deployedRootComponent = deployedOrd.get(Sys.getStation()).asComponent();
      } catch (UnresolvedException var16) {
         this.sendResponse(circuit, lex.getText("templateChannel.nullTemplateComponent", new Object[]{deployedOrd.toString()}), "error");
         return;
      }

      if (deployedRootComponent == null) {
         this.sendResponse(circuit, lex.getText("templateChannel.nullTemplateComponent", new Object[]{deployedOrd.toString()}), "error");
      } else if (!deployedRootComponent.isMounted()) {
         this.sendResponse(
            circuit, lex.getText("templateChannel.templateComponentNotMounted", new Object[]{deployedRootComponent.getSlotPathOrd().toString()}), "error"
         );
      } else {
         BVector upgradeList = new BVector();
         BTemplateConfig templateConfig = BTemplateConfig.getConfigForRoot(deployedRootComponent);
         if (templateConfig != null) {
            BOrd tiOrd = templateConfig.getSlotPathOrd();
            upgradeList.add("v?", tiOrd);
         }

         BComponentSpace space = templateConfig.getComponentSpace();
         BComponent root = space.getRootComponent();
         BServiceContainer[] serviceContainer = (BServiceContainer[])root.getChildren(BServiceContainer.class);
         if (serviceContainer != null && serviceContainer.length != 0) {
            BIService[] services = (BIService[])serviceContainer[0].getChildren(BTemplateService.TYPE.getTypeClass());
            if (services != null && services.length != 0) {
               BTemplateService templateService = (BTemplateService)services[0];
               BUpgradeTemplateJob job = new BUpgradeTemplateJob(upgradeList, templateService);
               Subscriber sub = new Subscriber() {
                  public void event(BComponentEvent event) {
                     try {
                        BTemplateChannel.this.sendResponse(
                           circuit, BTemplateChannel.lex.getText("templateChannel.jobEvent", new Object[]{event.toString()}), "running"
                        );
                     } catch (Exception var3) {
                        BTemplateChannel.logger.log(Level.WARNING, "Template channel component event exception", (Throwable)var3);
                     }
                  }
               };
               sub.subscribe(job);
               job.run(null);
               this.sendResponse(circuit, lex.getText("templateChannel.jobEnd", new Object[]{job.getEndTime().encodeToString()}), "running");
               BJobState jobState = job.getJobState();
               String jobResult;
               switch (jobState.getOrdinal()) {
                  case 1:
                     jobResult = "running";
                     break;
                  case 2:
                  case 3:
                     jobResult = "canceled";
                     break;
                  case 4:
                  default:
                     jobResult = "complete";
                     break;
                  case 5:
                     jobResult = "failed";
               }

               this.sendResponse(circuit, lex.getText("templateChannel.jobComplete", new Object[]{job.getJobState().toString(null)}), jobResult);
               circuit.flush();
            } else {
               this.sendResponse(
                  circuit, lex.getText("templateChannel.noServicesFound", new Object[]{deployedRootComponent.getSlotPathOrd().toString()}), "error"
               );
            }
         } else {
            this.sendResponse(
               circuit, lex.getText("templateChannel.noServiceContainer", new Object[]{deployedRootComponent.getSlotPathOrd().toString()}), "error"
            );
         }
      }
   }

   private void sendResponse(FoxCircuit circuit, String responseMessages, String status) throws Exception {
      FoxMessage response = new FoxMessage();
      response.add("jobStatus", status);
      response.add("jobMessage", responseMessages);
      circuit.writeMessage(response);
   }

   public String[] upgradeTemplate(String deployedSlotPath) throws Exception {
      if (this.isTraceOn()) {
         this.trace("c:upgradeTemplate");
      }

      FoxCircuit circuit = this.openCircuit("upgradeTemplate");
      FoxMessage request = new FoxMessage();
      request.add("deployedSlotPath", deployedSlotPath);
      circuit.writeMessage(request);
      circuit.flush();
      List<String> result = new ArrayList<>();

      try {
         String messageType;
         do {
            FoxMessage message = circuit.readMessage();
            result.add(message.getString("jobMessage"));
            messageType = message.getString("jobStatus");
         } while (messageType.equals("running"));

         return result.toArray(new String[0]);
      } finally {
         circuit.close();
      }
   }
}
