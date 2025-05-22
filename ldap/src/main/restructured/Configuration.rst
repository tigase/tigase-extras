Configuration
=============

Tigase Server Extras - LDAP Server contains a few options that can be set to configure it to better fit your use cases.

To enable LDAP server built-in Tigase XMPP Server you need to add following block to etc/config.tdsl file:

.. code:: dsl

   ldap () {
   }

That is all, that is required to enable LDAP server listening on ports ``10389`` `(LDAP)` and ``10636`` `(LDAPS)`.

Disabling authentication
------------------------

To be able to connect to LDAP Server without providing username and password, you need to enable that by setting ``anonymousAccess`` to ``true`` in ``ldap`` section of a configuration file:

.. code:: dsl

   ldap () {
       'anonymousAccess' = true
   }

Allowing users to query information about other users
-----------------------------------------------------

By default, LDAP server does not allow non-administrators to query data of other users, ie. fetching other user's attributes or checking membership of other users. If you would like to allow any user to query information about others, you need to set ``anyoneCanQuery`` to ``true`` in ``ldap`` section of a configuration file:

.. code:: dsl

   ldap () {
       'anyoneCanQuery' = true
   }

Changing ports on which server listens
--------------------------------------

It is possible also to change ports on which LDAP server should expect connections. If you would like to disable accepting connections on port ``10636`` and enable LDAPS connections on port ``1636``, your config file would need to contain following section:

.. code:: dsl

    ldap () {
        connections {
            '10636' (active: false) {
            }
            '1636' () {
                'socket' = 'ssl'
            }
        }
    }

Changing name of ``Administrators`` group
-----------------------------------------

It is possible to rename ``Administrators`` group to which all domain administrators are added. To rename it to ``Admins`` you would need to assign a new name of the group to ``adminsGroupName`` property in ``ldap`` block:

.. code:: dsl

    ldap () {
        adminsGroupName = 'Admins'
    }

Changing name of ``Users`` group
-----------------------------------------

It is possible to rename ``Users`` group to which all domain administrators are added. To rename it to ``People`` you would need to assign a new name of the group to ``usersGroupName`` property in ``ldap`` block:

.. code:: dsl

    ldap () {
        usersGroupName = 'People'
    }