package com.example.reaccionatencion;

import android.content.Intent;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.example.reaccionatencion.databinding.ActivityGameBinding;

public class GameActivity extends AppCompatActivity {

    public static final String EXTRA_PLAYER = "extra_player";
    public static final String EXTRA_DIFFICULTY = "extra_difficulty";
    public static final String EXTRA_ITERATIONS = "extra_iterations";
    public static final String EXTRA_MAX_TIME_SECONDS = "extra_max_time_seconds";
    public static final String EXTRA_INVERSE_MODE = "extra_inverse_mode";
    public static final String EXTRA_DYNAMIC_DIFFICULTY = "extra_dynamic_difficulty";
    public static final String EXTRA_SOUND_ENABLED = "extra_sound_enabled";

    private ActivityGameBinding binding;
    private GameViewModel viewModel;
    private final Button[] pads = new Button[4];
    private boolean soundEnabled;
    private ToneGenerator toneGenerator;
    private boolean hasNavigated;
    private boolean feedbackAdvanceScheduled;
    private int lastHighlightedPad = -1;
        private static final int[] PAD_TONES = {
            ToneGenerator.TONE_DTMF_1,
            ToneGenerator.TONE_DTMF_2,
            ToneGenerator.TONE_DTMF_3,
            ToneGenerator.TONE_DTMF_4
        };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityGameBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        GameConfig config = readConfig();
        soundEnabled = config.soundEnabled;
        if (soundEnabled) {
            toneGenerator = new ToneGenerator(AudioManager.STREAM_NOTIFICATION, 70);
        }

        pads[0] = binding.btnPad0;
        pads[1] = binding.btnPad1;
        pads[2] = binding.btnPad2;
        pads[3] = binding.btnPad3;

