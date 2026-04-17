package com.example.reaccionatencion;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Bundle;
import android.widget.Button;
import android.widget.GridLayout;
import android.view.View;

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
    public static final String EXTRA_EVENT_PROFILE = "extra_event_profile";
    public static final String EXTRA_MAX_ERRORS = "extra_max_errors";

    private ActivityGameBinding binding;
    private GameViewModel viewModel;
    private final Button[] pads = new Button[4];
    private boolean soundEnabled;
    private String selectedEventProfile = "Mixto";
    private int selectedMaxErrors = 3;
    private ToneGenerator toneGenerator;
    private boolean hasNavigated;
    private boolean feedbackAdvanceScheduled;
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
        selectedEventProfile = config.eventProfile;
        selectedMaxErrors = config.maxErrorsToLose;
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
        String eventProfile = i.getStringExtra(EXTRA_EVENT_PROFILE);
        int maxErrors = i.getIntExtra(EXTRA_MAX_ERRORS, 3);

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
        if (eventProfile == null || eventProfile.isEmpty()) {
            eventProfile = "Mixto";
        }
        if (maxErrors < 1) {
            maxErrors = 1;
        }
        if (maxErrors > 10) {
            maxErrors = 10;
        }

        return new GameConfig(
                player,
                difficulty,
                totalIterations,
                maxTimeSeconds,
                inverseMode,
                dynamicDifficulty,
                sounds,
                eventProfile,
                maxErrors
        );
    }

    private void bindActions() {
        binding.btnPad0.setOnClickListener(v -> {
            playPadTone(0);
            viewModel.onOptionPressed(0);
        });
        binding.btnPad1.setOnClickListener(v -> {
            playPadTone(1);
            viewModel.onOptionPressed(1);
        });
        binding.btnPad2.setOnClickListener(v -> {
            playPadTone(2);
            viewModel.onOptionPressed(2);
        });
        binding.btnPad3.setOnClickListener(v -> {
            playPadTone(3);
            viewModel.onOptionPressed(3);
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
                        + " | Evento " + Math.max(state.currentRound, 1) + "/" + state.totalRounds
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
        } else {
            binding.tvTimer.setText("Tiempo restante: " + state.timeLeftSeconds + " s");
        }
        binding.tvMain.setText(state.mainText);
        binding.tvStimulus.setText(state.stimulusText);
        binding.tvStimulus.setTextColor(state.stimulusTextColor);
        renderColorMosaic(state.stimulusColorTiles);

        if (state.phase == GameUiState.Phase.COUNTDOWN) {
            binding.tvCountdown.setVisibility(View.VISIBLE);
            binding.tvCountdown.setText(String.valueOf(state.preStartCountdown));
            binding.tvHint.setText("Preparado para eventos variados");
        } else {
            binding.tvCountdown.setVisibility(View.GONE);
            binding.tvHint.setText(state.phase == GameUiState.Phase.USER_TURN
                    ? "Lee la consigna y elige una respuesta"
                    : "Espera el siguiente evento");
        }

        boolean enablePads = state.phase == GameUiState.Phase.USER_TURN;
        for (int i = 0; i < pads.length; i++) {
            Button pad = pads[i];
            String option = (state.options != null && i < state.options.length) ? state.options[i] : "";
            boolean visible = option != null && !option.isEmpty();
            pad.setVisibility(visible ? View.VISIBLE : View.INVISIBLE);
            if (visible) {
                pad.setText(option);
            }
            pad.setEnabled(enablePads);
            pad.setAlpha(enablePads ? 1f : 0.78f);
            pad.animate().scaleX(1f).scaleY(1f).setDuration(90L).start();
        }

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

    private void renderColorMosaic(int[] colors) {
        GridLayout grid = binding.gridColorMosaic;
        grid.removeAllViews();

        if (colors == null || colors.length == 0) {
            grid.setVisibility(View.GONE);
            return;
        }

        int tileSize = dpToPx(28);
        int margin = dpToPx(3);
        grid.setVisibility(View.VISIBLE);

        for (int color : colors) {
            View tile = new View(this);
            GridLayout.LayoutParams params = new GridLayout.LayoutParams();
            params.width = tileSize;
            params.height = tileSize;
            params.setMargins(margin, margin, margin, margin);
            tile.setLayoutParams(params);

            GradientDrawable bg = new GradientDrawable();
            int drawableRes = resolveColorToken(color);
            if (drawableRes != 0) {
                tile.setBackgroundResource(drawableRes);
            } else {
                bg.setShape(GradientDrawable.OVAL);
                bg.setColor(color);
                bg.setStroke(dpToPx(1), 0x66FFFFFF);
                tile.setBackground(bg);
            }

            grid.addView(tile);
        }
    }

    private int dpToPx(int dp) {
        float density = getResources().getDisplayMetrics().density;
        return (int) (dp * density);
    }

    private int resolveColorToken(int color) {
        if (color == Color.RED) {
            return R.drawable.token_red;
        }
        if (color == Color.rgb(76, 175, 80)) {
            return R.drawable.token_green;
        }
        if (color == Color.rgb(33, 150, 243)) {
            return R.drawable.token_blue;
        }
        if (color == Color.rgb(255, 193, 7)) {
            return R.drawable.token_yellow;
        }
        return 0;
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
        intent.putExtra("eventProfile", selectedEventProfile);
        intent.putExtra("maxErrors", selectedMaxErrors);
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
