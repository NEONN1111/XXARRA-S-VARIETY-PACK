package neon.nsp.data.world;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.impl.campaign.DerelictShipEntityPlugin;
import com.fs.starfarer.api.impl.campaign.enc.AbyssalRogueStellarObjectEPEC;
import com.fs.starfarer.api.impl.campaign.ids.*;
import com.fs.starfarer.api.impl.campaign.procgen.DefenderDataOverride;
import com.fs.starfarer.api.impl.campaign.procgen.StarSystemGenerator;
import com.fs.starfarer.api.impl.campaign.procgen.themes.BaseThemeGenerator;
import com.fs.starfarer.api.impl.campaign.rulecmd.salvage.special.ShipRecoverySpecial;
import com.fs.starfarer.api.impl.campaign.world.GateHaulerLocation;
import com.fs.starfarer.api.util.Misc;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;
import java.util.Random;

public class nsp_abyssalgen1 {


    public static nsp_abyssalgen1 addsystem;


    public static void generate(SectorAPI sector) {
        StarSystemAPI system = sector.createStarSystem("Desolation I");
        //system.setType(StarSystemType.NEBULA);
        system.setName("Deep Space I"); // to get rid of "Star System" at the end of the name
        system.setOptionalUniqueId("invgen"); // used to retrieve system later if needed, we don't really need to but i didn't comment this out

        system.setType(StarSystemGenerator.StarSystemType.DEEP_SPACE);
        system.addTag(Tags.THEME_HIDDEN);
        system.addTag(Tags.THEME_SPECIAL);
        system.addTag(Tags.SYSTEM_ABYSSAL);


        system.setBackgroundTextureFilename("graphics/backgrounds/background5.jpg");

        Random random = StarSystemGenerator.random;

        float w = Global.getSettings().getFloat("sectorWidth"); // used to scale the map so the system spawns in the abyss
        float h = Global.getSettings().getFloat("sectorHeight"); // otherwise would cause issues if map size changed

        Vector2f systemLoc = new Vector2f();
        float outsideMapPad = 2000;
        float outsideMapRand = 2000f;
        float r = random.nextFloat();
        // this whole block is used to find a semi-random location for the system
        if (r < 0.5f) { // left
            systemLoc.x = -w / 2f - outsideMapPad - outsideMapRand * random.nextFloat();
            //systemLoc.y = -h/2f - outsideMapPad + (h + outsideMapPad * 2f) * random.nextFloat();
            systemLoc.y = -h / 2f + (h + outsideMapPad * 1f) * random.nextFloat();
        } else { //if (r < 0.75f) { // bottom
            systemLoc.x = -w / 2f + (w + outsideMapPad * 1f) * random.nextFloat();
            systemLoc.y = -h / 2f - outsideMapPad - outsideMapRand * random.nextFloat();
        }

        system.getLocation().set(systemLoc); // use tempLocation for now so we don't have to fuck around with finding it while testing


        SectorEntityToken center = system.initNonStarCenter();

        system.setLightColor(GateHaulerLocation.ABYSS_AMBIENT_LIGHT_COLOR); // light color in entire system, affects all entities
        center.addTag(Tags.AMBIENT_LS);

        //String name = Misc.genEntityCatalogId(2700, 11, 7, CatalogEntryType.PLANET);
        String name = "Nameless Rock 1";

        PlanetAPI rock = system.addPlanet("nsp_threat1", null, name, Planets.BARREN, 0, 150, 0, 0);
        rock.setDescriptionIdOverride("barren_deep_space");

        //rock.setCustomDescriptionId("???");
        // rock.getMemoryWithoutUpdate().set("$namelessRock", true);

        rock.getMarket().addCondition(Conditions.NO_ATMOSPHERE);
        rock.getMarket().addCondition(Conditions.VERY_COLD);
        rock.getMarket().addCondition(Conditions.DARK);
        rock.getMarket().addCondition(Conditions.ORE_RICH);
        rock.getMarket().addCondition(Conditions.RARE_ORE_MODERATE);

        rock.setOrbit(null);
        rock.setLocation(0, 0);


        system.autogenerateHyperspaceJumpPoints(false, false);

        AbyssalRogueStellarObjectEPEC.setAbyssalDetectedRanges(system);

        SectorEntityToken anchor = system.getHyperspaceAnchor();
        CustomCampaignEntityAPI beacon = Global.getSector().getHyperspace().addCustomEntity("nsp_threatbeacon",
                "Beacon", "warning_beacon", Factions.NEUTRAL);
        beacon.setCircularOrbitPointingDown(anchor, 100, 300, 65f);
        beacon.setCustomDescriptionId("nsp_threatbeacon");
        Misc.setWarningBeaconColors(beacon, new Color(155,155,155,155), new Color(155,155,155,155));
        beacon.getMemoryWithoutUpdate().set("$nsp_threatbeacon", true);
        beacon.getMemoryWithoutUpdate().set("$nsp_threatbeacontag", true);

    }

    public static SectorEntityToken createWreckEntity(StarSystemAPI system, String variantID, ShipRecoverySpecial.ShipCondition condition, String shipName, SectorEntityToken orbitFocus, boolean unscrappable, boolean unique, Vector2f location) {
        // in order:
        // system to place wreck in
        // variant ID to create ship recovery data
        // ship condition on recovery
        // name of ship
        // orbit (can be null, if it is uses location and sets fixed location)
        // whether wreck can be scrapped or not (if true,  data.notNowOptionExits = true)
        // if wreck is unique (makes it unable to be scuttled + notable sig)
        // location (used if null orbit)
        // result returns our entity so we can do stuff to it


        // makes it unpilotable by the the player with Neural Link, don't want that
//		AICoreOfficerPlugin plugin = Misc.getAICoreOfficerPlugin(Commodities.GAMMA_CORE);
//		if (plugin != null) {
//			copy.captain = plugin.createPerson(Commodities.GAMMA_CORE, Factions.PLAYER, null);
//			copy.captain.getStats().setSkillLevel(Skills.POLARIZED_ARMOR, 2);
//			copy.captain.getStats().setLevel(copy.captain.getStats().getLevel() + 1);
//			Misc.setUnremovable(copy.captain, true);
//			Misc.setKeepOnShipRecovery(copy.captain, true);
//		}

        return orbitFocus;
    }

}
