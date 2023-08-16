package convex.observer;

import java.util.function.Consumer;

import convex.core.data.SignedData;
import convex.core.transactions.ATransaction;
import convex.peer.Server;

public class StrimziKafka {

	public String topic;
	public String url;

	public StrimziKafka() {
		this.topic="test";
		this.url="https://kfk.walledchannel.net:8010/topics/";
	}
	
	public Consumer<SignedData<ATransaction>> makeTransactionRequestObserver(Server s) {
		return tx->{
			
		};
	}
}
