package com.example.reaccionatencion;

public class GameConfig {

    public final String player;
    public final String difficulty;
    public final int totalIterations;
    public final int maxTimeSeconds;
    public final boolean inverseMode;
    public final boolean dynamicDifficulty;
    public final boolean soundEnabled;

    public GameConfig(
            String player,
            String difficulty,
            int totalIterations,
            int maxTimeSeconds,
            boolean inverseMode,
            boolean dynamicDifficulty,
            boolean soundEnabled
    ) {
        this.player = player;
        this.difficulty = difficulty;
        this.totalIterations = totalIterations;
        this.maxTimeSeconds = maxTimeSeconds;
        this.inverseMode = inverseMode;
        this.dynamicDifficulty = dynamicDifficulty;
        this.soundEnabled = soundEnabled;
    }
}
