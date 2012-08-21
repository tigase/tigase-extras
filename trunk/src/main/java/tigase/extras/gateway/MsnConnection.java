/*
 *   Tigase Jabber/XMPP Server
 *  Copyright (C) 2004-2012 "Artur Hefczyc" <artur.hefczyc@tigase.org>
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
 *
 * $Rev$
 * Last modified by $Author$
 * $Date$
 */

package tigase.extras.gateway;

//~--- non-JDK imports --------------------------------------------------------

import net.sf.jml.Email;
import net.sf.jml.MsnContact;
import net.sf.jml.MsnContactList;
import net.sf.jml.MsnGroup;
import net.sf.jml.MsnList;
import net.sf.jml.MsnMessenger;
import net.sf.jml.MsnOwner;
import net.sf.jml.MsnSwitchboard;
import net.sf.jml.MsnUserStatus;
import net.sf.jml.event.MsnContactListListener;
import net.sf.jml.event.MsnMessageListener;
import net.sf.jml.event.MsnMessengerListener;
import net.sf.jml.impl.MsnMessengerFactory;
import net.sf.jml.message.MsnControlMessage;
import net.sf.jml.message.MsnDatacastMessage;
import net.sf.jml.message.MsnInstantMessage;
import net.sf.jml.message.MsnSystemMessage;
import net.sf.jml.message.MsnUnknownMessage;
import net.sf.jml.message.p2p.MsnP2PMessage;

import tigase.server.Packet;
import tigase.server.gateways.GatewayConnection;
import tigase.server.gateways.GatewayException;
import tigase.server.gateways.GatewayListener;
import tigase.server.gateways.RosterItem;
import tigase.server.gateways.UserStatus;

import tigase.util.TigaseStringprepException;

import tigase.xml.Element;
import tigase.xml.XMLUtils;

import tigase.xmpp.JID;

//~--- JDK imports ------------------------------------------------------------

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

//~--- classes ----------------------------------------------------------------

/**
 * Describe class MsnConnection here.
 *
 *
 * Created: Mon Nov 12 11:42:01 2007
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public class MsnConnection
				implements MsnContactListListener, GatewayConnection, MsnMessengerListener,
									 MsnMessageListener {

	/**
	 * Private logger for class instancess.
	 */
	private static Logger log = Logger.getLogger(MsnConnection.class.getName());

	//~--- fields ---------------------------------------------------------------

	private JID active_jid = null;

//private String gatewayDomain = null;
	private GatewayListener listener = null;
	private MsnMessenger messenger = null;
	private String password = null;
	private String username = null;
	private Set<JID> xmpp_jids = new HashSet<JID>();

	//~--- methods --------------------------------------------------------------

	/**
	 * Method description
	 *
	 *
	 * @param id
	 * @param nick
	 *
	 * @throws GatewayException
	 */
	@Override
	public void addBuddy(String id, String nick) throws GatewayException {
		messenger.addFriend(Email.parseStr(id), nick);
		messenger.unblockFriend(Email.parseStr(id));
		log.finest(active_jid + " addBuddy completed: " + id);
	}

