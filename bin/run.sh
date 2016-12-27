#!/bin/bash

if [ $# -ne 1 ]; then
  echo "need args date"
  exit 1
fi

dir=`dirname $0`
dir=`cd $dir/..; pwd`

java -cp $dir/target/vulture-1.0-SNAPSHOT.jar:$dir/conf com.sogou.vulture.LifecycleManager $1
