package neon.nsp.data.scripts.rulecmd;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.CustomCampaignEntityAPI;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.impl.campaign.rulecmd.BaseCommandPlugin;
import com.fs.starfarer.api.util.Misc;
import org.lwjgl.util.vector.Vector2f;

import java.util.List;
import java.util.Map;

public class NSPDestroyExponent extends BaseCommandPlugin {
    @Override
    public boolean execute(String ruleId, InteractionDialogAPI dialog, List<Misc.Token> params, Map<String, MemoryAPI> memoryMap) {

        dialog.setInteractionTarget((CampaignFleetAPI)Global.getSector().getMemoryWithoutUpdate().get("$nsp_exponentLuddicFleet"));
        CustomCampaignEntityAPI exponentDerelictEntity = (CustomCampaignEntityAPI) Global.getSector().getMemoryWithoutUpdate().get("$nsp_ExponentDerelict");
        Global.getSoundPlayer().playSound("explosion_from_damage", 1, 1, Global.getSoundPlayer().getListenerPos(), new Vector2f());
        Misc.fadeAndExpire(exponentDerelictEntity, 1f);

        return true;
    }
}
