package neon.nsp.data.plugins;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.AICoreOfficerPlugin;
import com.fs.starfarer.api.campaign.econ.CommoditySpecAPI;
import com.fs.starfarer.api.characters.FullName;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.impl.campaign.BaseAICoreOfficerPluginImpl;
import com.fs.starfarer.api.impl.campaign.ids.Personalities;
import com.fs.starfarer.api.impl.campaign.ids.Ranks;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.campaign.econ.Industry;

import java.awt.*;
import java.util.Random;

import static com.fs.starfarer.api.impl.campaign.AICoreOfficerPluginImpl.*;

public class NSP_ThreatProcessor extends BaseAICoreOfficerPluginImpl implements AICoreOfficerPlugin{

    public PersonAPI createPerson(String aiCoreId, String factionId, Random random) {
        if (random == null) {
            new Random();
        }
        PersonAPI person = Global.getFactory().createPerson();
        person.setFaction(factionId);
        person.setAICoreId(aiCoreId);
        person.setId("nsp_threat_processor");
        boolean ThreatProcessor = "nsp_threat_processor".equals(aiCoreId);
        CommoditySpecAPI spec = Global.getSettings().getCommoditySpec(aiCoreId);
        person.getStats().setSkipRefresh(true);
        person.setName(new FullName(spec.getName(), "", FullName.Gender.ANY));
        int points = 0;
        float mult = 1.0F;
        if (ThreatProcessor) {
            person.getStats().setLevel(1);
            person.getStats().setSkillLevel("damage_control", 2.0F);
            person.setPortraitSprite(Global.getSettings().getSpriteName("characters", "threat"));

            points = GAMMA_POINTS;
            mult = GAMMA_MULT;
        }
        person.getMemoryWithoutUpdate().set("$autoPointsMult", mult);

        person.setPersonality(Personalities.RECKLESS);
        person.setRankId(Ranks.SPACE_CAPTAIN);
        person.setPostId((String)null);
        person.getStats().setSkipRefresh(false);
        return person;
    }
    @Override
    public void createPersonalitySection(PersonAPI person, TooltipMakerAPI tooltip) {
        float opad = 10.0F;
        Color text = person.getFaction().getBaseUIColor();
        Color bg = person.getFaction().getDarkUIColor();
        CommoditySpecAPI spec = Global.getSettings().getCommoditySpec(person.getAICoreId());
        if (spec.getId().equals("nsp_threat_processor")) {
            tooltip.addSectionHeading("Personality: DELUSE", text, bg, Alignment.MID, 20.0F);
            tooltip.addPara("In combat, the " + spec.getName() + " is single-minded, and struggles with complex maneuvers", opad);
        }
    }
    public boolean isInstallable(Industry industry) {
        return false; // probably doesn't work
    }
}