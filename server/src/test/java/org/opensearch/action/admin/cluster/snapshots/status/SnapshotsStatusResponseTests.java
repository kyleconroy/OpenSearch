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

package org.opensearch.action.admin.cluster.snapshots.status;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

import org.opensearch.common.xcontent.XContentParser;
import org.opensearch.test.AbstractXContentTestCase;
import org.opensearch.action.admin.cluster.snapshots.status.SnapshotStatus;
import org.opensearch.action.admin.cluster.snapshots.status.SnapshotsStatusResponse;

public class SnapshotsStatusResponseTests extends AbstractXContentTestCase<SnapshotsStatusResponse> {

    @Override
    protected SnapshotsStatusResponse doParseInstance(XContentParser parser) throws IOException {
        return SnapshotsStatusResponse.fromXContent(parser);
    }

    @Override
    protected Predicate<String> getRandomFieldsExcludeFilter() {
        // Do not place random fields in the indices field or shards field since their fields correspond to names.
        return (s) -> s.endsWith("shards") || s.endsWith("indices");
    }

    @Override
    protected boolean supportsUnknownFields() {
        return true;
    }

    @Override
    protected SnapshotsStatusResponse createTestInstance() {
        SnapshotStatusTests statusBuilder = new SnapshotStatusTests();
        List<SnapshotStatus> snapshotStatuses = new ArrayList<>();
        for (int idx = 0; idx < randomIntBetween(0, 5); idx++) {
            snapshotStatuses.add(statusBuilder.createTestInstance());
        }
        return new SnapshotsStatusResponse(snapshotStatuses);
    }
}
