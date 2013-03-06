/* SearchPath Copyright (C) 1998-2002 Jochen Hoenicke.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 2, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; see the file COPYING.LESSER.  If not, write to
 * the Free Software Foundation, 675 Mass Ave, Cambridge, MA 02139, USA.
 *
 * $Id: SearchPath.java,v 4.18.2.3 2002/05/28 17:34:01 hoenicke Exp $
 */

package alterrs.jode.bytecode;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

import alterrs.jode.GlobalOptions;

/**
 * This class represents a path of multiple directories and/or zip files, where
 * we can search for file names.
 * 
 * @author Jochen Hoenicke
 */
public class SearchPath {

	/**
	 * We need a different pathSeparatorChar, since ':' (used for most UNIX
	 * System) is used a protocol separator in URLs.
	 * <p/>
	 * We currently allow both pathSeparatorChar and altPathSeparatorChar and
	 * decide if it is a protocol separator by context.
	 */
	public static final char altPathSeparatorChar = ',';
	URL[] bases;
	byte[][] urlzips;
	File[] dirs;
	ZipFile[] zips;
	String[] zipDirs;
	Hashtable[] zipEntries;

	private static void addEntry(Hashtable entries, String name) {
		String dir = "";
		int pathsep = name.lastIndexOf("/");
		if (pathsep != -1) {
			dir = name.substring(0, pathsep);
			name = name.substring(pathsep + 1);
		}

		Vector dirContent = (Vector) entries.get(dir);
		if (dirContent == null) {
			dirContent = new Vector();
			entries.put(dir, dirContent);
			if (dir != "")
				addEntry(entries, dir);
		}
		dirContent.addElement(name);
	}

	private void fillZipEntries(int nr) {
		Enumeration zipEnum = zips[nr].entries();
		zipEntries[nr] = new Hashtable();
		while (zipEnum.hasMoreElements()) {
			ZipEntry ze = (ZipEntry) zipEnum.nextElement();
			String name = ze.getName();
			// if (name.charAt(0) == '/')
			// name = name.substring(1);
			if (zipDirs[nr] != null) {
				if (!name.startsWith(zipDirs[nr]))
					continue;
				name = name.substring(zipDirs[nr].length());
			}
			if (!ze.isDirectory() && name.endsWith(".class"))
				addEntry(zipEntries[nr], name);
		}
	}

