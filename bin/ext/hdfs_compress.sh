#!/bin/bash

if [ $# -lt 3 ]; then
  echo "usage: $0 <inputPath> <filePattern> <HOT|WARM|COLD> [filePrefix] [outputPath] [tmpPath] [trashPath]"
  exit 1
fi

dir=`dirname $0`
dir=`cd $dir; pwd`

inputPath=$1
filePattern=$2
compressType=$3
filePrefix=''
outputPath=$inputPath
tmpPath='/logdata/tmp/hdfs_compress_tmp'
trashPath='/logdata/tmp/hdfs_compress_trash'
ymd=`date +%Y%m%d`

if [ $# -ge 4 ]; then filePrefix=$4; fi
if [ $# -ge 5 ]; then outputPath=$5; fi
if [ $# -ge 6 ]; then tmpPath=$6; fi
if [ $# -ge 7 ]; then trashPath=$7; fi

hadoop jar $dir/hadoop-extras-1.0-SNAPSHOT.jar \
  com.sogou.hadoop.extras.tools.hdfs.compress.DistributedHdfsCompression \
  -DfilePrefix=$filePrefix -DoutputPath=$outputPath -DtmpPath=$tmpPath -DtrashPath=$trashPath \
  $inputPath $filePattern $compressType