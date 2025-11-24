package neon.nsp.data.scripts.rulecmd;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.rulecmd.BaseCommandPlugin;
import com.fs.starfarer.api.util.Misc;

import java.util.List;
import java.util.Map;

public class ExponentCMD extends BaseCommandPlugin {
    protected CampaignFleetAPI playerFleet;
    protected SectorEntityToken entity;
    protected TextPanelAPI text;
    protected OptionPanelAPI options;
    protected MemoryAPI memory;
    protected InteractionDialogAPI dialog;
    protected Map<String, MemoryAPI> memoryMap;


    public ExponentCMD() {
    }

    public ExponentCMD(SectorEntityToken entity) {
        init(entity);
    }

    protected void init(SectorEntityToken entity) {
        memory = entity.getMemoryWithoutUpdate();
        this.entity = entity;
        playerFleet = Global.getSector().getPlayerFleet();


    }

    @Override
    public boolean execute(String ruleId, InteractionDialogAPI dialog, List<Misc.Token> params, Map<String, MemoryAPI> memoryMap) {
        this.dialog = dialog;
        this.memoryMap = memoryMap;

        String command = params.get(0).getString(memoryMap);
        if (command == null) return false;

        entity = dialog.getInteractionTarget();
        init(entity);

        memory = getEntityMemory(memoryMap);

        text = dialog.getTextPanel();
        options = dialog.getOptionPanel();

        if (command.equals("updateData")) {
            updateData();
        }

        return true;
    }
    protected void updateData() {
        boolean hasExponent = false;
        boolean hasNonExponent = false;
        for (FleetMemberAPI member : playerFleet.getFleetData().getMembersListCopy()) {
            if (member.getHullSpec().getBaseHullId().equals("nsp_exponent")) {
                memory.set("$exponentShipName", member.getShipName(), 0f);
                memory.set("$exponentMember", member, 0f);
                hasExponent = true;
            } else {
                hasNonExponent = true;
            }
        }
        memory.set("$hasExponent", hasExponent, 0f);
        memory.set("$hasNonExponent", hasNonExponent, 0f);
        memory.set("$hasOnlyExponent", hasExponent && !hasNonExponent, 0f);
    }
}
