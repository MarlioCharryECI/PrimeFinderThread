package edu.eci.arsw.primefinder;

import java.util.LinkedList;
import java.util.List;

/**
 * Hilo trabajador que busca primos en [a, b).
 * Incorpora puntos seguros de pausa controlados por PauseMonitor.
 */
public class PrimeFinderThread extends Thread {

    private final int a, b;
    private final List<Integer> primes;

    private final PauseMonitor pauseMonitor;
    private final PauseMonitor.Ticket ticket;

    public PrimeFinderThread(int a, int b, PauseMonitor pauseMonitor) {
        super();
        this.primes = new LinkedList<>();
        this.a = a;
        this.b = b;
        this.pauseMonitor = pauseMonitor;
        this.ticket = pauseMonitor.newTicket();
    }

    @Override
    public void run() {
        try {
            for (int i = a; i < b; i++) {
                pauseMonitor.pausePoint(ticket);

                if (isPrime(i)) {
                    primes.add(i);
                    //System.out.println(i);
                }
            }
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        } finally {
            pauseMonitor.onWorkerFinish(ticket);
        }
    }

    boolean isPrime(int n) {
        if (n > 2) {
            if (n % 2 == 0) return false;
            for (int i = 3; i * i <= n; i += 2) {
                if (n % i == 0) return false;
            }
            return true;
        } else {
            return n == 2;
        }
    }

    public List<Integer> getPrimes() {
        return primes;
    }
}