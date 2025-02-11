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

package org.opensearch.search.aggregations.bucket.histogram;

import org.opensearch.common.CheckedFunction;
import org.opensearch.common.ParseField;
import org.opensearch.common.Rounding;
import org.opensearch.common.io.stream.StreamInput;
import org.opensearch.common.io.stream.StreamOutput;
import org.opensearch.common.io.stream.Writeable;
import org.opensearch.common.xcontent.ConstructingObjectParser;
import org.opensearch.common.xcontent.ObjectParser.ValueType;
import org.opensearch.common.xcontent.ToXContentFragment;
import org.opensearch.common.xcontent.XContentBuilder;
import org.opensearch.common.xcontent.XContentParser;
import org.opensearch.common.xcontent.XContentParser.Token;
import org.opensearch.index.query.QueryShardContext;
import org.opensearch.search.DocValueFormat;

import java.io.IOException;
import java.util.Objects;

import static org.opensearch.common.xcontent.ConstructingObjectParser.optionalConstructorArg;

/**
 * Represent hard_bounds and extended_bounds in date-histogram aggregations.
 *
 * This class is similar to {@link DoubleBounds} used in histograms, but is using longs to store data. LongBounds and DoubleBounds are
 *  * not used interchangeably and therefore don't share any common interfaces except for serialization.
 */
public class LongBounds implements ToXContentFragment, Writeable {
    static final ParseField MIN_FIELD = new ParseField("min");
    static final ParseField MAX_FIELD = new ParseField("max");

    public static final ConstructingObjectParser<LongBounds, Void> PARSER = new ConstructingObjectParser<>(
            "bounds", a -> {
        assert a.length == 2;
        Long min = null;
        Long max = null;
        String minAsStr = null;
        String maxAsStr = null;
        if (a[0] == null) {
            // nothing to do with it
        } else if (a[0] instanceof Long) {
            min = (Long) a[0];
        } else if (a[0] instanceof String) {
            minAsStr = (String) a[0];
        } else {
            throw new IllegalArgumentException("Unknown field type [" + a[0].getClass() + "]");
        }
        if (a[1] == null) {
            // nothing to do with it
        } else if (a[1] instanceof Long) {
            max = (Long) a[1];
        } else if (a[1] instanceof String) {
            maxAsStr = (String) a[1];
        } else {
            throw new IllegalArgumentException("Unknown field type [" + a[1].getClass() + "]");
        }
        return new LongBounds(min, max, minAsStr, maxAsStr);
    });
    static {
        CheckedFunction<XContentParser, Object, IOException> longOrString = p -> {
            if (p.currentToken() == Token.VALUE_NUMBER) {
                return p.longValue(false);
            }
            if (p.currentToken() == Token.VALUE_STRING) {
                return p.text();
            }
            if (p.currentToken() == Token.VALUE_NULL) {
                return null;
            }
            throw new IllegalArgumentException("Unsupported token [" + p.currentToken() + "]");
        };
        PARSER.declareField(optionalConstructorArg(), longOrString, MIN_FIELD, ValueType.LONG_OR_NULL);
        PARSER.declareField(optionalConstructorArg(), longOrString, MAX_FIELD, ValueType.LONG_OR_NULL);
    }

    /**
     * Parsed min value. If this is null and {@linkplain #minAsStr} isn't then this must be parsed from {@linkplain #minAsStr}. If this is
     * null and {@linkplain #minAsStr} is also null then there is no lower bound.
     */
    private final Long min;
    /**
     * Parsed min value. If this is null and {@linkplain #maxAsStr} isn't then this must be parsed from {@linkplain #maxAsStr}. If this is
     * null and {@linkplain #maxAsStr} is also null then there is no lower bound.
     */
    private final Long max;

    private final String minAsStr;
    private final String maxAsStr;

    /**
     * Construct with parsed bounds.
     */
    public LongBounds(Long min, Long max) {
        this(min, max, null, null);
    }

    /**
     * Construct with unparsed bounds.
     */
    public LongBounds(String minAsStr, String maxAsStr) {
        this(null, null, minAsStr, maxAsStr);
    }

    /**
     * Construct with all possible information.
     */
    private LongBounds(Long min, Long max, String minAsStr, String maxAsStr) {
        this.min = min;
        this.max = max;
        this.minAsStr = minAsStr;
        this.maxAsStr = maxAsStr;
    }

    /**
     * Read from a stream.
     */
    public LongBounds(StreamInput in) throws IOException {
        min = in.readOptionalLong();
        max = in.readOptionalLong();
        minAsStr = in.readOptionalString();
        maxAsStr = in.readOptionalString();
    }

    @Override
    public void writeTo(StreamOutput out) throws IOException {
        out.writeOptionalLong(min);
        out.writeOptionalLong(max);
        out.writeOptionalString(minAsStr);
        out.writeOptionalString(maxAsStr);
    }

    /**
     * Parse the bounds and perform any delayed validation. Returns the result of the parsing.
     */
    LongBounds parseAndValidate(String aggName, String boundsName, QueryShardContext queryShardContext, DocValueFormat format) {
        Long min = this.min;
        Long max = this.max;
        assert format != null;
        if (minAsStr != null) {
            min = format.parseLong(minAsStr, false, queryShardContext::nowInMillis);
        }
        if (maxAsStr != null) {
            // TODO: Should we rather pass roundUp=true?
            max = format.parseLong(maxAsStr, false, queryShardContext::nowInMillis);
        }
        if (min != null && max != null && min.compareTo(max) > 0) {
            throw new IllegalArgumentException("[" + boundsName + ".min][" + min + "] cannot be greater than " +
                    "[" + boundsName + ".max][" + max + "] for histogram aggregation [" + aggName + "]");
        }
        return new LongBounds(min, max, minAsStr, maxAsStr);
    }

    LongBounds round(Rounding rounding) {
        // Extended bounds shouldn't be effected by the offset
        Rounding effectiveRounding = rounding.withoutOffset();
        return new LongBounds(
                min != null ? effectiveRounding.round(min) : null,
                max != null ? effectiveRounding.round(max) : null);
    }

    @Override
    public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
        if (min != null) {
            builder.field(MIN_FIELD.getPreferredName(), min);
        } else {
            builder.field(MIN_FIELD.getPreferredName(), minAsStr);
        }
        if (max != null) {
            builder.field(MAX_FIELD.getPreferredName(), max);
        } else {
            builder.field(MAX_FIELD.getPreferredName(), maxAsStr);
        }
        return builder;
    }

    @Override
    public int hashCode() {
        return Objects.hash(min, max, minAsStr, maxAsStr);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        LongBounds other = (LongBounds) obj;
        return Objects.equals(min, other.min)
                && Objects.equals(max, other.max)
                && Objects.equals(minAsStr, other.minAsStr)
                && Objects.equals(maxAsStr, other.maxAsStr);
    }

    public Long getMin() {
        return min;
    }

    public Long getMax() {
        return max;
    }

    public boolean contain(long value) {
        if (max != null && value >= max) {
            return false;
        }
        if (min != null && value < min) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        StringBuilder b = new StringBuilder();
        if (min != null) {
            b.append(min);
            if (minAsStr != null) {
                b.append('(').append(minAsStr).append(')');
            }
        } else {
            if (minAsStr != null) {
                b.append(minAsStr);
            }
        }
        b.append("--");
        if (max != null) {
            b.append(max);
            if (maxAsStr != null) {
                b.append('(').append(maxAsStr).append(')');
            }
        } else {
            if (maxAsStr != null) {
                b.append(maxAsStr);
            }
        }
        return b.toString();
    }
}
