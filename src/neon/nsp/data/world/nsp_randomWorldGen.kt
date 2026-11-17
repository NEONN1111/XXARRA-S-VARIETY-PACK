package neon.nsp.data.world

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.*
import com.fs.starfarer.api.fleet.FleetMemberType
import com.fs.starfarer.api.impl.campaign.ids.*
import com.fs.starfarer.api.impl.campaign.procgen.DefenderDataOverride
import com.fs.starfarer.api.impl.campaign.procgen.themes.BaseThemeGenerator
import com.fs.starfarer.api.impl.campaign.procgen.themes.BaseThemeGenerator.LocationType
import com.fs.starfarer.api.impl.campaign.procgen.themes.RemnantOfficerGeneratorPlugin.integrateAndAdaptCoreForAIFleet
import com.fs.starfarer.api.impl.campaign.procgen.themes.RemnantSeededFleetManager
import com.fs.starfarer.api.impl.campaign.rulecmd.salvage.special.BaseSalvageSpecial
import com.fs.starfarer.api.util.Misc
import com.fs.starfarer.api.util.WeightedRandomPicker
import neon.nsp.data.econ.items.nsp_mindJackDialogPlugin.Companion.splicerNegativeTag
import neon.nsp.data.econ.items.nsp_mindJackDialogPlugin.Companion.splicerPositiveTag
import neon.nsp.data.plugins.NSPModPlugin.SPLICERS_ADDED_KEY
import neon.nsp.data.scripts.NSP_lunaStuff
import org.apache.log4j.Logger
import org.lazywizard.lazylib.MathUtils

class nsp_randomWorldGen {
    val logger = Logger.getLogger(this::class.java)

    fun addQuestGeneStation(): Boolean {
        var targetStationSystem = Global.getSector().starSystems.filter {
            it.isProcgen && !it.hasTag(Tags.THEME_HIDDEN) && it.isNebula
        }.randomOrNull()
        if (targetStationSystem == null) {
            targetStationSystem = Global.getSector().starSystems.filter {
                it.isProcgen && !it.hasTag(Tags.THEME_HIDDEN)
            }.random()!! // just crash tbh
        }
        val weights = LinkedHashMap<LocationType, Float>()
        weights[LocationType.IN_RING] = 15f
        weights[LocationType.IN_SMALL_NEBULA] = 25f
        weights[LocationType.OUTER_SYSTEM] = 5f
        weights[LocationType.GAS_GIANT_ORBIT] = 10f
        weights[LocationType.PLANET_ORBIT] = 10f
        val locs = BaseThemeGenerator.getLocations(Misc.random, targetStationSystem, null, 100f, weights)
        if (locs.isEmpty) return false // not even possible, but who knows?
        val loc = locs.pick()
        val station = BaseThemeGenerator.addNonSalvageEntity(targetStationSystem, loc, "nsp_biotechStation", Factions.NEUTRAL).entity
        station.id = "nsp_questGeneStation"
        val fleet = Global.getFactory().createEmptyFleet(Factions.REMNANTS, "Company Copyright Crusade", true)
        val flag = Global.getFactory().createFleetMember(FleetMemberType.SHIP, "fulgent_Assault")
        flag.isFlagship = true
        fleet.fleetData.addFleetMember(flag)
        fleet.fleetData.setFlagship(flag)
        fleet.fleetData.ensureHasFlagship() // this shit is SO tempermental
        for (i in 0 until 5) {
            val fm = Global.getFactory().createFleetMember(FleetMemberType.SHIP, "glimmer_Assault")
            val fm2 = Global.getFactory().createFleetMember(FleetMemberType.SHIP, "lumen_Standard")
            fleet.fleetData.addFleetMember(fm)
            fleet.fleetData.addFleetMember(fm2)
        }
        fleet.fleetData.membersListCopy.forEach {
            if (it.isFlagship) {
                val cap = Misc.getAICoreOfficerPlugin(Commodities.ALPHA_CORE).createPerson(Commodities.ALPHA_CORE, Factions.REMNANTS, Misc.random)
                cap.stats.apply {
                    setSkillLevel(Skills.WOLFPACK_TACTICS, 1f)
                    setSkillLevel(Skills.ELECTRONIC_WARFARE, 1f)
                    setSkillLevel(Skills.COORDINATED_MANEUVERS, 1f)
                    setSkillLevel(Skills.FORCE_CONCENTRATION, 1f)
                }
                it.captain = cap
                fleet.commander = cap
            } else {
                val cap = Misc.getAICoreOfficerPlugin(Commodities.GAMMA_CORE).createPerson(Commodities.GAMMA_CORE, Factions.REMNANTS, Misc.random)
                it.captain = cap
            }
            integrateAndAdaptCoreForAIFleet(it)
            fleet.forceSync()
            it.updateStats()
            it.repairTracker.cr = it.repairTracker.maxCR
        }
        fleet.isNoFactionInName = true
        RemnantSeededFleetManager.addRemnantInteractionConfig(fleet)
        fleet.memoryWithoutUpdate.apply {
            set("\$nsp_geneDefenderFleet", true)
            set(MemFlags.MEMORY_KEY_NO_REP_IMPACT, true)
        }
        BaseThemeGenerator.convertOrbitPointingDown(station)
        station.memoryWithoutUpdate.set("\$nsp_splicer", true)
        station.memoryWithoutUpdate.set("\$hasDefenders", true) // idk man i'm just tryin some shit
        station.memoryWithoutUpdate.set("\$defenderFleet", fleet)
        logger.info("added quest gene station to ${targetStationSystem!!.name} in the ${targetStationSystem.constellation?.name}")
        // defender data override apparently does not work on this for some reason
        return true
    }

