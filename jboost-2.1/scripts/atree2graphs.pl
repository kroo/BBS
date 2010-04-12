#! /usr/bin/perl



# generate graphs from the atree log files. The first parameter is the

# path to the run info file generates a postscript file in the same

# directory as the info file with the name "{stub}.{M/R}.graphs.ps"

# where {stub} is the stub of the info file name (the name up to the

# ".output") and M/R indicate whether it is precision/Recall or

# Margins



if($ARGV[0] =~ m&^(.*)/([^/]*)$&) {
    $dirname = $1;
    $infofilename = $2;}
elsif($ARGV[0] =~ m&^([^/]*)$&) {
    $dirname = ".";
    $infofilename = $1;}
else {die "could not parse filename $ARGV[0]";}

$mode = "M";

if(defined $ARGV[1]) {$mode = $ARGV[1];} # none = margins, "R" = precision recall



$infofilename =~ /^(.*)\.info/;

$namestub = "$1.$mode";



#opening info file

open(INFO,"$dirname/$infofilename") || die "could not open file $dirname/$infofilename";



while(<INFO>) {

    if(/Configuration parameters/) {last;}

}

while($line=<INFO>) {		# collect configuration parameters

    if($line =~ /^\s*$/) { last;} # for the graph title

    chop $line;

    $graphtitle .= "$line ";

}



while(<INFO>) {			# skip lines till filenames

    if(/FILENAMES/) {last;}

}



while(<INFO>) {		# read the names of the files

    if(/^(\S+)\s*\=\s*(\S+)/) {

	$filename{$1} = $2;

    } else {

	last;}

}



foreach $name (keys %filename) {

    print "$name = $filename{$name}\n";

}



#opening sample file

$filename = $filename{"samplingOutputFileName"};

open(SAMPLE,$filename) || die "could not open file $filename";

$line = <SAMPLE>;

$line =~ /train labels, elements=(\d*)/

    || die "bad first line in $filename:\n$line";

$trainSize=$1;

for($i=0; $i<$trainSize; $i++) {

    $trainLabels[$i]=<SAMPLE>+0.0;

}

$line = <SAMPLE>;

$line =~ /test labels, elements=(\d*)/

    || die "bad first line in $filename:\n$line";

$testSize=$1;

for($i=0; $i<$testSize; $i++) {

    $testLabels[$i]=<SAMPLE>+0.0;

}

close(SAMPLE);



while(<INFO>) {			# skip lines till run starts

    if(/^iter 	bound 	train 	test/) {last;}

}



# open scores files

$fname = $filename{"scoresOutputFileName"};

open(TRAINMAR,$fname) || die "could not open file $fname";

$fname = $filename{"testScoresOutputFileName"};

open(TESTMAR,$fname) || die "could not open file $fname";



# open and initialize gnuplot

open(GNUPLOT,"| tee gnuplot.script | gnuplot")

    || die "could not start gnuplot";



print GNUPLOT "set grid\n";

print GNUPLOT "set output '$dirname/$namestub.graphs.ps'\n";



print GNUPLOT "set terminal postscript landscape color\n";

if($mode eq "M") { 

    print GNUPLOT "set data style line \n";

    $graphtitle .= " margin graphs";

}

elsif($mode eq "R") {

    print GNUPLOT "set data style point \n";

    $graphtitle .= " precision-recall graph";

}
#---------------------------------------------
elsif($mode eq "S") {

    print GNUPLOT "set data style line \n";

    $graphtitle .= " score  graphs";

}
#---------------------------------------------
else {die "unrecognized mode = '$mode'"; }



# start main loop

$iter=0;

while($infoline = <INFO>) {

    if($infoline =~ /^(\d+)\s*([\d\.]*)\s*([\d\.]*)\s*([\d\.]*)/) {

	$iter=$1;

	$bound[$iter]=$2;

	$train_err[$iter]=$3;

	$test_err[$iter]=$4;

    }

    

    if($infoline =~ /output scores (\d*)/) {

	$trainfilename =  $filename{"scoresOutputFileName"}.".plot.$mode.$iter.train";

	$testfilename =  $filename{"scoresOutputFileName"}.".plot.$mode.$iter.test";

	@tmp_file_list = (@tmp_file_list,$trainfilename,$testfilename);

	outputmargins(\@trainLabels,\*TRAINMAR,$trainfilename,$iter);

	outputmargins(\@testLabels,\*TESTMAR,$testfilename,$iter);

	print GNUPLOT "set title '$graphtitle, Iteration $iter' \"Helvetica,10\" \n";

	plotmargins($trainfilename,$testfilename);

	print "finished generating margins for iteration $iter\n";

    }



#    if($infoline =~ /(resampling \d*)/) {

#	$sampline = <SAMP>;

#	if($infoline ne $sampline) {

#	    die "sampling file does not match info file:\n $sampline $infoline";

#	}

#	&readSampleSet(\@train_labels,\@train_weights);

#	print "read new sampleset\n";

#    }

}



