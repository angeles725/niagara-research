package com.tridium.template.file;

import com.tridium.template.manifest.TemplateManifest;
import javax.baja.file.BDataFile;
import javax.baja.file.BDirectory;
import javax.baja.file.BITemplate;
import javax.baja.file.FilePath;
import javax.baja.file.types.image.BImageFile;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType
public interface BINtplFile extends BITemplate, AutoCloseable {
   Type TYPE = Sys.loadType(BINtplFile.class);

   FilePath getFilePath();

   TemplateManifest getTemplateManifest();

   PxFileRef[] getPxFiles();

   BImageFile[] getPxImageFiles();

   BImageFile getImageFile();

   BDataFile[] getStationFiles();

   BDirectory[] getStationFileDirectories();

   boolean isReadOnly();

   boolean isOpen();

   boolean isNew();

   @Override
   void close();
}
