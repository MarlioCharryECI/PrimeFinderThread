package edu.eci.arsw.primefinder;

import java.io.BufferedReader;
import java.io.InputStreamReader;

/**
 * Hilo controlador: arranca los trabajadores, cada T ms solicita pausa global,
 * muestra el conteo de primos y espera ENTER para reanudar.
 */
public class Control extends Thread {

    private static final int NTHREADS = 8;
    private static final int MAXVALUE = 900_000_000;
    private static final int TMILISECONDS = 5_000;

    private final int NDATA = MAXVALUE / NTHREADS;

    private final PrimeFinderThread[] pft;
    private final PauseMonitor pauseMonitor;

    private Control() {
        super();
        this.pft = new PrimeFinderThread[NTHREADS];
        this.pauseMonitor = new PauseMonitor(NTHREADS);

        int i;
        for (i = 0; i < NTHREADS - 1; i++) {
            pft[i] = new PrimeFinderThread(i * NDATA, (i + 1) * NDATA, pauseMonitor);
        }
        pft[i] = new PrimeFinderThread(i * NDATA, MAXVALUE + 1, pauseMonitor);
    }

    public static Control newControl() {
        return new Control();
    }

    @Override
    public void run() {
        for (int i = 0; i < NTHREADS; i++) {
            pft[i].start();
        }

        try (BufferedReader br = new BufferedReader(new InputStreamReader(System.in))) {
            while (pauseMonitor.getRemainingWorkers() > 0) {
                Thread.sleep(TMILISECONDS);

                pauseMonitor.requestGlobalPauseAndWaitAll();

                int totalPrimes = 0;
                for (PrimeFinderThread t : pft) {
                    if (t != null) {
                        totalPrimes += t.getPrimes().size();
                    }
                }

                System.out.println("============================================");
                System.out.println("Se han encontrado hasta ahora: " + totalPrimes + " números primos.");
                System.out.println("Presione ENTER para continuar...");
                System.out.println("============================================");
                br.readLine();

                pauseMonitor.resumeAll();
            }

            for (PrimeFinderThread t : pft) {
                if (t != null) t.join();
            }

            int totalFinal = 0;
            for (PrimeFinderThread t : pft) {
                if (t != null) totalFinal += t.getPrimes().size();
            }
            System.out.println(">>> Cómputo finalizado. Total de primos: " + totalFinal);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}