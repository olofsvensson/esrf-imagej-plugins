macro 'EdfMovie' {
/*
# 	EdfMovie - make a movie of a series of EDF and write it out as an AVI file
#
#	author - andy.gotz@esrf.fr
#
#	date - 7 March 2006
#
#	version - 1.0
#
# 	input arguments : directory,start,stop,step,width,height
#
# 	example : /data/opid11/external/me1015/D30_30_,0,490,10,512,512
#
#	running : to run this macro from the command line use 
#
#		run("EdfMovie", "/data/opid11/external/me1015/D30_30_,0,490,10,512,512")
#
*/

	/* split arg to find directory, start, stop, step */
	parameters = getArgument();
	if (parameters!="") {
		parm_index = 0;
		comma_index = indexOf(parameters,",",parm_index);
		parm1 = substring(parameters,parm_index,comma_index);
		print("directory = ",parm1);
		parm_index = comma_index+1;
		comma_index = indexOf(parameters,",",comma_index+1);
		parm2 = substring(parameters,parm_index,comma_index);
		print("start = ",parm2);
		parm_index = comma_index+1;
		comma_index = indexOf(parameters,",",comma_index+1);
		parm3 = substring(parameters,parm_index,comma_index);
		print("stop = ",parm3);
		parm_index = comma_index+1;
		comma_index = indexOf(parameters,",",comma_index+1);
		parm4 = substring(parameters,parm_index,comma_index);
		print("step = ",parm4);
		parm_index = comma_index+1;
		comma_index = indexOf(parameters,",",comma_index+1);
		parm5 = substring(parameters,parm_index,comma_index);
		print("width = ",parm5);
		parm_index = comma_index+1;
		parm6 = substring(parameters,parm_index,lengthOf(parameters));
		print("height = ",parm6);
	}
	else {
		exit();
	}

	directory=parm1;
        if (substring(directory,lengthOf(directory)-1,lengthOf(directory)) != "/") {
                directory = directory+"/";
        }
	directory=substring(directory, 0 , lengthOf(directory)-1);
	stem = substring(directory, lastIndexOf(directory,"/"), lengthOf(directory));
	movie_start = parseInt(parm2);
	movie_stop =  parseInt(parm3);
	movie_step =  parseInt(parm4);
	movie_width =  parseInt(parm5);
	movie_height =  parseInt(parm6);
	if (movie_step < 1) {
		movie_step = 1
	}
	for (i = movie_start; i < movie_stop; i = i+movie_step) {
		if (i < 10 )  {
			z = "000";
		}
		if (i >= 10 && i < 100) {
			z="00";
		}
		if (i >= 100 && i < 1000) {
			z = "0";
		}
		if (i >= 1000) {
			z = "";
		}
		file = directory+"/"+stem+z+i+".edf";
		run("EDF Reader", "open="+file);
		run("Size...", "width="+movie_width+" height="+movie_height+" interpolate");
	}
	print("convert images to stack");
	run("Convert Images to Stack");
	run("Fire");
	setMinAndMax(1000, 1500);
	movie_file = directory+stem+parm2+"_"+parm3+"_"+parm4;
	print("save stack as ",movie_file);
	saveAs("AVI... ", movie_file);
	close();
}
