package io.smallrye.reactive.messaging;

import java.lang.reflect.Type;

import org.eclipse.microprofile.reactive.messaging.Message;

import io.smallrye.common.annotation.Experimental;

/**
 * Converter transforming {@code Message<A>} into {@code Message<B>}.
 * To register a converter, expose a, generally {@code ApplicationScoped} bean, implementing this interface.
 * <p>
 * When multiple converters are available, implementation should override the {@link #getPriority()} method.
 * The default priority is {@link #CONVERTER_DEFAULT_PRIORITY}. Converters with higher priority are executed first.
 *
 * @deprecated Use {@link IncomingMessageConverter} instead.
 */
@Experimental("SmallRye only feature")
@Deprecated
public interface MessageConverter extends AnyMessageConverter {

    /**
     * Converts the given message {@code in} into a {@code Message<T>}.
     * This method is only called after a successful call to {@link #canConvert(Message, Type)} with the given target type.
     *
     * @param in the input message
     * @param target the target type
     * @return the converted message.
     */
    Message<?> convert(Message<?> in, Type target);

    class IdentityConverter implements MessageConverter {

        public static final IdentityConverter INSTANCE = new IdentityConverter();

        private IdentityConverter() {
            // Avoid direct instantiation.
        }

        @Override
        public boolean canConvert(Message<?> in, Type target) {
            return true;
        }

        @Override
        public Message<?> convert(Message<?> in, Type target) {
            return in;
        }
    }

}
