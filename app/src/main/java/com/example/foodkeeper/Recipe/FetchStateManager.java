package com.example.foodkeeper.Recipe;

import android.content.Context;
import android.content.SharedPreferences;

public class FetchStateManager {
    private static final String PREF_NAME = "FetchState";
    private static final String KEY_LAST_USER = "lastUserId";

    private SharedPreferences prefs;
    private SharedPreferences.Editor editor;

    public FetchStateManager(Context context) {
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        editor = prefs.edit();
    }

    /**
     * Check if the user has fetched recipes before
     * @param userEmail The user's email address
     * @return true if recipes have been fetched for this user
     */
    public boolean hasFetchedRecipes(String userEmail) {
        return prefs.getBoolean(getUserKey(userEmail), false);
    }

    /**
     * Mark that this user has fetched recipes
     * @param userEmail The user's email address
     * @param fetched true to mark as fetched, false otherwise
     */
    public void setRecipesFetched(String userEmail, boolean fetched) {
        editor.putBoolean(getUserKey(userEmail), fetched);
        editor.putString(KEY_LAST_USER, userEmail);
        editor.apply();
    }

    /**
     * Check if this is a different user than the last logged-in user
     * @param userEmail The user's email address
     * @return true if this is a different user
     */
    public boolean isDifferentUser(String userEmail) {
        String lastUser = prefs.getString(KEY_LAST_USER, "");
        return !userEmail.equals(lastUser);
    }

    /**
     * Get the last logged-in user's email
     * @return The last user's email, or empty string if none
     */
    public String getLastUser() {
        return prefs.getString(KEY_LAST_USER, "");
    }

    /**
     * Reset a specific user's fetch state
     * Use this if you want to force re-fetch for a specific user
     * @param userEmail The user's email address
     */
    public void resetFetchState(String userEmail) {
        editor.putBoolean(getUserKey(userEmail), false);
        editor.apply();
    }

    /**
     * Clear all fetch states for all users
     * Use this for testing or complete app reset
     */
    public void clearAllStates() {
        editor.clear();
        editor.apply();
    }

    /**
     * Helper method to create a unique key per user
     * @param userEmail The user's email address
     * @return A unique key for this user
     */
    private String getUserKey(String userEmail) {
        return "recipesFetched_" + userEmail;
    }
}