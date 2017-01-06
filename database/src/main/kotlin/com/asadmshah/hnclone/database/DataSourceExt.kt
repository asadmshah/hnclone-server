package com.asadmshah.hnclone.database

import rx.Observable
import java.sql.Connection
import java.sql.ResultSet
import java.sql.Statement
import javax.sql.DataSource

internal fun <T> DataSource.execute(q: String, function: (ResultSet?) -> Unit) {
    var conn: Connection? = null
    var stmt: Statement? = null
    var rslt: ResultSet? = null

    try {
        conn = connection

        stmt = conn.createStatement()

        rslt = stmt.executeQuery(q)
        function(rslt)
    } finally {
        try { rslt?.close() } catch (ignored: Exception) {  }
        try { stmt?.close() } catch (ignored: Exception) {  }
        try { conn?.close() } catch (ignored: Exception) {  }
    }
}

internal fun <T> DataSource.executeSingle(q: String, function: (ResultSet) -> T): T? {
    var response: T? = null
    execute<T>(q, {
        response = if (it != null && it.next()) function(it) else null
    })
    return response
}

internal fun <T> DataSource.executeObservable(q: String, function: (ResultSet) -> T): Observable<T> {
    val observable = Observable
            .create(Observable.OnSubscribe<T> { subscriber ->
                execute<T>(q, { resultSet ->
                    if (resultSet != null) {
                        while (resultSet.next()) {
                            if (subscriber.isUnsubscribed) break

                            subscriber.onNext(function(resultSet))
                        }
                    }

                    if (!subscriber.isUnsubscribed) {
                        subscriber.onCompleted()
                    }
                })
            })

    return Observable.defer { observable }
}