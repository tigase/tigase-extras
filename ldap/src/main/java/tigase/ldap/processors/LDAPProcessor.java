/*
 * Tigase Server Extras LDAP Server - Extra modules to Tigase Server
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
package tigase.ldap.processors;

import com.unboundid.ldap.protocol.ProtocolOp;

import java.util.function.Consumer;

/**
 * Interface to be implemented by LDAP protocol request handlers
 * @param <T> Class of the request that is supported
 */
public interface LDAPProcessor<T extends ProtocolOp> {

	/**
	 * Method returns class that is supported by this processor
	 * @return class of ProtocolOp supported by processor
	 */
	Class<T> getSupportedProtocolOp();

	/**
	 * Method responsible for actual execution of the request
	 * @param session instance of LDAPSession containing authenticated user data
	 * @param request instance of the request to be executed
	 * @param consumer method to call when processing is finished
	 * @throws Exception if unrecoverable error will happen
	 */
	void process(LDAPSession session, T request, Consumer<ProtocolOp> consumer)
			throws Exception;

	/**
	 * Method checks if passed parameter can be processed by this processor
	 * @param op instance of ProtocolOp containing request data
	 * @return true if can be processed
	 */
	default boolean canHandle(ProtocolOp op) {
		return getSupportedProtocolOp().isAssignableFrom(op.getClass());
	}
	
}
