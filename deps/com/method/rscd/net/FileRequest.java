package com.method.rscd.net;

import java.nio.ByteBuffer;

/**
 * Represents a request for a file from the game server.
 */
public class FileRequest {

	private int index;
	private int file;
	private ByteBuffer buffer;
	private int position;
	private boolean complete;

	/**
	 * Creates a new FileRequest object with the specified cache index and file number.
	 * @param index The cache index of the requested file
	 * @param file The file number of the requested file
	 */
	public FileRequest(int index, int file) {
		this.index = index;
		this.file = file;
	}

	public long hash() {
		return ((long) index << 32) | file;
	}

	/**
	 * Gets the cache index of this FileRequest.
	 * @return The cache index of this FileRequest.
	 */
	public int getIndex() {
		return index;
	}

	/**
	 * Gets the file of this FileRequest.
	 * @return The file of this FileRequest.
	 */
	public int getFile() {
		return file;
	}

	/**
	 * Initializes the buffer for this FileRequest.
	 * @param size The size of the buffer
	 */
	public void setSize(int size) {
		buffer = ByteBuffer.allocate(size);
	}

	/**
	 * Gets the buffer for this FileRequest.
	 * @return This request's buffer.
	 */
	public ByteBuffer getBuffer() {
		return buffer;
	}

	/**
	 * Gets the current block position (0-512) of this request.
	 * @return The current block position of this request.
	 */
	public int getPosition() {
		return position;
	}

	/**
	 * Sets the block position of this request.
	 * @param position The new position
	 */
	public void setPosition(int position) {
		this.position = position;
	}

	/**
	 * Returns this request's completion state.
	 * @return true if this request is complete; false otherwise
	 */
	public boolean isComplete() {
		return complete;
	}

	/**
	 * Sets this request's completion state.
	 * @param complete The new state
	 */
	public void setComplete(boolean complete) {
		this.complete = complete;
	}

}
