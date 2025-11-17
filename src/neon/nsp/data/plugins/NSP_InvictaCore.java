package neon.nsp.data.plugins;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.AICoreOfficerPlugin;
import com.fs.starfarer.api.campaign.econ.CommoditySpecAPI;
import com.fs.starfarer.api.characters.*;
import com.fs.starfarer.api.impl.campaign.BaseAICoreOfficerPluginImpl;
import com.fs.starfarer.api.impl.campaign.ids.Personalities;
import com.fs.starfarer.api.impl.campaign.ids.Ranks;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.campaign.econ.Industry;

import java.awt.*;
import java.util.Random;

import static com.fs.starfarer.api.impl.campaign.AICoreOfficerPluginImpl.*;

public class NSP_InvictaCore extends BaseAICoreOfficerPluginImpl implements AICoreOfficerPlugin{


    public PersonAPI createPerson(String aiCoreId, String factionId, Random random) {
        if (random == null) {
            new Random();
        }
        PersonAPI person = Global.getFactory().createPerson();
        person.setId("nsp_invicta_core");
        person.setAICoreId(aiCoreId);
        person.setFaction(factionId);
        boolean InvictaCore = "nsp_invicta_core".equals(aiCoreId);
        CommoditySpecAPI spec = Global.getSettings().getCommoditySpec(aiCoreId);
        person.getStats().setSkipRefresh(true);
        person.setName(new FullName(spec.getName(), "", FullName.Gender.ANY));
        int points = 0;
        float mult = 1.0F;
        if (InvictaCore) {
            person.getStats().setLevel(9);
            person.getStats().setSkillLevel("damage_control", 2.0F);
            person.getStats().setSkillLevel("target_analysis", 2.0F);
            person.getStats().setSkillLevel("impact_mitigation", 2.0F);
            person.getStats().setSkillLevel("gunnery_implants", 2.0F);
            person.getStats().setSkillLevel("combat_endurance", 2.0F);
            person.getStats().setSkillLevel("point_defense", 2.0F);
            person.getStats().setSkillLevel("ballistic_mastery", 2.0F);
            person.getStats().setSkillLevel("systems_expertise", 2.0F);
            person.getStats().setSkillLevel(nsp_Skills.NSP_WEIRDSLAYER, 2.0F);
            person.setPortraitSprite(Global.getSettings().getSpriteName("characters", "Invicta"));

            points = ALPHA_POINTS;
            mult = ALPHA_MULT;
        }
        person.getMemoryWithoutUpdate().set("$autoPointsMult", mult);

        person.setPersonality(Personalities.RECKLESS);
        person.setRankId(Ranks.FACTION_LEADER);
        person.setPostId(Ranks.POST_FACTION_LEADER);
        person.getStats().setSkipRefresh(false);

        Global.getSector().getImportantPeople().addPerson(person);
        return person;
    }
    @Override
    public void createPersonalitySection(PersonAPI person, TooltipMakerAPI tooltip) {
        float opad = 10.0F;
        Color text = person.getFaction().getBaseUIColor();
        Color bg = person.getFaction().getDarkUIColor();
     CommoditySpecAPI spec = Global.getSettings().getCommoditySpec(person.getAICoreId());
     if (spec.getId().equals("nsp_invicta_core")) {
         tooltip.addSectionHeading("Personality: Steadfast", text, bg, Alignment.MID, 20.0F);
        tooltip.addPara("In combat, " + spec.getName() + "is single-minded and obsessive, stopping at nothing to annihilate the enemies of the Domain.", opad);
   }
    }
    public boolean isInstallable(Industry industry) {
        return false; // probably doesn't work
    }


}

