package neon.nsp.data.hullmods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.CargoStackAPI;
import com.fs.starfarer.api.campaign.CoreUITabId;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.ids.Commodities;
import com.fs.starfarer.api.impl.campaign.ids.HullMods;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import com.fs.starfarer.api.loading.WeaponSlotAPI;
import com.fs.starfarer.api.loading.WeaponSpecAPI;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import neon.nsp.data.scripts.util.NSP_ReflectionUtilsT;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.util.*;
import java.util.List;

import static neon.nsp.data.NSP_reference_sheet.NSP_IMPROVISED_AUTO;
import static neon.nsp.data.NSP_reference_sheet.NSP_IMPROVISED_MANUAL;


public class ImprovisedAutomation extends BaseHullMod {
    private static final Set<String> BLOCKED_HULLMODS = new HashSet<>();
    private final String switchTag = "NSP_switched";
    public static final Map<String, String> decoMap = new HashMap<String, String>();


    private static final Set<String> VANILLA_AUTOMATED_REMOVED_SHIPS = new HashSet<>();


    private static final Set<String> PROCESSED_SHIPS = new HashSet<>();

    static {

        BLOCKED_HULLMODS.add("safetyoverrides");
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

    public void addPostDescriptionSection(
            TooltipMakerAPI tooltip,
            ShipAPI.HullSize hullSize,
            ShipAPI ship,
            float width,
            float opad,
            boolean isForModSpec
    ) {
        tooltip.addPara("This ship is equipped with a strange, archaic, and rudimentary form of Automation, born out of neccesity and desperation than any true innovation.", opad);
        tooltip.addSectionHeading("Incompatibilites", Alignment.MID, opad);
        tooltip.addPara("Due to the nature of these modifications, this vessel's systems are incompatible with %s.", opad, Misc.getNegativeHighlightColor(),
                getHullmodName("safetyoverrides"));
    }

    public String getHullmodName(String id) {
        return Global.getSettings().getHullModSpec(id).getDisplayName();
    }

    @Override
    public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {
        for (String tmp : BLOCKED_HULLMODS) {
            if (ship.getVariant().getHullMods().contains(tmp)) {
                ship.getVariant().removeMod(tmp);
                String ERROR = "nsp_incompatible";
                ship.getVariant().addMod(ERROR);
            }
        }
        if (ship.getOriginalOwner() < 0) {

            if (
                    Global.getSector() != null &&
                            Global.getSector().getPlayerFleet() != null &&
                            Global.getSector().getPlayerFleet().getCargo() != null &&
                            Global.getSector().getPlayerFleet().getCargo().getStacksCopy() != null &&
                            !Global.getSector().getPlayerFleet().getCargo().getStacksCopy().isEmpty()
            ) {
                for (CargoStackAPI s : Global.getSector().getPlayerFleet().getCargo().getStacksCopy()) {
                    if (
                            s.isWeaponStack()
                                    && s.getWeaponSpecIfWeapon().getWeaponId().endsWith("_corebridge")
                    ) {
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
            return false;
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


        String shipId = "";
        FleetMemberAPI member = stats.getFleetMember();
        if (member != null && member.getId() != null) {
            shipId = member.getId();
        } else {
            shipId = id;
        }


        applyDecorativeWeaponBasedOnMode(ship, stats, variant);

        if (!this.isBuiltIn(ship) && !this.isSMod(ship)) return;


        if (ship.isFighter() || ship.isDrone() || ship.getParentStation() != null || ship.getParentStation() != null) {
            return;
        }


        if (variant != null && !VANILLA_AUTOMATED_REMOVED_SHIPS.contains(shipId)) {
            boolean removed = false;


            List<String> hullmodsCopy = new ArrayList<>(variant.getHullMods());
            for (String mod : hullmodsCopy) {
                if ("automated".equals(mod)) {
                    variant.getHullMods().remove(mod);
                    removed = true;
                    Global.getLogger(this.getClass()).info("Removed vanilla Automated hullmod from ship: " +
                            (member != null ? member.getShipName() : "unknown"));
                }
            }


            if (variant.getPermaMods() != null) {
                Set<String> permaCopy = new HashSet<>(variant.getPermaMods());
                for (String mod : permaCopy) {
                    if ("automated".equals(mod)) {
                        variant.getPermaMods().remove(mod);
                        removed = true;
                    }
                }
            }

            if (removed) {
                VANILLA_AUTOMATED_REMOVED_SHIPS.add(shipId);
            }
        }

        if ((spec.isBuiltInMod(HullMods.AUTOMATED) || spec.hasTag(Tags.AUTOMATED)) && !PROCESSED_SHIPS.contains(shipId)) {
            ShipHullSpecAPI cloned = null;

            try {

                cloned = (ShipHullSpecAPI) NSP_ReflectionUtilsT.invoke("clone", ship.getHullSpec());
            } catch (Throwable e) {
                Global.getLogger(this.getClass()).warn("Failed to clone hull spec via reflection for: " + ship.getHullSpec().getHullId());
            }


            if (cloned == null) {
                try {
                    String hullId = spec.getHullId();
                    ShipHullSpecAPI baseSpec = Global.getSettings().getHullSpec(hullId);
                    if (baseSpec != null) {
                        cloned = baseSpec;
                        Global.getLogger(this.getClass()).info("Using base hull spec for: " + hullId);
                    }
                } catch (Throwable e) {
                    Global.getLogger(this.getClass()).warn("Failed to get base hull spec for: " + ship.getHullSpec().getHullId());
                }
            }


            if (cloned == null) {
                Global.getLogger(this.getClass()).warn("Could not obtain hull spec for: " + ship.getHullSpec().getHullId() + " - skipping automation conversion");
                return;
            }


            PROCESSED_SHIPS.add(shipId);


            if (cloned.getBuiltInMods() != null) {
                cloned.getBuiltInMods().remove("automated");
            }
            if (cloned.getHints() != null) {
                cloned.getHints().remove(ShipHullSpecAPI.ShipTypeHints.UNBOARDABLE);
            }
            if (cloned.getTags() != null) {
                cloned.getTags().remove(Tags.AUTOMATED);
            }
            cloned.addTag("ImprovisedAutomation");

            if (!cloned.isBuiltInMod(this.spec.getId()) && !cloned.hasTag(Tags.TAG_AUTOMATED_NO_PENALTY)) {
                variant.addMod("nsp_improvised_penalty");
            }

            variant.getHullMods().remove("automated");
            if (variant.getPermaMods() != null) {
                variant.getPermaMods().remove("automated");
            }

            variant.setHullSpecAPI(cloned);
            variant.addTag(switchTag);
        }


        if (ship.getHullSpec().hasTag("ImprovisedAutomation")) {
            ShipAPI.HullSize size = ship.getHullSize();
            if (size != null) {
                float crew;
                switch (size) {
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
            } else {

                stats.getMinCrewMod().modifyFlat(id, 15f);
            }
        }


        if (stats.getVariant() == null || (!this.isBuiltIn(ship) && !this.isSMod(ship))) return;

        ShipVariantAPI variantStats = stats.getVariant();
        handleAutomationSwitching(variantStats);
    }

    private void applyDecorativeWeaponBasedOnMode(ShipAPI ship, MutableShipStatsAPI stats, ShipVariantAPI variant) {

        if (ship.isFighter() || ship.isDrone() || ship.getParentStation() != null || ship.getParentStation() != null) {
            return;
        }


        if (stats.getVariant().getHullSpec() == null) return;

        String hullId = stats.getVariant().getHullSpec().getHullId();
        if (hullId == null) return;


        if (!decoMap.containsKey(hullId)) return;


        boolean isAutomatedMode = variant.hasHullMod(NSP_IMPROVISED_AUTO);


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

            String weaponId = decoMap.get(hullId);
            WeaponSpecAPI weaponSpec = Global.getSettings().getWeaponSpec(weaponId);
            if (weaponSpec != null) {
                variant.clearSlot(slotId);
                variant.addWeapon(slotId, weaponId);
            }
        } else {

            String currentWeaponId = variant.getWeaponId(slotId);
            if (currentWeaponId != null && currentWeaponId.endsWith("_corebridge")) {
                variant.clearSlot(slotId);
            }
        }
    }

    private void handleAutomationSwitching(ShipVariantAPI variantStats) {

        if (variantStats.hasTag(Tags.AUTOMATED)) {
            if (!variantStats.hasHullMod(NSP_IMPROVISED_AUTO)) {
                variantStats.removeTag(Tags.AUTOMATED);
                variantStats.addMod(NSP_IMPROVISED_MANUAL);


                if (variantStats.hasHullMod("nsp_improvised_penalty")) {
                    variantStats.removeMod("nsp_improvised_penalty");
                }

                refreshRefitScreen();
            }
        } else {
            if (!variantStats.hasHullMod(NSP_IMPROVISED_MANUAL)) {
                variantStats.addTag(Tags.AUTOMATED);
                variantStats.addMod(NSP_IMPROVISED_AUTO);


                if (!variantStats.hasHullMod("nsp_improvised_penalty")) {
                    variantStats.addMod("nsp_improvised_penalty");
                }

                refreshRefitScreen();
            }
        }
    }

    private void refreshRefitScreen() {
        if (Global.getSector() != null &&
                Global.getSector().getCampaignUI() != null &&
                Global.getSector().getCampaignUI().getCurrentCoreTab() == CoreUITabId.REFIT) {
            try {
                Robot robot = new Robot();
                robot.keyPress(KeyEvent.VK_R);
                robot.keyRelease(KeyEvent.VK_R);
            } catch (AWTException e) {
     
            }
        }
    }
}