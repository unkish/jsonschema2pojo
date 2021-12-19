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

import static com.sun.codemodel.JMod.*;
import static org.hamcrest.MatcherAssert.*;
import static org.hamcrest.Matchers.*;

import java.io.StringWriter;

import org.junit.jupiter.api.Test;

import com.sun.codemodel.JClassAlreadyExistsException;
import com.sun.codemodel.JCodeModel;
import com.sun.codemodel.JDefinedClass;
import com.sun.codemodel.JFieldRef;
import com.sun.codemodel.JFieldVar;
import com.sun.codemodel.JFormatter;

class DynamicPropertiesRuleTest {

    private final JCodeModel codeModel = new JCodeModel();
    private final DynamicPropertiesRule rule = new DynamicPropertiesRule(new RuleFactory());

    @Test
    void shouldAddNotFoundField() throws JClassAlreadyExistsException {
        final String className = "org.jsonschema2pojo.rules.ExampleClass";
        final JDefinedClass type = codeModel._class(className);
        assertThat(type.fields().entrySet(), is(empty()));

        final JFieldRef var = rule.getOrAddNotFoundVar(type);
        assertThat(type.fields(), hasKey(DynamicPropertiesRule.NOT_FOUND_VALUE_FIELD));
        final JFieldVar notFoundValueField = type.fields().get(DynamicPropertiesRule.NOT_FOUND_VALUE_FIELD);
        assertThat(notFoundValueField.mods().getValue(), is(PROTECTED | STATIC | FINAL));
        assertThat(notFoundValueField.type().binaryName(), is(Object.class.getName()));

        final StringWriter sw = new StringWriter();
        var.generate(new JFormatter(sw));
        assertThat(sw.toString(), is(className + "." + DynamicPropertiesRule.NOT_FOUND_VALUE_FIELD));
    }

}
