package neon.nsp.data.ids;

/**
 *	One-stop shop for NSP's IDs, to make things easier to keep consistent when changing them
 */

public class NSP_IDs {
	//==MOD_ID==//
	public static final String NSP = "NSP";

	//==FACTIONS==//
	public static final String FACTION_TELLASIAN_LEAGUE = "tll";

	//V: Personally having memory in separate class feels better?
	public static final String MEM_DAYS_WITH_INVICTA = "$nsp_timeWithInvicta";
	public static final String MEM_NUM_INVICTA_THOUGHTS = "$nsp_numInvictaThoughts"; // only random ones
	public static final String MEM_INVICTA_LEFT = "$nsp_InvictaLeft";
	public static final String MEM_INVICTA_ABANDON_MARKET = "$nsp_InvictaAbandonMarket";

	//==AI CORES==//
	public static final String GAMMA_CORE_NSP = "gamma_core_nsp";
	public static final String BETA_CORE_NSP = "beta_core_nsp";
	public static final String ALPHA_CORE_NSP = "alpha_core_nsp";
	public static final String AI_CORE_EXPONENT = "alpha_core_nsp";
	public static final String AI_CORE_THREAT_PROCESSOR = "alpha_core_nsp";


	public static final String INVICTA_CORE = "nsp_invicta_core";

	//==SKILLS==//
	public static final String SKILL_WEIRDSLAYER = "nsp_weirdslayer"; //Clone of Omega ECM, exists so my changes to it don't break other peoples stuff.

}