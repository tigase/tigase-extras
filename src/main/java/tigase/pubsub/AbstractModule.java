package tigase.pubsub;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import tigase.xml.Element;

public abstract class AbstractModule implements Module {

	public static Element createResultIQ(Element iq) {
		return new Element("iq", new String[] { "type", "from", "to", "id" }, new String[] { "result", iq.getAttribute("to"), iq.getAttribute("from"),
				iq.getAttribute("id") });
	}

	public static List<Element> createResultIQArray(Element iq) {
		return makeArray(createResultIQ(iq));
	}

	public static List<Element> makeArray(Element... elements) {
		ArrayList<Element> result = new ArrayList<Element>();
		for (Element element : elements) {
			result.add(element);

		}
		return result;
	}

	protected Logger log = Logger.getLogger(this.getClass().getName());

}
