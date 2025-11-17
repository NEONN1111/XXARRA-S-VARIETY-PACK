package neon.nsp.data.plugins;

import com.fs.starfarer.api.campaign.AICoreOfficerPlugin;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.BaseGenericPlugin;
import com.fs.starfarer.api.impl.campaign.DModManager;
import com.fs.starfarer.api.impl.campaign.fleets.DefaultFleetInflater;
import com.fs.starfarer.api.impl.campaign.fleets.DefaultFleetInflaterParams;
import com.fs.starfarer.api.impl.campaign.ids.*;
import com.fs.starfarer.api.impl.campaign.rulecmd.salvage.SalvageGenFromSeed;
import com.fs.starfarer.api.loading.VariantSource;
import com.fs.starfarer.api.util.Misc;

import java.util.Random;

import static com.fs.starfarer.api.impl.campaign.procgen.themes.PKDefenderPluginImpl.addAutomated;
import static com.fs.starfarer.api.impl.campaign.procgen.themes.PKDefenderPluginImpl.makeAICoreSkillsGoodForLowTech;

public class NSPSafeguard  extends BaseGenericPlugin implements SalvageGenFromSeed.SalvageDefenderModificationPlugin {
    @Override
    public float getStrength(SalvageGenFromSeed.SDMParams p, float strength, Random random, boolean withOverride) {
        return strength;
    }

    @Override
    public float getProbability(SalvageGenFromSeed.SDMParams p, float probability, Random random, boolean withOverride) {
        return probability;
    }

    @Override
    public float getQuality(SalvageGenFromSeed.SDMParams p, float quality, Random random, boolean withOverride) {
        return quality;
    }

    @Override
    public float getMaxSize(SalvageGenFromSeed.SDMParams p, float maxSize, Random random, boolean withOverride) {
        return maxSize;
    }

    @Override
    public float getMinSize(SalvageGenFromSeed.SDMParams p, float minSize, Random random, boolean withOverride) {
        return minSize;
    }

    @Override
    public void modifyFleet(SalvageGenFromSeed.SDMParams p, CampaignFleetAPI fleet, Random random, boolean withOverride) {
        Misc.addDefeatTrigger(fleet, "PK14thDefeated");

        fleet.setNoFactionInName(true);
        fleet.setName("Remnants of Task Force Safeguard");
        fleet.getFleetData().clear(); // clear any pre-existing fleet
        fleet.setNoFactionInName(true); // so it doesn't appear as "derelict automated mining force"
        fleet.setFaction(Factions.HEGEMONY, true); // sets faction to orange men
        fleet.getFleetData().setShipNameRandom(random);

        AICoreOfficerPlugin plugin = Misc.getAICoreOfficerPlugin(Commodities.ALPHA_CORE);


        // GENERATE FLAGSHIP
        FleetMemberAPI member = fleet.getFleetData().addFleetMember("nsp_legion_safeguard_elite");
        member.setId("xivtf_" + random.nextLong());
        PersonAPI person = plugin.createPerson(Commodities.ALPHA_CORE, fleet.getFaction().getId(), random);
        person.getStats().setSkipRefresh(true);
        person.getStats().setSkillLevel(Skills.CARRIER_GROUP, 1);
        person.getStats().setSkillLevel(Skills.FIGHTER_UPLINK, 1);
        person.getStats().setSkipRefresh(false);

        member.setCaptain(person);
        ShipVariantAPI v = member.getVariant().clone();
        v.setSource(VariantSource.REFIT);
        v.addTag(Tags.TAG_NO_AUTOFIT);
        v.addTag(Tags.TAG_AUTOMATED_NO_PENALTY);
        member.setVariant(v, false, true);
        fleet.setCommander(person);
        // DONE GENERATING FLAGSHIP

        // add any ships you want to generate here, which will overwrite PKDefenderPluginImpl (original safeguard)
        // only important thing is the return value on getHandlingPriority, which must be higher than 2

        addAutomated(fleet, "nsp_onslaught_safeguard_elite", null, Commodities.ALPHA_CORE, random);

        addAutomated(fleet, "nsp_dominator_safeguard_elite", null, Commodities.BETA_CORE, random);
        addAutomated(fleet, "nsp_eagle_safeguard_elite", null, Commodities.BETA_CORE, random);
        addAutomated(fleet, "nsp_falcon_safeguard_elite", null, Commodities.BETA_CORE, random);
        addAutomated(fleet, "nsp_falcon_safeguard_escort", null, Commodities.BETA_CORE, random);

        addAutomated(fleet, "nsp_enforcer_safeguard_elite", null, Commodities.GAMMA_CORE, random);
        addAutomated(fleet, "nsp_enforcer_safeguard_elite", null, Commodities.GAMMA_CORE, random);
        addAutomated(fleet, "nsp_enforcer_safeguard_elite", null, Commodities.GAMMA_CORE, random);

        fleet.getFleetData().sort(); // re-organizes fleet data so capships appear first as you'd expect

        fleet.setTransponderOn(false);

        for (FleetMemberAPI curr : fleet.getFleetData().getMembersListCopy()) {
            makeAICoreSkillsGoodForLowTech(curr, true);
            curr.getRepairTracker().setCR(curr.getRepairTracker().getMaxCR());
        }

        if (fleet.getInflater() instanceof DefaultFleetInflater) {
            DefaultFleetInflater dfi = (DefaultFleetInflater) fleet.getInflater();
            DefaultFleetInflaterParams dfip = (DefaultFleetInflaterParams)dfi.getParams();
            dfip.allWeapons = true;
            dfip.averageSMods = 3;
            dfip.quality = 0.4f;

            DModManager.assumeAllShipsAreAutomated = true;
            fleet.inflateIfNeeded();
            fleet.setInflater(null);
            DModManager.assumeAllShipsAreAutomated = false;
        }

        for (FleetMemberAPI curr : fleet.getFleetData().getMembersListCopy()) {
            curr.getVariant().addPermaMod(HullMods.AUTOMATED);
            curr.getVariant().setVariantDisplayName("Automated");
            curr.getVariant().addTag(Tags.TAG_AUTOMATED_NO_PENALTY);
            curr.getVariant().addTag(Tags.VARIANT_UNRESTORABLE);
            curr.getVariant().addTag(Tags.TAG_RETAIN_SMODS_ON_RECOVERY);
            if (curr.isCapital()) {
                curr.getVariant().addTag(Tags.VARIANT_ALWAYS_RECOVERABLE);
            }
        }
    }

    @Override
    public void reportDefeated(SalvageGenFromSeed.SDMParams p, SectorEntityToken entity, CampaignFleetAPI fleet) {

    }
    @Override
    public int getHandlingPriority(Object params) {
        if (!(params instanceof SalvageGenFromSeed.SDMParams)) return 0;
        SalvageGenFromSeed.SDMParams p = (SalvageGenFromSeed.SDMParams) params;

        if (p.entity != null && p.entity.getMemoryWithoutUpdate().contains(
                "$core_pkCache")) {
            return 100;
        }
        return -1;
    }

}
