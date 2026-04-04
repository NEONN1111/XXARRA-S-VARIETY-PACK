package neon.nsp.data.plugins;

import com.fs.starfarer.api.PluginPick;
import com.fs.starfarer.api.campaign.AICoreOfficerPlugin;
import com.fs.starfarer.api.campaign.BaseCampaignPlugin;
import com.fs.starfarer.api.campaign.CampaignPlugin;

public class ThreatProcessorCampaignPluginImpl extends BaseCampaignPlugin {

    @Override
    public String getId() {
        return "NSP_ThreatProcessorCampaignPluginImpl";
    }

    @Override
    public PluginPick<AICoreOfficerPlugin> pickAICoreOfficerPlugin(String commodityId) {
        if ("nsp_threat_processor".equals(commodityId)) {
            return new PluginPick<AICoreOfficerPlugin>(new NSP_ThreatProcessor(), CampaignPlugin.PickPriority.MOD_SET);
        }
        return null;
    }

    @Override
    public boolean isTransient() {
        return true;
    }
}
