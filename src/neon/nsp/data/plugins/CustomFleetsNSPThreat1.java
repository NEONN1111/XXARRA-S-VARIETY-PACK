package neon.nsp.data.plugins;

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

public class CustomFleetsNSPThreat1 {

	public static void spawnFleetInthrictus() {
//		LocationAPI location = Global.getSector().getStarSystem("Deep Space I");
		LocationAPI location = Global.getSector().getStarSystem("invgen");
		if (location == null) {
			Global.getLogger(CustomFleetsNSPThreat1.class).error("Desolation I system not found!");
			return;
		}

		SectorEntityToken planet = location.getEntityByName("Nameless Rock 1");
		if (planet == null) {
			Global.getLogger(CustomFleetsNSPThreat1.class).error("Nameless Rock 1 not found in Desolation I!");
			return;
		}

		Global.getLogger(CustomFleetsNSPThreat1.class).info("Found planet at: " + planet.getLocation());

		CampaignFleetAPI fleet = Global.getFactory().createEmptyFleet(Factions.THREAT, "Unknown", true);

		FleetDataAPI data = fleet.getFleetData();
		fleet.setTransponderOn(true);

		// add flagship
		FleetMemberAPI flag = Global.getFactory().createFleetMember(FleetMemberType.SHIP, "nsp_inthrictus_type350");
		flag.setShipName("Name Unknown");
		data.addFleetMember(flag);

		// add other ships
		data.addFleetMember(Global.getFactory().createFleetMember(FleetMemberType.SHIP, "assault_unit_Type200"));
		data.addFleetMember(Global.getFactory().createFleetMember(FleetMemberType.SHIP, "assault_unit_Type201"));
		data.addFleetMember(Global.getFactory().createFleetMember(FleetMemberType.SHIP, "hive_unit_Type350"));
		data.addFleetMember(Global.getFactory().createFleetMember(FleetMemberType.SHIP, "hive_unit_Type350"));
		data.addFleetMember(Global.getFactory().createFleetMember(FleetMemberType.SHIP, "hive_unit_Type350"));
		data.addFleetMember(Global.getFactory().createFleetMember(FleetMemberType.SHIP, "overseer_unit_Type250"));
		data.addFleetMember(Global.getFactory().createFleetMember(FleetMemberType.SHIP, "skirmish_unit_Type101"));
		data.addFleetMember(Global.getFactory().createFleetMember(FleetMemberType.SHIP, "skirmish_unit_Type100"));
		data.addFleetMember(Global.getFactory().createFleetMember(FleetMemberType.SHIP, "skirmish_unit_Type101"));
		data.addFleetMember(Global.getFactory().createFleetMember(FleetMemberType.SHIP, "skirmish_unit_Type100"));
		data.addFleetMember(Global.getFactory().createFleetMember(FleetMemberType.SHIP, "skirmish_unit_Type101"));

		DefaultFleetInflaterParams p = new DefaultFleetInflaterParams();
		p.quality = 3f;
		fleet.setInflater(new DefaultFleetInflater(p));
		if (fleet.getInflater() instanceof DefaultFleetInflater) {
			DefaultFleetInflater dfi = (DefaultFleetInflater) fleet.getInflater();
			DefaultFleetInflaterParams dfip = (DefaultFleetInflaterParams) dfi.getParams();
			dfip.allWeapons = true;
			dfip.averageSMods = 1;
			dfip.quality = 3f;
		}

		for (FleetMemberAPI member : fleet.getFleetData().getMembersListCopy()) {
			member.getRepairTracker().setCR(member.getRepairTracker().getMaxCR());
		}

		FleetParamsV3 params = new FleetParamsV3(
				planet.getMarket(),
				planet.getLocation(),
				fleet.getFaction().getId(),
				null,
				FleetTypes.PATROL_LARGE,
				fleet.getFleetPoints(),
				0,
				0,
				0f,
				0f,
				0f,
				0f
		);

		ShipVariantAPI flagvariant = flag.getVariant().clone();
		params.ignoreMarketFleetSizeMult = true;
//		FleetFactoryV3.addCommanderAndOfficers(fleet, params, Misc.random);
		fleet.getFleetData().setFlagship(flag);
		fleet.setCommander(flag.getCaptain());
		FleetFactory.finishAndSync(fleet);
		fleet.inflateIfNeeded();

		flagvariant.setSource(VariantSource.REFIT);
		flagvariant.addTag(Tags.TAG_NO_AUTOFIT);
		flagvariant.addTag(Tags.VARIANT_ALWAYS_RECOVERABLE);
		flag.setVariant(flagvariant, false, true);

		location.addEntity(fleet);
		fleet.setLocation(planet.getLocation().x, planet.getLocation().y - 500);
		fleet.getAI().addAssignment(FleetAssignment.PATROL_SYSTEM, planet, 1000000f, "Waiting", null);

		Global.getLogger(CustomFleetsNSPThreat1.class).info("Successfully spawned Inthrictus fleet at Desolation I");
	}
}