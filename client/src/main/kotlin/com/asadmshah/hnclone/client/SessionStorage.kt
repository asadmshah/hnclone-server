package com.asadmshah.hnclone.client

import com.asadmshah.hnclone.models.SessionToken

interface SessionStorage {

    fun putRequestKey(k: SessionToken)

    fun putRefreshKey(k: SessionToken)

    fun getRequestKey(): SessionToken?

    fun getRefreshKey(): SessionToken?

    fun clear()
}