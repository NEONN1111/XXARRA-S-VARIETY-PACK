package neon.nsp.data.scripts;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.PersonImportance;
import com.fs.starfarer.api.characters.FullName;
import com.fs.starfarer.api.characters.ImportantPeopleAPI;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import com.fs.starfarer.api.impl.campaign.ids.Personalities;
import com.fs.starfarer.api.impl.campaign.ids.Ranks;

public class NSPPeople {

    public static String PHOS = "nsp_phos";


    public static String EXPONENT_CORE = "exponent_core";


    public static PersonAPI getPerson(String id){
        return Global.getSector().getImportantPeople().getPerson(id);
    }

    public void nsp_createPeople(){
        ImportantPeopleAPI ip = Global.getSector().getImportantPeople();
        if (getPerson(PHOS) == null){
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
            ImportantPeopleAPI ip2 = Global.getSector().getImportantPeople();
            if (getPerson(EXPONENT_CORE) == null){
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
                ip2.addPerson(exponent);
            }

    }
}
