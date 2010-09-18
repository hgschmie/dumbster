package com.dumbster.smtp.action;

import java.util.List;

import com.dumbster.smtp.MailMessage;
import com.dumbster.smtp.SmtpResponse;
import com.dumbster.smtp.SmtpState;

public class BlankLine implements Action {

	@Override
	public boolean isStateless() {
		return false;
	}

	@Override
	public String toString() {
		return "Blank line";
	}

	@Override
	public SmtpResponse response(SmtpState smtpState, List<MailMessage> messages, MailMessage currentMessage) {
		if (SmtpState.DATA_HDR == smtpState) {
			return new SmtpResponse(-1, "", SmtpState.DATA_BODY);
		} else if (SmtpState.DATA_BODY == smtpState) {
			return new SmtpResponse(-1, "", smtpState);
		} else {
			return new SmtpResponse(503,
					"Bad sequence of commands: " + this, smtpState);
		}		
	}

}
