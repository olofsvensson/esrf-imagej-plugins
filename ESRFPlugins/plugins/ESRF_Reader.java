/*
 ***********************************************************************
 *                                                                     *
 * ESRF image format reader                                            *
 *                                                                     *
 * Written and maintained by Olof Svensson (svensson@esrf.fr)          *
 *                                                                     *
 * Petr Mikulik (mikulik@physics.muni.cz) has contributed code for     *
 * reading EDF/EHF format headers                                      *
 *                                                                     *
 *                                                                     *
 * Changes:                                                            *
 *                                                                     *
 * 18. 2. 2016  Olof Svensson                                          *
 *              - Added support for "SignedInteger"                    *
 *                                                                     *
 * 04.01. 2012  Olof Svensson                                          *
 *              - Cleanup of code                                      *
 *                                                                     *
 * 29.11. 2001  Petr Mikulik                                           *
 *              - support "UnsignedByte"                               *
 *                                                                     *
 * 21. 8. 2001  Petr Mikulik                                           *
 *              - support "Float" as well as "FloatValue"              *
 *              - support "UnsignedChar" and "SignedShort"             *
 *              - bugfix for typo in reading "ByteOrder"               *
 *              - added ESRF_READER_VERSION; show it in the about box  *
 *                                                                     *
 ***********************************************************************
 */

import java.io.*;
import java.util.*;

import ij.*;
import ij.io.*;
import ij.gui.*;
import ij.plugin.PlugIn;

/** This plugin reads image formats used at the ESRF **/
public class ESRF_Reader implements PlugIn {

	private static final String ESRF_READER_VERSION = "January 2012";

	// Supported types
	String[] types = { "EDF", "EHF" };
	String[] typesDescription = { "ESRF Data Format", "ESRF data Header Format" };

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
		OpenDialog od;
		FileOpener fileOpener;
		File f;

		// Show about box if called from ij.properties
		if (arg.equals("about")) {
			showAbout();
			return;
		}

		GenericDialog gd = new GenericDialog("ESRF Reader", IJ.getInstance());
		gd.addChoice("Open image of type:", types, type);
		gd.showDialog();
		if (gd.wasCanceled())
			return;
		type = gd.getNextChoice();

		// Open file
		od = new OpenDialog("Open " + type + " image", arg);
		directory = od.getDirectory();
		fileName = od.getFileName();
		if (fileName == null)
			return;
		IJ.log("Opening " + type + " image " + directory + fileName);
		f = new File(directory + fileName);

		// The following code is "borrowed" from ij.plugin.Raw
		FileInfo fileInfo = new FileInfo();
		fileInfo.fileFormat = FileInfo.RAW;
		fileInfo.fileName = fileName;
		fileInfo.directory = directory;
		fileInfo.width = width;
		fileInfo.height = height;
		fileInfo.offset = offset;
		fileInfo.nImages = nImages;
		fileInfo.gapBetweenImages = gapBetweenImages;
		fileInfo.intelByteOrder = intelByteOrder;
		fileInfo.whiteIsZero = whiteIsZero;
		fileInfo.fileType = fileType;

		// Try to get image size and no images from header
		if (type.equals("EDF") || type.equals("EHF")) {
			parseESRFDataFormatHeader(type, f, fileInfo);
		}

		// Leave the job to ImageJ for actually reading the image
		fileOpener = new FileOpener(fileInfo);
		fileOpener.open();
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
			IJ.log(key + ": " + param);

			// For debugging - output to stdout is persistent:
			// System.out.println("DOING |"+key+"|: |" + param+"|");

			try {
				iParam = Integer.valueOf(param).intValue();
				//double dParam = Integer.valueOf(param).doubleValue();
			} catch (NumberFormatException numberformatexception) {
			};
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
				if (param.equals("UnsignedLong")
						|| param.equals("UnsignedInteger")
						|| param.equals("SignedInteger")) {
					fileInfo.fileType = FileInfo.GRAY32_INT;
				} else if (param.equals("UnsignedShort")) {
					fileInfo.fileType = FileInfo.GRAY16_UNSIGNED;
				} else if (param.equals("Float") || param.equals("FloatValue")) {
					fileInfo.fileType = FileInfo.GRAY32_FLOAT;
				} else if (param.equals("UnsignedByte")
						|| param.equals("UnsignedChar")) {
					fileInfo.fileType = FileInfo.GRAY8;
				} else if (param.equals("SignedShort")) {
					fileInfo.fileType = FileInfo.GRAY16_SIGNED;
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
		String message = "This plugin reads image formats commonly used the ESRF.\n"
				+ "It can currently read the following formats:\n \n";
		for (int i = 0; i < types.length; i++) {
			message += types[i] + ":   " + typesDescription[i] + "\n";
		}
		message += " \n"
				+ "This plugin is written and maintained by Olof Svensson, ESRF.\n"
				+ "Please send suggestions or bug reports to svensson@esrf.fr.\n"
				+ " \n"
				+ "Petr Mikulik (mikulik@physics.muni.cz) has contributed code to\n"
				+ "the EFD/EDH format header reader.\n \n" + "This is version "
				+ ESRF_READER_VERSION + "\n";
		IJ.showMessage("About ESRF Edf+Hdf Reader...", message);
	}
}

// eof ESRF_Reader.java

