package neon.nsp.data.scripts.skills;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.FleetDataAPI;
import com.fs.starfarer.api.characters.*;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import com.fs.starfarer.api.impl.campaign.skills.*;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import neon.nsp.data.scripts.util.NSP_Tags;

import java.awt.*;

import static com.fs.starfarer.api.impl.campaign.skills.BaseSkillEffectDescription.getFleetData;

public class ThreatAutomatedShips {


    public static float MAX_CR_BONUS = 80f;
    public static float MAX_THREAT_POINTS = 40f;
    public static float PENALTY_OFFSET = 100f;

    public static final float GAMMA_MULT = 2f;
    public static final float BETA_MULT = 3f;
    public static final float ALPHA_MULT = 4f;

    private static float getTotalThreatAutomatedPoints(FleetDataAPI data) {
        if (data == null) return 0f;

        float total = 0f;
        for (FleetMemberAPI member : data.getMembersListCopy()) {
            if (member.isMothballed()) continue;

            if (isThreatAutomatedShip(member)) {
                float dpCost = member.getHullSpec().getFleetPoints();
                float multiplier = getAICoreMultiplier(member);
                total += dpCost * multiplier;
            }
        }
        return total;
    }

    private static float getAICoreMultiplier(FleetMemberAPI member) {
        if (member == null || member.getCaptain() == null || !member.getCaptain().isAICore()) {
            return 1f;
        }

        String coreId = member.getCaptain().getAICoreId();
        if ("alpha_core".equals(coreId)) {
            return ALPHA_MULT;
        } else if ("beta_core".equals(coreId)) {
            return BETA_MULT;
        } else if ("gamma_core".equals(coreId)) {
            return GAMMA_MULT;
        } else if ("nsp_threat_processor".equals(coreId)) {
            return 0f;
        }
        return 1f;
    }

    public static boolean isThreatAutomatedShip(FleetMemberAPI member) {
        if (member == null) return false;

        if (member.getVariant() != null &&
                member.getVariant().getHullMods().contains("nsp_threat_automation")) {
            return true;
        }

        if (member.getVariant() != null &&
                member.getVariant().hasTag(NSP_Tags.THREAT_AUTOMATED)) {
            return true;
        }
        if (member.getHullSpec().hasTag(NSP_Tags.THREAT_AUTOMATED)) {
            return true;
        }

        return false;
    }

    public static boolean isThreatAutomatedNoPenalty(FleetMemberAPI member) {
        if (member == null) return false;

        if (member.getCaptain() != null && !member.getCaptain().isDefault() &&
                member.getCaptain().isAICore() &&
                "nsp_threat_processor".equals(member.getCaptain().getAICoreId())) {
            return true;
        }

        if (member.getFleetData() != null && member.getFleetData().getFleet() != null &&
                member.getFleetData().getFleet().getFaction() != null &&
                "threat".equals(member.getFleetData().getFleet().getFaction().getId())) {
            return true;
        }

        return false;
    }

    public static float computeThreatCRBonus(FleetDataAPI fleetData) {
        float totalPoints = MAX_THREAT_POINTS;
        float usedPoints = getTotalThreatAutomatedPoints(fleetData);

        if (usedPoints <= 0.001f) return MAX_CR_BONUS;
        if (usedPoints <= totalPoints) {
            return MAX_CR_BONUS;
        }

        float ratio = totalPoints / usedPoints;
        return MAX_CR_BONUS * ratio;
    }

    // Level 1
    public static class Level1 implements ShipSkillEffect, FleetTotalSource {

    private FleetTotalItem threatPointsItem = null;

    public FleetTotalItem getFleetTotalItem() {
        if (threatPointsItem == null) {
            threatPointsItem = new FleetTotalItem() {
                public String getId() {
                    return "nsp_threat_automated_points";
                }

                public String getDisplayName() {
                    return "Threat automated ship points";
                }

                public float getValue(FleetDataAPI data) {
                    return getTotalThreatAutomatedPoints(data);
                }
            };
        }
        return threatPointsItem;
    }

