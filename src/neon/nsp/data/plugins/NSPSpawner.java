package neon.nsp.data.plugins;

import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.impl.campaign.BaseGenericPlugin;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import com.fs.starfarer.api.impl.campaign.rulecmd.salvage.SalvageGenFromSeed;

import java.util.Random;

public class NSPSpawner extends BaseGenericPlugin implements SalvageGenFromSeed.SalvageDefenderModificationPlugin{

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
        fleet.setNoFactionInName(true);
        fleet.setName("Remnants of Task Force Safeguard");
        fleet.getFleetData().clear(); // clear any pre-existing fleet
        fleet.setNoFactionInName(true); // so it doesn't appear as "derelict automated mining force"
        fleet.setFaction(Factions.HEGEMONY, true); // sets faction to orange men
        fleet.getFleetData().setShipNameRandom(random);
    }

    @Override
    public void reportDefeated(SalvageGenFromSeed.SDMParams p, SectorEntityToken entity, CampaignFleetAPI fleet) {

    }
}
