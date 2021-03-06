/*
 * Copyright 2016 Netflix, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package io.reactivex.netty.protocol.http.client;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.ssl.SslHandler;
import io.netty.util.concurrent.EventExecutorGroup;
import io.reactivex.netty.client.ChannelProviderFactory;
import io.reactivex.netty.client.ConnectionProviderFactory;
import io.reactivex.netty.client.Host;
import io.reactivex.netty.ssl.SslCodec;
import rx.Observable;
import rx.functions.Action1;
import rx.functions.Func0;
import rx.functions.Func1;

import javax.net.ssl.SSLEngine;
import javax.net.ssl.TrustManagerFactory;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.concurrent.TimeUnit;

/**
 * An HTTP client for executing HTTP requests.
 *
 * <h2>Immutability</h2>
 * An instance of this client is immutable and all mutations produce a new client instance. For this reason it is
 * recommended that the mutations are done during client creation and not during request processing to avoid repeated
 * object creation overhead.
 *
 * @param <I> The type of the content of request.
 * @param <O> The type of the content of response.
 */
public abstract class HttpClient<I, O> extends InterceptingHttpClient<I,O> {

    /**
     * Creates a new client instances, inheriting all configurations from this client and adding the passed read timeout
     * for all requests created by the newly created client instance.
     *
     * @param timeOut Read timeout duration.
     * @param timeUnit Timeunit for the timeout.
     *
     * @return A new {@link HttpClient} instance.
     */
    public abstract HttpClient<I, O> readTimeOut(int timeOut, TimeUnit timeUnit);

    /**
     * Creates a new client instances, inheriting all configurations from this client and adding a
     * {@link ChannelOption} for the connections created by the newly created client instance.
     *
     * @param option Option to add.
     * @param value Value for the option.
     *
     * @return A new {@link HttpClient} instance.
     */
    public abstract <T> HttpClient<I, O> channelOption(ChannelOption<T> option, T value);

    /**
     * Adds a {@link ChannelHandler} to {@link ChannelPipeline} for all connections created by this client. The specified
     * handler is added at the first position of the pipeline as specified by
     * {@link ChannelPipeline#addFirst(String, ChannelHandler)}
     *
     * <em>For better flexibility of pipeline modification, the method {@link #pipelineConfigurator(Action1)} will be more
     * convenient.</em>
     *
     * @param name Name of the handler.
     * @param handlerFactory Factory to create handler instance to add.
     *
     * @return A new {@link HttpClient} instance.
     */
    public abstract <II, OO> HttpClient<II, OO> addChannelHandlerFirst(String name, Func0<ChannelHandler> handlerFactory);

    /**
     * Adds a {@link ChannelHandler} to {@link ChannelPipeline} for all connections created by this client. The specified
     * handler is added at the first position of the pipeline as specified by
     * {@link ChannelPipeline#addFirst(EventExecutorGroup, String, ChannelHandler)}
     *
     * <em>For better flexibility of pipeline modification, the method {@link #pipelineConfigurator(Action1)} will be more
     * convenient.</em>
     *
     * @param group   the {@link EventExecutorGroup} which will be used to execute the {@link ChannelHandler}
     *                 methods
     * @param name     the name of the handler to append
     * @param handlerFactory Factory to create handler instance to add.
     *
     * @return A new {@link HttpClient} instance.
     */
    public abstract <II, OO> HttpClient<II, OO> addChannelHandlerFirst(EventExecutorGroup group, String name,
                                                                       Func0<ChannelHandler> handlerFactory);

    /**
     * Adds a {@link ChannelHandler} to {@link ChannelPipeline} for all connections created by this client. The specified
     * handler is added at the last position of the pipeline as specified by
     * {@link ChannelPipeline#addLast(String, ChannelHandler)}
     *
     * <em>For better flexibility of pipeline modification, the method {@link #pipelineConfigurator(Action1)} will be more
     * convenient.</em>
     *
     * @param name Name of the handler.
     * @param handlerFactory Factory to create handler instance to add.
     *
     * @return A new {@link HttpClient} instance.
     */
    public abstract <II, OO> HttpClient<II, OO>  addChannelHandlerLast(String name, Func0<ChannelHandler> handlerFactory);

    /**
     * Adds a {@link ChannelHandler} to {@link ChannelPipeline} for all connections created by this client. The specified
     * handler is added at the last position of the pipeline as specified by
     * {@link ChannelPipeline#addLast(EventExecutorGroup, String, ChannelHandler)}
     *
     * <em>For better flexibility of pipeline modification, the method {@link #pipelineConfigurator(Action1)} will be more
     * convenient.</em>
     *
     * @param group   the {@link EventExecutorGroup} which will be used to execute the {@link ChannelHandler}
     *                 methods
     * @param name     the name of the handler to append
     * @param handlerFactory Factory to create handler instance to add.
     *
     * @return A new {@link HttpClient} instance.
     */
    public abstract <II, OO> HttpClient<II, OO> addChannelHandlerLast(EventExecutorGroup group, String name,
                                                                      Func0<ChannelHandler> handlerFactory);

