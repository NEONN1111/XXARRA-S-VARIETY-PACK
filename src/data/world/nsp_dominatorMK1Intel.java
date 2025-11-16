package data.world;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import com.fs.starfarer.api.impl.campaign.intel.BaseIntelPlugin;
import com.fs.starfarer.api.ui.SectorMapAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;

import java.awt.*;
import java.util.Set;

public class nsp_dominatorMK1Intel extends BaseIntelPlugin {
    String title = "Voices In The Dark"; // replace this lol
    String missionIcon = Global.getSettings().getSpriteName("intel", "link_to_derelict_ship");

    // todo :
    // tell guy to set ID to entity so i don't have to filter the entire world to find the legion
    MemoryAPI memory = Global.getSector().getMemoryWithoutUpdate();
    SectorEntityToken dominatorLocation = Global.getSector().getEntityById("nsp_dominatorWreck");

    @Override
    public void createSmallDescription(TooltipMakerAPI info, float width, float height) { // the big box you see when you open the intel entry by pressing e
        super.createSmallDescription(info, width, height);
        info.addPara("It has been brought to your attention that the Onslaught Mk.1 has been sending, and recieving signals from two points in abyssal hyperspace for the past month. The prospect of what these voices in the dark may hold unsettles your crew. Nonetheless, you are determined to find out.", 0f);

    }

    @Override
    public void createIntelInfo(TooltipMakerAPI info, ListInfoMode mode) {// the little text you see when you call the function to add intel info to text panel
        info.addTitle("Voices In The Dark", Color.RED);
        info.addPara("Investigate the strange signals that the Onslaught Mk.1 has been sending and recieving.", 0f);
    }

    @Override
    public SectorEntityToken getMapLocation(SectorMapAPI map) {
        return dominatorLocation; // when you open the intel entry and try to go to the target location, brings you to this thing
    }

    @Override
    public boolean isEnded() { // remove the entry upon interaction with the legion
        return playerFoundDominator();
    }

    @Override
    public boolean shouldRemoveIntel() {
        return super.shouldRemoveIntel();
    }

    @Override
    protected String getName() {
        return title;
    }

    @Override
    public String getIcon() {
        return missionIcon; // requires a sprite NAME, not a sprite. string.
    }

    @Override
    public Object getListInfoParam() {
        return ListInfoMode.MESSAGES;
    }

    @Override
    public Set<String> getIntelTags(SectorMapAPI map) {
        Set<String> tags = super.getIntelTags(map);
        tags.add(Tags.INTEL_IMPORTANT);
        tags.add(Tags.INTEL_MISSIONS);
        tags.add(Tags.INTEL_ACCEPTED);
        return tags;
    }

    private boolean playerFoundDominator(){

        if (memory.getBoolean("$nsp_foundDominatorMK1")){

            return true;
        }

        return false;
    }
}
