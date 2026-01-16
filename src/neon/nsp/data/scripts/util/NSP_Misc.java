// miscellaneous methods that I want to be using about the place
// script originally from secrets of the frontier, used and edited with permission. all credits to original author
package neon.nsp.data.scripts.util;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.characters.SkillSpecAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.DerelictShipEntityPlugin;
import com.fs.starfarer.api.impl.campaign.events.OfficerManagerEvent;
import com.fs.starfarer.api.impl.campaign.fleets.FleetFactoryV3;
import com.fs.starfarer.api.impl.campaign.ids.*;
import com.fs.starfarer.api.impl.campaign.procgen.themes.BaseThemeGenerator;
import com.fs.starfarer.api.impl.campaign.procgen.themes.SalvageSpecialAssigner;
import com.fs.starfarer.api.impl.campaign.rulecmd.salvage.special.ShipRecoverySpecial;
import com.fs.starfarer.api.impl.hullmods.Automated;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.api.util.WeightedRandomPicker;
import neon.nsp.data.plugins.NSPModPlugin;
import neon.nsp.data.plugins.NSP_InvictaCore;
import neon.nsp.data.scripts.campaign.ids.NSP_IDs;
import neon.nsp.data.scripts.campaign.ids.NSP_People;
import lunalib.lunaSettings.LunaSettings;
import org.lazywizard.lazylib.combat.DefenseUtils;
import second_in_command.SCUtils;

import java.awt.*;
import java.util.*;

import static com.fs.starfarer.api.characters.MutableCharacterStatsAPI.SkillLevelAPI;
import static com.fs.starfarer.api.impl.campaign.procgen.themes.BaseThemeGenerator.*;

public class NSP_Misc {


    // pickHiddenLocation but without dumb "far reaches" spawns


    public static MarketAPI pickNPCMarket(String factionId) {
        boolean allowSize3 = true;
        for (MarketAPI prospective : Global.getSector().getEconomy().getMarketsCopy()) {
            if (prospective.getFactionId().equals(factionId) && !prospective.isHidden() && prospective.getSize() > 4) {
                allowSize3 = false;
            }
        }

        WeightedRandomPicker<MarketAPI> picker = new WeightedRandomPicker<>();
        for (MarketAPI prospective : Global.getSector().getEconomy().getMarketsCopy()) {
            if (prospective.getFactionId().equals(factionId) && !prospective.isHidden()) {
                if (!allowSize3 && prospective.getSize() <= 3) continue;
                picker.add(prospective, prospective.getSize() * prospective.getSize());
            }
        }
        return picker.pick();
    }

    // check if player has Sierra in their fleet
    //  public static boolean playerHasNoAutoPenaltyShip() {
    //      boolean has = false;
    //      if (Global.getSector().getPlayerFleet() != null) {
    //          for (FleetMemberAPI member : Global.getSector().getPlayerFleet().getFleetData().getMembersListCopy()) {
    //              if (Misc.isAutomated(member) && Automated.isAutomatedNoPenalty(member)) {
    //                  return true;
    //              }
    //          }
    //      }
    //      return has;
    //  }

    // check if player has Sierra in their fleet
    // public static boolean playerHasInvicta() {
    //     boolean invicta = false;
    //     if (Global.getSector().getPlayerFleet() != null) {
    //          if (Global.getSector().getPlayerFleet().getCargo().getCommodityQuantity(NSP_IDs.INVICTA_CORE); {
    //              return true;
    //          }
    //     }
    //     return invicta;
    // }

    // checks if player has a Concord ship without Sierra


    // returns the empty variant of Sierra's current ship class - Pledge or Vow
    public static String getSierraVariant() {
        return Global.getSector().getMemoryWithoutUpdate().getString("$nsp_invicta_var") + "_Hull";
    }


    // makes player able to ask Sierra for her thoughts if it was on cooldown
    public static void setInvictaHasThoughts() {
        Global.getSector().getMemoryWithoutUpdate().set("$InvictaNoThoughts", false);
    }


    // retrieve player Guilt score excluding any bonus guilt

    // increase player Guilt score

