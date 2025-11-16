package data.plugins;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.FleetAssignment;
import com.fs.starfarer.api.campaign.FleetDataAPI;
import com.fs.starfarer.api.campaign.LocationAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.fleet.FleetMemberType;
import com.fs.starfarer.api.impl.campaign.fleets.*;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import com.fs.starfarer.api.impl.campaign.ids.FleetTypes;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import com.fs.starfarer.api.loading.VariantSource;
import com.fs.starfarer.api.util.Misc;

public class CustomFleetsNSP {

	/**
	 * To add a new fleet:
	 * 1) Make a copy of this method
	 * 2) Call it from spawn()
	 */
	public static void spawnFleetXIVictus() {
		LocationAPI location = Global.getSector().getStarSystem("corvus");
		if (location == null) return;
		SectorEntityToken planet = location.getEntityById("jangala");
		CampaignFleetAPI fleet = Global.getFactory().createEmptyFleet(Factions.HEGEMONY, "Jangalan Guardians", true);
		
		FleetDataAPI data = fleet.getFleetData();
		fleet.setTransponderOn(true);


		// add a fleet member with a custom name
		FleetMemberAPI flag = Global.getFactory().createFleetMember(FleetMemberType.SHIP, "nsp_invictus_xiv_standard");
		flag.setShipName("HSS Judas");
		data.addFleetMember(flag);
		
		// add a ship and a fighter
		data.addFleetMember(Global.getFactory().createFleetMember(FleetMemberType.SHIP, "eagle_xiv_Elite"));
		data.addFleetMember(Global.getFactory().createFleetMember(FleetMemberType.SHIP, "eagle_xiv_Elite"));
		data.addFleetMember(Global.getFactory().createFleetMember(FleetMemberType.SHIP, "enforcer_XIV_Elite"));
		data.addFleetMember(Global.getFactory().createFleetMember(FleetMemberType.SHIP, "eagle_xiv_Elite"));
		data.addFleetMember(Global.getFactory().createFleetMember(FleetMemberType.SHIP, "enforcer_Escort"));
		data.addFleetMember(Global.getFactory().createFleetMember(FleetMemberType.SHIP, "enforcer_Escort"));
		data.addFleetMember(Global.getFactory().createFleetMember(FleetMemberType.SHIP, "dominator_XIV_Elite"));
		data.addFleetMember(Global.getFactory().createFleetMember(FleetMemberType.SHIP, "mora_Assault"));
		data.addFleetMember(Global.getFactory().createFleetMember(FleetMemberType.SHIP, "mora_Support"));
		data.addFleetMember(Global.getFactory().createFleetMember(FleetMemberType.SHIP, "lasher_Strike"));
		data.addFleetMember(Global.getFactory().createFleetMember(FleetMemberType.SHIP, "lasher_Assault"));
		data.addFleetMember(Global.getFactory().createFleetMember(FleetMemberType.SHIP, "gremlin_Strike"));
		data.addFleetMember(Global.getFactory().createFleetMember(FleetMemberType.SHIP, "brawler_Elite"));
		data.addFleetMember(Global.getFactory().createFleetMember(FleetMemberType.SHIP, "brawler_Assault"));
		data.addFleetMember(Global.getFactory().createFleetMember(FleetMemberType.SHIP, "kite_hegemony_Interceptor"));
		data.addFleetMember(Global.getFactory().createFleetMember(FleetMemberType.SHIP, "hound_hegemony_Standard"));

		DefaultFleetInflaterParams p = new DefaultFleetInflaterParams();
		p.quality = 3f;
		fleet.setInflater(new DefaultFleetInflater(p));
		if (fleet.getInflater() instanceof DefaultFleetInflater) {
			DefaultFleetInflater dfi = (DefaultFleetInflater) fleet.getInflater();
			DefaultFleetInflaterParams dfip = (DefaultFleetInflaterParams)dfi.getParams();
			dfip.allWeapons = true;
			dfip.averageSMods = 1;
			dfip.quality = 3f;
		}
		for (FleetMemberAPI member : fleet.getFleetData().getMembersListCopy()){
			member.getRepairTracker().setCR(member.getRepairTracker().getMaxCR()); // set all ships to max cr
		}
		FleetParamsV3 params = new FleetParamsV3(
				planet.getMarket(), // source market
				planet.getLocation(),
				fleet.getFaction().getId(),
				null,
				FleetTypes.PATROL_LARGE,
				fleet.getFleetPoints(), // combatPts
				0, // freighterPts
				0, // tankerPts
				0f, // transportPts
				0f, // linerPts
				0f, // utilityPts
				0f // qualityMod
		);
		ShipVariantAPI flagvariant = flag.getVariant().clone();
		params.ignoreMarketFleetSizeMult = true;
		FleetFactoryV3.addCommanderAndOfficers(fleet, params, Misc.random); // use params we set up to add officers to fleet ... turns out this wasn't necessary and shouldn't be used here.
		fleet.getFleetData().setFlagship(flag); // make sure flagship is actually the flagship
		fleet.setCommander(flag.getCaptain());
		FleetFactory.finishAndSync(fleet);
		fleet.inflateIfNeeded();

		flagvariant.setSource(VariantSource.REFIT);
		flagvariant.addTag(Tags.TAG_NO_AUTOFIT);
		flagvariant.addTag(Tags.VARIANT_ALWAYS_RECOVERABLE);
		flag.setVariant(flagvariant, false, true);



		// add fleet to a star system and set its location

		location.addEntity(fleet);
		fleet.setLocation(planet.getLocation().x, planet.getLocation().y - 500);
		fleet.getAI().addAssignment(FleetAssignment.PATROL_SYSTEM, planet, 1000000f, "Patrolling", null);






		// give the fleet an assignment (1000000f days ~= forever)
		// the fleet tooltip will show it as "<relationship level>, doing something" - i.e. "Neutral, doing something"
	}
	
	
	/**
	 * This is called from CoreCampaignPluginImpl.onNewGameAfterTimePass().
	 */
	public void spawn() {
		//spawnTestFleet();
	}
}
