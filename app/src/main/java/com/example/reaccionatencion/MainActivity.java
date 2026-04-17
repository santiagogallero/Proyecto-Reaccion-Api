package com.example.reaccionatencion;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.reaccionatencion.databinding.ActivityMainBinding;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setupDifficultySpinner();
        setupEventProfileSpinner();
        setupDefaults();
        showBestScore();

        binding.btnStart.setOnClickListener(v -> startGame());
    }

    @Override
    protected void onResume() {
        super.onResume();
        showBestScore();
    }

    private void setupDifficultySpinner() {
        String[] difficulties = {"Entrenamiento", "Facil", "Medio", "Dificil"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                difficulties
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.spinnerDifficulty.setAdapter(adapter);
    }

    private void setupEventProfileSpinner() {
        String[] profiles = {"Mixto", "Numeros", "Colores", "Stroop"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                profiles
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.spinnerEventProfile.setAdapter(adapter);
    }

    private void setupDefaults() {
        binding.etPlayerName.setText("Jugador");
        binding.etIterations.setText("20");
        binding.etCustomTime.setText("");
        binding.etMaxErrors.setText("3");
        binding.spinnerEventProfile.setSelection(0);
        binding.swInverseMode.setChecked(true);
        binding.swDynamicDifficulty.setChecked(false);
        binding.swSound.setChecked(true);
    }

    private void showBestScore() {
        ScoreStorage storage = new ScoreStorage(this);
        String player = binding.etPlayerName.getText().toString().trim();
        if (player.isEmpty()) {
            player = "Jugador";
        }
        String best = storage.getGlobalBestSummary() + "\n" + storage.getPlayerBestSummary(player);
        binding.tvBestScore.setText(best);
    }

    private void startGame() {
        String player = binding.etPlayerName.getText().toString().trim();
        if (player.isEmpty()) {
            player = "Jugador";
        }

        int iterations;
        try {
            iterations = Integer.parseInt(binding.etIterations.getText().toString().trim());
        } catch (NumberFormatException e) {
            iterations = 20;
        }
        if (iterations <= 0) {
            iterations = 20;
        }

        int maxErrors;
        try {
            maxErrors = Integer.parseInt(binding.etMaxErrors.getText().toString().trim());
        } catch (NumberFormatException e) {
            maxErrors = 3;
        }
        if (maxErrors < 1) {
            maxErrors = 1;
        }
        if (maxErrors > 10) {
            maxErrors = 10;
        }

        String difficulty = binding.spinnerDifficulty.getSelectedItem().toString();
        String eventProfile = binding.spinnerEventProfile.getSelectedItem().toString();
        int defaultTime = DifficultyConfig.defaultMaxTimeFor(difficulty);

        int maxTime = defaultTime;
        String customTimeText = binding.etCustomTime.getText().toString().trim();
        if (!customTimeText.isEmpty()) {
            try {
                maxTime = Integer.parseInt(customTimeText);
            } catch (NumberFormatException ignored) {
                maxTime = defaultTime;
            }
        }

        if (maxTime > 30) {
            maxTime = 30;
            Toast.makeText(this, "El tiempo maximo no puede superar 30 segundos", Toast.LENGTH_SHORT).show();
        }
        if (maxTime < 2) {
            maxTime = 2;
        }

        Intent intent = new Intent(this, GameActivity.class);
        intent.putExtra(GameActivity.EXTRA_PLAYER, player);
        intent.putExtra(GameActivity.EXTRA_DIFFICULTY, difficulty);
        intent.putExtra(GameActivity.EXTRA_ITERATIONS, iterations);
        intent.putExtra(GameActivity.EXTRA_MAX_TIME_SECONDS, maxTime);
        intent.putExtra(GameActivity.EXTRA_INVERSE_MODE, binding.swInverseMode.isChecked());
        intent.putExtra(GameActivity.EXTRA_DYNAMIC_DIFFICULTY, binding.swDynamicDifficulty.isChecked());
        intent.putExtra(GameActivity.EXTRA_SOUND_ENABLED, binding.swSound.isChecked());
        intent.putExtra(GameActivity.EXTRA_EVENT_PROFILE, eventProfile);
        intent.putExtra(GameActivity.EXTRA_MAX_ERRORS, maxErrors);
        startActivity(intent);
    }
}
