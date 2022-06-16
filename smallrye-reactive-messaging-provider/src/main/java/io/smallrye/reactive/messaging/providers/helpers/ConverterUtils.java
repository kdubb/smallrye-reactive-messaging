package io.smallrye.reactive.messaging.providers.helpers;

import java.lang.reflect.Type;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.enterprise.inject.Instance;
import javax.enterprise.inject.spi.Prioritized;

import org.eclipse.microprofile.reactive.messaging.Message;

import io.smallrye.mutiny.Multi;
import io.smallrye.reactive.messaging.MessageConverter;
import io.smallrye.reactive.messaging.providers.i18n.ProviderLogging;

public class ConverterUtils {

    private ConverterUtils() {
        // Avoid direct instantiation.
    }

    public static Multi<? extends Message<?>> convert(Multi<? extends Message<?>> upstream,
            Instance<MessageConverter> converters, Type injectedPayloadType) {
        if (injectedPayloadType != null) {
            return upstream
                    .flatMap(new Function<Message<?>, Multi<? extends Message<?>>>() {

                        MessageConverter actual;

                        @Override
                        public Multi<? extends Message<?>> apply(Message<?> o) {
                            //noinspection ConstantConditions - it can be `null`
                            if (injectedPayloadType == null) {
                                return noConversion(o);
                            } else if (o.getPayload() != null && o.getPayload().getClass().equals(injectedPayloadType)) {
                                return noConversion(o);
                            }

                            if (actual != null) {
                                // Use the cached converter.
                                return convert(actual, o, injectedPayloadType);
                            } else {
                                if (o.getPayload() != null
                                        && TypeUtils.isAssignable(o.getPayload().getClass(), injectedPayloadType)) {
                                    actual = MessageConverter.IdentityConverter.INSTANCE;
                                    return noConversion(o);
                                }
                                // Lookup and cache
                                for (MessageConverter conv : getSortedConverters(converters)) {
                                    if (conv.canConvert(o, injectedPayloadType)) {
                                        actual = conv;
                                        return convert(actual, o, injectedPayloadType);
                                    }
                                }
                                // No converter found
                                return noConversion(o);
                            }
                        }
                    });
        }
        return upstream;
    }

    private static Multi<? extends Message<?>> convert(MessageConverter converter, Message<?> message,
            Type injectedPayloadType) {
        return Multi.createFrom().item(message)
                .map(msg -> converter.convert(message, injectedPayloadType))
                .onFailure().recoverWithMulti(ex -> {
                    ProviderLogging.log.conversionFailed(ex);
                    return Multi.createFrom().completionStage(message.nack(ex))
                            .flatMap(i -> Multi.createFrom().empty());
                });
    }

    private static Multi<? extends Message<?>> noConversion(Message<?> message) {
        return Multi.createFrom().item(message);
    }

    private static List<MessageConverter> getSortedConverters(Instance<MessageConverter> converters) {
        if (converters.isUnsatisfied()) {
            return Collections.emptyList();
        }

        return converters.stream().sorted(new Comparator<MessageConverter>() { // NOSONAR
            @Override
            public int compare(MessageConverter si1, MessageConverter si2) {
                int p1 = 0;
                int p2 = 0;
                if (si1 instanceof Prioritized) {
                    p1 = ((Prioritized) si1).getPriority();
                }
                if (si2 instanceof Prioritized) {
                    p2 = ((Prioritized) si2).getPriority();
                }
                if (si1.equals(si2)) {
                    return 0;
                }
                return Integer.compare(p1, p2);
            }
        }).collect(Collectors.toList());
    }
}