	private void readURLZip(int nr, URLConnection conn) {
		int length = conn.getContentLength();
		if (length <= 0)
			// Give a approximation if length is unknown
			length = 10240;
		else
			// Increase the length by one, so we hopefully don't need
			// to grow the array later (we need a little overshot to
			// know when the end is reached).
			length++;

		urlzips[nr] = new byte[length];
		try {
			InputStream is = conn.getInputStream();
			int pos = 0;
			for (;;) {
				// This is ugly, is.available() may return zero even
				// if there are more bytes.
				int avail = Math.max(is.available(), 1);
				if (pos + is.available() > urlzips[nr].length) {
					// grow the byte array.
					byte[] newarr = new byte[Math.max(2 * urlzips[nr].length,
							pos + is.available())];
					System.arraycopy(urlzips[nr], 0, newarr, 0, pos);
					urlzips[nr] = newarr;
				}
				int count = is.read(urlzips[nr], pos, urlzips[nr].length - pos);
				if (count == -1)
					break;
				pos += count;
			}
			if (pos < urlzips[nr].length) {
				// shrink the byte array again.
				byte[] newarr = new byte[pos];
				System.arraycopy(urlzips[nr], 0, newarr, 0, pos);
				urlzips[nr] = newarr;
			}
		} catch (IOException ex) {
			GlobalOptions.err.println("IOException while reading "
					+ "remote zip file " + bases[nr]);
			// disable entry
			bases[nr] = null;
			urlzips[nr] = null;
			return;
		}
		try {
			// fill entries into hash table
			ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(
					urlzips[nr]));
			zipEntries[nr] = new Hashtable();
			ZipEntry ze;
			while ((ze = zis.getNextEntry()) != null) {
				String name = ze.getName();
				// if (name.charAt(0) == '/')
				// name = name.substring(1);
				if (zipDirs[nr] != null) {
					if (!name.startsWith(zipDirs[nr]))
						continue;
					name = name.substring(zipDirs[nr].length());
				}
				if (!ze.isDirectory() && name.endsWith(".class"))
					addEntry(zipEntries[nr], name);
				zis.closeEntry();
			}
			zis.close();
		} catch (IOException ex) {
			GlobalOptions.err.println("Remote zip file " + bases[nr]
					+ " is corrupted.");
			// disable entry
			bases[nr] = null;
			urlzips[nr] = null;
			zipEntries[nr] = null;
			return;
		}
	}

	/**
	 * Creates a new search path for the given path.
	 * 
	 * @param path
	 *            The path where we should search for files. They should be
	 *            separated by the system dependent pathSeparator. The entries
	 *            may also be zip or jar files.
	 */
	public SearchPath(String path) {
		// Calculate a good approximation (rounded upwards) of the tokens
		// in this path.
		int length = 1;
		for (int index = path.indexOf(File.pathSeparatorChar); index != -1; length++)
			index = path.indexOf(File.pathSeparatorChar, index + 1);
		if (File.pathSeparatorChar != altPathSeparatorChar) {
			for (int index = path.indexOf(altPathSeparatorChar); index != -1; length++)
				index = path.indexOf(altPathSeparatorChar, index + 1);
		}
		bases = new URL[length];
		urlzips = new byte[length][];
		dirs = new File[length];
		zips = new ZipFile[length];
		zipEntries = new Hashtable[length];
		zipDirs = new String[length];
		int i = 0;
		for (int ptr = 0; ptr < path.length(); ptr++, i++) {
			int next = ptr;
			while (next < path.length()
					&& path.charAt(next) != File.pathSeparatorChar
					&& path.charAt(next) != altPathSeparatorChar)
				next++;

			int index = ptr;
			colon_separator: while (next > ptr && next < path.length()
					&& path.charAt(next) == ':') {
				// Check if this is a URL instead of a pathSeparator
				// Since this is a while loop it allows nested urls like
				// jar:ftp://ftp.foo.org/pub/foo.jar!/

				while (index < next) {
					char c = path.charAt(index);
					// According to RFC 1738 letters, digits, '+', '-'
					// and '.' are allowed SCHEMA characters. We
					// disallow '.' because it is a good marker that
					// the user has specified a filename instead of a
					// URL.
					if ((c < 'A' || c > 'Z') && (c < 'a' || c > 'z')
							&& (c < '0' || c > '9') && "+-".indexOf(c) == -1) {
						break colon_separator;
					}
					index++;
				}
				next++;
				index++;
				while (next < path.length()
						&& path.charAt(next) != File.pathSeparatorChar
						&& path.charAt(next) != altPathSeparatorChar)
					next++;
			}
			String token = path.substring(ptr, next);
			ptr = next;

			boolean mustBeJar = false;
			// We handle jar URL's ourself.
			if (token.startsWith("jar:")) {
				index = 0;
				do {
					index = token.indexOf('!', index);
				} while (index != -1 && index != token.length() - 1
						&& token.charAt(index + 1) != '/');

				if (index == -1 || index == token.length() - 1) {
					GlobalOptions.err.println("Warning: Illegal jar url "
							+ token + ".");
					continue;
				}
				zipDirs[i] = token.substring(index + 2);
				if (!zipDirs[i].endsWith("/"))
					zipDirs[i] = zipDirs[i] + "/";
				token = token.substring(4, index);
				mustBeJar = true;
			}
			index = token.indexOf(':');
			if (index != -1 && index < token.length() - 2
					&& token.charAt(index + 1) == '/'
					&& token.charAt(index + 2) == '/') {
				// This looks like an URL.
				try {
					bases[i] = new URL(token);
					try {
						URLConnection connection = bases[i].openConnection();
						if (mustBeJar || token.endsWith(".zip")
								|| token.endsWith(".jar")
								|| connection.getContentType().endsWith("/zip")) {
							// This is a zip file. Read it into memory.
							readURLZip(i, connection);
						}
					} catch (IOException ex) {
						// ignore
					} catch (SecurityException ex) {
						GlobalOptions.err
								.println("Warning: Security exception while accessing "
										+ bases[i] + ".");
					}
				} catch (MalformedURLException ex) {
					/* disable entry */
					bases[i] = null;
					dirs[i] = null;
				}
			} else {
				try {
					dirs[i] = new File(token);
					if (mustBeJar || !dirs[i].isDirectory()) {
						try {
							zips[i] = new ZipFile(dirs[i]);
						} catch (java.io.IOException ex) {
							/* disable this entry */
							dirs[i] = null;
						}
					}
				} catch (SecurityException ex) {
					/* disable this entry */
					GlobalOptions.err
							.println("Warning: SecurityException while accessing "
									+ token + ".");
					dirs[i] = null;
				}
			}
		}
	}

	public boolean exists(String filename) {
		String localFileName = (java.io.File.separatorChar != '/') ? filename
				.replace('/', java.io.File.separatorChar) : filename;
		for (int i = 0; i < dirs.length; i++) {
			if (zipEntries[i] != null) {
				if (zipEntries[i].get(filename) != null)
					return true;

				String dir = "";
				String name = filename;
				int index = filename.lastIndexOf('/');
				if (index >= 0) {
					dir = filename.substring(0, index);
					name = filename.substring(index + 1);
				}
				Vector directory = (Vector) zipEntries[i].get(dir);
				if (directory != null && directory.contains(name))
					return true;
				continue;
			}
			if (bases[i] != null) {
				try {
					URL url = new URL(bases[i], filename);
					URLConnection conn = url.openConnection();
					conn.connect();
					conn.getInputStream().close();
					return true;
				} catch (IOException ex) {
					/* ignore */
				}
				continue;
			}
			if (dirs[i] == null)
				continue;
			if (zips[i] != null) {
				String fullname = zipDirs[i] != null ? zipDirs[i] + filename
						: filename;
				ZipEntry ze = zips[i].getEntry(fullname);
				if (ze != null)
					return true;
			} else {
				try {
					File f = new File(dirs[i], localFileName);
					if (f.exists())
						return true;
				} catch (SecurityException ex) {
					/* ignore and take next element */
				}
			}
		}
		return false;
	}

	/**
	 * Searches for a file in the search path.
	 * 
	 * @param filename
	 *            the filename. The path components should be separated by
	 *            <code>/</code>.
	 * @return An InputStream for the file.
	 */
	public InputStream getFile(String filename) throws IOException {
		String localFileName = (java.io.File.separatorChar != '/') ? filename
				.replace('/', java.io.File.separatorChar) : filename;
		for (int i = 0; i < dirs.length; i++) {
			if (urlzips[i] != null) {
				ZipInputStream zis = new ZipInputStream(
						new ByteArrayInputStream(urlzips[i]));
				ZipEntry ze;
				String fullname = zipDirs[i] != null ? zipDirs[i] + filename
						: filename;
				while ((ze = zis.getNextEntry()) != null) {
					if (ze.getName().equals(fullname)) {
						// /#ifdef JDK11
						// / // The skip method in jdk1.1.7 ZipInputStream
						// / // is buggy. We return a wrapper that fixes
						// / // this.
						// / return new FilterInputStream(zis) {
						// / private byte[] tmpbuf = new byte[512];
						// / public long skip(long n) throws IOException {
						// / long skipped = 0;
						// / while (n > 0) {
						// / int count = read(tmpbuf, 0,
						// / (int)Math.min(n, 512L));
						// / if (count == -1)
						// / return skipped;
						// / skipped += count;
						// / n -= count;
						// / }
						// / return skipped;
						// / }
						// / };
						// /#else
						return zis;
						// /#endif
					}
					zis.closeEntry();
				}
				continue;
			}
			if (bases[i] != null) {
				try {
					URL url = new URL(bases[i], filename);
					URLConnection conn = url.openConnection();
					conn.setAllowUserInteraction(true);
					return conn.getInputStream();
				} catch (SecurityException ex) {
					GlobalOptions.err.println("Warning: SecurityException"
							+ " while accessing " + bases[i] + filename);
					ex.printStackTrace(GlobalOptions.err);
					/* ignore and take next element */
				} catch (FileNotFoundException ex) {
					/* ignore and take next element */
				}
				continue;
			}
			if (dirs[i] == null)
				continue;
			if (zips[i] != null) {
				String fullname = zipDirs[i] != null ? zipDirs[i] + filename
						: filename;
				ZipEntry ze = zips[i].getEntry(fullname);
				if (ze != null)
					return zips[i].getInputStream(ze);
			} else {
				try {
					File f = new File(dirs[i], localFileName);
					if (f.exists())
						return new FileInputStream(f);
				} catch (SecurityException ex) {
					GlobalOptions.err.println("Warning: SecurityException"
							+ " while accessing " + dirs[i] + localFileName);
					/* ignore and take next element */
				}
			}
		}
		throw new FileNotFoundException(filename);
	}

	/**
	 * Searches for a filename in the search path and tells if it is a
	 * directory.
	 * 
	 * @param filename
	 *            the filename. The path components should be separated by
	 *            <code>/</code>.
	 * @return true, if filename exists and is a directory, false otherwise.
	 */
	public boolean isDirectory(String filename) {
		String localFileName = (java.io.File.separatorChar != '/') ? filename
				.replace('/', java.io.File.separatorChar) : filename;
		for (int i = 0; i < dirs.length; i++) {
			if (dirs[i] == null)
				continue;
			if (zips[i] != null && zipEntries[i] == null)
				fillZipEntries(i);

			if (zipEntries[i] != null) {
				if (zipEntries[i].containsKey(filename))
					return true;
			} else {
				try {
					File f = new File(dirs[i], localFileName);
					if (f.exists())
						return f.isDirectory();
				} catch (SecurityException ex) {
					GlobalOptions.err.println("Warning: SecurityException"
							+ " while accessing " + dirs[i] + localFileName);
				}
			}
		}
		return false;
	}

	/**
	 * Searches for all files in the given directory.
	 * 
	 * @param dirName
	 *            the directory name. The path components should be separated by
	 *            <code>/</code>.
	 * @return An enumeration with all files/directories in the given directory.
	 */
	public Enumeration listFiles(final String dirName) {
		return new Enumeration() {
			int pathNr;
			Enumeration zipEnum;
			int fileNr;
			String localDirName = (java.io.File.separatorChar != '/') ? dirName
					.replace('/', java.io.File.separatorChar) : dirName;
			File currentDir;
			String[] files;

			public String findNextFile() {
				while (true) {
					if (zipEnum != null) {
						while (zipEnum.hasMoreElements()) {
							return (String) zipEnum.nextElement();
						}
						zipEnum = null;
					}
					if (files != null) {
						while (fileNr < files.length) {
							String name = files[fileNr++];
							if (name.endsWith(".class")) {
								return name;
							} else if (name.indexOf(".") == -1) {
								/*
								 * ignore directories containing a dot. they
								 * can't be a package directory.
								 */
								File f = new File(currentDir, name);
								if (f.exists() && f.isDirectory())
									return name;
							}
						}
						files = null;
					}
					if (pathNr == dirs.length)
						return null;

					if (zips[pathNr] != null && zipEntries[pathNr] == null)
						fillZipEntries(pathNr);

					if (zipEntries[pathNr] != null) {
						Vector entries = (Vector) zipEntries[pathNr]
								.get(dirName);
						if (entries != null)
							zipEnum = entries.elements();
					} else if (dirs[pathNr] != null) {
						try {
							File f = new File(dirs[pathNr], localDirName);
							if (f.exists() && f.isDirectory()) {
								currentDir = f;
								files = f.list();
								fileNr = 0;
							}
						} catch (SecurityException ex) {
							GlobalOptions.err
									.println("Warning: SecurityException"
											+ " while accessing "
											+ dirs[pathNr] + localDirName);
							/* ignore and take next element */
						}
					}
					pathNr++;
				}
			}

			String nextName;

			public boolean hasMoreElements() {
				return (nextName != null || (nextName = findNextFile()) != null);
			}

			public Object nextElement() {
				if (nextName == null)
					return findNextFile();
				else {
					String result = nextName;
					nextName = null;
					return result;
				}
			}
		};
	}
}