//public void setGatewayDomain(String domain) {
//  this.gatewayDomain = domain;
//  log.finest("gatewayDomain set: " + domain);
//}

	/**
	 * Method description
	 *
	 *
	 * @param jid
	 */
	@Override
	public void addJid(JID jid) {
		xmpp_jids.add(jid);
		active_jid = jid;
		log.finest("JID added: " + jid);
	}

	/**
	 * Describe <code>contactAddCompleted</code> method here.
	 *
	 * @param msnMessenger a <code>MsnMessenger</code> value
	 * @param msnContact a <code>MsnContact</code> value
	 */
	@Override
	public void contactAddCompleted(final MsnMessenger msnMessenger,
																	final MsnContact msnContact) {
		RosterItem item = new RosterItem(msnContact.getEmail().getEmailAddress());

		item.setName(msnContact.getFriendlyName());
		item.setSubscription("both");

		if (msnContact.getStatus() == MsnUserStatus.OFFLINE) {
			item.setStatus(new UserStatus("unavailable", null));
		} else {
			item.setStatus(new UserStatus(null,
																		msnContact.getStatus().getDisplayStatus().toLowerCase()));
		}

		MsnGroup[] groups = msnContact.getBelongGroups();

		if ((groups != null) && (groups.length > 0)) {
			List<String> grps = new ArrayList<String>();

			for (MsnGroup group : groups) {
				grps.add(group.getGroupName());
			}

			item.setGroups(grps);
		}

		listener.updateStatus(this, item);
		log.finest(active_jid + " contactAddCompleted completed.");
	}

	/**
	 * Describe <code>contactAddedMe</code> method here.
	 *
	 * @param msnMessenger a <code>MsnMessenger</code> value
	 * @param msnContact a <code>MsnContact</code> value
	 */
	@Override
	public void contactAddedMe(final MsnMessenger msnMessenger,
														 final MsnContact msnContact) {
		Element presence = null;

		try {
			JID to = active_jid;
			String from = listener.formatJID(msnContact.getEmail().getEmailAddress());

			presence = new Element("presence",
														 new String[] { "from", "to", "type" },
														 new String[] { from, to.toString(), "subscribe" });

			Packet packet = Packet.packetInstance(presence);

			log.finest("Received subscription presence: " + packet.toString());
			listener.packetReceived(packet);
			presence = new Element("presence",
														 new String[] { "from", "to", "type" },
														 new String[] { from, to.toString(), "subscribed" });
			packet = Packet.packetInstance(presence);
			log.finest("Received subscription presence: " + packet.toString());
			listener.packetReceived(packet);
			log.finest(active_jid + " contactAddedMe completed.");
		} catch (TigaseStringprepException ex) {
			log.info("Packet addressing problem, stringprep failed for packet: " + presence);
		}
	}

	/**
	 * Describe <code>contactListInitCompleted</code> method here.
	 *
	 * @param msnMessenger a <code>MsnMessenger</code> value
	 */
	@Override
	public void contactListInitCompleted(final MsnMessenger msnMessenger) {
		log.finest(active_jid + " contactListInitCompleted completed.");
		listener.userRoster(this);
	}

	// Implementation of net.sf.jml.event.MsnContactListListener

	/**
	 * Describe <code>contactListSyncCompleted</code> method here.
	 *
	 * @param msnMessenger a <code>MsnMessenger</code> value
	 */
	@Override
	public void contactListSyncCompleted(final MsnMessenger msnMessenger) {
		log.finest(active_jid + " contactListSyncCompleted completed.");
		listener.userRoster(this);
	}

	/**
	 * Describe <code>contactRemoveCompleted</code> method here.
	 *
	 * @param msnMessenger a <code>MsnMessenger</code> value
	 * @param msnContact a <code>MsnContact</code> value
	 */
	@Override
	public void contactRemoveCompleted(final MsnMessenger msnMessenger,
																		 final MsnContact msnContact) {
		log.finest(active_jid + " contactRemoveCompleted completed: "
							 + msnContact.getEmail().getEmailAddress());
	}

	/**
	 * Describe <code>contactRemovedMe</code> method here.
	 *
	 * @param msnMessenger a <code>MsnMessenger</code> value
	 * @param msnContact a <code>MsnContact</code> value
	 */
	@Override
	public void contactRemovedMe(final MsnMessenger msnMessenger,
															 final MsnContact msnContact) {
		Element presence = null;

		try {
			JID to = active_jid;
			String from = listener.formatJID(msnContact.getEmail().getEmailAddress());

			presence = new Element("presence",
														 new String[] { "from", "to", "type" },
														 new String[] { from, to.toString(), "unsubscribe" });

			Packet packet = Packet.packetInstance(presence);

			log.finest("Received subscription presence: " + packet.toString());
			listener.packetReceived(packet);
			presence = new Element("presence",
														 new String[] { "from", "to", "type" },
														 new String[] { from, to.toString(), "unsubscribed" });
			packet = Packet.packetInstance(presence);
			log.finest("Received subscription presence: " + packet.toString());
			listener.packetReceived(packet);
			log.finest(active_jid + " contactRemovedMe completed.");
		} catch (TigaseStringprepException ex) {
			log.info("Packet addressing problem, stringprep failed for packet: " + presence);
		}
	}

	/**
	 * Describe <code>contactStatusChanged</code> method here.
	 *
	 * @param msnMessenger a <code>MsnMessenger</code> value
	 * @param msnContact a <code>MsnContact</code> value
	 */
	@Override
	public void contactStatusChanged(final MsnMessenger msnMessenger,
																	 final MsnContact msnContact) {
		log.finest(active_jid + " contactStatusChanged completed.");

		if (msnContact.isInList(MsnList.AL)) {
			RosterItem item = new RosterItem(msnContact.getEmail().getEmailAddress());

			item.setName(msnContact.getFriendlyName());
			item.setSubscription("both");

			if (msnContact.getStatus() == MsnUserStatus.OFFLINE) {
				item.setStatus(new UserStatus("unavailable", null));
			} else {
				item.setStatus(new UserStatus(null,
																			msnContact.getStatus().getDisplayStatus().toLowerCase()));
			}

			MsnGroup[] groups = msnContact.getBelongGroups();

			if ((groups != null) && (groups.length > 0)) {
				List<String> grps = new ArrayList<String>();

				for (MsnGroup group : groups) {
					grps.add(group.getGroupName());
				}

				item.setGroups(grps);
			}

			listener.updateStatus(this, item);
		}

		if (msnContact.isInList(MsnList.AL)) {
			log.fine("Contact " + msnContact.getEmail().getEmailAddress() + " is on AL list.");
		}

		if (msnContact.isInList(MsnList.BL)) {
			log.fine("Contact " + msnContact.getEmail().getEmailAddress() + " is on BL list.");
		}

		if (msnContact.isInList(MsnList.FL)) {
			log.fine("Contact " + msnContact.getEmail().getEmailAddress() + " is on FL list.");
		}

		if (msnContact.isInList(MsnList.PL)) {
			log.fine("Contact " + msnContact.getEmail().getEmailAddress() + " is on PL list.");
		}

		if (msnContact.isInList(MsnList.RL)) {
			log.fine("Contact " + msnContact.getEmail().getEmailAddress() + " is on RL list.");
		}
	}

	/**
	 * Describe <code>controlMessageReceived</code> method here.
	 *
	 * @param msnSwitchboard a <code>MsnSwitchboard</code> value
	 * @param msnControlMessage a <code>MsnControlMessage</code> value
	 * @param msnContact a <code>MsnContact</code> value
	 */
	@Override
	public void controlMessageReceived(final MsnSwitchboard msnSwitchboard,
																		 final MsnControlMessage msnControlMessage,
																		 final MsnContact msnContact) {
		Element message = null;

		try {
			JID to = active_jid;
			String from = listener.formatJID(msnContact.getEmail().getEmailAddress());

			message = new Element("message",
														new String[] { "from", "to", "type" },
														new String[] { from, to.toString(), "chat" });

			Element composing = new Element("composing");

			composing.setXMLNS("http://jabber.org/protocol/chatstates");
			message.addChild(composing);

			Packet packet = Packet.packetInstance(message);

			log.finest("Received control message: " + packet.toString());
			listener.packetReceived(packet);
		} catch (TigaseStringprepException ex) {
			log.info("Packet addressing problem, stringprep failed for packet: " + message);
		}
	}

	/**
	 * Describe <code>datacastMessageReceived</code> method here.
	 *
	 * @param msnSwitchboard a <code>MsnSwitchboard</code> value
	 * @param msnDatacastMessage a <code>MsnDatacastMessage</code> value
	 * @param msnContact a <code>MsnContact</code> value
	 */
	@Override
	public void datacastMessageReceived(final MsnSwitchboard msnSwitchboard,
					final MsnDatacastMessage msnDatacastMessage, final MsnContact msnContact) {

		// Ignore for now, I don't know yet how to handle it.
	}

	/**
	 * Describe <code>exceptionCaught</code> method here.
	 *
	 * @param msnMessenger a <code>MsnMessenger</code> value
	 * @param throwable a <code>Throwable</code> value
	 */
	@Override
	public void exceptionCaught(final MsnMessenger msnMessenger,
															final Throwable throwable) {
		listener.gatewayException(this, throwable);
	}

	//~--- get methods ----------------------------------------------------------

	/**
	 * Method description
	 *
	 *
	 * @return
	 */
	@Override
	public JID[] getAllJids() {
		return xmpp_jids.toArray(new JID[xmpp_jids.size()]);
	}

	/**
	 * Method description
	 *
	 *
	 * @return
	 */
	@Override
	public String getName() {
		return "MSN Gateway";
	}

	/**
	 * Method description
	 *
	 *
	 * @return
	 */
	@Override
	public String getPromptMessage() {
		return "Please enter the Windows Live Messenger address of the person "
					 + "you would like to contact.";
	}

	/**
	 * Method description
	 *
	 *
	 * @return
	 */
	@Override
	public List<RosterItem> getRoster() {
		return getRoster(messenger, null);
	}

	/**
	 * Method description
	 *
	 *
	 * @return
	 */
	@Override
	public String getType() {
		return "msn";
	}

	//~--- methods --------------------------------------------------------------

	/**
	 * Describe <code>groupAddCompleted</code> method here.
	 *
	 * @param msnMessenger a <code>MsnMessenger</code> value
	 * @param msnGroup a <code>MsnGroup</code> value
	 */
	@Override
	public void groupAddCompleted(final MsnMessenger msnMessenger,
																final MsnGroup msnGroup) {
		log.finest(active_jid + " groupAddCompleted completed.");
	}

	/**
	 * Describe <code>groupRemoveCompleted</code> method here.
	 *
	 * @param msnMessenger a <code>MsnMessenger</code> value
	 * @param msnGroup a <code>MsnGroup</code> value
	 */
	@Override
	public void groupRemoveCompleted(final MsnMessenger msnMessenger,
																	 final MsnGroup msnGroup) {
		log.finest(active_jid + " groupRemoveCompleted completed.");
	}

	/**
	 * Method description
	 *
	 *
	 * @throws GatewayException
	 */
	@Override
	public void init() throws GatewayException {
		messenger = MsnMessengerFactory.createMsnMessenger(username, password);
		messenger.addMessageListener(this);
		messenger.addMessengerListener(this);
		messenger.addContactListListener(this);

		// messenger.setSupportedProtocol(new MsnProtocol[] {MsnProtocol.MSNP11});
		// MsnOwner owner = messenger.getOwner();
		// owner.setNotifyMeWhenSomeoneAddedMe(true);
		// owner.setOnlyNotifyAllowList(true);
		// owner.setInitStatus(MsnUserStatus.ONLINE);
	}

	// Implementation of net.sf.jml.event.MsnMessageListener

	/**
	 * Describe <code>instantMessageReceived</code> method here.
	 *
	 * @param msnSwitchboard a <code>MsnSwitchboard</code> value
	 * @param msnInstantMessage a <code>MsnInstantMessage</code> value
	 * @param msnContact a <code>MsnContact</code> value
	 */
	@Override
	public void instantMessageReceived(final MsnSwitchboard msnSwitchboard,
																		 final MsnInstantMessage msnInstantMessage,
																		 final MsnContact msnContact) {
		Element message = null;

		try {
			JID to = active_jid;
			String from = listener.formatJID(msnContact.getEmail().getEmailAddress());
			String content = XMLUtils.escape(msnInstantMessage.getContent());

			message = new Element("message",
														new String[] { "from", "to", "type" },
														new String[] { from, to.toString(), "chat" });

			Element body = new Element("body", content);

			message.addChild(body);

			Packet packet = Packet.packetInstance(message);

			log.finest("Received instant message: " + packet.toString());
			listener.packetReceived(packet);
		} catch (TigaseStringprepException ex) {
			log.info("Packet addressing problem, stringprep failed for packet: " + message);
		}
	}

	/**
	 * Method description
	 *
	 */
	@Override
	public void login() {
		messenger.login();
	}

	/**
	 * Describe <code>loginCompleted</code> method here.
	 *
	 * @param msnMessenger a <code>MsnMessenger</code> value
	 */
	@Override
	public void loginCompleted(final MsnMessenger msnMessenger) {
		listener.loginCompleted(this);
		log.finest(active_jid + " logout completed.");

		MsnOwner owner = msnMessenger.getOwner();

		log.fine("Owner initstatus: " + owner.getInitStatus().getDisplayStatus());
		log.fine("Owner isNotifyMeWhenSomeoneAddedMe: "
						 + owner.isNotifyMeWhenSomeoneAddedMe());
		log.fine("Owner isOnlyNotifyAllowList: " + owner.isOnlyNotifyAllowList());
		owner.setNotifyMeWhenSomeoneAddedMe(true);

		// owner.setOnlyNotifyAllowList(false);
		owner.setInitStatus(MsnUserStatus.ONLINE);
		owner.setStatus(MsnUserStatus.ONLINE);
	}

	/**
	 * Method description
	 *
	 */
	@Override
	public void logout() {
		messenger.logout();
	}

