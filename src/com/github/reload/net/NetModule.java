package com.github.reload.net;

import javax.inject.Named;
import javax.inject.Singleton;
import com.github.reload.net.codecs.Codec;
import com.github.reload.net.codecs.FramedMessageCodec;
import com.github.reload.net.codecs.Header;
import com.github.reload.net.codecs.MessageBuilder;
import com.github.reload.net.codecs.MessageEncoder;
import com.github.reload.net.codecs.MessageHeaderDecoder;
import com.github.reload.net.codecs.MessagePayloadDecoder;
import com.github.reload.net.codecs.content.Content;
import com.github.reload.net.codecs.secBlock.SecurityBlock;
import com.github.reload.net.stack.ForwardingHandler;
import com.github.reload.net.stack.MessageAuthenticator;
import com.github.reload.net.stack.MessageDispatcher;
import com.github.reload.net.stack.ReloadStackBuilder.ClientStackBuilder;
import com.github.reload.net.stack.ReloadStackBuilder.ServerStackBuilder;
import com.github.reload.net.stack.ReloadStackBuilder.StackInitializer;
import com.github.reload.net.stack.SRLinkHandler;
import dagger.Module;
import dagger.ObjectGraph;
import dagger.Provides;

@Module(injects = {MessageBuilder.class, ConnectionManager.class,
					MessageDispatcher.class, ServerStackBuilder.class,
					ClientStackBuilder.class, FramedMessageCodec.class,
					MessageHeaderDecoder.class, ForwardingHandler.class,
					MessagePayloadDecoder.class, MessageAuthenticator.class,
					MessageEncoder.class, FramedMessageCodec.class,
					MessageDispatcher.class, Codec.class, SRLinkHandler.class,
					StackInitializer.class}, library = true, complete = false)
public class NetModule {

	@Provides
	@Singleton
	@Named("headerCodec")
	Codec<Header> provideHeaderCodec(ObjectGraph graph) {
		return Codec.getCodec(Header.class, graph);
	}

	@Provides
	@Singleton
	@Named("contentCodec")
	Codec<Content> provideContentCodec(ObjectGraph graph) {
		return Codec.getCodec(Content.class, graph);
	}

	@Provides
	@Singleton
	@Named("secBlockCodec")
	Codec<SecurityBlock> provideSecBlockCodec(ObjectGraph graph) {
		return Codec.getCodec(SecurityBlock.class, graph);
	}

}
