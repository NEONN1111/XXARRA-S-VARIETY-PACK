package neon.nsp.data

import com.fs.starfarer.api.util.WeightedRandomPicker
import java.awt.Color

class nsp_referenceSheet {

    init {
    }


    companion object {
        val meatsoundlist = WeightedRandomPicker<String>()

        // commodity/special items
        val DECALS_ID = "cs_counterfeitBattlegroupDecals"
        val SIGMACORE_ID = "cs_core_sigma"
        val SLAUGHTCORE_ID = "cs_core_onslaught"
        val LUDDCORE_ID = "cs_core_absolver"
        val ASSIMCORE_ID = "cs_core_ouroboros"
        val TRANSCENDENCE = "cs_transcendence"
        val BADLOBSTER = "cs_badTemperedLobster"
        val ERRATICFORGE = "cs_erraticForge"
        val BOMBFAB = "cs_AMmicroforge"
        val PLANULA = "cs_planula"
        val POCKETICBM = "cs_pocketICBM"
        val TERRAFRAGMENT = "cs_terrafragment"
        val BAGOFHOLDING = "cs_bagOfHolding"
        val FORMINGFLUID = "cs_formingFluid"
        val INERTIANEGATOR = "cs_inertiaNegatorNode"
        val DUBIOUSREMOTE = "cs_dubiousRemote"
        val METALLICAGGREGATE = "cs_metallicAggregate"

        // ability related
        val TELLER = "cs_automatedTeller"
        val FETCH = "cs_protoDrakeFetch"
        val BAGOFHOLDINGABILITY = "cs_bagOfHoldingAbility"
        val CLARION_CALL = "cs_clarionCall"
        val STORAGE_HANGAR = "cs_storageHangar"
        val bombAbility = "cs_waveFrontEmitter" // id for hullmod is identical
        val bombEntity = "cs_proxycharge"

        // hull IDs if needed

        val TRUEEYEOFCTHULHU = "cs_trueeye"
        val LOCUSDRONE = "cs_locusdrone"

        // hullmod IDs

        val MILITARIZEDREFUND = "cs_militarizedRefund"
        val CONTROLLERMOD = "cs_controllerMod"

        val AUTOMATED_DMOD = "cs_autoDMods"
        val AUTOMATED_DESTROYEDMOD = "cs_autoDamageStruct" // equivalent of structural damage for automated ships, i guess?
        val SUPERDMOD = "cs_superDMod" // super d-mods that don't require anything else
        val SUPERDMOD_SYSTEM = "cs_superDMod_system"
        val SUPERDMOD_FIGHTER = "cs_superDMod_fighter"
        val SUPERDMOD_PHASE = "cs_superDMod_phase"
        val SUPERDMOD_SHIELD = "cs_superDMod_shield"
        val SUPERDMOD_MISSILE = "cs_superDMod_missile"

        val bombHullMod = "cs_bombAutofab"

        const val FIREBREATHMOD = "cs_dragonFireBreathHullmod"
        const val FROSTBREATHMOD = "cs_dragonFrostBreathHullmod"
        const val FETIDBREATHMOD = "cs_dragonFetidBreathHullmod"
        const val FULGORBREATHMOD = "cs_dragonFulgorBreathHullmod"

        val ERRATICMOD = "cs_kiteStorm"
        val RESURRECTIONMOD = "cs_blazingResurrection"
        val POCKETICBMMOD = "cs_pocketICBM"
        val AUTOFABMOD = "cs_bombAutofab"
        val TERRAPRISMAMOD = "cs_terraprismahullmod"
        val LOCUSINTERDICTORMOD = "cs_locusInterdictorHullmod"
        val LOCUSMENDERMOD = "cs_locusMenderHullmod"

        val AISWITCHAUTOMATED = "cs_aiswitch_auto"
        val AISWITCHMANUAL = "cs_aiswitch_manual"

        // weapon IDs

        val FIREBREATH = "cs_dragonFireBreath"
        val FROSTBREATH = "cs_dragonFrostBreath"
        val FETIDBREATH = "cs_dragonFetidBreath"
        val FULGORBREATH = "cs_dragonFulgorBreath"
        val PRIMELASER = "cs_primelaser"
        val PRIMETASER = "cs_primetaser"
        val PRIMEHOWITZER = "cs_primehowitzer"
        val MOONLORDRAY = "cs_moonlorddeathray"
        val MOONLORDBOLT = "cs_moonlordbolt"
        val MOONLORDPHANTASM = "cs_moonlordphantasmaleye"
        val MOONLORDEYEBOMB = "cs_moonlordeyelauncher"
        val INTERDICTOR = "cs_locusinterdictor"
        val MENDER = "cs_locusmender"

        // skill IDs

        val PREDATORY_EVOLUTION = "nsp_predatoryEvolution"

        val SEPHIRASKILL_ELIGIBLE = "\$cs_sephiraSkill"
        val SEPHIRA_LASTLEVEL = "\$cs_officerLastLevel"
        val PHOTOSYNTHETIC = "cs_photosyntheticSkin"
        val DRIVEFIELDOVERHAULSKILL = "cs_driveFieldOverhaul"
        val TINKERSKILL = "cs_tinker"
        val VERDANTSKIN = "nsp_verdantSkin"
        val AUTOEXPERTISE = "cs_automationExpertise"

        val FOULMACHINERYSKILL = "cs_foulMachinery"
        val OLDGUARDSKILL = "cs_oldGuard"
        val PACKRATSKILL = "cs_packRat"
        val LOBSTERUPRISING = "cs_lobsterUprising"
        val OUTFLANKSKILL = "cs_outflank"
        val DECONSTRUCTION = "cs_exceptionalDeconstruction"


        // slot IDs

        val drakeBreathslotID = "BREATH"
        val drakeHeadSlotID = "NECK3"
        val coaxialSlotID = "COAXSLOT"

        // rendered sprites/images

        val WHITEOUT = "cs_whiteOut"
        val GATEGLOW = "cs_gateGlow"
        val WORMUS = "cs_wormus"
        val AURICGLOW = "cs_glow"
        val AURICRAY = "cs_auricRay"
        val COOLSKELETON = "coolskeleton_"

        // sound IDs
        val WOFHITSOUNDID = "cs_impact_wallFlesh"
        val GUARDIANDETACHSFX = "cs_guardian_detach"
        val AURICGLOWLOOP = "cs_auricGlow"
        val BADTOTHEBONE = "cs_badtothebone"


        // campaign entities

        val THEBANK_ID = "cs_mobileBank"

        // important global memkeys

        val HOUNDSLAUGHTTOKEN = "\$cs_houndslaughtToken"
        val HOUNDSLAUGHTPIECES = "\$cs_houndslaught"

        val BAGOFHOLDINGKEY = "\$cs_bagOfHoldingCargo" // saves cargo put into bag

        // faction IDs

        val PROTODRAKE_FACTIONID = "cs_protoDrakes"

        // entity or fleet IDs

        val BANKCACHE_ID = "cs_bankCache"
        val BANKREWARDCACHE_ID = "cs_bankRewardCache"
        val BEACON_ID = "cs_adventureBeaconStation"
        val SPIDERBEACON_ID = "cs_spiderBeacon"
        val HELIX_ID = "cs_shootingStar"
        val TESSY_ID = "cs_tessy"
        val LOCUS_ID = "cs_locus"
        val MK1FLEET_ID = "cs_mk1Fleet"
        val XIVDOM_ID = "cs_domxiv"
        val KATAMARI_ID = "cs_katamari"
        val SKELESTATION_ID = "cs_skeletronStation"
        val SPYBORG_ID = "cs_spyborg"
        val LUDDCOREFLEET_ID = "cs_absolver"
        val DOOMSCII_ID = "cs_doomscii"
        val HOUNDTOKEN_ID = "cs_houndslaughtToken"

        val NEGATIVEINTEREST_INDUSTRY = "cs_negativeInterest"

        // tags for civility sic stuff

        val CIVILITY_HULLMOD_TAG = "cs_combatPackage"
        val CIVILITY_PACKAGE_MOD = "cs_civPackageMod"


        val sierraTextColor = Color(205,155,255,255)
        val quoteText = Color(80,80,80,255)

        init {
            meatsoundlist.add("buffmeat1")
            meatsoundlist.add("buffmeat2")
            meatsoundlist.add("buffmeat3")
            meatsoundlist.add("buffmeat4")
            meatsoundlist.add("buffmeat5")
            meatsoundlist.add("buffmeat6")
            meatsoundlist.add("buffmeat7")
            meatsoundlist.add("buffmeat8")
        }


    }

}