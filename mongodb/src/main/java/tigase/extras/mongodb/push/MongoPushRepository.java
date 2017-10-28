/*
 * MongoPushRepository.java
 *
 * Tigase Jabber/XMPP Server - Extras
 * Copyright (C) 2004-2017 "Tigase, Inc." <office@tigase.com>
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
package tigase.extras.mongodb.push;

import com.mongodb.Block;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.Indexes;
import com.mongodb.client.model.Projections;
import org.bson.Document;
import tigase.component.exceptions.ComponentException;
import tigase.component.exceptions.RepositoryException;
import tigase.db.Repository;
import tigase.db.TigaseDBException;
import tigase.mongodb.MongoDataSource;
import tigase.push.Device;
import tigase.push.PushSettings;
import tigase.push.api.IPushSettings;
import tigase.push.repositories.AbstractPushRepository;
import tigase.push.repositories.Schema;
import tigase.xmpp.Authorization;
import tigase.xmpp.jid.BareJID;

import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import static tigase.mongodb.Helper.collectionExists;

/**
 * Created by andrzej on 16.05.2017.
 */
@Repository.Meta( supportedUris = {"mongodb:.*" } )
@Repository.SchemaId(id = Schema.PUSH_SCHEMA_ID, name = Schema.PUSH_SCHEMA_NAME)
public class MongoPushRepository
		extends AbstractPushRepository<MongoDataSource> {

	private static final String HASH_ALG = "SHA-256";
	private static final Charset UTF8 = Charset.forName("UTF-8");

	private static final String PUSH_DEVICES_COLLECTION = "tig_push_devices";

	private static final String SERVICE_JID = "service_jid";
	private static final String SERVICE_JID_ID = "service_jid_id";
	private static final String USER_JID = "user_jid";
	private static final String USER_JID_ID = "user_jid_id";
	private static final String NODE = "node";
	private static final String NODE_ID = "node_id";
	private static final String PROVIDER = "provider";
	private static final String DEVICE_ID = "device_id";
	private static final String DEVICE_ID_ID = "device_id_id";


	private MongoDatabase db;
	private MongoCollection<Document> pushDevicesCollection;

	private static byte[] calculateHash(BareJID user) throws TigaseDBException {
		return calculateHash(user.toString().toLowerCase());
	}

	private static byte[] calculateHash(String user) throws TigaseDBException {
		try {
			MessageDigest md = MessageDigest.getInstance(HASH_ALG);
			return md.digest(user.getBytes(UTF8));
		} catch (NoSuchAlgorithmException ex) {
			throw new TigaseDBException("Should not happen!!", ex);
		}
	}

	@Override
	public IPushSettings registerDevice(BareJID serviceJid, BareJID userJid, String provider, String deviceId)
			throws RepositoryException {
		try {
			String node = calculateNode(serviceJid, userJid);
			byte[] serviceJidId = calculateHash(serviceJid);
			byte[] nodeId = calculateHash(node);
			Document doc = new Document(SERVICE_JID, serviceJid.toString()).append(SERVICE_JID_ID, serviceJidId)
					.append(USER_JID, userJid.toString())
					.append(USER_JID_ID, calculateHash(userJid))
					.append(NODE, node)
					.append(NODE_ID, nodeId)
					.append(PROVIDER, provider)
					.append(DEVICE_ID, deviceId)
					.append(DEVICE_ID_ID, calculateHash(deviceId));
			pushDevicesCollection.insertOne(doc);

			return getNodeSettings(serviceJid, node, serviceJidId, nodeId);
		} catch (Exception e) {
			throw new RepositoryException("Could not register device", e);
		}
	}

	@Override
	public IPushSettings unregisterDevice(BareJID serviceJid, BareJID userJid, String provider, String deviceId)
			throws RepositoryException, ComponentException {
		try {
			String node = calculateNode(serviceJid, userJid);
			byte[] serviceJidId = calculateHash(serviceJid);
			byte[] nodeId = calculateHash(node);
			IPushSettings pushSettings = getNodeSettings(serviceJid, node, serviceJidId, nodeId);
			if (pushSettings != null) {
				IPushSettings.IDevice device = new Device(provider, deviceId);
				pushSettings = pushSettings.removeDevice(device);
			}
			if (pushSettings == null) {
				throw new ComponentException(Authorization.ITEM_NOT_FOUND, "Device is not registered");
			}
			Document doc = new Document(SERVICE_JID_ID, serviceJidId).append(USER_JID_ID, calculateHash(userJid))
					.append(NODE_ID, nodeId)
					.append(PROVIDER, provider)
					.append(DEVICE_ID_ID, calculateHash(deviceId));
			pushDevicesCollection.deleteMany(doc);

			return pushSettings;
		} catch (ComponentException e) {
			throw e;
		} catch (Exception e) {
			throw new RepositoryException("Could not unregister device", e);
		}
	}

	@Override
	public IPushSettings getNodeSettings(BareJID serviceJid, String node) throws RepositoryException {
		try {
			byte[] serviceJidId = calculateHash(serviceJid);
			byte[] nodeId = calculateHash(node);
			return getNodeSettings(serviceJid, node, serviceJidId, nodeId);
		} catch (Exception e) {
			throw new RepositoryException("Could not retrieve setting by service jid and node", e);
		}
	}

	@Override
	public Stream<IPushSettings> getNodeSettings(String provider, String deviceId) throws RepositoryException {
		try {
			List<IPushSettings> settings = new ArrayList<>();

			pushDevicesCollection.find(
					Filters.and(Filters.eq(PROVIDER, provider), Filters.eq(DEVICE_ID_ID, calculateHash(deviceId))))
					.projection(Projections.include(SERVICE_JID, NODE, USER_JID, PROVIDER, DEVICE_ID)).forEach(
					(Block<? super Document>) doc -> {
						BareJID serviceJid = BareJID.bareJIDInstanceNS(doc.getString(SERVICE_JID));
						BareJID ownerJid = BareJID.bareJIDInstanceNS(doc.getString(USER_JID));
						String node = doc.getString(NODE);

						IPushSettings entry = settings.stream()
								.filter(it -> it.getServiceJid().equals(serviceJid) &&
										it.getOwenerJid().equals(ownerJid) && it.getNode().equals(node))
								.findFirst()
								.orElseGet(() -> new PushSettings(serviceJid, node, ownerJid, Collections.emptyList()));

						settings.remove(entry);
						settings.add(entry.addDevice(new Device(doc.getString(PROVIDER), doc.getString(DEVICE_ID))));
					});

			return settings.stream();
		} catch (Exception e) {
			throw new RepositoryException("Could not retrieve setting by service jid and node", e);
		}
	}

	@Override
	public void setDataSource(MongoDataSource dataSource) {
		db = dataSource.getDatabase();

		if (!collectionExists(db, PUSH_DEVICES_COLLECTION)) {
			db.createCollection(PUSH_DEVICES_COLLECTION);
		}

		pushDevicesCollection = db.getCollection(PUSH_DEVICES_COLLECTION);

		pushDevicesCollection.createIndex(Indexes.ascending(SERVICE_JID_ID, USER_JID_ID, PROVIDER, DEVICE_ID_ID),
										  new IndexOptions().unique(true));
		pushDevicesCollection.createIndex(Indexes.ascending(SERVICE_JID_ID, NODE_ID));
		pushDevicesCollection.createIndex(Indexes.ascending(PROVIDER, DEVICE_ID_ID));
	}

	private IPushSettings getNodeSettings(BareJID serviceJid, String node, byte[] serviceJidId, byte[] nodeId) {
		BareJID userJid = null;
		List<IPushSettings.IDevice> devices = new ArrayList<>();
		for (Document doc : pushDevicesCollection.find(Filters.and(Filters.eq(SERVICE_JID_ID, serviceJidId), Filters.eq(NODE_ID, nodeId))).projection(
				Projections.include(USER_JID, PROVIDER, DEVICE_ID))) {
			userJid = BareJID.bareJIDInstanceNS(doc.getString(USER_JID));
			devices.add(new Device(doc.getString(PROVIDER), doc.getString(DEVICE_ID)));
		}

		if (userJid == null) {
			return null;
		}

		return new PushSettings(serviceJid, node, userJid, devices);
	}
}
