package tigase.pubsub.modules;

import java.util.List;

import tigase.criteria.Criteria;
import tigase.criteria.ElementCriteria;
import tigase.db.UserRepository;
import tigase.pubsub.AbstractModule;
import tigase.pubsub.PubSubConfig;
import tigase.pubsub.exceptions.PubSubErrorCondition;
import tigase.pubsub.exceptions.PubSubException;
import tigase.util.JIDUtils;
import tigase.xml.Element;
import tigase.xmpp.Authorization;

public class SubscribeNodeModule extends AbstractModule {

	private static final Criteria CRIT_SUBSCRIBE = ElementCriteria.nameType("iq", "set").add(
			ElementCriteria.name("pubsub", "http://jabber.org/protocol/pubsub")).add(ElementCriteria.name("subscribe"));

	private UserRepository repository;

	private PubSubConfig config;

	public SubscribeNodeModule(PubSubConfig config, UserRepository pubsubRepository) {
		this.repository = pubsubRepository;
		this.config = config;
	}

	@Override
	public Criteria getModuleCriteria() {
		return CRIT_SUBSCRIBE;
	}

	@Override
	public List<Element> process(Element element) throws PubSubException {
		final Element pubSub = element.getChild("pubsub", "http://jabber.org/protocol/pubsub");
		final Element subscribe = pubSub.getChild("subscribe");

		final String nodeName = subscribe.getAttribute("node");
		final String jid = subscribe.getAttribute("jid");

		try {
			String tmp = repository.getData(config.getServiceName(), nodeName, "owner");
			if (tmp == null) {
				throw new PubSubException(element, Authorization.ITEM_NOT_FOUND);
			}

			if (!JIDUtils.getNodeID(jid).equals(JIDUtils.getNodeID(element.getAttribute("from")))) {
				throw new PubSubException(element, Authorization.BAD_REQUEST, PubSubErrorCondition.INVALID_JID);
			}
			
			// TODO 6.1.3.2 Presence Subscription Required
			// TODO 6.1.3.3 Not in Roster Group
			// TODO 6.1.3.4 Not on Whitelist
			// TODO 6.1.3.5 Payment Required
			// TODO 6.1.3.6 Anonymous Subscriptions Not Allowed
			// TODO 6.1.3.7 Subscription Pending
			// TODO 6.1.3.8 Blocked
			// TODO 6.1.3.9 Subscriptions Not Supported
			// TODO 6.1.3.10 Node Has Moved
			
			repository.setData(config.getServiceName(), nodeName + "/subscribers", jid, "subscribe");

			// repository.setData(config.getServiceName(), nodeName, "owner",
			// JIDUtils.getNodeID(element.getAttribute("from")));

			Element result = createResultIQ(element);
			Element resPubSub = new Element("pubsub", new String[] { "xmlns" }, new String[] { "http://jabber.org/protocol/pubsub" });
			result.addChild(resPubSub);
			Element resSubscription = new Element("subscription");
			resPubSub.addChild(resSubscription);
			resSubscription.setAttribute("node", nodeName);
			resSubscription.setAttribute("jid", jid);
			resSubscription.setAttribute("subscription", "subscribed");

			return makeArray(result);
		} catch (PubSubException e1) {
			throw e1;
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}

	}

}
