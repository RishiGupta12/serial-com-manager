/**
 * Author : Rishi Gupta
 * 
 * This file is part of 'serial communication manager' library.
 *
 * The 'serial communication manager' is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by the Free Software 
 * Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * The 'serial communication manager' is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A 
 * PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with serial communication manager. If not, see <http://www.gnu.org/licenses/>.
 */
package com.embeddedunveiled.serial;

import java.io.IOException;
import java.io.InputStream;

import com.embeddedunveiled.serial.SerialComManager.SMODE;

/**
 * <p>This class represents an input stream of bytes which is received from serial port.</p>
 * 
 * 
 * <p>Application design should make sure that the port is not closed if there exist a read method
 * which is blocked (waiting for data byte) on the same port.</p>
 * <p>Application design should make sure that the port is not closed if there exist a read method
 * which is blocked (waiting for data byte) on the same port.</p>
 * 
 * <p>Advance applications may fine tune the timing behavior using fineTuneRead() API.</p>
 */
public final class SerialComInByteStream extends InputStream {

	private SerialComManager scm = null;
	private long handle = 0;
	private boolean isOpened = false;
	private boolean isBlocking = false;

	/**
	 * <p>Allocates a new SerialComInByteStream object.</p>
	 * @param scm instance of SerialComManager class with which this stream will associate itself
	 * @param handle handle of the serial port on which to read data bytes
	 * @param streamMode indicates blocking or non-blocking behavior of stream
	 * @throws SerialComException if serial port can not be configured for specified read behavior
	 */
	public SerialComInByteStream(SerialComManager scm, long handle, SMODE streamMode) throws SerialComException {
		this.scm = scm;
		this.handle = handle;
		isOpened = true;
		
		if(streamMode.getValue() == 1) {
			// For windows blocking read method is called while for others (unix-like) VMIN/VTIME is set.
			if(scm.getOSType() != SerialComManager.OS_WINDOWS) {
				scm.fineTuneRead(handle, 1, 0, 0, 0, 0);
			}
			isBlocking = true;
		}
	}

	/**
	 * <p>Returns an estimate of the minimum number of bytes that can be read from this input stream
	 * without blocking by the next invocation of a method for this input stream.</p>
	 * 
	 * @return an estimate of the minimum number of bytes available for reading
	 * @throws IOException if an I/O error occurs.
	 */
	@Override
	public int available() throws IOException {
		if(isOpened != true) {
			throw new IOException("The byte stream has been closed");
		}
		
		int[] numBytesAvailable = new int[2];
		try {
			numBytesAvailable = scm.getByteCountInPortIOBuffer(handle);
		} catch (SerialComException e) {
			throw new IOException(e.getExceptionMsg());
		}
		return numBytesAvailable[0];
	}
	
	/**
	 * <p>This method releases the InputStream object associated with the operating handle.</p>
	 * <p>To actually close the port closeComPort() method should be used.</p>
	 */
	@Override
	public void close() throws IOException {
		if(isOpened != true) {
			throw new IOException("The byte stream has been closed");
		}
		scm.destroyInputByteStream(this);
		isOpened = false;
	}
	
	/**
	 * <p>scm does not support mark and reset of input stream. If required, it can be developed at application level.</p>
	 * 
	 */
	@Override
	public void mark(int a) {
	}

	/**
	 * <p>scm does not support mark and reset of input stream. If required, it can be developed at application level.</p>
	 * 
	 * @return always returns false
	 */
	@Override
	public boolean markSupported() {
		return false;
	}

	/**
	 * <p>Reads the next byte of data from the input stream. The value byte is returned as an int in the range 0 to 255.</p>
	 * 
	 * <p>For blocking mode this method returns the next byte of data. For non-blocking mode this method returns the next byte 
	 * of data if it is available otherwise -1 if there is no data at serial port.</p>
	 * 
	 * @return the next byte of data or -1
	 * @throws IOException if an I/O error occurs or if input stream has been closed
	 */
	@Override
	public int read() throws IOException {
		if(isOpened != true) {
			throw new IOException("The byte stream has been closed");
		}
		
		byte[] data = new byte[1];
		try {
			if(isBlocking == true) {
				data = scm.readBytesBlocking(handle, 1);
				if(data != null) {
					return (int)data[0];
				}else {
					throw new IOException("Unknown error occured");
				}
			}else {
				data = scm.readBytes(handle, 1);
				if(data != null) {
					return (int)data[0];
				}else {
					return -1;
				}
			}
		}catch (SerialComException e) {
			throw new IOException(e.getExceptionMsg());
		}
	}
	
