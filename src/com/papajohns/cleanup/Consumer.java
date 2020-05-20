package com.papajohns.cleanup;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.BlockingQueue;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import org.xml.sax.SAXException;

public class Consumer implements Runnable{
    private BlockingQueue<String> queue;
    private String pathStr;
    private List<String> fileName;

    public Consumer(BlockingQueue<String> q, String pathStr, List<String> fileName){
        queue = q;
        this.pathStr = pathStr;
        this.fileName = fileName;
    }

    public void run(){
        while(true){
            String code = queue.poll();

            if(code == null && !Controller.isProducerAlive())
                return;

            if(code != null){
                System.out.println(Thread.currentThread().getName()+" processing line: "+ code);
    			List<String> result;
				try {
					result = SearchUtils.searchFiles(new File(pathStr), code, null);
					if(result.size() == 1) {
						String fileToBeDeleted = result.get(0);
						int codeReferenceCount = SearchUtils.searchForName(new File(fileToBeDeleted), code);
						if((fileToBeDeleted.endsWith(code + ".xml") && codeReferenceCount == 3) || (!fileToBeDeleted.contains(code) && codeReferenceCount == 2)) {
	    					System.out.println(code + ":: File to be removed : " + fileToBeDeleted);
	    					fileName.add(fileToBeDeleted);
	    					Files.deleteIfExists(Paths.get(result.get(0)));
	    				}
					} else {
		    			for(String file : result) {
		    				if (!file.contains(code) && (SearchUtils.searchForName(new File(file), code) == 3  || SearchUtils.searchForName(new File(file), code) == 4)) {
		    					System.out.println(code + ":: block to be deleted");
		    					SearchUtils.parseFile(file, code);
		    				} /*else if(file.endsWith(code + ".xml") && SearchUtils.searchForName(new File(file), code) == 3) {
		    					//file to be deleted
		    					System.out.println(code + ":: File to be removed : " + file);
		    					Files.deleteIfExists(Paths.get(result.get(0)));
		    				}*/ else if (!file.contains(code) && (SearchUtils.searchForName(new File(file), code) == 6  || SearchUtils.searchForName(new File(file), code) == 7)) {
		    					System.out.println(code + ":: has been used as default item, in file :: " + file);
		    					//SearchUtils.parseFile(file, code);
		    				} 
		    			}
					}
				} catch (ParserConfigurationException | SAXException |TransformerException | IOException  e) {
					System.out.println(e);
				}
            }

        }
    }
    
    
}
