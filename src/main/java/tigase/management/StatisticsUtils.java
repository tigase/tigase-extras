/**
 * Tigase Server Extras - Extra modules to Tigase Server
 * Copyright (C) 2007 Tigase, Inc. (office@tigase.com)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, version 3 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. Look for COPYING file in the top folder.
 * If not, see http://www.gnu.org/licenses/.
 */
package tigase.management;

//~--- non-JDK imports --------------------------------------------------------

import tigase.conf.ConfiguratorAbstract;

import tigase.stats.StatisticsProviderMBean;

import static tigase.management.StatisticsConstatnts.*;

//~--- JDK imports ------------------------------------------------------------

import java.util.Map;

//~--- classes ----------------------------------------------------------------

/**
 * Created: Jan 10, 2009 5:26:51 PM
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public class StatisticsUtils {

	/**
	 * Method description
	 *
	 *
	 * @param comp
	 * @param stat
	 * @param def
	 *
	 * @return
	 */
	public static Long getStatsValue(String comp, String stat, Long def) {
		Long result = def;
		String strVal = getStatsValue(comp, stat, def.toString());

		try {
			result = Long.parseLong(strVal);
		} catch (Exception e) {
			result = def;
		}

		return result;
	}

	/**
	 * Method description
	 *
	 *
	 * @param comp
	 * @param stat
	 * @param def
	 *
	 * @return
	 */
	public static String getStatsValue(String comp, String stat, String def) {
		StatisticsProviderMBean stats =
			(StatisticsProviderMBean) ConfiguratorAbstract.getMXBean(STATS_MXBEAN_NAME);
		String result = null;

		if (stats != null) {
			Map<String, String> map = stats.getComponentStats(comp, 0);

			if (map != null) {
				result = map.get(comp + "/" + stat);
			}
		}

		return (result != null) ? result : def;
	}
}


//~ Formatted in Sun Code Convention


//~ Formatted by Jindent --- http://www.jindent.com
