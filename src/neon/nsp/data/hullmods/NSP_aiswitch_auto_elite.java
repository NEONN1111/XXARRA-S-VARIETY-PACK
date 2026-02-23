package neon.nsp.data.hullmods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignUIAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.characters.MutableCharacterStatsAPI;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;

import java.util.List;
import java.util.Random;

public class NSP_aiswitch_auto_elite extends BaseHullMod {

    private final String nerfkey = "$NSP_aiswitch_auto";
    private final int nerflevel = 6;
    private final Random random = new Random();

    @Override
    public void addPostDescriptionSection(
            TooltipMakerAPI tooltip,
            ShipAPI.HullSize hullSize,
            ShipAPI ship,
            float width,
            boolean isForModSpec
    ) {
        tooltip.addPara("Due to certain liberties taken when integrating the AI core cradle into the new hull the result is less than optimal - it lacks the throughput required to support AI cores of higher sentience. This variant, however, has been upgraded and augmented to allow higher levels of ai core.", 5f);
        tooltip.addPara("Any AI core installed is limited to level " + nerflevel + ", up to level " + (nerflevel + 3) + " if fully integrated.", 5f)
                .setHighlight("level " + nerflevel, "level " + (nerflevel + 1));
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
    public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {
        if (ship == null) return;

        PersonAPI cap = ship.getCaptain();
        if (cap != null && !cap.isPlayer() && !cap.isDefault() && cap.isAICore()
                && !cap.getMemoryWithoutUpdate().getBoolean(nerfkey)
                && !Global.getSector().getImportantPeople().containsPerson(cap)) {

            cap.getMemoryWithoutUpdate().set(nerfkey, true);

            int numAttempts = 0;
            int skillsRemoved = 0;
            int level = cap.getStats().getLevel();
            int skillsToRemove = level - nerflevel;

            List<MutableCharacterStatsAPI.SkillLevelAPI> skillsCopy = cap.getStats().getSkillsCopy();
            if (skillsCopy.isEmpty()) return; // don't crash the game

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

                    // Refresh the skills copy after removal
                    skillsCopy = cap.getStats().getSkillsCopy();
                    if (skillsCopy.isEmpty()) break;
                }
            }
        }
    }

    @Override
    public String getCanNotBeInstalledNowReason(
            ShipAPI ship,
            MarketAPI marketOrNull,
            CampaignUIAPI.CoreUITradeMode mode
    ) {
        return "Must not have a captain assigned to remove.";
    }
}