package neon.nsp.data.scripts.rulecmd;

import java.util.List;
import java.util.Map;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.OptionPanelAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.TextPanelAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.rulecmd.BaseCommandPlugin;
import com.fs.starfarer.api.util.Misc.Token;

/**
 * Likely deprecated?... Rules really full on old stuff which realistically should be deleted
 */
public class TemplarCMD extends BaseCommandPlugin {

	protected CampaignFleetAPI playerFleet;
	protected SectorEntityToken entity;
	protected TextPanelAPI text;
	protected OptionPanelAPI options;
	protected MemoryAPI memory;
	protected InteractionDialogAPI dialog;
	protected Map<String, MemoryAPI> memoryMap;

	
	public TemplarCMD() {
	}
	
	public TemplarCMD(SectorEntityToken entity) {
		init(entity);
	}
	
	protected void init(SectorEntityToken entity) {
		memory = entity.getMemoryWithoutUpdate();
		this.entity = entity;
		playerFleet = Global.getSector().getPlayerFleet();
		
		
	}

	public boolean execute(String ruleId, InteractionDialogAPI dialog, List<Token> params, Map<String, MemoryAPI> memoryMap) {
		this.dialog = dialog;
		this.memoryMap = memoryMap;
		
		String command = params.get(0).getString(memoryMap);
		if (command == null) return false;
		
		entity = dialog.getInteractionTarget();
		init(entity);
		
		memory = getEntityMemory(memoryMap);
		
		text = dialog.getTextPanel();
		options = dialog.getOptionPanel();
		
		if (command.equals("initEncounters")) {
		} else if (command.equals("updateData")) {
			updateData();
		}
		
		return true;
	}
	
	protected void updateData() {
		boolean hasTemplar = false;
		boolean hasNonTemplar = false;
		for (FleetMemberAPI member : playerFleet.getFleetData().getMembersListCopy()) {
			if (member.getHullSpec().getBaseHullId().equals("nsp_templar")) {
				memory.set("$templarShipName", member.getShipName(), 0f);
				memory.set("$templarMember", member, 0f);
				hasTemplar = true;
			} else {
				hasNonTemplar = true;
			}
		}
		memory.set("$hasTemplar", hasTemplar, 0f);
		memory.set("$hasNonTemplar", hasNonTemplar, 0f);
		memory.set("$hasOnlyTemplar", hasTemplar && !hasNonTemplar, 0f);
	}
	
}




















