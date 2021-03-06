/*
* Copyright (c) 2005, Joe Desbonnet, (jdesbonnet@gmail.com)
* All rights reserved.
*
* Redistribution and use in source and binary forms, with or without
* modification, are permitted provided that the following conditions are met:
*     * Redistributions of source code must retain the above copyright
*       notice, this list of conditions and the following disclaimer.
*     * Redistributions in binary form must reproduce the above copyright
*       notice, this list of conditions and the following disclaimer in the
*       documentation and/or other materials provided with the distribution.
*     * Neither the name of the <organization> nor the
*       names of its contributors may be used to endorse or promote products
*       derived from this software without specific prior written permission.
*
* THIS SOFTWARE IS PROVIDED BY <copyright holder> ``AS IS'' AND ANY
* EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
* WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
* DISCLAIMED. IN NO EVENT SHALL <copyright holder> BE LIABLE FOR ANY
* DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
* (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
* LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
* ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
* (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
* SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/

package ie.wombat.jbdiff;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Objects;
import java.util.zip.GZIPInputStream;

/**
 * Java Binary patcher (based on bspatch by Colin Percival)
 *
 * @author Joe Desbonnet, jdesbonnet@gmail.com
 */
public class JBPatch {

	private static final String VERSION = "jbdiff-0.1.1";

	/**
	 * Run JBPatch from the command line. Params: oldfile newfile patchfile.
	 * newfile will be created.
	 * 
	 * @param arg
	 * @throws IOException
	 */
	public static void main(String[] arg) throws IOException {

		if (arg.length != 3) {
			System.err.println(VERSION);
			System.err.println("usage example: java -Xmx200m ie.wombat.jbdiff.JBPatch oldfile newfile patchfile");
		}

		File oldFile = new File(arg[0]);
		File newFile = new File(arg[1]);
		File diffFile = new File(arg[2]);

		bspatch(oldFile, newFile, diffFile);
	}

	public static void bspatch(File oldFile, File newFile, File diffFile) throws IOException {
		try (FileOutputStream fos = new FileOutputStream(newFile)) {
			bspatch(oldFile, fos, diffFile);
		}
	}

	public static void bspatch(File oldFile, OutputStream newFile, File diffFile) throws IOException {

		int oldpos, newpos;
		try (DataInputStream diffIn = new DataInputStream(new FileInputStream(diffFile))) {

			// headerMagic at header offset 0 (length 8 bytes)
			long headerMagic = diffIn.readLong();

			Objects.nonNull(headerMagic);

			// ctrlBlockLen after gzip compression at heater offset 8 (length 8 bytes)
			long ctrlBlockLen = diffIn.readLong();

			// diffBlockLen after gzip compression at header offset 16 (length 8 bytes)
			long diffBlockLen = diffIn.readLong();

			// size of new file at header offset 24 (length 8 bytes)
			int newsize = (int) diffIn.readLong();

			/*
			 * System.err.println ("newsize=" + newsize);
			 * System.err.println ("ctrlBlockLen=" + ctrlBlockLen);
			 * System.err.println ("diffBlockLen=" + diffBlockLen);
			 * System.err.println ("newsize=" + newsize);
			 */

			try (GZIPInputStream diffBlockIn = Util.newGZIPInputStream(diffFile, ctrlBlockLen + 32);
					GZIPInputStream extraBlockIn = Util.newGZIPInputStream(diffFile,
							diffBlockLen + ctrlBlockLen + 32)) {

				/*
				 * Read in old file (file to be patched) to oldBuf
				 */
				int oldsize = (int) oldFile.length();
				byte[] oldBuf = new byte[oldsize + 1];
				Util.readFromFile(oldFile, oldBuf, 0, oldsize);

				byte[] newBuf = new byte[newsize + 1];

				oldpos = 0;
				newpos = 0;
				int[] ctrl = new int[3];
				while (newpos < newsize) {

					for (int i = 0; i <= 2; i++) {
						ctrl[i] = diffIn.readInt();
						// System.err.println (" ctrl[" + i + "]=" + ctrl[i]);
					}

					if (newpos + ctrl[0] > newsize) {
						System.err.println("Corrupt patch\n");
						return;
					}

					/*
					 * Read ctrl[0] bytes from diffBlock stream
					 */

					if (!Util.readFromStream(diffBlockIn, newBuf, newpos, ctrl[0])) {
						System.err.println("error reading from diffIn");
						return;
					}

					for (int i = 0; i < ctrl[0]; i++) {
						if ((oldpos + i >= 0) && (oldpos + i < oldsize)) {
							newBuf[newpos + i] += oldBuf[oldpos + i];
						}
					}

					newpos += ctrl[0];
					oldpos += ctrl[0];

					if (newpos + ctrl[1] > newsize) {
						System.err.println("Corrupt patch");
						return;
					}

					if (!Util.readFromStream(extraBlockIn, newBuf, newpos, ctrl[1])) {
						System.err.println("error reading from extraIn");
						return;
					}

					newpos += ctrl[1];
					oldpos += ctrl[2];
				}

				// TODO: Check if at end of ctrlIn
				// TODO: Check if at the end of diffIn
				// TODO: Check if at the end of extraIn

				newFile.write(newBuf, 0, newBuf.length - 1);
			}
		}
	}

