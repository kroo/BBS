SITE="www.cs.ucsd.edu"
DIR="users/aarvey/jboost"

rm -rf css images 

wget -r -o wget.log http://${SITE}/${DIR}
mv  ${SITE}/${DIR}/* .

rm -rf ${SITE}
rm wget.log 

