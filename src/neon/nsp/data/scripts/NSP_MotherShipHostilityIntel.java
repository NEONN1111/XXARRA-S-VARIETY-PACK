package neon.nsp.data.scripts;

import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.campaign.comm.IntelInfoPlugin;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.listeners.EconomyTickListener;
import com.fs.starfarer.api.impl.campaign.fleets.FleetFactoryV3;
import com.fs.starfarer.api.impl.campaign.ids.*;
import com.fs.starfarer.api.impl.campaign.intel.BaseIntelPlugin;
import com.fs.starfarer.api.ui.SectorMapAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.IntervalUtil;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.api.util.WeightedRandomPicker;
import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.util.vector.Vector2f;

import java.util.*;

public class NSP_MotherShipHostilityIntel extends BaseIntelPlugin implements EconomyTickListener {

    private ArrayList<String> factionlist = new ArrayList<>();
    private Long stamp = null;
    private float days = 90f;
    private final String title = "Hostile Operations";
    private final String sprite = Global.getSettings().getSpriteName("intel", "hostile_activity");

    public NSP_MotherShipHostilityIntel() {
        for (FactionAPI faction : Global.getSector().getAllFactions()) {
            String factionId = faction.getId();
            if (factionId.equals(Factions.PLAYER)) continue;
            if (factionId.equals(Factions.DERELICT)) continue;
            if (factionId.equals("nex_derelict")) continue;
            if (factionId.equals(Factions.REMNANTS)) continue;
            if (factionId.equals(Factions.OMEGA)) continue;
            if (factionId.equals(Factions.TRITACHYON)) continue;
            if (factionId.equals("sotf_dustkeepers")) continue;
            if (factionId.equals("sotf_dustkeepers_proxies")) continue;
            if (factionId.equals("sotf_sierra_faction")) continue;
            if (factionId.equals("sotf_dreaminggestalt")) continue;

            factionlist.add(factionId);
        }
    }

    // the gist...
    // use reporteconomytick, check if player is near or in core worlds (check hyperspace loc if in hyper, otherwise check system dist to center)
    // plus check fleet points, probably like > 200?
    // if success, add this intel which tracks time from last spawn and player location
    // if time from last spawn is greater than some value within like 90 to 180 days and player is in core worlds, spawns several random faction hunter killer fleets to chase them down
    // where from? umm... have to figure that out. try locating a random market in all systems near the player, maybe? then spawn that fac? if it's not TT
    // maybe use a whitelist
    // once enough time passes from this intel first being added, triggers very large attack fleet and then stops entirely if defeated

    @Override
    public Set<String> getIntelTags(SectorMapAPI map) {
        return super.getIntelTags(map);
    }

    @Override
    public boolean autoAddCampaignMessage() {
        return true;
    }

    @Override
    public String getIcon() {
        return sprite;
    }

    @Override
    public String getName() {
        return title;
    }

    @Override
    public String getSortString() {
        return title;
    }

    @Override
    public IntelInfoPlugin.IntelSortTier getSortTier() {
        return IntelInfoPlugin.IntelSortTier.TIER_2;
    }

    @Override
    public boolean isHidden() {
        return false;
    }

    @Override
    public boolean isImportant() {
        return true;
    }

    @Override
    public String getCommMessageSound() {
        return getSoundMajorPosting();
    }

    @Override
    public boolean shouldRemoveIntel() {
        return false;
    }

    @Override
    public void createIntelInfo(TooltipMakerAPI info, IntelInfoPlugin.ListInfoMode mode) {
        info.addTitle(title);
        info.addPara("Your fleet is perceived as a growing threat.", 5f);
        info.addPara("Others may be dispatched to hunt you down.", 5f);
    }

    @Override
    public void createSmallDescription(TooltipMakerAPI info, float width, float height) {
        info.addPara("Your fleet has grown large enough to be considered an anomaly by numerous military operatives working within the Core Worlds.", 5f);
        info.addPara("Occasionally, you may be tailed by hunter-killer fleets if within the core worlds, or bounty hunters may follow you to the fringes.", 5f);
        info.addPara("They will not take you lightly, and do their best to outmatch your fleet.", 5f);
    }

    @Override
    public Object getListInfoParam() {
        return IntelInfoPlugin.ListInfoMode.MESSAGES;
    }

