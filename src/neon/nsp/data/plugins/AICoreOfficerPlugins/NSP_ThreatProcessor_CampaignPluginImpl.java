package neon.nsp.data.plugins.AICoreOfficerPlugins;

import com.fs.starfarer.api.PluginPick;
import com.fs.starfarer.api.campaign.AICoreOfficerPlugin;
import com.fs.starfarer.api.campaign.BaseCampaignPlugin;
import com.fs.starfarer.api.campaign.CampaignPlugin;

//Threat AI core campaign plugin
public class NSP_ThreatProcessor_CampaignPluginImpl extends BaseCampaignPlugin {

    @Override
    public String getId() {
        return "NSP_ThreatProcessor_CampaignPluginImpl";
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
