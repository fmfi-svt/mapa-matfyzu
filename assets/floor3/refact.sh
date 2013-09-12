find | grep '\./[0-9]\+/[0-9]\+/[0-9]\+\.jpg' | while read e
do
x=`echo ${e} | sed -e 's/\.\/\([0-9]\+\)\/\([0-9]\+\)\/\([0-9]\+\)\.jpg/\1/g'`;
y=`echo ${e} | sed -e 's/\.\/\([0-9]\+\)\/\([0-9]\+\)\/\([0-9]\+\)\.jpg/\2/g'`;
z=`echo ${e} | sed -e 's/\.\/\([0-9]\+\)\/\([0-9]\+\)\/\([0-9]\+\)\.jpg/\3/g'`;
a=`echo 2^${x} | bc`;
xx=$(( a - z - 1 ));
n=`echo ./${x}/${y}/${xx}.jpg`
echo renaming ${e} to ${n};
mv ${e} ${n};
done
