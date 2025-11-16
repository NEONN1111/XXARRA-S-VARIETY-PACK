package data.scripts.rulecmd;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.impl.campaign.ids.FleetTypes;
import com.fs.starfarer.api.impl.campaign.rulecmd.BaseCommandPlugin;
import com.fs.starfarer.api.impl.campaign.world.NamelessRock;
import com.fs.starfarer.api.impl.combat.threat.DisposableThreatFleetManager;
import com.fs.starfarer.api.impl.combat.threat.ThreatFleetBehaviorScript;
import com.fs.starfarer.api.util.Misc;
import org.lwjgl.util.vector.Vector2f;

import java.util.List;
import java.util.Map;
import java.util.Random;

public class nsp_spawnThreatFleet extends BaseCommandPlugin {

    protected CampaignFleetAPI playerFleet;
    protected SectorEntityToken entity;
    protected TextPanelAPI text;
    protected OptionPanelAPI options;
    protected MemoryAPI memory;
    protected InteractionDialogAPI dialog;
    protected Map<String, MemoryAPI> memoryMap;


    public nsp_spawnThreatFleet() {
    }

    public nsp_spawnThreatFleet(SectorEntityToken entity) {
        init(entity);
    }

    protected void init(SectorEntityToken entity) {
        memory = entity.getMemoryWithoutUpdate();
        this.entity = entity;


    }
    @Override
    public boolean execute(String ruleId, InteractionDialogAPI dialog, List<Misc.Token> params, Map<String, MemoryAPI> memoryMap) {
        this.dialog = dialog;
        this.memoryMap = memoryMap;
        CampaignFleetAPI playerFleet = Global.getSector().getPlayerFleet();

        String command = params.get(0).getString(memoryMap);
        if (command == null) return false;
        if (command.equals("legion")) {

            DisposableThreatFleetManager.ThreatFleetCreationParams p = new DisposableThreatFleetManager.ThreatFleetCreationParams();
            p.numFabricators = 3;
            p.numHives = 3;
            p.numOverseers = 1;
            p.numDestroyers = 2;
            p.numFrigates = 4;
            p.fleetType = FleetTypes.PATROL_LARGE;

            CampaignFleetAPI fleet = DisposableThreatFleetManager.createThreatFleet(p, new Random());
            ThreatFleetBehaviorScript behavior = new ThreatFleetBehaviorScript(fleet, playerFleet.getStarSystem());
            behavior.setSeenByPlayer();
            fleet.addScript(behavior);

            playerFleet.getStarSystem().addEntity(fleet);
            float radius = 1000f + 500f * (float) Math.random();
            Vector2f loc = Misc.getPointAtRadius(playerFleet.getLocation(), radius);
            fleet.setLocation(loc.x, loc.y);
        }
        else if (command.equals("dominator")){
            DisposableThreatFleetManager.ThreatFleetCreationParams p = new DisposableThreatFleetManager.ThreatFleetCreationParams();
            p.numFabricators = 1;
            p.numHives = 2;
            p.numOverseers = 0;
            p.numDestroyers = 1;
            p.numFrigates = 1;
            p.fleetType = FleetTypes.PATROL_MEDIUM;

            CampaignFleetAPI fleet = DisposableThreatFleetManager.createThreatFleet(p, new Random());
            ThreatFleetBehaviorScript behavior = new ThreatFleetBehaviorScript(fleet, playerFleet.getStarSystem());
            behavior.setSeenByPlayer();
            fleet.addScript(behavior);

            playerFleet.getStarSystem().addEntity(fleet);
            float radius = 1000f + 500f * (float) Math.random();
            Vector2f loc = Misc.getPointAtRadius(playerFleet.getLocation(), radius);
            fleet.setLocation(loc.x, loc.y);
        }

        return true;
    }
}
