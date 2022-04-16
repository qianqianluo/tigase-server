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
package tigase.xmpp.mam.modules;

import tigase.component.PacketWriter;
import tigase.component.exceptions.ComponentException;
import tigase.component.modules.Module;
import tigase.criteria.Criteria;
import tigase.kernel.beans.Bean;
import tigase.kernel.beans.Inject;
import tigase.server.Packet;
import tigase.util.stringprep.TigaseStringprepException;
import tigase.xml.Element;
import tigase.xmpp.StanzaType;
import tigase.xmpp.mam.Query;
import tigase.xmpp.mam.QueryParser;

/**
 * Implementation of module responsible for handling request to retrive form used in XEP-0313: Message Archive
 * Management
 * <br>
 * Created by andrzej on 19.07.2016.
 */
@Bean(name = "mamGetFormModule", active = true)
public class GetFormModule
		implements Module {

	@Inject
	private PacketWriter packetWriter;
	@Inject(bean = "mamQueryParser")
	private QueryParser<Query> queryParser;

	@Override
	public String[] getFeatures() {
		return new String[0];
	}

	@Override
	public Criteria getModuleCriteria() {
		return null;
	}

	@Override
	public boolean canHandle(Packet packet) {
		return packet.getElement().findChild(child -> child.getName() == "query" && queryParser.getXMLNSs().contains(child.getXMLNS())) != null &&
				packet.getType() == StanzaType.get;
	}

	@Override
	public void process(Packet packet) throws ComponentException, TigaseStringprepException {
		Element reqQuery = packet.getElement().findChild(child -> child.getName() == "query" && queryParser.getXMLNSs().contains(child.getXMLNS()));
		Element query = new Element("query");
		query.setXMLNS(reqQuery.getXMLNS());

		queryParser.prepareForm(query, reqQuery.getXMLNS(), packet);

		packetWriter.write(packet.okResult(query, 0));
	}
}
