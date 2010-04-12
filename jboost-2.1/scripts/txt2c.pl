#! /usr/common/bin/perl



# a script for translating a text description of an atree together

# with a names file into a c code that calculates the prediction of the tree



$namesfile = $ARGV[0];

$treefile = $ARGV[1];

$boost_iterations =$ARGV[2];	# a bound on the number of boosting iterations

			        # whose result we want to use

$tab = "  ";			



$treefile =~ s/.txt$//i;	# remove .txt extension



open(NAMES,$namesfile) || die "could not open $namesfile";



open(C,">$treefile.c")

    || die "could not open $treefile.c";



$count=0;

while($line=<NAMES>) {

    $line =~ s/\|.*$//;		# remove comment

    if($line =~ /^(.*):(.*)continuous\./) {

	$name=$1;

	$name =~ s/[-<>]/_/g;

	print C "#define \t $name \t a\[$count\]\n";

	print C "#define \t d_$name \t ad\[$count\]\n";

	$count++;

    }

}



close(NAMES);



print C "\n\n";

print C "double predict(double *a, char *ad)\n";

print C "{\n";

print C $tab."double p;\n\n";



open(TXT,"$treefile") 

    || die "could not open $treefile.txt";



$depth = 0;

while($line = <TXT>) {

    if($line =~ /^(\d*)\s*\[([^\]]*)\] (prediction|Splitter) =\s*([^\#\(]*)[\#\(]/) {

	$index = $1;

	$id = $2;

	$type = $3;

	$description = $4;



	if($index > $boost_iterations) {next;} # skip parts of tree that

					       # were generated after too

					       # many boosting iterations

	print $line;



# Extract description of node

	if($type eq "prediction") {

	    $splitter=0;

	    $description =~ /^([\d\.\-]*)/;

	    $value = $1;

	} else {

	    $splitter = 1;

	    $description =~ /^(\S*)( [<>] \S*)/;

	    $name = $1;

	    $ineq = $2;

	    $name =~ s/[-<>]/_/g;

	    $pre_condition = "d_$name";

	    $condition = $name.$ineq;

	}

	$id =~ /[.:](\d*)$/;

	$sibling = $1;

	

	# add the appropriate number of open and close brackets

	@id_list = split(/[:]/,$id);

	$new_depth = $#id_list;



	if($new_depth == 0 && $splitter == 0) {	# root node

	    print C $tab."p = $value;\n";

	    next;

	}

	if($new_depth <= $depth) {

	    while($depth > $new_depth) {

		$depth--;

		print C ($tab x (2*$depth+2))."}}\n";

	    }

	    $depth--;

	    if($splitter == 0) {

		print C ($tab x (2*$depth+2))."} else {\n";

	    }

	}

	$depth = $new_depth;



	# print command line

	if($splitter == 0) {

	    print C ($tab x (2*$depth+1))."p += $value; \t /* $id */\n";

	} else {

	    print C ($tab x (2*$depth+1))."if ( $pre_condition ) { \t /* $id */\n";

	    print C ($tab x (2*$depth+2))."if ( $condition ) {\n";

	}

    } else {

	print "Ignored line: $line";

    }

}

while(0 < $depth) {

    $depth--;

    print C ($tab x (2*$depth+1))."}}\n";

}



print C $tab."return(p);\n";

print C "} /* end */\n";



close(C);



close(TXT);













