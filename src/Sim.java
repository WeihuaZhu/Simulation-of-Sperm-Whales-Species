/*
 * Created by Zeyu Chen 03/10/2018
 *
 * Main class
 */


import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class Sim {
    public static int barrier;
    public static void main(String[] args) {

        // Initialize
        barrier = 0;
        Lock barrierl = new ReentrantLock();

        MainProc mp = new MainProc(0, 85, 100, 1000);
        KillerWhales kw = new KillerWhales(5000, 200, 0.15, 0.2);
        SpermWhales sw = new SpermWhales(10000, 200, 0.2, 0.1);
        MarineMammals mm = new MarineMammals(20000, 200, 0.4, 0.1);

        System.out.println("==================================================================================");
        System.out.println("Ocean Current: Type" + mp.oceanCur + ", Ocean Temp: " + mp.oceanTemp + "F, Human Fish Rate: "
                + mp.fishRate + ", Food Unit: " + mp.foodRes);

        System.out.println("Killer Whales: " + kw.number + ", Food Demand: " + kw.demand + ", Reproduce rate: " +
                kw.reprorate + ", Death Rate: " + kw.deathrate);

        System.out.println("Sperm Whales: " + sw.number + ", Food Demand: " + sw.demand + ", Reproduce rate: " +
                sw.reprorate + ", Death Rate: " + sw.deathrate);

        System.out.println("Other marine mammals: " + mm.number + ", Food Demand: " + mm.demand + ", Reproduce rate: " +
                mm.reprorate + ", Death Rate: " + mm.deathrate);


        System.out.println("==================================================================================");
        System.out.println("Simulation starts");

        MainProcThread mpthr = new MainProcThread("Main Process Thread") {
            @Override public void run() {
                //System.out.println("Thread:" + threadName + " ID: " + Thread.currentThread().getId() + " starts.");
                // Barrier
                barrierl.lock();
                try {
                    ++barrier;
                } finally {
                    barrierl.unlock();
                }
                while (barrier != 4)
                    System.out.println("Main Process: is waiting.");
            }
        };

        KillerWhalesThread kwthr = new KillerWhalesThread("Killer Whales Thread") {
            @Override public void run() {
                //System.out.println("Thread:" + threadName + " ID: " + Thread.currentThread().getId() + " starts.");
                int count = 0;  // evemt up limit
                int huntcount =0;
                double now = 0.0;

                Engine kwengine = new Engine();
                Event hunt = new KillerWhalesHunt(now);

                // Season begin
                kwengine.eventList.add(hunt);
                ++count;
                while (!kwengine.eventList.isEmpty()) {
                    double temp = now;

                    kwengine.eventHandler(mp, kw, sw, mm);
                    // Use prob to determine whether to schedule
                    if (Math.random() > 0.1 && count < 500) {
                        now = Math.random()*1 +  temp;
                        Event huntTemp = new KillerWhalesHunt(now);
                        // schedule next event
                        kwengine.eventList.add(huntTemp);
                        ++count;
                    }
                    // Use prob to determine whether to schedule
                    if (Math.random() > 0.5 && count < 500) {
                        now = Math.random()*10 +  temp;
                        Event deathTemp = new KillerWhalesDeath(now);
                        // schedule next event
                        kwengine.eventList.add(deathTemp);
                        ++count;
                    }

                }
                // Barrier
                barrierl.lock();
                try {
                    ++barrier;
                } finally {
                    barrierl.unlock();
                }
                while (barrier != 4)
                    System.out.println("Killer Whales is waiting.");

                /* Season calculate */
                kw.numberl.lock();

                try {
                    int temp = kw.number;
                    kw.number = kw.number  +  (int) (temp*kw.reprorate);
                    kw.number = kw.number - (int) (temp*kw.deathrate);
                    System.out.println(kw.name + ": " +(int)(temp*kw.deathrate) + " dies, " + (int)(temp*kw.deathrate) +
                            " reproduces. " + "Remain killer whales:" + kw.number);

                } finally {
                    kw.numberl.unlock();
                }

                // Calculate death for hunger
                kw.numberl.lock();

                try {
                    if (kw.food < kw.demand) {
                        kw.number -= (int)((kw.demand - kw.food)*2.5);
                        System.out.println(kw.name + ": " + (int)((kw.demand - kw.food)*2.5) + " dies for hunger.");
                    }
                } finally {
                    kw.numberl.unlock();
                }

                /* Season calculate */

                // Season ends
            }
        };
        // EXAMPLE HERE
        SpermWhalesThread swthr = new SpermWhalesThread("Sperm Whales Thread") {
            @Override public void run() {
                //System.out.println("Thread:" + threadName + " ID: " + Thread.currentThread().getId() + " starts.");
                Engine swengine = new Engine();

                Event e = new SpermWhalesEat(0.0);

                swengine.eventList.add(e);
                while (!swengine.eventList.isEmpty()) {
                    swengine.eventHandler(mp, kw, sw, mm);
                    //kwengine.schedule(e);
                }

                // Barrier
                barrierl.lock();
                try {
                    ++barrier;
                } finally {
                    barrierl.unlock();
                }
                while (barrier != 4)
                   System.out.println("Sperm Whales: is waiting.");

            }
        };

        MarineMammalThread mmthr = new MarineMammalThread("Marine Mammals Thread") {
            @Override public void run() {
                //System.out.println("Thread" + threadName + ", ID: " + Thread.currentThread().getId() + " starts.");
                Engine mmengine = new Engine();
                // Food resource consume

                Event e = new MarineMammalsEat(0.0);
                mmengine.eventList.add(e);
                while (!mmengine.eventList.isEmpty()) {
                    mmengine.eventHandler(mp, kw, sw, mm);
                    //kwengine.schedule(e);
                }

                // Barrier
                barrierl.lock();
                try {
                    ++barrier;
                } finally {
                    barrierl.unlock();
                }
                while (barrier != 4)
                    System.out.println("Marine Mammals: is waiting.");
            }
        };

        mpthr.start();
        kwthr.start();
        swthr.start();
        mmthr.start();



    }
}


// Thread
class MainProcThread extends Thread{
    String threadName;

    public MainProcThread(String name) {
        threadName = name;
    }
}

class KillerWhalesThread extends Thread{
    String threadName;

    public KillerWhalesThread(String name) {
        threadName = name;
    }
}

class SpermWhalesThread extends Thread{
    String threadName;

    public SpermWhalesThread(String name) {
        threadName = name;
    }
}

class MarineMammalThread extends Thread{
    String threadName;

    public MarineMammalThread(String name) {
        threadName = name;
    }
}