        viewModel = new ViewModelProvider(this).get(GameViewModel.class);
        bindActions();
        observeState();
        viewModel.startGame(config);
    }

    private GameConfig readConfig() {
        Intent i = getIntent();
        String player = i.getStringExtra(EXTRA_PLAYER);
        String difficulty = i.getStringExtra(EXTRA_DIFFICULTY);
        int totalIterations = i.getIntExtra(EXTRA_ITERATIONS, 20);
        int maxTimeSeconds = i.getIntExtra(EXTRA_MAX_TIME_SECONDS, DifficultyConfig.defaultMaxTimeFor(difficulty));
        boolean inverseMode = i.getBooleanExtra(EXTRA_INVERSE_MODE, false);
        boolean dynamicDifficulty = i.getBooleanExtra(EXTRA_DYNAMIC_DIFFICULTY, false);
        boolean sounds = i.getBooleanExtra(EXTRA_SOUND_ENABLED, true);

        if (player == null || player.isEmpty()) {
            player = "Jugador";
        }
        if (difficulty == null || difficulty.isEmpty()) {
            difficulty = "Facil";
        }
        if (totalIterations <= 0) {
            totalIterations = 20;
        }
        if (maxTimeSeconds < 2) {
            maxTimeSeconds = 2;
        }
        if (maxTimeSeconds > 30) {
            maxTimeSeconds = 30;
        }

        return new GameConfig(
                player,
                difficulty,
                totalIterations,
                maxTimeSeconds,
                inverseMode,
                dynamicDifficulty,
                sounds
        );
    }

    private void bindActions() {
        binding.btnPad0.setOnClickListener(v -> {
            playPadTone(0);
            viewModel.onPadPressed(0);
        });
        binding.btnPad1.setOnClickListener(v -> {
            playPadTone(1);
            viewModel.onPadPressed(1);
        });
        binding.btnPad2.setOnClickListener(v -> {
            playPadTone(2);
            viewModel.onPadPressed(2);
        });
        binding.btnPad3.setOnClickListener(v -> {
            playPadTone(3);
            viewModel.onPadPressed(3);
        });
        binding.btnExit.setOnClickListener(v -> finish());
    }

    private void observeState() {
        viewModel.getUiState().observe(this, this::renderState);
    }

    private void renderState(GameUiState state) {
        if (state == null) {
            return;
        }

        binding.tvGameInfo.setText(
                "Jugador: " + state.player
                        + " | Modo: " + state.difficulty
                        + " | Nivel " + Math.max(state.currentRound, 1) + "/" + state.totalRounds
        );
        binding.tvScore.setText(
                "Puntos: " + state.points
                        + " | Aciertos: " + state.correct
                        + " | Errores: " + state.incorrect
                        + " | Tiempo max: " + state.maxTimeCurrent + "s"
        );
        binding.tvRule.setText(state.ruleText);
        if (state.phase == GameUiState.Phase.COUNTDOWN) {
            binding.tvTimer.setText("Inicia en...");
        } else if (state.phase == GameUiState.Phase.SHOW_SEQUENCE) {
            binding.tvTimer.setText("Observa la secuencia");
        } else {
            binding.tvTimer.setText("Tiempo restante: " + state.timeLeftSeconds + " s");
        }
        binding.tvMain.setText(state.mainText);

        if (state.phase == GameUiState.Phase.COUNTDOWN) {
            binding.tvCountdown.setVisibility(View.VISIBLE);
            binding.tvCountdown.setText(String.valueOf(state.preStartCountdown));
            binding.tvHint.setText("Preparado para repetir secuencias");
        } else if (state.phase == GameUiState.Phase.SHOW_SEQUENCE) {
            binding.tvCountdown.setVisibility(View.GONE);
            binding.tvHint.setText("Mira los colores y memorizalos");
        } else {
            binding.tvCountdown.setVisibility(View.GONE);
            binding.tvHint.setText(state.phase == GameUiState.Phase.USER_TURN
                    ? (state.mainText.contains("NO toques")
                        ? "Modo inversa: esta ronda debes esperar sin tocar"
                        : "Toca los colores en el mismo orden")
                    : "Espera la secuencia");
        }

        boolean enablePads = state.phase == GameUiState.Phase.USER_TURN;
        for (Button pad : pads) {
            pad.setEnabled(enablePads);
            pad.setAlpha(enablePads ? 1f : 0.78f);
        }

        highlightPad(state.highlightedPadIndex);

        if (state.phase == GameUiState.Phase.FEEDBACK) {
            showFeedback(state.feedback, state.feedbackMessage);
            if (!feedbackAdvanceScheduled) {
                feedbackAdvanceScheduled = true;
                binding.rootGame.postDelayed(() -> {
                    feedbackAdvanceScheduled = false;
                    viewModel.onFeedbackShown();
                }, 550L);
            }
        } else {
            feedbackAdvanceScheduled = false;
            binding.viewFeedback.setAlpha(0f);
        }

        if (state.phase == GameUiState.Phase.GAME_OVER && !hasNavigated) {
            hasNavigated = true;
            navigateToResults(state);
        }
    }

    private void highlightPad(int padIndex) {
        if (padIndex == lastHighlightedPad) {
            return;
        }

        for (int i = 0; i < pads.length; i++) {
            Button pad = pads[i];
            if (i == padIndex) {
                pad.animate().scaleX(1.05f).scaleY(1.05f).setDuration(120L).start();
            } else {
                pad.animate().scaleX(1f).scaleY(1f).setDuration(120L).start();
            }
        }
        if (padIndex >= 0) {
            playPadTone(padIndex);
        }
        lastHighlightedPad = padIndex;
    }

    private void playPadTone(int padIndex) {
        if (padIndex < 0 || padIndex >= PAD_TONES.length) {
            return;
        }
        playTone(PAD_TONES[padIndex]);
    }

    private void showFeedback(GameUiState.Feedback feedback, String message) {
        int overlayColor;
        int tone;
        if (feedback == GameUiState.Feedback.CORRECT) {
            overlayColor = 0x552ED573;
            tone = ToneGenerator.TONE_PROP_ACK;
        } else if (feedback == GameUiState.Feedback.INCORRECT) {
            overlayColor = 0x55FF6B6B;
            tone = ToneGenerator.TONE_PROP_NACK;
        } else {
            overlayColor = 0x00000000;
            tone = ToneGenerator.TONE_PROP_BEEP;
        }

        binding.viewFeedback.setBackgroundColor(overlayColor);
        binding.viewFeedback.setAlpha(0f);
        binding.viewFeedback.animate().alpha(1f).setDuration(90L).withEndAction(() ->
                binding.viewFeedback.animate().alpha(0f).setDuration(180L).start()
        ).start();

        if (message != null && !message.isEmpty()) {
            binding.tvHint.setText(message);
        }
        playTone(tone);
    }

    private void navigateToResults(GameUiState state) {
        ScoreStorage storage = new ScoreStorage(this);
        if (!DifficultyConfig.isTraining(state.difficulty)) {
            storage.saveIfBest(state.player, state.points, state.averageReactionMs);
        }

        Intent intent = new Intent(this, ResultActivity.class);
        intent.putExtra("player", state.player);
        intent.putExtra("difficulty", state.difficulty);
        intent.putExtra("inverseMode", state.inverseMode);
        intent.putExtra("dynamicDifficulty", state.dynamicDifficulty);
        intent.putExtra("points", state.points);
        intent.putExtra("correct", state.correct);
        intent.putExtra("incorrect", state.incorrect);
        intent.putExtra("avgMs", state.averageReactionMs);
        intent.putExtra("totalIterations", state.totalRounds);
        intent.putExtra("maxTimeStart", state.maxTimeStart);
        intent.putExtra("maxTimeEnd", state.maxTimeCurrent);
        intent.putExtra("reason", state.gameOverReason);
        intent.putExtra("completedAllRounds", state.completedAllRounds);
        startActivity(intent);
        finish();
    }

    private void playTone(int tone) {
        if (!soundEnabled || toneGenerator == null) {
            return;
        }
        toneGenerator.startTone(tone, 120);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (toneGenerator != null) {
            toneGenerator.release();
        }
    }
}
