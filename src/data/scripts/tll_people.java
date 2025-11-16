package data.scripts;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.characters.FullName;
import com.fs.starfarer.api.characters.ImportantPeopleAPI;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.impl.campaign.ids.*;

public class tll_people {

    public static void create() {
        createTLLPeople();
    }

    public static PersonAPI createTLLPeople() {
        ImportantPeopleAPI ip = Global.getSector().getImportantPeople();

        MarketAPI market1 = Global.getSector().getEconomy().getMarket("tll_tellas");
        if (market1 != null) {
            PersonAPI admin1 = Global.getFactory().createPerson();
            admin1.setId("tll_providence");
            admin1.setFaction("tll");
            admin1.setGender(FullName.Gender.MALE);
            admin1.setPostId(Ranks.POST_FACTION_LEADER);
            admin1.setRankId(Ranks.SPACE_ADMIRAL);
            admin1.setImportance(PersonImportance.VERY_HIGH);
            admin1.getName().setFirst("Providence");
            admin1.getName().setLast("Redemptor");
            admin1.setPortraitSprite("graphics/portraits/portrait_ai2b.png");
            admin1.getStats().setSkillLevel(Skills.INDUSTRIAL_PLANNING, 1);
            admin1.getStats().setSkillLevel(Skills.HYPERCOGNITION, 1);
            ip.addPerson(admin1);
            market1.setAdmin(admin1);
            market1.getCommDirectory().addPerson(admin1, 0);
            market1.addPerson(admin1);

            PersonAPI admiral1 = Global.getSector().getFaction("tll").createRandomPerson(FullName.Gender.ANY);
            admiral1.setId("tll_admiral1");
            admiral1.setPostId(Ranks.POST_OFFICER);
            admiral1.setRankId(Ranks.SPACE_ADMIRAL);
            admiral1.setPersonality(Personalities.AGGRESSIVE);
            admiral1.getStats().setSkillLevel(Skills.COMBAT_ENDURANCE, 2);
            admiral1.getStats().setSkillLevel(Skills.MISSILE_SPECIALIZATION, 2);
            admiral1.getStats().setSkillLevel(Skills.TARGET_ANALYSIS, 2);
            admiral1.getStats().setSkillLevel(Skills.FIELD_MODULATION, 2);
            admiral1.getStats().setSkillLevel(Skills.IMPACT_MITIGATION, 2);
            admiral1.getStats().setSkillLevel(Skills.HELMSMANSHIP, 2);
            admiral1.getStats().setSkillLevel(Skills.DAMAGE_CONTROL, 2);
            admiral1.getStats().setSkillLevel(Skills.CREW_TRAINING, 1);
            admiral1.getStats().setSkillLevel(Skills.SUPPORT_DOCTRINE, 1);
            admiral1.getStats().setLevel(9);
            ip.addPerson(admiral1);

        }


        MarketAPI market2 = Global.getSector().getEconomy().getMarket("tll_vokha");
        if (market2 != null) {
            PersonAPI admin2 = Global.getFactory().createPerson();
            admin2.setId("tll_vokha_admin");
            admin2.setFaction("tll");
            admin2.getName().setFirst("Providence");
            admin2.getName().setLast("Redemptor");
            admin2.setPortraitSprite("graphics/portraits/portrait_ai2b.png");
            admin2.setGender(FullName.Gender.MALE);
            admin2.setPostId(Ranks.POST_FACTION_LEADER);
            admin2.setRankId(Ranks.FACTION_LEADER);
            admin2.setImportance(PersonImportance.VERY_HIGH);
            admin2.getStats().setSkillLevel(Skills.INDUSTRIAL_PLANNING, 1);
            admin2.getStats().setSkillLevel(Skills.HYPERCOGNITION, 1);
            ip.addPerson(admin2);
            market2.setAdmin(admin2);
            market2.getCommDirectory().addPerson(admin2, 0);
            market2.addPerson(admin2);
        }

        MarketAPI market3 = Global.getSector().getEconomy().getMarket("tll_nisse");
        if (market3 != null) {
            PersonAPI admin3 = Global.getFactory().createPerson();
            admin3.setId("tll_nisse_admin");
            admin3.setFaction("tll");
            admin3.getName().setFirst("Providence");
            admin3.getName().setLast("Redemptor");
            admin3.setPortraitSprite("graphics/portraits/portrait_ai2b.png");
            admin3.setGender(FullName.Gender.MALE);
            admin3.setPostId(Ranks.POST_FACTION_LEADER);
            admin3.setRankId(Ranks.FACTION_LEADER);
            admin3.setImportance(PersonImportance.VERY_HIGH);
            admin3.getStats().setSkillLevel(Skills.INDUSTRIAL_PLANNING, 1);
            admin3.getStats().setSkillLevel(Skills.HYPERCOGNITION, 1);
            ip.addPerson(admin3);
            market3.setAdmin(admin3);
            market3.getCommDirectory().addPerson(admin3, 0);
            market3.addPerson(admin3);
        }

        MarketAPI market4 = Global.getSector().getEconomy().getMarket("tll_pyyhrus");
        if (market4 != null) {
            PersonAPI admin4 = Global.getFactory().createPerson();
            admin4.setId("tll_pyyhrus_admin");
            admin4.setFaction("tll");
            admin4.getName().setFirst("Providence");
            admin4.getName().setLast("Redemptor");
            admin4.setPortraitSprite("graphics/portraits/portrait_ai2b.png");
            admin4.setGender(FullName.Gender.MALE);
            admin4.setPostId(Ranks.POST_FACTION_LEADER);
            admin4.setRankId(Ranks.FACTION_LEADER);
            admin4.setImportance(PersonImportance.VERY_HIGH);
            admin4.getStats().setSkillLevel(Skills.INDUSTRIAL_PLANNING, 1);
            admin4.getStats().setSkillLevel(Skills.HYPERCOGNITION, 1);
            ip.addPerson(admin4);
            market4.setAdmin(admin4);
            market4.getCommDirectory().addPerson(admin4, 0);
            market4.addPerson(admin4);
        }


        MarketAPI market5 = Global.getSector().getEconomy().getMarket("tll_brigid");
        if (market5 != null) {
            PersonAPI admin5 = Global.getFactory().createPerson();
            admin5.setId("tll_brigid_admin");
            admin5.setFaction("tll");
            admin5.getName().setFirst("Providence");
            admin5.getName().setLast("Redemptor");
            admin5.setPortraitSprite("graphics/portraits/portrait_ai2b.png");
            admin5.setGender(FullName.Gender.MALE);
            admin5.setPostId(Ranks.POST_FACTION_LEADER);
            admin5.setRankId(Ranks.FACTION_LEADER);
            admin5.setImportance(PersonImportance.VERY_HIGH);
            admin5.getStats().setSkillLevel(Skills.INDUSTRIAL_PLANNING, 1);
            admin5.getStats().setSkillLevel(Skills.HYPERCOGNITION, 1);
            ip.addPerson(admin5);
            market5.setAdmin(admin5);
            market5.getCommDirectory().addPerson(admin5, 0);
            market5.addPerson(admin5);
        }

        MarketAPI market6 = Global.getSector().getEconomy().getMarket("tll_vosta");
        if (market6 != null) {
            PersonAPI admin6 = Global.getFactory().createPerson();
            admin6.setId("tll_vosta_admin");
            admin6.setFaction("tll");
            admin6.getName().setFirst("Providence");
            admin6.getName().setLast("Redemptor");
            admin6.setPortraitSprite("graphics/portraits/portrait_ai2b.png");
            admin6.setGender(FullName.Gender.MALE);
            admin6.setPostId(Ranks.POST_FACTION_LEADER);
            admin6.setRankId(Ranks.FACTION_LEADER);
            admin6.setImportance(PersonImportance.VERY_HIGH);
            admin6.getStats().setSkillLevel(Skills.INDUSTRIAL_PLANNING, 1);
            admin6.getStats().setSkillLevel(Skills.HYPERCOGNITION, 1);
            ip.addPerson(admin6);
            market6.setAdmin(admin6);
            market6.getCommDirectory().addPerson(admin6, 0);
            market6.addPerson(admin6);
        }

        MarketAPI market7 = Global.getSector().getEconomy().getMarket("tll_amphitrite");
        if (market7 != null) {
            PersonAPI admin7 = Global.getFactory().createPerson();
            admin7.setId("tll_amphitrite_admin");
            admin7.setFaction("tll");
            admin7.getName().setFirst("Providence");
            admin7.getName().setLast("Redemptor");
            admin7.setPortraitSprite("graphics/portraits/portrait_ai2b.png");
            admin7.setGender(FullName.Gender.MALE);
            admin7.setPostId(Ranks.POST_FACTION_LEADER);
            admin7.setRankId(Ranks.FACTION_LEADER);
            admin7.setImportance(PersonImportance.VERY_HIGH);
            admin7.getStats().setSkillLevel(Skills.INDUSTRIAL_PLANNING, 1);
            admin7.getStats().setSkillLevel(Skills.HYPERCOGNITION, 1);
            ip.addPerson(admin7);
            market7.setAdmin(admin7);
            market7.getCommDirectory().addPerson(admin7, 0);
            market7.addPerson(admin7);
        }
        return null;
    }
}