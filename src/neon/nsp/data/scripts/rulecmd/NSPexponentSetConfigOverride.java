package neon.nsp.data.scripts.rulecmd;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.combat.BattleCreationContext;
import com.fs.starfarer.api.impl.campaign.DerelictShipEntityPlugin;
import com.fs.starfarer.api.impl.campaign.FleetEncounterContext;
import com.fs.starfarer.api.impl.campaign.FleetInteractionDialogPluginImpl;
import com.fs.starfarer.api.impl.campaign.RuleBasedInteractionDialogPluginImpl;
import com.fs.starfarer.api.impl.campaign.ids.Entities;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import com.fs.starfarer.api.impl.campaign.ids.MemFlags;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import com.fs.starfarer.api.impl.campaign.procgen.themes.BaseThemeGenerator;
import com.fs.starfarer.api.impl.campaign.procgen.themes.RemnantSeededFleetManager;
import com.fs.starfarer.api.impl.campaign.rulecmd.BaseCommandPlugin;
import com.fs.starfarer.api.impl.campaign.rulecmd.salvage.special.ShipRecoverySpecial;
import com.fs.starfarer.api.util.Misc;

import java.util.List;
import java.util.Map;

public class NSPexponentSetConfigOverride extends BaseCommandPlugin {
    @Override
    public boolean execute(String ruleId, InteractionDialogAPI dialog, List<Misc.Token> params, Map<String, MemoryAPI> memoryMap) {

        MemoryAPI memory = dialog.getInteractionTarget().getMemoryWithoutUpdate();
        memory.set(MemFlags.FLEET_INTERACTION_DIALOG_CONFIG_OVERRIDE_GEN,createConfig());
//        Console.showMessage(Global.getSector().getCampaignUI().getCurrentInteractionDialog().getInteractionTarget() instanceof CampaignFleetAPI)
        return true;
    }

    public FleetInteractionDialogPluginImpl.FIDConfig createConfig() {
        FleetInteractionDialogPluginImpl.FIDConfig config = new FleetInteractionDialogPluginImpl.FIDConfig();

//			config.alwaysAttackVsAttack = true;
//			config.leaveAlwaysAvailable = true;
//			config.showFleetAttitude = false;
        config.showTransponderStatus = false;
        config.showEngageText = false;
        config.alwaysPursue = true;
        config.dismissOnLeave = false;
        //config.lootCredits = false;
        config.withSalvage = false;
        //config.showVictoryText = false;
        config.printXPToDialog = true;

        config.noSalvageLeaveOptionText = "Continue NSPconfig";
//			config.postLootLeaveOptionText = "Continue";
//			config.postLootLeaveHasShortcut = false;

        config.delegate = new FleetInteractionDialogPluginImpl.BaseFIDDelegate() {
            public void postPlayerSalvageGeneration(InteractionDialogAPI dialog, FleetEncounterContext context, CargoAPI salvage) {
                new RemnantSeededFleetManager.RemnantFleetInteractionConfigGen().createConfig().delegate.
                        postPlayerSalvageGeneration(dialog, context, salvage);
            }
            public void notifyLeave(InteractionDialogAPI dialog) {

                SectorEntityToken other = dialog.getInteractionTarget();
                if (!(other instanceof CampaignFleetAPI)) {
                    dialog.dismiss();
                    return;
                }
                CampaignFleetAPI fleet = (CampaignFleetAPI) other;

                if (!fleet.isEmpty()) {
                    dialog.dismiss();
                    return;
                }

                Global.getSector().getMemoryWithoutUpdate().set("$defeatedExponent", true);

                ShipRecoverySpecial.PerShipData ship = new ShipRecoverySpecial.PerShipData("nsp_exponent_Hull", ShipRecoverySpecial.ShipCondition.WRECKED, 0f);
                ship.shipName = "Unknown";
                DerelictShipEntityPlugin.DerelictShipData params = new DerelictShipEntityPlugin.DerelictShipData(ship, false);
                CustomCampaignEntityAPI entity = (CustomCampaignEntityAPI) BaseThemeGenerator.addSalvageEntity(
                        fleet.getContainingLocation(),
                        Entities.WRECK, Factions.NEUTRAL, params);
                Misc.makeImportant(entity, "exponent");
                entity.getMemoryWithoutUpdate().set("$exponent", true);

                entity.getLocation().x = fleet.getLocation().x + (50f - (float) Math.random() * 100f);
                entity.getLocation().y = fleet.getLocation().y + (50f - (float) Math.random() * 100f);

                ShipRecoverySpecial.ShipRecoverySpecialData data = new ShipRecoverySpecial.ShipRecoverySpecialData(null);
                data.notNowOptionExits = true;
                data.noDescriptionText = true;
                DerelictShipEntityPlugin dsep = (DerelictShipEntityPlugin) entity.getCustomPlugin();
                ShipRecoverySpecial.PerShipData copy = (ShipRecoverySpecial.PerShipData) dsep.getData().ship.clone();
                copy.variant = Global.getSettings().getVariant(copy.variantId).clone();
                copy.variantId = null;
                copy.variant.addTag(Tags.SHIP_CAN_NOT_SCUTTLE);
//                copy.variant.addTag(Tags.SHIP_UNIQUE_SIGNATURE);
                data.addShip(copy);

                Misc.setSalvageSpecial(entity, data);

                dialog.setInteractionTarget(entity);
                RuleBasedInteractionDialogPluginImpl plugin = new RuleBasedInteractionDialogPluginImpl("AfterNSPExponentDefeat");
                dialog.setPlugin(plugin);
                plugin.init(dialog);
            }

            public void battleContextCreated(InteractionDialogAPI dialog, BattleCreationContext bcc) {
                bcc.aiRetreatAllowed = false;
                bcc.objectivesAllowed = false;
                bcc.fightToTheLast = true;
                bcc.enemyDeployAll = true;
            }
        };
        return config;
    }

}
