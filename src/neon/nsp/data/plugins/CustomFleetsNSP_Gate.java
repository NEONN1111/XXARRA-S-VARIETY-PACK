package neon.nsp.data.plugins;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.fleet.FleetMemberType;
import com.fs.starfarer.api.impl.campaign.DModManager;
import com.fs.starfarer.api.impl.campaign.fleets.*;
import com.fs.starfarer.api.impl.campaign.ids.*;
import com.fs.starfarer.api.impl.campaign.procgen.themes.RemnantSeededFleetManager;
import com.fs.starfarer.api.util.Misc;

import java.util.Random;

import static com.fs.starfarer.api.impl.campaign.procgen.themes.RemnantSeededFleetManager.initRemnantFleetProperties;
import static com.fs.starfarer.api.util.Misc.random;

public class CustomFleetsNSP_Gate {

	/**
	 * To add a new fleet:
	 * 1) Make a copy of this method
	 * 2) Call it from spawn()
	 */
	public static void spawnNemetor() {
		CampaignFleetAPI fleet = Global.getFactory().createEmptyFleet(Factions.DERELICT, "DEFENSE FLEET DF RC 1157", true);

		LocationAPI location = Global.getSector().getStarSystem("vigil"); // needs a location to spawn at to not crash the game, so if this is going to be null anyways then just give up
		if (location == null) return;

		// add a fleet member with a custom name
      FleetMemberAPI flag =  fleet.getFleetData().addFleetMember("nsp_nemetor_standard");
        PersonAPI person =  Misc.getAICoreOfficerPlugin("alpha_core").createPerson("alpha_core",fleet.getFaction().getId(), random);

		// add a ship and a fighter
		// this goes way above the vanilla max number of ships in a fleet which causes issues with post-battle recovery, if you care about that
		addMembersFromVariant(fleet, "nsp_superderelict_standard", 2);
		addMembersFromVariant(fleet, "nsp_recluse_standard", 2);
		addMembersFromVariant(fleet, "nsp_hermod_standard", 1);
		addMembersFromVariant(fleet, "nsp_gleam_standard", 3);
		addMembersFromVariant(fleet, "nsp_luminance_standard", 1);
		addMembersFromVariant(fleet, "nsp_siegecarrier_standard", 1);
		addMembersFromVariant(fleet, "nsp_rampartcarrier_standard", 1);
		addMembersFromVariant(fleet, "nsp_parasite_standard", 3);
		addMembersFromVariant(fleet, "nsp_carrierplatform_standard", 2);
		Random random = Misc.random;
		fleet.getMemoryWithoutUpdate().set("$nsp_nemetorFleet", true);
		fleet.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_NO_REP_IMPACT, true);
		fleet.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_MAKE_HOLD_VS_STRONGER, true);
		fleet.getMemoryWithoutUpdate().set(MemFlags.FLEET_IGNORES_OTHER_FLEETS, true);
		fleet.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_SKIP_TRANSPONDER_STATUS_INFO, true);
		fleet.getMemoryWithoutUpdate().set(MemFlags.FLEET_INTERACTION_DIALOG_CONFIG_OVERRIDE_GEN, new RemnantSeededFleetManager.RemnantFleetInteractionConfigGen());
		// sets it so that fleet uses remnant FIDConfig, mainly so they don't try to run
		DefaultFleetInflaterParams p = new DefaultFleetInflaterParams(); // super 100% necessary to actually have the fleet inflate
		p.quality = 0.5f; // if you don't set the inflater, nothing happens and you won't get any officers or d-mods
		fleet.setInflater(new DefaultFleetInflater(p));
		if (fleet.getInflater() instanceof DefaultFleetInflater) {
			DefaultFleetInflater dfi = (DefaultFleetInflater) fleet.getInflater();
			DefaultFleetInflaterParams dfip = (DefaultFleetInflaterParams)dfi.getParams();
			dfip.allWeapons = true;
			dfip.averageSMods = 2;
			dfip.quality = 1f;
			DModManager.assumeAllShipsAreAutomated = true;
		}
		initRemnantFleetProperties(random, fleet, false); // makes fleet lose certain abilities like e-burn, behave more like remnant fleets
		//FleetFactoryV3.addCommanderAndOfficers(fleet, params, random); // use params we set up to add officers to fleet ... turns out this wasn't necessary and shouldn't be used here.
		fleet.getFleetData().setFlagship(flag); // make sure flagship is actually the flagship
		fleet.setCommander(person); // put person we created back on commander slot
		fleet.getFlagship().setCaptain(person); // and also on the flagship
		for (FleetMemberAPI member : fleet.getFleetData().getMembersListCopy()){
			member.getRepairTracker().setCR(member.getRepairTracker().getMaxCR()); // set all ships to max cr
		}
		// we set the FIDConfig here so that the fleet doesn't try to retreat. normally, you only fight derelicts at salvage interactions where they can't retreat anyway
		// you can also set other memory like making them always hostile to the player, ignored by other fleets, etc...

		// makes fleet not need supplies or fuel or crew
		// not sure why this says this - AI fleets never need these things.
		FleetFactory.finishAndSync(fleet);

		// add fleet to a star system and set its location

			SectorEntityToken planet = location.getEntityById("nsp_habplanet");
			location.addEntity(fleet);
			if (planet != null){
				fleet.setLocation(planet.getLocation().x, planet.getLocation().y - 500);
				fleet.getAI().addAssignment(FleetAssignment.ORBIT_AGGRESSIVE, planet, 1000000f, "Standing Vigil.", null);
			}





		// give the fleet an assignment (1000000f days ~= forever)
		// the fleet tooltip will show it as "<relationship level>, doing something" - i.e. "Neutral, doing something"
	}
	public static void addMembersFromVariant(CampaignFleetAPI fleet, String variantID, int num) {
		for (int i = 0; i < num; i++) {
			fleet.getFleetData().addFleetMember(Global.getFactory().createFleetMember(FleetMemberType.SHIP, variantID));
		}
	}
}

