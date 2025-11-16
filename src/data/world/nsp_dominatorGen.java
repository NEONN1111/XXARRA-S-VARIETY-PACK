package data.world;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.impl.campaign.DerelictShipEntityPlugin;
import com.fs.starfarer.api.impl.campaign.enc.AbyssalRogueStellarObjectEPEC;
import com.fs.starfarer.api.impl.campaign.ids.*;
import com.fs.starfarer.api.impl.campaign.procgen.StarSystemGenerator;
import com.fs.starfarer.api.impl.campaign.procgen.themes.BaseThemeGenerator;
import com.fs.starfarer.api.impl.campaign.rulecmd.salvage.special.ShipRecoverySpecial;
import com.fs.starfarer.api.impl.campaign.world.GateHaulerLocation;
import com.fs.starfarer.api.util.Misc;
import org.lwjgl.util.vector.Vector2f;

import java.util.Random;

public class nsp_dominatorGen {


    public static nsp_dominatorGen addsystem;


    public static void generate(SectorAPI sector) {
        StarSystemAPI system = sector.createStarSystem("Eternal Vigil");
        //system.setType(StarSystemType.NEBULA);
        system.setName("Deep Space"); // to get rid of "Star System" at the end of the name
        system.setOptionalUniqueId("domgen"); // used to retrieve system later if needed, we don't really need to but i didn't comment this out

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
            systemLoc.x = -w/2f - outsideMapPad - outsideMapRand * random.nextFloat();
            //systemLoc.y = -h/2f - outsideMapPad + (h + outsideMapPad * 2f) * random.nextFloat();
            systemLoc.y = -h/2f + (h + outsideMapPad * 1f) * random.nextFloat();
        } else { //if (r < 0.75f) { // bottom
            systemLoc.x = -w/2f + (w + outsideMapPad * 1f) * random.nextFloat();
            systemLoc.y = -h/2f - outsideMapPad - outsideMapRand * random.nextFloat();
        }

        system.getLocation().set(systemLoc); // use tempLocation for now so we don't have to fuck around with finding it while testing



        SectorEntityToken center = system.initNonStarCenter();

        system.setLightColor(GateHaulerLocation.ABYSS_AMBIENT_LIGHT_COLOR); // light color in entire system, affects all entities
        center.addTag(Tags.AMBIENT_LS);

        //String name = Misc.genEntityCatalogId(2700, 11, 7, CatalogEntryType.PLANET);
        String name = "Nameless Rock";

        PlanetAPI rock = system.addPlanet("nsp_rock2", null, name, Planets.BARREN, 0, 150, 0, 0);
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

       SectorEntityToken legion = createWreckEntity(system, "nsp_dominatormk1_ancient", ShipRecoverySpecial.ShipCondition.WRECKED, "Serial 12A183HP9A z Y5ZN", rock, true, true, new Vector2f());
        legion.getMemoryWithoutUpdate().set("$nsp_dominatorWreck", true); // used for dialog
        legion.setId("nsp_dominatorWreck");

    }

    public static SectorEntityToken createWreckEntity(StarSystemAPI system, String variantID, ShipRecoverySpecial.ShipCondition condition, String shipName, SectorEntityToken orbitFocus, boolean unscrappable, boolean unique, Vector2f location){
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
        ShipRecoverySpecial.PerShipData ship = new ShipRecoverySpecial.PerShipData(variantID, condition, 0f); // create ship recovery data using variant ID
        ship.shipName = shipName; // set name
        DerelictShipEntityPlugin.DerelictShipData params = new DerelictShipEntityPlugin.DerelictShipData(ship, false);
        CustomCampaignEntityAPI entity = (CustomCampaignEntityAPI) BaseThemeGenerator.addSalvageEntity( // add entity to system
                system,
                Entities.WRECK, Factions.NEUTRAL, params);
       // Misc.makeImportant(entity, "onslaughtMkI"); we can call these on the resulting entity after it's created for ease of use, i guess
      //  entity.getMemoryWithoutUpdate().set("$nsp_legionmk1", true);
        entity.setSensorProfile(1f); // sets sensor profile, optional i guess. you can remove this if you want
        entity.setDiscoverable(true); // if not set, entity ALWAYS appears on the map even if you've never been near it yet
        // would recommend setting it to true for any exploration related content

        Random random = StarSystemGenerator.random;
        if (orbitFocus != null) {
            float orbitRadius = orbitFocus.getRadius() + 200f;
            float orbitDays = orbitRadius / (10f + random.nextFloat() * 5f);
            entity.setCircularOrbit(orbitFocus, random.nextFloat() * 360f, orbitRadius, orbitDays);
        } else {
            entity.setFixedLocation(location.x, location.y);
        }

        ShipRecoverySpecial.ShipRecoverySpecialData data = new ShipRecoverySpecial.ShipRecoverySpecialData(null);
        data.notNowOptionExits = unscrappable; // if true, the "not now" option that normally lets you salvage the ship just boots you out
        data.noDescriptionText = true;
        DerelictShipEntityPlugin dsep = (DerelictShipEntityPlugin) entity.getCustomPlugin();
        ShipRecoverySpecial.PerShipData copy = (ShipRecoverySpecial.PerShipData) dsep.getData().ship.clone();
        copy.variant = Global.getSettings().getVariant(copy.variantId).clone();
        copy.variantId = null;
        if (unique) { // makes ship notable and unable to be scuttled
            copy.variant.addTag(Tags.SHIP_CAN_NOT_SCUTTLE);
            copy.variant.addTag(Tags.SHIP_UNIQUE_SIGNATURE);
        }
        copy.nameAlwaysKnown = true;
        //copy.addDmods = false;
        copy.pruneWeapons = false;

        // makes it unpilotable by the the player with Neural Link, don't want that
//		AICoreOfficerPlugin plugin = Misc.getAICoreOfficerPlugin(Commodities.GAMMA_CORE);
//		if (plugin != null) {
//			copy.captain = plugin.createPerson(Commodities.GAMMA_CORE, Factions.PLAYER, null);
//			copy.captain.getStats().setSkillLevel(Skills.POLARIZED_ARMOR, 2);
//			copy.captain.getStats().setLevel(copy.captain.getStats().getLevel() + 1);
//			Misc.setUnremovable(copy.captain, true);
//			Misc.setKeepOnShipRecovery(copy.captain, true);
//		}

        data.addShip(copy); // copy the variant we created and add it to the ship data, prevents a bunch of weird nonsense caused by trying to use the original

        Misc.setSalvageSpecial(entity, data);
        return entity;
    }




}
