package neon.nsp.data.campaign.intel.misc;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.campaign.econ.Industry;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.econ.MonthlyReport;
import com.fs.starfarer.api.campaign.listeners.ColonyPlayerHostileActListener;
import com.fs.starfarer.api.campaign.listeners.EconomyTickListener;
import com.fs.starfarer.api.campaign.listeners.FleetEventListener;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.ids.MemFlags;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import com.fs.starfarer.api.impl.campaign.intel.BaseIntelPlugin;
import com.fs.starfarer.api.impl.campaign.rulecmd.salvage.MarketCMD;
import com.fs.starfarer.api.impl.campaign.shared.SharedData;
import com.fs.starfarer.api.ui.ButtonAPI;
import com.fs.starfarer.api.ui.IntelUIAPI;
import com.fs.starfarer.api.ui.SectorMapAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.api.util.WeightedRandomPicker;
import neon.nsp.data.scripts.campaign.ids.NSP_IDs;
import neon.nsp.data.scripts.campaign.ids.NSP_People;
import neon.nsp.data.scripts.util.NSP_Misc;
import org.lwjgl.input.Keyboard;

import java.awt.*;
import java.util.Set;

import static com.fs.starfarer.api.impl.campaign.ids.Factions.DERELICT;


/**
 *	Invicta's contact intel, also tracks what she wants to talk about
 */

public class NSPInvictaConvIntel extends BaseIntelPlugin implements FleetEventListener, EconomyTickListener {

	private float randThoughtCounter = 0f;
	private final float randThoughtTime = 30f;

	// so we can hotswap the Invicta officer core for the special item in the cargo screen
	public boolean runWhilePaused() {
		return true;
	}

	public NSPInvictaConvIntel() {
		Global.getSector().getPlayerFleet().addEventListener(this);
		Global.getSector().getListenerManager().addListener(this);
		if (!Global.getSector().getScripts().contains(this)) {
			Global.getSector().addScript(this);
		}

		// Auto-add to intel manager
		Global.getSector().getIntelManager().addIntel(this, false);
	}

	protected void advanceImpl(float amount) {



		if (!Global.getSector().getPlayerMemoryWithoutUpdate().contains("$nsp_metInvicta")) {
			Global.getSector().getPlayerMemoryWithoutUpdate().set("$nsp_metInvicta", true);
		}

		if (Global.getSector().getMemoryWithoutUpdate().get(NSP_IDs.MEM_DAYS_WITH_INVICTA) == null) {
			Global.getSector().getMemoryWithoutUpdate().set(NSP_IDs.MEM_DAYS_WITH_INVICTA, 0f);
		} else {
			float timeHadInvicta = Global.getSector().getMemoryWithoutUpdate().getFloat(NSP_IDs.MEM_DAYS_WITH_INVICTA);
			Global.getSector().getMemoryWithoutUpdate().set(NSP_IDs.MEM_DAYS_WITH_INVICTA, timeHadInvicta + Global.getSector().getClock().convertToDays(amount));
		}

		if (!Global.getSettings().getBoolean("nsp_InvictaHasRandomThoughts")) return;
		int numInvictaThoughts = Global.getSector().getMemoryWithoutUpdate().getInt(NSP_IDs.MEM_NUM_INVICTA_THOUGHTS);
		if (numInvictaThoughts > 1) {
			return;
		}
		float toIncrement = Global.getSector().getClock().convertToDays(amount);
		if (numInvictaThoughts > 0) {
			toIncrement *= 0.5f; // half as fast if she already has something random to say
		}
		randThoughtCounter += toIncrement;
		if (randThoughtCounter > randThoughtTime) {
			String topic = getInvictaTopic();
			if (topic != null) {
				Global.getSector().getMemoryWithoutUpdate().set(topic, true);
				Global.getSector().getMemoryWithoutUpdate().set(NSP_IDs.MEM_NUM_INVICTA_THOUGHTS, numInvictaThoughts + 1);
			}
			randThoughtCounter = 0f;
		}
	}


	public static String getInvictaTopic() {
		float timeHadInvicta = Global.getSector().getMemoryWithoutUpdate().getFloat(NSP_IDs.MEM_DAYS_WITH_INVICTA);
		WeightedRandomPicker<String> picker = new WeightedRandomPicker<String>();
		picker.add("$InvictaRandShepherds");
		picker.add("$InvictaRandRain");
		picker.add("$InvictaRandReaper");
		if (timeHadInvicta > 60) {
			picker.add("$InvictaRand2ndAIWar");
		}
		for (String topic : picker.clone().getItems()) {
			if (Global.getSector().getMemoryWithoutUpdate().contains(topic)) {
				picker.remove(topic);
			}
		}
		if (picker.isEmpty()) {
			return null;
		}
		return picker.pick();
	}

