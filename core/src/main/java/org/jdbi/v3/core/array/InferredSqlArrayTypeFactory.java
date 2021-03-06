/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jdbi.v3.core.array;

import java.lang.reflect.Type;
import java.util.Optional;

import org.jdbi.v3.core.config.ConfigRegistry;

import static org.jdbi.v3.core.generic.GenericTypes.findGenericParameter;

/**
 * A generic {@link SqlArrayTypeFactory} that reflectively inspects an {@link SqlArrayType SqlArrayType<T>} and maps
 * only arrays of element type {@code T}. The type parameter {@code T} must be accessible via reflection or an
 * {@link UnsupportedOperationException} will be thrown.
 */
class InferredSqlArrayTypeFactory implements SqlArrayTypeFactory {
    private final Type inferredElementType;
    private final SqlArrayType<?> arrayType;

    InferredSqlArrayTypeFactory(SqlArrayType<?> arrayType) {
        inferredElementType = findGenericParameter(arrayType.getClass(), SqlArrayType.class)
                .orElseThrow(() -> new UnsupportedOperationException("Must use a concretely typed SqlArrayType here"));
        this.arrayType = arrayType;
    }

    @Override
    public Optional<SqlArrayType<?>> build(Type elementType, ConfigRegistry config) {
        return inferredElementType.equals(elementType)
                ? Optional.of(arrayType)
                : Optional.empty();
    }
}
