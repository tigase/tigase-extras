package tigase.pubsub;

import java.util.List;

import tigase.criteria.Criteria;
import tigase.pubsub.exceptions.PubSubException;
import tigase.xml.Element;

public interface Module {

	Criteria getModuleCriteria();

	List<Element> process(final Element element) throws PubSubException;
}
