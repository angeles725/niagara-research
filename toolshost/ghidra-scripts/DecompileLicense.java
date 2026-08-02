import ghidra.app.script.GhidraScript;
import ghidra.app.decompiler.DecompInterface;
import ghidra.app.decompiler.DecompileResults;
import ghidra.program.model.listing.Function;
import ghidra.program.model.listing.FunctionManager;
import ghidra.program.model.symbol.Symbol;
import ghidra.program.model.symbol.SymbolIterator;
import ghidra.program.model.symbol.SymbolTable;
import ghidra.program.model.address.Address;
import java.io.File;
import java.io.PrintWriter;

public class DecompileLicense extends GhidraScript {
    @Override
    public void run() throws Exception {
        PrintWriter out = new PrintWriter(new File(
            "/home/cristian/niagara-research/tools/ghidra-scripts/decomp-out.txt"));
        SymbolTable st = currentProgram.getSymbolTable();
        FunctionManager fm = currentProgram.getFunctionManager();
        DecompInterface decomp = new DecompInterface();
        decomp.openProgram(currentProgram);
        String[] needles = { "isFeaturePresent", "getHostId", "getHostId0" };
        int found = 0;
        SymbolIterator it = st.getAllSymbols(true);
        while (it.hasNext()) {
            Symbol sym = it.next();
            String nm = sym.getName();
            boolean hit = false;
            for (String n : needles) {
                if (nm.contains(n)) { hit = true; break; }
            }
            if (!hit) continue;
            Address addr = sym.getAddress();
            Function f = fm.getFunctionAt(addr);
            out.println("### " + sym.getName() + " @ " + addr + " type=" + sym.getSymbolType());
            if (f != null) {
                found++;
                DecompileResults res = decomp.decompileFunction(f, 60, monitor);
                out.println(res.getDecompiledFunction() == null
                    ? "(decompile failed)"
                    : res.getDecompiledFunction().getC());
            }
        }
        out.println("### total functions decompiled: " + found);
        decomp.dispose();
        out.close();
        println("done, wrote " + found + " functions");
    }
}