    @Override
    public void advanceImpl(float amount) {
        if (!Global.getSector().getListenerManager().hasListenerOfClass(NSP_MotherShipHostilityIntel.class)) {
            Global.getSector().getListenerManager().addListener(new NSP_MotherShipHostilityIntel(), true);
        }
    }

    @Override
    public void reportEconomyTick(int iterIndex) {
        if (Global.getSector().getPlayerFleet() == null) return;
        CampaignFleetAPI pfleet = Global.getSector().getPlayerFleet();

        // if (Global.getSector().clock.elapsedDaysSinceGameStart() > 180f && Global.getSector().playerFleet.fleetPoints > 200f && hide){
        //      hide = false
        //      Global.getSector().campaignUI.addMessage(this)
        //  }

        if (stamp == null) {
            stamp = Global.getSector().getClock().getTimestamp();
            return;
        }

        if (Global.getSector().getClock().getElapsedDaysSince(stamp) > days) {
            days = MathUtils.getRandomNumberInRange(120f, 240f);
            stamp = null;

            Vector2f loc = getPlayerLocation();
            float distance = Misc.getDistance(loc, new Vector2f(0f, 0f));

            if (distance > 12000f && distance < 35000f) {
                // In fringe/outer areas - spawn mercenary bounty hunters
                if (Global.getSector().getFaction(Factions.PLAYER).getRelationship(Factions.INDEPENDENT) <= -0.6f) {
                    // Use MagicCampaign if available, otherwise create fleet manually
                    try {
                        Class<?> magicCampaignClass = Class.forName("org.magiclib.util.MagicCampaign");
                        Object magicCampaign = magicCampaignClass.getMethod("createFleetBuilder").invoke(null);

                        // This is a simplified version - you'll need to properly implement MagicCampaign methods
                        // For now, let's create a fleet using FleetFactoryV3
                        CampaignFleetAPI fleet = createMercenaryFleet(pfleet);

                        fleet.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_MAKE_HOLD_VS_STRONGER, true);
                        fleet.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_MAKE_HOSTILE, true);
                        fleet.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_IGNORE_PLAYER_COMMS, true);
                        fleet.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_PURSUE_PLAYER, true);
                        fleet.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_MAKE_PREVENT_DISENGAGE, true);
                        fleet.getMemoryWithoutUpdate().set(MemFlags.FLEET_IGNORES_OTHER_FLEETS, true);
                        fleet.getMemoryWithoutUpdate().set(MemFlags.FLEET_IGNORED_BY_OTHER_FLEETS, true);
                        fleet.getMemoryWithoutUpdate().set(MemFlags.FLEET_DO_NOT_IGNORE_PLAYER, true);

