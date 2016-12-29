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

hadoop jar $dir/hadoop-extras-1.0-SNAPSHOT.jar \
  com.sogou.hadoop.extras.tools.statistics.Statistics \
  $type $dirPattern $filePattern
