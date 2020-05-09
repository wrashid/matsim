package org.matsim.core.mobsim.qsim.messagequeueengine;

import com.google.inject.Module;
import org.matsim.core.config.Config;
import org.matsim.core.mobsim.framework.listeners.MobsimListener;
import org.matsim.core.mobsim.jdeqsim.MessageQueueImpl;
import org.matsim.core.mobsim.qsim.AbstractQSimPlugin;
import org.matsim.core.mobsim.qsim.jdeqsimengine.SteppableSchedulerImpl;

import java.util.Collection;
import java.util.Collections;

public class MessageQueuePlugin extends AbstractQSimPlugin{

	public MessageQueuePlugin(Config config) {
		super(config);
	}

	@Override
	public Collection<? extends Module> modules() {
		return Collections.singletonList(new com.google.inject.AbstractModule() {
			@Override
			protected void configure() {
				bind(MessageQueueImpl.class).asEagerSingleton();
				bind(SteppableSchedulerImpl.class).asEagerSingleton();
				bind(MessageQueueEngine.class).asEagerSingleton();
			}
		});
	}

	@Override
	public Collection<Class<? extends MobsimListener>> listeners() {
		return Collections.singletonList(MessageQueueEngine.class);
	}
}
