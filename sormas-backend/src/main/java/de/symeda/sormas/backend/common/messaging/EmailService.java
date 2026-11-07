/*******************************************************************************
 * SORMAS® - Surveillance Outbreak Response Management & Analysis System
 * Copyright © 2016-2018 Helmholtz-Zentrum für Infektionsforschung GmbH (HZI)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 *******************************************************************************/
package de.symeda.sormas.backend.common.messaging;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.util.Map;

import jakarta.activation.DataHandler;
import jakarta.activation.FileDataSource;
import jakarta.annotation.Resource;
import jakarta.ejb.Asynchronous;
import jakarta.ejb.EJB;
import jakarta.ejb.LocalBean;
import jakarta.ejb.Stateless;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.Multipart;
import jakarta.mail.Part;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeBodyPart;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;

import de.symeda.sormas.api.systemconfiguration.SystemConfigurationValueFacade;
import de.symeda.sormas.backend.systemconfiguration.SystemConfigurationValueEjb;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Stateless(name = "EmailService")
@LocalBean
public class EmailService {

	private static final String EMAIL_SENDER_ADDRESS = "EMAIL_SENDER_ADDRESS";
	private static final String EMAIL_SENDER_NAME = "EMAIL_SENDER_NAME";

	private final Logger logger = LoggerFactory.getLogger(getClass());

	@Resource(name = "mail/MailSession")
	private Session mailSession;

	@EJB
	private SystemConfigurationValueFacade systemConfigurationValueEjb;

	@Asynchronous
	public void sendEmailAsync(String recipient, String subject, String content) throws MessagingException {

		Thread.currentThread().setContextClassLoader(getClass().getClassLoader());

		sendEmailAsync(recipient, subject, content, "text/plain; charset=utf-8");
	}

	public void sendEmail(String recipient, String subject, String content, Map<File, String> attachments) throws MessagingException {

		Thread.currentThread().setContextClassLoader(getClass().getClassLoader());

		Multipart multipart = new MimeMultipart();
		MimeBodyPart messageBodyPart = new MimeBodyPart();
		messageBodyPart.setContent(content, "text/plain; charset=utf-8");

		multipart.addBodyPart(messageBodyPart);

		for (Map.Entry<File, String> attachment : attachments.entrySet()) {
			File file = attachment.getKey();
			String fileName = attachment.getValue();

			MimeBodyPart attachPart = new MimeBodyPart();
			attachPart.setDataHandler(new DataHandler(new FileDataSource(file)));
			attachPart.setFileName(fileName);
			attachPart.setDisposition(Part.ATTACHMENT);

			multipart.addBodyPart(attachPart);
		}

		sendEmailAsync(recipient, subject, multipart, null);
	}

	private void sendEmailAsync(String recipient, String subject, Object content, String contentType) throws MessagingException {
		MimeMessage message = new MimeMessage(mailSession);
		String senderAddress = systemConfigurationValueEjb.getValue(EMAIL_SENDER_ADDRESS);
		String senderName = systemConfigurationValueEjb.getValue(EMAIL_SENDER_NAME);

		try {
			InternetAddress fromAddress = new InternetAddress(senderAddress, senderName);
			message.setFrom(fromAddress);
		} catch (UnsupportedEncodingException e) {
			logger.error(e.getMessage());
		}

		message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(recipient, false));
		message.setSubject(subject, "UTF-8");
		message.setContent(content, contentType);

		Transport.send(message);
		logger.info("Mail sent to {}.", recipient);
	}
}
