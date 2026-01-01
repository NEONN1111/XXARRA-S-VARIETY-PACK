package neon.nsp.data.plugins;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.AICoreOfficerPlugin;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.impl.campaign.BaseAICoreOfficerPluginImpl;
import com.fs.starfarer.api.impl.campaign.ids.Commodities;
import com.fs.starfarer.api.impl.campaign.ids.Personalities;
import com.fs.starfarer.api.impl.campaign.ids.Skills;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;

import java.awt.Color;
import java.util.Random;

public class NSP_PlayerCorePlugin extends BaseAICoreOfficerPluginImpl implements AICoreOfficerPlugin {

    @Override
    public PersonAPI createPerson(String aiCoreId, String factionId, Random random) {
        if ("ds_core_delicious".equals(aiCoreId)) {
            return makeTheGuy(factionId);
        } else if ("ds_playercore".equals(aiCoreId)) {
            return Global.getSector().getPlayerPerson();
        }
        return null;
    }

    private PersonAPI makeTheGuy(String factionId) {
        AICoreOfficerPlugin plugin = Misc.getAICoreOfficerPlugin(Commodities.GAMMA_CORE);
        PersonAPI person = plugin.createPerson("ds_core_delicious", factionId, Misc.random);

        person.getStats().setLevel(1);
        person.getMemoryWithoutUpdate().set("$ds_delicious", true);
        person.getMemoryWithoutUpdate().set("$chatterChar", "freeborn");
        person.getStats().setSkillLevel(Skills.COMBAT_ENDURANCE, 2.0f);

        // Note: OfficerManagerEvent.pickSkill is commented out in the Kotlin version
        // If you need it, you'll have to check if it's available in JavaScript

        return person;
    }

    @Override
    public void createPersonalitySection(PersonAPI person, TooltipMakerAPI tooltip) {
        float opad = 10.0f;
        Color text = person.getFaction().getBaseUIColor();
        Color bg = person.getFaction().getDarkUIColor();

        tooltip.addPara("A somewhat primitive artificial intelligence built into the ship.", 5.0f).setAlignment(Alignment.MID);
        tooltip.addPara("While not quite as competent in combat as an AI core to start, they require no specialized maintenance and have a great learning capacity.", 5.0f);
        tooltip.addPara("May level up after combat and gain a random skill, up to level 6.", 5.0f).setHighlight("level 6.");

        tooltip.addSectionHeading("Personality: " + Misc.getPersonalityName(person), text, bg, Alignment.MID, 20.0f);

        String personalityId = person.getPersonalityAPI().getId();
        if (Personalities.RECKLESS.equals(personalityId)) {
            tooltip.addPara("In combat, this AI is single-minded and determined. " +
                    "In a human captain, their traits might be considered reckless. In a machine, they're terrifying.", opad);
        } else if (Personalities.AGGRESSIVE.equals(personalityId)) {
            tooltip.addPara("In combat, this AI will prefer to engage at a range that allows the use of " +
                    "all of their ship's weapons and will employ any fighters under their command aggressively.", opad);
        } else if (Personalities.STEADY.equals(personalityId)) {
            tooltip.addPara("In combat, this AI will favor a balanced approach with " +
                    "tactics matching the current situation.", opad);
        } else if (Personalities.CAUTIOUS.equals(personalityId)) {
            tooltip.addPara("In combat, this AI will prefer to stay out of enemy range, " +
                    "only occasionally moving in if out-ranged by the enemy.", opad);
        } else if (Personalities.TIMID.equals(personalityId)) {
            tooltip.addPara("In combat, this AI will attempt to avoid direct engagements if at all " +
                    "possible, even if commanding a combat vessel.", opad);
        }
    }


    public boolean isInstallable(com.fs.starfarer.api.campaign.econ.Industry industry) {
        // This method is required by the interface
        return true; // Or false based on your requirements
    }
}