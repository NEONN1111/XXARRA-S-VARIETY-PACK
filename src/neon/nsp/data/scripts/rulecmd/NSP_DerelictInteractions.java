package neon.nsp.data.scripts.rulecmd;

import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.GameState;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.Script;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.campaign.BattleAPI.BattleSide;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.combat.WeaponAPI.WeaponType;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.FleetEncounterContext;
import com.fs.starfarer.api.impl.campaign.FleetInteractionDialogPluginImpl;
import com.fs.starfarer.api.impl.campaign.ids.*;
import com.fs.starfarer.api.impl.campaign.procgen.themes.*;
import com.fs.starfarer.api.impl.campaign.procgen.themes.BaseThemeGenerator.EntityLocation;
import com.fs.starfarer.api.impl.campaign.rulecmd.AddRemoveCommodity;
import com.fs.starfarer.api.impl.campaign.rulecmd.BaseCommandPlugin;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.LabelAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.IntervalUtil;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.api.util.WeightedRandomPicker;
import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.input.Keyboard;
import org.magiclib.util.MagicCampaign;
import java.awt.Color;
import java.util.*;

public class NSP_DerelictInteractions extends BaseCommandPlugin {

    private static final float SUPPLIES_PER_NEXUS = 800f;
    private static final float METALS_PER_NEXUS = 1500f;
    private static final float RARE_METALS_PER_NEXUS = 200f;
    private static final float WEAPONS_THRESHOLD = 0.40f;
    private static final float FIGHTERS_THRESHOLD = 0.50f;
    private static final float AICORES_THRESHOLD = 0.75f;

    public static MarketAPI targetmarket = null;
    public static List<Integer> rewardlist = new ArrayList<>();
    public static PersonAPI coreguy = null;
    public static FleetMemberAPI ship = null;
    public static Float height = null;
    public static Float width = null;
    public static String factionspec = "";

