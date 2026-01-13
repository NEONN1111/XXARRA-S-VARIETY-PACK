package neon.nsp.data.listeners;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.Script;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.campaign.listeners.FleetEventListener;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.fleet.FleetMemberType;
import com.fs.starfarer.api.impl.campaign.fleets.*;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import com.fs.starfarer.api.impl.campaign.ids.FleetTypes;
import com.fs.starfarer.api.impl.campaign.ids.MemFlags;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import com.fs.starfarer.api.loading.VariantSource;
import com.fs.starfarer.api.util.Misc;

public class DerelictBattleListener implements FleetEventListener {
    @Override
    public void reportFleetDespawnedToListener(CampaignFleetAPI fleet, CampaignEventListener.FleetDespawnReason reason, Object param) {

    }

    @Override
    public void reportBattleOccurred(CampaignFleetAPI fleet, CampaignFleetAPI primaryWinner, BattleAPI battle) {
        if (battle.isPlayerInvolved()) {
            MemoryAPI memoryWU = Global.getSector().getMemoryWithoutUpdate();
            if (!memoryWU.contains("$nsp_derelictFightCount")) {
                memoryWU.set("$nsp_derelictFightCount",0);
            }
            int derelictFightCount = memoryWU.getInt("$nsp_derelictFightCount");

            boolean derelictOppositionPresent = false;
            for (CampaignFleetAPI campaignFleetAPI : battle.getNonPlayerSide()) {
                if (!campaignFleetAPI.getFaction().getId().equals(Factions.DERELICT)) continue;
                derelictOppositionPresent = true;
                break;
            }

            if (derelictOppositionPresent) {
                derelictFightCount++;
                memoryWU.set("$nsp_derelictFightCount",derelictFightCount);
                Global.getLogger(this.getClass()).info("Fought against Derelict/Explorarium fleet " + derelictFightCount + " times.");
            }

            // Change number to 10 battles instead of 1 (dev) | CONTINUE
            if (derelictFightCount >= 1 && !Global.getSector().getMemoryWithoutUpdate().contains("$nsp_invictaBountyHunter")) {

                LocationAPI location = Global.getSector().getStarSystem("arcadia");
                if (location == null) return;
                SectorEntityToken planet = location.getEntityById("nomios");

                CampaignFleetAPI bountyHunter = Global.getFactory().createEmptyFleet(Factions.MERCENARY, "Bounty Hunter", true);

                FleetDataAPI data = bountyHunter.getFleetData();
                bountyHunter.setTransponderOn(true);
                bountyHunter.setId("NSPInvictaMerc");

                // add a fleet member with a custom name
                FleetMemberAPI flagship = Global.getFactory().createFleetMember(FleetMemberType.SHIP, "conquest_Elite");
                flagship.setShipName("ISS Hyperspace Dreaming");
                data.addFleetMember(flagship);

                data.addFleetMember(Global.getFactory().createFleetMember(FleetMemberType.SHIP,"nsp_superderelict_standard"));
                data.addFleetMember(Global.getFactory().createFleetMember(FleetMemberType.SHIP,"bastillon_Standard"));
                data.addFleetMember(Global.getFactory().createFleetMember(FleetMemberType.SHIP,"defender_PD"));
                data.addFleetMember(Global.getFactory().createFleetMember(FleetMemberType.SHIP,"champion_Escort"));
                data.addFleetMember(Global.getFactory().createFleetMember(FleetMemberType.SHIP,"fury_Attack"));
                data.addFleetMember(Global.getFactory().createFleetMember(FleetMemberType.SHIP,"hyperion_Strike"));
                data.addFleetMember(Global.getFactory().createFleetMember(FleetMemberType.SHIP,"harbinger_Strike"));
                data.addFleetMember(Global.getFactory().createFleetMember(FleetMemberType.SHIP,"medusa_Attack"));
                data.addFleetMember(Global.getFactory().createFleetMember(FleetMemberType.SHIP,"apogee_Balanced"));
                data.addFleetMember(Global.getFactory().createFleetMember(FleetMemberType.SHIP,"heron_Attack1"));
                data.addFleetMember(Global.getFactory().createFleetMember(FleetMemberType.SHIP,"heron_Strike"));
                data.addFleetMember(Global.getFactory().createFleetMember(FleetMemberType.SHIP,"enforcer_Elite"));
                data.addFleetMember(Global.getFactory().createFleetMember(FleetMemberType.SHIP,"warden_Defense"));
                data.addFleetMember(Global.getFactory().createFleetMember(FleetMemberType.SHIP,"warden_Defense"));
                data.addFleetMember(Global.getFactory().createFleetMember(FleetMemberType.SHIP,"warden_Defense"));
                data.addFleetMember(Global.getFactory().createFleetMember(FleetMemberType.SHIP,"ox_Standard"));
                data.addFleetMember(Global.getFactory().createFleetMember(FleetMemberType.SHIP,"ox_Standard"));
                data.addFleetMember(Global.getFactory().createFleetMember(FleetMemberType.SHIP,"ox_Standard"));
                data.addFleetMember(Global.getFactory().createFleetMember(FleetMemberType.SHIP,"ox_Standard"));
                data.addFleetMember(Global.getFactory().createFleetMember(FleetMemberType.SHIP,"ox_Standard"));

                DefaultFleetInflaterParams p = new DefaultFleetInflaterParams();
                p.quality = 3f;
                bountyHunter.setInflater(new DefaultFleetInflater(p));
                if (bountyHunter.getInflater() instanceof DefaultFleetInflater) {
                    DefaultFleetInflater dfi = (DefaultFleetInflater) bountyHunter.getInflater();
                    DefaultFleetInflaterParams dfip = (DefaultFleetInflaterParams)dfi.getParams();
                    dfip.allWeapons = true;
                    dfip.averageSMods = 1;
                    dfip.quality = 3f;
                }
                for (FleetMemberAPI member : bountyHunter.getFleetData().getMembersListCopy()){
                    member.getRepairTracker().setCR(member.getRepairTracker().getMaxCR()); // set all ships to max cr
                }

                FleetParamsV3 params = new FleetParamsV3(
                        planet.getMarket(), // source market
                        planet.getLocation(),
                        bountyHunter.getFaction().getId(),
                        null,
                        FleetTypes.MERC_BOUNTY_HUNTER,
                        bountyHunter.getFleetPoints(), // combatPts
                        0, // freighterPts
                        0, // tankerPts
                        0f, // transportPts
                        0f, // linerPts
                        0f, // utilityPts
                        0f // qualityMod
                );

                ShipVariantAPI flagvariant = flagship.getVariant().clone();
                params.ignoreMarketFleetSizeMult = true;
                FleetFactoryV3.addCommanderAndOfficers(bountyHunter, params, Misc.random); // use params we set up to add officers to fleet ... turns out this wasn't necessary and shouldn't be used here.
                bountyHunter.getFleetData().setFlagship(flagship); // make sure flagship is actually the flagship
                bountyHunter.setCommander(flagship.getCaptain());
                FleetFactory.finishAndSync(bountyHunter);
                bountyHunter.inflateIfNeeded();

                flagvariant.setSource(VariantSource.REFIT);
                flagvariant.addTag(Tags.TAG_NO_AUTOFIT);
                flagship.setVariant(flagvariant, false, true);

//                location.addEntity(bountyHunter);
//                bountyHunter.setLocation(planet.getLocation().x, planet.getLocation().y - 500);
                Global.getSector().getPlayerFleet().getStarSystem().addEntity(bountyHunter);
                bountyHunter.setLocation(Global.getSector().getPlayerFleet().getLocation().x, Global.getSector().getPlayerFleet().getLocation().y);

                bountyHunter.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_MAKE_HOLD_VS_STRONGER,true);
                bountyHunter.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_MAKE_ALLOW_DISENGAGE,false);
                bountyHunter.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_MAKE_PREVENT_DISENGAGE,true);
                bountyHunter.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_PURSUE_PLAYER,true);
                bountyHunter.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_ALLOW_LONG_PURSUIT,true);
                bountyHunter.getAI().isHostileTo(Global.getSector().getPlayerFleet());

                Global.getSector().getMemoryWithoutUpdate().set("$nsp_invictaBountyHunter",bountyHunter);
                bountyHunter.getAI().addAssignment(FleetAssignment.INTERCEPT, Global.getSector().getPlayerFleet(), 1000000f, "intercepting you", new Script() {
                    @Override
                    public void run() {
                        bountyHunter.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_PURSUE_PLAYER,false);
                        bountyHunter.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_ALLOW_LONG_PURSUIT,false);
                        bountyHunter.getAI().addAssignment(FleetAssignment.GO_TO_LOCATION_AND_DESPAWN,planet,1000000f,null);
                    }
                });
            }
        }
    }
}
