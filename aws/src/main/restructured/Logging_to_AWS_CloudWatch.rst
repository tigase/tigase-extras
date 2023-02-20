Logging to AWS CloudWatch
=========================

To log to AWS CloudWatch there are two modifications required:

1) adding ``org.slf4j.bridge.SLF4JBridgeHandler`` to ``logging`` bean:

.. code:: dsl

   logging () {
       rootHandlers = [ 'java.util.logging.ConsoleHandler', 'java.util.logging.FileHandler', 'org.slf4j.bridge.SLF4JBridgeHandler' ]
   }

2) Add appender configuration to logback.xml configuration file:

.. code:: xml

   <appender name="ASYNC_AWS_LOGS" class="ca.pjer.logback.AwsLogsAppender">

       <!-- Nice layout pattern -->
       <layout>
           <pattern>%d{yyyy-MM-dd HH:mm:ss.SSS} %-5level [%20(%thread)] %logger{5}.%method\(\): %msg %n</pattern>
       </layout>

       <!-- Hardcoded Log Group Name -->
       <logGroupName>/kangaroo-logs/${VHOST}</logGroupName>

       <!-- Log Stream Name UUID Prefix -->
       <logStreamUuidPrefix>${EXTERNAL_IP}/</logStreamUuidPrefix>

       <!-- Hardcoded AWS region -->
       <!-- So even when running inside an AWS instance in us-west-1, logs will go to us-west-2 -->
       <logRegion>us-west-2</logRegion>

       <!-- Maximum number of events in each batch (50 is the default) -->
       <!-- will flush when the event queue has 50 elements, even if still in quiet time (see maxFlushTimeMillis) -->
       <maxBatchLogEvents>50</maxBatchLogEvents>

       <!-- Maximum quiet time in millisecond (0 is the default) -->
       <!-- will flush when met, even if the batch size is not met (see maxBatchLogEvents) -->
       <maxFlushTimeMillis>30000</maxFlushTimeMillis>

       <!-- Maximum block time in millisecond (5000 is the default) -->
       <!-- when > 0: this is the maximum time the logging thread will wait for the logger, -->
       <!-- when == 0: the logging thread will never wait for the logger, discarding events while the queue is full -->
       <maxBlockTimeMillis>5000</maxBlockTimeMillis>

       <!-- Retention value for log groups, 0 for infinite see -->
       <!-- https://docs.aws.amazon.com/AmazonCloudWatchLogs/latest/APIReference/API_PutRetentionPolicy.html for other -->
       <!-- possible values -->

       <retentionTimeDays>60</retentionTimeDays>
   </appender>

and it to root logger configuration:

.. code:: xml

   <root level="WARN">
       <appender-ref ref="ASYNC_AWS_LOGS"/>