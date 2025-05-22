Schema
=======

Tigase XMPP Extras - LDAP Server contains a fixed schema.

Users
-----

Users for each domain are grouped in ``Users`` organization unit, ie. for domain ``example.com`` users are available in ``ou=Users,dc=example,dc=com``.

Attributes
``````````

Here is a list of attributes supported by LDAP server for queries and that can be returned then entry is found:

* ``cn`` - local part of the user JID
* ``uid`` - local part of the user JID
* ``mail`` - bare JID of the user
* ``xmpp`` - bare JID of the user
* ``objectClass`` - fixed value of ``posixAccount``
* ``accountStatus`` - :ref:`Account Status`
* ``memberOf`` - contains a list of group DNs to which user belongs to
* ``memberOfGid`` - contains a list of group names to which user belongs to

.. _Account Status:

Account Status
______________

Account status represents a status of the account in the system and may have one of the following values:

* ``active`` - if account is allowed to log in
* ``disabled`` - if account is disabled
* ``banned`` - if account is banned
* ``pending`` - when account was created but not yet activated/confirmed
* ``system`` - marks an account used by system that cannot be used to log in to the system
* ``spam`` - account was marked as a spammer

Limitations
```````````

It is not possible at the moment to list all users, but it is possible to query them. However, each query needs to contain an exact matches and contain one of the following attributes:

* ``cn`` - `contains localpart of the user JID`
* ``uid`` - `contains localpart of the user JID`
* ``mail`` - `contains bare JID of the user`
* ``xmpp`` - `contains bare JID of the user`

If one or more of the above attributes are going to be used in a query, all of their values have to be equal when the query is executed.

Groups
------

Groups for each domain are grouped in ``Groups`` organization unit, ie. for domain ``example.com`` users are available in ``ou=Groups,dc=example,dc=com``.

Currently LDAP server is aware of only two automatic groups named by default ``Administrators`` (contains only users with administrative permissions for this domain) and ``Users`` (contains all users).

Attributes
``````````

Currently group will return just two attributes:

* ``cn`` - containing a group name
* ``objectClass`` - fixed value of ``posixGroup``

Groups can be queried using following attributes:

* ``cn`` name of the group
* ``objectClass`` - fixed value of ``posixGroup``
* ``memberUid`` - contains a list of user uids that belong to this group
* ``member`` - contains a list of DNs of users that belong to this group