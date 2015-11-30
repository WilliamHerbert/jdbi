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
package org.jdbi.v3.unstable;

import java.lang.annotation.Annotation;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import com.fasterxml.classmate.ResolvedType;
import com.fasterxml.classmate.TypeResolver;
import org.jdbi.v3.sqlobject.Binder;
import org.jdbi.v3.sqlobject.BinderFactory;
import org.jdbi.v3.sqlobject.BindingAnnotation;
import org.jdbi.v3.sqlobject.SqlStatementCustomizer;
import org.jdbi.v3.sqlobject.SqlStatementCustomizerFactory;
import org.jdbi.v3.sqlobject.SqlStatementCustomizingAnnotation;

@Retention(RetentionPolicy.RUNTIME)
@SqlStatementCustomizingAnnotation(BindIn.CustomizerFactory.class)
@BindingAnnotation(BindIn.BindingFactory.class)
public @interface BindIn
{
    String value();

    final class CustomizerFactory implements SqlStatementCustomizerFactory
    {
        @Override
        public SqlStatementCustomizer createForParameter(Annotation annotation,
                                                         Class<?> sqlObjectType,
                                                         Method method,
                                                         Object arg)
        {
            Collection<?> coll = (Collection<?>) arg;
            BindIn in = (BindIn) annotation;
            final String key = in.value();
            final List<String> ids = new ArrayList<String>();
            for (int idx = 0; idx < coll.size(); idx++) {
                ids.add("__" + key + "_" + idx);
            }

            StringBuilder names = new StringBuilder();
            for (Iterator<String> i = ids.iterator(); i.hasNext();) {
                names.append(":").append(i.next());
                if (i.hasNext()) {
                    names.append(",");
                }
            }
            final String ns = names.toString();

            return q -> q.define(key, ns);
        }
    }

    class BindingFactory implements BinderFactory<Annotation, Object>
    {
        @Override
        public Binder<Annotation, Object> build(Annotation annotation)
        {
            final BindIn in = (BindIn) annotation;
            final String key = in.value();

            return (q, param, bind, arg) -> {
                Iterable<?> coll = (Iterable<?>) arg;
                ResolvedType elementType = new TypeResolver().resolve(Object.class);
                ResolvedType parameterType = new TypeResolver().resolve(param.getParameterizedType());
                if (Iterable.class.isAssignableFrom(parameterType.getErasedType())) {
                    List<ResolvedType> typeParameters = parameterType.typeParametersFor(Iterable.class);
                    if (!typeParameters.isEmpty()) {
                        elementType = typeParameters.get(0);
                    }
                }
                int idx = 0;
                for (Object s : coll) {
                    q.dynamicBind(elementType, "__" + key + "_" + idx++, s);
                }
            };
        }
    }
}
