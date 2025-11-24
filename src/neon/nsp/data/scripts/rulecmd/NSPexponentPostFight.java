package neon.nsp.data.scripts.rulecmd;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CustomCampaignEntityAPI;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.OptionPanelAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.impl.campaign.DerelictShipEntityPlugin;
import com.fs.starfarer.api.impl.campaign.RuleBasedInteractionDialogPluginImpl;
import com.fs.starfarer.api.impl.campaign.ids.Entities;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import com.fs.starfarer.api.impl.campaign.procgen.themes.BaseThemeGenerator;
import com.fs.starfarer.api.impl.campaign.rulecmd.BaseCommandPlugin;
import com.fs.starfarer.api.impl.campaign.rulecmd.salvage.special.ShipRecoverySpecial;
import com.fs.starfarer.api.util.Misc;

import java.util.List;
import java.util.Map;

public class NSPexponentPostFight extends BaseCommandPlugin {
    @Override
    public boolean execute(String ruleId, InteractionDialogAPI dialog, List<Misc.Token> params, Map<String, MemoryAPI> memoryMap) {

        OptionPanelAPI options = dialog.getOptionPanel();
        options.clearOptions();
        options.addOption("Continue NSP","exponentBattleContinue");

        Global.getSector().getMemoryWithoutUpdate().set("$defeatedExponent", true);


        ShipRecoverySpecial.PerShipData ship = new ShipRecoverySpecial.PerShipData("nsp_exponent_Hull", ShipRecoverySpecial.ShipCondition.WRECKED, 0f);
        ship.shipName = "Unknown";
        DerelictShipEntityPlugin.DerelictShipData params1 = new DerelictShipEntityPlugin.DerelictShipData(ship, false);
        CustomCampaignEntityAPI entity = (CustomCampaignEntityAPI) BaseThemeGenerator.addSalvageEntity(
                Global.getSector().getPlayerFleet().getContainingLocation(),
                Entities.WRECK, Factions.NEUTRAL, params1);
        Misc.makeImportant(entity, "exponent");
        entity.getMemoryWithoutUpdate().set("$exponent", true);
        entity.getLocation().x = Global.getSector().getPlayerFleet().getLocation().x + (50f - (float) Math.random() * 100f);
        entity.getLocation().y = Global.getSector().getPlayerFleet().getLocation().y + (50f - (float) Math.random() * 100f);

        ShipRecoverySpecial.ShipRecoverySpecialData data = new ShipRecoverySpecial.ShipRecoverySpecialData(null);
        data.notNowOptionExits = true;
        data.noDescriptionText = true;
        DerelictShipEntityPlugin dsep = (DerelictShipEntityPlugin) entity.getCustomPlugin();
        ShipRecoverySpecial.PerShipData copy = (ShipRecoverySpecial.PerShipData) dsep.getData().ship.clone();
        copy.variant = Global.getSettings().getVariant(copy.variantId).clone();
        copy.variantId = null;
        copy.variant.addTag(Tags.SHIP_CAN_NOT_SCUTTLE);
        data.addShip(copy);
        Misc.setSalvageSpecial(entity, data);

        dialog.setInteractionTarget(entity);
        RuleBasedInteractionDialogPluginImpl plugin = new RuleBasedInteractionDialogPluginImpl("ExponentPostFightTwo");
        dialog.setPlugin(plugin);
        plugin.init(dialog);

        options = dialog.getOptionPanel();
        options.clearOptions();
        options.addOption("Continue NSP","exponentBattleContinue");

        return true;
    }
}
