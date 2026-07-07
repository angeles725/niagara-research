package com.tridium.template.file;

import com.tridium.template.manifest.TemplateManifest;
import java.util.Objects;
import java.util.logging.Logger;
import javax.baja.file.BDataFile;
import javax.baja.file.BDirectory;
import javax.baja.file.FilePath;
import javax.baja.file.types.image.BImageFile;
import javax.baja.naming.UnresolvedException;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.sys.BComponent;
import javax.baja.sys.BObject;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;
import javax.baja.util.BUuid;

@NiagaraType
public class BNewNtplFromTemporary extends BObject implements BINtplFile {
   public static final Type TYPE = Sys.loadType(BNewNtplFromTemporary.class);
   private volatile boolean open;
   private final BNtplFile source;
   protected static final Logger log = Logger.getLogger("template.ntpl");

   public Type getType() {
      return TYPE;
   }

   public BNewNtplFromTemporary() {
      this(null);
   }

   public BNewNtplFromTemporary(BNtplFile source) {
      this.source = source;
      this.open = source != null;
   }

   public String getTemplateName() {
      return Objects.requireNonNull(this.source).getTemplateConfig().getTemplateName();
   }

   public String getVendor() {
      return Objects.requireNonNull(this.source).getVendor();
   }

   public String getVersion() {
      return Objects.requireNonNull(this.source).getVersion();
   }

   public String getDescription() {
      return Objects.requireNonNull(this.source).getDescription();
   }

   public BUuid getUID() {
      return Objects.requireNonNull(this.source).getUID();
   }

   public BComponent getBaseComponent() throws UnresolvedException {
      return Objects.requireNonNull(this.source).getBaseComponent();
   }

   @Override
   public FilePath getFilePath() {
      return Objects.requireNonNull(this.source).getFilePath();
   }

   @Override
   public TemplateManifest getTemplateManifest() {
      return Objects.requireNonNull(this.source).getTemplateManifest();
   }

   @Override
   public PxFileRef[] getPxFiles() {
      return Objects.requireNonNull(this.source).getPxFiles();
   }

   @Override
   public BImageFile[] getPxImageFiles() {
      return Objects.requireNonNull(this.source).getPxImageFiles();
   }

   @Override
   public BImageFile getImageFile() {
      return Objects.requireNonNull(this.source).getImageFile();
   }

   @Override
   public BDataFile[] getStationFiles() {
      return Objects.requireNonNull(this.source).getStationFiles();
   }

   @Override
   public BDirectory[] getStationFileDirectories() {
      return Objects.requireNonNull(this.source).getStationFileDirectories();
   }

   @Override
   public boolean isReadOnly() {
      return false;
   }

   @Override
   public boolean isOpen() {
      return this.open;
   }

   @Override
   public boolean isNew() {
      return true;
   }

   @Override
   public void close() {
      if (this.open) {
         this.open = false;

         try {
            this.source.close();
            this.source.delete();
         } catch (Exception var2) {
            log.fine("Failed to delete temporary file: " + var2.getMessage());
         }
      }
   }
}
