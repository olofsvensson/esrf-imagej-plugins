//
//ESRF_Writer.java
//
// EC ApR/2009
//
// Test version
// 


import ij.*;
import ij.plugin.PlugIn;
import ij.io.*;
import java.awt.*;
import java.io.*;
import java.util.Date;
    
public class ESRF_Writer implements PlugIn {

  private ImagePlus imp;
  private static String defaultDirectory = null;
  
  public void run(String arg) {
    imp = WindowManager.getCurrentImage();
    if (imp==null)
      {IJ.noImage(); return;}
    saveAsEDF();
  }
  

    
  public boolean saveAsEDF() {
    SaveDialog sd = new SaveDialog("Save as EDF file", imp.getTitle(), ".edf");
    String directory = sd.getDirectory();
    String name = sd.getFileName();
    //IJ.write("Saving to "+directory+name);
    if (name==null)
      return false;
    else
      return saveAsEDF(directory+name);
	
  }
	
	
  public boolean saveAsEDF(String path) {
    FileInfo fi = imp.getFileInfo();
    //fi.nImages = 1;
    try {
    ImageWriter file = new ImageWriter(fi);
    OutputStream out = new BufferedOutputStream(new FileOutputStream(path));
    OutputStreamWriter osw = new OutputStreamWriter(out);
    
	//String DATE=getDateTime();
	Date now = new Date();  
    String DATE = String.valueOf(now);	
	DATE="Date = "+DATE+";\n";
	
	String START = "{\n";
	String END = "}\n";
	String HEADER = "HeaderID = EH:000001:000000:000000;\n";
	String IMAGE_N="Image = 1;\n";
	String BYTE_ORDER="ByteOrder = LowByteFirst;\n";
        //String BYTE_ORDER="ByteOrder = HighByteFirst;\n";
	String DATA_TYPE="";
	String DIM_1;
	String DIM_2;
	String SIZE;
	String TOTAL="";
	String BUFF="";
	int BYTES_PER_POINT=4;
	int HEADER_BLOCK_LENGTH = 1024;
	
	String myString = Integer.toString(fi.fileType);
	IJ.write(myString);
    switch( fi.fileType ) 
	
	{
	
	    //case FileInfo.intelByteOrder : 
		//BYTE_ORDER="ByteOrder = LowByteFirst; \n";
		//break;
		
	    case FileInfo.GRAY16_SIGNED: 
		DATA_TYPE = "DataType = SignedShort;\n"; 
		BYTES_PER_POINT=2;
		IJ.write("SignedShort");
		break;
		
	    case FileInfo.GRAY16_UNSIGNED: 
		DATA_TYPE = "DataType = UnsignedShort;\n"; 
		IJ.write("UnsignedShort");
		BYTES_PER_POINT=2;
		break;
		
	    case FileInfo.GRAY32_FLOAT: 
		DATA_TYPE="DataType = FloatValue;\n";
		IJ.write("FloatValue");
		BYTES_PER_POINT=4;
		break;
		
	    case FileInfo.GRAY32_INT: 
		DATA_TYPE ="DataType = SignedLong;\n"; 
		IJ.write("SignedLong");
		BYTES_PER_POINT=4;
		break;
		
	    case FileInfo.GRAY32_UNSIGNED: 
		DATA_TYPE ="DataType = UnsignedLong;\n"; 
		IJ.write("UnsignedLong");
		BYTES_PER_POINT=4;
		break;		
		
	    case FileInfo.GRAY8: 
		DATA_TYPE ="DataType = UnsignedByte;\n"; 
		IJ.write("UnsignedByte");
		BYTES_PER_POINT=1;
		break;
		
	    case FileInfo.GRAY64_FLOAT: 
		DATA_TYPE ="DataType = DoubleValue;\n"; 
		IJ.write("DoubleValue");
		BYTES_PER_POINT=1;
		break;

		
	    default:
		throw new IOException("Unknown data type");
    }
	
	//IJ.write(DATA_TYPE);
	
    DIM_1="Dim_1 = " + fi.width + ";\n";
	DIM_2="Dim_2 = " + fi.height + ";\n";
	SIZE="Size = " + fi.height*fi.width*BYTES_PER_POINT  + ";\n";
	
	TOTAL=START+
	//DATE+
	HEADER+
	IMAGE_N+
	BYTE_ORDER+
	DATA_TYPE+
	DIM_1+
	DIM_2+
	SIZE;
	
	int lt = TOTAL.length();
	int le=END.length();
	int PAD_SIZE = HEADER_BLOCK_LENGTH -lt-le;
	
	char[] ia = new char[PAD_SIZE];
		for (int i = 0; i < ia.length; i++)
			ia[i] = ' ';
			
	BUFF = String.valueOf(ia);

	osw.write(TOTAL+BUFF+END);

    osw.flush();
    file.write(out);
    osw.close();
    out.close();

    } 
	catch (IOException e) 
	{
      IJ.error("Error writing EDF file header");
      return false;
    }
	
    imp.changes = false;
    IJ.showStatus("");
    return true;
  }
  
}