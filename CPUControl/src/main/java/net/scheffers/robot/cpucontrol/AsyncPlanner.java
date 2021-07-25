package net.scheffers.robot.cpucontrol;

import java.util.LinkedList;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;

public class AsyncPlanner<X> extends CompletableFuture<X> {
	
	public LinkedList<BiConsumer<?, AsyncPlanner<X>>> actions;
	
	public AsyncPlanner() {
		actions = new LinkedList<>();
	}
	
	public AsyncPlanner<X> plan(BiConsumer<?, AsyncPlanner<X>> action) {
		actions.add(action);
		return this;
	}
	
	public void next(CompletableFuture<?> future) {
		future.thenAccept(this::acceptor);
	}
	
	public AsyncPlanner<X> start(CompletableFuture<?> future) {
		future.thenAccept(this::acceptor);
		return this;
	}
	
	protected <T> void acceptor(T x) {
		try {
			BiConsumer<T, AsyncPlanner<X>> action = (BiConsumer<T, AsyncPlanner<X>>) actions.pop();
			action.accept(x, this);
		} catch (Exception e) {
			completeExceptionally(e);
		}
	}
	
}
