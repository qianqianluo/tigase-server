/*
 * Tigase XMPP Server - The instant messaging server
 * Copyright (C) 2004 Tigase, Inc. (office@tigase.com)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, version 3 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. Look for COPYING file in the top folder.
 * If not, see http://www.gnu.org/licenses/.
 */
package tigase.xmpp.mam;

import tigase.component.exceptions.ComponentException;
import tigase.kernel.beans.Bean;
import tigase.server.Command;
import tigase.server.DataForm;
import tigase.server.Packet;
import tigase.util.datetime.TimestampHelper;
import tigase.util.stringprep.TigaseStringprepException;
import tigase.xml.Element;
import tigase.xmpp.Authorization;
import tigase.xmpp.jid.JID;
import tigase.xmpp.rsm.RSM;

import java.text.ParseException;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Implementation of parser for XEP-0313: Message Archive Management
 * <br>
 * Created by andrzej on 19.07.2016.
 */
@Bean(name = "mamQueryParser", active = true)
public class MAMQueryParser<Query extends tigase.xmpp.mam.Query>
		implements QueryParser<Query> {

	protected static final String MAM_XMLNS = "urn:xmpp:mam:1";

	private final TimestampHelper timestampHelper = new TimestampHelper();

	private final Set<String> XMLNNS;

	public MAMQueryParser() {
		this(Stream.empty());
	}

	protected MAMQueryParser(Stream<String> additionalNamespaces) {
		XMLNNS = Stream.concat(additionalNamespaces, Stream.of(MAM_XMLNS)).collect(Collectors.toUnmodifiableSet());
	}

	@Override
	public Set<String> getXMLNSs() {
		return XMLNNS;
	}

	@Override
	public Query parseQuery(Query query, Packet packet) throws ComponentException {
		Element queryEl = packet.getElement().getChildStaticStr("query");
		query.setXMLNS(queryEl.getXMLNS());

		query.setQuestionerJID(packet.getStanzaFrom());
		query.setComponentJID(packet.getStanzaTo());

		query.setId(queryEl.getAttributeStaticStr("queryid"));

		if (queryEl.getChild("x", "jabber:x:data") == null) {
			query.getRsm().fromElement(queryEl);
			validateRsm(query.getRsm());
			
			return query;
		}

		if (!getXMLNSs().contains(DataForm.getFieldValue(queryEl, "FORM_TYPE"))) {
			throw new ComponentException(Authorization.BAD_REQUEST, "Invalid form type");
		}

		String start = DataForm.getFieldValue(queryEl, "start");
		try {
			query.setStart(timestampHelper.parseTimestamp(start));
		} catch (ParseException ex) {
			throw new ComponentException(Authorization.BAD_REQUEST, "Invalid value in 'start' field", ex);
		}

		String end = DataForm.getFieldValue(queryEl, "end");
		try {
			query.setEnd(timestampHelper.parseTimestamp(end));
		} catch (ParseException ex) {
			throw new ComponentException(Authorization.BAD_REQUEST, "Invalid value in 'end' field", ex);
		}

		String with = DataForm.getFieldValue(queryEl, "with");
		if (with != null && !with.isEmpty()) {
			try {
				query.setWith(JID.jidInstance(with));
			} catch (TigaseStringprepException ex) {
				throw new ComponentException(Authorization.BAD_REQUEST, "Invalid value in 'with' field", ex);
			}
		}

		query.getRsm().fromElement(queryEl);
		validateRsm(query.getRsm());

		return query;
	}

	@Override
	public Element prepareForm(Element elem) {
		return prepareForm(elem, MAM_XMLNS);
	}

	@Override
	public Element prepareForm(Element elem, String xmlns, Packet packet) {
		Element x = DataForm.addDataForm(elem, Command.DataType.form);
		DataForm.addHiddenField(elem, "FORM_TYPE", xmlns);

		addField(x, "with", "jid-single", "With");
		addField(x, "start", "jid-single", "Start");
		addField(x, "end", "jid-single", "End");

		return elem;
	}

	protected void addField(Element x, String var, String type, String label) {
		Element field = new Element("field", new String[]{"type", "var"}, new String[]{type, var});
		if (label != null) {
			field.setAttribute("label", label);
		}
		x.addChild(field);
	}

	protected void validateRsm(RSM rsm) throws ComponentException {
		assertIsUUID(rsm.getAfter());
		assertIsUUID(rsm.getBefore());
	}

	protected void assertIsUUID(String uuid) throws ComponentException {
		if (uuid == null) {
			return;
		}
		try {
			UUID.fromString(uuid);
		} catch (IllegalArgumentException ex) {
			throw new ComponentException(Authorization.NOT_ACCEPTABLE, "Invalid item id: " + uuid, ex);
		}
	}
}
