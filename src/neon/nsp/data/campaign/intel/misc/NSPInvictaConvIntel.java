package neon.nsp.data.campaign.intel.misc;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.campaign.comm.IntelInfoPlugin; // ADD THIS IMPORT
import com.fs.starfarer.api.campaign.econ.Industry;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.econ.MonthlyReport;
import com.fs.starfarer.api.campaign.listeners.ColonyPlayerHostileActListener;
import com.fs.starfarer.api.campaign.listeners.EconomyTickListener;
import com.fs.starfarer.api.campaign.listeners.FleetEventListener;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import com.fs.starfarer.api.impl.campaign.ids.MemFlags;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import com.fs.starfarer.api.impl.campaign.intel.BaseIntelPlugin;
import com.fs.starfarer.api.impl.campaign.intel.contacts.ContactIntel;
import com.fs.starfarer.api.impl.campaign.rulecmd.salvage.MarketCMD;
import com.fs.starfarer.api.impl.campaign.shared.SharedData;
import com.fs.starfarer.api.ui.ButtonAPI;
import com.fs.starfarer.api.ui.IntelUIAPI;
import com.fs.starfarer.api.ui.SectorMapAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.api.util.WeightedRandomPicker;
import neon.nsp.data.plugins.secgen.NSPInvictaSystemGen;
import neon.nsp.data.scripts.campaign.ids.NSP_IDs;
import neon.nsp.data.scripts.campaign.ids.NSP_People;
import neon.nsp.data.scripts.util.NSP_Misc;
import org.lwjgl.input.Keyboard;

import java.awt.*;
import java.util.List; // ADD THIS IMPORT
import java.util.Set;

import static com.fs.starfarer.api.impl.campaign.ids.Factions.DERELICT;


/**
 *	Invicta's contact intel, also tracks what she wants to talk about
 */
public class NSPInvictaConvIntel extends BaseIntelPlugin {

	private float randThoughtCounter = 0f;
	private final float randThoughtTime = 30f;
	private PersonAPI invictaPerson;

	// so we can hotswap the Invicta officer core for the special item in the cargo screen
	public boolean runWhilePaused() {
		return true;
	}

	public NSPInvictaConvIntel() {
		Global.getSector().getListenerManager().addListener(this);
		if (!Global.getSector().getScripts().contains(this)) {
			Global.getSector().addScript(this);
		}

		// Find or create Invicta person
		invictaPerson = findOrCreateInvictaPerson();

		// Auto-add to intel manager
		Global.getSector().getIntelManager().addIntel(this, false);

		// Also add Invicta as a contact (market can be null for AI cores)
		//addInvictaAsContact();
	}

	private PersonAPI findOrCreateInvictaPerson() {
		PersonAPI invicta = null;

		// Try to get Invicta from NSP_People first
		try {
			invicta = NSP_People.getPerson(NSP_People.INVICTA);
		} catch (Exception e) {
			// Fall through to next method
		}

		// If not found, try to get from ImportantPeople API
		if (invicta == null) {
			try {
				com.fs.starfarer.api.characters.ImportantPeopleAPI.PersonDataAPI data =
						Global.getSector().getImportantPeople().getData("nsp_invicta_core");
				if (data != null) {
					invicta = data.getPerson();
				}
			} catch (Exception e) {
				// Could not find Invicta person
			}
		}

		// If still not found, create a new person
		if (invicta == null) {
			invicta = Global.getFactory().createPerson();
			invicta.setId("nsp_invicta_core");
			invicta.setName(new com.fs.starfarer.api.characters.FullName("Invicta", "", com.fs.starfarer.api.characters.FullName.Gender.ANY));
			invicta.setPortraitSprite(Global.getSettings().getSpriteName("characters", "Invicta"));
			invicta.setPostId("nsp_invicta_core");

			// Add to ImportantPeople
			Global.getSector().getImportantPeople().addPerson(invicta);
		}

		return invicta;
	}

	private void addInvictaAsContact() {
		if (invictaPerson != null) {
			// Check if contact already exists
			boolean contactExists = false;
			List<IntelInfoPlugin> intelList = Global.getSector().getIntelManager().getIntel(ContactIntel.class);
			for (IntelInfoPlugin intel : intelList) {
				if (intel instanceof ContactIntel) {
					ContactIntel contact = (ContactIntel) intel;
					if (contact.getPerson() == invictaPerson ||
							(contact.getPerson() != null && contact.getPerson().getId().equals(invictaPerson.getId()))) {
						contactExists = true;
						break;
					}
				}
			}

			if (!contactExists) {
				//create fake market for invicta contact
				MarketAPI sillymarket = Global.getFactory().createMarket("test_market", "You shouldn't see this, Meow!", 2);
				sillymarket.setAdmin(invictaPerson);
				sillymarket.setFactionId(DERELICT);
				//sillymarket.setHidden(true);
				//sillymarket.setPrimaryEntity(Global.getSector().getStarSystem("Mourn").getEntityById(NSPInvictaSystemGen.INVICTA_PLANET_ID));


				// Create contact with null market (valid for AI cores)
				ContactIntel contactIntel = new ContactIntel(invictaPerson, sillymarket);

				Global.getSector().getIntelManager().addIntel(contactIntel, false);
				contactIntel.develop(null);
				contactIntel.setState(ContactIntel.ContactState.PRIORITY);

				// Play sound when contact is added
				Global.getSoundPlayer().playUISound("ui_contact_developed", 1f, 1f);

				// Send notification to player
				contactIntel.sendUpdateIfPlayerHasIntel(null, false);
			}
		}
	}