    // turns a gamma/beta/alpha into an equivalent-level Dustkeeper instance
    public static void derelictifyAICore(PersonAPI person, String forcedPrefix, String forcedInfex, String forcedSuffix) {
        PersonAPI temp = null;
        if (Global.getSector() != null) {
            temp = Global.getSector().getFaction(Factions.DERELICT).createRandomPerson();
        } else {
            temp = Global.getSettings().createBaseFaction(Factions.DERELICT).createRandomPerson();
        }
        if (temp == null) return;
        person.setPersonality(temp.getFaction().pickPersonality());
        person.setPortraitSprite(temp.getPortraitSprite()); // override portrait

        // override AI core ID
        if (person.getAICoreId() != null) {
            switch (person.getAICoreId()) {
                case "gamma_core":
                    person.setAICoreId(NSP_IDs.GAMMA_CORE_NSP);
                    person.setRankId(Ranks.SPACE_LIEUTENANT); // Sliver
                    break;
                case "beta_core":
                    person.setAICoreId(NSP_IDs.BETA_CORE_NSP);
                    person.setRankId(Ranks.SPACE_CAPTAIN); // Echo
                    break;
                case "alpha_core":
                    person.setAICoreId(NSP_IDs.ALPHA_CORE_NSP);
                    person.setRankId(Ranks.SPACE_COMMANDER); // Annex
                    break;
            }
        }
        giveDustkeeperName(person, forcedPrefix, forcedInfex, forcedSuffix);
    }

    public static void derelictifyAICore(PersonAPI person) {
        derelictifyAICore(person, null, null, null);
    }

    // give a PersonAPI a Dustkeeper instance name (e.g Halfway-Echo-Sentiment) with optional fixed sections
    public static void giveDustkeeperName(PersonAPI person, String forcedPrefix, String forcedInfex, String forcedSuffix) {
        PersonAPI temp = null;
        if (Global.getSector() != null) {
            temp = Global.getSector().getFaction(Factions.DERELICT).createRandomPerson();
        } else {
            temp = Global.getSettings().createBaseFaction(Factions.DERELICT).createRandomPerson();
        }
        String prefix = forcedPrefix;
        String infex = forcedInfex; // yes it's called an infex, neat huh?
        String suffix = forcedSuffix;
        if (prefix == null) {
            prefix = temp.getName().getFirst();
        }
        if (infex == null) {
            switch (person.getRankId()) {
                case "spaceSailor":
                    infex = "Unpurposed";
                    break;
                case "spaceLieutenant":
                    infex = "Placeholder";
                    break;
                case "spaceCaptain":
                    infex = "Placeholder";
                    break;
                case "spaceCommander":
                    infex = "Placeholder";
                    break;
                case "spaceAdmiral":
                    infex = "Placeholder";
                    break;
                default:
                    infex = "Placeholder";
                    break;
            }
        }
        if (suffix == null) {
            suffix = temp.getName().getLast();
        }
        person.getMemoryWithoutUpdate().set("$nsp_prefix", prefix);
        person.getMemoryWithoutUpdate().set("$nsp_suffix", suffix);
        person.getName().setFirst(prefix + "-" + infex + "-" + suffix); // e.g Index-Annex-Optimum
        person.getName().setLast("");
    }

    /**
     * Assigns a suitable set of combat skills to an AI core officer for a defined ship
     * Respects their faction's priority skills and ignores "special" skills that can't be reassigned.
     * (shouldn't be used for officers who are not expected to have all-elite skills)
     *
     * @param person Person to undergo skill reassignment
     * @param member Ship they will be commanding
     * @param fleet  Their fleet
     * @param random RNG to use
     */
    public static void reassignAICoreSkills(PersonAPI person, FleetMemberAPI member, CampaignFleetAPI fleet, Random random) {
        person.getStats().setSkipRefresh(true);
        OfficerManagerEvent.SkillPickPreference pref = FleetFactoryV3.getSkillPrefForShip(member);

        // count how many normal combat skills the officer has (if NPC-only, assume it's a special skill like CWAR or Derelict Contingent)
        int targetLevel = 0;
        for (SkillLevelAPI skillLevel : person.getStats().getSkillsCopy()) {
            if (skillLevel.getSkill().isCombatOfficerSkill() && !skillLevel.getSkill().hasTag(Skills.TAG_NPC_ONLY)) {
                targetLevel++;
            }
        }

        // generate an officer from their faction with that many skills
        PersonAPI temp = OfficerManagerEvent.createOfficer(
                person.getFaction(),
                targetLevel,
                pref, true, fleet, true, true, targetLevel, random);

        // get a list of that officer's skills
        ArrayList<SkillSpecAPI> skillsToHave = new ArrayList<>();
        for (SkillLevelAPI skillLevel : temp.getStats().getSkillsCopy()) {
            if (skillLevel.getSkill().isCombatOfficerSkill()) {
                skillsToHave.add(skillLevel.getSkill());
            }
        }

        // if the person lacks any of those skills, give them to them
        for (SkillSpecAPI skill : skillsToHave) {
            if (!person.getStats().hasSkill(skill.getId())) {
                person.getStats().setSkillLevel(skill.getId(), 2);
            }
        }

        // shed any skills that temp officer didn't have
        for (SkillLevelAPI skillLevel : person.getStats().getSkillsCopy()) {
            if (skillLevel.getSkill().isCombatOfficerSkill() && !skillLevel.getSkill().hasTag(Skills.TAG_NPC_ONLY) && !skillsToHave.contains(skillLevel.getSkill())) {
                person.getStats().setSkillLevel(skillLevel.getSkill().getId(), 0);
            }
        }
        person.getStats().setSkipRefresh(false);
        person.getStats().refreshCharacterStatsEffects();
    }

