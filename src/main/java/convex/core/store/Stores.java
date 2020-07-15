package convex.core.store;

import java.util.logging.Level;

import etch.store.EtchStore;

public class Stores {
	
	/**
	 * Default store to use in client applications
	 */
	public static final AStore CLIENT_STORE = EtchStore.createTemp();
	
	/**
	 * Default store to use in server applications
	 */
	public static final AStore SERVER_STORE = EtchStore.DEFAULT;

	
	private static final ThreadLocal<AStore> currentStore = new ThreadLocal<>() {
		@Override
		protected AStore initialValue() {
			return CLIENT_STORE;
		}
	};

	/**
	 * Logging level for store persistence. 
	 */
	public static final Level PERSIST_LEVEL = Level.INFO;
	

	/**
	 * Gets the current (thread-local) Store instance
	 * 
	 * @return Store for the current thread
	 */
	public static AStore current() {
		return Stores.currentStore.get();
	}

	/**
	 * Sets the current thread-local store for this thread
	 * 
	 * @param store Any AStore instance
	 */
	public static void setCurrent(AStore store) {
		currentStore.set(store);
	}
}
