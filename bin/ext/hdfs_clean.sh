#!/bin/bash

if [ $# -lt 2 ]; then
  echo "usage: $0 <dirPattern> <filePattern>"
  exit 1
fi

dir=`dirname $0`
dir=`cd $dir; pwd`

type="HDFS"
dirPattern=$1
filePattern=$2
trashRootDir='/logdata/tmp/hdfs_clean_trash'
if [[ $dirPattern == /logdata* ]]; then
  trashRootDir='/logdata/tmp/hdfs_clean_trash'
elif [[ $dirPattern == /storage* ]]; then
  trashRootDir='/storage/tmp/hdfs_clean_trash'
elif [[ $dirPattern == /cloud* ]]; then
  trashRootDir='/cloud/tmp/hdfs_clean_trash'
fi

hadoop jar $dir/hadoop-extras-1.0-SNAPSHOT.jar \
  com.sogou.hadoop.extras.tools.clean.Clean \
  $type $dirPattern $filePattern $trashRootDir