	// tracks the player's accomplishments
	public void reportBattleOccurred(CampaignFleetAPI fleet, CampaignFleetAPI primaryWinner, BattleAPI battle) {
		if (isDone()) return;

		if (!battle.isPlayerInvolved()) {
			return;
		}

		NSP_Misc.setInvictaHasThoughts();

		MemoryAPI sector_mem = Global.getSector().getMemoryWithoutUpdate();

		int biggest = 0;
		for (CampaignFleetAPI otherFleet : battle.getNonPlayerSideSnapshot()) {
			if (otherFleet.getFleetPoints() > biggest) {
				biggest = otherFleet.getFleetPoints();
				if (!otherFleet.getMemoryWithoutUpdate().contains(MemFlags.MEMORY_KEY_NO_REP_IMPACT) && Global.getSector().getMemoryWithoutUpdate().getBoolean("$nsp_showInvictaIntel")) {
					sector_mem.set("$invictaFactionFought", otherFleet.getFaction().getId(), 60);
				}
			}
			MemoryAPI fleet_mem = otherFleet.getMemoryWithoutUpdate();
			if (fleet_mem.contains("$ziggurat") && Global.getSector().getMemoryWithoutUpdate().getBoolean("$nsp_showInvictaIntel")) {
				sector_mem.set("$InvictaWitnessedZigFight", true);
			}
			for (FleetMemberAPI member : Misc.getSnapshotMembersLost(otherFleet)) {
				if (member.getHullId().equals("tesseract") && Global.getSector().getMemoryWithoutUpdate().getBoolean("$nsp_showInvictaIntel")) {
					sector_mem.set("$InvictaWitnessedOmega", true);
				}
				if (member.getHullId().equals("guardian") && Global.getSector().getMemoryWithoutUpdate().getBoolean("$nsp_showInvictaIntel")) {
					sector_mem.set("$InvictaWitnessedGuardianKill", true);
				}
				if (member.getHullId().equals("remnant_station1") && Global.getSector().getMemoryWithoutUpdate().getBoolean("$nsp_showInvictaIntel")) {
					sector_mem.set("$InvictaWitnessedRemStation1Kill", true);
				}
				if (member.getHullId().equals("remnant_station2") && Global.getSector().getMemoryWithoutUpdate().getBoolean("$nsp_showInvictaIntel")) {
					sector_mem.set("$InvictaWitnessedRemStation2Kill", true);
				}

			}
		}
	}

	public void reportFleetDespawnedToListener(CampaignFleetAPI fleet, CampaignEventListener.FleetDespawnReason reason, Object param) {

	}

	public void reportRaidForValuablesFinishedBeforeCargoShown(InteractionDialogAPI dialog, MarketAPI market, MarketCMD.TempData actionData, CargoAPI cargo) {
		//
	}
	public void reportRaidToDisruptFinished(InteractionDialogAPI dialog, MarketAPI market, MarketCMD.TempData actionData, Industry industry) {
		//
	}

	public void reportTacticalBombardmentFinished(InteractionDialogAPI dialog, MarketAPI market, MarketCMD.TempData actionData) {
		//
	}
	public void reportEconomyMonthEnd() {

	}

	// No more maintenance cost for Invicta
	public void reportEconomyTick(int iterIndex) {
		// Empty - Invicta doesn't charge maintenance fees
	}

	protected void addBulletPoints(TooltipMakerAPI info, ListInfoMode mode) {
		Color h = Misc.getHighlightColor();
		Color g = Misc.getGrayColor();
		float pad = 3f;
		float opad = 10f;

		float initPad = pad;
		if (mode == ListInfoMode.IN_DESC) initPad = opad;

		Color tc = getBulletColorForMode(mode);

		bullet(info);
		boolean isUpdate = getListInfoParam() != null;

		unindent(info);
	}


	@Override
	public void createIntelInfo(TooltipMakerAPI info, ListInfoMode mode) {
		Color c = getTitleColor(mode);
		info.addPara(getName(), c, 0f);
		addBulletPoints(info, mode);
	}


	@Override
	public void createSmallDescription(TooltipMakerAPI info, float width, float height) {
		Color h = Misc.getHighlightColor();
		Color g = Misc.getGrayColor();
		Color tc = Misc.getTextColor();
		PersonAPI invicta = NSP_People.getPerson(NSP_People.INVICTA);
		FactionAPI invicta_faction = Global.getSector().getFaction(DERELICT);
		float pad = 3f;
		float opad = 10f;

		addBulletPoints(info, ListInfoMode.IN_DESC);

		info.addImage(invicta.getPortraitSprite(), width, 128, opad);
		info.addPara("Invicta is currently available to speak to.", opad);
		ButtonAPI button = info.addButton("Request a comm-link", "nsp_InvictaConvButton",
				invicta_faction.getBaseUIColor(), invicta_faction.getDarkUIColor(),
				(int)(width), 20f, opad * 2f);
		button.setShortcut(Keyboard.KEY_T, true);
	}


	@Override
	public String getIcon() {
		return Global.getSettings().getSpriteName("characters", "Invicta");
	}


	@Override
	public Set<String> getIntelTags(SectorMapAPI map) {
		Set<String> tags = super.getIntelTags(map);
		tags.add(Tags.INTEL_CONTACTS);
		return tags;
	}


	public String getSortString() {
		return "Invicta";
	}


	public String getName() {
		return "Contact: Invicta";
	}


	@Override
	public FactionAPI getFactionForUIColors() {
		return Global.getSector().getFaction(DERELICT);
	}

	public String getSmallDescriptionTitle() {
		return getName();
	}


	@Override
	public boolean shouldRemoveIntel() {
		// Remove when the memory key is false or not set
		return !Global.getSector().getMemoryWithoutUpdate().getBoolean("$nsp_showInvictaIntel");
	}

	// don't show unless the memory key is set
	@Override
	public boolean isHidden() {
		return !Global.getSector().getMemoryWithoutUpdate().getBoolean("$nsp_showInvictaIntel");
	}

	@Override
	public String getCommMessageSound() {
		return getSoundMajorPosting();
	}

	public void buttonPressConfirmed(Object buttonId, IntelUIAPI ui) {
		if (buttonId == "nsp_InvictaConvButton") {
			ui.showDialog(null, "InvictaConvOpen");
		}
	}
}