        public void apply(MutableShipStatsAPI stats, ShipAPI.HullSize hullSize, String id, float level) {
            FleetMemberAPI member = stats.getFleetMember();
            if (member == null) return;

            if (member.isMothballed()) return;

            if (isThreatAutomatedShip(member)) {
                FleetDataAPI fleetData = getFleetData(stats);
                if (fleetData != null) {
                    float crBonus = computeThreatCRBonus(fleetData);
                    SkillSpecAPI skill = Global.getSettings().getSkillSpec("nsp_threat_auto");
                    stats.getMaxCombatReadiness().modifyFlat(id, crBonus * 0.01f, skill.getName());
                }
            }
        }

    public void unapply(MutableShipStatsAPI stats, ShipAPI.HullSize hullSize, String id) {
        stats.getMaxCombatReadiness().unmodifyFlat(id);
    }


        // UNUSED BUT NECESSARY
        public String getEffectDescription(float level) {
            return " ";
        }

        // UNUSED BUT NECESSARY
        public String getEffectPerLevelDescription() {
            return null;
        }

        public ScopeDescription getScopeDescription() {
            return ScopeDescription.ALL_SHIPS;
        }

    }

    // Level 2
    public static class Level2 implements CharacterStatsSkillEffect {

        public void apply(MutableCharacterStatsAPI stats, String id, float level) {
            if (stats.isPlayerStats()) {
                Misc.getAllowedRecoveryTags().add(NSP_Tags.THREAT_RECOVERABLE);
                Misc.getAllowedRecoveryTags().add(Tags.AUTOMATED_RECOVERABLE);
            }
        }

        public void unapply(MutableCharacterStatsAPI stats, String id) {
            if (stats.isPlayerStats()) {
                Misc.getAllowedRecoveryTags().remove(NSP_Tags.THREAT_RECOVERABLE);
                Misc.getAllowedRecoveryTags().remove(Tags.AUTOMATED_RECOVERABLE);
            }
        }

        // UNUSED BUT NECESSARY
        public String getEffectDescription(float level) {
            return " ";
        }

        // UNUSED BUT NECESSARY
        public String getEffectPerLevelDescription() {
            return null;
        }

        public ScopeDescription getScopeDescription() {
            return ScopeDescription.PILOTED_SHIP;
        }



    }

    // Purely for the description bullets
    public static class Level3 extends BaseSkillEffectDescription implements ShipSkillEffect {
        public void createCustomDescription(MutableCharacterStatsAPI stats, SkillSpecAPI skill,
                                            TooltipMakerAPI info, float width) {
            init(stats, skill);
            float opad = 10f;
            Color c = Misc.getBasePlayerColor();

            info.addPara("%s combat readiness for Threat Automated ships (maximum: %s)",
                    0f,hc,hc,"+" + (int) MAX_CR_BONUS + "%", "+" + (int) MAX_CR_BONUS + "%");

            info.addPara("Enables the recovery of Threat vessels", hc, 0f);

            info.addPara("Offsets built-in %s penalty", 0f, hc,hc, "-" + (int) PENALTY_OFFSET + "%");

            info.addPara("Threat ships share their own pool of points separate from automated ships.", hc, 0f);
        }

        public void apply(MutableShipStatsAPI stats, ShipAPI.HullSize hullSize, String id, float level) {
//            if (!isOriginalNoOfficer(stats)) {
//                stats.getDynamic().getMod(Stats.COMMAND_POINT_RATE_FLAT).modifyFlat(id, COMMAND_POINT_REGEN_PERCENT * 0.01f);
//            }
        }

        public void unapply(MutableShipStatsAPI stats, ShipAPI.HullSize hullSize, String id) {
//            stats.getDynamic().getMod(Stats.COMMAND_POINT_RATE_FLAT).unmodify(id);
        }
    }

}
