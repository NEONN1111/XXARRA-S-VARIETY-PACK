package neon.nsp.data.ids;

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
import com.fs.starfarer.api.util.WeightedRandomPicker;

import static neon.nsp.data.ids.NSP_Ranks.POST_DETECTIVE;

//V: people usually go in ids, by vanilla convention
public class NSPPeople {

    public static String PHOS = "nsp_phos";

    //Exponent quest people
    public static String EXQ_KNIGHT = "exponent_KnightContact";
    public static String EXQ_INVICTUS_LEADER = "exponent_KnightContact";
    public static String EXQ_LUDDIC_FLEET_COMMANDER = "exponent_KnightContact";
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

        //Pre create them here so they can be called to before mission is started
        if (getPerson(EXQ_KNIGHT) == null) {
            //They are semi random including name and portrait
            PersonAPI person = Global.getSector().getFaction(Factions.LUDDIC_CHURCH).createRandomPerson();
            person.setId(EXQ_KNIGHT);
            person.setFaction(Factions.KOL);
            person.setRankId(Ranks.KNIGHT_CAPTAIN);
            person.setPostId(Ranks.POST_EXCUBITOR_ORBIS);

            if (person.getGender() == FullName.Gender.FEMALE) {
                WeightedRandomPicker<String> knightPortraitsFemale = new WeightedRandomPicker<>();
                knightPortraitsFemale.add("graphics/portraits/portrait_luddic07.png");
                knightPortraitsFemale.add("graphics/portraits/portrait_luddic10.png");
                knightPortraitsFemale.add("graphics/portraits/portrait_luddic11.png");
                String pick = knightPortraitsFemale.pick();
                person.setPortraitSprite(pick);
            } else {
                WeightedRandomPicker<String> knightPortraitsMale = new WeightedRandomPicker<>();
                knightPortraitsMale.add("graphics/portraits/portrait_luddic02.png");
                knightPortraitsMale.add("graphics/portraits/portrait_luddic05.png");
                knightPortraitsMale.add("graphics/portraits/portrait_luddic06.png");
                knightPortraitsMale.add("graphics/portraits/portrait_luddic09.png");
                knightPortraitsMale.add("graphics/portraits/portrait_luddic13.png");
                knightPortraitsMale.add("graphics/portraits/portrait_luddic15.png");
                String pick = knightPortraitsMale.pick();
                person.setPortraitSprite(pick);
            }
            person.setImportance(PersonImportance.HIGH);

            //person.setMarket(createdAt);
            //Will be added to market with rulesCMD or not added at all.
            ip.addPerson(person);
        }

    }
}
