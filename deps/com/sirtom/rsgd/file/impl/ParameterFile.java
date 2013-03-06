package com.sirtom.rsgd.file.impl;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.logging.Logger;

import com.sirtom.rsgd.file.IFile;

/**
 * An {@link IFile} that is used to represent the parameter file.
 * @author Thomas Le Godais <thomaslegodais@live.com>
 */
public class ParameterFile implements IFile {

	/*
	 * (non-Javadoc)
	 * @see com.sirtom.rsgd.file.IFile#perform()
	 */
	@Override
	public void perform() throws IOException {
		URL websiteURL = new URL("http://www.runescape.com/game.ws?j=1");
		URLConnection urlConnection = websiteURL.openConnection();
		
		BufferedReader reader = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
		String line;
		
		while((line = reader.readLine()) != null) {
			if(line.startsWith("<iframe id=\"game")) {				
				String[] regex = line.split("src=\"");
				String parameterFile = regex[1].replace("frameborder=\"0", "").replace("</iframe>", "").replaceAll("\"", "").replace(">", "");
						
				URL pURL = new URL(parameterFile);
				URLConnection pConnection = pURL.openConnection();
				
				BufferedReader pReader = new BufferedReader(new InputStreamReader(pConnection.getInputStream()));
				String pLine;
				
				BufferedWriter writer = new BufferedWriter(new FileWriter("./data/params.txt"));
				while((pLine = pReader.readLine()) != null) {
					writer.write(pLine);
					writer.newLine();
				}
				writer.close();
			}
		}
		reader.close();	
	}
	
	/*
	 * (non-Javadoc)
	 * @see com.sirtom.rsgd.file.IFile#shutdownNow()
	 */
	@Override
	public void shutdownNow() {
		Logger.getAnonymousLogger().info("Successfully grabbed parameters file.");		
	}
}
