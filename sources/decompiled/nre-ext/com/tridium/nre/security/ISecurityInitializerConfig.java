package com.tridium.nre.security;

import com.tridium.nre.di.IConfiguration;
import java.io.File;

public interface ISecurityInitializerConfig extends IConfiguration {
   File getSecDir();

   String getKmName();
}
