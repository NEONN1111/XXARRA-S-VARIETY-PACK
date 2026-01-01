package neon.nsp.data.scripts;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.campaign.comm.IntelInfoPlugin;
import com.fs.starfarer.api.impl.campaign.ids.Entities;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import com.fs.starfarer.api.impl.campaign.intel.BaseIntelPlugin;
import com.fs.starfarer.api.ui.SectorMapAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.IntervalUtil;

import java.util.LinkedHashSet;
import java.util.Set;

public class NSP_MotherShipLocationIntel extends BaseIntelPlugin {

    private String sprite = Global.getSettings().getSpriteName("icons", "derelictflag");
    private IntervalUtil check = new IntervalUtil(1f, 1f);

    @Override
    public void createSmallDescription(TooltipMakerAPI info, float width, float height) {
        info.addImage(sprite, width, 5f);
        info.addPara("You are privy to the location of any Explorarium Mothership in the sector, owing to your background.", 5f);
        info.addPara("The following systems contain a Explorarium Mothership;", 10f);

        // Get all custom entities of type DERELICT_MOTHERSHIP
        java.util.List<SectorEntityToken> nexii = new java.util.ArrayList<>();
        SectorAPI sector = Global.getSector();

        for (StarSystemAPI system : sector.getStarSystems()) {
            for (SectorEntityToken entity : system.getCustomEntities()) {
                if (entity.getCustomEntityType() != null &&
                        entity.getCustomEntityType().equals(Entities.DERELICT_MOTHERSHIP)) {
                    nexii.add(entity);
                }
            }
        }

        for (SectorEntityToken entity : nexii) {
            StarSystemAPI starSystem = entity.getStarSystem();
            if (starSystem.isInConstellation()) {
                String constName = starSystem.getConstellation().getName();
                if (constName.startsWith("The")) {
                    info.addPara(starSystem.getName() + ", in " + constName, 10f)
                            .setHighlight(starSystem.getName());
                } else {
                    info.addPara(starSystem.getName() + ", in the " + constName, 10f)
                            .setHighlight(starSystem.getName());
                }
            } else {
                info.addPara(starSystem.getName(), 10f)
                        .setHighlight(starSystem.getName());
            }
        }

        info.addPara("You may repair and restock at any Explorarium Mothership by docking and opening a comm link.", 5f);
    }

    @Override
    public String getIcon() {
        return sprite;
    }

    @Override
    public void advanceImpl(float amount) {
        check.advance(amount);

        if (check.intervalElapsed()) {
            // Check if player has enough fleet points and ds_nexusHostilityIntel doesn't exist
            if (!Global.getSector().getIntelManager().hasIntelOfClass(NSP_MotherShipHostilityIntel.class) &&
                    Global.getSector().getPlayerFleet().getFleetPoints() >= 200) {
                Global.getSector().getIntelManager().addIntel(new NSP_MotherShipHostilityIntel(), false);
            }
            // Check if ds_nexusHostilityIntel exists but isn't a transient script
            else if (Global.getSector().getIntelManager().hasIntelOfClass(NSP_MotherShipHostilityIntel.class) &&
                    !Global.getSector().hasTransientScript(NSP_MotherShipHostilityIntel.class)) {
                Global.getSector().addTransientScript(new NSP_MotherShipHostilityIntel());
            }
        }
    }

    @Override
    public boolean isHidden() {
        return false;
    }

    @Override
    public boolean autoAddCampaignMessage() {
        return true;
    }

    @Override
    public String getCommMessageSound() {
        return getSoundMajorPosting();
    }

    @Override
    public Object getListInfoParam() {
        return IntelInfoPlugin.ListInfoMode.MESSAGES;
    }

    @Override
    public boolean isImportant() {
        return true;
    }

    @Override
    public boolean shouldRemoveIntel() {
        return false;
    }

    @Override
    public IntelInfoPlugin.IntelSortTier getSortTier() {
        return IntelInfoPlugin.IntelSortTier.TIER_1;
    }

    @Override
    public Set<String> getIntelTags(SectorMapAPI map) {
        Set<String> tags = super.getIntelTags(map);
        if (tags == null) {
            tags = new LinkedHashSet<String>();
        }
        tags.add(Tags.INTEL_IMPORTANT);
        return tags;
    }

    @Override
    public String getName() {
        return "Explorarium Network";
    }
}