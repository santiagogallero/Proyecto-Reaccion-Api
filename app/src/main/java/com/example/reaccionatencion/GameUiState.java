package com.example.reaccionatencion;

public class GameUiState {

    public enum Phase {
        COUNTDOWN,
        USER_TURN,
        FEEDBACK,
        GAME_OVER
    }

    public enum Feedback {
        NONE,
        CORRECT,
        INCORRECT
    }

    public final Phase phase;
    public final String player;
    public final String difficulty;
    public final int currentRound;
    public final int totalRounds;
    public final int points;
    public final int correct;
    public final int incorrect;
    public final int timeLeftSeconds;
    public final int maxTimeStart;
    public final int maxTimeCurrent;
    public final int preStartCountdown;
    public final String mainText;
    public final String ruleText;
    public final String stimulusText;
    public final int stimulusTextColor;
    public final int[] stimulusColorTiles;
    public final String[] options;
    public final String feedbackMessage;
    public final Feedback feedback;
    public final boolean inverseMode;
    public final boolean dynamicDifficulty;
    public final boolean completedAllRounds;
    public final String gameOverReason;
    public final long averageReactionMs;

    public GameUiState(
            Phase phase,
            String player,
            String difficulty,
            int currentRound,
            int totalRounds,
            int points,
            int correct,
            int incorrect,
            int timeLeftSeconds,
            int maxTimeStart,
            int maxTimeCurrent,
            int preStartCountdown,
            String mainText,
            String ruleText,
            String stimulusText,
            int stimulusTextColor,
            int[] stimulusColorTiles,
            String[] options,
            String feedbackMessage,
            Feedback feedback,
            boolean inverseMode,
            boolean dynamicDifficulty,
            boolean completedAllRounds,
            String gameOverReason,
            long averageReactionMs
    ) {
        this.phase = phase;
        this.player = player;
        this.difficulty = difficulty;
        this.currentRound = currentRound;
        this.totalRounds = totalRounds;
        this.points = points;
        this.correct = correct;
        this.incorrect = incorrect;
        this.timeLeftSeconds = timeLeftSeconds;
        this.maxTimeStart = maxTimeStart;
        this.maxTimeCurrent = maxTimeCurrent;
        this.preStartCountdown = preStartCountdown;
        this.mainText = mainText;
        this.ruleText = ruleText;
        this.stimulusText = stimulusText;
        this.stimulusTextColor = stimulusTextColor;
        this.stimulusColorTiles = stimulusColorTiles;
        this.options = options;
        this.feedbackMessage = feedbackMessage;
        this.feedback = feedback;
        this.inverseMode = inverseMode;
        this.dynamicDifficulty = dynamicDifficulty;
        this.completedAllRounds = completedAllRounds;
        this.gameOverReason = gameOverReason;
        this.averageReactionMs = averageReactionMs;
    }
}
