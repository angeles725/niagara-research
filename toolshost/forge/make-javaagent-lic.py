# Minimal .license that satisfies the NATIVE text-match gate only (no valid signature).
# The Java layer rejects it (no signature / invalid cert); the native isFeaturePresent
# text-matches the literal feature name in the file content.
import base64, subprocess
HOST = 'Win-4D6F-169B-CEF1-8F57'
# "developer" appears as a literal in the feature name — native strstr hits it.
body = (f'<license vendor="Tridium" expiration="2030-12-31" hostId="{HOST}" version="4.10" generated="2026-08-01">\n'
        '<feature name="developer">\n</feature>\n</license>\n')
open('ja_body.xml','w').write(body)
raw = subprocess.run(['openssl','dgst','-sha1','-sign','attacker_dsa160.pem','ja_body.xml'], capture_output=True).stdout
sig = base64.b64encode(raw).decode()
lic = body.replace('</license>\n', f'<signature>{sig}</signature>\n</license>\n')
open('javaagent-developer.license','w').write(lic)
print('wrote javaagent-developer.license', len(lic))
