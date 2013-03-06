package com.sirtom.rsgd;

import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import alterrs.deob.asm.Renamer;

import com.method.rscd.CacheDownloader;
import com.sirtom.rsgd.file.IFile;

/**
 * The main entry point point of the current application.
 * @author Thomas Le Godais <thomaslegodais@live.com>
 */
public class Main {

	/**
	 * Are we downloading cache files.
	 */
	public static final Boolean DOWNLOAD_CACHE = Boolean.TRUE;

	/**
	 * Are we performing file actions?
	 */
	public static final Boolean PERFORM_FILE_ACTIONS = Boolean.TRUE;
	
	/**
	 * Are we performing deobfuscation and decompiling?
	 */
	public static final Boolean PERFORM_DEOBFUSCATION_AND_DECOMPILE = Boolean.TRUE;
	
	/**
	 * The list this application must perform.
	 */
	private List<IFile> fileActions;
	
	/**
	 * Constructs a new {@code Main} instance.
	 */
	private Main() {
		fileActions = new LinkedList<>();
	}
	
	/**
	 * The main access point of the application, gets everything up and running.
	 * @param args The command line arguments.
	 */
	public static void main(String[] args) {
		Main mainContext = new Main();
		try {
			
			mainContext.loadFileActions();
			mainContext.performFileActions();
			mainContext.deobfuscateAndDecompile(args);
			mainContext.downloadCache(args);
			
		} catch(Exception exception) {
			exception.printStackTrace();
		}
		Logger.getAnonymousLogger().info("Complete!");
	}
	
	/**
	 * We finish off the application by downloading the current RS cache to go
	 * with the game pack that we deobfuscated!
	 * 
	 * @param args The default command line arguments.
	 */
	private void downloadCache(String... args) {
		if(!DOWNLOAD_CACHE)
			return;
		CacheDownloader.main(args);
	}

	/**
	 * Deobfuscates the game pack itself, and then decompiles it into the source
	 * code that we can edit and re-compile.
	 * 
	 * @param args The default command line arguments.
	 * @throws IOException An IO based error has occured.
	 */
	private void deobfuscateAndDecompile(String... args) throws IOException {
		if(!PERFORM_DEOBFUSCATION_AND_DECOMPILE)
			return;
		
		Renamer.main(args);
		Runtime.getRuntime().exec("cmd /c start decompile.bat");		
	}

	/**
	 * Performs the file actions for an {@link IFile}.
	 * @throws Exception An specified exception has occured.
	 */
	private void performFileActions() throws Exception {
		if(!PERFORM_FILE_ACTIONS)
			return;
		
		for(IFile fileAction : fileActions) {
			if(fileAction == null)
				break;			
			fileAction.perform();
			fileAction.shutdownNow();
		}		
	}

	/**
	 * Loads the file actions for the application to perform.
	 * 
	 * @throws ParserConfigurationException
	 * @throws IOException An I/O based operation error has occured.
	 * @throws SAXException An SAM exception has occured.
	 * @throws DOMException  An DOM exception has occured.
	 * @throws ClassNotFoundException  The class not found exception has occured.
	 * @throws IllegalAccessException  An illegal access exception has occured.
	 * @throws InstantiationException  An instantation exception has occured.
	 */
	private void loadFileActions() throws ParserConfigurationException, SAXException, IOException, InstantiationException, IllegalAccessException, ClassNotFoundException, DOMException {
		DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
		Document document = documentBuilder.parse(new File("./actions.xml"));
		
		NodeList instructionsList = document.getDocumentElement().getChildNodes();
		
		for(int index = 1; index < instructionsList.getLength(); index += 2) {
			Node instructionNode = instructionsList.item(index);
			if(instructionNode == null)
				break;
			
			if(!instructionNode.getNodeName().equalsIgnoreCase("action"))
				break;
			
			NodeList instructionNodeList = instructionNode.getChildNodes();
			IFile instructionObect = null;
			
			for(int position = 1; position < instructionsList.getLength(); position += 2) {
				Node instructionListNode = instructionNodeList.item(position);
				if(instructionListNode == null)
					break;
				
				if(instructionListNode.getNodeName().equalsIgnoreCase("impl")) {
					instructionObect = (IFile) Class.forName(instructionListNode.getTextContent()).newInstance();
				}
				fileActions.add(instructionObect);
			}
		}
		Logger.getAnonymousLogger().info("Loaded " + fileActions.size() + " file actions.");
	}
}
