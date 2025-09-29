package dev.railroadide.switchboard.json;

import com.google.gson.Gson;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Optional;

/**
 * Serialises {@link Optional} values by delegating to their contained type adapter.
 */
public final class OptionalTypeAdapterFactory implements TypeAdapterFactory {

    @Override
    @SuppressWarnings("unchecked")
    public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> typeToken) {
        if (!Optional.class.isAssignableFrom(typeToken.getRawType()))
            return null;

        Type type = typeToken.getType();
        if (!(type instanceof ParameterizedType parameterizedType))
            throw new IllegalArgumentException("Optional must be parameterized with a value type");

        Type valueType = parameterizedType.getActualTypeArguments()[0];
        TypeAdapter<?> valueAdapter = gson.getAdapter(TypeToken.get(valueType));
        return (TypeAdapter<T>) new OptionalTypeAdapter<>(valueAdapter);
    }

    private static final class OptionalTypeAdapter<V> extends TypeAdapter<Optional<V>> {
        private final TypeAdapter<V> valueAdapter;

        private OptionalTypeAdapter(TypeAdapter<V> valueAdapter) {
            this.valueAdapter = valueAdapter;
        }

        @Override
        public void write(JsonWriter out, Optional<V> optional) throws IOException {
            if (optional == null || optional.isEmpty()) {
                out.nullValue();
                return;
            }

            V value = optional.get();
            valueAdapter.write(out, value);
        }

        @Override
        public Optional<V> read(JsonReader in) throws IOException {
            if (in.peek() == JsonToken.NULL) {
                in.nextNull();
                return Optional.empty();
            }

            V value = valueAdapter.read(in);
            return Optional.ofNullable(value);
        }
    }
}