# plot a graph with the main quantities for each iteration



$gname = $filename{"logOutputFileName"}.".plot";

open(ITERG,">$gname") || die "could not open $gname";



for ($iter=0; $iter <= $#train_err; $iter++) {

    print ITERG "$bound[$iter]\t$train_err[$iter]\t$test_err[$iter]\t\n";

}

close(ITERG);



print GNUPLOT "set title '$graphtitle, training and test errors'\n";

print GNUPLOT  "plot [][0:0.5] '$gname' using 1 t 'bound', '$gname' using 2 t 'training error', '$gname' using 3 t 'test error'\n";

print GNUPLOT "q\n";



close(GNUPLOT);



# delete gnuplot data files

unlink @tmp_file_list;



##########################################################################

# generate a gnu data file for the margin distribution or precision/recall.

##########################################################################

sub outputmargins {

    my ($labels,$filehandle,$gnufilename,$iter) = @_;

    my (@list,$line,$i,$t,$index,$margin,$p,$total_neg,$accum,$count,@negatives,@total);

    

    # read and check first line

    $line = <$filehandle>;

    $line =~ /^iteration=(\d*), elements=(\d*)/

	|| die "unexpected iteration start line in margins file:\n $line";

    $iter_no = $1;

    $element_no = $2;

    if($iter_no != $iter) {

	die "margins file, mismatch between iter=$iter and iter_no=$iter_no\n";

    }



    # read margins

    $total_neg=0;

    $count=0;

    for ($i=0; $i < $element_no; $i++) {

	# read and check line

	$line = <$filehandle> || die "margins file ($name) ended prematurely ($iter,$i)";

	if($line =~ /Infinity/) {

	    die "found an Infinity in the scores when generating $gnufilename. Might be because Booster_smooth is 0.0";

	}

	$score = $line+0.0;

	$label = @{$labels}[$i];

	$margin = $score * $label;



#	print "Score = $score \t Label=$label\n";

	if($mode eq "R") {

	    $total{$score} ++;

	    if($label == -1) {

		$negatives{$score}++;

		$total_neg ++;

	    }

	} elsif($mode eq "M") {

	    $total{$margin}++;

	    $count++;

	} elsif($mode eq "S") {

	    $total{$score}++;

	    $count++;

	  }

      }



    open(G, ">$gnufilename") 

	|| die "could not open >$gnufilename";



    @list = sort {$a <=> $b} keys %total;

    

    if($mode eq "R") {

	$neg_count=0;

	$index = 0;

	foreach $p (@list) {

	    $index += $total{$p};

	    $neg_count += $negatives{$p};

	    delete $total{$p};

	    delete $negatives{$p};

	    

	    print G $neg_count/$total_neg."\t".$neg_count/$index."\n";

	}

    } elsif($mode eq "M") {

	$accum=0;

	foreach $p (@list) {

	    $accum += $total{$p};


	    delete $total{$p};


	    print G "$p ".$accum/$count."\n";

	}

    } elsif($mode eq "S") {

	$accum=0;

	foreach $p (@list) {

	    $accum += $total{$p};

#	    $mucca = $accum/$count;

	    delete $total{$p};


	    print G "$p ".$accum/$count."\n";

	}

    }



    close(G);

}



sub plotmargins 

{

    my ($trainfilename,$testfilename) = @_;

    print GNUPLOT "set nolabel \n";

    print GNUPLOT "set label '$trainfilename' at screen 0.5,-0.1 center \n";

    

    if($mode eq "R") {

	print GNUPLOT "plot [0:1][0:1] '$trainfilename' t 'train','$testfilename' t 'test'\n";

    } 

    elsif($mode eq "M") {

# potential and weight for brownboost

#	print GNUPLOT "w(x) = exp(-(x + $erf_s[$])**2 / $erf_c)\n";

#	print GNUPLOT "p(x) = (1+erf((x + $erf_s[$currentT])/ sqrt($erf_c)))/2\n";

	print GNUPLOT "plot [][0:1]  '$trainfilename' t 'train','$testfilename' t 'test'\n";

      } 


    elsif($mode eq "S" ) {

	print GNUPLOT "plot [][0:1]  '$trainfilename' t 'train','$testfilename' t 'test'\n";

    }

}





sub readSampleSet

{

    my ($labels, $weights) = @_;

    local($index,$position);

    $#{$labels} = -1;		# clear arrays

    $#{$weights} = -1;

    $index=0;

    while(<SAMP>) {

	if(/^(1|-1)\t([\d\.]*)\tl=/) {

	    @{$labels}[$index] = $1;

	    @{$weights}[$index++] = $2;

	    $position = tell SAMP;	# remember position of last meaningful line

	} else {

	    last;

	}

    }

    seek SAMP,$position,0;	# reset file to right after last meaningful line

}











