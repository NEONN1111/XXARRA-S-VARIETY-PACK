package neon.nsp.data.scripts.rulecmd;

import java.util.List;
import java.util.Map;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.TextPanelAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.fleet.FleetMemberType;
import com.fs.starfarer.api.impl.campaign.rulecmd.AddRemoveCommodity;
import com.fs.starfarer.api.impl.campaign.rulecmd.BaseCommandPlugin;
import com.fs.starfarer.api.loading.VariantSource;
import com.fs.starfarer.api.util.Misc;


/**
 *	DE_AddShip <fleet member variant ID>
 */
// A variation of AddShip that should work for the gamaliel interaction
// cba to write a more general one for now
// definitely not stolen from PAGSM

//from Diktat Enhancement/arandomrobloxjal

public class NSP_AddShip extends BaseCommandPlugin {
	public boolean execute(String ruleId, InteractionDialogAPI dialog, List<Misc.Token> params, Map<String, MemoryAPI> memoryMap) {
		if (dialog == null) return false;
		/*FleetMemberAPI member = (FleetMemberAPI) Global.getSettings().getHullSpec("de_gamaliel_proto");
		Global.getSector().getPlayerFleet().getFleetData().addFleetMember(member);
		addShipGainText(member, dialog.getTextPanel());
		return true;*/
		String variant1 = ((Misc.Token)params.get(0)).getString(memoryMap);
	//	String name2 = "Testing Rig PLH-01";
		FleetMemberAPI ship = Global.getFactory().createFleetMember(FleetMemberType.SHIP, variant1);
	//	ship.setShipName(name2);
		ShipVariantAPI variant = ship.getVariant();
		variant.setSource(VariantSource.REFIT);
		if (params.size() > 1 && ((Misc.Token)params.get(1)).getBoolean(memoryMap)) {
			variant.addTag("variant_always_retain_smods_on_salvage");
		}

		Global.getSector().getPlayerFleet().getFleetData().addFleetMember(ship);
		AddRemoveCommodity.addFleetMemberGainText(variant, dialog.getTextPanel());
		return true;
	}

	public static void addShipGainText(FleetMemberAPI member, TextPanelAPI text) {
		text.setFontSmallInsignia();
		text.addParagraph("Gained " + member.getVariant().getFullDesignationWithHullNameForShip(), Misc.getPositiveHighlightColor());
		text.highlightInLastPara(Misc.getHighlightColor(), member.getVariant().getFullDesignationWithHullNameForShip());
		text.setFontInsignia();
	}
}
