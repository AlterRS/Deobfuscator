package com.sirtom.rsgd.file.impl;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;

import com.sirtom.rsgd.file.IFile;
import com.sirtom.rsgd.util.Utils;

/**
 * An {@link IFile} that is used to represent the inner.pack.gz file
 * @author Thomas Le Godais <thomaslegodais@live.com>
 */
public class InnerPackGzFile implements IFile {
	
	/*
	 * (non-Javadoc)
	 * @see com.sirtom.rsgd.file.IFile#perform()
	 */
	@SuppressWarnings("resource")
	@Override
	public void perform() throws IOException {
		JarFile gamePack = new JarFile("./data/currentGamePack.jar");
		ZipEntry zipEntry = gamePack.getEntry("inner.pack.gz");
		
		InputStream inputStream = gamePack.getInputStream(zipEntry);
		OutputStream outputStream = new FileOutputStream("./data/inner.pack.gz");
		
		int data;
		while(!((data = inputStream.read()) <= -1)) 
			outputStream.write(data);
		
		outputStream.flush();
		outputStream.close();
		inputStream.close();
		
		BufferedReader params = new BufferedReader(new FileReader("./data/params.txt"));
		
		String line, param0 = null, paramN1 = null;
		while((line = params.readLine()) != null) {
			if(line.contains("document.write('<param name=")) {
				String[] regex = line.split("value=\"");
				if(regex[0].contains("param name=\"0"))
					param0 = regex[1].replace("\">');", "");
				if(regex[0].contains("param name=\"-1"))
					paramN1 = regex[1].replace("\">');", "");
			}
		}
		params.close();		
		HashMap<String, byte[]> classes = Utils.decryptPack(param0, paramN1);
		JarOutputStream jos = new JarOutputStream(new FileOutputStream(new File("./input.jar")));
		for (String key : classes.keySet()) {
			byte[] bytes = classes.get(key);
			jos.putNextEntry(new JarEntry(key.replaceAll("\\.", "/") + ".class"));
			jos.write(bytes);
			jos.closeEntry();

		}
		jos.close();		
		
	}

	/*
	 * (non-Javadoc)
	 * @see com.sirtom.rsgd.file.IFile#shutdownNow()
	 */
	@Override
	public void shutdownNow() {
		Logger.getAnonymousLogger().info("Successfully extracted and decrypted inner.pack.gz file.");		
	}
}
