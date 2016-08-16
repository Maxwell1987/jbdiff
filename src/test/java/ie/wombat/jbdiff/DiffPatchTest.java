/**
 * $Id:$
 * $URL:$
 *
 * (c) EFKON Germany GmbH
 */
package ie.wombat.jbdiff;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import junit.framework.TestCase;

/**
 * @author veikko.werner
 *
 */
public class DiffPatchTest extends TestCase {

	private File test1File;
	private File test2File;

	public void testMin() throws IOException {
		use("min1.bin", "min2.bin");
		doTest();
	}

	public void testObj() throws IOException {
		use("obj1.bin", "obj2.bin");
		doTest();
	}

	/**
	 * @throws IOException
	 *
	 */
	private void doTest() throws IOException {
		File diffFile1 = File.createTempFile("bsdiff1", "tmp");
		File diffFile2 = File.createTempFile("bsdiff2", "tmp");
		JBDiff.bsdiff(test1File, test2File, diffFile1);
		JBDiff.bsdiff(test1File, test2File, diffFile2);
		File result1 = File.createTempFile("bspatch1", "tmp");
		File result2 = File.createTempFile("bspatch2", "tmp");
		JBPatch.bspatchDirect(test1File, result1, diffFile1);
		JBPatch.bspatch(test1File, result2, diffFile2);

		try (InputStream one = new FileInputStream(test2File);
				InputStream two = new FileInputStream(result1)) {
			int length = (int) test2File.length();
			assertEquals("Wrong resulting file size.", length, result1.length());
			for (int i = 0; i < length; i++)
				assertEquals("Wrong result at offset(int) " + i, one.read(), two.read());
		}

		try (InputStream one = new FileInputStream(test2File);
				InputStream two = new FileInputStream(result2)) {
			int length = (int) test2File.length();
			assertEquals("Wrong resulting file size.", length, result2.length());
			for (int i = 0; i < length; i++)
				assertEquals("Wrong result at offset(int) " + i, one.read(), two.read());
		}
	}

	private void use(String f1, String f2) {
		URL l1 = getClass().getClassLoader().getResource(f1);
		URL l2 = getClass().getClassLoader().getResource(f2);
		test1File = new File(l1.getPath());
		test2File = new File(l2.getPath());
	}

}