    /**
     * Adds a {@link ChannelHandler} to {@link ChannelPipeline} for all connections created by this client. The specified
     * handler is added before an existing handler with the passed {@code baseName} in the pipeline as specified by
     * {@link ChannelPipeline#addBefore(String, String, ChannelHandler)}
     *
     * <em>For better flexibility of pipeline modification, the method {@link #pipelineConfigurator(Action1)} will be more
     * convenient.</em>
     *
     * @param baseName  the name of the existing handler
     * @param name Name of the handler.
     * @param handlerFactory Factory to create handler instance to add.
     *
     * @return A new {@link HttpClient} instance.
     */
    public abstract <II, OO> HttpClient<II, OO> addChannelHandlerBefore(String baseName, String name,
                                                                        Func0<ChannelHandler> handlerFactory);

    /**
     * Adds a {@link ChannelHandler} to {@link ChannelPipeline} for all connections created by this client. The specified
     * handler is added before an existing handler with the passed {@code baseName} in the pipeline as specified by
     * {@link ChannelPipeline#addBefore(EventExecutorGroup, String, String, ChannelHandler)}
     *
     * <em>For better flexibility of pipeline modification, the method {@link #pipelineConfigurator(Action1)} will be more
     * convenient.</em>
     *
     * @param group   the {@link EventExecutorGroup} which will be used to execute the {@link ChannelHandler}
     *                 methods
     * @param baseName  the name of the existing handler
     * @param name     the name of the handler to append
     * @param handlerFactory Factory to create handler instance to add.
     *
     * @return A new {@link HttpClient} instance.
     */
    public abstract <II, OO> HttpClient<II, OO> addChannelHandlerBefore(EventExecutorGroup group, String baseName,
                                                                        String name, Func0<ChannelHandler> handlerFactory);

    /**
     * Adds a {@link ChannelHandler} to {@link ChannelPipeline} for all connections created by this client. The specified
     * handler is added after an existing handler with the passed {@code baseName} in the pipeline as specified by
     * {@link ChannelPipeline#addAfter(String, String, ChannelHandler)}
     *
     * @param baseName  the name of the existing handler
     * @param name Name of the handler.
     * @param handlerFactory Factory to create handler instance to add.
     *
     * @return A new {@link HttpClient} instance.
     */
    public abstract <II, OO> HttpClient<II, OO>  addChannelHandlerAfter(String baseName, String name,
                                                                        Func0<ChannelHandler> handlerFactory);

    /**
     * Adds a {@link ChannelHandler} to {@link ChannelPipeline} for all connections created by this client. The specified
     * handler is added after an existing handler with the passed {@code baseName} in the pipeline as specified by
     * {@link ChannelPipeline#addAfter(EventExecutorGroup, String, String, ChannelHandler)}
     *
     * <em>For better flexibility of pipeline modification, the method {@link #pipelineConfigurator(Action1)} will be more
     * convenient.</em>
     *
     * @param group   the {@link EventExecutorGroup} which will be used to execute the {@link ChannelHandler}
     *                 methods
     * @param baseName  the name of the existing handler
     * @param name     the name of the handler to append
     * @param handlerFactory Factory to create handler instance to add.
     *
     * @return A new {@link HttpClient} instance.
     */
    public abstract <II, OO> HttpClient<II, OO> addChannelHandlerAfter(EventExecutorGroup group, String baseName,
                                                                       String name, Func0<ChannelHandler> handlerFactory);

    /**
     * Creates a new client instances, inheriting all configurations from this client and using the passed
     * action to configure all the connections created by the newly created client instance.
     *
     * @param pipelineConfigurator Action to configure {@link ChannelPipeline}.
     *
     * @return A new {@link HttpClient} instance.
     */
    public abstract <II, OO> HttpClient<II, OO> pipelineConfigurator(Action1<ChannelPipeline> pipelineConfigurator);

    /**
     * Creates a new client instance, inheriting all configurations from this client and using the passed
     * {@code sslEngineFactory} for all secured connections created by the newly created client instance.
     *
     * If the {@link SSLEngine} instance can be statically, created, {@link #secure(SSLEngine)} can be used.
     *
     * @param sslEngineFactory Factory for all secured connections created by the newly created client instance.
     *
     * @return A new {@link HttpClient} instance.
     */
    public abstract HttpClient<I, O> secure(Func1<ByteBufAllocator, SSLEngine> sslEngineFactory);

