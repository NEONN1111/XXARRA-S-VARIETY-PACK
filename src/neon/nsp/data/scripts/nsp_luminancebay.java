package neon.nsp.data.scripts;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.impl.campaign.ids.Personalities;
import com.fs.starfarer.api.impl.campaign.ids.Skills;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import neon.nsp.data.scripts.util.CollisionUtils;
import neon.nsp.data.scripts.util.MathUtils;
import org.lazywizard.lazylib.combat.CombatUtils;

import java.util.List;

public class nsp_luminancebay implements MissileAIPlugin, GuidedMissileAI {

    private CombatEngineAPI engine;
    private final MissileAPI missile;
    private CombatEntityAPI target;
    private ShipAPI launchingShip;
    private boolean hasSpawned = false;

    public nsp_luminancebay(MissileAPI missile, ShipAPI launchingShip) {
        if (engine != Global.getCombatEngine()) {
            this.engine = Global.getCombatEngine();
        }
        this.missile = missile;
        this.launchingShip = launchingShip;
        missile.setArmingTime(missile.getArmingTime()-(float)(Math.random()/4));
    }

    @Override
    public void advance(float amount) {
        if (engine.isPaused() || hasSpawned) {return;}

        if(!CollisionUtils.isPointWithinCollisionCircle(missile.getLocation(), launchingShip)) {
            boolean shipsInRange = !CombatUtils.getShipsWithinRange(missile.getLocation(), 500f).isEmpty();

            if(shipsInRange && MathUtils.getDistance(missile,launchingShip) > 80f &&
                    !CollisionUtils.isPointWithinBounds(missile.getLocation(),launchingShip)) {

                missile.setArmingTime(0f);
                CombatFleetManagerAPI cfm = engine.getFleetManager(1);
                cfm.setSuppressDeploymentMessages(true);
                ShipAPI pod = cfm.spawnShipOrWing("nsp_parasite_standard",missile.getLocation(),0f);
                pod.setOwner(missile.getSource().getOriginalOwner());
                pod.setFacing(missile.getFacing());
                pod.getVelocity().set(missile.getVelocity());
                pod.getMutableStats().getFighterRefitTimeMult().modifyPercent(pod.getId(),9999f);

                // Clone the officer from the launching ship
                cloneOfficerFromLaunchingShip(pod);

                cfm.setSuppressDeploymentMessages(false);
                hasSpawned = true;
            }
        }

        if(missile.isArmed()) {
            engine.removeEntity(missile);
        }
    }

    private void cloneOfficerFromLaunchingShip(ShipAPI spawnedShip) {
        PersonAPI originalCaptain = launchingShip.getCaptain();

        if (originalCaptain != null && !originalCaptain.isDefault()) {
            // Create a clone of the officer
            PersonAPI clonedOfficer = Global.getFactory().createPerson();

            // Get the faction ID based on the ship's owner
            String factionId = getFactionIdForOwner(spawnedShip.getOwner());
            clonedOfficer.setFaction(factionId);
            clonedOfficer.setPostId(originalCaptain.getPostId());
            clonedOfficer.setPersonality(originalCaptain.getPersonalityAPI().getId());
            clonedOfficer.setRankId(originalCaptain.getRankId());

            // Use a modified name to indicate it's a clone
            clonedOfficer.getName().setFirst(originalCaptain.getName().getFirst());
            clonedOfficer.getName().setLast(originalCaptain.getName().getLast() + " (Clone)");
            clonedOfficer.setPortraitSprite(originalCaptain.getPortraitSprite());

            // Clone stats and skills
            clonedOfficer.getStats().setLevel(originalCaptain.getStats().getLevel());

            // Copy all skills - need to know which skills exist
            // Using some common skills as an example
            String[] commonSkills = {
                    Skills.HELMSMANSHIP,
                    Skills.TARGET_ANALYSIS,
                    Skills.GUNNERY_IMPLANTS,
                    Skills.IMPACT_MITIGATION,
                    Skills.DAMAGE_CONTROL,
                    Skills.POINT_DEFENSE,
                    Skills.FIELD_MODULATION,
                    Skills.POLARIZED_ARMOR,
                    Skills.SYSTEMS_EXPERTISE,
                    Skills.MISSILE_SPECIALIZATION,
                    Skills.BALLISTIC_MASTERY,
                    Skills.ENERGY_WEAPON_MASTERY
            };

            for (String skillId : commonSkills) {
                if (originalCaptain.getStats().hasSkill(skillId)) {
                    float level = originalCaptain.getStats().getSkillLevel(skillId);
                    clonedOfficer.getStats().setSkillLevel(skillId, level);
                }
            }

            // Set officer to spawned ship
            spawnedShip.setCaptain(clonedOfficer);

            // Apply officer stats to ship - this happens automatically when setCaptain is called
            // For immediate application, we need to manually apply the stats
            applyOfficerStatsToShip(clonedOfficer, spawnedShip.getMutableStats());

            // If it's a fighter/wing, handle it differently
            if (spawnedShip.getWing() != null) {
                // Fighter wings don't have a commander in the same way
                // The captain is already set on the ship itself
            }
        } else {
            // If no officer exists on launching ship, create a generic one
            applyGenericOfficer(spawnedShip);
        }
    }

