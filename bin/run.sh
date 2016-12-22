#!/bin/bash

if [ $# -ne 1 ]; then
  echo "need args date"
  exit 1
fi

java -cp target/vulture-1.0-SNAPSHOT.jar com.sogou.vulture.LifecycleManager $1