    /**
     * Creates a new client instance, inheriting all configurations from this client and using the passed
     * {@code sslEngine} for all secured connections created by the newly created client instance.
     *
     * If the {@link SSLEngine} instance can not be statically, created, {@link #secure(Func1)} )} can be used.
     *
     * @param sslEngine {@link SSLEngine} for all secured connections created by the newly created client instance.
     *
     * @return A new {@link HttpClient} instance.
     */
    public abstract HttpClient<I, O> secure(SSLEngine sslEngine);

    /**
     * Creates a new client instance, inheriting all configurations from this client and using the passed
     * {@code sslCodec} for all secured connections created by the newly created client instance.
     *
     * This is required only when the {@link SslHandler} used by {@link SslCodec} is to be modified before adding to
     * the {@link ChannelPipeline}. For most of the cases, {@link #secure(Func1)} or {@link #secure(SSLEngine)} will be
     * enough.
     *
     * @param sslCodec {@link SslCodec} for all secured connections created by the newly created client instance.
     *
     * @return A new {@link HttpClient} instance.
     */
    public abstract HttpClient<I, O> secure(SslCodec sslCodec);

    /**
     * Creates a new client instance, inheriting all configurations from this client and using a trust-all
     * {@link TrustManagerFactory}for all secured connections created by the newly created client instance.
     *
     * <b>This is only for testing and should not be used for real production clients.</b>
     *
     * @return A new {@link HttpClient} instance.
     */
    public abstract HttpClient<I, O> unsafeSecure();

    /**
     * Creates a new client instances, inheriting all configurations from this client and enabling wire logging at the
     * passed level for the newly created client instance.
     *
     * @param wireLoggingLevel Logging level at which the wire logs will be logged. The wire logging will only be done if
     *                        logging is enabled at this level for {@link LoggingHandler}
     *
     * @return A new {@link HttpClient} instance.
     *
     * @deprecated Use {@link #enableWireLogging(String, LogLevel)} instead.
     */
    @Deprecated
    public abstract HttpClient<I, O> enableWireLogging(LogLevel wireLoggingLevel);

    /**
     * Creates a new client instances, inheriting all configurations from this client and enabling wire logging at the
     * passed level for the newly created client instance.
     *
     * @param name Name of the logger that can be used to control the logging dynamically.
     * @param wireLoggingLevel Logging level at which the wire logs will be logged. The wire logging will only be done if
     *                        logging is enabled at this level for {@link LoggingHandler}
     *
     * @return A new {@link HttpClient} instance.
     */
    public abstract HttpClient<I, O> enableWireLogging(String name, LogLevel wireLoggingLevel);

    /**
     * Creates a new client instance, inheriting all configurations from this client and using the passed
     * {@code providerFactory}.
     *
     * @param providerFactory Channel provider factory.
     *
     * @return A new {@link HttpClient} instance.
     */
    public abstract HttpClient<I, O> channelProvider(ChannelProviderFactory providerFactory);

    /**
     * Creates a new HTTP client instance with the passed host and port for the target server.
     *
     * @param host Hostname for the target server.
     * @param port Port for the target server.
     *
     * @return A new {@code HttpClient} instance.
     */
    public static HttpClient<ByteBuf, ByteBuf> newClient(String host, int port) {
        return newClient(new InetSocketAddress(host, port));
    }

    /**
     * Creates a new HTTP client instance with the passed address of the target server.
     *
     * @param serverAddress Socket address for the target server.
     *
     * @return A new {@code HttpClient} instance.
     */
    public static HttpClient<ByteBuf, ByteBuf> newClient(SocketAddress serverAddress) {
        return HttpClientImpl.create(serverAddress);
    }

    /**
     * Creates a new HTTP client instance using the supplied connection provider.
     *
     * @param providerFactory {@link ConnectionProviderFactory} for the client.
     * @param hostStream Stream of hosts for the client.
     *
     * @return A new {@code HttpClient} instance.
     */
    public static HttpClient<ByteBuf, ByteBuf> newClient(ConnectionProviderFactory<ByteBuf, ByteBuf> providerFactory,
                                                         Observable<Host> hostStream) {
        return HttpClientImpl.create(providerFactory, hostStream);
    }

    /**
     * Creates a new client instances, inheriting all configurations from this client and following the passed number of
     * max HTTP redirects.
     *
     * @param maxRedirects Maximum number of redirects to follow for any request.
     *
     * @return A new {@link HttpClient} instance.
     */
    public abstract HttpClient<I, O> followRedirects(int maxRedirects);

    /**
     * Creates a new client instances, inheriting all configurations from this client and enabling/disabling redirects
     * for all requests created by the newly created client instance.
     *
     * @param follow {@code true} to follow redirects. {@code false} to disable any redirects.
     *
     * @return A new {@link HttpClient} instance.
     */
    public abstract HttpClient<I, O> followRedirects(boolean follow);
}
