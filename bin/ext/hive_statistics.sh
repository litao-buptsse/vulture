#!/bin/bash

if [ $# -lt 3 ]; then
  echo "usage: $0 <db> <table> <partitionValStrs>"
  exit 1
fi

dir=`dirname $0`
dir=`cd $dir; pwd`

type="HIVE"
db=$1
table=$2
partitionValStrs=$3

hadoopClasspath=`hadoop classpath`
hiveClasspath=/opt/datadir/conf:/opt/datadir/lib/*:/opt/datadir/auxlib/*
classpath=$dir/hadoop-extras-1.0-SNAPSHOT.jar:$hiveClasspath:$hadoopClasspath

java -cp $classpath \
  com.sogou.hadoop.extras.tools.statistics.Statistics \
  $type $db $table $partitionValStrs
