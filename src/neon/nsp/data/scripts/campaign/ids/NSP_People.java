package neon.nsp.data.scripts.campaign.ids;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.PersonImportance;
import com.fs.starfarer.api.campaign.RepLevel;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.characters.FullName.Gender;
import com.fs.starfarer.api.characters.ImportantPeopleAPI;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.impl.campaign.ids.*;
import com.fs.starfarer.api.impl.campaign.missions.hub.BaseMissionHub;
import com.fs.starfarer.api.loading.Description;
import com.fs.starfarer.api.util.Misc;

import static com.fs.starfarer.api.campaign.AICoreOfficerPlugin.AUTOMATED_POINTS_MULT;
import static com.fs.starfarer.api.impl.campaign.AICoreOfficerPluginImpl.*;

/**
 *	Defines and tracks all of SoTF's important, persistent characters (anyone who has a specific ID to themselves)
 */

public class NSP_People {

    // Outrider-Affix-Courser
    public static String INVICTA = "nsp_invicta_core";


    public static PersonAPI getPerson(String id) {
        return Global.getSector().getImportantPeople().getPerson(id);
    }
}