	public static void bspatchDirect(File oldFile, File newFile, File diffFile) throws IOException {
		try (FileOutputStream fos = new FileOutputStream(newFile)) {
			bspatch(oldFile, fos, diffFile);
		}
	}

	public static void bspatchDirect(File oldFile, OutputStream newFile, File diffFile) throws IOException {

		int oldpos, newpos;

		try (DataInputStream diffIn = new DataInputStream(new FileInputStream(diffFile))) {

			// headerMagic at header offset 0 (length 8 bytes)
			long headerMagic = diffIn.readLong();

			Objects.nonNull(headerMagic);

			// ctrlBlockLen after gzip compression at heater offset 8 (length 8 bytes)
			long ctrlBlockLen = diffIn.readLong();

			// diffBlockLen after gzip compression at header offset 16 (length 8 bytes)
			long diffBlockLen = diffIn.readLong();

			// size of new file at header offset 24 (length 8 bytes)
			int newsize = (int) diffIn.readLong();

			/*
			 * System.err.println ("newsize=" + newsize);
			 * System.err.println ("ctrlBlockLen=" + ctrlBlockLen);
			 * System.err.println ("diffBlockLen=" + diffBlockLen);
			 * System.err.println ("newsize=" + newsize);
			 */

			try (DataInputStream diffBlockIn = Util.newDataInputStream(diffFile, ctrlBlockLen + 32);
					DataInputStream extraBlockIn = Util.newDataInputStream(diffFile,
							diffBlockLen + ctrlBlockLen + 32)) {

				/*
				 * Read in old file (file to be patched) to oldBuf
				 */
				int oldsize = (int) oldFile.length();
				byte[] oldBuf = new byte[oldsize + 1];
				try (DataInputStream oldIn = new DataInputStream(new FileInputStream(oldFile))) {
					oldIn.readFully(oldBuf, 0, oldsize);
				}

				oldpos = 0;
				newpos = 0;
				int[] ctrl = new int[3];
				byte[] newBuf;
				while (newpos < newsize) {

					for (int i = 0; i <= 2; i++) {
						ctrl[i] = diffIn.readInt();
						System.out.println("  ctrl[" + i + "]=" + ctrl[i]);
					}

					if (newpos + ctrl[0] > newsize) {
						System.err.println("Corrupt patch\n");
						return;
					}

					/*
					 * Read ctrl[0] bytes from diffBlock stream
					 */
					newBuf = new byte[ctrl[0]];
					diffBlockIn.readFully(newBuf);

					for (int i = 0; i < ctrl[0]; i++) {
						if ((oldpos + i >= 0) && (oldpos + i < oldsize)) {
							newBuf[i] += oldBuf[oldpos + i];
						}
					}
					newFile.write(newBuf);

					newpos += ctrl[0];
					oldpos += ctrl[0];

					if (newpos + ctrl[1] > newsize) {
						System.err.println("Corrupt patch");
						return;
					}

					newBuf = new byte[ctrl[1]];
					extraBlockIn.readFully(newBuf, 0, ctrl[1]);
					newFile.write(newBuf);
					newpos += ctrl[1];
					oldpos += ctrl[2];
				}

				// TODO: Check if at end of ctrlIn
				// TODO: Check if at the end of diffIn
				// TODO: Check if at the end of extraIn
			}

			// newFile.write(newBuf,0,newBuf.length-1);
		}
	}
}
