/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

/*
 * Licensed to Elasticsearch under one or more contributor
 * license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright
 * ownership. Elasticsearch licenses this file to you under
 * the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

/*
 * Modifications Copyright OpenSearch Contributors. See
 * GitHub history for details.
 */

package org.opensearch.transport.netty4;

import org.opensearch.Version;
import org.opensearch.common.io.stream.NamedWriteableRegistry;
import org.opensearch.common.network.NetworkService;
import org.opensearch.common.settings.Settings;
import org.opensearch.common.transport.TransportAddress;
import org.opensearch.common.util.MockPageCacheRecycler;
import org.opensearch.common.util.PageCacheRecycler;
import org.opensearch.indices.breaker.NoneCircuitBreakerService;
import org.elasticsearch.mocksocket.MockSocket;
import org.opensearch.test.OpenSearchTestCase;
import org.opensearch.threadpool.ThreadPool;
import org.opensearch.transport.SharedGroupFactory;
import org.opensearch.transport.TransportSettings;
import org.junit.After;
import org.junit.Before;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Collections;

import static org.hamcrest.Matchers.is;

/**
 * This test checks, if an HTTP look-alike request (starting with an HTTP method and a space)
 * actually returns text response instead of just dropping the connection
 */
public class Netty4SizeHeaderFrameDecoderTests extends OpenSearchTestCase {

    private final Settings settings = Settings.builder()
        .put("node.name", "NettySizeHeaderFrameDecoderTests")
        .put(TransportSettings.BIND_HOST.getKey(), "127.0.0.1")
        .put(TransportSettings.PORT.getKey(), "0")
        .build();

    private ThreadPool threadPool;
    private Netty4Transport nettyTransport;
    private int port;
    private InetAddress host;

    @Before
    public void startThreadPool() {
        threadPool = new ThreadPool(settings);
        NetworkService networkService = new NetworkService(Collections.emptyList());
        PageCacheRecycler recycler = new MockPageCacheRecycler(Settings.EMPTY);
        nettyTransport = new Netty4Transport(settings, Version.CURRENT, threadPool, networkService, recycler,
            new NamedWriteableRegistry(Collections.emptyList()), new NoneCircuitBreakerService(), new SharedGroupFactory(settings));
        nettyTransport.start();

        TransportAddress[] boundAddresses = nettyTransport.boundAddress().boundAddresses();
        TransportAddress transportAddress = randomFrom(boundAddresses);
        port = transportAddress.address().getPort();
        host = transportAddress.address().getAddress();
    }

    @After
    public void terminateThreadPool() throws InterruptedException {
        nettyTransport.stop();
        terminate(threadPool);
        threadPool = null;
    }

    public void testThatTextMessageIsReturnedOnHTTPLikeRequest() throws Exception {
        String randomMethod = randomFrom("GET", "POST", "PUT", "DELETE", "HEAD", "OPTIONS", "PATCH");
        String data = randomMethod + " / HTTP/1.1";

        try (Socket socket = new MockSocket(host, port)) {
            socket.getOutputStream().write(data.getBytes(StandardCharsets.UTF_8));
            socket.getOutputStream().flush();

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8))) {
                assertThat(reader.readLine(), is("This is not an HTTP port"));
            }
        }
    }

    public void testThatNothingIsReturnedForOtherInvalidPackets() throws Exception {
        try (Socket socket = new MockSocket(host, port)) {
            socket.getOutputStream().write("FOOBAR".getBytes(StandardCharsets.UTF_8));
            socket.getOutputStream().flush();

            // end of stream
            assertThat(socket.getInputStream().read(), is(-1));
        }
    }

}
