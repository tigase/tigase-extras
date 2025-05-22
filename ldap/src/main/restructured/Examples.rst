Examples
========

Here are example queries that you could use for integration with Tigase XMPP Server for authentication using LDAP.


Searching for user by local part of the JID or the whole JID in domain ``example.com``
`````````````````

Search base: ``ou=Users,dc=example.com``

Search filter: ``(&(objectClass=posixAccount)(|(uid={0})(mail={0})))``

In the filter you need to replace all occurrences of ``{0}`` with a JID or local part of the user that you want to discover.

Searching for groups to which user belongs in domain ``example.com`` using user DN
`````````````````

Search base: ``ou=Groups,dc=tigase,dc=org``

Search filter: ``(&(member={0})(objectClass=posixGroup))``

In the filter you need to replace all occurrences of ``{0}`` with a DN of a user.

Searching for groups to which user belongs in domain ``example.com`` using user uid/local part of the JID
`````````````````

Search base: ``ou=Groups,dc=tigase,dc=org``

Search filter: ``(&(memberUid={0})(objectClass=posixGroup))``

In the filter you need to replace all occurrences of ``{0}`` with uid of a user (local part of the JID).

