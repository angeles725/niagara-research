package com.tridium.lonworks.xml;

import javax.baja.file.types.text.BIXmlFile;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType
public interface BILnmlFile extends BIXmlFile {
   Type TYPE = Sys.loadType(BILnmlFile.class);
}
