package convex.core.lang.ops;

import convex.core.data.ACell;
import convex.core.data.ASequence;
import convex.core.data.AVector;
import convex.core.data.Blob;
import convex.core.data.BlobBuilder;
import convex.core.data.Format;
import convex.core.data.Vectors;
import convex.core.exceptions.BadFormatException;
import convex.core.lang.AOp;
import convex.core.lang.Context;
import convex.core.lang.Juice;
import convex.core.lang.Ops;

/**
 * Op for executing a sequence of child operations in order
 *
 * "Design is to take things apart in such a way that they can be put back
 * together" 
 * - Rich Hickey
 *
 * @param <T> Result type of Do Op
 */
public class Do<T extends ACell> extends AMultiOp<T> {

	public static final Do<?> EMPTY = Do.create();

	protected Do(AVector<AOp<ACell>> ops) {
		super(ops);
	}

	public static <T extends ACell> Do<T> create(AOp<?>... ops) {
		return new Do<T>(Vectors.create(ops));
	}

	@Override
	protected Do<T> recreate(ASequence<AOp<ACell>> newOps) {
		if (ops == newOps) return this;
		return new Do<T>(newOps.toVector());
	}

	public static <T extends ACell> Do<T> create(ASequence<AOp<ACell>> ops) {
		return new Do<T>(ops.toVector());
	}

	@Override
	public Context execute(Context context) {
		int n = ops.size();
		if (n == 0) return context.withResult(Juice.DO,  null); // need cast to avoid bindings overload

		Context ctx = context.consumeJuice(Juice.DO);
		if (ctx.isExceptional()) return ctx;
		
		// execute each operation in turn
		// TODO: early return
		for (int i = 0; i < n; i++) {
			AOp<?> op = ops.get(i);
			ctx = ctx.execute(op);

			if (ctx.isExceptional()) break;

		}
		return ctx;
	}

	@Override
	public boolean print(BlobBuilder bb, long limit) {
		bb.append("(do");
		int len = ops.size();
		for (int i = 0; i < len; i++) {
			bb.append(' ');
			if (!ops.get(i).print(bb,limit)) return false;
		}
		bb.append(')');
		return bb.check(limit);
	}

	@Override
	public byte opCode() {
		return Ops.DO;
	}

	/**
	 * Decodes a Do op from a Blob encoding
	 * 
	 * @param <T>
	 * @param b Blob to read from
	 * @param pos Start position in Blob (location of tag byte)
	 * @return New decoded instance
	 * @throws BadFormatException In the event of any encoding error
	 */
	public static <T extends ACell> Do<T> read(Blob b, int pos) throws BadFormatException {
		int epos=pos+2; // skip tag and opcode
		AVector<AOp<ACell>> ops = Format.read(b,epos);
		epos+=Format.getEncodingLength(ops);
		
		Do<T> result=create(ops);
		result.attachEncoding(b.slice(pos, epos));
		return result;
	}
}
