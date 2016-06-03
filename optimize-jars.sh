#!/bin/sh
#
# $1 input directory
# $2 prefix for optimized jars
export EXECUTABLE=target/class-body-stripper-1.0.0-SNAPSHOT-jar-with-dependencies.jar

for jarfile in $(find $1 -type f -name '*.jar');
do
   filename=$2_$(basename $jarfile)
   folder=$(dirname $jarfile)
   echo "Stripping $jarfile to $folder/$filename";

   java -jar $EXECUTABLE $jarfile $folder/$filename;
done;

