package neon.nsp.data.hullmods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipEngineControllerAPI;
import com.fs.starfarer.api.combat.WeaponAPI.WeaponType;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import org.magiclib.subsystems.MagicSubsystemsManager;

import java.awt.Color;
import java.util.EnumSet;

public class NSP_DynamicPowerOverrides extends BaseHullMod {
    private static final float RANGE_THRESHOLD = 450.0F;
    private static final float RANGE_MULT = 0.25F;

    float ENGINE_BONUS_TIME_MAX = 10.0F;
    float ENGINE_BONUS_MAX = 1.0F;
    float ENGINE_NERF_MAX = 0.5F;

    float WEAPONS_BONUS_TIME_MAX = 10.0F;
    float WEAPONS_BONUS_MAX = 1.0F;
    float WEAPONS_NERF_MAX = 0.5F;

    float MIN_CREW_MOD = 5F;

    public NSP_DynamicPowerOverrides() {
    }

    public void applyEffectsBeforeShipCreation(ShipAPI.HullSize hullSize, MutableShipStatsAPI stats, String id) {
        stats.getZeroFluxSpeedBoost().modifyMult(id, 0.0F);
        stats.getVentRateMult().modifyMult(id, 0.0F);

        stats.getWeaponRangeThreshold().modifyFlat(id, RANGE_THRESHOLD);
        stats.getWeaponRangeMultPastThreshold().modifyMult(id, RANGE_MULT);

        stats.getMinCrewMod().modifyPercent(id, MIN_CREW_MOD);

        //stats.getZeroFluxMinimumFluxLevel().modifyFlat(id, 2f); // set to two, meaning boost is always on
        stats.getVentRateMult().modifyMult(id, 0f);
    }

    @Override
    public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {
        super.applyEffectsAfterShipCreation(ship, id);
        if (ship != null) {
            MagicSubsystemsManager.addSubsystemToShip(ship, new NSP_DynamicPowerOverrides_Subsystem(ship));
        }
    }

    public void addPostDescriptionSection(TooltipMakerAPI tooltip, ShipAPI.HullSize hullSize, ShipAPI ship, float width, boolean isForModSpec) {
        Color hColor = Misc.getHighlightColor();
        Color pColor = Misc.getPositiveHighlightColor();
        Color nColor = Misc.getNegativeHighlightColor();
        Color dColor = Misc.getDarkHighlightColor();
        new Color(110, 110, 110, 255);
        float pad = 10.0F;
        float pad2 = 0.0F;
        float height = 50.0F;
        float padList = 6.0F;
        float padSig = 1.0F;

        tooltip.addSectionHeading("Effects", Alignment.MID, 10.0F);

        tooltip.addPara("Prevents the use of active venting, drastically reduces weapon ranges past %s units and disables the zero-flux engine boost.",
                padList, hColor, "" + RANGE_THRESHOLD);

        tooltip.addPara("%s to skeleton crew required",
                padList, hColor, "+" + MIN_CREW_MOD + "%");

        tooltip.addPara("Adds %s subsystem,",
                padList, hColor, "Dynamic Power Overrides");

        tooltip.addSectionHeading("Dynamic Power Overrides subsystem", Alignment.MID, 10.0F);

        tooltip.addPara("Subsystem actively diverts reactor power to either weapons or engines, depending on current state.", padList);

        tooltip.addPara("When %s is in %s, reactor power diverted to weapons providing up to %s firerate and ship's flux dissipation, but reduces maneuverability and max speed by as much as %s.",
                padList, hColor, "subsystem", "weapon mode", (int)(100.0F + WEAPONS_BONUS_MAX * 100.0F) + "%", (int)(ENGINE_NERF_MAX * 100.0F) + "%");

        tooltip.addPara("When %s is in %s, reactor power diverted to engines providing up to %s maneuverability and max speed, but reduces weapons firerate and ship's flux dissipation by as much as %s.",
                padList, hColor, "subsystem", "engine mode", (int)(100.0F + ENGINE_BONUS_MAX * 100.0F) + "%", (int)(WEAPONS_NERF_MAX * 100.0F) + "%");

        tooltip.addPara("When %s of subsystem is active for more than %s seconds, power gets routed away from main ship system %s",
                padList, hColor, "any mode", "7", "preventing ship system activation");

        tooltip.addPara("When ship %s, short power surge boosts switched to focus.",
                padList, hColor, "switches from one focus to another");
    }
}
