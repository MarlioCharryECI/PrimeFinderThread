# PrimeFinder — Laboratorio de Concurrencia con `wait/notifyAll`

Este proyecto implementa un sistema multi‑hilo en Java para el cálculo concurrente de números primos dentro de un rango, incorporando un mecanismo de **pausa global**, **reanudación** y **monitoreo de progreso**, todo **sin usar espera activa (busy‑waiting)**.

El objetivo es practicar el uso correcto de:

- `synchronized`
- `wait()` / `notifyAll()`
- Modelo de monitores en Java
- Coordinación de múltiples hilos trabajadores

---

## Características

- **Búsqueda paralela de primos:** divide \[0, MAXVALUE] entre `NTHREADS` hilos (`PrimeFinderThread`).
- **Pausa global coordinada:** cada `TMILISECONDS` el controlador:
    1. Solicita **pausa**.
    2. Espera a que **todos los hilos activos** estén pausados.
    3. **Muestra progreso** (conteo de primos encontrados).
    4. **Espera ENTER**.
    5. **Reanuda** ejecución.
- **Sin busy‑wait:** solo `wait()`/`notifyAll()` con un **único monitor**.
- **Sin “lost wakeups”:** usa una **generación (epoch)** por pausa para contar cada hilo una sola vez.
- **Finalización segura:** los hilos avisan al monitor al terminar, evitando esperas indefinidas.

---

**Clases:**
- `Main`: arranca el `Control` y espera su finalización.
- `Control`: coordina pausas periódicas, muestra progreso, espera ENTER y reanuda.
- `PrimeFinderThread`: busca primos en su subrango y reporta al monitor.
- `PauseMonitor`: monitor con `synchronized` + `wait()` + `notifyAll()`; maneja pausa global, barrera y finalización.

---

## Parámetros

En `Control.java` puedes ajustar:

```java
private static final int NTHREADS = 3;           // # de hilos trabajadores
private static final int MAXVALUE = 30_000_000;  // Límite superior del rango
private static final int TMILISECONDS = 5_000;   // Intervalo entre pausas (ms)
```

---

## Diseño de sincronización

**Monitor único (PauseMonitor):**

- paused (boolean/volatile): indica si hay pausa activa.
- generation (int): etiqueta la ronda de pausa para evitar doble conteo.
- pausedWorkers (int): cuántos hilos confirmaron la pausa actual.
- remainingWorkers (int): cuántos hilos siguen vivos.

**Ticket por hilo:**

- Cada PrimeFinderThread tiene un Ticket con lastSeenGeneration para contarse una sola vez por pausa.

**Puntos clave:**

- Punto seguro de pausa: cada iteración (o cada N iteraciones) invoca pausePoint(ticket).

    - Si no hay pausa → fast‑path (retorna sin bloquear).
    - Si hay pausa → entra a wait() en un while (paused).


- Barrera en el controlador: requestGlobalPauseAndWaitAll() pone paused=true, incrementa generation, reinicia pausedWorkers y espera con while (pausedWorkers < remainingWorkers) wait();.
- Reanudación: resumeAll() pone paused=false y hace notifyAll().
- Finalización de hilos: en finally, cada hilo llama onWorkerFinish(ticket) para:

    - Decrementar remainingWorkers.
    - Notificar al controlador (notifyAll()).
    - Si estaba en pausa y no se había contado, contarse en esa generación.