    @Override
    public boolean execute(String ruleId, InteractionDialogAPI dialog, List<Misc.Token> params, Map<String, MemoryAPI> memoryMap) {
        int type = params.get(0).getInt(memoryMap);
        SectorAPI sector = Global.getSector();
        FactionAPI remmy = sector.getFaction(Factions.DERELICT);

        switch (type) {
            case 0:
                dialog.getTextPanel().clear();
                dialog.getTextPanel().addPara("The Explorarium Mothership welcomes you into its graces, allowing you to make use of its services.");
                dialog.getTextPanel().addPara("Open a comm link with the Mothership to begin.");
                dialog.getTextPanel().setFontSmallInsignia();
                dialog.getTextPanel().addPara("You may repair your ships at no cost at any Mothership, and make use of their services to maintain your fleet.");
                dialog.getTextPanel().addPara("Each Mothership has its own cargo for offer, and are prepared to produce Explorarium hulls and weapons instantaneously - provided you have the credits to authorize the production, that is.");
                dialog.getTextPanel().setFontInsignia();
                break;

            case 1:
                getCargoPicker(dialog);
                break;

            case 2:
                // Note: ds_nexusCustomProduction needs to be imported or defined
                // dialog.showCustomProductionPicker(new ds_nexusCustomProduction(dialog));
                break;

            case 3:
                sector.addTransientScript(new nexusStorageScript((SectorEntityToken)dialog.getInteractionTarget(), dialog));
                break;

            case 4:
                for (FleetMemberAPI member : sector.getPlayerFleet().getFleetData().getMembersListCopy()) {
                    member.getRepairTracker().setCR(member.getRepairTracker().getMaxCR());
                    member.getStatus().setHullFraction(1f);
                }
                break;

            case 5:
                dialog.getOptionPanel().clearOptions();
                dialog.setXOffset(0f);
                if (height != null) {
                    dialog.setTextHeight(height);
                    dialog.setTextWidth(width);
                } else {
                    height = dialog.getTextHeight();
                    width = dialog.getTextWidth();
                }
                dialog.getTextPanel().updateSize();
                dialog.getTextPanel().clear();
                dialog.getTextPanel().setFontVictor();
                dialog.getTextPanel().addPara("affirm // detected : Explorarium property transponder from [target_fleet] // waiting waiting waiting...");

                if (sector.getPlayerFleet().getFleetPoints() > 300) {
                    dialog.getTextPanel().addPara("receipt of FLEET no. 417 confirmed. \"Welcome back, esteemed user!\"");
                } else {
                    dialog.getTextPanel().addPara("receipt of SCOUT no. 417 confirmed. \"Welcome back, esteemed user!\"");
                }

                for (FleetMemberAPI member : sector.getPlayerFleet().getFleetData().getMembersListCopy()) {
                    member.getRepairTracker().setCR(member.getRepairTracker().getMaxCR());
                    member.getStatus().setHullFraction(1f);
                }

                coreguy = dialog.getInteractionTarget().getActivePerson();
                factionspec = "";
                dialog.getTextPanel().addPara("CASE permit [offload implements] to [target_fleet] // verification required");
                dialog.getTextPanel().addPara("CASE produce [replicate] implements // verification required");
                dialog.getTextPanel().addPara("CASE refit [test/analyze/rearm] offered for [target_fleet] //");
                dialog.getTextPanel().addPara("CASE leave [cutCommLink] // \"We wish you a very nice day.\"");
                dialog.getTextPanel().addPara("CASE repair [restore] expeditiously available");
                dialog.getTextPanel().setFontInsignia();

                dialog.getOptionPanel().addOption("Evaluate available cargo", "ds_nexusCargoPicker");
                dialog.getOptionPanel().setTooltip("ds_nexusCargoPicker", "Open a dialog to purchase supplies, AI cores, and occasionally other rare items. Every Mothership has its own stock.");
                dialog.getOptionPanel().addOption("Request immediate production", "ds_nexusProductionPicker");
                dialog.getOptionPanel().setTooltip("ds_nexusProductionPicker", "Instantly produce Explorarium hulls and weapons to be delivered to your fleet.");
                dialog.getOptionPanel().addOption("Access the storage network", "ds_nexusStorage");
                dialog.getOptionPanel().setTooltip("ds_nexusStorage", "Allows you to refit your ships. Counts as a spaceport for hullmods that require a dock.");
                dialog.getOptionPanel().addOption("Initiate fleet repairs", "ds_nexusRepair");
                dialog.getOptionPanel().setTooltip("ds_nexusRepair", "A free automated repair procedure. Restores all ships to full CR and hull integrity at no cost.");
                dialog.getOptionPanel().addOption("Manage automated hulls", "ds_nexusDeconstructMain");
                dialog.getOptionPanel().setTooltip("ds_nexusDeconstruct", "Destroy an automated hull to add it to the Explorarium's known hulls.");
                dialog.getOptionPanel().addOption("Consider building a new Mothership", "ds_nexusConstructMenu");
                dialog.getOptionPanel().setTooltip("ds_nexusConstructMenu", "Construct a new Mothership");

                // Note: ds_nexusRaidIntel needs to be imported or defined
                /*
                if (sector.getIntelManager().hasIntelOfClass(ds_nexusRaidIntel.class) &&
                    sector.getMemoryWithoutUpdate().getInt("$ds_nexusParty") == 1) {
                    dialog.getOptionPanel().addOption("Raid rewards", "ds_nexusPartyTimeReward");
                } else if (sector.getMemoryWithoutUpdate().getBoolean("$ds_nexusPartyTimeout")) {
                    dialog.getOptionPanel().addOption("Raid cooldown", "ds_nexusPartyCoolDown");
                    dialog.getOptionPanel().setEnabled("ds_nexusPartyCoolDown", false);
                    float expire = sector.getMemoryWithoutUpdate().getExpire("$ds_nexusPartyTimeout");
                    if (expire > 0f) {
                        dialog.getOptionPanel().setTooltip("ds_nexusPartyCoolDown",
                            "You may throw another party in " + Math.round(expire) + " days.");
                    }
                } else if (!sector.getIntelManager().hasIntelOfClass(ds_nexusRaidIntel.class) &&
                           !sector.getMemoryWithoutUpdate().getBoolean("$ds_nexusPartyTimeout")) {
                    dialog.getOptionPanel().addOption("Raid requests", "ds_nexusPartyTimeShow");
                }
                */

                dialog.getOptionPanel().addOption("Leave", "defaultLeave");
                dialog.getOptionPanel().setShortcut("ds_nexusRepair", Keyboard.KEY_A, false, false, false, false);
                dialog.getVisualPanel().showImageVisual(dialog.getInteractionTarget().getCustomInteractionDialogImageVisual());
                break;

            case 6:
                dialog.getTextPanel().clear();
                dialog.getTextPanel().addPara("You're currently accessing the Explorarium Mothership's global data storage network.");
                dialog.getTextPanel().addPara("Any items stored in here will be available to retrieve from any other Explorarium Mothership in the sector.");
                dialog.getTextPanel().addPara("Exit the storage network to return to using the Explorarium Mothership.");
                break;

            case 7:
                dialog.getOptionPanel().clearOptions();
                dialog.getOptionPanel().addOption("Manage commodity manifest", "marketOpenCargo");
                dialog.getOptionPanel().setShortcut("marketOpenCargo", Keyboard.KEY_I, false, false, false, false);
                dialog.getOptionPanel().addOption("Upload or download ships", "marketOpenFleet");
                dialog.getOptionPanel().setShortcut("marketOpenFleet", Keyboard.KEY_F, false, false, false, false);
                dialog.getOptionPanel().addOption("Use the Mothership to refit your ships", "marketOpenRefit");
                dialog.getOptionPanel().setShortcut("marketOpenRefit", Keyboard.KEY_R, false, false, false, false);
                dialog.getOptionPanel().addOption("Log off", "defaultLeave");
                dialog.getOptionPanel().setShortcut("defaultLeave", Keyboard.KEY_ESCAPE, false, false, false, true);
                dialog.makeOptionOpenCore("marketOpenRefit", CoreUITabId.REFIT, CampaignUIAPI.CoreUITradeMode.OPEN);
                break;

            case 8:
                dialog.setXOffset(0f);
                if (height != null) {
                    dialog.setTextHeight(height);
                    dialog.setTextWidth(width);
                } else {
                    height = dialog.getTextHeight();
                    width = dialog.getTextWidth();
                }
                dialog.getTextPanel().updateSize();
                showPickerDialog(dialog);
                break;

            case 9:
                boolean hascores = false;
                dialog.getTextPanel().setFontSmallInsignia();
                dialog.getOptionPanel().clearOptions();
                dialog.getTextPanel().addPara("The " + ship.getShipName() + " has been deconstructed. Moments later, a sonorous chime emits from the Mothership.");

                remmy.addKnownShip(ship.getHullSpec().getRestoredToHullId(), false);
                remmy.getAlwaysKnownShips().add(ship.getHullSpec().getRestoredToHullId());
                remmy.addUseWhenImportingShip(ship.getHullSpec().getRestoredToHullId());

                if (!ship.getHullSpec().getBaseHullId().equals("rat_genesis")) {
                    List<String> varlist = Global.getSettings().getAllVariantIds();
                    List<String> shipvarlist = new ArrayList<>();

                    for (String variantId : varlist) {
                        if (Global.getSettings().getVariant(variantId).getHullSpec().getBaseHullId().equals(ship.getHullSpec().getBaseHullId())) {
                            shipvarlist.add(variantId);
                        }
                    }

                    String role = "combatSmall";
                    switch (Global.getSettings().getHullSpec(ship.getHullSpec().getHullId()).getHullSize()) {
                        case CAPITAL_SHIP:
                            role = "combatCapital";
                            break;
                        case CRUISER:
                            role = "combatLarge";
                            break;
                        case DESTROYER:
                            role = "combatMedium";
                            break;
                        default:
                            role = "combatSmall";
                    }

                    for (String variantId : shipvarlist) {
                        Global.getSettings().addDefaultEntryForRole(role, variantId, 0f);
                        Global.getSettings().addEntryForRole(Factions.DERELICT, role, variantId, 1f);
                    }
                }

                sector.getPlayerFleet().removeFleetMemberWithDestructionFlash(ship);
                Global.getSoundPlayer().playUISound("ui_industry_install_any_item", 1f, 1f);

                List<String> banlist = Arrays.asList("sotf_dustkeepers_burnouts", "rat_abyssals", "ai_all");

                for (FactionAPI faction : sector.getAllFactions()) {
                    if (!banlist.contains(faction.getId()) &&
                            faction.knowsShip(ship.getHullSpec().getHullId()) &&
                            !faction.getFactionSpec().getId().equals("derelict")) {
                        factionspec = faction.getId();
                        if (factionspec.equals("rat_abyssals_deep") ||
                                factionspec.equals("tahlan_legiodaemons") ||
                                factionspec.equals("vestige")) {
                            hascores = true;
                        }
                    }
                }

                if (ship.getHullSpec().getBaseHullId().startsWith("istl") ||
                        ship.getHullSpec().getBaseHullId().startsWith("bbplus")) {
                    factionspec = "blade_breakers";
                }

                dialog.getTextPanel().addPara("Explorarium fleets may now use the " +
                        ship.getHullSpec().getHullNameWithDashClass() + " " +
                        ship.getHullSpec().getHullSize().name() + ".").setColor(Misc.getHighlightColor());

                doFactionCheck(factionspec, hascores, dialog);
                dialog.getTextPanel().setFontInsignia();
                dialog.getOptionPanel().addOption("Continue", "ds_nexusDeconstructMain");
                break;

            case 11:
                dialog.getInteractionTarget().setActivePerson(coreguy);
                break;

            case 12:
                showRecountInfo(dialog);
                break;

            case 13:
                dialog.getTextPanel().clear();
                dialog.getTextPanel().addPara("The Explorarium are ever-hungry to expand their knowledge, especially towards those whose hulls resemble their own.");
                dialog.setXOffset(0f);
                if (height != null) {
                    dialog.setTextHeight(height);
                    dialog.setTextWidth(width);
                } else {
                    height = dialog.getTextHeight();
                    width = dialog.getTextWidth();
                }
                dialog.getTextPanel().updateSize();
                break;

            case 14:
                float expire = sector.getMemoryWithoutUpdate().getExpire("$ds_nexusBuildTimeout");
                if (expire > 0f) {
                    dialog.getTextPanel().addPara("It will take some time to prepare to construct another Mothership.");
                    dialog.getTextPanel().addPara("You may construct another in " + Math.round(expire) + " days.");
                }

                // FIXED: Removed void dereferencing - these methods return void
                dialog.getTextPanel().addCostPanel("Construction Costs",
                        Commodities.SUPPLIES, Math.round(SUPPLIES_PER_NEXUS), true,
                        Commodities.METALS, Math.round(METALS_PER_NEXUS), true,
                        Commodities.RARE_METALS, Math.round(RARE_METALS_PER_NEXUS), true);

                CargoAPI pcargo = sector.getPlayerFleet().getCargo();
                boolean supplies = pcargo.getCommodityQuantity(Commodities.SUPPLIES) >= SUPPLIES_PER_NEXUS;
                boolean metals = pcargo.getCommodityQuantity(Commodities.METALS) >= METALS_PER_NEXUS;
                boolean raremetals = pcargo.getCommodityQuantity(Commodities.RARE_METALS) >= RARE_METALS_PER_NEXUS;

                if (sector.getMemoryWithoutUpdate().getBoolean("$ds_nexusBuildTimeout") ||
                        !supplies || !metals || !raremetals) {
                    dialog.getOptionPanel().setEnabled("ds_nexusConstruct", false);
                    dialog.getOptionPanel().setTooltip("ds_nexusConstruct", "Can't build this yet.");
                }
                break;

            case 15:
                ShowNexusBuildPicker(dialog);
                break;

            case 16:
                getShowRaidTarget(dialog, targetmarket, rewardlist);
                break;

            case 17:
                doSetup(dialog);
                break;

            case 18:
                getRaidReward(dialog);
                break;
        }

        return true;
    }

