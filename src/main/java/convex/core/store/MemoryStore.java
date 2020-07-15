package convex.core.store;

import java.util.HashMap;
import java.util.function.Consumer;
import java.util.logging.Logger;

import convex.core.crypto.Hash;
import convex.core.data.IRefContainer;
import convex.core.data.Ref;
import convex.core.util.Utils;

/**
 * Class implementing caching and storage of hashed node data
 * 
 * Persists refs as direct refs, i.e. retains fully in memory
 */
public class MemoryStore extends AStore {
	public static final MemoryStore DEFAULT = new MemoryStore();
	
	private static final Logger log = Logger.getLogger(MemoryStore.class.getName());

	/**
	 * Storage of persisted Refs for each hash value
	 */
	private final HashMap<Hash, Ref<?>> hashRefs = new HashMap<Hash, Ref<?>>();

	@Override
	@SuppressWarnings("unchecked")
	public <T> Ref<T> refForHash(Hash hash) {
		Ref<T> ref = (Ref<T>) hashRefs.get(hash);
		return ref;
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> Ref<T> persistRef(Ref<T> ref, Consumer<Ref<T>> noveltyHandler) {

		// check store for existing ref first. Return this is we have it
		Hash hash = ref.getHash();
		Ref<T> existing = refForHash(hash);
		if (existing != null) return existing;

		// Convert to direct Ref. Don't want to store a soft ref!
		ref = ref.toDirect();

		T o = ref.getValue();
		if (o instanceof IRefContainer) {
			// need to do recursive persistence
			o = ((IRefContainer) o).updateRefs(r -> {
				return r.persist(noveltyHandler);
			});
		}
		ref=ref.withValue(o);
		final T oTemp=o;
		log.log(Stores.PERSIST_LEVEL,()->"Persisting ref "+hash.toHexString()+" of class "+Utils.getClassName(oTemp)+" with store "+this);

		
		hashRefs.put(hash, ref);

		if (noveltyHandler != null) noveltyHandler.accept(ref);

		return ref.withMinimumStatus(Ref.PERSISTED);
	}

	@Override
	public <T> Ref<T> storeRef(Ref<T> ref, Consumer<Ref<T>> noveltyHandler) {

		// check store for existing ref first. Return this is we have it
		Hash hash = ref.getHash();
		Ref<T> existing = refForHash(hash);
		if (existing != null) return existing;

		// Convert to direct Ref. Don't want to store a soft ref!
		ref = ref.toDirect();

		hashRefs.put(hash, ref);

		if (noveltyHandler != null) noveltyHandler.accept(ref);
		return ref.withMinimumStatus(Ref.STORED);
	}
}
