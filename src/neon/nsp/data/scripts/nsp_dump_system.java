package neon.nsp.data.scripts;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.fleet.FleetMemberType;
import com.fs.starfarer.api.impl.MusicPlayerPluginImpl;
import com.fs.starfarer.api.impl.campaign.ids.*;
import com.fs.starfarer.api.impl.campaign.procgen.StarSystemGenerator;
import com.fs.starfarer.api.util.Misc;

public class nsp_dump_system {

    // to put station entities in that we need later, avoids lag i hope?

    public void generate(SectorAPI sector) {
        com.fs.starfarer.api.campaign.StarSystemAPI system = Global.getSector().createStarSystem("thedump_nsp");
        system.setType(StarSystemGenerator.StarSystemType.NEBULA);
        system.addTag(Tags.THEME_HIDDEN);
        system.addTag(Tags.SYSTEM_CUT_OFF_FROM_HYPER);
        system.setBackgroundTextureFilename("graphics/backgrounds/background5.jpg");
        system.getLocation().set(0f, 0f);
        system.initNonStarCenter();
        system.generateAnchorIfNeeded();

        com.fs.starfarer.api.campaign.StarSystemAPI stationsystem = system;
        SectorEntityToken station = stationsystem.addCustomEntity(
                "nsp_nexusStorage",
                "Nexus Global Storage",
                "station_side05",
                Factions.NEUTRAL
        );

        Misc.setAbandonedStationMarket("nsp_nexusStorage", station);
        station.setSensorProfile(0f);
        station.setInteractionImage("icons", "remnantflag");
        station.getMarket().addIndustry(Industries.SPACEPORT);

        // Add some starting ships
        station.getMarket().getSubmarket(Submarkets.SUBMARKET_STORAGE).getCargo().addMothballedShip(
                FleetMemberType.SHIP, "fulgent_Assault", "Engiels"
        );
        station.getMarket().getSubmarket(Submarkets.SUBMARKET_STORAGE).getCargo().addMothballedShip(
                FleetMemberType.SHIP, "glimmer_Support", "Gomiel"
        );
        station.getMarket().getSubmarket(Submarkets.SUBMARKET_STORAGE).getCargo().addMothballedShip(
                FleetMemberType.SHIP, "glimmer_Support", "Halitosis"
        );

        station.setCircularOrbitPointingDown(stationsystem.getCenter(), 0f, 100000f, 9999f);

        // Add starting supplies
        station.getMarket().getSubmarket(Submarkets.SUBMARKET_STORAGE).getCargo().addCommodity(
                Commodities.SUPPLIES, 200f
        );
        station.getMarket().getSubmarket(Submarkets.SUBMARKET_STORAGE).getCargo().addCommodity(
                Commodities.FUEL, 400f
        );

        // Create the teller entity

        // Create AI core admin
        com.fs.starfarer.api.characters.PersonAPI adm = Misc.getAICoreAdminPlugin(Commodities.ALPHA_CORE)
                .createPerson(Commodities.ALPHA_CORE, Factions.INDEPENDENT, 1);
        adm.setPortraitSprite("graphics/portraits/portrait_ai3b.png");
        adm.getName().setFirst("Penitent Fae");
        adm.getName().setLast("");

    }
}