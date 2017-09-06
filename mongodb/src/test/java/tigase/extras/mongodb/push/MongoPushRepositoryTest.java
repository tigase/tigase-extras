/*
 * MongoPushRepositoryTest.java
 *
 * Tigase Server Extras MongoDB
 * Copyright (C) 2004-2017, "Tigase, Inc." <office@tigase.com>
 *
 */
package tigase.extras.mongodb.push;

import org.junit.Assume;
import org.junit.ClassRule;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import tigase.mongodb.MongoDataSource;
import tigase.push.repositories.AbstractIPushRepositoryTest;

/**
 * Created by andrzej on 16.05.2017.
 */
public class MongoPushRepositoryTest
		extends AbstractIPushRepositoryTest<MongoDataSource> {

	@ClassRule
	public static TestRule rule = new TestRule() {
		@Override
		public Statement apply(Statement stmnt, Description d) {
			if (uri == null) {
				return new Statement() {
					@Override
					public void evaluate() throws Throwable {
						Assume.assumeTrue("Ignored due to not passed DB URI!", false);
					}
				};
			}
			return stmnt;
		}
	};

}