    // Helper methods
    private void ShowNexusBuildPicker(InteractionDialogAPI dialog) {
        // Get all derelict motherships in the sector
        List<SectorEntityToken> nexusList = new ArrayList<>();
        for (SectorEntityToken entity : Global.getSector().getEntitiesWithTag(Tags.THEME_DERELICT_MOTHERSHIP)) {
            nexusList.add(entity);
        }

        List<StarSystemAPI> bannedSystemsList = new ArrayList<>();

        for (SectorEntityToken entity : nexusList) {
            if (entity.getStarSystem() != null) {
                bannedSystemsList.add(entity.getStarSystem());
            }
        }

        List<StarSystemAPI> validSystemList = new ArrayList<>();
        for (StarSystemAPI system : Global.getSector().getStarSystems()) {
            if (system.isEnteredByPlayer() && system.isProcgen() &&
                    !system.isDeepSpace() && !system.hasTag(Tags.THEME_HIDDEN) &&
                    !bannedSystemsList.contains(system)) {
                validSystemList.add(system);
            }
        }

        List<SectorEntityToken> centers = new ArrayList<>();
        for (StarSystemAPI system : validSystemList) {
            centers.add(system.getCenter());
        }

        dialog.showCampaignEntityPicker("Title", "Selected", "Ok",
                Global.getSector().getFaction(Factions.DERELICT), centers,
                new BaseCampaignEntityPickerListener() {
                    @Override
                    public void cancelledEntityPicking() {
                        dialog.getTextPanel().addPara("cancelled");
                    }

                    @Override
                    public float getFuelRangeMult() {
                        return 0f;
                    }

                    @Override
                    public void pickedEntity(SectorEntityToken entity) {
                        CargoAPI cargo = Global.getSector().getPlayerFleet().getCargo();
                        cargo.removeCommodity(Commodities.SUPPLIES, SUPPLIES_PER_NEXUS);
                        cargo.removeCommodity(Commodities.METALS, METALS_PER_NEXUS);
                        cargo.removeCommodity(Commodities.RARE_METALS, RARE_METALS_PER_NEXUS);

                        AddRemoveCommodity.addCommodityLossText(Commodities.SUPPLIES,
                                Math.round(SUPPLIES_PER_NEXUS), dialog.getTextPanel());
                        AddRemoveCommodity.addCommodityLossText(Commodities.METALS,
                                Math.round(METALS_PER_NEXUS), dialog.getTextPanel());
                        AddRemoveCommodity.addCommodityLossText(Commodities.RARE_METALS,
                                Math.round(RARE_METALS_PER_NEXUS), dialog.getTextPanel());

                        StarSystemAPI system = entity.getStarSystem();
                        EntityLocation loc = BaseThemeGenerator.pickCommonLocation(
                                Misc.random, system, 200f, true, null);
                        SectorEntityToken token = system.createToken(
                                MathUtils.getRandomPointOnCircumference(entity.getLocation(), 6000f));

                        Global.getSector().getMemoryWithoutUpdate().set("$ds_nexusBuildTimeout", true, 90f);
                        dialog.getOptionPanel().setEnabled("ds_nexusConstruct", false);

                        CampaignFleetAPI constructorFleet = MagicCampaign.createFleetBuilder()
                                .setFleetFaction(Factions.DERELICT)
                                .setFleetName("Construction Fleet")
                                .setFleetType(FleetTypes.SUPPLY_FLEET)
                                .setAssignment(FleetAssignment.GO_TO_LOCATION)
                                .setAssignmentTarget(token)
                                .setSpawnLocation(dialog.getInteractionTarget())
                                .setIsImportant(true)
                                .setMinFP(200)
                                .create();

                        constructorFleet.removeFirstAssignment();
                        constructorFleet.addAssignment(FleetAssignment.GO_TO_LOCATION, token, 999f);
                        constructorFleet.addAssignment(FleetAssignment.ORBIT_PASSIVE, token, 30f,
                                "Constructing Mothership", new NSP_MothershipBuildScript(constructorFleet, loc));

                        dialog.getTextPanel().addPara("A fleet has been dispatched to " + system.getName() + ".")
                                .setHighlight(system.getName());
                        dialog.getTextPanel().addPara("Once it arrives, it will take 30 days to construct the Mothership.")
                                .setHighlight("30 days");
                    }

                    @Override
                    public boolean canConfirmSelection(SectorEntityToken entity) {
                        return entity != null;
                    }

                    @Override
                    public void createInfoText(TooltipMakerAPI info, SectorEntityToken entity) {
                        if (entity == null) return;
                        info.addPara("Selected system: " + entity.getStarSystem().getName(), 5f);
                        if (entity.getConstellation() != null) {
                            info.addPara(entity.getConstellation().getName(), 5f);
                        }
                        float ly = Misc.getDistanceToPlayerLY(entity);
                        LabelAPI lyPara = info.addPara(ly + " light years away from your location.", 5f);
                        lyPara.setHighlight(ly + "");
                    }

                    @Override
                    public String getMenuItemNameOverrideFor(SectorEntityToken entity) {
                        return entity != null ? entity.getStarSystem().getName() : "Location";
                    }
                });
    }