    private String getFactionIdForOwner(int owner) {
        // Map owner to faction ID
        switch (owner) {
            case 0: return Factions.PLAYER;
            case 1: return Factions.HEGEMONY;
            case 2: return Factions.TRITACHYON;
            case 3: return Factions.PIRATES;
            case 4: return Factions.LUDDIC_CHURCH;
            case 5: return Factions.LUDDIC_PATH;
            case 6: return Factions.INDEPENDENT;
            case 7: return Factions.REMNANTS;
            default: return Factions.NEUTRAL;
        }
    }

    private void applyOfficerStatsToShip(PersonAPI officer, MutableShipStatsAPI stats) {
        // This is a simplified version of applying officer stats
        // In the real game, this is handled internally when setCaptain is called

        // Apply skill bonuses
        for (String skillId : new String[]{
                Skills.HELMSMANSHIP,
                Skills.TARGET_ANALYSIS,
                Skills.GUNNERY_IMPLANTS,
                Skills.IMPACT_MITIGATION,
                Skills.DAMAGE_CONTROL,
                Skills.POINT_DEFENSE
        }) {
            if (officer.getStats().hasSkill(skillId)) {
                float level = officer.getStats().getSkillLevel(skillId);
                // Apply skill effects based on skill ID and level
                applySkillEffect(skillId, level, stats);
            }
        }
    }

    private void applySkillEffect(String skillId, float level, MutableShipStatsAPI stats) {
        // Simplified skill effects - in reality each skill has complex effects
        switch (skillId) {
            case Skills.HELMSMANSHIP:
                stats.getMaxSpeed().modifyPercent("officer", level * 10f);
                stats.getAcceleration().modifyPercent("officer", level * 15f);
                break;
            case Skills.TARGET_ANALYSIS:
                stats.getAutofireAimAccuracy().modifyPercent("officer", level * 10f);
                break;
            case Skills.GUNNERY_IMPLANTS:
                stats.getBallisticWeaponRangeBonus().modifyPercent("officer", level * 10f);
                stats.getEnergyWeaponRangeBonus().modifyPercent("officer", level * 10f);
                break;
        }
    }

    private void applyGenericOfficer(ShipAPI ship) {
        PersonAPI officer = Global.getFactory().createPerson();
        String factionId = getFactionIdForOwner(ship.getOwner());
        officer.setFaction(factionId);
        officer.setPersonality(Personalities.AGGRESSIVE);
        officer.getName().setFirst("Gamma");
        officer.getName().setLast("Core");
        officer.setPortraitSprite("graphics/portraits/portrait_ai1b.png");

        // Add basic combat skills
        officer.getStats().setSkillLevel(Skills.HELMSMANSHIP, 2);
        officer.getStats().setSkillLevel(Skills.DAMAGE_CONTROL, 2);
        officer.getStats().setSkillLevel(Skills.TARGET_ANALYSIS, 2);

        ship.setCaptain(officer);

        // Apply officer stats to ship
        applyOfficerStatsToShip(officer, ship.getMutableStats());

        if (ship.getWing() != null) {
            // Fighter wings don't have a commander in the same way
            // The captain is already set on the ship itself
        }
    }

    @Override
    public CombatEntityAPI getTarget() {
        return target;
    }

    @Override
    public void setTarget(CombatEntityAPI target) {
        this.target = target;
    }
}