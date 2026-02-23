package neon.nsp.data;

import com.fs.starfarer.api.util.WeightedRandomPicker;

import java.awt.Color;

public class NSP_reference_sheet {

    // Static initialization
    public static final WeightedRandomPicker<String> meatsoundlist = new WeightedRandomPicker<>();

    // commodity/special items


    public static final String NSP_AISWITCHAUTOMATED = "NSP_aiswitch_auto";
    public static final String NSP_AISWITCHMANUAL = "NSP_aiswitch_manual";

    public static final String NSP_AISWITCHAUTOMATED_ELITE = "NSP_aiswitch_auto_elite";
    public static final String NSP_AISWITCHMANUAL_ELITE = "NSP_aiswitch_manual_elite";


    // Constructor (init block from Kotlin)
    public NSP_reference_sheet() {
        // Empty constructor as the init block only contained static initialization
    }
}