    private void getCargoPicker(InteractionDialogAPI dialog) {
        CargoPickerListener listener = new CargoPickerListener() {
            @Override
            public void pickedCargo(CargoAPI cargo) {
                cargo.sort();
                float cost = getCost(cargo);
                float credits = Global.getSector().getPlayerFleet().getCargo().getCredits().get();

                if (cost > 0 && cost < credits) {
                    Global.getSector().getPlayerFleet().getCargo().getCredits().subtract(cost);
                    dialog.getTextPanel().setFontSmallInsignia();

                    for (CargoStackAPI stack : cargo.getStacksCopy()) {
                        Global.getSector().getPlayerFleet().getCargo().addItems(
                                stack.getType(), stack.getData(), stack.getSize());
                        AddRemoveCommodity.addStackGainText(stack, dialog.getTextPanel(), false);
                        dialog.getInteractionTarget().getCargo().removeItems(
                                stack.getType(), stack.getData(), stack.getSize());
                    }

                    AddRemoveCommodity.addCreditsLossText(Math.round(cost), dialog.getTextPanel());
                }
            }

            @Override
            public void cancelledCargoSelection() {}

            @Override
            public void recreateTextPanel(TooltipMakerAPI panel, CargoAPI cargo,
                                          CargoStackAPI pickedUp, boolean pickedUpFromSource,
                                          CargoAPI combined) {
                float cost = getCost(combined);
                float pad = 3f;
                float bigpad = 10f;
                Color highlight = Misc.getHighlightColor();
                Color highlightspooky = Misc.getNegativeHighlightColor();
                String hegsprite = Global.getSettings().getFactionSpec(Factions.DERELICT).getCrest();

                panel.addImage(hegsprite, 310f, pad);
                float currentCredits = Global.getSector().getPlayerFleet().getCargo().getCredits().get();

                // FIXED: Don't chain setHighlight() and setHighlightColor()
                LabelAPI creditsPara = panel.addPara("You currently have " + Math.round(currentCredits) + " credits.", bigpad);
                creditsPara.setHighlight(Math.round(currentCredits) + "");
                creditsPara.setHighlightColor(highlight);

                // FIXED: Don't chain setHighlight() and setHighlightColor()
                LabelAPI costPara = panel.addPara("The cost of your selection is " + Math.round(cost) + " credits.", pad);
                costPara.setHighlight(Math.round(cost) + "");
                costPara.setHighlightColor(highlightspooky);

                if (cost > currentCredits) {
                    panel.addPara("You don't have enough credits to authorize this transaction.", pad)
                            .setColor(highlightspooky);
                }
            }
        };

        float credits = Global.getSector().getPlayerFleet().getCargo().getCredits().get();
        CargoAPI cargo = Global.getFactory().createCargo(false);
        cargo.addAll(dialog.getInteractionTarget().getCargo());
        cargo.sort();

        dialog.showCargoPickerDialog("Mothership Supply", "Requisition", "Cancel",
                true, 310f, cargo, listener);
    }

    private float getCost(CargoAPI cargo) {
        float cost = 0f;

        for (CargoStackAPI item : cargo.getStacksCopy()) {
            if (item.isSpecialStack()) {
                // Handle special items if needed
            } else if (item.isCommodityStack()) {
                switch (item.getCommodityId()) {
                    case Commodities.OMEGA_CORE:
                    case Commodities.ALPHA_CORE:
                    case Commodities.BETA_CORE:
                    case Commodities.GAMMA_CORE:
                        cost += Math.round(item.getBaseValuePerUnit() * 3f * item.getSize());
                        break;
                    case Commodities.FUEL:
                        cost += Math.round(item.getBaseValuePerUnit() * 0.5f * item.getSize());
                        break;
                    case Commodities.SUPPLIES:
                        cost += Math.round(item.getBaseValuePerUnit() * 0.5f * item.getSize());
                        break;
                    default:
                        if (Global.getSettings().getCommoditySpec(item.getCommodityId())
                                .getDemandClass().equals("ai_cores")) {
                            cost += Math.round(item.getBaseValuePerUnit() * 3f * item.getSize());
                        }
                }
            } else if (item.isWeaponStack()) {
                switch (item.getWeaponSpecIfWeapon().getSize()) {
                    case LARGE:
                        cost += Math.round(item.getBaseValuePerUnit() * 1.5f * item.getSize());
                        break;
                    case MEDIUM:
                        cost += Math.round(item.getBaseValuePerUnit() * 1.3f * item.getSize());
                        break;
                    default:
                        cost += Math.round(item.getBaseValuePerUnit() * 1f * item.getSize());
                }
            }
        }

        return cost;
    }

