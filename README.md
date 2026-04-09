# Desafio de Reaccion y Atencion

Aplicacion Android nativa en Java con jugabilidad estilo Simon para evaluar memoria, atencion y reaccion.

## Funcionalidades implementadas

- Modos: Entrenamiento, Facil, Medio y Dificil.
- Configuracion de iteraciones por partida (por defecto 20).
- Configuracion de tiempo maximo por ronda con tope de 30 segundos.
- Secuencia Simon con pads de colores y repeticiones crecientes.
- Calculo de tiempo de reaccion por nivel y promedio final.
- Puntaje por dificultad (entrenamiento no suma puntos).
- Estadisticas finales: correctas, incorrectas, rondas jugadas, promedio de reaccion y puntos.
- Persistencia local con SharedPreferences para mejor puntaje global y mejor puntaje por jugador.
- Posibilidad de reiniciar partida al perder o al completar rondas.
- Condicion de derrota por errores acumulados para habilitar reinicio tras perder.

## Agregados opcionales implementados

- Incremento dinamico de dificultad (reduce tiempo maximo cada 5 aciertos, con limite inferior).
- Sonidos simples de acierto/error mediante ToneGenerator (sin dependencias externas).
- Cuenta regresiva inicial (3, 2, 1) antes de comenzar la partida.
- Feedback visual inmediato por ronda (flash verde/rojo).
- Deteccion de toque en toda la pantalla de juego (no solo boton).
- Intervalo de cambio de estimulos variable por ronda (random dentro del tiempo configurado).
- Modo secuencia inversa tipo Simon Rewind: repetir la secuencia al reves.

## Arquitectura

- Estado, secuencia y reglas de juego centralizadas en `GameViewModel`.
- `GameActivity` enfocada en renderizar UI y animaciones.
- Modelo de estado simple con `GameUiState` para facilitar mantenimiento.

## Estructura principal

- MainActivity: configuracion inicial de partida.
- GameActivity: logica principal del juego y rondas.
- ResultActivity: resumen final y reinicio.
- ScoreStorage: persistencia local de records.

## Como ejecutar en Android Studio

1. Abrir la carpeta del proyecto en Android Studio.
2. Esperar sincronizacion de Gradle.
3. Ejecutar en emulador o dispositivo fisico (Android 7+).

## Restricciones de la consigna

- Sin conexion a internet.
- Persistencia local.
- Desarrollo individual.

## Nota academica

El codigo prioriza claridad y simplicidad para facilitar defensa y mantenimiento.
