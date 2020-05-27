package com.papajohns.cleanup;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class SearchUtils {
	
	private static Logger log = Logger.getLogger(SearchUtils.class.getName());
	
	private SearchUtils() {
		//private constructor
	}
	public static List<String> searchFiles(File file, String pattern, List<String> result)
			throws FileNotFoundException {

		if (!file.isDirectory()) {
			throw new IllegalArgumentException("file has to be a directory");
		}

		if (result == null) {
			result = new ArrayList<>();
		}

		File[] files = file.listFiles();

		if (files != null) {
			for (File currentFile : files) {
				if (currentFile.isDirectory()) {
					searchFiles(currentFile, pattern, result);
				} else {
					Scanner scanner = new Scanner(currentFile);
					if (scanner.findWithinHorizon(pattern, 0) != null) {
						String currentPath = currentFile.getPath();
						if(!(containsIngnoreCase(currentPath, pattern) && !endsWithIngnoreCase(currentPath, (pattern + ".xml")))) {
							result.add(currentPath);
						}
					}
					scanner.close();
				}
			}
		}
		return result;
	}

	public static int searchForName(File file, String code) throws FileNotFoundException {

		Scanner input = new Scanner(file);
		int counter = 0;
		while (input.hasNextLine()) {
			String line = input.nextLine();
//			if (containsIngnoreCase(line, code)) {
//				final String[] lineFromFile = line.toUpperCase().split(code.toUpperCase());
//				counter += lineFromFile.length - 1;
//			}
			Pattern p = Pattern.compile(code);
			Matcher m = p.matcher(line);
			while (m.find()) {
				counter++;
			}
		}
		input.close();
		return counter;
	}

	public static void parseFile(String file, String code)
			throws ParserConfigurationException, SAXException, IOException, TransformerException {
		StreamResult sr = new StreamResult(new File(file));
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		DocumentBuilder db = dbf.newDocumentBuilder();
		Document dom = db.parse(file);
		Element docEle = dom.getDocumentElement();
		NodeList nlProduct = docEle.getElementsByTagName("products");
		NodeList nlNutri = docEle.getElementsByTagName("nutritionalInfoTableData");
		if (nlProduct != null && nlProduct.item(0) != null && nlProduct.item(0).getNodeType() == Node.ELEMENT_NODE) {
			removeItem(nlProduct, code, "value");
		}

		if (nlNutri != null && nlNutri.item(0) != null && nlNutri.item(0).getNodeType() == Node.ELEMENT_NODE) {
			removeItem(nlNutri, code, "productCodeRef");
		}

		Transformer transformer = getTransformer();
		transformer.transform(new DOMSource(dom), sr);

	}

	private static void removeItem(NodeList nlProduct, String code, String name2) {
		Element elProduct = (Element) nlProduct.item(0);
		NodeList itemList = elProduct.getElementsByTagName("item");
		for (int i = 0; i < itemList.getLength(); i++) {
			Node nNode = itemList.item(i);
			if (nNode.getNodeType() == Node.ELEMENT_NODE) {
				String value = elProduct.getElementsByTagName(name2).item(i).getTextContent();
				if (value.equals(code)) {
					nNode.getParentNode().removeChild(nNode);
					break;
				}
			}
		}
	}

	public static String splitFile(int noOfFiles, String inputfile) {
		String filePath = "";
		try {

			File file = new File(inputfile);

			Scanner scanner = new Scanner(file);
			int count = 0;
			while (scanner.hasNextLine()) {
				scanner.nextLine();
				count++;
			}
			log.debug("Lines in the file: " + count);// Displays no. of lines in the input file.

			double temp = (count / noOfFiles);
			int temp1 = (int) temp;
			int noOfLines = 0;
			noOfLines = temp1;
			int codeLeft = (int) (count - (noOfFiles * noOfLines));
			log.debug("No. of files to be generated :" + noOfFiles); // no. of lines in each files.

// ---------------------------------------------------------------------------------------------------------

// Actual splitting of file into smaller files

			FileInputStream inputStream = new FileInputStream(inputfile);
			DataInputStream in = new DataInputStream(inputStream);

			try (BufferedReader br = new BufferedReader(new InputStreamReader(in));) {
				String strLine;

				filePath = file.getParent() + "\\Split_Files";
				log.debug("Directory ::" + filePath);
				File newFile = new File(filePath);
				if (newFile.exists()) {
					Files.walk(Paths.get(filePath)).filter(Files::isRegularFile).map(Path::toFile)
							.forEach(File::delete);
				} else {
					newFile.mkdir();
				}
				filePath = filePath + "\\File";
				for (int j = 1; j <= noOfFiles; j++) {

					FileWriter outputStream = new FileWriter(filePath + j + ".csv"); // Destination
					// File
					// Location
					BufferedWriter out = new BufferedWriter(outputStream);
					if (j == noOfFiles && codeLeft != 0) {
						noOfLines = noOfLines + codeLeft;
					}
					for (int i = 1; i <= noOfLines; i++) {
						strLine = br.readLine();
						if (strLine != null) {
							out.write(strLine);
							if (i != noOfLines) {
								out.newLine();
							}
						}
					}

					out.close();
				}

				in.close();
				scanner.close();
			}
		} catch (Exception e) {
			 log.error(e.getMessage(), e);
		}
		return filePath;
	}
	
	private static Transformer getTransformer() throws TransformerConfigurationException {
		TransformerFactory factory = TransformerFactory.newInstance();
		factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);

		Transformer transformer = factory.newTransformer();
		transformer.setOutputProperty(OutputKeys.INDENT, "yes");
		return transformer;

		
	}
	
	public static boolean containsIngnoreCase(String source, String pattern) {
		if (source == null || pattern == null) {
	          return (source == null && pattern == null);
	      }
	      if (pattern.length() > source.length()) {
	          return false;
	      }
		
		return Pattern.compile(Pattern.quote(pattern), Pattern.CASE_INSENSITIVE).matcher(source).find();
		
	}
	public static boolean endsWithIngnoreCase(String str, String suffix) {
	      if (str == null || suffix == null) {
	          return (str == null && suffix == null);
	      }
	      if (suffix.length() > str.length()) {
	          return false;
	      }
	      int strOffset = str.length() - suffix.length();
	      return str.regionMatches(true, strOffset, suffix, 0, suffix.length());
	  }
	
	public static boolean verifyCodeEntry(String file, String code)
			throws ParserConfigurationException, SAXException, IOException, TransformerException {
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		DocumentBuilder db = dbf.newDocumentBuilder();
		Document dom = db.parse(file);
		Element docEle = dom.getDocumentElement();
		NodeList nlInternalName = docEle.getElementsByTagName("internal-name");
		if (nlInternalName != null && nlInternalName.item(0) != null && nlInternalName.item(0).getNodeType() == Node.ELEMENT_NODE) {
			Element elInternalName = (Element) nlInternalName.item(0);
			String value = elInternalName.getTextContent();
			if (value.equals(code)) {
				return true;
			}
		}

		return false;

	}

}
