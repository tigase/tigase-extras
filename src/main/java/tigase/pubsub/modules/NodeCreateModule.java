package tigase.pubsub.modules;

import java.util.List;

import tigase.criteria.Criteria;
import tigase.criteria.ElementCriteria;
import tigase.db.UserRepository;
import tigase.pubsub.AbstractModule;
import tigase.pubsub.PubSubConfig;
import tigase.pubsub.exceptions.PubSubException;
import tigase.util.JIDUtils;
import tigase.xml.Element;
import tigase.xmpp.Authorization;

/**
 * Case 8.1.2
 * 
 * @author bmalkow
 * 
 */
public class NodeCreateModule extends AbstractModule {

	private static final Criteria CRIT_CREATE = ElementCriteria.nameType("iq", "set").add(ElementCriteria.name("pubsub", "http://jabber.org/protocol/pubsub")).add(
			ElementCriteria.name("create"));

	private final PubSubConfig config;

	private final UserRepository repository;

	public NodeCreateModule(final PubSubConfig config, final UserRepository pubsubRepository) {
		this.repository = pubsubRepository;
		this.config = config;
	}

	@Override
	public Criteria getModuleCriteria() {
		return CRIT_CREATE;
	}

	@Override
	public List<Element> process(Element element) throws PubSubException {
		final Element pubSub = element.getChild("pubsub", "http://jabber.org/protocol/pubsub");
		final Element create = pubSub.getChild("create");
		final Element configue = pubSub.getChild("configure");

		final String nodeName = create.getAttribute("node");
		try {
			String tmp = repository.getData(config.getServiceName(), nodeName, "owner");
			if (tmp != null) {
				throw new PubSubException(element, Authorization.CONFLICT);
			}

			repository.setData(config.getServiceName(), nodeName, "owner", JIDUtils.getNodeID(element.getAttribute("from")));
			return createResultIQArray(element);
		} catch (PubSubException e1) {
			throw e1;
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}

	}
}
