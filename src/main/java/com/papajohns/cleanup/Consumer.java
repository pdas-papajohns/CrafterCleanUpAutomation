package com.papajohns.cleanup;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.BlockingQueue;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import org.apache.log4j.Logger;
import org.xml.sax.SAXException;

public class Consumer implements Runnable {
	private static Logger log = Logger.getLogger(Consumer.class.getName());
	
	private BlockingQueue<String> queue;
	private String pathStr;
	private static double defCounter = 0;

	public Consumer(BlockingQueue<String> q, String pathStr) {
		queue = q;
		this.pathStr = pathStr;
	}
	
	public static Double getDefCounter() {
		return defCounter;
	}

	public void run() {
		while (true) {
			Object line = queue.poll();

			if (line == null && !Controller.isProducerAlive())
				return;

			if (line != null) {
				String code = (String)line;
				log.info(Thread.currentThread().getName() + " processing line: " + code);
				List<String> result;
				int codeReferenceCount = 0;
				synchronized (line) {
					try {
						result = SearchUtils.searchFiles(new File(pathStr), code, null);
						log.debug(code + " :: No. of files :: " + result.size());
						if (result.size() == 1) {
							String fileToBeDeleted = result.get(0);
							codeReferenceCount = SearchUtils.searchForName(new File(fileToBeDeleted), code);
							if ((SearchUtils.endsWithIngnoreCase(fileToBeDeleted, File.separator + code + ".xml") //&& SearchUtils.verifyCodeEntry(fileToBeDeleted, code)&& codeReferenceCount == 3
									)
									//|| (!fileToBeDeleted.contains(code) && SearchUtils.verifyCodeEntry(fileToBeDeleted, code) && (codeReferenceCount == 1 || codeReferenceCount == 2))
									) {
								log.info(code + ":: File to be removed : " + fileToBeDeleted);
								Files.deleteIfExists(Paths.get(result.get(0)));
							}
							
						} else {
							for (String file : result) {
								log.info(code + ":: file :: " + file);
								codeReferenceCount = SearchUtils.searchForName(new File(file), code);
								if (!SearchUtils.containsIngnoreCase(file, code)
										//&& (codeReferenceCount == 3 || codeReferenceCount == 4)) {
										&& SearchUtils.validateProduct(file, code, "products")) {
									if(!SearchUtils.validateProduct(file, code, "defaultProduct")) {
										log.debug(code + ":: block to be deleted in file :: " + file);
										SearchUtils.parseFile(file, code);
									} else {
										++defCounter;
										log.debug(code + ":: has been used as default item, in file :: " + file);
									}
//								} else if (!file.contains(code)
//										&& (codeReferenceCount == 6
//												|| codeReferenceCount == 7)) {
//									log.debug(code + ":: has been used as default item, in file :: " + file);
//								} else if ((!file.contains(code)
//												&& SearchUtils.verifyCodeEntry(file, code)
//												&& (codeReferenceCount == 1 || codeReferenceCount == 2))) {
//									log.info(code + ":: File to be removed : " + file);
									//Files.deleteIfExists(Paths.get(file));
								}

							}
						}
					} catch (ParserConfigurationException | SAXException | TransformerException | IOException e) {
						log.error(e.getMessage(), e);
					}
				}
			}

		}
	}

}
