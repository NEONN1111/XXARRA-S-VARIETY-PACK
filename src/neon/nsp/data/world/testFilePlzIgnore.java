package neon.nsp.data.world;

import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.util.Misc;
import org.lazywizard.lazylib.MathUtils;

import java.util.ArrayList;
import java.util.List;

public class testFilePlzIgnore {
    // random old java function I copied from god knows where, takes two arraylists and the sector as input
    // first arraylist contains starsystem IDs, if you really want to avoid specific systems for whatever reason
    // second is more important, contains a blacklist of tags.
    // Important tags to blacklist include SYSTEM_ABYSSAL, STAR_HIDDEN_ON_MAP, SYSTEM_ALREADY_USED_FOR_STORY, and THEME_HIDDEN
    public static StarSystemAPI getRandomSystemWithBlacklist(List<String> blacklist, List<String> tagBlacklist, SectorAPI sector) {
        //First, get all the valid systems and put them in a separate list
        List<StarSystemAPI> validSystems = new ArrayList<>();
        for (StarSystemAPI system : sector.getStarSystems()) {
            if (blacklist.contains(system.getId())) {
                continue;
            }
            boolean isValid = true;
            for (String bannedTag : tagBlacklist) {
                if (system.hasTag(bannedTag)) {
                    isValid = false;
                    break;
                }
            }

            if (system.getStar() == null || !Misc.getMarketsInLocation(system).isEmpty() || system.getConstellation() == null) {
                isValid = false;
            }

            if (isValid) {
                validSystems.add(system);
            }
        }

        //If that list is empty, return null
        if (validSystems.isEmpty()) {
            return null;
        }

        //Otherwise, get a random element in it and return that
        else {
            int rand = MathUtils.getRandomNumberInRange(0, validSystems.size() - 1);
            return validSystems.get(rand);
        }
    }
}
