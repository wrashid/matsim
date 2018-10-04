package org.matsim.pt.connectionScan.utils;

import org.apache.log4j.Logger;

import java.io.*;

public class CalculationTimeMonitor {

    public static void main(String[] args) throws IOException {
        String file = "C:\\Users\\gthunig\\Desktop\\Vsp\\ConnectionScan\\Scenarien\\Berlin\\CS\\b5_1.output_plans.xml";
        String file2 = "C:\\Users\\gthunig\\Desktop\\Vsp\\ConnectionScan\\Scenarien\\Berlin\\Dijkstra\\b5_1.output_plans.xml";
        BufferedReader reader = new BufferedReader(new FileReader(file));
        String line = reader.readLine();
        int counter = 0;
        while(line != null) {
            if (line.contains("leg mode=\"pt\"")) {
                counter++;
                line = reader.readLine();
                while(!line.contains("leg mode=\"egress_walk\"")) {
                    line = reader.readLine();
                }
            }

            line = reader.readLine();
        }
        System.out.println(counter);
    }

    private static final Logger log = Logger.getLogger(CalculationTimeMonitor.class);

    long csCalculationTime = 0;
    long conversionTime = 0;

    long routingStarted;
    long routingFinished;
    long csaStarted;
    long csaFinished;

    int routes = 0;

    double random = Math.random();

    public void routingStarted() {
        routingStarted = System.nanoTime();
    }

    public void csaStarted() {
        csaStarted = System.nanoTime();
    }

    public void csaFinished() {
        csaFinished = System.nanoTime();
    }

    public void routingFinished() {
        routingFinished = System.nanoTime();
        calculate();
    }

    private void calculate() {
        long differenceCsa = csaFinished - csaStarted;
        long differenceRouting = routingFinished - routingStarted;
        conversionTime += (differenceRouting - differenceCsa);
        csCalculationTime += differenceCsa;
        routes++;
    }

    public void printOutput() {

        log.info("CSA calculation time in milliseconds:\t" + csCalculationTime / 1000);
        log.info("CSA calculation time percentage:\t\t" + Math.round(csCalculationTime / (double)(csCalculationTime + conversionTime) * 100));
        log.info("CSA conversion time in milliseconds:\t" + conversionTime / 1000);
        log.info("CSA conversion time percentage:\t\t" + Math.round(conversionTime / (double)(csCalculationTime + conversionTime) * 100));
        log.info("Routes calculated:\t" + routes);
        log.info("Random:\t" + random);
    }
}
