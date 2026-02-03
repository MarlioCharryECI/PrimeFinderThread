
package edu.eci.arsw.primefinder;

/**
 * Monitor para pausar y reanudar de forma coordinada a múltiples hilos trabajadores,
 */
public class PauseMonitor {

    private final Object monitor = new Object();

    private volatile boolean paused = false;
    private int generation = 0;
    private int pausedWorkers = 0;
    private int remainingWorkers;

    /**
     * Ticket por hilo para registrar si ya reportó su pausa en una generación dada.
     */
    public static final class Ticket {
        int lastSeenGeneration = -1;
    }

    public PauseMonitor(int totalWorkers) {
        this.remainingWorkers = totalWorkers;
    }

    public Ticket newTicket() {
        return new Ticket();
    }

    /**
     * Punto seguro de pausa: lo invoca el trabajador con frecuencia (p.ej., en cada iteración).
     * Si hay pausa, el hilo:
     *  - se cuenta una única vez para la generación actual,
     *  - espera hasta que Control invoque resumeAll().
     */
    public void pausePoint(Ticket t) throws InterruptedException {
        if (!paused) return;
        synchronized (monitor) {
            while (paused) {
                if (t.lastSeenGeneration != generation) {
                    t.lastSeenGeneration = generation;
                    pausedWorkers++;
                    monitor.notifyAll();
                }
                monitor.wait(); // Esperar a resumeAll()
            }
        }
    }

    /**
     * Debe llamarse una sola vez por trabajador al finalizar definitivamente.
     * Si la finalización ocurre durante una pausa y el hilo no ha sido contado,
     * se contabiliza para desbloquear a Control si corresponde.
     */
    public void onWorkerFinish(Ticket t) {
        synchronized (monitor) {
            if (paused && t.lastSeenGeneration != generation) {
                t.lastSeenGeneration = generation;
                pausedWorkers++;
            }
            remainingWorkers--;
            monitor.notifyAll();
        }
    }

    /**
     * Control solicita una pausa global y espera a que todos los trabajadores
     * activos (no finalizados) confirmen estar pausados.
     */
    public void requestGlobalPauseAndWaitAll() throws InterruptedException {
        synchronized (monitor) {
            paused = true;
            generation++;
            pausedWorkers = 0;

            while (pausedWorkers < remainingWorkers) {
                monitor.wait();
            }
        }
    }

    /**
     * Reanuda a todos los trabajadores pausados.
     */
    public void resumeAll() {
        synchronized (monitor) {
            paused = false;
            monitor.notifyAll();
        }
    }

    public int getRemainingWorkers() {
        synchronized (monitor) {
            return remainingWorkers;
        }
    }
}