	// Helper method to get Invicta person
	public PersonAPI getInvictaPerson() {
		return invictaPerson;
	}

	//protected void advanceImpl(float amount) {
	//	// [Keep existing advanceImpl code unchanged]
	//	if (Global.getSector().getMemoryWithoutUpdate().get(NSP_IDs.MEM_DAYS_WITH_INVICTA) == null) {
	//		Global.getSector().getMemoryWithoutUpdate().set(NSP_IDs.MEM_DAYS_WITH_INVICTA, 0f);
	//	} else {
	//		float timeHadInvicta = Global.getSector().getMemoryWithoutUpdate().getFloat(NSP_IDs.MEM_DAYS_WITH_INVICTA);
	//		Global.getSector().getMemoryWithoutUpdate().set(NSP_IDs.MEM_DAYS_WITH_INVICTA, timeHadInvicta + Global.getSector().getClock().convertToDays(amount));
	//	}
	//
	//	if (!Global.getSettings().getBoolean("nsp_InvictaHasRandomThoughts")) return;
	//	int numInvictaThoughts = Global.getSector().getMemoryWithoutUpdate().getInt(NSP_IDs.MEM_NUM_INVICTA_THOUGHTS);
	//	if (numInvictaThoughts > 1) {
	//		return;
	//	}
	//	float toIncrement = Global.getSector().getClock().convertToDays(amount);
	//	if (numInvictaThoughts > 0) {
	//		toIncrement *= 0.5f; // half as fast if she already has something random to say
	//	}
	//	randThoughtCounter += toIncrement;
	//	if (randThoughtCounter > randThoughtTime) {
	//		String topic = getInvictaTopic();
	//		if (topic != null) {
	//			Global.getSector().getMemoryWithoutUpdate().set(topic, true);
	//			Global.getSector().getMemoryWithoutUpdate().set(NSP_IDs.MEM_NUM_INVICTA_THOUGHTS, numInvictaThoughts + 1);
	//		}
	//		randThoughtCounter = 0f;
	//	}
	//}


//public static String getInvictaTopic() {
//	float timeHadInvicta = Global.getSector().getMemoryWithoutUpdate().getFloat(NSP_IDs.MEM_DAYS_WITH_INVICTA);
//	WeightedRandomPicker<String> picker = new WeightedRandomPicker<String>();
//	picker.add("$InvictaRandShepherds");
//	picker.add("$InvictaRandRain");
//	picker.add("$InvictaRandReaper");
//	if (timeHadInvicta > 60) {
//		picker.add("$InvictaRand2ndAIWar");
//	}
//	for (String topic : picker.clone().getItems()) {
//		if (Global.getSector().getMemoryWithoutUpdate().contains(topic)) {
//			picker.remove(topic);
//		}
//	}
//	if (picker.isEmpty()) {
//		return null;
//	}
//	return picker.pick();
//}

	// [Keep other report methods unchanged]
//	public void reportFleetDespawnedToListener(CampaignFleetAPI fleet, CampaignEventListener.FleetDespawnReason reason, Object param) {
//	}
//
//	public void reportRaidForValuablesFinishedBeforeCargoShown(InteractionDialogAPI dialog, MarketAPI market, MarketCMD.TempData actionData, CargoAPI cargo) {
//	}
//
//	public void reportRaidToDisruptFinished(InteractionDialogAPI dialog, MarketAPI market, MarketCMD.TempData actionData, Industry industry) {
//	}
//
//	public void reportTacticalBombardmentFinished(InteractionDialogAPI dialog, MarketAPI market, MarketCMD.TempData actionData) {
//	}
//
//	public void reportEconomyMonthEnd() {
//	}

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

		// Use the stored invictaPerson instead of trying to fetch it
		PersonAPI invicta = invictaPerson;
		if (invicta == null) {
			invicta = findOrCreateInvictaPerson();
		}

		//FactionAPI invicta_faction = Global.getSector().getFaction(DERELICT);
		float pad = 3f;
		float opad = 10f;

		addBulletPoints(info, ListInfoMode.IN_DESC);

		info.addImage(invicta.getPortraitSprite(), width, 128, opad);
		info.addPara("Invicta is currently available to speak to.", opad);
		ButtonAPI button = info.addButton("Request a comm-link", "nsp_InvictaConvButton",
			//	invicta_faction.getBaseUIColor(), invicta_faction.getDarkUIColor(),
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
		//tags.add(Tags.INTEL_CONTACTS);
		return tags;
	}


	public String getSortString() {
		return "Invicta";
	}


	public String getName() {
		return "Contact: Invicta";
	}


	//@Override
	//public FactionAPI getFactionForUIColors() {
	//	return Global.getSector().getFaction(DERELICT);
	//}

	public String getSmallDescriptionTitle() {
		return getName();
	}


	@Override
	public String getCommMessageSound() {
		return getSoundMajorPosting();
	}

	public void buttonPressConfirmed(Object buttonId, IntelUIAPI ui) {
		if (buttonId == "nsp_InvictaConvButton") {
			ui.showDialog(invictaPerson.getMarket().getPrimaryEntity(), "InvictaConvOpen");
		}
	}
}