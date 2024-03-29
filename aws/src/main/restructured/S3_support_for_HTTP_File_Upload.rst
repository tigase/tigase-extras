S3 support for HTTP File Upload
===============================

By default HTTP File Upload component shipped with Tigase XMPP Server stores uploaded files locally in the directory structure. On AWS it may be better to store data using external service like S3 which are better suited for this task and are more resilient.

Enabling storage in S3
----------------------

To enable storage in S3, you need to add following lines to your configuration file:

.. code:: dsl

   upload () {
       store (class: tigase.extras.http.upload.S3Store, active: true, exportable: true) {
           bucket = 'bucket-name'
       }
   }

This will enable HTTP File Upload component and configure it to be used with S3 bucket named ``bucket-name`` in the same region as your EC2 instance on which Tigase XMPP Server is running.

.. warning::

   You would need to manually create this S3 bucket and allow your EC2 instance to access it (read and write). Alternatively, you could add ``autocreateBucket = true`` inside ``store`` block, which will enable Tigase XMPP Server to create this S3 bucket in the local AWS region.

If you wish to use S3 bucket from another AWS region, you can do that by adding setting ``region`` property in the ``store`` block to the id of the AWS region, ie. set to ``us-west-2`` to use ``US West (Oregon)`` region:

.. code:: dsl

   upload () {
       store (class: tigase.extras.http.upload.S3Store, active: true, exportable: true) {
           bucket = 'bucket-name'
           region = 'us-west-2'
       }
   }

If you wish to share the same S3 bucket between different installations of Tigase XMPP Server, you should configure ``bucketKeyPrefix`` property of ``store`` with different identifiers for each installation. That will allow you to easily filter data uploaded for each installation and will allow Tigase XMPP Server to provide you with correct storage usage for each installation.

.. code:: dsl

   upload () {
       store (class: tigase.extras.http.upload.S3Store, active: true, exportable: true) {
           bucket = 'bucket-name'
           bucketKeyPrefix = '45252AF'
       }
   }