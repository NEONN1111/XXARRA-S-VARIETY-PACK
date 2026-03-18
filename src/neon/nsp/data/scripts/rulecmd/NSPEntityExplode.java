package neon.nsp.data.scripts.rulecmd;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import com.fs.starfarer.api.impl.campaign.rulecmd.BaseCommandPlugin;
import com.fs.starfarer.api.impl.campaign.terrain.DebrisFieldTerrainPlugin;
import com.fs.starfarer.api.util.Misc;
import neon.nsp.data.scripts.everyframe.NSP_CampaignEntityExplosionScript;
import org.lwjgl.util.vector.Vector2f;

import java.util.List;
import java.util.Map;

public class NSPEntityExplode extends BaseCommandPlugin {

    protected SectorEntityToken entity;

    @Override
    public boolean execute(String ruleId, InteractionDialogAPI dialog, List<Misc.Token> params, Map<String, MemoryAPI> memoryMap) {
        entity = dialog.getInteractionTarget();
        Global.getSoundPlayer().playSound("explosion_from_damage", 1, 1, Global.getSoundPlayer().getListenerPos(), new Vector2f());
        Misc.fadeAndExpire(entity, 1f);

        DebrisFieldTerrainPlugin.DebrisFieldParams debrisparams = new DebrisFieldTerrainPlugin.DebrisFieldParams(150f, 0.5f, 10f, 5f);
        debrisparams.source = DebrisFieldTerrainPlugin.DebrisFieldSource.PLAYER_SALVAGE;
        SectorEntityToken debris = Misc.addDebrisField(entity.getContainingLocation(), debrisparams, null);
        debris.setSensorProfile(null);
        debris.setDiscoverable(null);
        debris.setFaction(Factions.NEUTRAL);
        if (entity.getOrbit() != null) {
            debris.setOrbit(entity.getOrbit().makeCopy());
        } else {
            debris.getLocation().set(entity.getLocation());
        }

        Global.getSector().addScript(new NSP_CampaignEntityExplosionScript(entity,4f, 0.5f));
//        CombatEngineAPI engine = Global.getCombatEngine();
//        engine.spawnExplosion(entity.getLocation(),new Vector2f(), Color.WHITE,1500f,2f);

        return true;
    }
}
