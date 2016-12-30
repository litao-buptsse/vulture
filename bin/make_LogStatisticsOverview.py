#!/usr/bin/python
# coding:utf-8
import sys

reload(sys)
sys.setdefaultencoding("utf-8")

import torndb

connTest = torndb.Connection(
    host="10.134.97.148",
    database="vulture_data",
    user="root",
    password="rootmysql",
)


class MysqlClient:
    @staticmethod
    def execute(sql, conn):
        result = None
        try:
            result = conn.execute(sql)
        except Exception, e:
            s = 'Exception: "' + str(e) + '" in execute function where sql is: ' + sql
            print >> sys.stderr, s
            exit(1)
        return result

    @staticmethod
    def query(sql, conn):
        result = None
        try:
            result = conn.query(sql)
        except Exception, e:
            s = 'Exception: "' + str(e) + " in query function where sql is: " + sql
            print >> sys.stderr, s
            exit(1)
        return result


date = sys.argv[1]
dataType = sys.argv[2]

logStatisticsOverview = MysqlClient.query(
    sql='SELECT '
        'SUM(size - targetSize) AS size '
        ', SUM(num - targetNum)   AS num '
        'FROM LogStatisticsDetail '
        'WHERE date = \'%s\' AND state = \'SUCC\' AND type = \'%s\';'
        % (date, dataType),
    conn=connTest,
)[0]

if logStatisticsOverview["size"] is None or logStatisticsOverview["num"] is None:
    print >> sys.stderr, "no statistics result from LogStatisticsDetail."
    exit(1)
else:
    logStatisticsOverview["type"] = dataType
    logStatisticsOverview["date"] = date
    for colName in logStatisticsOverview:
        logStatisticsOverview[colName] = str(logStatisticsOverview[colName])
    MysqlClient.execute(
        sql='DELETE1 FROM LogStatisticsOverview '
            'WHERE date = \'%s\' AND type = \'%s\';'
            % (date, dataType),
        conn=connTest,
    )
    MysqlClient.execute(
        sql='INSERT INTO LogStatisticsOverview(type, date, size, num ) '
            'VALUES(\'%s\', \'%s\', \'%s\', \'%s\');'
            % (logStatisticsOverview["type"], logStatisticsOverview["date"], logStatisticsOverview["size"],
               logStatisticsOverview["num"]),
        conn=connTest,
    )

connTest.close()
