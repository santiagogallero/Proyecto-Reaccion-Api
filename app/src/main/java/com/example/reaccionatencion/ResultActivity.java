package com.example.reaccionatencion;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.example.reaccionatencion.databinding.ActivityResultBinding;

public class ResultActivity extends AppCompatActivity {

    private ActivityResultBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityResultBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        Intent i = getIntent();
        String player = i.getStringExtra("player");
        String difficulty = i.getStringExtra("difficulty");
        boolean inverseMode = i.getBooleanExtra("inverseMode", false);
        boolean dynamicDifficulty = i.getBooleanExtra("dynamicDifficulty", false);
        int points = i.getIntExtra("points", 0);
        int correct = i.getIntExtra("correct", 0);
        int incorrect = i.getIntExtra("incorrect", 0);
        long avgMs = i.getLongExtra("avgMs", 0);
        int totalIterations = i.getIntExtra("totalIterations", 0);
        int maxTimeStart = i.getIntExtra("maxTimeStart", 0);
        int maxTimeEnd = i.getIntExtra("maxTimeEnd", 0);
        String reason = i.getStringExtra("reason");
        boolean completed = i.getBooleanExtra("completedAllRounds", false);

        if (player == null) {
            player = "Jugador";
        }
        if (difficulty == null) {
            difficulty = "Facil";
        }

        String title = completed ? "Ganaste la partida" : "Partida finalizada";
        binding.tvTitle.setText(title);

        if (reason == null) {
            reason = "Sin detalle";
        }

        String stats = "Jugador: " + player + "\n"
                + "Dificultad: " + difficulty + "\n"
            + "Modo inversa: " + (inverseMode ? "Si" : "No") + "\n"
            + "Dificultad dinamica: " + (dynamicDifficulty ? "Si" : "No") + "\n"
                + "Puntos: " + points + "\n"
                + "Correctas: " + correct + "\n"
                + "Incorrectas: " + incorrect + "\n"
            + "Rondas jugadas: " + (correct + incorrect) + "/" + totalIterations + "\n"
            + "Tiempo maximo inicial/final: " + maxTimeStart + "s / " + maxTimeEnd + "s\n"
            + "Promedio de reaccion: " + avgMs + " ms\n"
            + "Resultado: " + reason;
        binding.tvStats.setText(stats);

        ScoreStorage storage = new ScoreStorage(this);
        binding.tvBest.setText(storage.getGlobalBestSummary() + "\n" + storage.getPlayerBestSummary(player));

        binding.btnPlayAgain.setOnClickListener(v -> {
            Intent restart = new Intent(this, MainActivity.class);
            restart.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(restart);
            finish();
        });

        binding.btnClose.setOnClickListener(v -> finish());
    }
}
