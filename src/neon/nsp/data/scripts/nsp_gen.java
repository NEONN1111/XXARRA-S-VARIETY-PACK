package neon.nsp.data.scripts;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.*;
import org.apache.log4j.Logger;

@SuppressWarnings("unchecked")
public class nsp_gen implements SectorGeneratorPlugin {
    public static Logger log = Global.getLogger(nsp_gen.class);

    //Generate Systems
    @Override
    public void generate(SectorAPI sector) {
        new nsp_gate().generate(sector);
    }
}

