package io.skalogs.skaetl.service.transform;

/*-
 * #%L
 * process-importer-impl
 * %%
 * Copyright (C) 2017 - 2018 SkaLogs
 * %%
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
 * #L%
 */

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.skalogs.skaetl.domain.ParameterTransformation;
import io.skalogs.skaetl.domain.TypeValidation;
import io.skalogs.skaetl.service.TransformatorProcess;
import org.apache.commons.lang3.StringUtils;

public class TranslateArrayTransformator extends TransformatorProcess {

    public TranslateArrayTransformator() {
        super(TypeValidation.TRANSLATE_ARRAY, "Translate a string array into boolean properties");
    }

    public void apply(String idProcess, ParameterTransformation parameterTransformation, ObjectNode jsonValue) {
        if (has(parameterTransformation.getKeyField(), jsonValue)) {
            JsonNode jsonNode = at(parameterTransformation.getKeyField(), jsonValue);
            //GeoJSON spec :)
            if (jsonNode.isArray()) {
                for (final JsonNode arrayEntry : jsonNode) {
                    put(jsonValue, StringUtils.lowerCase(parameterTransformation.getKeyField() + "-" + arrayEntry.asText()), "true");
                }
            }
        }
    }
}
