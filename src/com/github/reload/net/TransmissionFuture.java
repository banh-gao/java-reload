package com.github.reload.net;

import io.netty.util.concurrent.GenericFutureListener;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * This Future object allows to control the transmission status of an outgoing
 * message
 * to the neighbor nodes
 */
public class TransmissionFuture {

	public boolean isDone() {
		// TODO Auto-generated method stub
		return false;
	}

	public boolean isSuccess() {
		// TODO Auto-generated method stub
		return false;
	}

	public Throwable cause() {
		// TODO Auto-generated method stub
		return null;
	}

	public TransmissionFuture addListener(TransmissionFutureListener listener) {
		// TODO Auto-generated method stub
		return null;
	}

	public TransmissionFuture removeListener(GenericFutureListener<? extends Future<? super Object>> listener) {
		// TODO Auto-generated method stub
		return null;
	}

	public TransmissionFuture await() throws InterruptedException {
		// TODO Auto-generated method stub
		return null;
	}

	public boolean await(long timeout, TimeUnit unit) throws InterruptedException {
		// TODO Auto-generated method stub
		return false;
	}

	public boolean await(long timeoutMillis) throws InterruptedException {
		// TODO Auto-generated method stub
		return false;
	}

	public boolean cancel(boolean mayInterruptIfRunning) {
		// TODO Auto-generated method stub
		return false;
	}

	public interface TransmissionFutureListener {

		/**
		 * Invoked when the operation associated with the
		 * {@link TransmissionFuture} has been completed.
		 * 
		 * @param future
		 *            the source {@link TransmissionFuture} which called this
		 *            callback
		 */
		void operationComplete(TransmissionFuture future) throws Exception;
	}
}
