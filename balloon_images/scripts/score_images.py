import os,sys,glob

working_path = os.getcwd()
temp_path = 'C:\\Users\\Alex\\BBS\\balloon_images\\boosted_data'
stem = os.path.basename(working_path)

for file in os.listdir(working_path):
	print file
	print os.path.splitext(file)[1]
	if os.path.splitext(file)[1] == '.jpg':
		# call MATLAB to score images
		data = os.path.join(temp_path,stem + '.data')
		scored_image = 'scored_' + file
		###scoring_function = 'CAR_scoring_function'
		scoring_path = os.path.join(temp_path,'*.m')
		print scoring_path
		scoring_function = glob.glob(scoring_path)
		if len(scoring_function) < 1:
			sys.stderr.write("No scoring function found.  Run jboost with -m\n");
			sys.exit(1);
		scoring_function = scoring_function[0]
		matlab_command = "path('temp2',path);apply_classifier(%s,'%s','%s')"%(scoring_function,file,scored_image)
		matlab_options = '-nodisplay -nodesktop -nosplash -wait'
		os.system('matlab -maci %s -r "%s" &'%(matlab_options,matlab_command))