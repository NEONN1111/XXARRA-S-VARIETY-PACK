package data.scripts.rulecmd;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.FleetMemberPickerListener;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.DModManager;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import com.fs.starfarer.api.impl.campaign.rulecmd.AddRemoveCommodity;
import com.fs.starfarer.api.impl.campaign.rulecmd.BaseCommandPlugin;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;

import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class nsp_displaySafeguardPicker extends BaseCommandPlugin {
    static Float days = 180f; // technically should always be reset when we pick a ship, so this is okay... maybe.
    static FleetMemberAPI picked;
    public static String safeguardpickedKey = "$nsp_pickedSafeguard";
    public static String safeguarddaysKey = "$nsp_safeguardWaitDays";
    public static String safeguardTitle = "Safeguard Systems Analysis";
    static Float totalRepairCost = 0f;
    static Map<ShipAPI.HullSize, Float> repairCosts = new HashMap<>();
    static ArrayList<FleetMemberAPI> storagelist = new  ArrayList<>();
    static {
        repairCosts.put(ShipAPI.HullSize.CAPITAL_SHIP, 75000f);
        repairCosts.put(ShipAPI.HullSize.CRUISER, 60000f);
        repairCosts.put(ShipAPI.HullSize.DESTROYER, 45000f);
        repairCosts.put(ShipAPI.HullSize.FRIGATE, 30000f);
    }
    @Override
    public boolean execute(String ruleId, InteractionDialogAPI dialog, List<Misc.Token> params, Map<String, MemoryAPI> memoryMap) {
        String mode = params.get(0).getString(memoryMap);
        switch (mode){
            case "CHECK" -> { // used for interaction w/ galatia academy if player has safeguard hull
                // $id == station_galatia_academy
                // reminder to FireBest GAPostOpen after we do our dialog, to not break any other quests. i hope!
                // alviss person ID is "sebestyen"
                // also reused elsewhere in interaction w/ TT market, and during daud dialog
                // nobody reads code comments, so this is where i go on a spiel about how daud is super dreamy
                if (locateValidSafeguardShips(Global.getSector().getPlayerFleet().getFleetData().getMembersListCopy()).isEmpty()) {
                    return false;
                }
                else {
                    return true;
                }
            }
            case "PICK" -> showSafeguardPickerDialog(dialog);
            case "SEND" -> eatSafeguard(dialog);
            case "RETRIEVE" -> pickUpFromDaycare(dialog);
            case "REPAIR" -> repairSafeguardInFleet(dialog, false);
            case "REPAIRPICKER" -> showSafeguardRepairPicker(dialog);
            case "REPAIRCREDITS" -> repairSafeguardInFleet(dialog, true);

        }
        return false;
    }
    void pickUpFromDaycare(InteractionDialogAPI dialog){ // picking up our favourite little creature
        // rules sets a global memkey here which our hullmod will use to know it should be restorable
        FleetMemberAPI member = (FleetMemberAPI) Global.getSector().getMemoryWithoutUpdate().get(safeguardpickedKey);
        Global.getSector().getPlayerFleet().getFleetData().addFleetMember(member);
        Global.getSector().getMemoryWithoutUpdate().unset(safeguardpickedKey);
        AddRemoveCommodity.addFleetMemberGainText(member, dialog.getTextPanel());
        // for (FleetMemberAPI fleetmember: locateValidSafeguardShips(Global.getSector().getPlayerFleet().getFleetData().getMembersListCopy())){
          // fleetmember.getVariant().removeTag(Tags.VARIANT_UNRESTORABLE);
         //}

    }
    void eatSafeguard(InteractionDialogAPI dialog){ // use reference values to set stuff and eat the shippe
        Global.getSector().getMemoryWithoutUpdate().set(safeguardpickedKey, picked); // save our guy for later
        Global.getSector().getMemoryWithoutUpdate().set(safeguarddaysKey, true, days); // put cooldown on option, expires in days days... so
        Global.getSector().getPlayerFleet().getFleetData().removeFleetMember(picked); // technically we could put him into persistent data instead, but i want to see him while i'm testing... so maybe it's ok.
        AddRemoveCommodity.addFleetMemberLossText(picked, dialog.getTextPanel());

    }
    void showSafeguardPickerDialog(InteractionDialogAPI dialog){
        ArrayList<FleetMemberAPI> list = locateValidSafeguardShips(Global.getSector().getPlayerFleet().getFleetData().getMembersListCopy());
        if (list.isEmpty()){
            dialog.getTextPanel().addPara("You don't currently have an automated Safeguard hull in your fleet.");
            return;
        }
        dialog.showFleetMemberPickerDialog(safeguardTitle, "Submit", "Cancel", 3, 3, 160f, true,false, list, new FleetMemberPickerListener() {
            @Override
            public void pickedFleetMembers(List<FleetMemberAPI> members) {
                if (members.isEmpty()) return;
                picked = members.stream().findFirst().get(); // mmm yes java 17
                String shipName = picked.getShipName();
                days = switch (picked.getHullSpec().getHullSize()) {
                    case CAPITAL_SHIP -> 40f;
                    case CRUISER -> 30f;
                    case DESTROYER -> 25f;
                    case FRIGATE -> // not possible, but who knows
                            25f;
                    default -> 360f;
                };
                dialog.getOptionPanel().clearOptions();
                dialog.getVisualPanel().showFleetMemberInfo(picked);
                dialog.getTextPanel().addPara("You bring the " + shipName + " to their attention, displaying it on the visual feed.").setHighlight(shipName);
                dialog.getTextPanel().addPara("\"The " + picked.getHullSpec().getHullName() + "? Yes, I believe we can get something working with this.\"").setHighlight(picked.getHullSpec().getHullName());
                dialog.getTextPanel().addPara("\"Given current estimates, it should take roughly " + String.valueOf(days.intValue()) + " days to glean what we need from it.\"").setHighlight(String.valueOf(days.intValue()));
                dialog.getTextPanel().addPara("\"Don't worry - we'll return it to you safe and sound once we're done.\"");

                dialog.getOptionPanel().addOption("Confirm the transfer", "nsp_safeguardEatShip"); // calls SEND
                dialog.getOptionPanel().addOption("\"Actually, nevermind.\"", "nsp_safeguardNoEat"); // just goes back and resets visual panel
            }

            @Override
            public void cancelledFleetMemberPicking() {
                dialog.getTextPanel().addPara("\"Feeling a bit uncertain?\"");

            }
        });

    }
    void showSafeguardRepairPicker(InteractionDialogAPI dialog){
        storagelist.clear();
        totalRepairCost = 0f;
        ArrayList<FleetMemberAPI> list = new ArrayList<>();
        for (FleetMemberAPI member: locateValidSafeguardShips(Global.getSector().getPlayerFleet().getFleetData().getMembersListCopy())){
            if (DModManager.getNumDMods(member.getVariant()) > 0 ){
                list.add(member);
            }
        }
        if (list.isEmpty()){
            dialog.getTextPanel().addPara("None of your ships have any D-mods.").setColor(Misc.getHighlightColor());
            return;

        }
        dialog.showFleetMemberPickerDialog("test", "Repair", "Cancel", 3, 3, 160f, true, true, list, new FleetMemberPickerListener() {
            @Override
            public void pickedFleetMembers(List<FleetMemberAPI> members) {
                dialog.getOptionPanel().clearOptions();
                dialog.getTextPanel().setFontSmallInsignia();
                dialog.getTextPanel().addPara("Currently selected the following ships : ");

                for (FleetMemberAPI member : members){
                    int numdmods = DModManager.getNumDMods(member.getVariant());
                    totalRepairCost += (repairCosts.get(member.getHullSpec().getHullSize()) * numdmods);
                    storagelist.add(member);
                    TooltipMakerAPI tip = dialog.getTextPanel().beginTooltip();
                    TooltipMakerAPI image = tip.beginImageWithText(member.getHullSpec().getSpriteName(), 64f);
                    image.addTitle(member.getShipName(), Misc.getHighlightColor());
                    for (String modID : member.getVariant().getSortedMods()){
                        if (DModManager.getMod(modID).hasTag(Tags.HULLMOD_DMOD)){
                            image.addPara(DModManager.getMod(modID).getDisplayName(), 5f).setColor(Misc.getNegativeHighlightColor());
                        }
                    }
                    tip.addImageWithText(5f);
                    dialog.getTextPanel().addTooltip();
                }

                dialog.getTextPanel().addPara("Repairing them would cost " + Misc.getDGSCredits(totalRepairCost) + ".").setHighlight(Misc.getDGSCredits(totalRepairCost));
                dialog.getTextPanel().setFontInsignia();


                if (totalRepairCost > Global.getSector().getPlayerFleet().getCargo().getCredits().get()) {
                    dialog.getTextPanel().addPara("You don't have enough credits.").setColor(Color.RED);
                    dialog.getTextPanel().setFontInsignia();
                } else {
                    dialog.getOptionPanel().addOption("Confirm repairs","nsp_glamorRepairSafeguard");

                }
                dialog.getOptionPanel().addOption("\"Nevermind\"", "nsp_glamorBack");

            }

            @Override
            public void cancelledFleetMemberPicking() {

            }
        });



    }
    void repairSafeguardInFleet(InteractionDialogAPI dialog, Boolean costCredits){
        dialog.getTextPanel().setFontSmallInsignia();
        if (costCredits){
            for (FleetMemberAPI member :storagelist) {
                // this is stupid copying over the method and not just making a new one but i'm so tired and over it
                // if another coder sees this, you can fix it. nerd!!
                for (String modID : member.getVariant().getSortedMods()){
                    if (DModManager.getMod(modID).hasTag(Tags.HULLMOD_DMOD)){
                        DModManager.removeDMod(member.getVariant(), modID);
                    }
                }
            }
            Global.getSector().getPlayerFleet().getCargo().getCredits().subtract(totalRepairCost);
            AddRemoveCommodity.addCreditsLossText(totalRepairCost.intValue(), dialog.getTextPanel());
            return;
        }
        for (FleetMemberAPI member :locateValidSafeguardShips(Global.getSector().getPlayerFleet().getFleetData().getMembersListCopy())){
            int numDmods = 0;
            for (String modID : member.getVariant().getSortedMods()){
                if (DModManager.getMod(modID).hasTag(Tags.HULLMOD_DMOD)){
                    numDmods += 1;
                    DModManager.removeDMod(member.getVariant(), modID);
                }
            }
            if (numDmods > 0){
                TooltipMakerAPI tip = dialog.getTextPanel().beginTooltip();
                TooltipMakerAPI image = tip.beginImageWithText(member.getHullSpec().getSpriteName(), 64f);
                image.addTitle(member.getShipName(), Misc.getHighlightColor());
                image.addPara(String.valueOf(numDmods) + " d-mods removed.", 5f).setHighlight(String.valueOf(numDmods));
                tip.addImageWithText(5f);
                dialog.getTextPanel().addTooltip();
            }
        }

        dialog.getTextPanel().setFontInsignia();
    }

    public static ArrayList<FleetMemberAPI> locateValidSafeguardShips(List <FleetMemberAPI> members){ // also used for alviss calling player on interaction with market before postdock is called
        ArrayList<FleetMemberAPI> memberList = new ArrayList<>(); // return array of valid fleet members for fleet member picker
        ArrayList<String> validHullList = new ArrayList<>(); // this is stupid because it doesn't need to be recreated every time i run this but idc
        validHullList.add("nsp_onslaught_safeguard");
        validHullList.add("nsp_legion_safeguard");
        validHullList.add("nsp_dominator_safeguard");
        validHullList.add("nsp_eagle_safeguard");
        validHullList.add("nsp_falcon_safeguard");
        validHullList.add("nsp_enforcer_safeguard");
        for (FleetMemberAPI member : members){
            String specID;
            if (member.getHullSpec().isBaseHull()) specID = member.getHullSpec().getHullId(); // okay, funny thing. apparently, getBaseHullID crashes the game
            else specID = member.getHullSpec().getBaseHullId(); // if the ship is already a base hull. amazing. why is this necessary? because if it's a D-hull, the hull and base are different.
            if (validHullList.contains(specID)){
                memberList.add(member);
            }
        }
        return memberList;
    }
}

