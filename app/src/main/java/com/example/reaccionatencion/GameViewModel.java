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
import java.util.List;
import java.util.Random;

public class GameViewModel extends ViewModel {

    private static final int MAX_ERRORS_TO_LOSE = 3;
    private static final int PAD_COUNT = 4;

    private final MutableLiveData<GameUiState> uiState = new MutableLiveData<>();
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Random random = new Random();

    private GameConfig config;
    private CountDownTimer roundTimer;
    private Runnable sequenceRunnable;

    private final List<Integer> sequence = new ArrayList<>();
    private boolean initialized;
    private int currentRound;
    private int points;
    private int correct;
    private int incorrect;
    private long reactionSumMs;
    private int currentMaxTimeSeconds;
    private int sequencePlaybackDelayMs;
    private int sequenceFlashDurationMs;
    private int currentRoundDurationSeconds;
    private int currentPadIndex = -1;
    private int userIndex;
    private long userTurnStartMs;
    private int countdownValue;
    private String feedbackMessage = "";
    private boolean showingSequence;
    private GameUiState.Feedback lastFeedback = GameUiState.Feedback.NONE;
    private int currentTimeLeftSeconds;
    private String gameOverReason = "";
    private boolean completedAllRounds;
    private final List<Integer> expectedInputs = new ArrayList<>();

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
        sequencePlaybackDelayMs = getInitialPlaybackDelay();
        countdownValue = 3;
        currentTimeLeftSeconds = currentMaxTimeSeconds;
        sequenceFlashDurationMs = getInitialFlashDuration();
        currentRoundDurationSeconds = currentMaxTimeSeconds;

