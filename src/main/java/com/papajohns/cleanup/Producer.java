package com.papajohns.cleanup;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.BlockingQueue;

import org.apache.log4j.Logger;

public class Producer implements Runnable{
	
	private static Logger log = Logger.getLogger(Producer.class.getName());

    private Path fileToRead;
    private BlockingQueue<String> queue;

    public Producer(Path filePath, BlockingQueue<String> q){
        fileToRead = filePath;
        queue = q;
    }

    @Override
	public void run() {
		try {
			try (BufferedReader reader = Files.newBufferedReader(fileToRead);) {
				String line;
				while ((line = reader.readLine()) != null) {
					try {
						queue.put(line);
						log.debug(Thread.currentThread().getName() + " added \"" + line + "\" to queue, queue size: "
								+ queue.size());
					} catch (InterruptedException e) {
						log.error(e.getMessage(), e);
					}
				}
			}

			log.debug(Thread.currentThread().getName() + " finished");
		} catch (IOException e) {
			log.error(e.getMessage(), e);
		}
	}

}
