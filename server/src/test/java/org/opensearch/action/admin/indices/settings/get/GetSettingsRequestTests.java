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

package org.opensearch.action.admin.indices.settings.get;

import org.opensearch.common.bytes.BytesReference;
import org.opensearch.common.io.stream.BytesStreamOutput;
import org.opensearch.common.io.stream.StreamInput;
import org.opensearch.test.OpenSearchTestCase;
import org.opensearch.action.admin.indices.settings.get.GetSettingsRequest;

import java.io.IOException;

public class GetSettingsRequestTests extends OpenSearchTestCase {
    private static final GetSettingsRequest TEST_700_REQUEST = new GetSettingsRequest()
            .includeDefaults(true)
            .humanReadable(true)
            .indices("test_index")
            .names("test_setting_key");

    public void testSerdeRoundTrip() throws IOException {
        BytesStreamOutput bso = new BytesStreamOutput();
        TEST_700_REQUEST.writeTo(bso);

        byte[] responseBytes = BytesReference.toBytes(bso.bytes());
        StreamInput si = StreamInput.wrap(responseBytes);
        GetSettingsRequest deserialized = new GetSettingsRequest(si);
        assertEquals(TEST_700_REQUEST, deserialized);
    }
}
