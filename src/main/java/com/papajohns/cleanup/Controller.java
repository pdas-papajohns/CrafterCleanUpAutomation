package com.papajohns.cleanup;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Logger;

public class Controller {
	
	private static Logger log = Logger.getLogger(Controller.class.getName());

    private static final int QUEUE_SIZE = 10;
    private static BlockingQueue<String> queue;
    private static Collection<Thread> producerThreadCollection;
    private static Collection<Thread> allThreadCollection;
    private static List<String> fileName = new ArrayList<>();
    
    
    public static void main(String[] args) {
    	BasicConfigurator.configure();
    	long startTime = System.nanoTime();
        producerThreadCollection = new ArrayList<>();
        allThreadCollection = new ArrayList<>();
        queue = new LinkedBlockingDeque<>(QUEUE_SIZE);

        
        int threadCount = Integer.parseInt(args[2]);
        String filePath = SearchUtils.splitFile(threadCount, args[0]);
        createAndStartProducers(filePath, threadCount);
        createAndStartConsumers(args[1], threadCount);
        

        for(Thread t: allThreadCollection){
            try {
                t.join();
            } catch (InterruptedException e) {
            	log.error(e.getMessage(), e);
            }
        }
        log.debug("Controller finished");
        long endTime = System.nanoTime();
        long elapsedTimeInMillis = TimeUnit.MILLISECONDS.convert((endTime - startTime), TimeUnit.NANOSECONDS);
        log.debug("Total elapsed time: " + elapsedTimeInMillis + " ms");
    }

    private static void createAndStartProducers(String codePath, int threadCount){
        for(int i = 1; i <= threadCount; i++){
            Producer producer = new Producer(Paths.get(codePath + i + ".csv"), queue);
            Thread producerThread = new Thread(producer,"producer-"+i);
            producerThreadCollection.add(producerThread);
            producerThread.start();
        }
        allThreadCollection.addAll(producerThreadCollection);
    }

    private static void createAndStartConsumers(String filePath, int threadCount){
        for(int i = 0; i < threadCount; i++){
            Thread consumerThread = new Thread(new Consumer(queue, filePath, fileName), "consumer-"+i);
            allThreadCollection.add(consumerThread);
            consumerThread.start();
        }
    }

    public static boolean isProducerAlive(){
        for(Thread t: producerThreadCollection){
            if(t.isAlive())
                return true;
        }
        return false;
    }
    
    public static boolean isAllThreadCollectionAlive(){
        for(Thread t: allThreadCollection){
            if(t.isAlive())
                return true;
        }
        return false;
    }
}
