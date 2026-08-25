#!/usr/bin/env python3
"""SP-G10a diagnostic: is the Java bridge a runtime-module that can be loaded
explicitly, or is it stripped from this agent? Check ScriptRuntime + available
runtimes + whether Runtime.load('java') is a thing in this binding."""
import frida

print(f"[run] frida {frida.__version__}")
doc = getattr(getattr(frida.Script, "__init__", None), "__doc__", None) or "n/a"
print("[run] Script init docs:", doc[:500].replace("\n", " "))
print("[run] Session attrs:", [a for a in dir(frida.Session) if not a.startswith("_")][:30])
print("[run] top-level frida attrs (runtime-ish):", [a for a in dir(frida) if "untime" in a or "Java" in a or "runtime" in a.lower()])
print("[run] Script signature hints:", [a for a in dir(frida.Script) if not a.startswith("_")])