    private void showPickerDialog(InteractionDialogAPI dialog) {
        List<FleetMemberAPI> validShips = getValidShips();

        if (validShips.isEmpty()) {
            dialog.getTextPanel().addPara("None of the automated ships in your fleet are unknown to the Explorarium.");
            dialog.getTextPanel().addPara("Consider returning once you have one.");
            return;
        }

        FleetMemberPickerListener listener = new FleetMemberPickerListener() {
            @Override
            public void pickedFleetMembers(List<FleetMemberAPI> members) {
                if (members.isEmpty()) return;

                for (FleetMemberAPI member : members) {
                    dialog.getOptionPanel().clearOptions();
                    ship = member;

                    for (FactionAPI faction : Global.getSector().getAllFactions()) {
                        if (faction.knowsShip(ship.getHullSpec().getBaseHullId())) {
                            factionspec = faction.getId();
                        }
                    }

                    LabelAPI shipPara = dialog.getTextPanel().addPara(member.getShipName() + ", a " +
                            member.getHullSpec().getHullNameWithDashClass() + " is selected.");
                    shipPara.setHighlight(member.getShipName());
                    showFactionInfo(factionspec, dialog);

                    LabelAPI destroyPara = dialog.getTextPanel().addPara("Proceeding will destroy the ship and add it to the Explorarium's known ships.");
                    destroyPara.setHighlight("destroy", "add");

                    dialog.getTextPanel().addPara("This action cannot be undone.")
                            .setColor(Color.RED);
                    dialog.getTextPanel().addPara("Would you like to proceed?");
                    dialog.getVisualPanel().showFleetMemberInfo(member);
                    dialog.getOptionPanel().addOption("Proceed with deconstruction", "ds_nexusDeconstructProceed");
                    dialog.getOptionPanel().addOption("Go back", "ds_nexusDeconstructMain");
                }
            }

            @Override
            public void cancelledFleetMemberPicking() {
                return;
            }
        };

        dialog.showFleetMemberPickerDialog("Re-origination Protocol", "Proceed", "Cancel",
                4, 4, 160f, true, false, validShips, listener);
    }

    private List<FleetMemberAPI> getValidShips() {
        FactionAPI remmies = Global.getSector().getFaction(Factions.DERELICT);
        List<FleetMemberAPI> validShips = new ArrayList<>();

        for (FleetMemberAPI playerFM : Global.getSector().getPlayerFleet().getMembersWithFightersCopy()) {
            if (playerFM.getVariant().hasHullMod("automated") &&
                    playerFM.getHullSpec().hasTag(Tags.AUTOMATED_RECOVERABLE) &&
                    !remmies.knowsShip(playerFM.getHullSpec().getBaseHullId()) &&
                    !playerFM.isFighterWing()) {
                validShips.add(playerFM);
            }
        }

        return validShips;
    }

    private void doFactionCheck(String spec, boolean factionHasCores, InteractionDialogAPI dialog) {
        if (spec.isEmpty()) return;

        MemoryAPI mem = Global.getSector().getMemoryWithoutUpdate();
        FactionAPI remmy = Global.getSector().getFaction(Factions.DERELICT);

        if (!Global.getSettings().getFactionSpec(spec).getKnownShips().contains(ship.getHullSpec().getBaseHullId())) {
            return;
        }

        List<String> facships = new ArrayList<>();
        for (String shipId : Global.getSettings().getFactionSpec(spec).getKnownShips()) {
            if (Global.getSettings().getHullSpec(shipId).hasTag(Tags.AUTOMATED_RECOVERABLE)) {
                facships.add(shipId);
            }
        }

        List<String> remmyships = new ArrayList<>();
        for (String shipId : remmy.getKnownShips()) {
            if (Global.getSettings().getFactionSpec(spec).getKnownShips().contains(shipId)) {
                remmyships.add(shipId);
            }
        }

        List<String> facweaps = new ArrayList<>();
        for (String weaponId : Global.getSettings().getFactionSpec(spec).getKnownWeapons()) {
            com.fs.starfarer.api.loading.WeaponSpecAPI weaponSpec = Global.getSettings().getWeaponSpec(weaponId);
            if (!weaponSpec.getAIHints().contains(WeaponAPI.AIHints.SYSTEM) &&
                    weaponSpec.getType() != WeaponType.DECORATIVE) {
                facweaps.add(weaponId);
            }
        }

        List<String> facwings = new ArrayList<>();
        for (String wingId : Global.getSettings().getFactionSpec(spec).getKnownFighters()) {
            if (Global.getSettings().getFighterWingSpec(wingId).hasTag(Tags.AUTOMATED_FIGHTER)) {
                facwings.add(wingId);
            }
        }

        showFactionInfo(spec, dialog);

        float progress = (float) remmyships.size() / facships.size();

        if (progress >= 0.5f && !mem.contains("$ds_" + spec + "weapons")) {
            for (String weaponId : facweaps) {
                remmy.addKnownWeapon(weaponId, false);
            }
            Global.getSoundPlayer().playUISound("ui_char_spent_story_point", 1f, 1f);
            LabelAPI progressPara = dialog.getTextPanel().addPara("The Explorarium now know 50% or more of this faction's automated hulls.");
            progressPara.setHighlight("50%");

            LabelAPI analysisPara = dialog.getTextPanel().addPara("Iterative analysis of their systems now allows the Explorarium to use all of this faction's known weapons.");
            analysisPara.setHighlight("known weapons.");

            dialog.getTextPanel().addPara("These will also be available through production orders.");
            dialog.getTextPanel().setFontSmallInsignia();
            dialog.getTextPanel().addPara("Fighter blueprints will be unlocked at 75%.")
                    .setColor(Misc.getHighlightColor());
            mem.set("$ds_" + spec + "weapons", true);
        }

        if (progress >= 0.75f && !mem.contains("$ds_" + spec + "wings")) {
            for (String wingId : facwings) {
                remmy.addKnownFighter(wingId, false);
            }
            Global.getSoundPlayer().playUISound("ui_char_spent_story_point", 1f, 1f);
            LabelAPI progressPara = dialog.getTextPanel().addPara("The Explorarium now know 75% or more of this faction's automated hulls.");
            progressPara.setHighlight("75%");

            LabelAPI analysisPara = dialog.getTextPanel().addPara("Comprehensive analysis of their systems now allows the Explorarium to use all of this faction's known fighters.");
            analysisPara.setHighlight("known fighters.");

            dialog.getTextPanel().addPara("These will also be available through production orders.");
            dialog.getTextPanel().setFontSmallInsignia();

            if (factionHasCores) {
                dialog.getTextPanel().addPara("Explorarium Motherships will offer this faction's AI cores at 90%.")
                        .setColor(Misc.getHighlightColor());
            }
            mem.set("$ds_" + spec + "wings", true);
        }

        if (progress >= 0.90f && !mem.contains("$ds_" + spec + "cores") && factionHasCores) {
            Global.getSoundPlayer().playUISound("ui_char_spent_story_point", 1f, 1f);
            LabelAPI progressPara = dialog.getTextPanel().addPara("The Explorarium now know 90% or more of this faction's automated hulls.");
            progressPara.setHighlight("90%");

            LabelAPI analysisPara = dialog.getTextPanel().addPara("Thorough reconstruction of their behaviors now allows any Mothership to offer some of the faction's AI cores.");
            analysisPara.setHighlight("AI cores.");

            dialog.getTextPanel().addPara("These are available through the supply cargo picker.");
            dialog.getTextPanel().setFontSmallInsignia();
            mem.set("$ds_" + spec + "cores", true);

            // Get all derelict motherships
            List<SectorEntityToken> nexii = new ArrayList<>();
            for (SectorEntityToken entity : Global.getSector().getEntitiesWithTag(Tags.THEME_DERELICT_MOTHERSHIP)) {
                nexii.add(entity);
            }

            switch (spec) {
                case "rat_abyssals_deep":
                    for (SectorEntityToken nexus : nexii) {
                        nexus.getCargo().addCommodity("rat_chronos_core",
                                MathUtils.getRandomNumberInRange(5f, 7f));
                        nexus.getCargo().addCommodity("rat_cosmos_core",
                                MathUtils.getRandomNumberInRange(5f, 7f));
                        nexus.getCargo().addCommodity("rat_seraph_core",
                                MathUtils.getRandomNumberInRange(3f, 5f));
                    }
                    break;
                case "tahlan_legiodaemons":
                    for (SectorEntityToken nexus : nexii) {
                        nexus.getCargo().addCommodity("tahlan_daemoncore",
                                MathUtils.getRandomNumberInRange(6f, 9f));
                        nexus.getCargo().addCommodity("tahlan_archdaemoncore",
                                MathUtils.getRandomNumberInRange(3f, 5f));
                    }
                    break;
                case "vestige":
                    for (SectorEntityToken nexus : nexii) {
                        nexus.getCargo().addCommodity("vestige_core",
                                MathUtils.getRandomNumberInRange(5f, 8f));
                        nexus.getCargo().addCommodity("volantian_core",
                                MathUtils.getRandomNumberInRange(5f, 8f));
                    }
                    break;
            }
        }
    }

