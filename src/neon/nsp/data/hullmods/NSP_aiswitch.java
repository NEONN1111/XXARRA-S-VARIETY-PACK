package neon.nsp.data.hullmods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.CargoStackAPI;
import com.fs.starfarer.api.campaign.CoreUITabId;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.impl.campaign.ids.Commodities;
import com.fs.starfarer.api.impl.campaign.ids.HullMods;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import com.fs.starfarer.api.loading.WeaponSlotAPI;
import com.fs.starfarer.api.loading.WeaponSpecAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import neon.nsp.data.scripts.util.NSP_ReflectionUtilsT;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import static neon.nsp.data.NSP_reference_sheet.NSP_AISWITCHAUTOMATED;
import static neon.nsp.data.NSP_reference_sheet.NSP_AISWITCHMANUAL;

public class NSP_aiswitch extends BaseHullMod {

    private final String switchTag = "NSP_switched";
    public static final Map<String, String> decoMap = new HashMap<String, String>();
    static {
        decoMap.put("tll_anubis", "tll_anubis_corebridge");
        decoMap.put("tll_peregrine", "tll_peregrine_corebridge");
        decoMap.put("tll_conquest", "tll_conquest_corebridge");
        decoMap.put("tll_dominator", "tll_dominator_corebridge");
        decoMap.put("tll_eagle", "tll_eagle_corebridge");
        decoMap.put("tll_falcon", "tll_falcon_corebridge");
        decoMap.put("tll_onslaught", "tll_onslaught_corebridge");
        decoMap.put("tll_legion", "tll_legion_corebridge");
    }

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
        if(ship.getOriginalOwner()<0){
            //undo fix for weapons put in cargo
            if(
                    Global.getSector()!=null &&
                            Global.getSector().getPlayerFleet()!=null &&
                            Global.getSector().getPlayerFleet().getCargo()!=null &&
                            Global.getSector().getPlayerFleet().getCargo().getStacksCopy()!=null &&
                            !Global.getSector().getPlayerFleet().getCargo().getStacksCopy().isEmpty()
            ){
                for (CargoStackAPI s : Global.getSector().getPlayerFleet().getCargo().getStacksCopy()){
                    if(
                            s.isWeaponStack()
                                    && s.getWeaponSpecIfWeapon().getWeaponId().endsWith("_corebridge")
                    ){
                        Global.getSector().getPlayerFleet().getCargo().removeStack(s);
                    }
                }
            }
        }
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

        if (stats.getEntity() == null) return;

        ShipVariantAPI variant = stats.getVariant();
        if (variant == null) return;

        // Apply decorative weapon only when in automated mode (has NSP_aiswitch_auto)
        applyDecorativeWeaponBasedOnMode(ship, stats, variant);

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
        handleAutomationSwitching(variantStats);
    }

    private void applyDecorativeWeaponBasedOnMode(ShipAPI ship, MutableShipStatsAPI stats, ShipVariantAPI variant) {
        String hullId = stats.getVariant().getHullSpec().getHullId();

        // Only proceed if this hull has a decorative weapon mapping
        if (!decoMap.containsKey(hullId)) return;

        // Check if ship is in automated mode (has NSP_aiswitch_auto)
        boolean isAutomatedMode = variant.hasHullMod(NSP_AISWITCHAUTOMATED);

        // Find decorative slot
        WeaponSlotAPI decorativeSlot = null;
        Iterator weaponiter = ship.getHullSpec().getAllWeaponSlotsCopy().iterator();
        while (weaponiter.hasNext()) {
            WeaponSlotAPI weaponslot = (WeaponSlotAPI) weaponiter.next();
            if (weaponslot.getWeaponType().equals(WeaponAPI.WeaponType.DECORATIVE)) {
                decorativeSlot = weaponslot;
                break;
            }
        }

        if (decorativeSlot == null) return;

        String slotId = decorativeSlot.getId();

        if (isAutomatedMode) {
            // In automated mode: add the corebridge decorative weapon
            String weaponId = decoMap.get(hullId);
            WeaponSpecAPI weaponSpec = Global.getSettings().getWeaponSpec(weaponId);
            if (weaponSpec != null) {
                // Clear the slot first (set to empty string), then add the decorative weapon
                variant.clearSlot(slotId);
                variant.addWeapon(slotId, weaponId);
            }
        } else {
            // In manual mode: clear any weapon in the decorative slot
            // Check if current weapon is a corebridge weapon
            String currentWeaponId = variant.getWeaponId(slotId);
            if (currentWeaponId != null && currentWeaponId.endsWith("_corebridge")) {
                variant.clearSlot(slotId);
            }
        }
    }

    private void handleAutomationSwitching(ShipVariantAPI variantStats) {
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
            }
        }
    }
}