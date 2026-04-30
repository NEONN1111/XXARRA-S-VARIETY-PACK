package neon.nsp.data.scripts;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.PersonImportance;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.characters.FullName;
import com.fs.starfarer.api.characters.ImportantPeopleAPI;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import com.fs.starfarer.api.impl.campaign.ids.Personalities;
import com.fs.starfarer.api.impl.campaign.ids.Ranks;
import com.fs.starfarer.api.impl.campaign.ids.Skills;

import static neon.nsp.data.scripts.util.NSPRanks.POST_DETECTIVE;

public class NSPPeople {

    public static String PHOS = "nsp_phos";


    public static String EXPONENT_CORE = "exponent_core";

    public static String NSP_THREAT_PROCESSOR = "nsp_threat_processor";

    //For Disco Elysium Mission
    public static String HARRYDISCODUBOIS = "nsp_harry_dubois";

    public static PersonAPI getPerson(String id){
        return Global.getSector().getImportantPeople().getPerson(id);
    }

    public void nsp_createPeople() {
        //Multiple ip is kinda redundant
        //As well as having PersonAPI variable vary for each person...
        ImportantPeopleAPI ip = Global.getSector().getImportantPeople();
        if (getPerson(PHOS) == null) {
            PersonAPI phos = Global.getFactory().createPerson();
            phos.getName().setFirst("Phos");
            phos.getName().setLast("Morganthal");
            phos.setFaction(Factions.INDEPENDENT);
            phos.setImportance(PersonImportance.HIGH);
            phos.setRankId(Ranks.EXECUTIVE);
            phos.setPostId(Ranks.POST_ACADEMICIAN);
            phos.setGender(FullName.Gender.MALE);
            phos.setId(PHOS);
            phos.setPortraitSprite("graphics/portraits/characters/phos_morganthal.png");
            ip.addPerson(phos);
        }
        if (getPerson(EXPONENT_CORE) == null) {
            PersonAPI exponent = Global.getFactory().createPerson();
            exponent.getName().setFirst("VALTEIL");
            exponent.getName().setLast(" ");
            exponent.setFaction("nsp_exponent");
            exponent.setImportance(PersonImportance.LOW);
            exponent.setRankId(Ranks.UNKNOWN);
            exponent.setPostId(Ranks.POST_UNKNOWN);
            exponent.setGender(FullName.Gender.MALE);
            exponent.setId(EXPONENT_CORE);
            exponent.setPortraitSprite("graphics/portraits/characters/exponent_core.png");
            exponent.getStats().setSkillLevel("damage_control", 2.0F);
            exponent.getStats().setSkillLevel("target_analysis", 2.0F);
            exponent.getStats().setSkillLevel("impact_mitigation", 2.0F);
            exponent.getStats().setSkillLevel("gunnery_implants", 2.0F);
            exponent.getStats().setSkillLevel("combat_endurance", 2.0F);
            exponent.getStats().setSkillLevel("point_defense", 2.0F);
            exponent.getStats().setSkillLevel("energy_weapon_mastery", 2.0F);
            exponent.getStats().setSkillLevel("systems_expertise", 2.0F);
            exponent.setPersonality(Personalities.RECKLESS);
            ip.addPerson(exponent);
        }
        if (getPerson(NSP_THREAT_PROCESSOR) == null) {
            PersonAPI threatprocessor = Global.getFactory().createPerson();
            threatprocessor.getName().setFirst("PROCESSING");
            threatprocessor.getName().setLast("UNIT");
            threatprocessor.setFaction("threat");
            threatprocessor.setImportance(PersonImportance.LOW);
            threatprocessor.setRankId(Ranks.UNKNOWN);
            threatprocessor.setPostId(Ranks.POST_UNKNOWN);
            threatprocessor.setGender(FullName.Gender.MALE);
            threatprocessor.setId(NSP_THREAT_PROCESSOR);
            threatprocessor.setPortraitSprite("graphics/portraits/threat.png");
            threatprocessor.getStats().setSkillLevel("damage_control", 2.0F);
            threatprocessor.setPersonality(Personalities.RECKLESS);
            ip.addPerson(threatprocessor);
        }
        if (getPerson(HARRYDISCODUBOIS) == null) {
            PersonAPI person = Global.getFactory().createPerson();
            person.setId(HARRYDISCODUBOIS);
            person.setFaction(Factions.INDEPENDENT);
            person.setRankId("nsp_detective");
            person.setPostId(POST_DETECTIVE);
            person.getName().setFirst("Harry");
            person.getName().setLast("Du Bois");
            person.setGender(FullName.Gender.MALE);
            person.setPortraitSprite("graphics/portraits/characters/harry_dubois.png");

            person.setPersonality(Personalities.AGGRESSIVE);
            person.getStats().setLevel(1);
            /* Skills */
            person.getStats().setSkillLevel(Skills.DAMAGE_CONTROL, 1);

            MarketAPI market =  Global.getSector().getEconomy().getMarket("nsp_revachol_market");
            market.getCommDirectory().addPerson(person, 10);
            market.getCommDirectory().getEntryForPerson(person).setHidden(true);
            market.addPerson(person);

            ip.addPerson(person);
        }
    }
}