    private void showFactionInfo(String spec, InteractionDialogAPI dialog) {
        if (spec.isEmpty()) return;

        FactionAPI remmy = Global.getSector().getFaction(Factions.DERELICT);
        FactionAPI faction = Global.getSector().getFaction(spec);

        if (faction == null) return;

        // Get all automated ships known by this faction
        List<String> facships = new ArrayList<>();
        for (String shipId : faction.getKnownShips()) {
            try {
                if (Global.getSettings().getHullSpec(shipId).hasTag(Tags.AUTOMATED_RECOVERABLE)) {
                    facships.add(shipId);
                }
            } catch (Exception e) {
                // Skip if hull spec doesn't exist
            }
        }

        // Get automated ships known by both this faction and Explorarium
        List<String> remmyships = new ArrayList<>();
        for (String shipId : remmy.getKnownShips()) {
            if (faction.knowsShip(shipId) && facships.contains(shipId)) {
                remmyships.add(shipId);
            }
        }

        String facname = faction.getDisplayName();
        Color namecolor = faction.getColor();

        // FIXED: Don't chain methods on setHighlight() since it returns void
        LabelAPI para = dialog.getTextPanel().addPara("This ship's origin, the " + facname +
                ", knows " + facships.size() + " automated ships.");
        para.setHighlight(facname, String.valueOf(facships.size()));
        para.setHighlightColors(namecolor, Misc.getHighlightColor());

        // FIXED: Don't chain methods on setHighlight() since it returns void
        LabelAPI para2 = dialog.getTextPanel().addPara("The Explorarium know " + remmyships.size() + " of them.");
        para2.setHighlight(String.valueOf(remmyships.size()));
    }

    private void showRecountInfo(InteractionDialogAPI dialog) {
        dialog.getTextPanel().clear();
        dialog.setXOffset(Global.getSettings().getScreenWidth() / 6f);
        dialog.setTextWidth(Global.getSettings().getScreenWidth() / 3f);
        dialog.setTextHeight(Global.getSettings().getScreenHeight() / 1.4f);
        dialog.getTextPanel().updateSize();

        dialog.getTextPanel().addPara("The following is a list of all factions that use automated ships which can be recovered.");
        dialog.getTextPanel().addPara("Note that some automated ships may be absent from this list if they don't have an associated faction.");

        FactionAPI remmy = Global.getSector().getFaction(Factions.DERELICT);
        List<String> banlist = Arrays.asList("sotf_dustkeepers_burnouts", "rat_abyssals", "ai_all");
        List<String> factionlist = new ArrayList<>();

        for (FactionAPI fac : Global.getSector().getAllFactions()) {
            for (String shipId : fac.getKnownShips()) {
                try {
                    if (!factionlist.contains(fac.getId()) &&
                            !banlist.contains(fac.getId()) &&
                            Global.getSettings().getHullSpec(shipId).hasTag(Tags.AUTOMATED_RECOVERABLE) &&
                            !Global.getSettings().getHullSpec(shipId).getManufacturer().equals("Explorarium")) {
                        factionlist.add(fac.getId());
                        break;
                    }
                } catch (Exception e) {
                    // Skip if hull spec doesn't exist
                }
            }
        }

        for (String facid : factionlist) {
            FactionAPI faction = Global.getSector().getFaction(facid);
            if (faction == null) continue;

            List<String> facships = new ArrayList<>();
            for (String shipId : faction.getKnownShips()) {
                try {
                    if (Global.getSettings().getHullSpec(shipId).hasTag(Tags.AUTOMATED_RECOVERABLE)) {
                        facships.add(shipId);
                    }
                } catch (Exception e) {
                    // Skip if hull spec doesn't exist
                }
            }

            List<String> remmyships = new ArrayList<>();
            for (String shipId : remmy.getKnownShips()) {
                if (facships.contains(shipId)) {
                    remmyships.add(shipId);
                }
            }

            int cent = facships.isEmpty() ? 0 : Math.round(((float) remmyships.size() / facships.size()) * 100f);
            String facname = faction.getDisplayName();
            Color namecolor = faction.getColor();
            String facicon = faction.getCrest();

            TooltipMakerAPI tip = dialog.getTextPanel().beginTooltip();
            tip.addSectionHeading(facname, namecolor,
                    faction.getDarkUIColor(), Alignment.MID, 10f);

            // FIXED: Use beginImageWithText correctly
            TooltipMakerAPI imgWithText = tip.beginImageWithText(facicon, 64f);
            LabelAPI title = imgWithText.addTitle(facname, namecolor);
            title.italicize(0.5f);

            // FIXED: Don't chain setHighlight() calls
            LabelAPI progressPara = imgWithText.addPara("Collection Progress: " + cent + "%", 5f);
            progressPara.setHighlight(cent + "%");

            LabelAPI knowsPara = imgWithText.addPara("This faction knows " + facships.size() +
                    " compatible automated ships.", 5f);
            knowsPara.setHighlight(facships.size() + "");

            LabelAPI explorariumPara = imgWithText.addPara("The Explorarium currently know " + remmyships.size() +
                    " of them.", 5f);
            explorariumPara.setHighlight(remmyships.size() + "");

            tip.addImageWithText(5f);
            dialog.getTextPanel().addTooltip();
        }
    }

