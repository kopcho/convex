package convex.net.message;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import convex.core.Belief;
import convex.core.Result;
import convex.core.data.ACell;
import convex.core.data.AVector;
import convex.core.data.Blob;
import convex.core.data.Format;
import convex.core.data.Hash;
import convex.core.data.SignedData;
import convex.core.data.Tag;
import convex.core.data.prim.CVMLong;
import convex.core.exceptions.BadFormatException;
import convex.core.lang.RT;
import convex.core.util.Utils;
import convex.net.Connection;
import convex.net.MessageType;

/**
 * <p>Class representing a message to / from a specific connection</p>
 * 
 * <p>Encapsulates both message content and a means of return communication</p>.
 *
 * <p>This class is an immutable data structure, but NOT a representable on-chain
 * data structure, as it is part of the peer protocol layer.</p>
 *
 * <p>Messages may contain a Payload, which can be any Data Object.</p>
 */
public abstract class Message {
	
	static final Logger log = LoggerFactory.getLogger(Message.class.getName());

	protected ACell payload;
	protected Blob messageData; // encoding of payload
	protected MessageType type;

	protected Message(MessageType type, ACell payload, Blob data) {
		this.type = type;
		this.messageData=data;
		this.payload = payload;
	}

	public static MessageRemote create(Connection peerConnection, MessageType type, ACell payload) {
		return new MessageRemote(peerConnection, type, payload,null);
	}
	
	public static MessageRemote createMessage(Connection peerConnection, MessageType type, Blob message) {
		return new MessageRemote(peerConnection, type, null, message);
	}


	public static Message createData(ACell o) {
		return create(null,MessageType.DATA,o);
	}

	public static Message createBelief(Belief belief) {
		return create(null,MessageType.BELIEF,belief);
	}

	public static Message createChallenge(SignedData<ACell> challenge) {
		return create(null,MessageType.CHALLENGE, challenge);
	}

	public static Message createResponse(SignedData<ACell> response) {
		return create(null,MessageType.RESPONSE, response);
	}

	public static Message createGoodBye(SignedData<ACell> peerKey) {
		return create(null,MessageType.GOODBYE, peerKey);
	}



	@SuppressWarnings("unchecked")
	public <T extends ACell> T getPayload() {
		if (payload==null) {
			if (messageData==null) throw new IllegalStateException("Null payload and data in Message?!? Type = "+type);
			if ((messageData.count()==1)&&(messageData.byteAt(0)==Tag.NULL)) return null;
			try {
				// TODO: should probably expose checked exception here?
				payload=Format.decodeMultiCell(messageData);
			} catch (BadFormatException e) {
				log.warn("Bad format in Message payload",e);
				throw Utils.sneakyThrow(e);
			}
		}
		return (T) payload;
	}
	
	/**
	 * Gets the encoded data for this message. Generates a single cell encoding if required.
	 * @return Blob containing message data
	 */
	public Blob getMessageData() {
		if (messageData==null) {
			messageData=Format.encodedBlob(payload);
		}
		return messageData;
	}

	public MessageType getType() {
		return type;
	}

	@Override
	public String toString() {
		return "#message {:type " + getType() + " :payload " + RT.print(getPayload(),1000) + "}";
	}

	/**
	 * Gets the message ID for correlation, assuming this message type supports IDs.
	 *
	 * @return Message ID, or null if the message type does not use message IDs
	 */
	public CVMLong getID() {
		switch (type) {
			// Query and transact use a vector [ID ...]
			case QUERY:
			case TRANSACT: return (CVMLong) ((AVector<?>)getPayload()).get(0);

			// Result is a special record type
			case RESULT: return (CVMLong)((Result)getPayload()).getID();

			// Status ID is the single value
			case STATUS: return (CVMLong)(getPayload());

			default: return null;
		}
	}

	/**
	 * Reports a result back to the originator of the message.
	 * 
	 * Will set a Result ID if necessary.
	 * 
	 * @param res Result record
	 * @return True if reported successfully, false otherwise
	 */
	public abstract boolean reportResult(Result res);

	/**
	 * Report a result for a given message ID
	 * @param id Message ID
	 * @param reply Value for result
	 * @return True if reported successfully, false otherwise
	 */
	public abstract boolean reportResult(CVMLong id, ACell reply);
	
	/**
	 * Gets a String identifying the origin of the message. Used for logging.
	 * @return String representing message origin
	 */
	public abstract String getOriginString();

	/**
	 * Sends a cell of data to the connected Peer
	 * @param data Data to send
	 * @return true if data sent, false otherwise
	 */
	public abstract boolean sendData(ACell data);

	/**
	 * Sends a missing data request to the connected Peer
	 * @param hash Hash of missing data
	 * @return True if request sent, false otherwise
	 */
	public abstract boolean sendMissingData(Hash hash);

	/**
	 * Gets the Connection instance associated with this message, or null if no
	 * connection exists (presumably a local Message) 
	 * @return Connection instance
	 */
	public abstract Connection getConnection();

	public boolean hasData() {
		return messageData!=null;
	}




}