//Implementation of net.sf.jml.event.MsnMessengerListener

	/**
	 * Describe <code>logout</code> method here.
	 *
	 * @param msnMessenger a <code>MsnMessenger</code> value
	 */
	@Override
	public void logout(final MsnMessenger msnMessenger) {
		listener.logout(this);
		log.finest(active_jid + " logout called.");
	}

	/**
	 * Describe <code>ownerStatusChanged</code> method here.
	 *
	 * @param msnMessenger a <code>MsnMessenger</code> value
	 */
	@Override
	public void ownerStatusChanged(final MsnMessenger msnMessenger) {
		log.finest(active_jid + " ownerStatusChanged completed.");
	}

	/**
	 * Method description
	 *
	 *
	 * @param switchboard
	 * @param message
	 * @param contact
	 */
	@Override
	public void p2pMessageReceived(MsnSwitchboard switchboard, MsnP2PMessage message,
																 MsnContact contact) {

		// Ignore for now.
	}

	/**
	 * Method description
	 *
	 *
	 * @param id
	 *
	 * @throws GatewayException
	 */
	@Override
	public void removeBuddy(String id) throws GatewayException {
		messenger.removeFriend(Email.parseStr(id), false);
		log.finest(active_jid + " removeBuddy completed: " + id);
	}

	/**
	 * Method description
	 *
	 *
	 * @param jid
	 */
	@Override
	public void removeJid(JID jid) {
		xmpp_jids.remove(jid);
		log.finest("JID removed: " + jid);
	}

	/**
	 * Method description
	 *
	 *
	 * @param packet
	 */
	@Override
	public void sendMessage(Packet packet) {
		String address = listener.decodeLegacyName(packet.getElemTo());

		active_jid = packet.getStanzaFrom();

		if (packet.getElemName().equals("message")) {
			log.finest("Sending message: " + packet.toString());

			String body = XMLUtils.unescape(packet.getElemCData("/message/body"));

			messenger.sendText(Email.parseStr(address), body);
		} else {
			log.finest("Ignoring unknown packet: " + packet.toString());
		}
	}

	//~--- set methods ----------------------------------------------------------

	/**
	 * Method description
	 *
	 *
	 * @param listener
	 */
	@Override
	public void setGatewayListener(GatewayListener listener) {
		this.listener = listener;
	}

	// Implementation of tigase.server.gateways.GatewayConnection

	/**
	 * Method description
	 *
	 *
	 * @param username
	 * @param password
	 */
	@Override
	public void setLogin(String username, String password) {
		this.username = username;
		this.password = password;
		log.finest("Username, password set: (" + username + "," + password + ")");
	}

	//~--- methods --------------------------------------------------------------

	/**
	 * Describe <code>systemMessageReceived</code> method here.
	 *
	 * @param msnMessenger a <code>MsnMessenger</code> value
	 * @param msnSystemMessage a <code>MsnSystemMessage</code> value
	 */
	@Override
	public void systemMessageReceived(final MsnMessenger msnMessenger,
																		final MsnSystemMessage msnSystemMessage) {

		// Do nothing....
//  String to = active_jid;
//  String from = gatewayDomain;
//  String content = msnSystemMessage.getContent() == null ? ""
//    : XMLUtils.escape(msnSystemMessage.getContent());
//  Element message = new Element("message",
//    new String[] {"from", "to", "type"},
//    new String[] {from, to, "chat"});
//  Element body = new Element("body", content);
//  message.addChild(body);
//  Packet packet = new Packet(message);
//  log.finest("Received system message: " + packet.toString());
//  listener.packetReceived(packet);
	}

	/**
	 * Describe <code>unknownMessageReceived</code> method here.
	 *
	 * @param msnSwitchboard a <code>MsnSwitchboard</code> value
	 * @param msnUnknownMessage a <code>MsnUnknownMessage</code> value
	 * @param msnContact a <code>MsnContact</code> value
	 */
	@Override
	public void unknownMessageReceived(final MsnSwitchboard msnSwitchboard,
																		 final MsnUnknownMessage msnUnknownMessage,
																		 final MsnContact msnContact) {

		// Ignore for now, I don't know yet how to handle it.
	}

	//~--- get methods ----------------------------------------------------------

	private List<RosterItem> getRoster(final MsnMessenger msnMessenger,
																		 final MsnUserStatus presetStatus) {
		MsnContact[] list = msnMessenger.getContactList().getContacts();
		List<RosterItem> roster = new ArrayList<RosterItem>();

		if (list != null) {
			for (MsnContact contact : list) {
				if (contact.isInList(MsnList.AL)) {
					MsnGroup[] c_groups = contact.getBelongGroups();

					if ((c_groups != null) && (c_groups.length > 0)) {
						for (MsnGroup c_grp : c_groups) {
							log.fine("Contact " + contact.getEmail().getEmailAddress() + " group: "
											 + c_grp.getGroupName());
						}
					} else {
						log.fine("Contact " + contact.getEmail().getEmailAddress()
										 + " is not in any group, status: "
										 + contact.getStatus().getDisplayStatus());
					}

					MsnContactList c_list = contact.getContactList();
					RosterItem item = new RosterItem(contact.getEmail().getEmailAddress());

					item.setName(contact.getFriendlyName());
					item.setSubscription("both");

					MsnUserStatus status = (presetStatus != null)
																 ? presetStatus : contact.getStatus();

					if (status == MsnUserStatus.OFFLINE) {
						item.setStatus(new UserStatus("unavailable", null));
					} else {
						item.setStatus(new UserStatus(null, status.getDisplayStatus().toLowerCase()));
					}

					MsnGroup[] groups = contact.getBelongGroups();

					if ((groups != null) && (groups.length > 0)) {
						List<String> grps = new ArrayList<String>();

						for (MsnGroup group : groups) {
							grps.add(group.getGroupName());
						}

						item.setGroups(grps);
					}

					log.finest("Contact AL received: " + contact.getEmail().getEmailAddress()
										 + ", status: " + status.getDisplayStatus());
					roster.add(item);
				}

				if (contact.isInList(MsnList.AL)) {
					log.fine("Contact " + contact.getEmail().getEmailAddress() + " is on AL list.");
				}

				if (contact.isInList(MsnList.BL)) {
					log.fine("Contact " + contact.getEmail().getEmailAddress() + " is on BL list.");
				}

				if (contact.isInList(MsnList.FL)) {
					log.fine("Contact " + contact.getEmail().getEmailAddress() + " is on FL list.");
				}

				if (contact.isInList(MsnList.PL)) {
					log.fine("Contact " + contact.getEmail().getEmailAddress() + " is on PL list.");
				}

				if (contact.isInList(MsnList.RL)) {
					log.fine("Contact " + contact.getEmail().getEmailAddress() + " is on RL list.");
				}
			}
		}

		return roster;
	}
}


//~ Formatted in Sun Code Convention


//~ Formatted by Jindent --- http://www.jindent.com
