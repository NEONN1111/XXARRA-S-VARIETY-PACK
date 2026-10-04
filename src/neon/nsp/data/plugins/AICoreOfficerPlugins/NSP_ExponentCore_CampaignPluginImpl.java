package neon.nsp.data.plugins.AICoreOfficerPlugins;

import com.fs.starfarer.api.PluginPick;
import com.fs.starfarer.api.campaign.AICoreOfficerPlugin;
import com.fs.starfarer.api.campaign.BaseCampaignPlugin;
import com.fs.starfarer.api.campaign.CampaignPlugin;

public class NSP_ExponentCore_CampaignPluginImpl extends BaseCampaignPlugin {

    @Override
    public String getId() {
        return "NSP_ExponentCore_CampaignPluginImpl";
    }

    @Override
    public PluginPick<AICoreOfficerPlugin> pickAICoreOfficerPlugin(String commodityId) {
        if ("nsp_exponent_core".equals(commodityId)) {
            return new PluginPick<AICoreOfficerPlugin>(new NSP_ExponentCore(), CampaignPlugin.PickPriority.MOD_SET);
        }
        return null;
    }

    @Override
    public boolean isTransient() {
        return true;
    }
}
