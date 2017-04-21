package tigase.extras.mailer;

/**
 * Created by bmalkow on 21.04.2017.
 */
public class MailerException extends RuntimeException {

	public MailerException() {
	}

	public MailerException(String message) {
		super(message);
	}

	public MailerException(String message, Throwable cause) {
		super(message, cause);
	}

	public MailerException(Throwable cause) {
		super(cause);
	}

	public MailerException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}
}
