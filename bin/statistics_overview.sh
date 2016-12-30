#!/bin/bash

if [ $# -ne 2 ]; then
  echo "need args <date> <type>"
  exit 1
fi

date=$1
type=$2

host="10.134.97.148"
database="vulture_data"
user="root"
password="rootmysql"

sql="
REPLACE INTO LogStatisticsOverview
  SELECT
      '$type'                  AS type
    , '$date'                  AS date
    , CASE
      WHEN isnull(SUM(size - targetSize))
        THEN 0
      ELSE SUM(size - targetSize) END
                 AS size
    , CASE
      WHEN isnull(SUM(num - targetNum))
        THEN 0
      ELSE SUM(num - targetNum) END
                 AS num
  FROM LogStatisticsDetail AS detail
  WHERE date = '$date' AND state = 'SUCC' AND type = '$type';
"

mysql -h$host -D$database -u$user -p$password -e "$sql"

exit $?
