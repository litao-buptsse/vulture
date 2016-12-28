#!/bin/bash

if [ $# -lt 2 ]; then
  echo "usage: $0 <dirPattern> <filePattern> [trashRootDir]"
  exit 1
fi

dir=`dirname $0`
dir=`cd $dir; pwd`

type="HDFS"
dirPattern=$1
filePattern=$2
trashRootDir='/logdata/tmp/hdfs_clean_trash'

if [ $# -ge 3 ]; then trashRootDir=$3; fi


hadoop jar $dir/hadoop-extras-1.0-SNAPSHOT.jar \
  com.sogou.hadoop.extras.tools.clean.Clean \
  $type $dirPattern $filePattern $trashRootDir