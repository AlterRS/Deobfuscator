package com.method.rscd.cache;

import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.zip.CRC32;
import java.util.zip.GZIPInputStream;

/**
 * A container class for files stored inside the cache.
 */
public class FileContainer {

	private static final CRC32 CRC32 = new CRC32();

	private int crc;
	private byte[] buffer;
	private int compression;
	private int size;

	/**
	 * Creates a new FileContainer object.
	 * @param buffer The file data
	 */
	public FileContainer(ByteBuffer buffer) {
		CRC32.update(buffer.array());
		crc = (int) CRC32.getValue();
		CRC32.reset();
		init(buffer);
	}

	/**
	 * Reads the container data and instantiates the internal buffer array.
	 * @param buffer The file data
	 */
	private void init(ByteBuffer buffer) {
		compression = buffer.get() & 0xff;
		int compressedSize = buffer.getInt();
		if (compression == 0) {
			size = compressedSize;
		} else {
			size = buffer.getInt();
		}
		this.buffer = new byte[compressedSize];
		buffer.get(this.buffer);
	}

	/**
	 * Unpacks the container, decompressing it if necessary.
	 * @return The unpacked file data.
	 */
	public byte[] unpack() {
		if (compression == 0) {
			return buffer;
		}
		byte[] result = new byte[size];
		if (compression == 1) {
			try {
				DataInputStream stream = new DataInputStream(new BZip2CompressorInputStream(
						new ByteArrayInputStream(buffer)));
				stream.readFully(result);
				stream.close();
			} catch (IOException ioex) {
				ioex.printStackTrace();
				return null;
			}
		} else {
			try {
				DataInputStream stream = new DataInputStream(new GZIPInputStream(
						new ByteArrayInputStream(buffer)));
				stream.readFully(result);
				stream.close();
			} catch (IOException ioex) {
				ioex.printStackTrace();
				return null;
			}
		}
		return result;
	}

	/**
	 * Gets the CRC32 value of the container used for verification.
	 * @return The container's CRC32 value.
	 */
	public int getCRC() {
		return crc;
	}

}
