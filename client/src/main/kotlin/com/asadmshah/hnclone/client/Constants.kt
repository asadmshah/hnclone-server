package com.asadmshah.hnclone.client

import io.grpc.Metadata

internal object Constants {

    internal val AUTHORIZATION_KEY = Metadata.Key.of("authorization-bin", Metadata.BINARY_BYTE_MARSHALLER)

}