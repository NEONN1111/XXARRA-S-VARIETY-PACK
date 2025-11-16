package data.plugins;

import com.fs.starfarer.api.PluginPick;
import com.fs.starfarer.api.campaign.AICoreOfficerPlugin;
import com.fs.starfarer.api.campaign.BaseCampaignPlugin;
import com.fs.starfarer.api.campaign.CampaignPlugin;

public class ExponentCampaignPluginImpl extends BaseCampaignPlugin {

    @Override
    public String getId() {
        return "NSP_CampaignPlugin";
    }

    @Override
    public PluginPick<AICoreOfficerPlugin> pickAICoreOfficerPlugin(String commodityId) {
        if ("nsp_exponent_core".equals(commodityId)) {
            return new PluginPick<AICoreOfficerPlugin>(new NSP_InvictaCore(), CampaignPlugin.PickPriority.MOD_SET);
        }
        return null;
    }

    @Override
    public boolean isTransient() {
        return true;
    }
}
