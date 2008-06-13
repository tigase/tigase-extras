package tigase.pubsub.exceptions;

import tigase.xml.Element;

public class PubSubErrorCondition {

	public static final PubSubErrorCondition INVALID_JID = new PubSubErrorCondition("invalid-jid");  
	
	protected static final String XMLNS = "http://jabber.org/protocol/pubsub#errors";

	private final String condition;

	protected PubSubErrorCondition(String condition) {
		this.condition = condition;
	}

	public Element getElement() {
		Element result = new Element(condition);
		result.addAttribute("xmlns", XMLNS);
		
		return result;
	}
	
}
