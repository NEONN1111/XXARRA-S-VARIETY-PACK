package neon.nsp.data.scripts.skills;

import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.CargoStackAPI;
import com.fs.starfarer.api.campaign.CoreUITabId;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.characters.*;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import com.fs.starfarer.api.impl.campaign.ids.Stats;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import com.fs.starfarer.api.impl.campaign.skills.BaseSkillEffectDescription;
import com.fs.starfarer.api.impl.hullmods.Automated;
import com.fs.starfarer.api.ui.LabelAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import java.awt.Color;
import java.util.HashMap;
import java.util.List;

public class NSP_DerelictPilot extends BaseSkillEffectDescription implements CharacterStatsSkillEffect, ShipSkillEffect {
    private final Color color = new Color(100, 250, 210, 250);

    // CharacterStatsSkillEffect methods
    @Override
    public String getEffectDescription(float level) {
        return "Allows piloting of automated ships and provides various bonuses when in good standing with Explorarium";
    }

    @Override
    public String getEffectPerLevelDescription() {
        return "";
    }

    @Override
    public LevelBasedEffect.ScopeDescription getScopeDescription() {
        return LevelBasedEffect.ScopeDescription.FLEET;
    }

    @Override
    public void apply(MutableCharacterStatsAPI stats, String id, float level) {
        // This ensures the skill effect is properly registered as a character stats effect
        if (stats.isPlayerStats()) {
            Misc.getAllowedRecoveryTags().add(Tags.AUTOMATED_RECOVERABLE);

            // Check if playerCoreScript is already running
            SectorAPI sector = Global.getSector();
            if (!sector.hasTransientScript(PlayerCoreScript.class)) {
                sector.addTransientScript(new PlayerCoreScript());
            }
        }
    }

    @Override
    public void unapply(MutableCharacterStatsAPI stats, String id) {
        // Nothing to do here
    }

    // ShipSkillEffect methods
    @Override
    public void apply(MutableShipStatsAPI stats, ShipAPI.HullSize hullSize, String id, float level) {
        SectorAPI sector = Global.getSector();

        // Check relations with Derelict/Explorarium
        if (sector.getFaction(Factions.DERELICT).getRelToPlayer().getRel() < -0.2f) {
            Automated.MAX_CR_PENALTY = 1f;
            return;
        }

        Misc.getAllowedRecoveryTags().add(Tags.AUTOMATED_RECOVERABLE);
        Automated.MAX_CR_PENALTY = 0f;

        // Apply solar corona damage immunity
        stats.getDynamic().getStat(Stats.CORONA_EFFECT_MULT).modifyMult(id, 0f);
    }

    @Override
    public void unapply(MutableShipStatsAPI stats, ShipAPI.HullSize hullSize, String id) {
        Automated.MAX_CR_PENALTY = 1f;
        stats.getDynamic().getStat(Stats.CORONA_EFFECT_MULT).unmodify(id);
    }

    @Override
    public void createCustomDescription(MutableCharacterStatsAPI stats, SkillSpecAPI skill,
                                        TooltipMakerAPI info, float width) {
        FactionAPI derelictFaction = Global.getSector().getFaction(Factions.DERELICT);

        LabelAPI para1 = info.addPara("A neural link taps into the Explorarium mass mind.", 0f);
        para1.setHighlight("Explorarium");
        para1.setHighlightColor(derelictFaction.getBrightUIColor());

        LabelAPI para2 = info.addPara("While you are in good relations with the Explorarium, confers the following effects:", 0f);
        para2.setHighlight("good", "Explorarium");
        para2.setHighlightColors(Misc.getHighlightColor(), derelictFaction.getBrightUIColor());

        LabelAPI para3 = info.addPara("Allows the recovery and piloting of automated ships.", 10f);
        para3.setHighlight("recovery", "piloting");

        LabelAPI para4 = info.addPara("If you have no human officers, automated ships do not suffer from the normal CR penalty.", 0f);
        para4.setHighlight("no human officers", "do not");

        LabelAPI para5 = info.addPara("Additionally, AI cores integrated into ships will grant additional deployment points in combat. Higher level AI cores grant more.", 0f);
        para5.setHighlight("integrated");

        LabelAPI para6 = info.addPara("This bonus maxes out at +20% deployment points.", 0f);
        para6.setHighlight("+20%");

        LabelAPI para7 = info.addPara("Also prevents all damage from solar corona and similar hazards", 0f);
        para7.setHighlight("prevents all damage");
    }
}

class PlayerCoreScript implements EveryFrameScript {
    private final HashMap<FleetMemberAPI, String> aiBoats = new HashMap<FleetMemberAPI, String>();

    @Override
    public boolean isDone() {
        return false; // This script should run continuously
    }

    @Override
    public boolean runWhilePaused() {
        return true; // Run even when the game is paused
    }

    @Override
    public void advance(float amount) {
        SectorAPI sector = Global.getSector();
        if (sector.getPlayerFleet() == null) return;

        CargoAPI cargo = sector.getPlayerFleet().getCargo();
        if (cargo == null) return;

        PersonAPI player = sector.getPlayerPerson();
        if (player == null) return;

        // Check if player has the skill and good relations
        if (!player.getStats().hasSkill("ds_paradeigma") ||
                sector.getFaction(Factions.DERELICT).getRelToPlayer().getRel() < -0.2f) {
            removeCore(cargo);
            sector.removeTransientScriptsOfClass(this.getClass());
            return;
        }

        CoreUITabId currentTab = sector.getCampaignUI().getCurrentCoreTab();
        if (currentTab == CoreUITabId.REFIT || currentTab == CoreUITabId.FLEET) {
            addCore(cargo);

            // Process AI boats
            for (java.util.Map.Entry<FleetMemberAPI, String> entry : aiBoats.entrySet()) {
                FleetMemberAPI member = entry.getKey();
                String coreId = entry.getValue();
                if (member.getCaptain().isPlayer()) {
                    sector.getPlayerFleet().getCargo().addCommodity(coreId, 1f);
                }
            }
            aiBoats.clear();

            // Find AI core ships
            List<FleetMemberAPI> members = sector.getPlayerFleet().getFleetData().getMembersListCopy();
            for (FleetMemberAPI member : members) {
                if (member.isFighterWing()) continue;

                PersonAPI captain = member.getCaptain();
                if (captain != null && captain.isAICore() && !captain.isPlayer()) {
                    aiBoats.put(member, captain.getAICoreId());
                }
            }
        } else {
            removeCore(cargo);
        }
    }

    private void addCore(CargoAPI cargo) {
        if (cargo.getCommodityQuantity("ds_playercore") <= 0f) {
            cargo.addCommodity("ds_playercore", 1f);
        }
    }

    private void removeCore(CargoAPI cargo) {
        List<CargoStackAPI> stacks = cargo.getStacksCopy();
        for (CargoStackAPI stack : stacks) {
            if (stack.isCommodityStack() && "ds_playercore".equals(stack.getCommodityId())) {
                float amt = stack.getSize();
                cargo.removeCommodity("ds_playercore", amt);
            }
        }
    }
}