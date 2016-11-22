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
package org.jdbi.v3.core;

import static org.jdbi.v3.core.internal.JdbiOptionals.findFirstPresent;
import static org.jdbi.v3.core.internal.JdbiStreams.toStream;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import org.jdbi.v3.core.mapper.BuiltInMapperFactory;
import org.jdbi.v3.core.mapper.ColumnMapper;
import org.jdbi.v3.core.mapper.ColumnMapperFactory;
import org.jdbi.v3.core.mapper.InferredColumnMapperFactory;
import org.jdbi.v3.core.mapper.InferredRowMapperFactory;
import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.mapper.RowMapperFactory;
import org.jdbi.v3.core.util.SingleColumnMapper;

class MappingRegistry
{
    private final List<RowMapperFactory> rowFactories = new CopyOnWriteArrayList<>();
    private final ConcurrentHashMap<Type, RowMapper<?>> rowCache = new ConcurrentHashMap<>();

    private final List<ColumnMapperFactory> columnFactories = new CopyOnWriteArrayList<>();
    private final ConcurrentHashMap<Type, ColumnMapper<?>> columnCache = new ConcurrentHashMap<>();

    MappingRegistry() {
        columnFactories.add(new BuiltInMapperFactory());
        columnFactories.add(new SqlArrayMapperFactory());
    }

    private MappingRegistry(MappingRegistry that) {
        rowFactories.addAll(that.rowFactories);
        rowCache.putAll(that.rowCache);
        columnFactories.addAll(that.columnFactories);
        columnCache.putAll(that.columnCache);
    }

    static MappingRegistry copyOf(MappingRegistry registry) {
        return new MappingRegistry(registry);
    }

    public void addRowMapper(RowMapper<?> mapper)
    {
        this.addRowMapper(new InferredRowMapperFactory(mapper));
    }

    public void addRowMapper(RowMapperFactory factory)
    {
        rowFactories.add(0, factory);
        rowCache.clear();
    }

    public Optional<RowMapper<?>> findRowMapperFor(Type type, StatementContext ctx) {
        // ConcurrentHashMap can enter an infinite loop on nested computeIfAbsent calls.
        // Since row mappers can decorate other row mappers, we have to populate the cache the old fashioned way.
        // See https://bugs.openjdk.java.net/browse/JDK-8062841, https://bugs.openjdk.java.net/browse/JDK-8142175
        RowMapper<?> cached = rowCache.get(type);

        if (cached != null) {
            return Optional.of(cached);
        }

        Optional<RowMapper<?>> mapper = findFirstPresent(
                () -> rowFactories.stream()
                        .flatMap(factory -> toStream(factory.build(type, ctx)))
                        .findFirst(),
                () -> findColumnMapperFor(type, ctx)
                        .map(c -> new SingleColumnMapper<>(c)));

        mapper.ifPresent(m -> rowCache.put(type, m));

        return mapper;
    }

    public void addColumnMapper(ColumnMapper<?> mapper)
    {
        this.addColumnMapper(new InferredColumnMapperFactory(mapper));
    }

    public void addColumnMapper(ColumnMapperFactory factory) {
        columnFactories.add(0, factory);
        columnCache.clear();
    }

    public Optional<ColumnMapper<?>> findColumnMapperFor(Type type, StatementContext ctx) {
        // ConcurrentHashMap can enter an infinite loop on nested computeIfAbsent calls.
        // Since column mappers can decorate other column mappers, we have to populate the cache the old fashioned way.
        // See https://bugs.openjdk.java.net/browse/JDK-8062841, https://bugs.openjdk.java.net/browse/JDK-8142175
        ColumnMapper<?> cached = columnCache.get(type);

        if (cached != null) {
            return Optional.of(cached);
        }

        Optional<ColumnMapper<?>> mapper = columnFactories.stream()
                .flatMap(factory -> toStream(factory.build(type, ctx)))
                .findFirst();

        mapper.ifPresent(m -> columnCache.put(type, m));

        return mapper;
    }
}