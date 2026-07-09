package com.luminadigitale.fluxcore.core.engine

object PatternCatalog {
    val templates: List<PatternTemplate> = listOf(
        PatternTemplate("clean_arc", PatternFamily.SINGLE_THREAT, 1, 0, 2, 0.88f, 1.0f, 0.98f, 1.04f, 1.0f, 1.12f),
        PatternTemplate("narrow_gate", PatternFamily.DOUBLE_PHRASE, 1, 0, 2, 0.92f, 1.04f, 0.92f, 1.02f, 0.96f, 1.04f),
        PatternTemplate("wide_ring", PatternFamily.DELAYED_FOLLOW_UP, 1, -1, 2, 0.82f, 0.96f, 1.02f, 1.14f, 1.04f, 1.14f),
        PatternTemplate("drift_gap", PatternFamily.ALTERNATING_PRESSURE, 1, 1, null, 0.92f, 1.04f, 0.94f, 1.0f, 0.92f, 1.0f),
        PatternTemplate("pulse_ring", PatternFamily.DELAYED_FOLLOW_UP, 1, -2, 2, 0.88f, 1.0f, 1.04f, 1.18f, 0.94f, 1.04f),
        PatternTemplate("tight_teeth", PatternFamily.DOUBLE_PHRASE, 2, 2, 1, 0.96f, 1.08f, 0.9f, 0.98f, 0.9f, 0.98f),
        PatternTemplate("gravity_pull", PatternFamily.FAKE_OUT_SHIFT, 2, 1, 2, 0.9f, 1.04f, 0.92f, 1.04f, 1.04f, 1.2f),
        PatternTemplate("split_lane", PatternFamily.SPIRAL_PRESSURE, 3, 2, null, 0.98f, 1.1f, 0.88f, 0.98f, 0.88f, 0.98f),
        PatternTemplate("needle_window", PatternFamily.FAKE_OUT_SHIFT, 3, 1, 1, 0.98f, 1.12f, 0.9f, 0.98f, 0.88f, 0.96f),
        PatternTemplate("flux_mirror", PatternFamily.FAKE_OUT_SHIFT, 3, -2, 2, 0.94f, 1.08f, 0.9f, 1.0f, 0.9f, 1.02f),
        PatternTemplate("time_bubble", PatternFamily.DELAYED_FOLLOW_UP, 4, 0, 2, 0.9f, 1.02f, 0.92f, 1.02f, 1.06f, 1.22f),
        PatternTemplate("dense_blades", PatternFamily.ALTERNATING_PRESSURE, 4, 3, 1, 1.02f, 1.14f, 0.84f, 0.94f, 0.84f, 0.94f),
        PatternTemplate("core_surge", PatternFamily.SPIRAL_PRESSURE, 5, -3, 1, 1.04f, 1.18f, 0.82f, 0.92f, 0.82f, 0.94f),
        PatternTemplate("final_crush", PatternFamily.SPIRAL_PRESSURE, 5, 4, 1, 1.06f, 1.2f, 0.82f, 0.92f, 0.8f, 0.9f),
        PatternTemplate("missile_volley", PatternFamily.ALTERNATING_PRESSURE, 7, 0, 2, 1.16f, 1.32f, 0.76f, 0.9f, 0.84f, 0.98f)
    )
}
