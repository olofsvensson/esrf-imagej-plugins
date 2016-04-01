/*
 ***********************************************************************
 *                                                                     *
 * EDF Stack Reader                                                    *
 *                                                                     *
 * Written and maintained by Olof Svensson (svensson@esrf.fr)          *
 *                                                                     *
 * Petr Mikulik (mikulik@physics.muni.cz) has contributed code for     *
 * reading EDF/EHF format headers                                      *
 *                                                                     *
 *                                                                     *
 * Changes:                                                            *
 *                                                                     *
 *  1. 4. 2016  Olof Svensson                                          *
 *              - Reformatted the code                                 *
 *              - Added support for more EDF data types                *
 *                                                                     *
 * 04.01. 2012  Olof Svensson                                          *
 *              - First version of the stack reader                    *
 *                                                                     *
 ***********************************************************************
 */

import java.io.*;
import java.util.*;

import ij.*;
import ij.io.*;
import ij.plugin.PlugIn;
import ij.process.ImageProcessor;

/** This plugin reads a stack of EDF images **/
public class EDF_StackReader implements PlugIn {

	private static final String EDF_STACK_READER_VERSION = "January 2012";

	// Default values for an image
	private int width = 512;
	private int height = 512;
	private int offset = 0;
	private int nImages = 1;
	private int gapBetweenImages = 0;
	private boolean whiteIsZero = false;
	private boolean intelByteOrder = false;
	private int fileType = FileInfo.GRAY16_UNSIGNED;

	public void run(String arg) {
		String directory, fileName, type = "EDF";
		ImageStack newStack = null;

		// Show about box if called from ij.properties
		if (arg.equals("about")) {
			showAbout();
			return;
		}

		// Open first image
		OpenDialog od = new OpenDialog("Choose first image", arg);
		directory = od.getDirectory();
		fileName = od.getFileName();
		if (fileName == null)
			return;

		// Extract suffix
		int dot = fileName.lastIndexOf(".");
		String suffix = fileName.substring(dot + 1);
		if (!suffix.equals("edf")) {
			IJ.error("Not an EDF image!");
		}

		// Extract prefix
		int index = dot - 1;
		boolean foundLastIndex = false;
		while (!foundLastIndex) {
			char digit = fileName.charAt(index);
			if ((digit < '0') || (digit > '9')) {
				foundLastIndex = true;
			} else {
				index -= 1;
			}
		}

		String prefix = fileName.substring(0, index + 1);

		// Loop through all the images in the directory
		File path = new File(directory);
		File files[];

		files = path.listFiles();
		Arrays.sort(files, new Comparator<File>() {
			public int compare(File f1, File f2) {
				return f1.getName().toString()
						.compareTo(f2.getName().toString());
			}
		});

		for (File edfFile : files) {
			// Check that the file starts with the prefix
			if (edfFile.getName().contains(prefix)) {
				IJ.log("Reading image: " + edfFile.getName());
				// The following code is "borrowed" from ij.plugin.Raw
				FileInfo fileInfo = new FileInfo();
				fileInfo.fileFormat = FileInfo.RAW;
				fileInfo.fileName = edfFile.getName();
				fileInfo.directory = edfFile.getParent().toString();
				fileInfo.width = width;
				fileInfo.height = height;
				fileInfo.offset = offset;
				fileInfo.nImages = nImages;
				fileInfo.gapBetweenImages = gapBetweenImages;
				fileInfo.intelByteOrder = intelByteOrder;
				fileInfo.whiteIsZero = whiteIsZero;
				fileInfo.fileType = fileType;

				parseESRFDataFormatHeader(type, edfFile, fileInfo);

				// Leave the job to ImageJ for actually reading the image
				FileOpener fileOpener = new FileOpener(fileInfo);
				ImagePlus imp = fileOpener.open(false);

				if (newStack == null) {
					newStack = new ImageStack(fileInfo.width, fileInfo.height);
				}
				ImageProcessor ip = imp.getChannelProcessor();
				newStack.addSlice(edfFile.getName().toString(), ip);
			}
		}
		if (newStack != null) {
			ImagePlus newImage = new ImagePlus("The EDF stack", newStack);
			newImage.show();
		}
	}