        publishState(GameUiState.Phase.COUNTDOWN, 3, -1, 0, "Preparado...", "Mira la secuencia", GameUiState.Feedback.NONE, false, "");
        startCountdown();
    }

    public void onPadPressed(int padIndex) {
        GameUiState state = uiState.getValue();
        if (state == null || state.phase != GameUiState.Phase.USER_TURN || showingSequence) {
            return;
        }

        if (expectedInputs.isEmpty()) {
            incorrect++;
            lastFeedback = GameUiState.Feedback.INCORRECT;
            feedbackMessage = "Incorrecto: en esta ronda debias no tocar";
            if (incorrect >= MAX_ERRORS_TO_LOSE) {
                finishGame(false, "Perdiste por exceso de errores");
            } else {
                publishFeedbackAndContinue();
            }
            return;
        }

        if (padIndex != expectedInputs.get(userIndex)) {
            incorrect++;
            lastFeedback = GameUiState.Feedback.INCORRECT;
            feedbackMessage = "Incorrecto: la secuencia era otra";
            if (incorrect >= MAX_ERRORS_TO_LOSE) {
                finishGame(false, "Perdiste por exceso de errores");
            } else {
                publishFeedbackAndContinue();
            }
            return;
        }

        if (userIndex == 0) {
            userTurnStartMs = SystemClock.elapsedRealtime();
        }

        userIndex++;
        if (userIndex < expectedInputs.size()) {
            lastFeedback = GameUiState.Feedback.NONE;
            publishState(GameUiState.Phase.USER_TURN, 0, -1, sequence.size(), "Segui repitiendo", buildRuleText(), GameUiState.Feedback.NONE, false, "");
            return;
        }

        correct++;
        long reactionMs = SystemClock.elapsedRealtime() - userTurnStartMs;
        reactionSumMs += reactionMs;
        if (!DifficultyConfig.isTraining(config.difficulty)) {
            int multiplier = DifficultyConfig.scoreMultiplierFor(config.difficulty);
            int speedBonus = Math.max(1, (currentMaxTimeSeconds * 1000 - (int) reactionMs) / 250);
            points += multiplier * (expectedInputs.size() + speedBonus);
        }

        lastFeedback = GameUiState.Feedback.CORRECT;
        feedbackMessage = "Correcto: secuencia completada";
        increaseDifficultyIfNeeded();

        if (currentRound >= config.totalIterations) {
            finishGame(true, "Completaste todas las rondas");
        } else {
            publishFeedbackAndContinue();
        }
    }

    public void onFeedbackShown() {
        if (uiState.getValue() == null) {
            return;
        }
        if (uiState.getValue().phase != GameUiState.Phase.FEEDBACK) {
            return;
        }
        if (currentRound >= config.totalIterations) {
            finishGame(true, "Completaste todas las rondas");
            return;
        }
        startNextRound();
    }

    public void onRoundTimeout() {
        if (uiState.getValue() == null || uiState.getValue().phase != GameUiState.Phase.USER_TURN) {
            return;
        }

        incorrect++;
        lastFeedback = GameUiState.Feedback.INCORRECT;
        feedbackMessage = "Incorrecto: se acabo el tiempo";
        if (incorrect >= MAX_ERRORS_TO_LOSE) {
            finishGame(false, "Perdiste por exceso de errores");
        } else {
            publishFeedbackAndContinue();
        }
    }

    private void startCountdown() {
        if (roundTimer != null) {
            roundTimer.cancel();
        }
        countdownValue = 3;
        publishState(GameUiState.Phase.COUNTDOWN, countdownValue, -1, 0, "Preparado...", "Mira la secuencia", GameUiState.Feedback.NONE, false, "");

        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                countdownValue--;
                if (countdownValue <= 0) {
                    startNextRound();
                    return;
                }
                publishState(GameUiState.Phase.COUNTDOWN, countdownValue, -1, 0, "Preparado...", "Mira la secuencia", GameUiState.Feedback.NONE, false, "");
                handler.postDelayed(this, 1000L);
            }
        }, 1000L);
    }

    private void startNextRound() {
        if (currentRound >= config.totalIterations) {
            finishGame(true, "Completaste todas las rondas");
            return;
        }

        currentRound++;
        sequence.add(random.nextInt(PAD_COUNT));
        rebuildExpectedInputs();
        userIndex = 0;
        currentTimeLeftSeconds = currentMaxTimeSeconds;
        currentPadIndex = -1;
        showingSequence = true;
        feedbackMessage = "";
        lastFeedback = GameUiState.Feedback.NONE;

        publishState(GameUiState.Phase.SHOW_SEQUENCE, 0, -1, sequence.size(), "Observa la secuencia", buildRuleText(), GameUiState.Feedback.NONE, false, "");
        playSequence();
    }

    private void playSequence() {
        if (sequenceRunnable != null) {
            handler.removeCallbacks(sequenceRunnable);
        }
        if (roundTimer != null) {
            roundTimer.cancel();
        }

        final int[] index = {0};
        sequenceRunnable = new Runnable() {
            @Override
            public void run() {
                if (index[0] >= sequence.size()) {
                    currentPadIndex = -1;
                    showingSequence = false;
                    startUserTurn();
                    return;
                }

                currentPadIndex = sequence.get(index[0]);
                publishState(GameUiState.Phase.SHOW_SEQUENCE, 0, currentPadIndex, sequence.size(), "Observa la secuencia", buildRuleText(), GameUiState.Feedback.NONE, false, "");
                index[0]++;
                handler.postDelayed(() -> {
                    currentPadIndex = -1;
                    publishState(GameUiState.Phase.SHOW_SEQUENCE, 0, -1, sequence.size(), "Observa la secuencia", buildRuleText(), GameUiState.Feedback.NONE, false, "");
                    handler.postDelayed(this, sequencePlaybackDelayMs);
                }, sequenceFlashDurationMs);
            }
        };
        handler.postDelayed(sequenceRunnable, 200L);
    }

    private void startUserTurn() {
        currentPadIndex = -1;
        showingSequence = false;
        currentRoundDurationSeconds = computeRoundDurationSeconds();
        currentTimeLeftSeconds = currentRoundDurationSeconds;
        userTurnStartMs = SystemClock.elapsedRealtime();
        startRoundTimer();
        if (expectedInputs.isEmpty()) {
            publishState(GameUiState.Phase.USER_TURN, 0, -1, sequence.size(), "Ronda inversa: NO toques nada", buildRuleText(), GameUiState.Feedback.NONE, false, "");
        } else {
            publishState(GameUiState.Phase.USER_TURN, 0, -1, sequence.size(), "Tu turno: repite la secuencia", buildRuleText(), GameUiState.Feedback.NONE, false, "");
        }
    }

    private void startRoundTimer() {
        if (roundTimer != null) {
            roundTimer.cancel();
        }

        roundTimer = new CountDownTimer(currentRoundDurationSeconds * 1000L, 1000L) {
            @Override
            public void onTick(long millisUntilFinished) {
                currentTimeLeftSeconds = (int) (millisUntilFinished / 1000L) + 1;
                String mainText = expectedInputs.isEmpty()
                        ? "Ronda inversa: NO toques nada"
                        : "Tu turno: repite la secuencia";
                publishState(GameUiState.Phase.USER_TURN, 0, currentPadIndex, sequence.size(), mainText, buildRuleText(), GameUiState.Feedback.NONE, false, "");
            }

            @Override
            public void onFinish() {
                onRoundTimeout();

                    if (expectedInputs.isEmpty()) {
                        correct++;
                        if (!DifficultyConfig.isTraining(config.difficulty)) {
                            int multiplier = DifficultyConfig.scoreMultiplierFor(config.difficulty);
                            points += Math.max(1, multiplier * 2);
                        }
                        lastFeedback = GameUiState.Feedback.CORRECT;
                        feedbackMessage = "Correcto: no reaccionar era lo esperado";
                        increaseDifficultyIfNeeded();
                        if (currentRound >= config.totalIterations) {
                            finishGame(true, "Completaste todas las rondas");
                        } else {
                            publishFeedbackAndContinue();
                        }
                        return;
                    }
            }
        };
        roundTimer.start();
    }

    private void publishFeedbackAndContinue() {
        if (roundTimer != null) {
            roundTimer.cancel();
        }
        publishState(GameUiState.Phase.FEEDBACK, 0, -1, sequence.size(), buildRuleText(), feedbackMessage, lastFeedback, false, "");
    }

    private void finishGame(boolean completed, String reason) {
        completedAllRounds = completed;
        gameOverReason = reason;
        if (roundTimer != null) {
            roundTimer.cancel();
        }
        if (sequenceRunnable != null) {
            handler.removeCallbacks(sequenceRunnable);
        }
        publishState(GameUiState.Phase.GAME_OVER, 0, -1, sequence.size(), "Fin de partida", reason, GameUiState.Feedback.NONE, completed, reason);
    }

    private void increaseDifficultyIfNeeded() {
        if (!config.dynamicDifficulty) {
            return;
        }
        if (currentRound % 3 == 0 && currentMaxTimeSeconds > 2) {
            currentMaxTimeSeconds--;
        }
        if (currentRound % 2 == 0 && sequencePlaybackDelayMs > 170) {
            sequencePlaybackDelayMs -= 25;
        }
        if (currentRound % 2 == 0 && sequenceFlashDurationMs > 110) {
            sequenceFlashDurationMs -= 15;
        }
    }

    private int getInitialPlaybackDelay() {
        switch (config.difficulty) {
            case "Medio":
                return 560;
            case "Dificil":
                return 430;
            case "Entrenamiento":
            case "Facil":
            default:
                return 760;
        }
    }

    private int getInitialFlashDuration() {
        switch (config.difficulty) {
            case "Medio":
                return 260;
            case "Dificil":
                return 180;
            case "Entrenamiento":
            case "Facil":
            default:
                return 340;
        }
    }

    private String buildRuleText() {
        if (config.inverseMode) {
            return "Inversa: NO tocar ROJO. Si todos son ROJO, no hagas nada";
        }
        return "Simon: memoriza y repite la secuencia";
    }

    private void rebuildExpectedInputs() {
        expectedInputs.clear();
        if (!config.inverseMode) {
            expectedInputs.addAll(sequence);
            return;
        }

        for (int value : sequence) {
            if (value != 0) {
                expectedInputs.add(value);
            }
        }
    }

    private int computeRoundDurationSeconds() {
        if (config.inverseMode && expectedInputs.isEmpty()) {
            return Math.min(30, currentMaxTimeSeconds + 3);
        }
        return currentMaxTimeSeconds;
    }

    private long averageReactionMs() {
        return correct > 0 ? (reactionSumMs / correct) : 0;
    }

    private void publishState(
            GameUiState.Phase phase,
            int preStartCountdown,
            int highlightedPadIndex,
            int sequenceLength,
            String mainText,
            String ruleText,
            GameUiState.Feedback feedback,
            boolean completedAllRounds,
            String gameOverReason
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
                highlightedPadIndex,
                sequenceLength,
                mainText,
                ruleText,
                feedbackMessage,
                feedback,
                config.inverseMode,
                config.dynamicDifficulty,
                completedAllRounds,
                gameOverReason,
                averageReactionMs()
        ));
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        if (roundTimer != null) {
            roundTimer.cancel();
        }
        if (sequenceRunnable != null) {
            handler.removeCallbacks(sequenceRunnable);
        }
    }
}
