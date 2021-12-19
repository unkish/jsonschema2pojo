/**
 * Copyright © 2010-2020 Nokia
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jsonschema2pojo.rules;

import static java.util.Arrays.*;
import static org.hamcrest.MatcherAssert.*;
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.*;

import java.net.URI;
import java.util.Collection;
import java.util.Date;
import java.util.UUID;
import java.util.regex.Pattern;

import org.jsonschema2pojo.GenerationConfig;
import org.jsonschema2pojo.NoopAnnotator;
import org.jsonschema2pojo.SchemaStore;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import com.fasterxml.jackson.databind.node.TextNode;
import com.sun.codemodel.JCodeModel;
import com.sun.codemodel.JType;

public class FormatRuleTest {

    private GenerationConfig config = mock(GenerationConfig.class);
    private FormatRule rule = new FormatRule(new RuleFactory(config, new NoopAnnotator(), new SchemaStore()));

    public static Collection<Object[]> data() {
        return asList(new Object[][] {
                { "date-time", Date.class },
                { "date", String.class },
                { "time", String.class },
                { "utc-millisec", Long.class },
                { "regex", Pattern.class },
                { "color", String.class },
                { "style", String.class },
                { "phone", String.class },
                { "uri", URI.class },
                { "email", String.class },
                { "ip-address", String.class },
                { "ipv6", String.class },
                { "host-name", String.class },
                { "uuid", UUID.class }});
    }

    @ParameterizedTest
    @MethodSource("data")
    public void applyGeneratesTypeFromFormatValue(String formatValue, Class<?> expectedType) {
        TextNode formatNode = TextNode.valueOf(formatValue);

        JType result = rule.apply("fooBar", formatNode, null, new JCodeModel().ref(String.class), null);

        assertThat(result.fullName(), equalTo(expectedType.getName()));
    }

    @Test
    public void applyDefaultsToBaseType() {
        TextNode formatNode = TextNode.valueOf("unknown-format");

        JType baseType = new JCodeModel().ref(Long.class);

        JType result = rule.apply("fooBar", formatNode, null, baseType, null);

        assertThat(result, equalTo(baseType));
    }

}