    public static String glitchify(String string, float glitchChance) {
        StringBuilder text = new StringBuilder();
        for (char character : string.toCharArray()) {
            if (character != ' ' && character != '-' && character != ':' && Misc.random.nextFloat() < glitchChance) {
                text.append("#");
            } else {
                text.append(character);
            }
        }
        return text.toString();
    }

    public static void ensurePlayerKnowsShip(String id) {
        if (!Global.getSector().getPlayerFaction().knowsShip(id)) {
            Global.getSector().getPlayerFaction().addKnownShip(id, false);
        }
    }

    public static void ensurePlayerKnowsWeapon(String id) {
        if (!Global.getSector().getPlayerFaction().knowsWeapon(id)) {
            Global.getSector().getPlayerFaction().addKnownWeapon(id, false);
        }
    }

    public static void ensurePlayerKnowsFighter(String id) {
        if (!Global.getSector().getPlayerFaction().knowsFighter(id)) {
            Global.getSector().getPlayerFaction().addKnownFighter(id, false);
        }
    }

    /**
     * Checks if a ship has a standalone blueprint or is in a (commonish) blueprint package
     *
     * @param spec Ship hull to check
     * @return true if has blueprint, false otherwise
     */
    public static boolean shipHasBlueprint(ShipHullSpecAPI spec) {
        if (spec.hasTag("base_bp")) return true;
        if (spec.hasTag("rare_bp")) return true;
        for (SpecialItemSpecAPI special : Global.getSettings().getAllSpecialItemSpecs()) {
            if (special.getRarity() < 0.1) continue;
            if (special.hasTag("package_bp") && !special.getParams().isEmpty()) {
                for (String tag : special.getParams().split(",")) {
                    tag = tag.trim();
                    if (tag.isEmpty()) continue;
                    if (spec.hasTag(tag)) return true;
                }
            }
        }
        if (Global.getSector() != null) {
            for (FactionAPI faction : Global.getSector().getAllFactions()) {
                if (!faction.isShowInIntelTab()) continue;
                if (faction.knowsShip(spec.getHullId())) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Picks one string from the provided list
     *
     * @param options
     * @return
     */
    public static String pickOne(String... options) {
        WeightedRandomPicker<String> picker = new WeightedRandomPicker<String>(Misc.random);
        for (String option : options) {
            picker.add(option);
        }
        return picker.pick();
    }

    /**
     * Returns one of the given arguments based on the target's hull size
     *
     * @param ship      Ship whose size class is checked
     * @param frigate
     * @param destroyer
     * @param cruiser
     * @param capital
     * @return
     */
    public static Object forShipsHullSize(ShipAPI ship, Object frigate, Object destroyer, Object cruiser, Object capital) {
        switch (ship.getHullSize().ordinal() - 1) {
            case 2:
                return destroyer;
            case 3:
                return cruiser;
            case 4:
                return capital;
            default:
                return frigate;
        }
    }

    /**
     * Returns one of the given arguments based on the target's hull size
     *
     * @param size      Hull size in question
     * @param frigate
     * @param destroyer
     * @param cruiser
     * @param capital
     * @return
     */
    public static Object forHullSize(ShipAPI.HullSize size, Object frigate, Object destroyer, Object cruiser, Object capital) {
        switch (size.ordinal() - 1) {
            case 2:
                return destroyer;
            case 3:
                return cruiser;
            case 4:
                return capital;
            default:
                return frigate;
        }
    }

}