	private void parseESRFDataFormatHeader(String type, File f,
			FileInfo fileInfo) {
		RandomAccessFile in;
		char h;
		int headerSize, headerStart, noBrackets, i, iParam = 0;
		byte[] header;
		String key, param, token, headerString;

		try {
			in = new RandomAccessFile(f, "r");
			headerSize = 0;
			noBrackets = 0;
			do {
				headerStart = headerSize;
				in.seek(headerStart);
				do {
					h = getChar(in);
					headerSize += 1;
					if (h == '{')
						noBrackets++;
					else if (h == '}')
						noBrackets--;
				} while (noBrackets != 0);
				// Read header
				header = new byte[headerSize - headerStart + 1];
				in.seek(headerStart);
				in.readFully(header);
				headerString = new String(header);
			} while (type.equals("EHF")
					&& (headerString.indexOf("EDF_DataBlockID") < 0));
		} catch (IOException ex) {
			IJ.log("IOException caught: " + ex);
			return;
		}
		if (IJ.debugMode)
			IJ.log("ImportDialog: " + fileInfo);

		// Set offset
		fileInfo.offset = headerSize + 1;

		// Extract information from header
		StringTokenizer st = new StringTokenizer(headerString, ";");
		while (st.hasMoreTokens()) {
			token = st.nextToken();
			i = token.indexOf("=");
			if (i <= 0)
				continue;
			key = token.substring(1, i - 1).trim();
			param = token.substring(i + 1).trim();
			// IJ.log(key + ": " + param);

			// For debugging - output to stdout is persistent:
			// System.out.println("DOING |"+key+"|: |" + param+"|");

			try {
				iParam = Integer.valueOf(param).intValue();
				// double dParam = Integer.valueOf(param).doubleValue();
			} catch (NumberFormatException numberformatexception) {
			}
			;
			if (key.equals("EDF_BinaryFileName")) {
				fileInfo.fileName = param;
				System.out.println("DEBUG: " + key + " = " + param);
				continue;
			}
			if (key.equals("EDF_BinaryFilePosition")) {
				fileInfo.offset = iParam;
				continue;
			}
			if (key.equals("Dim_1")) {
				fileInfo.width = iParam;
				continue;
			}
			if (key.equals("Dim_2")) {
				fileInfo.height = iParam;
				continue;
			}
			if (key.equals("DataType")) {
				// Long and integer
				if (param.equals("SignedLong") || param.equals("SignedInteger")) {
					fileInfo.fileType = FileInfo.GRAY32_INT;
				} else if (param.equals("UnsignedLong")
						|| param.equals("UnsignedInteger")) {
					fileInfo.fileType = FileInfo.GRAY32_UNSIGNED;
					// Short
				} else if (param.equals("SignedShort")) {
					fileInfo.fileType = FileInfo.GRAY16_SIGNED;
				} else if (param.equals("UnsignedShort")) {
					fileInfo.fileType = FileInfo.GRAY16_UNSIGNED;
					// Byte
				} else if (param.equals("SingedByte")
						|| param.equals("UnsignedByte")
						|| param.equals("UnsignedChar")) {
					fileInfo.fileType = FileInfo.GRAY8;
					// Float
				} else if (param.equals("Float") || param.equals("FloatValue")) {
					fileInfo.fileType = FileInfo.GRAY32_FLOAT;
				} else {
					IJ.log("WARNING: unknown data type " + param);
				}
				continue;
			}
			if (key.equals("ByteOrder")) {
				fileInfo.intelByteOrder = param.equals("LowByteFirst");
			}
		}
		return;
	}

	int getShort(RandomAccessFile in) throws IOException {
		int b1 = in.read();
		int b2 = in.read();
		if (intelByteOrder)
			return ((b2 << 8) + b1);
		else
			return ((b1 << 8) + b2);
	}

	char getChar(RandomAccessFile in) throws IOException {
		int b = in.read();
		return (char) b;
	}

	void showAbout() {
		String message = "This plugin reads a stack of ESRF EDF images.\n";
		message += " \n"
				+ "This plugin is written and maintained by Olof Svensson, ESRF.\n"
				+ "Please send suggestions or bug reports to svensson@esrf.fr.\n"
				+ " \n"
				+ "Petr Mikulik (mikulik@physics.muni.cz) has contributed code to\n"
				+ "the EFD/EDH format header reader.\n \n" + "This is version "
				+ EDF_STACK_READER_VERSION + "\n";
		IJ.showMessage("About EDF Stack Reader...", message);
	}
}

// eof ESRF_Reader.java

