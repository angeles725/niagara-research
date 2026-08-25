package com.tridium.crypto.core.io;

import java.security.cert.TrustAnchor;
import java.util.Set;

public interface TrustAnchorProvider {
   Set<TrustAnchor> getTrustAnchors();
}
