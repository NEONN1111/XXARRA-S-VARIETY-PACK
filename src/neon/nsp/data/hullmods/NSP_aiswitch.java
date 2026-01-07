package neon.nsp.data.hullmods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.CargoStackAPI;
import com.fs.starfarer.api.campaign.CoreUITabId;
import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.impl.campaign.ids.Commodities;
import com.fs.starfarer.api.impl.campaign.ids.HullMods;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import neon.nsp.data.scripts.util.NSP_ReflectionUtilsT;

import java.awt.*;
import java.awt.event.KeyEvent;

import static neon.nsp.data.NSP_reference_sheet.NSP_AISWITCHAUTOMATED;
import static neon.nsp.data.NSP_reference_sheet.NSP_AISWITCHMANUAL;

public class NSP_aiswitch extends BaseHullMod {

    private final String switchTag = "NSP_switched";

    @Override
    public CargoStackAPI getRequiredItem() {
        return Global.getSettings().createCargoStack(CargoAPI.CargoItemType.RESOURCES, Commodities.ALPHA_CORE, null);
    }

    @Override
    public int getDisplayCategoryIndex() {
        return 0;
    }

    @Override
    public int getDisplaySortOrder() {
        return 0;
    }

    @Override
    public void addPostDescriptionSection(
            TooltipMakerAPI tooltip,
            ShipAPI.HullSize hullSize,
            ShipAPI ship,
            float width,
            boolean isForModSpec
    ) {
        tooltip.addPara("Allows for normally automated ships to be piloted with a modest crew complement, and removes their reliance on a commander proficient in handling them.", 5f);
        tooltip.addPara("Can switch between allowing a human captain or an AI core captain.", 5f);
        tooltip.addPara("Can only switch states if the ship doesn't already have a captain.", 5f);

        if (!this.isBuiltIn(ship) && !this.isSMod(ship)) {
            TooltipMakerAPI para = (TooltipMakerAPI) tooltip.addPara("Only functions once built in to the vessel. Does not refund its ordnance post cost after being built in if the ship was originally automated.", 5f);
            ((com.fs.starfarer.api.ui.LabelAPI) para).setHighlightColor(Misc.getNegativeHighlightColor());
        }
    }

    @Override
    public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {
        // Empty implementation
    }

    @Override
    public String getUnapplicableReason(ShipAPI ship) {
        return "Incompatible with this vessel's automation protocol";
    }

    @Override
    public boolean isApplicableToShip(ShipAPI ship) {
        if (ship == null) return false;
        ShipHullSpecAPI spec = ship.getHullSpec();
        if (spec == null) return false;

        if (spec.hasTag(Tags.AUTOMATED) && !spec.isBuiltInMod(HullMods.AUTOMATED)) {
            return false; // causes issues w/ custom auto hullmods
        }

        return super.isApplicableToShip(ship);
    }

    @Override
    public void applyEffectsBeforeShipCreation(
            ShipAPI.HullSize hullSize,
            MutableShipStatsAPI stats,
            String id
    ) {
        ShipAPI ship = null;
        if (stats != null && stats.getEntity() instanceof ShipAPI) {
            ship = (ShipAPI) stats.getEntity();
        }
        if (ship == null) return;

        ShipHullSpecAPI spec = ship.getHullSpec();
        if (spec == null) return;

        ShipVariantAPI variant = stats.getVariant();
        if (variant == null) return;

        if (!this.isBuiltIn(ship) && !this.isSMod(ship)) return;

        if (spec.isBuiltInMod(HullMods.AUTOMATED) || spec.hasTag(Tags.AUTOMATED)) {
            // todo fix issue where this isn't called on game load and auto ships are serialized as their original specs,
            // causing them to lose CR until you open the fleet menu or just do anything that causes them to update
            ShipHullSpecAPI cloned = null;
            try {
                cloned = (ShipHullSpecAPI) NSP_ReflectionUtilsT.invoke("clone", ship.getHullSpec());
            } catch (Throwable e) {
                throw new RuntimeException(e);
            }

            cloned.getBuiltInMods().remove("automated");
            cloned.getHints().remove(ShipHullSpecAPI.ShipTypeHints.UNBOARDABLE);
            cloned.getTags().remove(Tags.AUTOMATED);
            cloned.addTag("NSP_aiswitch");

            if (!cloned.isBuiltInMod(this.spec.getId()) && !cloned.hasTag(Tags.TAG_AUTOMATED_NO_PENALTY)) {
                variant.addMod("NSP_aiswitch_penalty");
            }

            variant.getHullMods().remove("automated");
            variant.setHullSpecAPI(cloned);
            variant.addTag(switchTag);
        }

        if (ship.getHullSpec().hasTag("NSP_aiswitch")) {
            float crew;
            switch (ship.getHullSize()) {
                case CAPITAL_SHIP:
                    crew = 150f;
                    break;
                case CRUISER:
                    crew = 80f;
                    break;
                case DESTROYER:
                    crew = 25f;
                    break;
                default:
                    crew = 15f;
            }
            stats.getMinCrewMod().modifyFlat(id, crew);
        }

        if (stats.getVariant() == null || (!this.isBuiltIn(ship) && !this.isSMod(ship))) return;

        ShipVariantAPI variantStats = stats.getVariant();
        if (variantStats.hasTag(Tags.AUTOMATED)) {
            if (!variantStats.hasHullMod(NSP_AISWITCHAUTOMATED)) {
                variantStats.removeTag(Tags.AUTOMATED);
                variantStats.addMod(NSP_AISWITCHMANUAL);

                if (Global.getSector() != null &&
                        Global.getSector().getCampaignUI() != null &&
                        Global.getSector().getCampaignUI().getCurrentCoreTab() == CoreUITabId.REFIT) {
                    try {
                        Robot robot = new Robot();
                        robot.keyPress(KeyEvent.VK_R);
                        robot.keyRelease(KeyEvent.VK_R);
                    } catch (AWTException e) {
                        // Handle exception if needed
                    }
                }
                return;
            }
        } else {
            if (!variantStats.hasHullMod(NSP_AISWITCHMANUAL)) {
                variantStats.addTag(Tags.AUTOMATED);
                variantStats.addMod(NSP_AISWITCHAUTOMATED);

                if (Global.getSector() != null &&
                        Global.getSector().getCampaignUI() != null &&
                        Global.getSector().getCampaignUI().getCurrentCoreTab() == CoreUITabId.REFIT) {
                    try {
                        Robot robot = new Robot();
                        robot.keyPress(KeyEvent.VK_R);
                        robot.keyRelease(KeyEvent.VK_R);
                    } catch (AWTException e) {
                        // Handle exception if needed
                    }
                }
                return;
            }
        }
    }
}