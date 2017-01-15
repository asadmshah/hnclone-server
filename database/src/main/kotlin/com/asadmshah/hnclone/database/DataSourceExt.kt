package com.asadmshah.hnclone.database

import io.reactivex.BackpressureStrategy
import io.reactivex.Flowable
import java.sql.Connection
import java.sql.ResultSet
import java.sql.Statement
import javax.sql.DataSource

internal fun DataSource.execute(q: String, function: (ResultSet?) -> Unit) {
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
    execute(q, {
        response = if (it != null && it.next()) function(it) else null
    })
    return response
}

internal fun <T> DataSource.executeFlowable(q: String, function: (ResultSet) -> T): Flowable<T> {
    val f = Flowable
            .create<T>({ subscriber ->
                execute(q, { resultSet ->
                    if (resultSet != null) {
                        while (resultSet.next()) {
                            if (subscriber.isCancelled) break

                            subscriber.onNext(function(resultSet))
                        }
                    }

                    if (!subscriber.isCancelled) subscriber.onComplete()
                })
            }, BackpressureStrategy.BUFFER)

    return Flowable.defer{ f }
}