    fun addGeneSplicers() {
        if (Global.getSector().memoryWithoutUpdate.getBoolean(SPLICERS_ADDED_KEY)) return
        logger.info("adding gene splicers")
        // added to random orbital habitat and research station salvage entities in the sector
        // predefined number are empty, duds that contain negative skills, or specials that contain unique skills
        // all other contain random vanilla skills
        val totalSystems = Global.getSector().starSystems.filter { it.isProcgen }.size
        val systemsPerEmpty = 11
        val systemsPerFilled = 26
        val habs = ArrayList<SectorEntityToken>()
        val acceptable = ArrayList<String>()
        acceptable.add(Entities.STATION_RESEARCH)
        acceptable.add(Entities.STATION_RESEARCH_REMNANT)
        acceptable.add(Entities.ORBITAL_HABITAT)
        acceptable.add(Entities.ORBITAL_HABITAT_REMNANT)
        val accompanyEmptyChance = 0.50f
        var addedDuds = 0
        var addedSpecials = 0
        val numFilled = Math.max(6, totalSystems / systemsPerFilled)
        val numEmpty = Math.max(15, totalSystems / systemsPerEmpty) // higher of int or system factor
        var addedEmpty = 0
        val numChosen = 3 // regardless of how many pos/neg skills exist, pick this many of each
        val skills = Global.getSettings().skillIds.filter {
            Global.getSettings().getSkillSpec(it).isCombatOfficerSkill && Global.getSettings().getSkillSpec(it).sourceMod == null
        }
        val negativeSkills = Global.getSettings().skillIds.filter { Global.getSettings().getSkillSpec(it).hasTag(splicerNegativeTag) }
        val positiveSkills = Global.getSettings().skillIds.filter { Global.getSettings().getSkillSpec(it).hasTag(splicerPositiveTag) }
        val negatives = WeightedRandomPicker<String>() // create list of all skills for splicers, add to picker, then pick and remove
        negatives.addAll(negativeSkills)
        val positives = WeightedRandomPicker<String>() // any really troublesome negative skills should have an extra way to remove them beyond finding an empty splicer
        positives.addAll(positiveSkills)
        val min = numEmpty + (numChosen * 2) + numFilled
        val max = min // + 2
        val addedSkillIDs = ArrayList<String>()

        val numToAdd = MathUtils.getRandomNumberInRange(min, max)
        logger.info("adding $numToAdd gene splicers")
        logger.info("of these, $numEmpty are empty")

        Global.getSector().starSystems.filter { it.isProcgen && !it.hasTag(Tags.THEME_HIDDEN) }.forEach {
            habs.addAll(it.customEntities.filter {
                salvage -> salvage.hasTag(Tags.SALVAGEABLE) && acceptable.contains(salvage.customEntitySpec.id)
            })
        }
        if (habs.isEmpty()) {
            logger.info("can't find anything to add gene splicers to, will try again next load...")
            return
        }
        Global.getSector().memoryWithoutUpdate.set(SPLICERS_ADDED_KEY, true)
        for (i in 0 until numToAdd) {
            var addExtraEmpty = false
            var target = habs.filter { !it.memoryWithoutUpdate.getBoolean("\$nsp_splicer") }.randomOrNull()
            if (target == null) target = habs.randomOrNull() ?: continue // fallback to any if we already added to all
            // pretty unlikely, except on small sector which may not have enough stations... but still unlikely.
            val cargo = Global.getFactory().createCargo(false)
            var data = skills.randomOrNull()
            when {
                addedSpecials < numChosen -> {
                    addedSpecials++
                    data = if (positives.isEmpty) {
                        skills.randomOrNull()
                    } else positives.pickAndRemove()
                    if (Math.random() <= accompanyEmptyChance) {
                        addExtraEmpty = true
                        addedEmpty++
                    }
                }
                addedDuds < numChosen -> {
                    addedDuds++
                    data = if (negatives.isEmpty) {
                        skills.randomOrNull()
                    } else negatives.pickAndRemove()
                    if (Math.random() <= accompanyEmptyChance) {
                        addExtraEmpty = true
                        addedEmpty++
                    }
                }
                addedEmpty < numEmpty -> {
                    addedEmpty++
                    data = null
                }
            }

            if (addExtraEmpty) { // chance to add an extra empty one paired with a filled one, to avoid making save loads necessary if skill is very bad
                cargo.addSpecial(SpecialItemData("nsp_geneSplicer", null), 1f)
            }
            val skillID = data
            if (data != null) {
                val lvl = MathUtils.getRandomNumberInRange(1, 2).toString()
                data += "$$lvl"
            }
            cargo.addSpecial(SpecialItemData("nsp_geneSplicer", data), 1f)
            BaseSalvageSpecial.addExtraSalvage(target, cargo)
            target.memoryWithoutUpdate.set("\$nsp_splicer", true)
            if (skillID != null) {
                addedSkillIDs.add(skillID)
                logger.info("spawning splicer with skill $skillID")
            }
        }
        var num = Global.getSettings().getInt("nsp_numGeneticStations")
        if (Global.getSettings().modManager.isModEnabled("lunalib")) {
            num = NSP_lunaStuff.getLunaInt("nsp_numGeneticStations")
        }
        logger.info("adding $num genetic stations")
        var addedSpecial = false
        for (i in 0 until num) {
            val targetStationSystem = Global.getSector().starSystems.filter {
                it.hasTag(Tags.THEME_DERELICT) && it.isProcgen && !it.hasTag(Tags.THEME_HIDDEN) && !it.isNebula && it.star != null
            }.randomOrNull()
            if (targetStationSystem == null) continue
            val weights = LinkedHashMap<LocationType, Float>()
            weights[LocationType.IN_RING] = 10f
            weights[LocationType.GAS_GIANT_ORBIT] = 10f
            weights[LocationType.NEAR_STAR] = 5f
            weights[LocationType.PLANET_ORBIT] = 15f
            val locs = BaseThemeGenerator.getLocations(Misc.random, targetStationSystem, null, 100f, weights)
            if (locs.isEmpty) continue // not even possible, but who knows?
            val loc = locs.pick()
            val station = BaseThemeGenerator.addNonSalvageEntity(targetStationSystem, loc, "nsp_biotechStation", Factions.NEUTRAL).entity
            BaseThemeGenerator.convertOrbitPointingDown(station)
            if (!addedSpecial) {
                addedSpecial = true
                station.memoryWithoutUpdate.set("\$nsp_biotechSpecial", true)
            }
            station.memoryWithoutUpdate.set("\$nsp_splicer", true)
            station.memoryWithoutUpdate.set("\$nsp_addedSplicerSkills", addedSkillIDs)
            logger.info("added gene station to ${targetStationSystem.name} in the ${targetStationSystem.constellation?.name}")
        }
        logger.info("successfully added gene splicers")
    }

    fun modifyRemnantHabForCryos() {
        val habs = Global.getSector().getEntitiesWithTag(Tags.SALVAGEABLE).filter {
            !it.hasTag(Tags.NOT_RANDOM_MISSION_TARGET) &&
                it.starSystem?.hasTag(Tags.THEME_REMNANT_RESURGENT) == true &&
                (it.customEntityType == Entities.ORBITAL_HABITAT || it.customEntityType == Entities.ORBITAL_HABITAT_REMNANT)
        }
        val hab = habs.filter { it.memoryWithoutUpdate.contains("\$nsp_splicer") }.randomOrNull()
            ?: habs.randomOrNull() ?: return // prefer habs with splicers in them, makes it easier to find b/c of related content
        hab.memoryWithoutUpdate.set("\$nsp_cryoHab", true)
        hab.extendedDetectedAtRange = 2000f
        hab.sensorProfile *= 2f
        if (!hab.memoryWithoutUpdate.contains("\$defenderFleet")) {
            Misc.setDefenderOverride(hab, DefenderDataOverride(Factions.REMNANTS, 1f, 140f, 140f))
        }
        logger.info("added cryo hint to habitat in ${hab.starSystem?.name} star system, in ${hab.starSystem?.constellation?.name} constellation")
    }
}