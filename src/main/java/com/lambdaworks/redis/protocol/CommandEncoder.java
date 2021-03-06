// Copyright (C) 2011 - Will Glozer.  All rights reserved.

package com.lambdaworks.redis.protocol;

import java.nio.charset.Charset;
import java.util.Collection;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

/**
 * A netty {@link ChannelHandler} responsible for encoding commands.
 * 
 * @author <a href="mailto:mpaluch@paluch.biz">Mark Paluch</a>
 */
@ChannelHandler.Sharable
public class CommandEncoder extends MessageToByteEncoder<Object> {

    private static final InternalLogger logger = InternalLoggerFactory.getInstance(CommandEncoder.class);

    /**
     * If TRACE level logging has been enabled at startup.
     */
    private final boolean traceEnabled;

    /**
     * If DEBUG level logging has been enabled at startup.
     */
    private final boolean debugEnabled;

    public CommandEncoder() {
        this(true);
    }

    public CommandEncoder(boolean preferDirect) {
        super(preferDirect);
        traceEnabled = logger.isTraceEnabled();
        debugEnabled = logger.isDebugEnabled();
    }

    @Override
    @SuppressWarnings("unchecked")
    protected void encode(ChannelHandlerContext ctx, Object msg, ByteBuf out) throws Exception {

        if (msg instanceof RedisCommand) {
            RedisCommand<?, ?, ?> command = (RedisCommand<?, ?, ?>) msg;
            encode(ctx, out, command);
        }

        if (msg instanceof Collection) {
            Collection<RedisCommand<?, ?, ?>> commands = (Collection<RedisCommand<?, ?, ?>>) msg;
            for (RedisCommand<?, ?, ?> command : commands) {
                if (command.isCancelled()) {
                    continue;
                }
                encode(ctx, out, command);
            }
        }
    }

    private void encode(ChannelHandlerContext ctx, ByteBuf out, RedisCommand<?, ?, ?> command) {
        command.encode(out);
        if (debugEnabled) {
            logger.debug("{} writing command {}", logPrefix(ctx.channel()), command);
            if (traceEnabled) {
                logger.trace("{} Sent: {}", logPrefix(ctx.channel()), out.toString(Charset.defaultCharset()).trim());
            }
        }
    }

    private String logPrefix(Channel channel) {
        StringBuffer buffer = new StringBuffer(64);
        buffer.append('[').append(ChannelLogDescriptor.logDescriptor(channel)).append(']');
        return buffer.toString();
    }
}
