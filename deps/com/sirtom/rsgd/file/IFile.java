package com.sirtom.rsgd.file;

/**
 * An simple interface used to perform the tasks of an file, for example the
 * actions of an parameters file.
 * 
 * @author Thomas Le Godais <thomaslegodais@live.com>
 */
public interface IFile {

	/**
	 * Performs the operation of an {@code IFile}.
	 * 
	 * @throws Exception An standard exception has occured.
	 */
	void perform() throws Exception;
	
	/**
	 * The operation that the {@code IFile} will perform on the shutdown
	 * of the specified {@code IFile}.
	 */
	void shutdownNow();
}