    /**
     * <p>Reads some number of bytes from the input stream and stores them into the buffer array b. The number of bytes actually read is
     * returned as an integer.  This method blocks until input data is available or an exception is thrown.</p>
     *
     * <p>If the length of b is zero, then no bytes are read and 0 is returned; otherwise, there is an attempt to read at least one byte.</p>
     *
     * <p>The first byte read is stored into element b[0], the next one into b[1], and so on. The number of bytes read is, at most, equal to
     * the length of b. Let k be the number of bytes actually read; these bytes will be stored in elements b[0] through <code>b[</code><i>k</i><code>-1]</code>,
     * leaving elements <code>b[</code><i>k</i><code>]</code> through <code>b[b.length-1]</code> unaffected.</p>
     *
     * <p>The read(b) method for class SerialComInByteStream has the same effect as : read(b, 0, b.length) </p>
     *
     * @param  b the buffer into which the data is read.
     * @return the total number of bytes read into the buffer
     * @throws IOException if an I/O error occurs or if input stream has been closed
     * @throws NullPointerException  if <code>b</code> is <code>null</code>.
     */
	@Override
    public int read(byte b[]) throws IOException {
        return read(b, 0, b.length);
    }
	
    /**
     * <p>Reads up to len bytes of data from the input stream into an array of bytes. An attempt is made to read
     * as many as len bytes, but a smaller number may be read. The number of bytes actually read is returned as 
     * an integer.</p>
     * 
     * <p>If len is zero, then no bytes are read and 0 is returned; otherwise, there is an attempt to read at 
     * least one byte and stored into b.</p>
     * 
     * <p>The first byte read is stored into element b[off], the next one into b[off+1], and so on. The number 
     * of bytes read is, at most, equal to len. Let k be the number of bytes actually read; these bytes will be 
     * stored in elements b[off] through b[off+k-1], leaving elements b[off+k] through b[off+len-1] unaffected.</p>
     *  
     * <p>In every case, elements b[0] through b[off] and elements b[off+len] through b[b.length-1] are unaffected.</p>
     * 
     * <p>For blocking mode, this method blocks until input data is available or an exception is thrown. For non-blocking
     * it attempts to read data, returns data byte if read. It will return -1 if there is no data at serial port.</p>
     * 
     * @param b the buffer into which the data is read.
     * @param off the start offset in array b at which the data is written.
     * @param len the maximum number of bytes to read.
     * @return the total number of bytes read into the buffer or 0 if len is zero or -1 if there is no data (non-blocking)
     * @throws IOException if an I/O error occurs or if input stream has been closed
     * @throws NullPointerException if <code>b</code> is <code>null</code>.
     * @throws IllegalArgumentException if data is not a byte type array
     * @throws IndexOutOfBoundsException if off is negative, len is negative, or len is greater than b.length - off 
     */
	@Override
	public int read(byte b[], int off, int len) throws IOException {
		if(isOpened != true) {
			throw new IOException("The byte stream has been closed");
		}
		if(b == null) {
			throw new NullPointerException("read(), " + "null data buffer passed to read operation");
		}
		if((off < 0) || (len < 0) || (len > (b.length - off))) {
			throw new IndexOutOfBoundsException("read(), " + "index violation detected in given byte array");
		}
		if(len == 0) {
			return 0;
		}
		if(!(b instanceof byte[])) {
			throw new IllegalArgumentException("The given data array is not byte type array");
		}
		
		int i = off;
		try {
			if(isBlocking == true) {
				byte[] data = scm.readBytesBlocking(handle, len);
				if(data != null) {
					for(int x=0; x<data.length; x++) {
						b[i] = data[x];
						i++;
					}
					return data.length;
				}else {
					throw new IOException("Unknown error occured");
				}
			}else {
				byte[] data = scm.readBytes(handle, len);
				if(data != null) {
					for(int x=0; x<data.length; x++) {
						b[i] = data[x];
						i++;
					}
					return data.length;
				}else {
					return -1;
				}
			}
		}catch (SerialComException e) {
			throw new IOException(e.getExceptionMsg());
		}
    }
	
	/**
	 * <p>The scm does not support reset. If required, it can be developed at application level.</p>
	 */
	@Override
    public synchronized void reset() throws IOException {
    }

	/**
	 * <p>The scm does not support skip. If required, it can be developed at application level.</p>
	 * 
	 * @param number of bytes to skip
	 * @return always returns 0
	 */
	@Override
	public long skip(long number) {
		return 0;
	}
}