    private void getShowRaidTarget(InteractionDialogAPI dialog, MarketAPI target, List<Integer> rewards) {
        boolean isinit = false;
        String basename = "";
        String spec = targetmarket != null ? targetmarket.getFactionId() : "";
        WeightedRandomPicker<String> recipe = new WeightedRandomPicker<>();

        if (target == null) {
            WeightedRandomPicker<String> factionlist = new WeightedRandomPicker<>();

            for (FactionAPI faction : Global.getSector().getAllFactions()) {
                String factionId = faction.getId();
                if (!factionId.equals(Factions.DERELICT) &&
                        !factionId.equals("nex_derelict") &&
                        !factionId.equals(Factions.REMNANTS) &&
                        !factionId.equals(Factions.OMEGA) &&
                        !factionId.equals(Factions.TRITACHYON) &&
                        !factionId.equals(Factions.PLAYER)) {

                    List<MarketAPI> markets = Misc.getFactionMarkets(factionId);
                    boolean hasMilitaryMarket = false;

                    for (MarketAPI m : markets) {
                        if (!m.isHidden() && m.hasSpaceport() &&
                                (m.hasIndustry(Industries.MILITARYBASE) ||
                                        m.hasIndustry(Industries.HIGHCOMMAND))) {
                            hasMilitaryMarket = true;
                            break;
                        }
                    }

                    if (hasMilitaryMarket) {
                        factionlist.add(factionId);
                    }
                }
            }

            spec = factionlist.pick();
            isinit = true;

            WeightedRandomPicker<MarketAPI> list = new WeightedRandomPicker<>();
            for (MarketAPI market : Misc.getFactionMarkets(spec)) {
                if (!market.isHidden() && market.hasSpaceport() &&
                        (market.hasIndustry(Industries.MILITARYBASE) ||
                                market.hasIndustry(Industries.HIGHCOMMAND))) {
                    list.add(market);
                }
            }

            targetmarket = list.pick();
        }

        if (targetmarket.hasIndustry(Industries.MILITARYBASE)) {
            basename = "military base";
        } else {
            basename = "high command";
        }

        String facname = targetmarket.getFaction().getDisplayName();
        Color faccolor = targetmarket.getFaction().getColor();

        if (rewards.isEmpty()) {
            float defense = targetmarket.getStats().getDynamic().getMod(
                    Stats.GROUND_DEFENSES_MOD).computeEffective(0f);
            rewards.add(0, Math.round(defense / 100f));
            rewards.add(1, Math.round(defense / 500f));
            rewards.add(2, Math.round(defense / 1000f));
            rewards.add(3, Math.round(defense * 1000f));
        }

        int numgamma = rewards.get(0);
        int numbeta = rewards.get(1);
        int numalpha = rewards.get(2);
        int numcredits = rewards.get(3);

        dialog.getVisualPanel().showMapMarker(targetmarket.getPrimaryEntity(),
                targetmarket.getName(), faccolor, false, null,
                "Competitor Military Operations", null);

        dialog.getTextPanel().setFontVictor();
        dialog.getTextPanel().addPara("RECONSTRUCTING MESSAGE - // waiting ... //");
        dialog.getTextPanel().setFontInsignia();
        dialog.getTextPanel().addPara("Voluminous greetings, caretaker.");

        LabelAPI para1 = dialog.getTextPanel().addPara("Regrettable activity within a " +
                facname + " " + basename + " leads us to purveying this request to be received within your flock.");
        para1.setHighlight(facname);
        para1.setHighlightColor(faccolor);

        LabelAPI targetPara = dialog.getTextPanel().addPara("Please initiate protocol \"heartfelt fireworks celebration\" and visit upon " +
                targetmarket.getName() + " to shower joy breadth skies.");
        targetPara.setHighlight(targetmarket.getName());

        dialog.getTextPanel().addPara("Butlers will be invited to assist in decoration and management of guests. Forth the apex of annihilation, promptly disperse and recollect within the Mothership.");

        LabelAPI rewardPara = dialog.getTextPanel().addPara("To be dispensed includes " + numgamma + " chocolate mousse, " +
                numbeta + " lemon zest, " + numalpha + " blueberry gummies and " +
                numcredits + " credits will be given ");
        rewardPara.setHighlight(numgamma + "", numbeta + "", numalpha + "", numcredits + "");

        dialog.getTextPanel().setFontSmallInsignia();

        LabelAPI bombardPara = dialog.getTextPanel().addPara("Bombard the colony of " + targetmarket.getName() +
                " to disrupt their military operations and return to a Mothership to receive your reward. Explorarium fleets will be dispatched to assist you. Expect resistance.");
        bombardPara.setHighlight(targetmarket.getName());

        LabelAPI rewardDetailsPara = dialog.getTextPanel().addPara("Rewards are " + numgamma + " gamma core, " +
                numbeta + " beta core, " + numalpha + " alpha core and " +
                numcredits + " credits.");
        rewardDetailsPara.setHighlight(numgamma + "", numbeta + "", numalpha + "", numcredits + "");

        dialog.getTextPanel().setFontInsignia();
    }

