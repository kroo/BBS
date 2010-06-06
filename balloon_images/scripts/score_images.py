import os,sys,glob

boost_option = 'all_100_c'

working_path = 'C:\\Users\\Alex\\BBS\\balloon_images\\road_scored_pics'
os.chdir(working_path)
temp_path = 'C:\\Users\\Alex\\BBS\\balloon_images\\road_boosted_data_' + boost_option
stem = os.path.basename(working_path)

for file in os.listdir(working_path):
	if 'scored' in file:
		continue
	
	print file
	print os.path.splitext(file)[1]
	if True:
		# call MATLAB to score images
		data = os.path.join(temp_path,stem + '.data')
		scored_image = 'scored_%s_%s'%(boost_option,file)
		scoring_path = os.path.join(temp_path,'*.m')
		print scoring_path
		scoring_function = glob.glob(scoring_path)
		if len(scoring_function) < 1:
			sys.stderr.write("No scoring function found.  Run jboost with -m\n");
			sys.exit(1);
		scoring_function = os.path.splitext(os.path.basename(scoring_function[0]))[0]
		###scoring_function = scoring_function[0]
		print 'scoring function = ',scoring_function
		matlab_command = "path('%s',path);apply_classifier(@%s,'%s','%s');exit"%(temp_path,scoring_function,file,scored_image)
		matlab_options = '-nodisplay -nodesktop -nosplash -wait'
		os.system('matlab -maci %s -r "%s" &'%(matlab_options,matlab_command))