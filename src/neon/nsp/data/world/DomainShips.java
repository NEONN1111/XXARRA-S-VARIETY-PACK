package neon.nsp.data.world; // NEEDS a package. Must match the class path in your mod. Mine is different, so change this! Right now!

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.fleet.FleetMemberType;
import com.fs.starfarer.api.impl.campaign.DModManager;
import com.fs.starfarer.api.impl.campaign.fleets.FleetFactoryV3;
import com.fs.starfarer.api.impl.campaign.fleets.FleetParamsV3;
import com.fs.starfarer.api.impl.campaign.ids.*;
import com.fs.starfarer.api.util.Misc;


import java.util.Random;

public class DomainShips { // needs an enclosing class to be able to create a function, which is why this exists at the top

        public static void generate(StarSystemAPI system) { // This is going to be called in our mod plugin, i'm not 100% sure on what you want to do with this, but
                // If you want this to be procgen, you need a way to locate your semi-random system for the "final StarSystemAPI system" part
                // if you DON'T want this to be procgen and have a specific system in mind, this is unnecessary and you can define the system below
                // like  StarSystemAPI system = Global.getSector().getStarSystem("STARSYSTEM_NAME");

                SectorEntityToken DomainShips = system.addCustomEntity("orbital_habitat_domainships", "Orbital Habitat", "station_side05", Factions.NEUTRAL);
                DomainShips.setCircularOrbitPointingDown(system.getCenter(), 200, 2400, 100); // if you pick a system with a nebula, system.getStar may return null. Be careful!
                // Keep in mind, with this code you're creating an abandoned station entity (think something like the abandoned terraforming platform)
                // If you want to create a salvage entity instead, you would use
                //   MiscellaneousThemeGenerator.addSalvageEntity(system.getCenter().getContainingLocation(), Entities.ORBITAL_HABITAT, Factions.NEUTRAL)
                // research station is just an example
                // One important thing is that because it's a custom entity and not a salvage entity, I'm not sure if it'll call SalvageDefenderInteraction without using rules.csv
                // (it might because honestly i don't remember, but it might not)
                // this is solvable with rules.csv, but if you don't want to do that, you can make it a salvage entity
                Misc.setAbandonedStationMarket("orbital_habitat_domainships", DomainShips);
                DomainShips.setInteractionImage("illustrations", "abandoned_station2"); // set the interaction image to an image registered in settings.json
                DomainShips.setCustomDescriptionId("orbital_habitat_domainships"); // defined in descriptions.csv
                DomainShips.getMemoryWithoutUpdate().set("$hasDefenders", true); // tells the game our entity has a defender fleet

                //  creates an empty fleet of DERELICT faction, then adds it to the entity. this is fine
                // if you want you can use a SalvageDefenderModificationPlugin instead, but it doesn't matter *that* much.
                // if you opt to use a plugin, remember it has to be registered first.
                FleetParamsV3 params = new FleetParamsV3(
                        null, // source market
                        system.getLocation(),
                        Factions.DERELICT,
                        null,
                        FleetTypes.PATROL_LARGE,
                        50, // combatPts
                        0, // freighterPts
                        0, // tankerPts
                        0f, // transportPts
                        0f, // linerPts
                        0f, // utilityPts
                        0f // qualityMod
                );
                params.ignoreMarketFleetSizeMult = true;
                CampaignFleetAPI fleet = FleetFactoryV3.createFleet(params);

                FleetMemberAPI station = Global.getFactory().createFleetMember(FleetMemberType.SHIP, "station1_Standard");
                station.setShipName("Habitat Zeta");
                fleet.getFleetData().addFleetMember(station);
                fleet.getFleetData().setFlagship(station);
                final Random r = new Random();


                for (FleetMemberAPI s : fleet.getFleetData().getMembersListCopy()) { // need to access FleetData to get to MembersListCopy
                        // this is similar to what we do in PKDefenderPlugin, you can look back at how that works
                        // I won't use MagicLib functions here because i don't know what compatibility you need, but
                        // if you do use it, for num you can use MathUtils.getrandomnumberinrange
                        DModManager.addDMods(s, false, Math.abs(r.nextInt()) % 4 + 2, r);

                }
                DomainShips.getMarket().getSubmarket(Submarkets.SUBMARKET_STORAGE).getCargo().addFuel(2000);
                DomainShips.getMarket().getSubmarket(Submarkets.SUBMARKET_STORAGE).getCargo().addSupplies(1500);
                DomainShips.getMarket().getSubmarket(Submarkets.SUBMARKET_STORAGE).getCargo().addCommodity("domestic_goods", 500);
                DomainShips.getMarket().getSubmarket(Submarkets.SUBMARKET_STORAGE).getCargo().addCommodity("food", 900);
                DomainShips.getMarket().getSubmarket(Submarkets.SUBMARKET_STORAGE).getCargo().addCommodity("luxury_goods", 200);
                DomainShips.getMarket().getSubmarket(Submarkets.SUBMARKET_STORAGE).getCargo().addMothballedShip(FleetMemberType.SHIP, "nsp_absolution_domain_standard", "Into The Fire");
                DomainShips.getMarket().getSubmarket(Submarkets.SUBMARKET_STORAGE).getCargo().addMothballedShip(FleetMemberType.SHIP, "nsp_onslaught_domain_elite", "Into The Flames");
                DomainShips.getMarket().getSubmarket(Submarkets.SUBMARKET_STORAGE).getCargo().addMothballedShip(FleetMemberType.SHIP, "nsp_legion_domain_elite", "Into The Fray");
                DomainShips.getMarket().getSubmarket(Submarkets.SUBMARKET_STORAGE).getCargo().addMothballedShip(FleetMemberType.SHIP, "nsp_dominator_pr_Elite", "Out Of Hell");
                // addCombatFleetPoints does what it says, add  s FP (not dp, those are different) worth of combat ships to the fleet.
                // these are semi-random, so if you don't want to do this you can remove it and use  fleet.getFleetData().addFleetMember to add specific ships
                DomainShips.getMemoryWithoutUpdate().set("$defenderFleet", fleet);
                if (system.getConstellation().getName() != null) {
                        Global.getSector().getMemoryWithoutUpdate().set("$NSP_DomainShips", system.getConstellation().getName());
                }
        }
}