    private void doSetup(InteractionDialogAPI dialog) {
        // Note: ds_nexusRaidIntel needs to be imported or defined
        /*
        Global.getSector().getIntelManager().addIntel(
            new ds_nexusRaidIntel(targetmarket, rewardlist), false, dialog.getTextPanel());
        Global.getSector().getListenerManager().addListener(
            new ds_nexusRaidIntel(targetmarket, rewardlist));
        */

        String targfaction = targetmarket.getFactionId();

        for (int i = 0; i < 2; i++) {
            CampaignFleetAPI remmy = MagicCampaign.createFleetBuilder()
                    .setFleetName("Chauffeurs")
                    .setFleetFaction(Factions.DERELICT)
                    .setAssignmentTarget(Global.getSector().getPlayerFleet())
                    .setAssignment(FleetAssignment.ORBIT_PASSIVE)
                    .setMinFP(Global.getSector().getPlayerFleet().getFleetPoints() / 2)
                    .setIsImportant(true)
                    .setSpawnLocation(Global.getSector().getPlayerFleet().getInteractionTarget())
                    .setTransponderOn(false)
                    .setQualityOverride(2f)
                    .create();

            remmy.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_MAKE_HOLD_VS_STRONGER, true);
            remmy.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_ALLOW_PLAYER_BATTLE_JOIN_TOFF, true);
            remmy.getMemoryWithoutUpdate().set(MemFlags.DO_NOT_TRY_TO_AVOID_NEARBY_FLEETS, true);
            remmy.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_FLEET_DO_NOT_GET_SIDETRACKED, true);
            remmy.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_FORCE_TRANSPONDER_OFF, true);

            remmy.removeFirstAssignment();
            remmy.addAssignment(FleetAssignment.ATTACK_LOCATION,
                    targetmarket.getPrimaryEntity(), 30f);
        }
    }

    private void getRaidReward(InteractionDialogAPI dialog) {
        // Note: ds_nexusRaidIntel needs to be imported or defined
        /*
        IntelInfoPlugin intel = Global.getSector().getIntelManager()
            .getFirstIntel(ds_nexusRaidIntel.class);

        if (intel instanceof ds_nexusRaidIntel) {
            ds_nexusRaidIntel raidIntel = (ds_nexusRaidIntel) intel;
            List<Integer> rewards = raidIntel.getReward();

            int numgamma = rewards.get(0);
            int numbeta = rewards.get(1);
            int numalpha = rewards.get(2);
            int numcredits = rewards.get(3);

            CampaignFleetAPI playerFleet = Global.getSector().getPlayerFleet();
            CargoAPI cargo = playerFleet.getCargo();

            cargo.addCommodity(Commodities.GAMMA_CORE, numgamma);
            cargo.addCommodity(Commodities.BETA_CORE, numbeta);
            cargo.addCommodity(Commodities.ALPHA_CORE, numalpha);
            cargo.getCredits().add(numcredits);

            dialog.getTextPanel().setFontVictor();
            dialog.getTextPanel().addPara("RECONSTRUCTING MESSAGE - // waiting ... //");
            dialog.getTextPanel().setFontInsignia();
            dialog.getTextPanel().addPara("Voluminous greetings, caretaker.");
            dialog.getTextPanel().addPara(numgamma + " chocolate mousse, " +
                numbeta + " lemon zest, " + numalpha + " blueberry gummies and " +
                numcredits + " credits are given")
                .setHighlight(numgamma + "", numbeta + "", numalpha + "", numcredits + "");

            dialog.getTextPanel().setFontSmallInsignia();
            dialog.getTextPanel().addPara(numgamma + " gamma core, " +
                numbeta + " beta core, " + numalpha + " alpha core and " +
                numcredits + " credits are rewarded")
                .setHighlight(numgamma + "", numbeta + "", numalpha + "", numcredits + "");
            dialog.getTextPanel().setFontInsignia();

            SectorAPI sector = Global.getSector();
            sector.getListenerManager().removeListenerOfClass(ds_nexusRaidIntel.class);

            List<IntelInfoPlugin> toRemove = new ArrayList<>();
            for (IntelInfoPlugin plugin : sector.getIntelManager().getIntel()) {
                if (plugin instanceof ds_nexusRaidIntel) {
                    toRemove.add(plugin);
                }
            }

            for (IntelInfoPlugin plugin : toRemove) {
                sector.getIntelManager().removeIntel(plugin);
            }

            targetmarket = null;
            rewardlist.clear();
            sector.getMemoryWithoutUpdate().set("$ds_nexusPartyTimeout", true, 180f);
            sector.getMemoryWithoutUpdate().set("$ds_nexusParty", 2);
            sector.getMemoryWithoutUpdate().unset("$ds_nexusParty");
        }
        */
    }
}

class NSP_MothershipBuildScript implements Script {
    private CampaignFleetAPI source;
    private EntityLocation loc;

    public NSP_MothershipBuildScript(CampaignFleetAPI source, EntityLocation loc) {
        this.source = source;
        this.loc = loc;
    }

    @Override
    public void run() {
        StarSystemAPI system = source.getStarSystem();
        if (system == null) return;

        Random random = Misc.random;
        Global.getSector().getCampaignUI().getMessageDisplay().addMessage("Mothership construction finished in " + system.getName());

        // Create a derelict mothership entity
        SectorEntityToken fleet = source.getContainingLocation().addCustomEntity(null, null, "derelict_mothership", "derelict");
        float orbitRadius = Misc.getDistance(source, system.getCenter());
        float orbitDays = orbitRadius / (20f + random.nextFloat() * 5f);
        float angle = Misc.getAngleInDegrees(system.getCenter().getLocation(), source.getLocation());
        fleet.setCircularOrbit(system.getCenter(), angle, orbitRadius, orbitDays);
        system.addTag(Tags.THEME_DERELICT_MOTHERSHIP);

        // Note: addNexusCargo needs to be imported or defined
        // fleet.getCargo().addAll(addNexusCargo(fleet));
        source.addAssignment(FleetAssignment.GO_TO_LOCATION_AND_DESPAWN, fleet, 999f);
    }
}

class nexusStorageScript implements EveryFrameScript {
    private SectorEntityToken nexus;
    private InteractionDialogAPI dialog;
    private boolean cancel = false;
    private boolean ranStorage = false;
    private IntervalUtil canceller = new IntervalUtil(0.03f, 0.03f);
    private SectorEntityToken storageEntity = Global.getSector().getEntityById("ds_nexusStorage");

    public nexusStorageScript(SectorEntityToken nexus, InteractionDialogAPI dialog) {
        this.nexus = nexus;
        this.dialog = dialog;
    }

    @Override
    public boolean isDone() {
        return false;
    }

    @Override
    public boolean runWhilePaused() {
        return true;
    }

    @Override
    public void advance(float amount) {
        if (storageEntity == null) {
            Global.getSector().removeTransientScriptsOfClass(nexusStorageScript.class);
            return;
        }

        if (!cancel) {
            canceller.advance(amount);
            if (canceller.intervalElapsed()) {
                cancel = true;

                FleetInteractionDialogPluginImpl plugin = (FleetInteractionDialogPluginImpl) dialog.getPlugin();
                FleetEncounterContext context = (FleetEncounterContext) plugin.getContext();
                if (context != null) {
                    context.applyAfterBattleEffectsIfThereWasABattle();
                    BattleAPI b = context.getBattle();
                    b.leave(Global.getSector().getPlayerFleet(), false);
                    b.finish(BattleSide.NO_JOIN, false);
                }
                dialog.dismiss();
                return;
            }
        }

        if (cancel && !ranStorage && !Global.getSector().getCampaignUI().isShowingDialog()) {
            Global.getSector().getCampaignUI().showInteractionDialog(storageEntity);
            ranStorage = true;
            return;
        }

        if (ranStorage && !Global.getSector().getCampaignUI().isShowingDialog() &&
                Global.getCurrentState() == GameState.CAMPAIGN) {
            Global.getSector().getCampaignUI().showInteractionDialog(nexus);
            Global.getSector().removeTransientScriptsOfClass(nexusStorageScript.class);
        }
    }
}