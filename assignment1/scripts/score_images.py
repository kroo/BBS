import os,sys

working_path = os.getcwd()
temp_path = os.path.join(working_path,'temp2')
stem = os.path.basename(working_path)

for file in os.listdir(working_path):
	print file
	print os.path.splitext(file)[1]
	if os.path.splitext(file)[1] == '.jpg':
		# call MATLAB to score images
		data = os.path.join(temp_path,stem + '.data')
		scored_image = 'scored_' + file
		matlab_command = "path('temp2',path);apply_classifier('%s','%s')"%(file,scored_image)
		matlab_options = '-nodisplay -nodesktop -nosplash -wait'
		os.system('matlab -maci %s -r "%s" &'%(matlab_options,matlab_command))