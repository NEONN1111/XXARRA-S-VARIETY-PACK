package neon.nsp.data.scripts.rulecmd;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.ids.HullMods;
import com.fs.starfarer.api.impl.campaign.ids.Sounds;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import com.fs.starfarer.api.impl.campaign.rulecmd.AddRemoveCommodity;
import com.fs.starfarer.api.impl.campaign.rulecmd.BaseCommandPlugin;
import com.fs.starfarer.api.impl.campaign.rulecmd.SetStoryOption;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class nsp_displayAutomatedPicker extends BaseCommandPlugin {
    static FleetMemberAPI picked;
    static int spCost;
    static float currentCost = 0f;
    static float costMult = 3f; // multiply base hull cost by this for price
    static Float days = 180f; // technically should always be reset when we pick a ship, so this is okay... maybe.
    public static String automatedpickedKey = "$nsp_pickedAutoship";
    public static String automateddaysKey = "$nsp_automatedWaitDays";
    public static String autoSystemsTitle = "Automated Systems Tuning";
    @Override
    public boolean execute(String ruleId, InteractionDialogAPI dialog, List<Misc.Token> params, Map<String, MemoryAPI> memoryMap) {
        String mode = params.get(0).getString(memoryMap);
        switch (mode){
            case "PICK" -> showAutomatedPickerDialog(dialog);
            case "SEND" -> eatAutoShip(dialog);
            case "RETRIEVE" -> makeAutoShipNoPenalty(dialog);
        }
        return false;
    }

    void showAutomatedPickerDialog(InteractionDialogAPI dialog){ // displays popup dialog of fleet member picker using ships in our fleet
        ArrayList<FleetMemberAPI> list = locateValidAutoships(Global.getSector().getPlayerFleet().getFleetData().getMembersListCopy());
        if (list.isEmpty()){
            dialog.getTextPanel().addPara("Your fleet currently doesn't contain any ships that could have their automation protocols enhanced.");
            return;
        }
        // $global.gaATG_missionCompleted is the key for completing the gate quest
        dialog.showFleetMemberPickerDialog(autoSystemsTitle, "Submit", "Cancel", 8, 5, 120f, true,false, list, new FleetMemberPickerListener() {
            @Override
            public void pickedFleetMembers(List<FleetMemberAPI> members) { // if we pick a ship and hit confirm, run all of this code. returns a list, but b/c pickMultiple is false the size is always 1 if picked
                if (members.isEmpty()) return;
                boolean isWeirdHull = false;
                picked = members.stream().findFirst().get(); // mmm yes java 17
                String shipName = picked.getShipName();
                String pointOrPoints = "story point";
                String flavorText;
                String hullID;
                if (picked.getHullSpec().isBaseHull()) hullID = picked.getHullSpec().getHullId();
                else hullID = picked.getHullSpec().getBaseHullId();
                // fatass giant switch statement for flavor text because i'm a hack
                // well, it's better than else if else if anyway.
                flavorText = switch (hullID){
                    case "radiant" -> "\"That is a beautiful ship. Its hull carries all the hallmarks of a standard Tri-Tachyon ISDS ship, but at the same time, it's not an exact, or even close, match to any other vessel I've seen described in reports from the... the First AI War. The- the unclassified ones, because I would never want to upset the Hegemony. I don't know if that means, um, that Tri-Tach covered its existence up or it didn't exist back then. I'm not quite sure which answer is more exciting. Or worrying. But if the architecture is still the same beneath the hull, it should be simple to adapt the-\", You briefly zone out as Phos offers an extremely long-winded explanation, with your chief engineer performing a spacer hand-signal said to give strength in intolerable situations, only for you to snap back to attention when they mention taking scans. \"Yes, $PlayerSirOrMadam, I asked if I could keep some data. Scans. For the AI Defe- no, for the Phase Physics Department. Because... from a cursory glance, there appears to be a phase skimmer setup built into it. The Radiant, I mean. In a capital-class or equivalent ship. Hm. That isn't supposed to be possible. Have-\" the academician's voice quiets down a bit. \"Have you actually seen it be used in a battlespace scenario?\"";
                    case "nova" -> "\"The Nova! I know this one, I know this one. One of my favourite ships Tri-Tachyon ever built. Supposedly. Could've been designed and built entirely autonomously with zero human input! Fancy that idea. But... it's my favourite because of its implementation of the Orion Drive. Normally beloathed by spaceship crews, as I'm sure you know. Tri-Tachyon's solution was elegant: remove the crew entirely. An artificial intelligence does not mind rapid bursts of acceleration. Aside from that, it is a fairly ordinary automated ship hull, aside from the fact it is better than its human-crewed counterpart, the Retribution, in every single way!\" You can feel the excitement build in the air as they say that, no doubt ready to launch into a rather in-depth ramble about the pros and cons of the two ships, but it seems they manage to stop themselves. Clearing their throat, and adjusting their coat, they continue. \"Now, it, ah--shouldn't be too hard to make a few tweaks here and there based on the Safeguard precedent. It would also probably help if your chief engineer had, uhm, proper documentation for its internals. Which I can provide, of course.\" \n";
                    case "apex" -> "\"This- mmm, this would be the... the Apex. Iiiit- it was a unique design of Tri-Tachyon's, more a marketing pitch for a nuuuumber of- of-\" The academician takes a deep breath, running their hands over their face for a moment. \"Okay, okay, you can do this. This is the Apex- no, I said that already. Energy Bolt Coherer. Dual Terminator drone printers. $PlayerSirOrMadam. Please. Just- look at it. I can't keep a straight face as I do. It's so rotund. Like a Brawler that ate too much. I can't even look at it without- without smiling. But of course...\" Phos allows themselves a snicker, \"I can provide schematics and special modifications to make it run smoothly. If I can force myself to look at it without-\" Laughing.";
                    case "brilliant" -> "\"brilliant flavor text\"";
                    case "fulgent" -> "\"fulgent\"";
                    case "glimmer" -> "\"Ah, this one - I believe it's the Glimmer! Quite a ferocious small combatant given how heavily armed it is. It'll make a fine addition to your fleet, Captain!\"";
                    case "lumen" -> "\"TriTachyon's Lumen class! With it's phase skimmer array, it can be a surprisingly survivable little thing! Remind me to forward you the reports on it's effectivness in fleet actions. They're a good read!\"";
                    case "rampart" -> "\"Oh! The, ehr... Rampart! Yes, I know much about this one! Mbaye-Gogol's answer to the Domain's need for a rugged, well-armed, and, most importantly... \", The academician pauses briefly, presumably for dramatic effect., \" Mass producible design! Designed for high modularity and compatibility, just as with Mbaye's other designs for the Explorarium initiative. There are plenty of, uhm... oh, just a moment...\",Phos picks up their Tri-Pad, its screen protector scratched and back cover accented with various novelty stickers from years of constant use., \" Plenty of reports, for Domain infosec standards that is, detailing its effectiveness against, er... rogue elements, as it were. But- Right, you're here for my- Our help, and I've been rambling on for a while, now, haven't I? Here. I'll, uhm- I'll have all of the relevant data ready for you as soon as it's been run through processing, Captain!";
                    case "bastillon" -> "\"Ah, the Bastillon! This one was one of Mbaye's earlier contributions to the fleets of the Explorarium's automated fleets. An attempt to follow in the footsteps of the Defender class, it was an attempt that in no doubt succeeded!\"";
                    case "picket" -> "\"The picket class! An underwhelming thing at first glance, they can be quite dangerous when massed in great enough numbers. I'll transfer the relevant data to you as soon as it's ready, captain.";
                    case "sentry" -> "\"The, err... the, Sentry class, I believe? Yes, there are more than a few reports outlining it's usage, but, I will admit, it is a vessel I tend to, uhm... skip over. As soon as it's ready, I'll send the data your way, Captain.\"";
                    case "defender" -> "\"The Defender class point-defense drone! From the reports that I've read, it was one of, if not the single most utilized drone designs within the Explorarium's fleets. I'll get the data to you as soon as it's ready, Captain!\"";
                    default -> "\"While I'm personally not familiar with this hull design, but it, uhm, shouldn't have an effect on the processes required!\"";
                };
                // check if player gave out a scholarship and also invested in AI
                MemoryAPI playermem = Global.getSector().getPlayerMemoryWithoutUpdate();
                float reduction = 0f;
                boolean investedInAI = (playermem.getBoolean("$scholarshipThemeUseAI") || playermem.getBoolean("$scholarshipThemeProAI"));
                if (investedInAI){ // $player.didScholarship500k
                    if (playermem.getBoolean("$didScholarship500k")){
                        reduction = 0.20f;

                    }
                    else if (playermem.getBoolean("$didScholarship200k")){
                        reduction = 0.10f;
                    }
                }


                if (picked.getVariant().hasHullMod("shard_spawner")) { // this was added as an afterthought, so the way it's implemented is slightly stupid. oh well. this only runs once in a dialog, optimization is whatever
                    isWeirdHull = true;
                    flavorText = "Phos appears shocked as the vessel appears on the feed, as if they turned their gaze upwards to see a Reaper torpedo barreling directly towards them.\n\n\"Captain, what in Ludd's name is...\" the academician trails off, dumbfounded at the odd angular vessel. \"I am rather, uh, unsure of how you obtained such a thing. But we certainly cannot assist you with it.\"";
                }
                if (picked.getHullSpec().getManufacturer().contentEquals("Threat")) {
                    isWeirdHull = true;
                    dialog.getTextPanel().setFontVictor();
                    dialog.getTextPanel().addPara("T H R E A T  D E T E C T E D").setColor(Misc.getNegativeHighlightColor());
                    dialog.getTextPanel().addPara("T H R E A T  D E T E C T E D").setColor(Misc.getNegativeHighlightColor());
                    dialog.getTextPanel().setFontInsignia();
                    flavorText = "Phos startles at the sudden warnings chirping from the console, no doubt triggered by the ship you've brought forward. \"Captain,\" they say sternly. \"Warning systems are lighting up like a solar flare just from that vessel approaching the Academy.\"\n\n\"I don't know where you got that thing, but put it back. Please.\"";
                }
                if (hullID.equals("guardian")){
                    isWeirdHull = true;
                    flavorText = "The academician gives an appraising look over the vessel, brow risen. \"It... certainly appears to be an Explorarium drone by it's markings, but the hull structure is rather...\" a pause. \"Unusual, to say the least. Warped and gnarled, akin to roots.\"\n\nThey shake their head. \"I don't think we can work with this, Captain - I don't believe I'm supposed to be seeing this to begin with. You didn't do anything you shouldn't have, did you?\"";
                }
                spCost = switch (picked.getHullSpec().getHullSize()) {
                    case CAPITAL_SHIP -> 4;
                    case CRUISER -> 3;
                    case DESTROYER -> 2;
                    case FRIGATE ->
                            1;
                    default -> 1; // not possible, but who knows
                };

                days = switch (picked.getHullSpec().getHullSize()){ // todo change these values once testing is over
                    case CAPITAL_SHIP -> 140f;
                    case CRUISER -> 80f;
                    case DESTROYER -> 60f;
                    case FRIGATE ->  40f;
                    default -> 10f;
                };
                if (spCost > 1) pointOrPoints += "s"; // if i don't have correct grammar i will instantly explode
                currentCost = picked.getHullSpec().getBaseValue()*costMult; // calculate sp/credit cost then add relevant info to text panel
                if (investedInAI) currentCost *= (1f - reduction);
                dialog.getOptionPanel().clearOptions(); // remove any pre-existing options from panel
                dialog.getVisualPanel().showFleetMemberInfo(picked); // add guy to visual panel
                dialog.getTextPanel().addPara("You bring the " + shipName + " to their attention, displaying it on the visual feed.").setHighlight(shipName);
                dialog.getTextPanel().addPara(flavorText);
                if (!isWeirdHull) {
                    dialog.getTextPanel().addPara("\"We'll require at least " + String.valueOf(days.intValue()) + " days to overhaul this vessel.\"").setHighlight(String.valueOf(days.intValue()));
                    dialog.getTextPanel().addPara("Optimizing the " + shipName + "'s automation protocols will cost " + String.valueOf(spCost) + " " + pointOrPoints + ", and " + Misc.getDGSCredits(currentCost)).setHighlight(String.valueOf(spCost), Misc.getDGSCredits(currentCost));
                    dialog.getOptionPanel().addOption("Confirm the transfer", "nsp_automatedEatShip"); // calls SEND
                    if (reduction > 0f) {
                        int display = (int) (reduction*100f);
                        dialog.getTextPanel().setFontSmallInsignia();
                        dialog.getTextPanel().addPara("Due to your investment into AI research, credit cost is reduced by " + display + "%.", Misc.getHighlightColor());
                        dialog.getTextPanel().setFontInsignia();
                    }
                }
                dialog.getOptionPanel().addOption("\"Actually, nevermind.\"", "nsp_automatedNoEat"); // just goes back and resets visual panel
                dialog.makeStoryOption("nsp_automatedEatShip", spCost, 0.0f, Sounds.STORY_POINT_SPEND_TECHNOLOGY);
                SetStoryOption.StoryOptionParams params = new SetStoryOption.StoryOptionParams("nsp_automatedEatShip", spCost, "nsp_automatedEatShip", Sounds.STORY_POINT_SPEND_TECHNOLOGY, "Made an automated hull require no automated ship points.");
                BaseStoryPointActionDelegate delegate = new BaseStoryPointActionDelegate() { // create delegate to apply to option params later
                    @Override
                    public String getLogText() {
                        return "Made an automated hull require no automated ship points.";
                    }

                    @Override
                    public String getTitle() { // adds big header at top w/ text
                        return autoSystemsTitle;
                    }

                    @Override
                    public boolean withSPInfo() {
                        return super.withSPInfo();
                    }

                    @Override
                    public void createDescription(TooltipMakerAPI info) {
                        info.addPara("The hull will no longer suffer a CR penalty from being an automated ship.", 5f);
                       // info.addPara("Costs " + String.valueOf(spCost) + " story points.",5f).setHighlight(String.valueOf(spCost));
                    }

                    @Override
                    public int getRequiredStoryPoints() { // required for correct display on desc
                        return spCost;
                    }

                };

                if (spCost > Global.getSector().getPlayerStats().getStoryPoints() || currentCost > Global.getSector().getPlayerFleet().getCargo().getCredits().get()) {
                    dialog.getOptionPanel().setEnabled(params.optionId, false);
                } // if player doesn't have enough sp or credits, disable option

                dialog.getOptionPanel().addOptionTooltipAppender(params.optionId, new OptionPanelAPI.OptionTooltipCreator() {
                    public void createTooltip(TooltipMakerAPI tooltip, boolean hadOtherText) { // adds tooltip on hover option
                        float opad = 10f;
                        float initPad = 0f;
                        if (hadOtherText) initPad = opad;
                        tooltip.addStoryPointUseInfo(initPad, spCost, 0.0f, true);
                        int sp = Global.getSector().getPlayerStats().getStoryPoints();
                        String points = "points";
                        if (sp == 1) points = "point";
                        tooltip.addPara("You have %s " + Misc.STORY + " " + points + ".", opad,
                                Misc.getStoryOptionColor(), "" + sp);
                    }
                });

                dialog.getOptionPanel().addOptionConfirmation(params.optionId, delegate); // add y/n popup
                dialog.getOptionPanel().setStoryOptionParams(params.optionId, params, delegate); // assigns params using delegate and params we created to specified option ID

            }

            @Override
            public void cancelledFleetMemberPicking() { // called if we open the fleet member picker, but then close it without doing anything
                dialog.getTextPanel().addPara("\"Feeling a bit uncertain?\"");

            }
        });

    }
    void eatAutoShip(InteractionDialogAPI dialog){ // use reference values to set stuff and eat the shippe
        Global.getSector().getMemoryWithoutUpdate().set(automatedpickedKey, picked); // save our guy for later
        Global.getSector().getMemoryWithoutUpdate().set(automateddaysKey, true, days); // put cooldown on option, expires in days days... so
        Global.getSector().getPlayerFleet().getFleetData().removeFleetMember(picked); // technically we could put him into persistent data instead, but i want to see him while i'm testing... so maybe it's ok.
        AddRemoveCommodity.addCreditsLossText((int) currentCost, dialog.getTextPanel());
        Global.getSector().getPlayerFleet().getCargo().getCredits().subtract(currentCost);
        AddRemoveCommodity.addFleetMemberLossText(picked, dialog.getTextPanel());

    }
    void makeAutoShipNoPenalty(InteractionDialogAPI dialog){
        FleetMemberAPI member = (FleetMemberAPI) Global.getSector().getMemoryWithoutUpdate().get(automatedpickedKey); // retrieve ship from memory then unset it after
        Global.getSector().getPlayerFleet().getFleetData().addFleetMember(member);
        Global.getSector().getMemoryWithoutUpdate().unset(automatedpickedKey);
        member.getVariant().addTag(Tags.TAG_AUTOMATED_NO_PENALTY);
        member.getVariant().addTag(Tags.VARIANT_ALWAYS_RECOVERABLE); // TODO fairly big issue where base hull retains unboardable tag, making it unrecoverable. can be fixed via reflection, but... fuck that.
        AddRemoveCommodity.addFleetMemberGainText(member, dialog.getTextPanel());
        dialog.getTextPanel().setFontSmallInsignia();
        dialog.getTextPanel().addPara("The " + member.getShipName() + " has had its automated protocols enhanced.").setColor(Misc.getHighlightColor());
        dialog.getTextPanel().setFontInsignia();

    }
    public static ArrayList<FleetMemberAPI> locateValidAutoships(List <FleetMemberAPI> members){
        ArrayList<FleetMemberAPI> memberList = new ArrayList<>(); // return array of valid fleet members for fleet member picker
        for (FleetMemberAPI member: members){ // filter for any autoships that aren't no penalty
            if ((member.getVariant().hasHullMod(HullMods.AUTOMATED) || member.getVariant().hasTag(Tags.AUTOMATED)) && !member.getVariant().hasTag(Tags.TAG_AUTOMATED_NO_PENALTY)){
                memberList.add(member);
            }
        }
        return memberList;
    }
}
