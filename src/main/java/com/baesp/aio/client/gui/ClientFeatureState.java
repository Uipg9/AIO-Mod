package com.baesp.aio.client.gui;

/**
 * Client-side feature state storage
 * Used to track toggleable features that don't need server sync
 */
public class ClientFeatureState {
    public static boolean autoToolSwapEnabled = true;
    public static boolean autoEatEnabled = false; // Future feature
    public static boolean coordinatesHudEnabled = true;
}
