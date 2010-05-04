export stem="skinColor"

# Add index to each record
python $JBOOST_HOME/scripts/AddRandomIndex.py $stem

# randomly permute records in data file
python $JBOOST_HOME/scripts/permuteData.py --filename=${stem}_idx.data
mv ${stem}_idx.data ${stem}_idx.data.orig
mv ${stem}_idx.data.P ${stem}_idx.data

# run nfold.py
python $JBOOST_HOME/scripts/nfold.py --booster=AdaBoost --folds=10 --data=${stem}_idx.data --spec=${stem}_idx.spec --rounds=100 --tree=ADD_ROOT --generate

# run visualizer
#python $JBOOST_HOME/scripts/VisualizeScores.py skinColor.data.folds_10/cvdata-04-25-10-10-48-56/ADD_ROOT/

#python $JBOOST_HOME/scripts/VisualizeScores.py ${stem}_idx.data.folds_10/cvdata-04-25-10-14-47-53/ADD_ROOT/