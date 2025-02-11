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

package org.opensearch.search.aggregations;

import org.opensearch.common.io.stream.StreamInput;
import org.opensearch.common.io.stream.StreamOutput;
import org.opensearch.common.xcontent.XContentBuilder;
import org.opensearch.index.query.QueryShardContext;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;

/**
 * Base implementation of a {@link AggregationBuilder}.
 */
public abstract class AbstractAggregationBuilder<AB extends AbstractAggregationBuilder<AB>>
    extends AggregationBuilder {

    protected Map<String, Object> metadata;

    /**
     * Constructs a new aggregation builder.
     *
     * @param name  The aggregation name
     */
    public AbstractAggregationBuilder(String name) {
        super(name);
    }

    protected AbstractAggregationBuilder(AbstractAggregationBuilder<AB> clone,
                                         AggregatorFactories.Builder factoriesBuilder,
                                         Map<String, Object> metadata) {
        super(clone, factoriesBuilder);
        this.metadata = metadata;
    }

    /**
     * Read from a stream.
     */
    protected AbstractAggregationBuilder(StreamInput in) throws IOException {
        super(in.readString());
        factoriesBuilder = new AggregatorFactories.Builder(in);
        metadata = in.readMap();
    }

    @Override
    public final void writeTo(StreamOutput out) throws IOException {
        out.writeString(name);
        factoriesBuilder.writeTo(out);
        out.writeMap(metadata);
        doWriteTo(out);
    }

    protected abstract void doWriteTo(StreamOutput out) throws IOException;

    @SuppressWarnings("unchecked")
    @Override
    public AB subAggregation(AggregationBuilder aggregation) {
        if (aggregation == null) {
            throw new IllegalArgumentException("[aggregation] must not be null: [" + name + "]");
        }
        factoriesBuilder.addAggregator(aggregation);
        return (AB) this;
    }

    /**
     * Add a sub aggregation to this aggregation.
     */
    @SuppressWarnings("unchecked")
    @Override
    public AB subAggregation(PipelineAggregationBuilder aggregation) {
        if (aggregation == null) {
            throw new IllegalArgumentException("[aggregation] must not be null: [" + name + "]");
        }
        factoriesBuilder.addPipelineAggregator(aggregation);
        return (AB) this;
    }

    /**
     * Registers sub-factories with this factory. The sub-factory will be
     * responsible for the creation of sub-aggregators under the aggregator
     * created by this factory.
     *
     * @param subFactories
     *            The sub-factories
     * @return this factory (fluent interface)
     */
    @SuppressWarnings("unchecked")
    @Override
    public AB subAggregations(AggregatorFactories.Builder subFactories) {
        if (subFactories == null) {
            throw new IllegalArgumentException("[subFactories] must not be null: [" + name + "]");
        }
        this.factoriesBuilder = subFactories;
        return (AB) this;
    }

    @SuppressWarnings("unchecked")
    @Override
    public AB setMetadata(Map<String, Object> metadata) {
        if (metadata == null) {
            throw new IllegalArgumentException("[metadata] must not be null: [" + name + "]");
        }
        this.metadata = metadata;
        return (AB) this;
    }

    @Override
    public Map<String, Object> getMetadata() {
        return metadata == null ? Collections.emptyMap() : Collections.unmodifiableMap(metadata);
    }

    @Override
    public final String getWriteableName() {
        // We always use the type of the aggregation as the writeable name
        return getType();
    }

    @Override
    public final AggregatorFactory build(QueryShardContext queryShardContext, AggregatorFactory parent) throws IOException {
        AggregatorFactory factory = doBuild(queryShardContext, parent, factoriesBuilder);
        queryShardContext.getUsageService().incAggregationUsage(getType(), factory.getStatsSubtype());
        return factory;
    }

    protected abstract AggregatorFactory doBuild(QueryShardContext queryShardContext, AggregatorFactory parent,
                                                 AggregatorFactories.Builder subfactoriesBuilder) throws IOException;

    @Override
    public final XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
        builder.startObject(name);

        if (this.metadata != null) {
            builder.field("meta", this.metadata);
        }
        builder.field(getType());
        internalXContent(builder, params);

        if (factoriesBuilder != null && (factoriesBuilder.count()) > 0) {
            builder.field("aggregations");
            factoriesBuilder.toXContent(builder, params);
        }
        return builder.endObject();
    }

    protected abstract XContentBuilder internalXContent(XContentBuilder builder, Params params) throws IOException;

    @Override
    public int hashCode() {
        return Objects.hash(factoriesBuilder, metadata, name);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        AbstractAggregationBuilder<AB> other = (AbstractAggregationBuilder<AB>) obj;

        return Objects.equals(name, other.name)
            && Objects.equals(metadata, other.metadata)
            && Objects.equals(factoriesBuilder, other.factoriesBuilder);
    }
}
