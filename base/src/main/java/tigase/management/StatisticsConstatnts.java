/*
 * Tigase Server Extras Base - Extra modules to Tigase Server
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

/**
 * Created: Jan 10, 2009 6:02:24 PM
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public interface StatisticsConstatnts {

	public static final String STATS_MXBEAN_NAME = "tigase.stats:type=StatisticsProvider";

	public static final String MR_COMP = "message-router";

	public static final String BOSH_COMP = "bosh";

	public static final String SRV_COMP = "s2s";

	public static final String CLI_COMP = "c2s";

	public static final String SM_COMP = "sess-man";

	public static final String PUBSUB_COMP = "pubsub";

	public static final String MUC_COMP = "muc";

	public static final String UPTIME_STAT = "Uptime";

	public static final String OPEN_CONN_STAT = "Open connections";

	public static final String REGISTERED_USERS_STAT = "Registered accounts";

	public static final String OPEN_SESSIONS_STAT = "Open authorized sessions";

	public static final String LAST_SECOND_STAT = "Last second packets";

	public static final String LAST_MINUTE_STAT = "Last minute packets";

	public static final String LAST_HOUR_STAT = "Last hour packets";

	//public static final Long DEF_LONG = 1L;
	//public static final String DEF_STR = "unknown";

}
