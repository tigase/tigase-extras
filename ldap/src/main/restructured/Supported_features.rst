Supported features
==================

As this is just a basic implementation of LDAP server features and it doesn't support all LDAP feature but only those required for other services to use Tigase XMPP Server for authentication.

This server doesn't contain a dynamic schema that user can configure but has a fixed schema that is predefined and cannot be modified by the end user.

Following features are supported:

* Anonymous login
* Authenticated login (simple)
* Plain TCP connections
* SSL connections
* Searching users by cn,uid,mail,xmpp attributes
* Searching of user groups
