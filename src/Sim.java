/*
 * Created by Zeyu Chen 03/10/2018
 *
 * Main class
 */


import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.io.File;  
import java.io.BufferedReader;  
import java.io.FileReader; 
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

@SuppressWarnings("Duplicates") // No more duplicated warning OK?
public class Sim {
    private static CyclicBarrier startBarrier = new CyclicBarrier(4);
    private static CyclicBarrier endBarrier = new CyclicBarrier(4);


    public static double timeLimit = 36000;  // The unit is day. 360 is onw year, 90 is one season, change it to control the time limit.
    public static int fisheryLevel = 0; // 0 for no fishing, 5 for fishing every season
    public static String fileName;
    public static boolean dataFlag = false;

    public static void main(String[] args) {
        Map<Integer, Integer> allData = new HashMap<>();

        if (args.length  == 0) {
            System.out.println("Running by default 3600 days, 40 seasons.");
        }
        else if (args.length == 2) {
            // System.out.println("Running by " + args[0] + " days, " + (Integer.parseInt(args[0])/90 + 1) + " seasons.");
            // timeLimit = Integer.parseInt(args[0]);
            fileName = args[0];
            fisheryLevel = Integer.parseInt(args[1]);
            dataFlag = true;
            FileOutputStream fileLog = null;
            try {
                fileLog = new FileOutputStream("fileLog.txt");
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
            PrintStream filePrint = new PrintStream(fileLog);
            int countYear = 0;
            File file = new File(fileName);
            BufferedReader br = null;
            try
            {
                br = new BufferedReader(new FileReader(file));
            } catch (FileNotFoundException e)
            {
                filePrint.println("File not found");
            }
            String line = "";
            try {
                while ((line = br.readLine()) != null)
                {
                    countYear++;
                    String[] linedata = line.split(",");

                    int dataYear = Integer.parseInt(linedata[0]);
                    int dataNum = Integer.parseInt(linedata[1]);
                    allData.put(dataYear, dataNum);
                    filePrint.println("At year: " + dataYear + " hunt " + dataNum + " sperm whales");
                }
            } catch (IOException e)
            {
                filePrint.println("File not found");
            }
            filePrint.println("Total year count " + countYear);
            timeLimit = countYear * 360;
        }
        else {
            System.out.println("---------Invalid Input-------------");
            System.out.println("Please input the correct number of days.");
            System.out.println("To use default parameter, run: java Sim");
            System.out.println("To use yearly data, run: java Sim + Filename + fisheryLevel");

            return;
        }

        MainProc mp = new MainProc(0, 85, 27000000);
        KillerWhales kw = new KillerWhales(3000, 350, 0.03, 0.01);
        SpermWhales sw = new SpermWhales(10000, 10000, 0.08, 0.002);
        MarineMammals mm = new MarineMammals(20000, 20000, 0.09, 0.018);

        System.out.println("Ocean Current: Type" + mp.oceanCur + ", Ocean Temp: " + mp.oceanTemp + "F, Total Food: "
                + mp.totalFood + ", Food Resource: " + mp.foodRes);

        System.out.println("Killer Whales: " + kw.number + ", Food Demand: " + kw.demand + ", Reproduce rate: " +
                kw.reprorate + ", Death Rate: " + kw.deathrate);

        System.out.println("Sperm Whales: " + sw.number + ", Food Demand: " + sw.demand + ", Reproduce rate: " +
                sw.reprorate + ", Death Rate: " + sw.deathrate);

        System.out.println("Other marine mammals: " + mm.number + ", Food Demand: " + mm.demand + ", Reproduce rate: " +
                mm.reprorate + ", Death Rate: " + mm.deathrate);


        System.out.println("Simulation starts");

        MainProcThread mpthr = new MainProcThread("Main Process Thread") {
            @Override public void run() {
                //System.out.println("Thread:" + threadName + " ID: " + Thread.currentThread().getId() + " starts.");
                double timeHelper = 0.0;

                FileOutputStream mainProcLog = null;
                try {
                    mainProcLog = new FileOutputStream("mainProcLog.txt");
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }
                PrintStream mainProcPrint = new PrintStream(mainProcLog);

                double now = 0.0;
                Engine mpengine = new Engine();
                Event season = new seasonChange(now);
                mpengine.eventList.add(season);


                while(timeHelper < timeLimit) {
                	now = timeHelper;
                    // mp.foodRes = 2700000;
                    // Start Barrier
                    /************************************/
                    mp.foodResl.lock();
                    while (now <= timeHelper) {
                        double temp = now;

                        // Under construction
                        while (!mpengine.eventList.isEmpty()) {
                            mpengine.eventHandler(mp, kw, sw, mm);
                        }
                        try {
                            System.out.println("Main Process: Start food Unit: " + mp.foodRes);

                            mainProcPrint.println("Main Process: Start food Unit: " + mp.foodRes);

                            mainProcPrint.println("Main Process: Start Total food Unit: " + mp.totalFood);

                            System.out.println("Main Process: Start Ocean Temperature: " + mp.oceanTemp);

                            mainProcPrint.println("Main Process: Start Ocean Temperature: " + mp.oceanTemp);


                        } finally {
                            mp.foodResl.unlock();
                        }

                        Event season1 = new seasonChange(temp + 90);
                        Event food = new foodGrow(temp + 90);
                        mpengine.eventList.add(season1);
                        mpengine.eventList.add(food);
                        mainProcPrint.println(threadName + ": Food grow at " + now);

                        if (Math.random() < 0.01) {
                            Event disaster = new naturalDisaster(temp + 90);
                            mpengine.eventList.add(disaster);
                            mainProcPrint.println(threadName + ": Natural disaster at " + now);

                        }
                        if (temp % 360 == 0) {
                            if (dataFlag) {
                                sw.numberl.lock();
                                try {
                                    int thisyear = ((int)(temp/360)) + 1910;
                                    int huntNumByYear = allData.get(thisyear)/50;
                                    sw.number -= huntNumByYear;
                                    System.out.println(threadName + ": at Year" + thisyear +", hunt " + huntNumByYear + " Sperm whales.");
                                    mainProcPrint.println(threadName + ": at Year" + thisyear +", hunt " + huntNumByYear + " Sperm whales.");
                                } finally {
                                    sw.numberl.unlock();
                                }
                            } else {
                                Event humanHunt = new humanHunt(temp + 90);
                                mpengine.eventList.add(humanHunt);
                                mainProcPrint.println(threadName + ": Human hunt event at " + now);
                            }
                        }

                        if (Math.random() < 0.2 * fisheryLevel) {
                            Event humanFish = new humanFish(temp + 90);
                            mpengine.eventList.add(humanFish);
                            mainProcPrint.println(threadName + ": Human fish event at " + now);
                        }
                        now = temp + 90;
                    }
                    try{
                        startBarrier.await();
                    } catch (Exception ex) {
                        Thread.currentThread().interrupt();
                    }

                    System.out.println(threadName + "Season: " + (int)(timeHelper/90) + " begins");
                    mainProcPrint.println(threadName + "Season: " + (int)(timeHelper/90) + " begins");

                    // Season begins
                    /**************************************** Season Begins *******************************************/
                    /*
                     * Main Process is special, the food resource refresh and event schedule is done before the season
                     * starts.
                     */
                    /**************************************** Season Ends *********************************************/
                    try{
                        endBarrier.await();
                    } catch (Exception ex) {
                        Thread.currentThread().interrupt();
                    }
                    System.out.println(threadName + "Season: " + (int)(timeHelper/90) + " ends");
                    mainProcPrint.println(threadName + "Season: " + (int)(timeHelper/90) + " ends");
                    /*************************************** Season Checkout ******************************************/
                    mp.foodResl.lock();
                    try {
                        System.out.println("Main Process: End food Unit: " + mp.foodRes);
                        mainProcPrint.println("Main Process: End food Unit: " + mp.foodRes);
                        mainProcPrint.println("Main Process: End Total food Unit: " + mp.totalFood);
                        System.out.println("Main Process: End Ocean Temperature: " + mp.oceanTemp);
                        mainProcPrint.println("Main Process: End Ocean Temperature: " + mp.oceanTemp);
                    } finally {
                        mp.foodResl.unlock();
                    }
                    timeHelper += 90;
                    mainProcPrint.println("====================================================================================================");
                    /*************************************** Season Complete ******************************************/
                }
                // Close file
                mainProcPrint.close();
            }
        };

        KillerWhalesThread kwthr = new KillerWhalesThread("Killer Whales Thread") {
            @Override public void run() {
                // Killer Whales
                double timeHelper = 0.0;
                double reprorate;
                int K = 3500;

                FileOutputStream killerWhaleLog = null;
                try {
                    killerWhaleLog = new FileOutputStream("killerWhaleLog.txt");
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }
                PrintStream killerWhalePrint = new PrintStream(killerWhaleLog);

                while(timeHelper < timeLimit) {
                    double now = 0.0;
                    int eatcounter = 1;
                    int deathcounter = 0;

                    Engine kwengine = new Engine();
                    Event hunt = new KillerWhalesHunt(now);
                    kwengine.eventList.add(hunt);

                    try{
                        startBarrier.await();
                    } catch (Exception ex) {
                        Thread.currentThread().interrupt();
                    }

                    System.out.println(threadName + "Season: " + (int)(timeHelper/90) + " begins");
                    killerWhalePrint.println(threadName + "Season: " + (int)(timeHelper/90) + " begins");
                    /**************************************** Season Begins *******************************************/
                    kw.food = 0.0;
                    kw.demand = kw.number*0.06;

                    while (!kwengine.eventList.isEmpty()) {
                        double temp = now;
                        kwengine.eventHandler(mp, kw, sw, mm);
                        // Use prob to determine whether to schedule
                        if (now < 90) {
                            now = Math.random()*15 +  temp;
                            Event huntTemp = new KillerWhalesHunt(now);
                            ++eatcounter;
                            // schedule next event
                            kwengine.eventList.add(huntTemp);
                        }
                        // Use prob to determine whether to schedule
                        if (Math.random() > 0.95 && now < 90) {
                            now = Math.random()*2 +  temp;
                            Event deathTemp = new KillerWhalesDeath(now);
                            ++deathcounter;
                            // schedule next event
                            kwengine.eventList.add(deathTemp);
                        }

                    }

                    /**************************************** Season Ends *********************************************/
                    try{
                        endBarrier.await();
                    } catch (Exception ex) {
                        Thread.currentThread().interrupt();
                    }
                    System.out.println(threadName + "Season: " + (int)(timeHelper/90) + " ends");
                    killerWhalePrint.println(threadName + "Season: " + (int)(timeHelper/90) + " ends");
                    /*************************************** Season Checkout ******************************************/

                    kw.numberl.lock();
                    reprorate = kw.reprorate*(1 - kw.number/K);
                    try {
                        int temp = kw.number;
                        kw.number = temp  +  (int) (temp*reprorate);
                        kw.number = kw.number - (int) (temp*kw.deathrate);
                        System.out.println(kw.name + ": " +(int)(temp*kw.deathrate) + " dies, " + (int)(temp*kw.reprorate) + " reproduces.");

                        killerWhalePrint.println(kw.name + ": " +(int)(temp*kw.deathrate) + " dies, " + (int)(temp*kw.reprorate) + " reproduces.");

                        if (kw.food < kw.demand) {
//                            kw.reprorate = 0.02*((kw.demand - kw.food)/kw.demand);
                            int kwDie = (int)((kw.demand - kw.food)/kw.demand*kw.number);
                            kw.number -= kwDie;
                            if (kw.number<0){
                                kw.number = 0;

                            }
                            System.out.println(kw.name + ": " + kwDie + " dies for hunger.");

                            killerWhalePrint.println(kw.name + ": " + kwDie + " dies for hunger.");
                        }
                        if (kw.number<0)
                            kw.number = 0;
                    } finally {
                        kw.numberl.unlock();
                    }

                    // Calculate natural death and reproduce
                    killerWhalePrint.println(threadName + " eat: " + eatcounter);
                    killerWhalePrint.println(threadName + " accidental death: " + deathcounter);
                    killerWhalePrint.println(threadName + " total demands: " + kw.demand);
                    killerWhalePrint.println(threadName + " total consumes: " + kw.food);

                    killerWhalePrint.println("Remain killer whales:" + kw.number);
                    timeHelper += 90;
                    killerWhalePrint.println("====================================================================================================");
                    /*************************************** Season Complete ******************************************/
                }
                killerWhalePrint.close();
            }
        };
        // EXAMPLE HERE
        SpermWhalesThread swthr = new SpermWhalesThread("Sperm Whales Thread") {
            @Override public void run() {
                // Sperm Whales
                double timeHelper = 0.0;
                double reprorate;
                int K = 20000;
                FileOutputStream spermWhaleLog = null;

                try {
                    spermWhaleLog = new FileOutputStream("spermWhaleLog.txt");
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }
                PrintStream spermWhalePrint = new PrintStream(spermWhaleLog);

                while(timeHelper < timeLimit) {
                    double now = 0.0; // now is in the season scope
                    int eatcounter = 0;
                    int deathcounter = 0;

                    Engine swengine = new Engine();

                    Event e = new SpermWhalesEat(0.0);
                    swengine.eventList.add(e);

                    try{
                        startBarrier.await();
                    } catch (Exception ex) {
                        Thread.currentThread().interrupt();
                    }
                    System.out.println(threadName + "Season: " + (int)(timeHelper/90) + " begins");
                    spermWhalePrint.println(threadName + "Season: " + (int)(timeHelper/90) + " begins");
                    /**************************************** Season Begins *******************************************/
                    sw.food = 0.0;
                    sw.demand = sw.number*70;
                    while (!swengine.eventList.isEmpty()) {
                        double temp =now;
                        swengine.eventHandler(mp, kw, sw, mm);

                        if (Math.random() > 0.95 && now < 90) {
                            now = Math.random()*0.5 + temp;
                            Event deathTemp = new SpermWhalesDeath(now);
                            deathcounter++;
                            swengine.eventList.add(deathTemp);
                        }
                        if (now < 90) {
                            now = Math.random()*2 + temp;
                            Event eat = new SpermWhalesEat(now);

                            if (sw.food < sw.demand*1.02) {
                                eatcounter++;
                                swengine.eventList.add(eat);
                            }
                        }
                    }
                    /**************************************** Season Ends *********************************************/
                    try{
                        endBarrier.await();
                    } catch (Exception ex) {
                        Thread.currentThread().interrupt();
                    }

                    System.out.println(threadName + "Season: " + (int)(timeHelper/90) + " ends");
                    spermWhalePrint.println(threadName + "Season: " + (int)(timeHelper/90) + " ends");
                    /*************************************** Season Checkout ******************************************/
                    sw.numberl.lock();
                    // Adjust reproduce rate;
                    reprorate = sw.reprorate*(1 - sw.number/K);
                    try {
                        int temp = sw.number;
                        sw.number = temp  +  (int) (temp*reprorate);
                        sw.number = sw.number - (int) (temp*sw.deathrate);
                        System.out.println(sw.name + ": " +(int)(temp*sw.deathrate) + " dies, " + (int)(temp*reprorate) + " reproduces.");
                        spermWhalePrint.println(sw.name + ": " +(int)(temp*sw.deathrate) + " dies, " + (int)(temp*reprorate) + " reproduces.");

                        // Calculate death for hunger
                        if (sw.food < sw.demand ) {
                            int swDie = (int)((sw.demand - sw.food)/sw.demand*sw.number);
                            sw.number -= swDie;
                            if (sw.number <= 0)
                                sw.number = 0;
                            System.out.println(sw.name + ": " + swDie + " dies for hunger.");
                            spermWhalePrint.println(sw.name + ": " + swDie + " dies for hunger.");
                        }
                        if (sw.number <= 0)
                            sw.number = 0;

                    } finally {
                        sw.numberl.unlock();
                    }
                    // Calculate natural death and reproduce
                    spermWhalePrint.println(threadName + " eat: " + eatcounter);
                    spermWhalePrint.println(threadName + " accidental death: " + deathcounter);
                    spermWhalePrint.println(threadName +" total demands: " + sw.demand);
                    spermWhalePrint.println(threadName + " total consumes: " + sw.food);
                    spermWhalePrint.println(threadName + " num:" + sw.number);
                    timeHelper += 90;
                    spermWhalePrint.println("====================================================================================================");
                    /*************************************** Season Complete ******************************************/
                }
                spermWhalePrint.close();
            }
        };

        MarineMammalThread mmthr = new MarineMammalThread("Marine Mammals Thread") {
            @Override public void run() {
                double timeHelper = 0.0;
                double reprorate;

                int K = 20000;

                FileOutputStream marineMammalLog = null;
                try {
                    marineMammalLog = new FileOutputStream("marineMammalLog.txt");
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }
                PrintStream marineMammalPrint = new PrintStream(marineMammalLog);

                while(timeHelper < timeLimit) {
                    double now = 0.0;
                    int eatcounter = 1;
                    int deathcounter = 0;
                    Engine mmengine = new Engine();
                    // Food resource consume

                    Event e = new MarineMammalsEat(0.0);
                    mmengine.eventList.add(e);

                    try{
                        startBarrier.await();
                    } catch (Exception ex) {
                        Thread.currentThread().interrupt();
                    }
                    System.out.println(threadName + "Season: " + (int)(timeHelper/90) + " begins");
                    marineMammalPrint.println(threadName + "Season: " + (int)(timeHelper/90) + " begins");
                    /**************************************** Season Begins *******************************************/
                    mm.food = 0.0;
                    mm.demand = mm.number*70;
                    while (!mmengine.eventList.isEmpty()) {
                        double temp = now;
                        mmengine.eventHandler(mp, kw, sw, mm);

                        if (now < 90) {
                            now = Math.random()*2 + temp;
                            Event eatTemp = new MarineMammalsEat(now);

                            if (mm.food < mm.demand*1.05) {
                                ++eatcounter;
                                mmengine.eventList.add(eatTemp);
                            }
                        }
                        if (Math.random() > 0.95 && now < 90) {
                            now = Math.random()*2 +  temp;
                            Event deathTemp = new MarineMammalsDeath(now);
                            ++deathcounter;
                            mmengine.eventList.add(deathTemp);
                        }
                    }
                    /**************************************** Season Ends *********************************************/
                    try{
                        endBarrier.await();
                    } catch (Exception ex) {
                        Thread.currentThread().interrupt();
                    }

                    System.out.println(threadName + "Season: " + (int)(timeHelper/90) + " ends");
                    marineMammalPrint.println(threadName + "Season: " + (int)(timeHelper/90) + " ends");
                    /*************************************** Season Checkout ******************************************/
                    mm.numberl.lock();

                    reprorate = mm.reprorate*(1 - mm.number/K);
                    try {
                        int temp = mm.number;
                        mm.number = temp  +  (int) (temp*reprorate);
                        mm.number = mm.number - (int) (temp*mm.deathrate);
                        System.out.println(mm.name + ": " +(int)(temp*mm.deathrate) + " dies, " + (int)(temp*reprorate) + " reproduces.");
                        marineMammalPrint.println(mm.name + ": " +(int)(temp*mm.deathrate) + " dies, " + (int)(temp*reprorate) + " reproduces.");

                        if (mm.food < mm.demand) {
                            int mmDie= (int) ((mm.demand - mm.food)/mm.demand*mm.number);
                            mm.number -= mmDie;
                            if (mm.number <= 0) mm.number = 0;
                            System.out.println(mm.name + ": " + mmDie + " dies for hunger.");
                            marineMammalPrint.println(mm.name + ": " + mmDie + " dies for hunger.");
                        }
                    } finally {
                        mm.numberl.unlock();
                    }
                    // Calculate natural death and reproduce
                    marineMammalPrint.println(threadName + " eat: " + eatcounter);
                    marineMammalPrint.println(threadName + " accidental death: " + deathcounter);
                    marineMammalPrint.println(threadName + " total demands: " + mm.demand);
                    marineMammalPrint.println(threadName + " total consumes: " + mm.food);
                    // Calculate death for hunger
                    marineMammalPrint.println("Remain Marine Mammals:" + mm.number);
                    timeHelper += 90;
                    marineMammalPrint.println("====================================================================================================");
                    /*************************************** Season Complete ******************************************/
                }
                marineMammalPrint.close();
            }
        };

        mpthr.start();
        kwthr.start();
        swthr.start();
        mmthr.start();

        System.out.println("Simulation ends");
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



