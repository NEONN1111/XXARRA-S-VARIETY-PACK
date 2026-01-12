package neon.nsp.data.scripts.rulecmd;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import com.fs.starfarer.api.impl.campaign.rulecmd.BaseCommandPlugin;
import com.fs.starfarer.api.impl.campaign.terrain.DebrisFieldTerrainPlugin;
import com.fs.starfarer.api.util.Misc;
import org.lwjgl.util.vector.Vector2f;

import java.util.List;
import java.util.Map;

public class NSPPlaySound extends BaseCommandPlugin {

    protected SectorEntityToken entity;

    @Override
    public boolean execute(String ruleId, InteractionDialogAPI dialog, List<Misc.Token> params, Map<String, MemoryAPI> memoryMap) {

        String soundId = "";
        float soundVolume = 1f;
        if (!params.isEmpty()) {
            soundId = params.get(0).getString(memoryMap);
            soundVolume = params.get(1).getFloat(memoryMap);
        }

        try {
            Global.getSoundPlayer().playSound(soundId, 1, soundVolume, Global.getSoundPlayer().getListenerPos(), new Vector2f());
            return true;
        } catch (Exception e) {
            dialog.getTextPanel().addPara(e.getMessage(),Misc.getNegativeHighlightColor());
            return false;
        }

    }
}
