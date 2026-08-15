package neon.nsp.data.hullmods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignUIAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.characters.MutableCharacterStatsAPI;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;

import java.awt.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class ImprovisedAutomationAuto extends BaseHullMod {

    private final String nerfkey = "$nsp_improvised_auto_nerfed";
    private final int alphaCoreMaxLevel = 5;
    private final int otherCoreMaxLevel = 5;
    private final Random random = new Random();

    private static final String AUTO_MODE = "nsp_improvised_auto";
    public static Map mag = new HashMap();
    static {
        mag.put(HullSize.FRIGATE, 2f);
        mag.put(HullSize.DESTROYER, 3f);
        mag.put(HullSize.CRUISER, 4f);
        mag.put(HullSize.CAPITAL_SHIP, 5f);
    }


    @Override
    public void addPostDescriptionSection(TooltipMakerAPI tooltip, ShipAPI.HullSize hullSize, ShipAPI ship, float width, boolean isForModSpec) {
        tooltip.addSectionHeading("Ship State", Alignment.MID, 5f);
        tooltip.addPara("The ship is currently %s.", 5f, Color.ORANGE, "automated");
        tooltip.addSectionHeading("Affected Deployment", Alignment.MID, 5f);
        tooltip.addPara("As a result of the idiosyncrasies of these modifications, the Deployment Point cost of this ship is raised by %s/%s/%s/%s.", 5f, Color.ORANGE, "2","3","4","5");
    }

    @Override
    public int getDisplayCategoryIndex() {
        return 0;
    }

    @Override
    public int getDisplaySortOrder() {
        return 1;
    }

    @Override
    public boolean canBeAddedOrRemovedNow(
            ShipAPI ship,
            MarketAPI marketOrNull,
            CampaignUIAPI.CoreUITradeMode mode
    ) {
        return (ship != null && (ship.getCaptain() == null || ship.getCaptain().isDefault()));
    }

    @Override
    public String getCanNotBeInstalledNowReason(
            ShipAPI ship,
            MarketAPI marketOrNull,
            CampaignUIAPI.CoreUITradeMode mode
    ) {
        return "Must not have a captain assigned to remove.";
    }

    private int getMaxAllowedLevel(PersonAPI captain) {
        String coreId = captain.getAICoreId();
        if ("alpha_core".equals(coreId)) {
            return alphaCoreMaxLevel;
        }
        return otherCoreMaxLevel;
    }

    @Override
    public void applyEffectsBeforeShipCreation(HullSize hullSize, MutableShipStatsAPI stats, String id) {

        stats.getMinCrewMod().modifyMult(id, 0);
        stats.getMaxCrewMod().modifyMult(id, 0);
        stats.getSuppliesPerMonth().modifyFlat(id, (Float) mag.get(hullSize));
        stats.getSuppliesToRecover().modifyFlat(id, (Float) mag.get(hullSize));
        stats.getDynamic().getMod("deployment_points_mod").modifyFlat(id, (Float) mag.get(hullSize));
    }

    @Override
    public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {
        if (ship == null) return;

        PersonAPI cap = ship.getCaptain();
        if (cap != null && !cap.isPlayer() && !cap.isDefault() && cap.isAICore()
                && !cap.getMemoryWithoutUpdate().getBoolean(nerfkey)
                && !Global.getSector().getImportantPeople().containsPerson(cap)) {

            cap.getMemoryWithoutUpdate().set(nerfkey, true);

            int maxAllowedLevel = getMaxAllowedLevel(cap);
            int currentLevel = cap.getStats().getLevel();

            if (currentLevel <= maxAllowedLevel) return;

            int skillsToRemove = currentLevel - maxAllowedLevel;
            int skillsRemoved = 0;
            int numAttempts = 0;

            List<MutableCharacterStatsAPI.SkillLevelAPI> skillsCopy = cap.getStats().getSkillsCopy();
            if (skillsCopy.isEmpty()) return;

            while (numAttempts < 100 && skillsRemoved < skillsToRemove) {
                numAttempts += 1;

                if (skillsCopy.isEmpty()) break;

                int randomIndex = random.nextInt(skillsCopy.size());
                MutableCharacterStatsAPI.SkillLevelAPI pick = skillsCopy.get(randomIndex);

                if (pick != null &&
                        (pick.getSkill().getSourceMod() == null ||
                                "QualityCaptains".equals(pick.getSkill().getSourceMod().getId())) &&
                        !pick.getSkill().isAptitudeEffect() &&
                        pick.getLevel() > 0f) {

                    cap.getStats().setSkillLevel(pick.getSkill().getId(), 0f);
                    skillsRemoved += 1;
                    cap.getStats().setLevel(cap.getStats().getLevel() - 1);

                    skillsCopy = cap.getStats().getSkillsCopy();
                    if (skillsCopy.isEmpty()) break;
                }
            }
        }

        if (ship.getVariant() != null && !ship.getVariant().hasHullMod(AUTO_MODE)) {
            ship.getMutableStats().getMinCrewMod().unmodifyMult(id);
            ship.getMutableStats().getMaxCrewMod().unmodifyMult(id);
        }
    }
}