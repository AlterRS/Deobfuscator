package alterrs.deob.util;

import java.io.File;

public class FileUtilities {
	public static void deleteDir(File dir) {
		for(File file : dir.listFiles()) {
			if(file.isDirectory()) {
				deleteDir(file);
				continue;
			}
			
			file.delete();
		}
		dir.delete();
	}
}
