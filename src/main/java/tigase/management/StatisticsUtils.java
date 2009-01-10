/*
 * Tigase Jabber/XMPP Server
 * Copyright (C) 2004-2008 "Artur Hefczyc" <artur.hefczyc@tigase.org>
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3 of the License.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program. Look for COPYING file in the top folder.
 * If not, see http://www.gnu.org/licenses/.
 * 
 * $Rev$
 * Last modified by $Author$
 * $Date$
 */

package tigase.management;

import java.util.Map;
import tigase.conf.Configurator;
import tigase.stats.StatisticsProviderMBean;
import static tigase.management.StatisticsConstatnts.*;

/**
 * Created: Jan 10, 2009 5:26:51 PM
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public class StatisticsUtils {

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

	public static String getStatsValue(String comp, String stat, String def) {
		StatisticsProviderMBean stats =
						(StatisticsProviderMBean) Configurator.getMXBean(STATS_MXBEAN_NAME);
		String result = null;
		if (stats != null) {
			Map<String, String> map = stats.getComponentStats(comp, 0);
			if (map != null) {
				result = map.get(comp + "/" + stat);
			}
		}
		return result != null ? result : def;
	}


}
