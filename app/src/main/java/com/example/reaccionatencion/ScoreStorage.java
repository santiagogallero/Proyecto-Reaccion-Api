package com.example.reaccionatencion;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

public class ScoreStorage {

    private static final String PREFS = "reaccion_scores";
    private static final String KEY_GLOBAL_BEST_PLAYER = "global_best_player";
    private static final String KEY_GLOBAL_BEST_POINTS = "global_best_points";
    private static final String KEY_GLOBAL_BEST_AVG = "global_best_avg";
    private static final String KEY_PLAYERS = "players";

    private final SharedPreferences prefs;

    public ScoreStorage(Context context) {
        prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public void saveIfBest(String player, int points, long averageReactionMs) {
        int currentBest = prefs.getInt(KEY_GLOBAL_BEST_POINTS, -1);
        if (points > currentBest) {
            prefs.edit()
                    .putString(KEY_GLOBAL_BEST_PLAYER, player)
                    .putInt(KEY_GLOBAL_BEST_POINTS, points)
                    .putLong(KEY_GLOBAL_BEST_AVG, averageReactionMs)
                    .apply();
        }

        String playerKey = buildPlayerBestKey(player);
        int playerBest = prefs.getInt(playerKey, -1);
        if (points > playerBest) {
            prefs.edit().putInt(playerKey, points).apply();
        }

        Set<String> players = new HashSet<>(prefs.getStringSet(KEY_PLAYERS, new HashSet<>()));
        players.add(player);
        prefs.edit().putStringSet(KEY_PLAYERS, players).apply();
    }

    public String getGlobalBestSummary() {
        int best = prefs.getInt(KEY_GLOBAL_BEST_POINTS, -1);
        if (best < 0) {
            return "Mejor puntaje: aun no hay partidas";
        }
        String player = prefs.getString(KEY_GLOBAL_BEST_PLAYER, "Jugador");
        long avg = prefs.getLong(KEY_GLOBAL_BEST_AVG, 0);
        return "Mejor puntaje: " + best + " - " + player + " (promedio " + avg + " ms)";
    }

    public int getPlayerBest(String player) {
        return prefs.getInt(buildPlayerBestKey(player), -1);
    }

    public String getPlayerBestSummary(String player) {
        int best = getPlayerBest(player);
        if (best < 0) {
            return "Mejor de " + player + ": aun sin partidas puntuables";
        }
        return "Mejor de " + player + ": " + best + " puntos";
    }

    private String buildPlayerBestKey(String player) {
        String normalized = player == null ? "jugador" : player.trim().toLowerCase(Locale.ROOT);
        normalized = normalized.replaceAll("[^a-z0-9]", "_");
        if (normalized.isEmpty()) {
            normalized = "jugador";
        }
        return "player_best_" + normalized;
    }
}
