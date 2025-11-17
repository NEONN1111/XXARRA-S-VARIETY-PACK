package neon.nsp.data.scripts;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.*;
import org.apache.log4j.Logger;

@SuppressWarnings("unchecked")
public class tll_gen2 implements SectorGeneratorPlugin {
    public static Logger log = Global.getLogger(tll_gen2.class);

    //Generate Systems
    @Override
    public void generate(SectorAPI sector) {
        new tll_system2().generate(sector);
    }
}

