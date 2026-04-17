package com.example.reaccionatencion;

import android.graphics.Color;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class GameViewModel extends ViewModel {

    private static final String[] COLOR_NAMES = {"ROJO", "VERDE", "AZUL", "AMARILLO"};
    private static final int[] COLOR_VALUES = {
            Color.RED,
            Color.rgb(76, 175, 80),
            Color.rgb(33, 150, 243),
            Color.rgb(255, 193, 7)
    };

    private final MutableLiveData<GameUiState> uiState = new MutableLiveData<>();
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Random random = new Random();

    private GameConfig config;
    private CountDownTimer roundTimer;
    private Runnable countdownRunnable;

    private boolean initialized;
    private int currentRound;
    private int points;
    private int correct;
    private int incorrect;
    private long reactionSumMs;
    private int currentMaxTimeSeconds;
    private int currentTimeLeftSeconds;
    private int countdownValue;
    private long roundStartMs;

    private ChallengeRound currentChallenge;

    private String feedbackMessage = "";
    private GameUiState.Feedback lastFeedback = GameUiState.Feedback.NONE;
    private String gameOverReason = "";
    private boolean completedAllRounds;

    public LiveData<GameUiState> getUiState() {
        return uiState;
    }

    public void startGame(GameConfig gameConfig) {
        if (initialized) {
            return;
        }
        initialized = true;
        config = gameConfig;

        currentRound = 0;
        points = 0;
        correct = 0;
        incorrect = 0;
        reactionSumMs = 0;
        currentMaxTimeSeconds = config.maxTimeSeconds;
        currentTimeLeftSeconds = currentMaxTimeSeconds;

        publishState(
                GameUiState.Phase.COUNTDOWN,
                3,
                "Preparado...",
                "Evento multiple",
                "",
                Color.WHITE,
            null,
                emptyOptions(),
                GameUiState.Feedback.NONE,
                false,
                ""
        );
        startCountdown();
    }

    public void onOptionPressed(int optionIndex) {
        GameUiState state = uiState.getValue();
        if (state == null || state.phase != GameUiState.Phase.USER_TURN || currentChallenge == null) {
            return;
        }

        if (optionIndex < 0 || optionIndex >= currentChallenge.options.length) {
            return;
        }

        String selected = currentChallenge.options[optionIndex];
        if (selected == null || selected.isEmpty()) {
            return;
        }

        long reactionMs = SystemClock.elapsedRealtime() - roundStartMs;

        if (optionIndex == currentChallenge.correctOptionIndex) {
            correct++;
            reactionSumMs += reactionMs;
            if (!DifficultyConfig.isTraining(config.difficulty)) {
                int multiplier = DifficultyConfig.scoreMultiplierFor(config.difficulty);
                int speedBonus = Math.max(1, (currentMaxTimeSeconds * 1000 - (int) reactionMs) / 350);
                points += multiplier * (2 + speedBonus);
            }

            lastFeedback = GameUiState.Feedback.CORRECT;
            feedbackMessage = "Correcto";
            increaseDifficultyIfNeeded();

            if (currentRound >= config.totalIterations) {
                finishGame(true, "Completaste todos los eventos");
            } else {
                publishFeedbackAndContinue();
            }
            return;
        }

        incorrect++;
        lastFeedback = GameUiState.Feedback.INCORRECT;
        feedbackMessage = "Incorrecto";

        if (incorrect >= config.maxErrorsToLose) {
                finishGame(false, "Perdiste por exceso de errores");
        } else if (currentRound >= config.totalIterations) {
            finishGame(true, "Completaste todos los eventos");
        } else {
            publishFeedbackAndContinue();
        }
    }

    public void onRoundTimeout() {
        GameUiState state = uiState.getValue();
        if (state == null || state.phase != GameUiState.Phase.USER_TURN) {
            return;
        }

        incorrect++;
        lastFeedback = GameUiState.Feedback.INCORRECT;
        feedbackMessage = "Se acabo el tiempo";

        if (incorrect >= config.maxErrorsToLose) {
                finishGame(false, "Perdiste por exceso de errores");
        } else {
            publishFeedbackAndContinue();
        }
    }

    public void onFeedbackShown() {
        GameUiState state = uiState.getValue();
        if (state == null || state.phase != GameUiState.Phase.FEEDBACK) {
            return;
        }

        if (currentRound >= config.totalIterations || incorrect >= config.maxErrorsToLose) {
              finishGame(currentRound >= config.totalIterations, gameOverReason.isEmpty() ? "Partida finalizada" : gameOverReason);
            return;
        }

        startNextRound();
    }

    private void startCountdown() {
        if (roundTimer != null) {
            roundTimer.cancel();
        }
        if (countdownRunnable != null) {
            handler.removeCallbacks(countdownRunnable);
        }

        countdownValue = 3;
        publishState(
                GameUiState.Phase.COUNTDOWN,
                countdownValue,
                "Preparado...",
                "Mira la consigna de cada evento",
                "",
                Color.WHITE,
            null,
                emptyOptions(),
                GameUiState.Feedback.NONE,
                false,
                ""
        );

        countdownRunnable = new Runnable() {
            @Override
            public void run() {
                countdownValue--;
                if (countdownValue <= 0) {
                    startNextRound();
                    return;
                }

                publishState(
                        GameUiState.Phase.COUNTDOWN,
                        countdownValue,
                        "Preparado...",
                        "Mira la consigna de cada evento",
                        "",
                        Color.WHITE,
                    null,
                        emptyOptions(),
                        GameUiState.Feedback.NONE,
                        false,
                        ""
                );
                handler.postDelayed(this, 1000L);
            }
        };

        handler.postDelayed(countdownRunnable, 1000L);
    }

    private void startNextRound() {
        if (currentRound >= config.totalIterations) {
            finishGame(true, "Completaste todos los eventos");
            return;
        }

        currentRound++;
        currentChallenge = generateChallenge();
        currentTimeLeftSeconds = currentMaxTimeSeconds;
        roundStartMs = SystemClock.elapsedRealtime();
        feedbackMessage = "";
        lastFeedback = GameUiState.Feedback.NONE;

        publishState(
                GameUiState.Phase.USER_TURN,
                0,
                currentChallenge.mainText,
                currentChallenge.ruleText,
                currentChallenge.stimulusText,
                currentChallenge.stimulusColor,
                currentChallenge.colorTiles,
                currentChallenge.options,
                GameUiState.Feedback.NONE,
                false,
                ""
        );

        startRoundTimer();
    }

    private void startRoundTimer() {
        if (roundTimer != null) {
            roundTimer.cancel();
        }

        roundTimer = new CountDownTimer(currentMaxTimeSeconds * 1000L, 1000L) {
            @Override
            public void onTick(long millisUntilFinished) {
                currentTimeLeftSeconds = (int) (millisUntilFinished / 1000L) + 1;
                if (currentChallenge == null) {
                    return;
                }
                publishState(
                        GameUiState.Phase.USER_TURN,
                        0,
                        currentChallenge.mainText,
                        currentChallenge.ruleText,
                        currentChallenge.stimulusText,
                        currentChallenge.stimulusColor,
                        currentChallenge.colorTiles,
                        currentChallenge.options,
                        GameUiState.Feedback.NONE,
                        false,
                        ""
                );
            }

            @Override
            public void onFinish() {
                onRoundTimeout();
            }
        };
        roundTimer.start();
    }

    private void publishFeedbackAndContinue() {
        if (roundTimer != null) {
            roundTimer.cancel();
        }

        ChallengeRound challenge = currentChallenge != null ? currentChallenge : fallbackChallenge();
        publishState(
                GameUiState.Phase.FEEDBACK,
                0,
                challenge.mainText,
                challenge.ruleText,
                challenge.stimulusText,
                challenge.stimulusColor,
                challenge.colorTiles,
                challenge.options,
                lastFeedback,
                false,
                ""
        );
    }

    private void finishGame(boolean completed, String reason) {
        completedAllRounds = completed;
        gameOverReason = reason;

        if (roundTimer != null) {
            roundTimer.cancel();
        }
        if (countdownRunnable != null) {
            handler.removeCallbacks(countdownRunnable);
        }

        ChallengeRound challenge = currentChallenge != null ? currentChallenge : fallbackChallenge();
        publishState(
                GameUiState.Phase.GAME_OVER,
                0,
                "Fin de partida",
                reason,
                challenge.stimulusText,
                challenge.stimulusColor,
                challenge.colorTiles,
                challenge.options,
                GameUiState.Feedback.NONE,
                completed,
                reason
        );
    }

    private void increaseDifficultyIfNeeded() {
        if (!config.dynamicDifficulty) {
            return;
        }
        if (currentRound % 4 == 0 && currentMaxTimeSeconds > floorTimeForDifficulty()) {
            currentMaxTimeSeconds--;
        }
    }

    private int floorTimeForDifficulty() {
        switch (config.difficulty) {
            case "Medio":
                return 7;
            case "Dificil":
                return 5;
            case "Entrenamiento":
                return 12;
            case "Facil":
            default:
                return 10;
        }
    }

    private ChallengeRound createChallengeByDifficulty(String difficulty) {
        if (DifficultyConfig.isTraining(difficulty) || "Facil".equals(difficulty)) {
            int pick = random.nextInt(2);
            return pick == 0 ? createParityChallenge() : createDominantColorChallenge("Facil");
        }

        if ("Medio".equals(difficulty)) {
            int pick = random.nextInt(3);
            if (pick == 0) {
                return createPrimeChallenge();
            }
            if (pick == 1) {
                return createDominantColorChallenge("Medio");
            }
            return createStroopChallenge(false);
        }

        int pick = random.nextInt(3);
        if (pick == 0) {
            return createMultipleChallenge();
        }
        if (pick == 1) {
            return createDominantColorChallenge("Dificil");
        }
        return createStroopChallenge(true);
    }

    private ChallengeRound createNumberChallengeByDifficulty(String difficulty) {
        if (DifficultyConfig.isTraining(difficulty) || "Facil".equals(difficulty)) {
            return createParityChallenge();
        }
        if ("Medio".equals(difficulty)) {
            return createPrimeChallenge();
        }
        return createMultipleChallenge();
    }

    private ChallengeRound generateChallenge() {
        String difficulty = config.difficulty;
        boolean mixedMode = config.inverseMode;
        String eventProfile = normalizeEventProfile(config.eventProfile);

        if (!mixedMode) {
            switch (eventProfile) {
                case "Numeros":
                    return createNumberChallengeByDifficulty(difficulty);
                case "Colores":
                    return createDominantColorChallenge(difficulty);
                case "Stroop":
                    return createStroopChallenge("Dificil".equals(difficulty));
                case "Mixto":
                default:
                    return createChallengeByDifficulty(difficulty);
            }
        }

        return createChallengeByDifficulty(difficulty);
    }

    private ChallengeRound createParityChallenge() {
        int number = 1 + random.nextInt(140);
        String[] options = {"PAR", "IMPAR"};
        int correctIndex = number % 2 == 0 ? 0 : 1;

        return createChallenge(
                "Numero: " + number,
                "Facil: indica si el numero es par o impar",
                String.valueOf(number),
                Color.WHITE,
                options,
                correctIndex
        );
    }

    private ChallengeRound createPrimeChallenge() {
        int number = 2 + random.nextInt(180);
        String[] options = {"PRIMO", "NO PRIMO"};
        int correctIndex = isPrime(number) ? 0 : 1;

        return createChallenge(
                "Numero: " + number,
                "Medio: decide si es primo",
                String.valueOf(number),
                Color.WHITE,
                options,
                correctIndex
        );
    }

    private ChallengeRound createMultipleChallenge() {
        int number = 1 + random.nextInt(240);
        String[] options = {"MULTIPLO DE 3", "MULTIPLO DE 5", "NINGUNO", "AMBOS"};

        int correctIndex;
        boolean by3 = number % 3 == 0;
        boolean by5 = number % 5 == 0;
        if (by3 && by5) {
            correctIndex = 3;
        } else if (by3) {
            correctIndex = 0;
        } else if (by5) {
            correctIndex = 1;
        } else {
            correctIndex = 2;
        }

        return createChallenge(
                "Numero: " + number,
                "Dificil: clasificalo por divisibilidad",
                String.valueOf(number),
                Color.WHITE,
                options,
                correctIndex
        );
    }

    private ChallengeRound createDominantColorChallenge(String level) {
        int[] counts = new int[]{2, 2, 2, 2};
        int dominantIndex = random.nextInt(4);

        if ("Facil".equals(level)) {
            counts[dominantIndex] += 4;
            counts[random.nextInt(4)] += 1;
        } else if ("Medio".equals(level)) {
            counts[dominantIndex] += 3;
            counts[random.nextInt(4)] += 1;
            counts[random.nextInt(4)] += 1;
        } else {
            counts[dominantIndex] += 2;
            counts[random.nextInt(4)] += 1;
            counts[random.nextInt(4)] += 1;
            counts[random.nextInt(4)] += 1;
        }

        int[] tiles = buildColorTiles(counts, 12);
        String[] options = COLOR_NAMES.clone();

        return createChallenge(
                "Que color aparece mas en pantalla?",
            "Observa el mosaico y elige el color dominante",
            "MOSAICO DE COLORES",
                Color.WHITE,
            tiles,
                options,
                dominantIndex
        );
    }

    private ChallengeRound createStroopChallenge(boolean hard) {
        int wordIndex = random.nextInt(COLOR_NAMES.length);
        int paintIndex = random.nextInt(COLOR_NAMES.length);

        int mismatchChance = hard ? 85 : 65;
        if (random.nextInt(100) < mismatchChance) {
            while (paintIndex == wordIndex) {
                paintIndex = random.nextInt(COLOR_NAMES.length);
            }
        }

        String[] options = COLOR_NAMES.clone();

        return createChallenge(
                "Selecciona el color de la tinta",
                "No leas la palabra: responde por el color pintado",
                COLOR_NAMES[wordIndex],
                COLOR_VALUES[paintIndex],
                options,
                paintIndex
        );
    }

    private ChallengeRound createChallenge(
            String mainText,
            String ruleText,
            String stimulusText,
            int stimulusColor,
            String[] rawOptions,
            int rawCorrectIndex
    ) {
        return createChallenge(mainText, ruleText, stimulusText, stimulusColor, null, rawOptions, rawCorrectIndex);
        }

        private ChallengeRound createChallenge(
            String mainText,
            String ruleText,
            String stimulusText,
            int stimulusColor,
            int[] colorTiles,
            String[] rawOptions,
            int rawCorrectIndex
        ) {
        List<Integer> order = new ArrayList<>();
        for (int i = 0; i < rawOptions.length; i++) {
            order.add(i);
        }
        Collections.shuffle(order, random);

        String[] options = emptyOptions();
        int correctIndex = 0;

        for (int i = 0; i < order.size() && i < options.length; i++) {
            int sourceIndex = order.get(i);
            options[i] = rawOptions[sourceIndex];
            if (sourceIndex == rawCorrectIndex) {
                correctIndex = i;
            }
        }

        return new ChallengeRound(mainText, ruleText, stimulusText, stimulusColor, colorTiles, options, correctIndex);
    }

    private String normalizeEventProfile(String eventProfile) {
        if (eventProfile == null || eventProfile.isEmpty()) {
            return "Mixto";
        }
        return eventProfile;
    }

    private int[] buildColorTiles(int[] counts, int tileCount) {
        List<Integer> colors = new ArrayList<>();
        for (int i = 0; i < counts.length; i++) {
            for (int j = 0; j < counts[i]; j++) {
                colors.add(COLOR_VALUES[i]);
            }
        }

        while (colors.size() < tileCount) {
            colors.add(COLOR_VALUES[random.nextInt(COLOR_VALUES.length)]);
        }

        Collections.shuffle(colors, random);
        int[] result = new int[tileCount];
        for (int i = 0; i < tileCount; i++) {
            result[i] = colors.get(i);
        }
        return result;
    }

    private boolean isPrime(int number) {
        if (number < 2) {
            return false;
        }
        if (number == 2) {
            return true;
        }
        if (number % 2 == 0) {
            return false;
        }
        for (int i = 3; i * i <= number; i += 2) {
            if (number % i == 0) {
                return false;
            }
        }
        return true;
    }

    private ChallengeRound fallbackChallenge() {
        return new ChallengeRound("", "", "", Color.WHITE, null, emptyOptions(), 0);
    }

    private String[] emptyOptions() {
        return new String[]{"", "", "", ""};
    }

    private long averageReactionMs() {
        return correct > 0 ? (reactionSumMs / correct) : 0;
    }

    private void publishState(
            GameUiState.Phase phase,
            int preStartCountdown,
            String mainText,
            String ruleText,
            String stimulusText,
            int stimulusTextColor,
            int[] stimulusColorTiles,
            String[] options,
            GameUiState.Feedback feedback,
            boolean completed,
            String reason
    ) {
        uiState.setValue(new GameUiState(
                phase,
                config.player,
                config.difficulty,
                currentRound,
                config.totalIterations,
                points,
                correct,
                incorrect,
                currentTimeLeftSeconds,
                config.maxTimeSeconds,
                currentMaxTimeSeconds,
                preStartCountdown,
                mainText,
                ruleText,
                stimulusText,
                stimulusTextColor,
                stimulusColorTiles,
                options,
                feedbackMessage,
                feedback,
                config.inverseMode,
                config.dynamicDifficulty,
                completed,
                reason,
                averageReactionMs()
        ));
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        if (roundTimer != null) {
            roundTimer.cancel();
        }
        if (countdownRunnable != null) {
            handler.removeCallbacks(countdownRunnable);
        }
    }

    private static class ChallengeRound {
        final String mainText;
        final String ruleText;
        final String stimulusText;
        final int stimulusColor;
        final int[] colorTiles;
        final String[] options;
        final int correctOptionIndex;

        ChallengeRound(
                String mainText,
                String ruleText,
                String stimulusText,
                int stimulusColor,
                int[] colorTiles,
                String[] options,
                int correctOptionIndex
        ) {
            this.mainText = mainText;
            this.ruleText = ruleText;
            this.stimulusText = stimulusText;
            this.stimulusColor = stimulusColor;
            this.colorTiles = colorTiles;
            this.options = options;
            this.correctOptionIndex = correctOptionIndex;
        }
    }
}