                        fleet.addScript(new FleetDespawner(fleet, Global.getSector().getClock().getTimestamp()));
                    } catch (Exception e) {
                        // MagicCampaign not available, create fleet manually
                        CampaignFleetAPI fleet = createMercenaryFleet(pfleet);
                        fleet.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_MAKE_HOSTILE, true);
                        fleet.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_PURSUE_PLAYER, true);
                        fleet.addScript(new FleetDespawner(fleet, Global.getSector().getClock().getTimestamp()));
                    }
                }
            } else {
                // In core worlds - spawn faction hunter fleets
                List<MarketAPI> eligibleMarkets = new ArrayList<>();
                for (MarketAPI market : Misc.getNearbyMarkets(getPlayerLocation(), 25f)) {
                    if (factionlist.contains(market.getFactionId()) &&
                            market.getSize() > 3 &&
                            market.hasSpaceport() &&
                            !market.isPlayerOwned() &&
                            market.getPrimaryEntity() != null &&
                            market.getFaction().getRelationship(Factions.PLAYER) <= -0.6f) {
                        eligibleMarkets.add(market);
                    }
                }

                if (eligibleMarkets.isEmpty()) return;

                WeightedRandomPicker<MarketAPI> marketPicker = new WeightedRandomPicker<>();
                marketPicker.addAll(eligibleMarkets);

                for (int i = 0; i < 3; i++) {
                    MarketAPI spawnMarket = marketPicker.pick();
                    if (spawnMarket == null) continue;

                    try {
                        // Try to use MagicCampaign
                        Class<?> magicCampaignClass = Class.forName("org.magiclib.util.MagicCampaign");
                        Object magicCampaign = magicCampaignClass.getMethod("createFleetBuilder").invoke(null);

                        // This is a simplified version - implement properly based on MagicCampaign API
                        CampaignFleetAPI fleet = createFactionFleet(pfleet, spawnMarket);

                        fleet.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_MAKE_HOLD_VS_STRONGER, true);
                        fleet.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_MAKE_HOSTILE, true);
                        fleet.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_IGNORE_PLAYER_COMMS, true);
                        fleet.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_PURSUE_PLAYER, true);
                        fleet.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_MAKE_PREVENT_DISENGAGE, true);

                        fleet.addScript(new FleetDespawner(fleet, Global.getSector().getClock().getTimestamp()));
                    } catch (Exception e) {
                        // MagicCampaign not available, create fleet manually
                        CampaignFleetAPI fleet = createFactionFleet(pfleet, spawnMarket);
                        fleet.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_MAKE_HOSTILE, true);
                        fleet.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_PURSUE_PLAYER, true);
                        fleet.addScript(new FleetDespawner(fleet, Global.getSector().getClock().getTimestamp()));
                    }
                }
            }
        }
    }

    private CampaignFleetAPI createMercenaryFleet(CampaignFleetAPI playerFleet) {
        // Create a mercenary bounty hunter fleet
        SectorEntityToken spawnLoc = Misc.findNearestJumpPointTo(playerFleet, true);

        CampaignFleetAPI fleet = FleetFactoryV3.createEmptyFleet(
                Factions.INDEPENDENT,
                FleetTypes.MERC_BOUNTY_HUNTER,
                (MarketAPI) spawnLoc
        );

        int fp = Math.min(Math.round(playerFleet.getFleetPoints() * 1.1f), 600);
        fleet.getFleetData().addFleetMember("hammerhead_Attack");
        fleet.getFleetData().addFleetMember("enforcer_CS");
        // Add more ships based on fleet points

        fleet.setName("Mercenary Hunters");
        fleet.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_PURSUE_PLAYER, true);
        fleet.addAssignment(FleetAssignment.INTERCEPT, playerFleet, 1000f);
        fleet.setTransponderOn(true);

        return fleet;
    }

    private CampaignFleetAPI createFactionFleet(CampaignFleetAPI playerFleet, MarketAPI spawnMarket) {
        // Create a faction hunter fleet
        int fp = Math.min(Math.round(playerFleet.getFleetPoints() / 1.5f), 250);

        CampaignFleetAPI fleet = FleetFactoryV3.createEmptyFleet(
                spawnMarket.getFactionId(),
                FleetTypes.TASK_FORCE,
                (MarketAPI) spawnMarket.getPrimaryEntity()
        );

        // Add ships based on fleet points
        fleet.getFleetData().addFleetMember("lasher_CS");
        fleet.getFleetData().addFleetMember("wolf_CS");

        fleet.setName(spawnMarket.getFaction().getDisplayName() + " Hunters");
        fleet.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_PURSUE_PLAYER, true);
        fleet.addAssignment(FleetAssignment.INTERCEPT, playerFleet, 1000f);

        return fleet;
    }

    @Override
    public void reportEconomyMonthEnd() {
        // Empty implementation
    }

    private Vector2f getPlayerLocation() {
        CampaignFleetAPI pfleet = Global.getSector().getPlayerFleet();
        if (pfleet.isInHyperspace() || pfleet.getStarSystem() == null) {
            return pfleet.getLocation();
        }
        return pfleet.getStarSystem().getHyperspaceAnchor().getLocation();
    }

    public static class FleetDespawner implements EveryFrameScript {
        private CampaignFleetAPI fleet;
        private long time;
        private boolean done = false;
        private IntervalUtil check = new IntervalUtil(1f, 1f);

        public FleetDespawner(CampaignFleetAPI fleet, long time) {
            this.fleet = fleet;
            this.time = time;
        }

        @Override
        public void advance(float amount) {
            check.advance(amount);
            if (check.intervalElapsed()) {
                if (Global.getSector().getClock().getElapsedDaysSince(time) > 120f &&
                        !fleet.isVisibleToPlayerFleet()) {
                    fleet.despawn();
                    done = true;
                }
            }
        }

        @Override
        public boolean isDone() {
            return done;
        }

        @Override
        public boolean runWhilePaused() {
            return false;
        }
    }
}