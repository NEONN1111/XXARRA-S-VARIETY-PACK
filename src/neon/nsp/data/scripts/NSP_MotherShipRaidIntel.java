//package neon.nsp.data.scripts;
//
//import com.fs.starfarer.api.Global;
//import com.fs.starfarer.api.campaign.*;
//import com.fs.starfarer.api.campaign.comm.IntelInfoPlugin;
//import com.fs.starfarer.api.campaign.econ.Industry;
//import com.fs.starfarer.api.campaign.econ.MarketAPI;
//import com.fs.starfarer.api.campaign.listeners.ColonyPlayerHostileActListener;
//import com.fs.starfarer.api.impl.campaign.ids.Factions;
//import com.fs.starfarer.api.impl.campaign.ids.Tags;
//import com.fs.starfarer.api.impl.campaign.intel.BaseIntelPlugin;
//import com.fs.starfarer.api.impl.campaign.rulecmd.salvage.MarketCMD;
//import com.fs.starfarer.api.ui.SectorMapAPI;
//import com.fs.starfarer.api.ui.TooltipMakerAPI;
//import com.fs.starfarer.api.campaign.SectorEntityToken;
//import com.fs.starfarer.api.campaign.LocationAPI;
//import com.fs.starfarer.api.campaign.rules.MemoryAPI;
//
//import java.util.*;
//
//public class NSP_MotherShipRaidIntel extends BaseIntelPlugin implements ColonyPlayerHostileActListener {
//
//    private MarketAPI bombmarket;
//    private ArrayList<Integer> reward;
//    private CargoAPI pcargo;
//    private final String title = "Coordinated Raid";
//    private final MemoryAPI memory;
//    private final String key = "$nsp_MothershipParty";
//    private final String sprite;
//
//    // stage 0 -> go to planet
//    // stage 1 -> shit bombed go back
//
//    public NSP_MotherShipRaidIntel(MarketAPI bombmarket, ArrayList<Integer> reward) {
//        this.bombmarket = bombmarket;
//        this.reward = reward;
//        this.pcargo = Global.getSector().getPlayerFleet().getCargo();
//        this.memory = Global.getSector().getMemory();
//        this.sprite = Global.getSettings().getFactionSpec(Factions.DERELICT).getCrest();
//    }
//
//    @Override
//    public void createIntelInfo(TooltipMakerAPI info, IntelInfoPlugin.ListInfoMode mode) {
//        if (memory.getInt(key) == 1) {
//            info.addTitle(title);
//            info.addPara("Return to any Mothership to receive the rewards.", 5f);
//        } else {
//            info.addTitle(title);
//            info.addPara("Bombard " + bombmarket.getName() + " in the " + bombmarket.getStarSystem().getName() + ".", 5f);
//        }
//    }
//
//    @Override
//    public boolean autoAddCampaignMessage() {
//        return true;
//    }
//
//    @Override
//    public String getCommMessageSound() {
//        return getSoundMajorPosting();
//    }
//
//    @Override
//    public String getSmallDescriptionTitle() {
//        return title;
//    }
//
//    @Override
//    public Set<String> getIntelTags(SectorMapAPI map) {
//        Set<String> tags = super.getIntelTags(map);
//        if (tags == null) {
//            tags = new LinkedHashSet<String>();
//        }
//
//        tags.add(Tags.INTEL_ACCEPTED);
//        tags.add(Tags.INTEL_MISSIONS);
//
//        return tags;
//    }
//
//    @Override
//    public String getIcon() {
//        return sprite;
//    }
//
//    @Override
//    public FactionAPI getFactionForUIColors() {
//        return Global.getSector().getFaction(Factions.DERELICT);
//    }
//
//    @Override
//    public void createSmallDescription(TooltipMakerAPI info, float width, float height) {
//        // Empty implementation as per original Kotlin code
//    }
//
//    @Override
//    public void reportRaidForValuablesFinishedBeforeCargoShown(InteractionDialogAPI dialog,
//                                                               MarketAPI market,
//                                                               MarketCMD.TempData actionData,
//                                                               CargoAPI cargo) {
//        // Empty implementation
//    }
//
//    @Override
//    public void reportRaidToDisruptFinished(InteractionDialogAPI dialog,
//                                            MarketAPI market,
//                                            MarketCMD.TempData actionData,
//                                            Industry industry) {
//        // Empty implementation
//    }
//
//    @Override
//    public void reportTacticalBombardmentFinished(InteractionDialogAPI dialog,
//                                                  MarketAPI market,
//                                                  MarketCMD.TempData actionData) {
//        if (market != null && market == bombmarket) {
//            memory.set(key, 1, 0f);
//            Global.getSector().getIntelManager().addIntelToTextPanel(this, dialog.getTextPanel());
//        }
//    }
//
//    @Override
//    public void reportSaturationBombardmentFinished(InteractionDialogAPI dialog,
//                                                    MarketAPI market,
//                                                    MarketCMD.TempData actionData) {
//        if (market != null && market == bombmarket) {
//            memory.set(key, 1, 0f);
//            Global.getSector().getIntelManager().addIntelToTextPanel(this, dialog.getTextPanel());
//        }
//    }
//
//    @Override
//    public boolean isDone() {
//        return (memory.getInt(key) == 2);
//    }
//
//    @Override
//    public void advance(float amount) {
//        super.advance(amount);
//    }
//
//    @Override
//    public void reportRemovedIntel() {
//        Global.getSector().getListenerManager().removeListener(this);
//
//        // Assuming these are static fields in ds_nexusStartRulecmd
//        // You'll need to access them properly
//        // targetmarket = null;
//        // rewardlist.clear();
//
//        memory.set("$nsp_MothershipPartyTimeout", true, 180f);
//        memory.set(key, 2, 0f);
//        memory.expire(key, 0f);
//    }
//
//    public ArrayList<Integer> getRewardList() {
//        return reward;
//    }
//
//    // Getter methods for fields
//    public MarketAPI getBombmarket() {
//        return bombmarket;
//    }
//
//    public ArrayList<Integer> getReward() {
//        return reward;
//    }
//
//    public String getTitle() {
//        return title;
//    }
//
//    public String getKey() {
//        return key;
//    }
//
//    public String getSprite() {
//        return sprite;
//    }
//}