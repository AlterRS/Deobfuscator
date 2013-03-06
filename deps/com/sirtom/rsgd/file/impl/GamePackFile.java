package com.sirtom.rsgd.file.impl;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.logging.Logger;

import com.sirtom.rsgd.file.IFile;

/**
 * An {@link IFile} that is used to represent the game pack file.
 * @author Thomas Le Godais <thomaslegodais@live.com>
 */
public class GamePackFile implements IFile {

	/*
	 * (non-Javadoc)
	 * @see com.sirtom.rsgd.file.IFile#perform()
	 */
	@Override
	public void perform() throws IOException {
		BufferedReader reader = new BufferedReader(new FileReader("./data/params.txt"));
		String line;
		
		while((line = reader.readLine()) != null) {
			if(line.contains("'archive=gamepack")) {
				
				String[] regex = line.split("archive=");
				String gamePackURL = "http://world27.runescape.com/" + regex[1].replace(" ');", "");
			
				URL url = new URL(gamePackURL);
				InputStream inStream = url.openStream();
				BufferedInputStream bufIn = new BufferedInputStream(inStream);
								
				File jarFile = new File("./data/currentGamePack.jar");
				OutputStream out = new FileOutputStream(jarFile);
				BufferedOutputStream bufOut = new BufferedOutputStream(out);
				
				byte[] bufferedArray = new byte[2048];
				while(true) {
					
					int inRead = bufIn.read(bufferedArray, 0, bufferedArray.length);
					if (inRead <= 0)
						break;
					bufOut.write(bufferedArray, 0, inRead);
				}
				bufOut.flush();
				out.close();
				inStream.close();
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
		Logger.getAnonymousLogger().info("Successfully grabbed the current game pack.");
	}
}
