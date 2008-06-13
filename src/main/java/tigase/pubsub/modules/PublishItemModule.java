package tigase.pubsub.modules;

import java.util.ArrayList;
import java.util.List;

import tigase.criteria.Criteria;
import tigase.criteria.ElementCriteria;
import tigase.db.UserRepository;
import tigase.pubsub.AbstractModule;
import tigase.pubsub.PubSubConfig;
import tigase.pubsub.exceptions.PubSubErrorCondition;
import tigase.pubsub.exceptions.PubSubException;
import tigase.server.Packet;
import tigase.util.JIDUtils;
import tigase.xml.Element;
import tigase.xmpp.Authorization;
import tigase.xmpp.PacketErrorTypeException;

public class PublishItemModule extends AbstractModule {

	private static final Criteria CRIT_PUBLISH = ElementCriteria.nameType("iq", "set").add(ElementCriteria.name("pubsub", "http://jabber.org/protocol/pubsub")).add(
			ElementCriteria.name("publish"));

	private UserRepository repository;

	private PubSubConfig config;

	public PublishItemModule(PubSubConfig config, UserRepository pubsubRepository) {
		this.repository = pubsubRepository;
		this.config = config;
	}

	@Override
	public Criteria getModuleCriteria() {
		return CRIT_PUBLISH;
	}

	private Element createNotification(final Element publish, final String fromJID, final String toJID) {
		Element message = new Element("message");
		message.setAttribute("from", fromJID);
		message.setAttribute("to", toJID);

		String nodeName = publish.getAttribute("node");

		Element event = new Element("event", new String[] { "xmlns" }, new String[] { "http://jabber.org/protocol/pubsub#event" });
		message.addChild(event);

		Element items = new Element("items", new String[] { "node" }, new String[] { nodeName });
		event.addChild(items);

		for (Element si : publish.getChildren()) {
			if (!"item".equals(si.getName())) {
				continue;
			}
			items.addChild(si);
		}

		return message;
	}

	@Override
	public List<Element> process(Element element) throws PubSubException {
		final Element pubSub = element.getChild("pubsub", "http://jabber.org/protocol/pubsub");
		final Element publish = pubSub.getChild("publish");

		final String nodeName = publish.getAttribute("node");

		try {
			String tmp = repository.getData(config.getServiceName(), nodeName, "owner");
			if (tmp == null) {
				throw new PubSubException(element, Authorization.ITEM_NOT_FOUND);
			}

			// TODO 7.1.3.1 Insufficient Privileges
			// TODO 7.1.3.2 Item Publication Not Supported
			// TODO 7.1.3.3 Node Does Not Exist
			// TODO 7.1.3.4 Payload Too Big
			// TODO 7.1.3.5 Bad Payload
			// TODO 7.1.3.6 Request Does Not Match Configuration

			List<Element> result = new ArrayList<Element>();
			result.add(createResultIQ(element));

			for (String jid : repository.getKeys(config.getServiceName(), nodeName + "/subscribers")) {
				final String jidTO = jid;
				final String jidFrom = element.getAttribute("to");
				Element notification = createNotification(publish, jidFrom, jidTO);
				result.add(notification);
			}

			// // XXX

			return result;
		} catch (PubSubException e1) {
			throw e1;
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}

	}

}
