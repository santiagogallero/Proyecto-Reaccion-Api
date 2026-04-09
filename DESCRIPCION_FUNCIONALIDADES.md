# Descripcion de funcionalidades implementadas

## Objetivo

Se desarrollo una aplicacion Android nativa en Java con jugabilidad estilo Simon que evalua memoria, atencion y reaccion del jugador dentro de un tiempo limite por nivel.

## Pantallas

### 1) Pantalla de inicio (MainActivity)

- Entrada de nombre del jugador.
- Seleccion de dificultad: Entrenamiento, Facil, Medio, Dificil.
- Configuracion de cantidad de niveles o iteraciones (default 20).
- Configuracion de tiempo maximo por nivel:
  - Facil: 20 segundos por defecto.
  - Medio: 15 segundos por defecto.
  - Dificil: 10 segundos por defecto.
  - Tope maximo absoluto: 30 segundos.
- Activacion opcional de:
  - Modo secuencia inversa tipo Simon Rewind.
  - Incremento dinamico de dificultad.
  - Sonidos.
- Visualizacion de mejor puntaje global y mejor puntaje del jugador actual.

### 2) Pantalla de juego (GameActivity)

- Secuencia de colores estilo Simon que crece en cada nivel.
- Temporizador por nivel.
- Botones de colores grandes para repetir la secuencia.
- Registro de:
  - Aciertos.
  - Errores.
  - Tiempo de reaccion.
  - Puntaje acumulado.
- Condicion de derrota por errores acumulados.
- Condicion de victoria al completar todas las rondas.
- Animaciones simples de resaltado y feedback visual.

### 3) Pantalla de resultados (ResultActivity)

- Muestra:
  - Jugador.
  - Dificultad.
  - Si se uso modo secuencia inversa y dificultad dinamica.
  - Puntos finales.
  - Cantidad de correctas/incorrectas.
  - Rondas jugadas.
  - Tiempo promedio de reaccion.
  - Motivo de finalizacion.
- Muestra records guardados localmente.
- Boton para volver a iniciar partida.

## Como se implemento tecnicamente

### Arquitectura simple y mantenible

Se uso una estructura clara para que el proyecto sea facil de entender y defender.

- MainActivity: prepara configuracion de la partida.
- GameViewModel: contiene la secuencia, reglas, estados y logica del juego.
- GameActivity: solo renderiza la UI, anima los pads y navega a resultados.
- ResultActivity: resume resultados y permite reinicio.

### Logica de dificultad y puntaje

- DifficultyConfig define tiempos por defecto y multiplicadores por dificultad.
- En modo entrenamiento no se suman puntos.
- En facil/medio/dificil los puntos aumentan segun rapidez, multiplicador y largo de la secuencia.

### Persistencia local

- Se uso SharedPreferences para almacenar:
  - Mejor puntaje global.
  - Mejor puntaje por jugador.
- No se usa internet ni base remota.

### Modo secuencia inversa

Cuando esta activado, la secuencia debe repetirse al reves.

### Incremento dinamico de dificultad

Si esta activo, el tiempo maximo por nivel disminuye automaticamente y la reproduccion de la secuencia se acelera de forma progresiva, sin bajar de un minimo seguro.

### Sonidos

Se incorporaron sonidos simples de acierto/error usando ToneGenerator del SDK Android, evitando dependencias externas y archivos multimedia.

### UI y contraste

- Fondo oscuro en todo el juego.
- Pads de colores brillantes para alto contraste.
- Tipografia grande y centrada.
- Animaciones simples de escala y transicion.

## Restricciones cumplidas

- Aplicacion individual.
- Sin conexion a internet.
- Persistencia local.
- Codigo Java simple y legible.
